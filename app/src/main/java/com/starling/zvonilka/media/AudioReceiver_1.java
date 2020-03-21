package com.starling.zvonilka.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;

import com.starling.zvonilka.jniwrappers.OpusDecoder;
import com.starling.zvonilka.utils.Logg;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by starling on 2/22/2018.
 */

//TODO TEST CLASS
public class AudioReceiver_1 extends Thread {

    public final int SAMPLE_RATE = 16000;// Sample rate must be one supported by Opus.
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

    DatagramSocket serverSocket;
    AudioTrack audioTrack = null;


    short[] outBuf = new short[BUF_SIZE];
    byte[] receiveDataBuf = new byte[1024];

    private ArrayBlockingQueue<ByteBuffer> incomingAudio = new ArrayBlockingQueue<ByteBuffer>(1000);


    public AudioReceiver_1() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        try {
            serverSocket = new DatagramSocket(50005);
            serverSocket.setReuseAddress(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            int minBufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM);


            audioTrack.play();
            AudioPlayer audioPlayer = new AudioPlayer();
            audioPlayer.start();

            DatagramPacket receivePacket = new DatagramPacket(receiveDataBuf, receiveDataBuf.length);
            Logg.ing("Network audio listening thread started");

            long totalDecodedBytes = 0;
            while (!stopped) {
                serverSocket.receive(receivePacket);
                Logg.ing("received packet len = " + receivePacket.getLength());

                //receive data from socket (cutted to 15 bytes --> TO DO data packetization )
                byte[] receivedBuff = Arrays.copyOfRange(receivePacket.getData(), 0, 15);//receivePacket.getData().length);
                incomingAudio.add(ByteBuffer.wrap(receivedBuff));

                //decoding and playing
//                int decoded = opusDecoder.decode(receivedBuff, outBuf);
//                totalDecodedBytes += decoded;
//                Logg.ing("tablet decoded=" + decoded + " bytes");
//                audioTrack.write(outBuf, 0, decoded);

//                audioTrack.write(receivedBuff, 0, receivedBuff.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startListening() {
        start();
    }

    public void stopListening() {
        this.stopped = true;
        serverSocket.disconnect();
        serverSocket.close();
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

    public class AudioPlayer extends Thread {

        OpusDecoder opusDecoder;

        public AudioPlayer() {
            opusDecoder = new OpusDecoder();
            opusDecoder.init(SAMPLE_RATE, 1, FRAME_SIZE / 2);
        }

        @Override
        public void run() {
            Logg.ing("Audio player started, NET RECEIVER");

            long totalDecodedBytes = 0;
            while (!stopped) {
                try {
                    if (!incomingAudio.isEmpty() && incomingAudio.size() > 25) {
                        ByteBuffer byteBuffer = incomingAudio.poll();
                        Logg.ing("POLLED buffer " + byteBuffer.array().length + " bytes, outGoingAudio collection size:" + incomingAudio.size());


                        int decoded = opusDecoder.decode(byteBuffer.array(), outBuf);
                        totalDecodedBytes += decoded;
                        Logg.ing("tablet decoded=" + decoded + " bytes");
                        audioTrack.write(outBuf, 0, decoded);

                        //decoding and playing
//                        int decoded = opusDecoder.decode(encodedBuf_2, decodedOutBuf);
//                        Logg.ing("DE_ENCODED = " + decoded);
//                        audioTrack.write(decodedOutBuf, 0, decoded);

//                        audioTrack.write(shortBuffer.array(), 0, shortBuffer.array().length);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }

    }

}
