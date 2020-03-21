package com.starling.zvonilka.sipua.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.starling.zvonilka.sipua.SipEngine;
import com.starling.zvonilka.sipua.impl.SipManager;
import com.starling.zvonilka.utils.CustomLogger;
import com.starling.zvonilka.utils.Logg;

/**
 * Created by starling on 3/6/2018.
 */

public class SipReconnectService extends Service {

    private Context context;
    private Thread backgroundThread;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        this.context = this.getApplicationContext();
        this.backgroundThread = new Thread(myTask);
    }

    private Runnable myTask = new Runnable() {
        public void run() {
            Logg.line("SIP RECONNECT START:" + SipManager.getInstance().getSipManagerState().toString());
            CustomLogger.appendLog("SIP RECONNECT START: " +
                    "   sipManagerState=" + SipManager.getInstance().getSipManagerState().toString() +
                    "   Initialize=" + SipManager.getInstance().isInitialized());

            SipEngine.startSipEngine(context);
            stopSelf();
        }
    };

    @Override
    public void onDestroy() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.backgroundThread.start();
        return START_NOT_STICKY;
    }

}