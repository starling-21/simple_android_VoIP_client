package com.starling.zvonilka.sipua.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.starling.zvonilka.sipua.impl.SipManager;
import com.starling.zvonilka.utils.Logg;
import com.starling.zvonilka.utils.NetworkUtil;


/**
 * Created by starling on 17-Jan-17.
 * reinitialize sip stack after connectivity changes
 */

public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Logg.line("NetworkChangeReceiver -> onReceive()  haveNetworkConnection=" + NetworkUtil.haveNetworkConnection(context));
        SipManager.getInstance().setInitialized(false);
        try {
            Thread.sleep(18000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (NetworkUtil.haveNetworkConnection(context)) {
            SipManager.getInstance().reinitializeSipStack();
        }
    }

}
