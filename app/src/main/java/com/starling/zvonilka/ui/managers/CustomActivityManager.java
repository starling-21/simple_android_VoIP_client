package com.starling.zvonilka.ui.managers;

import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.starling.zvonilka.ui.SettingsActivity;
import com.starling.zvonilka.ui.TestCallActivity;
import com.starling.zvonilka.ui.TestPreCallActivity;

/**
 * Created by starling on 1/28/2018.
 */

public class CustomActivityManager {

    public static void showPreCallActivity(Context context) {
        Intent preCallActivityIntent = new Intent(context, TestPreCallActivity.class);
        context.startActivity(preCallActivityIntent);
    }


    /**
     * show activity on inoming call
     * @param context
     * @param phoneNumber
     */
    public static void showCallActivity(Context context, String phoneNumber) {
        PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "INCALLWAKEUP");
        wl.acquire(30000L);//30 seconds for incoming ringing
//        wl.acquire();
        Intent callActivityIntent = new Intent(context, TestCallActivity.class);
        callActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        callActivityIntent.putExtra("phone_number", phoneNumber);
        context.startActivity(callActivityIntent);
        wl.release();
    }

    public static void showSettingsActivity(Context context) {
        Intent settingsActivity = new Intent(context, SettingsActivity.class);
        context.startActivity(settingsActivity);
    }

}
