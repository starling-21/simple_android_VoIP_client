package com.starling.zvonilka.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;

import com.starling.zvonilka.jniwrappers.OpusDecoder;
import com.starling.zvonilka.jniwrappers.OpusEncoder;
import com.starling.zvonilka.utils.Logg;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by starling on 2/24/2018.
 */

//TODO TEST CLASS
public class AudioRecorder_2 extends Thread {
    public DatagramSocket socket;
    InetAddress destinationAddress;
    DatagramPacket packet;
    private int port = 50005;

    OpusEncoder opusEncoder;
    OpusDecoder opusDecoder;
    AudioRecord audioRecord = null;
    AudioTrack audioTrack = null;

    private ArrayBlockingQueue<ShortBuffer> outgoingAudio = new ArrayBlockingQueue<ShortBuffer>(1000);


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



    short[] inBuf = new short[BUF_SIZE];

    public AudioRecorder_2() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        ///init sending client socket
        try {
            socket = new DatagramSocket();
            Logg.ing("Sending Socket Created");

            destinationAddress = InetAddress.getByName("192.168.137.155");
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Logg.ing("Audio, Running Audio Thread");

        try {
            int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);  // ENCODING_PCM_8BIT  - not guaranted that supported by device

            //inBuf = new short[minBufferSize/2];

            Logg.ing("AudioRecord.getMinBufferSize=" + minBufferSize);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,  // read about - MediaRecorder.AudioSource.VOICE_COMMUNICATION
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize);


//            AudioRecord.OnRecordPositionUpdateListener positionUpdater = new AudioRecord.OnRecordPositionUpdateListener() {
//                @Override
//                public void onPeriodicNotification(AudioRecord recorder) {
////                    Date d = new Date();
////                    Logg.ing("periodic notification " + d.toLocaleString() + " mili " + d.getTime());
////                    int readedAudioBytes = audioRecord.read(testBuff, 0, testBuff.length);
////                    encoded = opusEncoder.encode(testBuff, encodedBuf);
//////                sendAudioBuffer.write(encodedBuf, 0, encoded);
//////                decodeOffset += encoded;
////                    Logg.ing("Readed " + readedAudioBytes + " bytes of audio into " + encoded + " bytes");
////
////
////                    //do something amazing with audio data
//                }
//
//                @Override
//                public void onMarkerReached(AudioRecord recorder) {
//                    Logg.ing("marker reached");
//                }
//            };
//            audioRecord.setRecordPositionUpdateListener(positionUpdater);
//            audioRecord.setPositionNotificationPeriod(160);


            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM);

            audioRecord.startRecording();
            audioTrack.play();
            TestAudioSender audioPlayer = new TestAudioSender();
            audioPlayer.start();


            /*
             * Loops until something outside of this thread stops it.
             * Reads the data from the recorder and writes it to the audio track for playback.
             */
            long allMSTime = 0;
            long samples = 0;
            long milis = Calendar.getInstance().getTimeInMillis();
            Logg.ing("runTest recording: time=" + milis);


/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            while (!stopped) {

                int readedAudioBytes = audioRecord.read(inBuf, 0, inBuf.length);
                if (outgoingAudio.remainingCapacity() > 0) {
                    outgoingAudio.add(ShortBuffer.wrap(inBuf));
                    Logg.ing("readed bytes: " + readedAudioBytes  + "bytes to the QUEUE, capacity=" + outgoingAudio.size());
                }


                //reading audio and putting encoded data to Queue audio buffer
//                int readedAudioBytes = audioRecord.read(inBuf,0,inBuf.length);
//                int encoded = opusEncoder.encode(inBuf, encodedBuf);
////                sendAudioBuffer.write(encodedBuf, 0, encoded);
////                decodeOffset += encoded;
//                Logg.ing("Readed " + readedAudioBytes + " bytes of audio into " + encoded + " bytes,      encodedBuf size=" + encodedBuf.length);

                //printing time between recording
                long currentTime = Calendar.getInstance().getTimeInMillis();
                long timeDiff = currentTime - milis;
                milis = currentTime;
                allMSTime += timeDiff;
                samples++;
                Logg.ing("dx t=" + timeDiff + "    all_time=" + allMSTime + "    samples=" + samples);

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
        }
    }

    public static short[] byte2short(byte[] data) {
        int resIndex = 0;
        short resultShort[] = new short[data.length / 2];
        for (int i = 0; i < data.length; i++) {
            if (i % 2 == 0) {
                ByteBuffer bb = ByteBuffer.allocate(2);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                bb.put(data[i]);
                bb.put(data[i + 1]);
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


////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////
    public class TestAudioSender extends Thread {

        byte[] sendBuf;

        OpusEncoder opusEncoder;
        OpusDecoder opusDecoder;
        byte[] encodedBuf = new byte[1024];

        public TestAudioSender() {
            opusEncoder = new OpusEncoder();
            opusEncoder.init(SAMPLE_RATE, 1, FRAME_SIZE / 2);

            opusDecoder = new OpusDecoder();
            opusDecoder.init(SAMPLE_RATE, 1, FRAME_SIZE / 2);
        }

        @Override
        public void run() {
            Logg.ing("AUdio player started");

            short[] decodedOutBuf = new short[BUF_SIZE];
            while (!stopped) {
                try {
                    if (!outgoingAudio.isEmpty() && outgoingAudio.size() > 40) {
                        ShortBuffer shortBuffer = outgoingAudio.poll();
                        Logg.ing("POLLED buffer " + shortBuffer.array().length + " bytes, outGoingAudio collection size:" + outgoingAudio.size());

                        //encode and copy to new buffer
                        int encoded = opusEncoder.encode(shortBuffer.array(), encodedBuf);
                        Logg.ing("ENCODED = " + encoded);
                        byte[] encodedBuf_2 = Arrays.copyOf(encodedBuf, encoded);

                        //decoding and playing
//                        int decoded = opusDecoder.decode(encodedBuf_2, decodedOutBuf);
//                        Logg.ing("DE_ENCODED = " + decoded);
//                        audioTrack.write(decodedOutBuf, 0, decoded);
//                      sendAudioBuffer.write(encodedBuf, 0, encoded);

                        sendBuf = encodedBuf_2;

                        packet = new DatagramPacket(sendBuf, sendBuf.length, destinationAddress, port);
                        socket.send(packet);
                        Logg.ing("sending " + packet.getData().length + " bytes");
                    }
                } catch (Exception e) {e.printStackTrace();}
            }
        }

    }
}
