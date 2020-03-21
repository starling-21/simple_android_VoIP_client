package com.starling.zvonilka.sipua;

/**
 * Created by starling on 1/19/2018.
 *
 * interface for basic SIP and CALL function for UA
 */

public interface ISipManager {

    void register();

    void unregister();

    void call(String toNumber);

    void cancelCalling();

    void hangUp();

    void acceptCall();

    void rejectCall();

    void sendMessage();

}
