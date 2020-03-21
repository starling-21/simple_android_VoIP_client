package com.starling.zvonilka.sipua.impl;

import android.gov.nist.javax.sdp.SessionDescriptionImpl;
import android.gov.nist.javax.sdp.parser.SDPAnnounceParser;
import android.gov.nist.javax.sip.message.SIPMessage;
import android.javax.sdp.MediaDescription;
import android.javax.sdp.SdpException;

import com.starling.zvonilka.media.MediaLauncher;
import com.starling.zvonilka.utils.Logg;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

/**
 * Created by starling on 3/12/2018.
 */

public class SdpUtil {

    public static int getRemoteAudioPort(SIPMessage sipMessage) {
        int remoteAudioPort = -1;
        try {
            //getting remote UDP port for audio from SDP
            byte[] rawContent = sipMessage.getRawContent();
            String sdpContent = new String(rawContent, "UTF-8");
            SDPAnnounceParser parser = new SDPAnnounceParser(sdpContent);
            SessionDescriptionImpl sessiondescription = parser.parse();
            MediaDescription incomingMediaDescriptor = (MediaDescription) sessiondescription
                    .getMediaDescriptions(false).get(0);
            remoteAudioPort = incomingMediaDescriptor.getMedia()
                    .getMediaPort();

            Logg.ing("remote port fof AUDIO=" + remoteAudioPort);

        } catch (SdpException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return remoteAudioPort;
    }

}
