package com.starling.zvonilka.sipua;

import android.content.Context;

import com.starling.zvonilka.sipua.impl.CustomSipEventProcessor;
import com.starling.zvonilka.sipua.impl.SipManager;
import com.starling.zvonilka.sipua.services.SipReconnectScheduleUtil;

/**
 * Created by starling on 1/27/2018.
 *
 * This class is a main point to manage all sip interaction between User Agent (UA)
 * and sip server.
 *
 * SipEngine functions:
 *
 * startSipEngine()
 * - initialize sip stack - SipManager,
 * - create listener for SipManager
 * - register UA on sip server
 * - create scheduller which automatically register UA on sip server
 *
 *
 * stopSipEngine()
 * - unregister UA
 * - delete all CustomSipEvent listens
 * - uninitialize SipManager which is considered as SipStack
 * - remove automatic registration schedule
 */

public class SipEngine {


    /**
     * init SIP STACK, add sip events listener
     * then send registration request
     * and create sip auto-registration schedulle
     * @param context
     */
    public static void startSipEngine(final Context context) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                SipReconnectScheduleUtil.createWebSocketReconnectScedulle(context);
                SipManager sipManager = SipManager.getInstance();
                if(!SipManager.getInstance().isInitialized()) {
                    sipManager.addCustomSipEventListener(new CustomSipEventProcessor(context));
                    sipManager.init(context);
                }
                sipManager.register();
            }
        };
        new Thread(runnable).start();

    }

    public static void stopSipEngine() {
        SipManager sipManager = SipManager.getInstance();
        sipManager.stopSipStack();
    }

}
