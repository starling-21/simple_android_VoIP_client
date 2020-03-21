package com.starling.zvonilka.sipua;

import android.gov.nist.javax.sip.message.SIPMessage;

import com.starling.zvonilka.sipua.impl.CustomSipEvent;

/**
 * Created by starling on 1/27/2018.
 */

public interface ISipEventListener {

    /**
     * triggers when higher layer process sip messages
     * @param customSipEvent - enum, sip event, most common incoming events
     * @param message incoming SipMEssage
     */
    void onCustomSipEvent(CustomSipEvent customSipEvent, SIPMessage message);

}
