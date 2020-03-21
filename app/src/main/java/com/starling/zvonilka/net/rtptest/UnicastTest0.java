package com.starling.zvonilka.net.rtptest;

import com.starling.zvonilka.net.jlibrtp.Participant;
import com.starling.zvonilka.net.jlibrtp.RTPAppIntf;
import com.starling.zvonilka.net.jlibrtp.RTPSession;
import com.starling.zvonilka.net.jlibrtp.RtpPkt;

import java.net.DatagramSocket;

/**
 * Created by starling on 3/2/2018.
 */

public class UnicastTest0 {

    RTPSession rtpSession;

    public void runTest() {
        // 1. Create sockets for the RTPSession
        DatagramSocket rtpSocket = null;
        DatagramSocket rtcpSocket = null;
        try {
            rtpSocket = new DatagramSocket(16384);
            rtcpSocket = new DatagramSocket(16385);
        } catch (Exception e) {
            System.out.println("RTPSession failed to obtain port");
        }

        // 2. Create the RTP session
        rtpSession = new RTPSession(rtpSocket, rtcpSocket);

        // 3. Instantiate the application object
        UnicastExample uex = new UnicastExample(rtpSession);

        // 4. Add participants we want to notify upon registration
        // a. Hopefully nobody is listening on this port.
        Participant part = new Participant("127.0.0.1",17384,17385);
        rtpSession.addParticipant(part);

        // 5. Register the callback interface, this launches RTCP threads too
        // The two null parameters are for the RTCP and debug interfaces, not use here
        rtpSession.RTPSessionRegister(uex, null, null);

        // Wait 2500 ms, because of the initial RTCP wait
        try{ Thread.sleep(2000); } catch(Exception e) {}

        // Note: The wait is optional, but insures SDES packets
        //       receive participants before continuing

        // 6. Send some data
        String str = "Hi there!";
        System.out.println("Sending " + str);
        rtpSession.sendData(str.getBytes());

        // 7. Terminate the session, takes a few ms to kill threads in order.
        //rtpSession.endSession();
        //This may result in "Sleep interrupted" messages, ignore them
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    class UnicastExample implements RTPAppIntf {
        /** Holds a RTPSession instance */
        RTPSession rtpSession = null;

        /** A minimal constructor */
        public UnicastExample(RTPSession rtpSession) {
            this.rtpSession = rtpSession;
        }

        // RTPAppIntf  All of the following are documented in the JavaDocs
        /** Used to receive data from the RTP Library. We expect no data */
        public void receiveData(RtpPkt frame, Participant p) {
            /**
             * This concatenates all received packets for a single timestamp
             * into a single byte[]
             */
            byte[] data = frame.getPayload();

            /**
             * This returns the CNAME, if any, associated with the SSRC
             * that was provided in the RTP packets received.
             */
            String cname = p.getCNAME();

            System.out.println("Received data from " + cname);
            System.out.println(new String(data));

            System.out.println("Sending received data back");
            rtpSession.sendData(frame.getPayload());
        }

        /** Used to communicate updates to the user database through RTCP */
        public void userEvent(int type, Participant[] participant) {
            //Do nothing
        }

        /** How many packets make up a complete frame for the payload type? */
        public int getBufferSize() {
            return 1;
        }
        @Override
        public int getFirstSeqNumber() {
            return 0;
        }

    }
}

