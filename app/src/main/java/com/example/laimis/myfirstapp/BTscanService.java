package com.example.laimis.myfirstapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

public class BTscanService extends Service {

    int mStartMode=START_STICKY;       // indicates how to behave if the service is killed
    private final IBinder myBinder = new BTLocalBinder();

    private static final long UPDATE_INTERVAL = 1000*5;
    private Timer timer = new Timer();

    public int mCnt=0;

    public BTscanService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return myBinder;
    }

    public class BTLocalBinder extends Binder {
        BTscanService getService() {
            return BTscanService.this;
        }
    }

    @Override
    public void onCreate() {
        // The service is being created

        timer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        doBTscan();
                    }
                },
                0,
                UPDATE_INTERVAL);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        return mStartMode;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
    }

    public void doBTscan() {
        mCnt++;
    }

}
