package com.biosensetek.vitalsign.diapnote;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    EditText contentEt;
    Button sendBtn;
    private ServiceConnection sc;
    public SocketService socketService;
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        contentEt = findViewById(R.id.contentEt);
        sendBtn = findViewById(R.id.sendBtn);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String data = contentEt.getText().toString().trim();
                socketService.sendOrder(data);
            }
        });
        //test
        Log.i("WifiWake", "onCreate");
        bindSocketService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(sc);

        Intent intent = new Intent(getApplicationContext(), SocketService.class);

        stopService(intent);
    }

    //
    private void bindSocketService() {
        Log.i("WifiWake", "bindSocketService");
        /*通過binder拿到service*/
        sc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                Log.i("WifiWake", "onServiceConnected");
                SocketService.SocketBinder binder = (SocketService.SocketBinder) iBinder;
                socketService = binder.getService();
            }
            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.i("WifiWake", "onServiceDisconnected");
            }
        };
        Intent intent = new Intent(getApplicationContext(), SocketService.class);
        bindService(intent, sc, BIND_AUTO_CREATE);
    }


}