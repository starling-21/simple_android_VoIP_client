package com.starling.zvonilka.sipua.impl;

import android.content.Context;
import android.gov.nist.javax.sip.message.SIPMessage;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import com.starling.zvonilka.media.MediaLauncher;
import com.starling.zvonilka.media.RingBackPlayer;
import com.starling.zvonilka.sipua.ICallUserActionListener;
import com.starling.zvonilka.sipua.ISipEventListener;
import com.starling.zvonilka.sipua.extracallevents.AbonentBusyEvent;
import com.starling.zvonilka.sipua.extracallevents.AbonentTemporaryUnavaliable;
import com.starling.zvonilka.sipua.extracallevents.CallAcceptedEvent;
import com.starling.zvonilka.sipua.extracallevents.CallCanceledEvent;
import com.starling.zvonilka.sipua.extracallevents.CallFinishedEvent;
import com.starling.zvonilka.sipua.extracallevents.CallRejectedEvent;
import com.starling.zvonilka.sipua.extracallevents.CallingTimeOutEvent;
import com.starling.zvonilka.ui.TestCallActivity;
import com.starling.zvonilka.ui.managers.CustomActivityManager;
import com.starling.zvonilka.utils.Logg;
import com.starling.zvonilka.utils.NotifUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by starling on 1/27/2018.
 * This class is a higher level events handler for CUSTOM_SIP_EVENTS which go from SipManager and
 * for CALL_USER_ACTION (ICallUserActionListener) (like mute call, take on hold and etc.)
 */

public class CustomSipEventProcessor implements ISipEventListener, ICallUserActionListener {

    Context context;
    CustomSipEvent currentCustomSipEvent;


    MediaLauncher mediaStreamingLauncher;
    RingBackPlayer ringBackPlayer;
    Ringtone ringtonePlayer;


    public CustomSipEventProcessor(Context context) {
        this.context = context;
    }

    /**
     * second layer handler for sip events
     * all sip events goes from SipManagere vid dispatchCustomSipEvent() and being processed here
     *
     * @param customSipEvent - enum, sip event, most common incoming events
     * @param message        incoming SipMEssage
     */
    @Override
    public void onCustomSipEvent(CustomSipEvent customSipEvent, SIPMessage message) {
        currentCustomSipEvent = customSipEvent;

        switch (customSipEvent) {
//-----------------  INCOMING ACTIONS ZONE  --------------------------------------------------------
            case REGISTERED:
                processRegistered();
                break;

            case UNREGISTERED:
                processUnregistered();
                break;

            case INCOMING_CALL:
                processIncomingCall(message);
                break;

            case INCOMING_CANCEL:
                processIncomingCancel();
                break;

            case INCOMING_BYE:
                processIncomingBye();
                break;

            case INCOMING_FORBIDDEN:
                processIncomingForbidden();
                break;

            case INCOMING_BUSY:
                processIncomingBusy();
                break;

            case INCOMING_TEMPORARY_UNAVALIABLE:
                processIncomingTemporaryUnavaliable();
                break;

            case INCOMING_CALL_CONFIRMED:
                processIncomingCallConfirmed(message);
                break;

            case INCOMING_TIMEOUT:
                processIncomingTimeout();
                break;

//-----------------  OUTGOING ACTIONS ZONE  --------------------------------------------------------
            case OUTGOING_CALL_STARTED:
                processOutgoingCallStarted();
                break;

            case OUTGOING_CALL_CONFIRMED:
                processOutgoingCallConfirmed(message);
                break;

            case HANG_UP:
                processHangUp();
                break;

            case CANCELL_CALLING:
                processCancelCalling();
                break;

            case CALL_ACCEPT_SENT:
                processCallAcceptSent();
                break;

            case INCOMING_CALL_REJECTED:
                processIncomingCallRejected();
                break;
        }
    }
//--------------------------------------------------------------------------------------------------

    private void processRegistered() {
        NotifUtil.showSipStatusNotif(context, "READY");
        Logg.ing("processRegistered");
    }

    private void processUnregistered() {
        NotifUtil.showSipStatusNotif(context, "OFFLINE");
        Logg.ing("processUnregistered");
    }

    private void processIncomingCancel() {
        stopRingtone();
        if (mediaStreamingLauncher != null && mediaStreamingLauncher.isStreaming()) {
            mediaStreamingLauncher.stopStreaming();
        }
        EventBus.getDefault().post(new CallCanceledEvent(currentCustomSipEvent.toString()));
        Logg.ing("processIncomingCancel");
    }

    private void processIncomingCall(SIPMessage sipMessage) {
        startRingtone();

        Logg.ing("processIncomingCall");
        String callerPhoneNumber = sipMessage.getFrom().getAddress().getDisplayName();
        CustomActivityManager.showCallActivity(context, callerPhoneNumber);

        int rtpPort = SdpUtil.getRemoteAudioPort(sipMessage);
        mediaStreamingLauncher = new MediaLauncher(rtpPort);

    }

    private void processIncomingBye() {
        Logg.ing("processIncomingBye");
        if (mediaStreamingLauncher != null && mediaStreamingLauncher.isStreaming()) {
            mediaStreamingLauncher.stopStreaming();
        }
        EventBus.getDefault().post(new CallFinishedEvent(currentCustomSipEvent.toString()));
    }

    /**
     * proceed outgoint call rejection by remote side (code 403 for invite from UAS)
     */
    private void processIncomingForbidden() {
        stopRingBack();

        EventBus.getDefault().post(new CallRejectedEvent(currentCustomSipEvent.toString()));
        Logg.ing("processIncomingForbidden");
    }

    private void processIncomingBusy() {
        EventBus.getDefault().post(new AbonentBusyEvent(currentCustomSipEvent.toString()));
        Logg.ing("processIncomingBusy");
    }

    private void processIncomingTemporaryUnavaliable() {
        EventBus.getDefault().post(new AbonentTemporaryUnavaliable(currentCustomSipEvent.toString()));
        Logg.ing("processIncomingTemporaryUnavaliable");
    }

    /**
     * we accept incoming call
     *
     * @param sipMessage
     */
    private void processIncomingCallConfirmed(SIPMessage sipMessage) {
        mediaStreamingLauncher.startStreaming();
        EventBus.getDefault().post(new CallAcceptedEvent(currentCustomSipEvent.toString()));//TODO it does no change call activity title
        Logg.ing("processIncomingCallConfirmed");
    }


    /**
     * process incoming DECLINE as timout for outgoing calling
     */
    private void processIncomingTimeout() {
        stopRingBack();
        EventBus.getDefault().post(new CallingTimeOutEvent(currentCustomSipEvent.toString()));
        Logg.ing("processIncomingTimeout");
    }

//------------------  OUTGOING ACTIONS METHODS  ----------------------------------------------------

    /**
     * stop ringBack player tone generator and start media streaming
     *
     * @param sipMessage
     */
    private void processOutgoingCallConfirmed(SIPMessage sipMessage) {
        Logg.ing("processOutgoingCallConfirmed");

        stopRingBack();

        EventBus.getDefault().post(new CallAcceptedEvent(currentCustomSipEvent.toString()));//TODO it does no change call activity title
        int rtpPort = SdpUtil.getRemoteAudioPort(sipMessage);
        mediaStreamingLauncher = new MediaLauncher(rtpPort);
        mediaStreamingLauncher.startStreaming();

    }


    /**
     * stop media streaming
     */
    private void processHangUp() {
        Logg.ing("process HangUp");

        if (mediaStreamingLauncher != null && mediaStreamingLauncher.isStreaming()) {
            mediaStreamingLauncher.stopStreaming();
        }
    }


    /**
     * starts playing ringtonePlayer signal to the user until remote side pick up the phone
     */
    private void processOutgoingCallStarted() {
        Logg.ing("process processOutgoingCallStarted");
        startRingBack();
    }

    private void processCancelCalling() {
        Logg.ing("process processCancelCalling" + " ringback.isRInging=" + ringBackPlayer.isRinging());
        stopRingBack();
    }

    private void processCallAcceptSent() {
        stopRingtone();
    }

    private void processIncomingCallRejected() {
        stopRingtone();
    }


    //------------------   DIFFERENT SUPPORTIVE STAFF   ------------------------------------------------
    private void startRingBack() {
        if (ringBackPlayer == null) {
            ringBackPlayer = new RingBackPlayer();
            ringBackPlayer.startRinging();
        }
    }

    private void stopRingBack() {
        if (ringBackPlayer != null && ringBackPlayer.isRinging()) {
            ringBackPlayer.stopRinging();
            ringBackPlayer = null;
        }
    }


    private void startRingtone() {
        stopRingtone();
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtonePlayer = RingtoneManager.getRingtone(context, uri);
        ringtonePlayer.play();

        //stop ringtone after 60 second if call has not started
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Logg.ing("RINGTON activity thread checker");
                if (SipManager.getInstance().getSipManagerState() !=
                        SipManager.getInstance().getSipManagerState().ESTABLISHED) {
                    if (ringtonePlayer != null)
                        ringtonePlayer.stop();
                }
            }
        }, 60000L);
    }

    private void stopRingtone() {
        if (ringtonePlayer != null && ringtonePlayer.isPlaying()) {
            ringtonePlayer.stop();
            ringtonePlayer = null;
        }
    }


    @Override
    public void onCallMute() {
        Logg.line("MUTE BTN IS PRESSED");
        mediaStreamingLauncher.muteMedia();
    }
}
