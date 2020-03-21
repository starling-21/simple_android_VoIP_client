package com.starling.zvonilka.net.rtptest;

import com.starling.zvonilka.net.jlibrtp.Participant;
import com.starling.zvonilka.net.jlibrtp.RTPAppIntf;
import com.starling.zvonilka.net.jlibrtp.RTPSession;
import com.starling.zvonilka.net.jlibrtp.RtpPkt;

import java.net.DatagramSocket;

/**
 * Created by starling on 3/2/2018.
 */

public class UnicastTest1 {

    static boolean jing = false;
    static boolean abhijeet = true;
    long byteCounter = 0;

    int portDelta = 1000;

    public UnicastTest1() {}

    public UnicastTest1(int portDelta) {
        this.portDelta = portDelta;
    }

    public void runTest(String[] args) {
        // 1. Create sockets for the RTPSession
        DatagramSocket rtpSocket = null;
        DatagramSocket rtcpSocket = null;

        if (jing) {
            System.out.println("Running test case for Jing");
        } else if (abhijeet) {
            System.out.println("Running test case for Abhijeet");
        } else {
            System.out.println("Enable one of the test cases in the source code first.");
            return;
        }

        int portnum = 0;
        if (args.length != 1) {
            System.out.println("Please specify 1 or 2, denoting the participant.");
            return;
        } else {
            if (args[0].equals("1")) {
                portnum = 16384 + portDelta;
            } else if (args[0].equals("2")) {
                portnum = 16386 + portDelta;
            } else {
                System.out.println("Unknown argument");
                return;
            }
        }

        try {
            rtpSocket = new DatagramSocket(portnum);
            rtcpSocket = new DatagramSocket(portnum + 1);
        } catch (Exception e) {
            System.out.println("RTPSession failed to obtain port");
        }

        // 2. Create the RTP session
        RTPSession rtpSession = new RTPSession(rtpSocket, rtcpSocket);
        //rtpSession.naivePktReception(true);

        System.out.println("My rtpSession CNAME:" + rtpSession.CNAME());

        // 3. Instantiate the application object
        UnicastExample2 uex = new UnicastExample2(rtpSession);

        // 4. Add participants we want to notify upon registration
        // a. Hopefully nobody is listening on this port.
        if (portnum == (16386 + portDelta))
            portnum = 16382 + portDelta;

//        Participant part = new Participant("127.0.0.1", portnum + 2, portnum + 3);
        Participant part = new Participant("127.0.0.1", portnum - 1000, portnum - 1000 + 1 );
        System.out.println("Participant " + args[0] + ", binding to " + (portnum - 1000) + "," + (portnum - 1000 + 1));
        System.out.println("Will send stuff to " + (portnum - 1000) + "," + (portnum - 1000 + 1));
        rtpSession.addParticipant(part);

        // 5. Register the callback interface, this launches RTCP threads too
        // The two null parameters are for the RTCP and debug interfaces, not use here
        rtpSession.RTPSessionRegister(uex, null, null);

        // Wait 2500 ms, because of the initial RTCP wait
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }

        // Note: The wait is optional, but insures SDES packets
        //       receive participants before continuing

        // 6. Send some data
        if (args[0].equals("1")) {
            System.out.print("Sending data");
        } else {
            System.out.print("Receiving data");
        }
        if (jing) {
            // RTCP SR, RR test for Jing... so we send a little data, and spend a long
            // time doing it to collect some reports
            byte[] tmp = new byte[10];
            int i;

            for (i = 0; i < 60; i++) {
                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                }

                if (args[0].equals("1"))
                    rtpSession.sendData(tmp);

                if (i % 10 == 0)
                    System.out.print(".");
            }

            if (args[0].equals("1"))
                System.out.println("Done, sent " + (i * 10) + " bytes ");

        } else if (abhijeet) {
            // Test for abhijeet

            // The question was whether jlibrtp has a problem if you send 4 Mbyte+?
            // Could also be related to number of packets, but there are
            // current no known bugs in that category either.

            byte[] tmp = new byte[1024];
            int i;

            for (i = 0; i < 4000; i++) {
                try {
                    Thread.sleep(5);
                } catch (Exception e) {
                }

                if (args[0].equals("1")) {
                    rtpSession.sendData(tmp);

                    if (i % 100 == 0)
                        System.out.print(".");
                }
            }

            if (args[0].equals("1"))
                System.out.println("Done sent, sent " + i + " kbytes ");
        }

        // Give the sending threads etc a chance to complete.
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
        }
        System.out.println("Received " + byteCounter + " bytes");
        // 7. Terminate the session, takes a few ms to kill threads in order.
        rtpSession.endSession();
        //This may result in "Sleep interrupted" messages, ignore them
    }


    class UnicastExample2 implements RTPAppIntf {
        /**
         * The tests included in this file
         */

//        long byteCounter = 0;
        long pktCounter = 0;

        /**
         * Holds a RTPSession instance
         */
        RTPSession rtpSession = null;


        /**
         * A minimal constructor
         */
        public UnicastExample2(RTPSession rtpSession) {
            this.rtpSession = rtpSession;
        }

        // RTPAppIntf  All of the following are documented in the JavaDocs

        /**
         * Used to receive data from the RTP Library. We expect no data
         */
        public void receiveData(RtpPkt frame, Participant p) {
            /**
             * This concatenates all received packets for a single timestamp
             * into a single byte[]
             */
            byte[] data = frame.getPayload();
            byteCounter += data.length;
            pktCounter++;

            if (pktCounter % 100 == 0) {
                System.out.print(".");
            }

            String cname = p.getCNAME();
            //if(! abhijeet) {
            	System.out.println("Received data from " + cname + "  pkt counter=" + pktCounter);
            	System.out.println(new String(data));
            //}
        }

        /**
         * Used to communicate updates to the user database through RTCP
         */
        public void userEvent(int type, Participant[] participant) {
            //Do nothing
        }

        /**
         * How many packets make up a complete frame for the payload type?
         */
        public int getBufferSize() {
            return 1;
        }

        @Override
        public int getFirstSeqNumber() {
            return 0;
        }
        // RTPAppIntf/


    }

}
