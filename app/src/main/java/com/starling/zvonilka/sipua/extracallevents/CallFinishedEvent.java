package com.starling.zvonilka.sipua.extracallevents;

/**
 * Created by starling on 1/28/2018.
 * Extra event for activity
 */

public class CallFinishedEvent {

    String sipEvent;

    public CallFinishedEvent(String sipEvent) {
        this.sipEvent = sipEvent;
    }

}
