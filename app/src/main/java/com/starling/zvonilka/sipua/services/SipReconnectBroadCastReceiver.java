package com.starling.zvonilka.sipua.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.starling.zvonilka.utils.Logg;

/**
 * Created by starling on 3/6/2018.
 * start SipReconnect service on received reconnect broadcast
 */

public class SipReconnectBroadCastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent reconnectIntent = new Intent(context, SipReconnectService.class);
        Logg.line("SipReconnectBroadCastReceiver -> onReceive");
        context.startService(reconnectIntent);
    }

}
