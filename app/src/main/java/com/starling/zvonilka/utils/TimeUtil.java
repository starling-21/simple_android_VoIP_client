package com.starling.zvonilka.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by starling on 1/27/2018.
 */

public class TimeUtil {

    public static String getTime_HH_mm_ss() {
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => "+c.getTime());

//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }

}
