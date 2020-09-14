package com.biosensetek.vitalsign.diapnote;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.TabHost;
import android.widget.Toast;

import com.biosensetek.vitalsign.diapnote.Constants;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;


public class SocketService extends Service {

    /*socket*/
    private Socket socket;
    /*連線執行緒*/
    private Thread connectThread;
    private Timer timer = new Timer();
    private OutputStream outputStream;
    private SocketBinder sockerBinder = new SocketBinder();
    private String ip;
    private String port;
    private TimerTask task;

    /*預設重連*/
    private boolean isReConnect = true;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public IBinder onBind(Intent intent) {
        return sockerBinder;
    }
    public class SocketBinder extends Binder {

        /*返回SocketService 在需要的地方可以通過ServiceConnection獲取到SocketService  */
        public SocketService getService() {
            return SocketService.this;
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("WifiWake", "onCreate SocketService");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("WifiWake", "onStartCommand");
        /*拿到傳遞過來的ip和埠號*/
        /*
        ip = intent.getStringExtra(Constants.INTENT_IP);
        port = intent.getStringExtra(Constants.INTENT_PORT);
        */
        ip = "10.0.2.2";
        port = "4005";
        /*初始化socket*/
        initSocket();
        return super.onStartCommand(intent, flags, startId);
    }
    /*初始化socket*/
    private void initSocket() {
        Log.i("WifiWake", "initSocket");
        if (socket == null && connectThread == null) {
            connectThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.i("WifiWake", "initsocket");
                    socket = new Socket();
                    try {
                        /*超時時間為2秒*/
                        socket.connect(new InetSocketAddress(ip, Integer.valueOf(port)), 2000);
                        /*連線成功的話  傳送心跳包*/
                        if (socket.isConnected()) {

                            /*因為Toast是要執行在主執行緒的  這裡是子執行緒  所以需要到主執行緒哪裡去顯示toast*/
                            toastMsg("socket已連線");
                            /*傳送連線成功的訊息*/
                            /*
                            EventMsg msg = new EventMsg();
                            msg.setTag(Constants.CONNET_SUCCESS);
                            EventBus.getDefault().post(msg);
                            */
                            /*傳送心跳資料*/
                            sendBeatData();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        if (e instanceof SocketTimeoutException) {
                            toastMsg("連線超時，正在重連");
                            releaseSocket();

                        } else if (e instanceof NoRouteToHostException) {
                            toastMsg("該地址不存在，請檢查");
                            stopSelf();
                        } else if (e instanceof ConnectException) {
                            toastMsg("連線異常或被拒絕，請檢查");
                            stopSelf();
                        }
                    }
                }
            });
            /*啟動連線執行緒*/
            connectThread.start();
        }

    }

    /*因為Toast是要執行在主執行緒的   所以需要到主執行緒哪裡去顯示toast*/
    private void toastMsg(final String msg) {

        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /*傳送資料*/
    public void sendOrder(final String order) {
        if (socket != null && socket.isConnected()) {
            /*傳送指令*/
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        outputStream = socket.getOutputStream();
                        if (outputStream != null) {
                            outputStream.write((order).getBytes("gbk"));
                            outputStream.flush();
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }).start();

        } else {
            toastMsg("socket連線錯誤,請重試");
        }
    }

    /*定時傳送資料*/
    private void sendBeatData() {
        if (timer == null) {
            timer = new Timer();
        }

        if (task == null) {
            task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        outputStream = socket.getOutputStream();

                        /*這裡的編碼方式根據你的需求去改*/
                        outputStream.write(("test").getBytes("gbk"));
                        outputStream.flush();
                    } catch (Exception e) {
                        /*傳送失敗說明socket斷開了或者出現了其他錯誤*/
                        toastMsg("連線斷開，正在重連");
                        /*重連*/
                        releaseSocket();
                        e.printStackTrace();


                    }
                }
            };
        }

        timer.schedule(task, 0, 2000);
    }


    /*釋放資源*/
    private void releaseSocket() {

        if (task != null) {
            task.cancel();
            task = null;
        }
        if (timer != null) {
            timer.purge();
            timer.cancel();
            timer = null;
        }

        if (outputStream != null) {
            try {
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            outputStream = null;
        }

        if (socket != null) {
            try {
                socket.close();

            } catch (IOException e) {
            }
            socket = null;
        }

        if (connectThread != null) {
            connectThread = null;
        }

        /*重新初始化socket*/
        if (isReConnect) {
            initSocket();
        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("SocketService", "onDestroy");
        isReConnect = false;
        releaseSocket();
    }
}

