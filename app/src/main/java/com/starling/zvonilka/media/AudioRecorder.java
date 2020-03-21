package com.starling.zvonilka.media;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.starling.zvonilka.jniwrappers.OpusEncoder;
import com.starling.zvonilka.utils.Logg;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by starling on 3/2/2018.
 * Reading microphone audio and putting it to the concurent outgoing audio queue
 */

public class AudioRecorder {

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

    ArrayBlockingQueue<ByteBuffer> outgoingAudioQueue;
    AudioRecord audioRecord;
    OpusEncoder opusEncoder;

    short[] inBuf = new short[BUF_SIZE];
    byte[] encodedBuf = new byte[1024];

    Thread workingThread;


    public AudioRecorder(ArrayBlockingQueue<ByteBuffer> outgoingAudioQueue) {
        this.outgoingAudioQueue = outgoingAudioQueue;

        opusEncoder = new OpusEncoder();
        opusEncoder.init(SAMPLE_RATE, 1, FRAME_SIZE / 2);
    }


    public void startJob() {
        workingThread = new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
                Logg.ing("Audio, Running AudioRecorder Thread");

                try {
                    int minBufferSize = AudioRecord.getMinBufferSize(
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT);  // ENCODING_PCM_8BIT  - not guaranted that supported by device

                    audioRecord = new AudioRecord(
                            MediaRecorder.AudioSource.MIC,  // read about - MediaRecorder.AudioSource.VOICE_COMMUNICATION
                            SAMPLE_RATE,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            minBufferSize);

                    audioRecord.startRecording();


                    int encodedLen;
////////////////////////////////////////////////////////////////////////////////////////////////////
                    while (!stopped) {
//                        int readedAudioBytes = audioRecord.read(inBuf, 0, inBuf.length);
                        audioRecord.read(inBuf, 0, inBuf.length);

                        //encode audio and put to encoded buffer
                        encodedLen = opusEncoder.encode(inBuf, encodedBuf);
                        //Logg.ing("readed " + readedAudioBytes + " bytes and ENCODED into " + encodedLen + " bytes");

                        if (outgoingAudioQueue.remainingCapacity() > 0) {
                            ByteBuffer byteBuffer = ByteBuffer.allocate(encodedLen);
                            outgoingAudioQueue.add(byteBuffer.put(encodedBuf, 0, encodedLen));
                        }
                    }
                } catch (Exception x) {
                    Logg.ing("Audio, Error reading voice audio" + x);
                    x.printStackTrace();
                } finally {
                    audioRecord.stop();
                    audioRecord.release();
                }
            }
        };

        workingThread.start();
    }

    public void terminateJob() {
        this.stopped = true;
        opusEncoder.close();
    }
}
