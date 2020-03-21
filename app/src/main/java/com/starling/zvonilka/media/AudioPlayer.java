package com.starling.zvonilka.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import com.starling.zvonilka.jniwrappers.OpusDecoder;
import com.starling.zvonilka.utils.Logg;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by starling on 3/5/2018.
 *
 * this class takes Concurent audio queue, takes audio chanks from it, decode and play
 */

public class AudioPlayer {

    public final int SAMPLE_RATE = 8000;// Sample rate must be one supported by Opus.
    public final int pTime = 20;//packetization times in ms

    // frames per second, depends on packetization time
    public final int FRAME_RATE = 1000 / pTime;

    public final int SAMPLES_PER_SECOND = SAMPLE_RATE / FRAME_RATE;

    // frame size depends on sample rate based on packetization time for next processing
    //x2 because PCM_16 bit = 2 bytes per sample  //320; //opus_encode() - pcm param-> frame_size*sizeof(opus_int16)
    public final int FRAME_SIZE = SAMPLES_PER_SECOND * 2;

    //diviced by 2 because of short[] buffer for voice
    public final int BUF_SIZE = FRAME_SIZE / 2;

    private boolean stopped = false;

    ArrayBlockingQueue<ByteBuffer> incomingAudioQueue;

    AudioTrack audioTrack;
    OpusDecoder opusDecoder;

    short[] outBuf = new short[BUF_SIZE];

    Thread workingThread;


    public AudioPlayer(ArrayBlockingQueue<ByteBuffer> incomingAudioQueue) {
        this.incomingAudioQueue = incomingAudioQueue;

        opusDecoder = new OpusDecoder();
        opusDecoder.init(SAMPLE_RATE, 1, FRAME_SIZE / 2);
    }


    public void startJob() {
        workingThread = new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Logg.ing("Audio player thread started");

                try {
                    int minBufferSize = AudioRecord.getMinBufferSize(
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);

                    audioTrack = new AudioTrack(
//                            AudioManager.STREAM_MUSIC,
                            AudioManager.STREAM_VOICE_CALL,
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_OUT_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBufferSize,
                            AudioTrack.MODE_STREAM);


                    audioTrack.play();

                    int decoded;
                    while (!stopped) {
                        if (!incomingAudioQueue.isEmpty() && incomingAudioQueue.size() > 20) {
                            ByteBuffer byteBuffer = incomingAudioQueue.poll();
                            //Logg.ing("POLLED buffer " + byteBuffer.array().length + " bytes, incomingAudioQueue size:" + incomingAudioQueue.size());

                            decoded = opusDecoder.decode(byteBuffer.array(), outBuf);
                            //Logg.ing("decoded = " + decoded + " bytes");
                            audioTrack.write(outBuf, 0, decoded);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        workingThread.start();
    }

    public void terminateJob() {
        this.stopped = true;
        opusDecoder.close();
//        audioTrack.stop();//TODO stop all audio Track operation
    }

}
