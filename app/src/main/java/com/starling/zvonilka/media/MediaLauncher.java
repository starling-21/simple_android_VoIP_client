package com.starling.zvonilka.media;

import com.starling.zvonilka.net.AudioStreamingSession;
import com.starling.zvonilka.utils.Logg;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by starling on 3/2/2018.
 *
 * this class starts all needed threads related to voice recording, playing and streaming
 */

public class MediaLauncher {

    AudioRecorder audioRecorder;
    AudioPlayer audioPlayer;
    AudioStreamingSession audioStreamingSocket;

    ArrayBlockingQueue<ByteBuffer> outgoingAudioQueue = new ArrayBlockingQueue<ByteBuffer>(1000);
    ArrayBlockingQueue<ByteBuffer> incomingAudioQueue = new ArrayBlockingQueue<ByteBuffer>(1000);

    boolean streaming;
    boolean muted;

    int remote_udp_port;

    public MediaLauncher(int remote_udp_port) {
        this.remote_udp_port = remote_udp_port;
    }


    /**
     * start UDP (rtp session)
     */
    public void startStreaming() {
        streaming = true;

        this.audioRecorder = new AudioRecorder(outgoingAudioQueue);
        this.audioPlayer = new AudioPlayer(incomingAudioQueue);

        this.audioStreamingSocket = new AudioStreamingSession(outgoingAudioQueue, incomingAudioQueue, remote_udp_port);

        audioRecorder.startJob();
        audioPlayer.startJob();
        audioStreamingSocket.startJob();
    }


    /**
     * stop all threads related to recording, streaming and playing
     */
    public void stopStreaming() {
        streaming = false;
        audioRecorder.terminateJob();
        audioPlayer.terminateJob();
        audioStreamingSocket.terminateJob();
    }

    public boolean isStreaming() {
        return streaming;
    }


    /**
     * mute and un_mute media stream
     * stop and restart audio recording and playing stream
     */
    public void muteMedia() {
        muted = !muted;
        if (muted) {
            audioRecorder.terminateJob();
            audioPlayer.terminateJob();
        } else {
            outgoingAudioQueue.clear();
            incomingAudioQueue.clear();
            this.audioRecorder = new AudioRecorder(outgoingAudioQueue);
            this.audioPlayer = new AudioPlayer(incomingAudioQueue);
            audioRecorder.startJob();
            audioPlayer.startJob();
        }
    }

}
