package com.starling.zvonilka.sipua.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.starling.zvonilka.utils.Logg;
import com.starling.zvonilka.utils.TimeUtil;

/**
 * Created by starling on 3/6/2018.
 */

public class SipReconnectScheduleUtil {


    /**
     * register broadcast intents generatore in AlarmManager for triggering SIP reconnect service
     */
    public static void createWebSocketReconnectScedulle(Context context) {

//        int minutes = 20;
        int minutes = 2;

        Intent alarm = new Intent(context, SipReconnectBroadCastReceiver.class);
        boolean alarmRunning = (PendingIntent.getBroadcast(context, 0, alarm, PendingIntent.FLAG_NO_CREATE) != null);

        Logg.line("SipReconnectScheduleUtil createWebSocketReconnectScedulle, alarmsRunning=" + alarmRunning + " time=" + TimeUtil.getTime_HH_mm_ss());
        if (alarmRunning == false) {
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarm, 0);
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);
            alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), minutes*60000, pendingIntent);
        }
    }

}
