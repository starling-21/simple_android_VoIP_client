package com.starling.zvonilka.net;

import com.starling.zvonilka.net.jlibrtp.Participant;
import com.starling.zvonilka.net.jlibrtp.RTPAppIntf;
import com.starling.zvonilka.net.jlibrtp.RTPSession;
import com.starling.zvonilka.net.jlibrtp.RtpPkt;

import java.net.DatagramSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by starling on 3/2/2018.
 *
 * this class represend symmetric RTP streaming session
 */

public class AudioStreamingSession extends Thread implements RTPAppIntf {

    ArrayBlockingQueue<ByteBuffer> outgoingAudioQueue;
    ArrayBlockingQueue<ByteBuffer> imcomingAudioQueue;

    DatagramSocket rtpSocket = null;
    DatagramSocket rtcpSocket = null;
    RTPSession rtpSession = null;

    int localRtpPort = 16200;
    int localRtcpPort = 16201;

    int remoteRtpPort;
    int remoteRtcpPort;

    boolean stopped = false;

    String remoteIpAddress = "91.214.114.106";//TODO change for dynamic initialization

    /**
     *
     * @param outgoingAudioQueue - councurent safe audio quue
     * @param incomingAudioQueue - councurent safe audio quue
     * @param remoteRtpPort - remote UDP port for rtp streaming
     */
    public AudioStreamingSession(ArrayBlockingQueue<ByteBuffer> outgoingAudioQueue,
                                 ArrayBlockingQueue<ByteBuffer> incomingAudioQueue,
                                 int remoteRtpPort) {

        this.outgoingAudioQueue = outgoingAudioQueue;
        this.imcomingAudioQueue = incomingAudioQueue;

        this.remoteRtpPort = remoteRtpPort;
    }



    @Override
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        initSocket();

        while (!stopped) {
//            if (!outgoingAudioQueue.isEmpty() && outgoingAudioQueue.size() > 20) {
            if (!outgoingAudioQueue.isEmpty()) {
                ByteBuffer byteBuffer = outgoingAudioQueue.poll();
//                Logg.ing("POLLED buffer " + byteBuffer.array().length + " bytes, outGoingAudio collection size:" + outgoingAudioQueue.size());

                rtpSession.sendData(byteBuffer.array());
                //Logg.ing("SENDING data, size is " + byteBuffer.array().length);

                try {
                    Thread.sleep(5); //TODO this is test staff
                } catch (Exception e) {}
            }
        }
    }

    /**
     * init rtp session sockets and remote participant
     */
    private void initSocket() {
        try {
            rtpSocket = new DatagramSocket(localRtpPort);
            rtcpSocket = new DatagramSocket(localRtcpPort);
        } catch (Exception e) {
            System.out.println("RTPSession failed to obtain port");
        }

        rtpSession = new RTPSession(rtpSocket, rtcpSocket);
        rtpSession.RTPSessionRegister(this, null, null);

        rtpSession.payloadType(96);//dydnamic rtp payload type for opus

        //adding participant, where to send traffic
        Participant p = new Participant(remoteIpAddress, remoteRtpPort, remoteRtcpPort);
        rtpSession.addParticipant(p);
    }

    @Override
    public void receiveData(RtpPkt aFrame, Participant participant) {
        byte[] receivedBuff = Arrays.copyOfRange(aFrame.getPayload(), 0, aFrame.getPayloadLength());
        imcomingAudioQueue.add(ByteBuffer.wrap(receivedBuff));
        //System.out.println("RE_CEIVED data from socket, size = " + aFrame.getPayloadLength());
    }

    @Override
    public void userEvent(int type, Participant[] participant) {
    }

    @Override
    public int getBufferSize() {
        return 1;
    }

    @Override
    public int getFirstSeqNumber() {
        return 0;
    }

    public void startJob() {
        super.start();
    }

    public void terminateJob() {
        this.stopped = true;
        rtpSession.endSession();
    }


}
