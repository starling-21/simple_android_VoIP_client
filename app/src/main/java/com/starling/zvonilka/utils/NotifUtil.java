package com.starling.zvonilka.utils;

import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.starling.zvonilka.sipua.impl.SipManager;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by starling on 1/7/2018.
 * class for showing test notification
 */

public class NotifUtil {

    public static void showSipStatusNotif(Context context, String text) {
        SipManager sipManager = SipManager.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        NotificationCompat.Builder mBuilder = null;
        mBuilder = new NotificationCompat.Builder(context)
//                .setSmallIcon(android.R.drawable.alert_light_frame)
                .setContentTitle(text)
                .setContentText("time: " + currentDateandTime);

        if (text.equalsIgnoreCase("READY")) {
            mBuilder.setSmallIcon(android.R.drawable.alert_light_frame);
        } else {
            mBuilder.setSmallIcon(android.R.drawable.stat_notify_error);
        }
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(001, mBuilder.build());
    }

}
