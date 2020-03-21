package com.starling.zvonilka.utils;

import android.util.Log;

/**
 * Created by starling on 1/3/2018.
 * custom logger class, just for filter logs during testing
 */

public class Logg {

    public static String INFO_TAG = "STA_R_LING-->::";

    public static void ing(String loggingString) {
        Log.i(INFO_TAG, " \r\n\r\n");
        Log.i(INFO_TAG, loggingString);
    }

    public static void line(String loggingString) {
        Log.i(INFO_TAG, " " + TimeUtil.getTime_HH_mm_ss() + "  " + loggingString);
    }

}
