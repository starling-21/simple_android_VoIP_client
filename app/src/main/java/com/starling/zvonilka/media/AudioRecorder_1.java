package com.starling.zvonilka.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

import com.starling.zvonilka.jniwrappers.OpusDecoder;
import com.starling.zvonilka.jniwrappers.OpusEncoder;
import com.starling.zvonilka.utils.Logg;
import com.starling.zvonilka.utils.TimeUtil;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;

/**
 * Created by starling on 2/10/2018.
 * <p>
 * Thread to manage live voice recording/playback from the device's microphone.
 */

//TODO TEST CLASS
public class AudioRecorder_1 extends Thread {

//    BufferedOutputStream os;
    public DatagramSocket socket;
    private int port=50005;

    OpusEncoder opusEncoder;
    OpusDecoder opusDecoder;


    // Sample rate must be one supported by Opus.
    public static final int SAMPLE_RATE = 8000;

    // Number of samples per frame is not arbitrary,
    // it must match one of the predefined values, specified in the standard.
    // frame size depends on sample rate based on packetization time for next processing
    public static final int FRAME_SIZE = 160;       //opus_encode() - pcm param-> frame_size*sizeof(opus_int16)
    public static final int BUF_SIZE = FRAME_SIZE;

    private boolean stopped = false;

    int encoded;
    int decoded;


    short[] inBuf = new short[BUF_SIZE];
    byte[] encodedBuf = new byte[1024];


    byte[] encodedBuf_2;
    short[] outBuf = new short[BUF_SIZE];

    public AudioRecorder_1() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
//        os = prepareAudioFile();
        // init opus encoder
        opusEncoder = new OpusEncoder();
        opusEncoder.init(SAMPLE_RATE, 1, FRAME_SIZE);

        opusDecoder = new OpusDecoder();
        opusDecoder.init(SAMPLE_RATE, 1, FRAME_SIZE);



    }

    @Override
    public void run() {
        Logg.ing("Audio, Running Audio Thread");
        AudioRecord audioRecord = null;
        AudioTrack audioTrack = null;

        /*
         * Initialize buffer to hold continuously recorded audio data, runTest recording, and runTest
         * playback.
         */
        try {
            int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);  // ENCODING_PCM_8BIT  - not guaranted that supported by device

            Logg.ing("AudioRecord.getMinBufferSize=" + minBufferSize);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,  // read about - MediaRecorder.AudioSource.VOICE_COMMUNICATION
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);


            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM);

            audioRecord.startRecording();
            audioTrack.play();


            ///sending socket test part
            DatagramSocket socket = new DatagramSocket();
            Logg.ing("Sending Socket Created");
            DatagramPacket packet;
            final InetAddress destination = InetAddress.getByName("192.168.137.88");
            Log.d("VS", "Address retrieved");

            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            long allMSTime = 0;
            long samples = 0;
            long milis = Calendar.getInstance().getTimeInMillis();
            Logg.ing("runTest recording: time=" + milis);



            int decodeOffset = 0;
            while (!stopped) {

//                int to_read = inBuf.length;
//                int offset = 0;
//                while (to_read > 0) {
//                    int read = audioRecord.read(inBuf, offset, to_read);
//                    if (read < 0) {
//                        throw new Exception("recorder.read() returned error " + read);
//                    }
//                    to_read -= read;
//                    offset += read;
//                    if (read != inBuf.length) {
//                        Logg.ing("DOESN't match");
//                    }
//                }

                int readedAudioBytes = audioRecord.read(inBuf,0,inBuf.length);
                encoded=opusEncoder.encode(inBuf,encodedBuf);
                decodeOffset += encoded;
                Logg.ing("Readed " + readedAudioBytes + " bytes of audio into " + encoded);

                //printing time between recording
                long currentTime = Calendar.getInstance().getTimeInMillis();
                long timeDiff = currentTime - milis;
                milis = currentTime;
                allMSTime += timeDiff;
                samples++;
                Logg.ing("dx time=" + timeDiff + "    all_time=" + allMSTime + "    samples=" + samples);


                encodedBuf_2 = Arrays.copyOf(encodedBuf, encoded);


                //putting buffer in the packet
                //sending audio to the network
                byte[] sendBuf = encodedBuf_2;
                Logg.ing("encoded buff size=" + sendBuf.length + " bytes");
                packet = new DatagramPacket (sendBuf, sendBuf.length, destination, port);

                socket.send(packet);
                Logg.ing("sending " + packet.getData().length + " bytes");


                //decoding and playing
//                decoded = opusDecoder.decode(encodedBuf_2, outBuf);
//                audioTrack.write(outBuf, 0, decoded);

                //play with no encoding
//                os.write(short2byte(inBuf), 0, inBuf.length);
//                audioTrack.write(inBuf, 0, readedAudioBytes);
            }
        } catch (Throwable x) {
            Logg.ing("Audio, Error reading voice audio" + x);
        }
        /*
         * Frees the thread's resources after the loop completes so that it can be run again
         */ finally {
            audioRecord.stop();
            audioRecord.release();
            audioTrack.stop();
            audioTrack.release();

            //close file output stream
//            try {
//                os.close();
//            } catch (Exception e) {
//                Log.e("Log", "Error when releasing", e);
//            }
        }
    }


    //used to create file in SD card source
    private BufferedOutputStream prepareAudioFile() {
        String filePath = Environment.getExternalStorageDirectory().getPath()
                + "/" + "opus.raw";
        BufferedOutputStream os = null;
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            os = new BufferedOutputStream(new FileOutputStream(file));
        } catch (FileNotFoundException e) {
            Log.e("Log", "File not found for recording ", e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return os;
    }

//    private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener(){
//        public void onPeriodicNotification(AudioRecord recorder){
//            //int result = aud.read(buffer, 0, buffer.length);
//        }
//
//        public void onMarkerReached(AudioRecord recorder){
//            Logg.ing("audio record, onMarkerReached");
//        }
//    };

    public static short[] byte2short(byte[] data) {
        int resIndex = 0;
        short resultShort[] = new short[data.length/2];
        for(int i=0; i<data.length; i++) {
            if (i % 2 == 0) {
                ByteBuffer bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(data[i]);
                bb.put(data[i+1]);
                short shortVal = bb.getShort(0);
                resultShort[resIndex] = shortVal;
                resIndex++;
                //return (short)((data[0]<<8) | (data[1]));
            }
        }
        return resultShort;
    }

    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            //sData[i] = 0;
        }
        return bytes;
    }

    public void startRecording() {
        start();
    }

    public void stopRecording() {
        this.stopped = true;
    }

}
