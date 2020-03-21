package com.starling.zvonilka.media;

import android.media.AudioManager;
import android.media.ToneGenerator;

import com.starling.zvonilka.utils.Logg;

/**
 * Created by starling on 3/11/2018.
 * this class play ring back diring user start call
 */

public class RingBackPlayer {

    ToneGenerator tonGen;
    private boolean ringing = false;
    int ringingTime = 0;

    Thread workingThread;

    public RingBackPlayer() {

    }


    /**
     * starts ring tone generator
     */
    public void startRinging() {
//        ringing = true;
        workingThread = new Thread() {
            @Override
            public void run() {
                ringing = true;
                Logg.line("THREAD STRTED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                ringingTime = 0;
                tonGen = new ToneGenerator(
                        AudioManager.STREAM_VOICE_CALL, ToneGenerator.MAX_VOLUME);

                while (ringing) { //&& ringingTime < 15) {
                    tonGen.startTone(ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK, 1000);
                    try {
                        Thread.currentThread().sleep(3000);
                        ringingTime += 3;
                        Logg.line("RINGING =" + ringing);
                        if (ringingTime > 27) {
                            Logg.line("My Ringing time excid");
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
//        if (ringing == true) {
//            abonetNeAbonent();
//        }

            }
        };

        workingThread.start();
    }

    public boolean isRinging() {
        return this.ringing;
    }

    public void stopRinging() {
        try {
            ringing = false;
            tonGen.stopTone();
            tonGen.release();
        } catch (Exception e) {e.printStackTrace();}
        Logg.line("STOP RINGING");
    }

}