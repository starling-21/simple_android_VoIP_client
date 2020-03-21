package com.starling.zvonilka.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by starling on 21.03.2017.
 */

public class CustomLogger {

    public static void appendLog(String text) {
        if (Consts.DEBUG) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd_HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            String log = currentDateandTime + " --> " + text;

            File logFile = new File("sdcard/log.txt");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(log);
                buf.newLine();
                buf.close();
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
