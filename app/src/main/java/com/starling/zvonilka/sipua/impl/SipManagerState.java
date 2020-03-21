package com.starling.zvonilka.sipua.impl;

/**
 * Created by starling on 1/27/2018.
 * declare app's internal sip manager possible states
 */

public enum SipManagerState {

    /**
     * This state is used while the SipManager tries to register the SIP account with the provider.
     */
    REGISTERING,


    /**
     * This state is used while the SipManager tries to unregister the SIP account from the provider.
     */
    UNREGISTERING,


    /**
     * This state heppens when client sends wrong credential to the server
     */
    UNAUTHORIZED,

    /**
     * This state is used when the SipManager is initialized (online) and registered.
     */
    READY,


    /**
     * This state is used when SipManager is unregistered and in offline state
     */
    OFFLINE,


    /**
     * This state is used for incoming call occured
     */
    INCOMING_CALLING,


    /**
     * This state is used for ougoing call
     */
    OUTCOING_CALLING,


    /**
     * This state used when call is establishing
     */
    ESTABLISHING,


    /**
     * This state is used when the call is established.
     */
    ESTABLISHED,


    /**
     * This state is used when an error occurred.
     */
    ERROR

}
