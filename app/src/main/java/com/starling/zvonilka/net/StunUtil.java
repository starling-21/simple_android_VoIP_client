package com.starling.zvonilka.net;

import com.starling.zvonilka.utils.Logg;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import de.javawi.jstun.attribute.ChangeRequest;
import de.javawi.jstun.attribute.MappedAddress;
import de.javawi.jstun.attribute.MessageAttribute;
import de.javawi.jstun.attribute.MessageAttributeParsingException;
import de.javawi.jstun.header.MessageHeader;
import de.javawi.jstun.util.UtilityException;

/**
 * Created by starling on 2/3/2018.
 *
 * allow to get egres NAT binding
 */

public class StunUtil {


    /**
     * return NAT'ed outside adresss binding
     * @return - bundle, engress IP and port
     */
    public static MappedAddress getMappedAddress() {
//        Thread t = new Thread() {
//
//            @Override
//            public void run() {
        MappedAddress mappedAddress = null;
                try {
                    MessageHeader sendMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingRequest);
                    // sendMH.generateTransactionID();

                    // add an empty ChangeRequest attribute. Not required by the
                    // standard,
                    // but JSTUN server requires it

                    ChangeRequest changeRequest = new ChangeRequest();
                    sendMH.addMessageAttribute(changeRequest);

                    byte[] data = sendMH.getBytes();


                    DatagramSocket s = new DatagramSocket();
                    s.setReuseAddress(true);

                    DatagramPacket p = new DatagramPacket(data, data.length, InetAddress.getByName("stun.l.google.com"), 19302);
                    s.send(p);

                    DatagramPacket rp;

                    rp = new DatagramPacket(new byte[32], 32);

                    s.receive(rp);
                    MessageHeader receiveMH = new MessageHeader(MessageHeader.MessageHeaderType.BindingResponse);
                    // System.out.println(receiveMH.getTransactionID().toString() + "Size:"
                    // + receiveMH.getTransactionID().length);
                    receiveMH.parseAttributes(rp.getData());
                    mappedAddress = (MappedAddress) receiveMH
                            .getMessageAttribute(MessageAttribute.MessageAttributeType.MappedAddress);
                    Logg.ing("PUBLIC IP BINDINGS=" + mappedAddress.getAddress()+" "+mappedAddress.getPort());

                } catch (UtilityException e1) {
                    e1.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (MessageAttributeParsingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        return mappedAddress;
//            }
//        };
//        t.runTest();
    }


}
