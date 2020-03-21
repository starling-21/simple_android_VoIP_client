package com.starling.zvonilka.sipua.impl;

/**
 * Created by starling on 1/27/2018.
 */

public enum CustomSipEvent {

//----------------   INCOMING UAS ACTIONS   --------------------------------------------------------

    /**
     * UA is registered on UAS
     */
    REGISTERED,

    /**
     * UA is unRegistered on UAS
     */
    UNREGISTERED,

    INCOMING_CALL,
    INCOMING_CANCEL,
    INCOMING_BYE,
    INCOMING_DECLINE,
    INCOMING_BUSY,

    /**
     * outgoint call rejection by remote side
     */
    INCOMING_FORBIDDEN,

    INCOMING_TEMPORARY_UNAVALIABLE,


    /**
     * triggers when remote site confirm it's started call by sending ACK for our OK response
     */
    INCOMING_CALL_CONFIRMED,

    /**
     * outgoing request timeout for any request
     */
    INCOMING_TIMEOUT,


//----------------   OUTGOING UA ACTIONS   ---------------------------------------------------------

    /**
     * triggers when user start outgoing call
     */
    OUTGOING_CALL_STARTED,

    /**
     * trigger when we confirm our outgoing call by sending ACK for incoming OK response
     */
    OUTGOING_CALL_CONFIRMED,


    /**
     * hang up
     */
    HANG_UP,

    /**
     * trigger when UA cancel outgoing call before it sterted
     */
    CANCELL_CALLING,


    /**
     * triggers when UA accept incoming call by sending OK to remote site
     */
    CALL_ACCEPT_SENT,

    /**
     * triggers when UA reject incoming call
     */
    INCOMING_CALL_REJECTED,

}
