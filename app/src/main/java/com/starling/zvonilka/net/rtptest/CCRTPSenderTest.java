package com.starling.zvonilka.net.rtptest;

import com.starling.zvonilka.net.jlibrtp.Participant;
import com.starling.zvonilka.net.jlibrtp.RTPAppIntf;
import com.starling.zvonilka.net.jlibrtp.RTPSession;
import com.starling.zvonilka.net.jlibrtp.RtpPkt;
import com.starling.zvonilka.utils.Logg;

import java.net.DatagramSocket;

/**
 * Created by starling on 3/1/2018.
 */

public class CCRTPSenderTest {

    public void runTest() {
        CCRTPSender me = new CCRTPSender();

        DatagramSocket rtpSocket = null;
        DatagramSocket rtcpSocket = null;

        try {
            rtpSocket = new DatagramSocket(16384);
            rtcpSocket = new DatagramSocket(16385);
        } catch (Exception e) {
            System.out.println("RTPSession failed to obtain port");
        }

        me.rtpSession = new RTPSession(rtpSocket, rtcpSocket);
        me.rtpSession.naivePktReception(true);
        me.rtpSession.RTPSessionRegister(me,null,null);

        Participant p = new Participant("127.0.0.1", 16386, 16387);
        me.rtpSession.addParticipant(p);

        //me.rtpSession.setPayloadType(0);

        for(int i=0; i<10; i++) {
            String str = "Test number " + i;
            me.rtpSession.sendData(str.getBytes());
        }
        Logg.ing("CCRTPSenderTest is done");

    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    class CCRTPSender implements RTPAppIntf {
        RTPSession rtpSession = null;

        public CCRTPSender() {
            // Do nothing;
        }

        public void receiveData(RtpPkt frame, Participant p) {
            System.out.println("Got data: " + new String(frame.getPayload()));
        }

        public void userEvent(int type, Participant[] participant) {
            //Do nothing
        }

        public int getBufferSize() {
            return 1;
        }

        @Override
        public int getFirstSeqNumber() {
            return 0;
        }
    }
}