package com.starling.zvonilka.sipua.extracallevents;

/**
 * Created by starling on 1/28/2018.
 * Extra event for activity
 * this events triggers when remote side reject the call and sends DECLINE (603) in response
 */

public class CallRejectedEvent {

    String sipEvent;

    public CallRejectedEvent(String sipEvent) {
        this.sipEvent = sipEvent;
    }
}
