package com.starling.zvonilka.ui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.starling.zvonilka.R;
import com.starling.zvonilka.sipua.ICallUserActionListener;
import com.starling.zvonilka.sipua.extracallevents.AbonentBusyEvent;
import com.starling.zvonilka.sipua.extracallevents.AbonentTemporaryUnavaliable;
import com.starling.zvonilka.sipua.extracallevents.CallAcceptedEvent;
import com.starling.zvonilka.sipua.extracallevents.CallCanceledEvent;
import com.starling.zvonilka.sipua.extracallevents.CallFinishedEvent;
import com.starling.zvonilka.sipua.extracallevents.CallRejectedEvent;
import com.starling.zvonilka.sipua.extracallevents.CallingTimeOutEvent;
import com.starling.zvonilka.sipua.impl.SipManager;
import com.starling.zvonilka.sipua.impl.SipManagerState;
import com.starling.zvonilka.ui.managers.CustomActivityManager;
import com.starling.zvonilka.utils.Logg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by starling on 1/28/2018.
 * <p>
 * this is temporary activity for current call
 * int the future it must be replaced
 */

public class TestCallActivity extends Activity implements View.OnClickListener {

    TextView callDirectionTextView;
    TextView phoneTextView;

    Button accept_btn;
    Button reject_btn;
    Button finish_btn;
    Button cancel_btn;

    Button mute_btn;
    ICallUserActionListener callUserActionListener;

    String phoneNumber;
    boolean muted;

    private Window wind;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_call_activity);

        callDirectionTextView = (TextView) findViewById(R.id.t3_call_status_tv);
        phoneTextView = (TextView) findViewById(R.id.t3_phone_tv);

        accept_btn = (Button) findViewById(R.id.t2_accept_call_btn);
        accept_btn.setOnClickListener(this);

        reject_btn = (Button) findViewById(R.id.t2_reject_call_btn);
        reject_btn.setOnClickListener(this);

        finish_btn = (Button) findViewById(R.id.t2_finish_call_btn);
        finish_btn.setOnClickListener(this);

        cancel_btn = (Button) findViewById(R.id.t2_cancel_btn);
        cancel_btn.setOnClickListener(this);

        mute_btn = (Button) findViewById(R.id.t2_mute_btn);
        mute_btn.setOnClickListener(this);


        //extracting extras variables
        Bundle bundle = getIntent().getExtras();
        phoneNumber = bundle.getString("phone_number");


        //link for muting the call
        callUserActionListener = (ICallUserActionListener) SipManager.getInstance().getCustomSipEventListener();
    }


    @Override
    public void onResume() {
        super.onResume();

        wind = this.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);//only for phones which do not have security enabled locks like pattern lock
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);//bring your current Activity on the top
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);


        //registering for incoming custom sip events
        //ERROR NULL when "this" is in long term of inactivity
        try {
            EventBus.getDefault().register(this);
            initGuiElements(phoneNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }

        //close activity it not a call
        if (SipManager.getInstance().getSipManagerState() == SipManagerState.READY ||
                SipManager.getInstance().getSipManagerState() == SipManagerState.OFFLINE) {
            CustomActivityManager.showPreCallActivity(this);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.t2_accept_call_btn) {
            acceptCall();
        }

        if (v.getId() == R.id.t2_reject_call_btn) {
            rejectCall();
        }

        if (v.getId() == R.id.t2_finish_call_btn) {
            finishCall();
        }

        if (v.getId() == R.id.t2_cancel_btn) {
            cancellCall();
        }

        if (v.getId() == R.id.t2_mute_btn) {
            muteCall();
        }
    }


    private void updateCallDirection(String call_direction) {
        callDirectionTextView.setText(call_direction);
    }


    private void updatePhoneTag(String text) {
        phoneTextView.setText(text);
    }

    /**
     * update GUI, show and hide needed elements
     *
     * @param phoneNumber
     */
    private void initGuiElements(String phoneNumber) {
        SipManagerState sipManagerState = SipManager.getInstance().getSipManagerState();
        if (sipManagerState == SipManagerState.OUTCOING_CALLING) {
            updateGuiForOutgoingCall();
        } else if (sipManagerState == SipManagerState.INCOMING_CALLING) {
            updateGuiForIncomingCall();
        }

        updateCallDirection(sipManagerState.toString());
        updatePhoneTag(phoneNumber);
    }

    private void updateGuiForOutgoingCall() {
        accept_btn.setVisibility(View.GONE);
        reject_btn.setVisibility(View.GONE);
        finish_btn.setVisibility(View.GONE);
        cancel_btn.setVisibility(View.VISIBLE);
    }

    private void updateGuiForIncomingCall() {
        accept_btn.setVisibility(View.VISIBLE);
        reject_btn.setVisibility(View.VISIBLE);
        finish_btn.setVisibility(View.GONE);
        cancel_btn.setVisibility(View.GONE);
    }

    private void updateGuiForCallEstablished() {
        accept_btn.setVisibility(View.GONE);
        reject_btn.setVisibility(View.GONE);
        finish_btn.setVisibility(View.VISIBLE);
        cancel_btn.setVisibility(View.GONE);
    }

    private void cancellCall() {
        SipManager.getInstance().cancelCalling();
        CustomActivityManager.showPreCallActivity(this);
    }

    private void finishCall() {
        SipManager.getInstance().hangUp();
        CustomActivityManager.showPreCallActivity(this);
    }

    private void rejectCall() {
        SipManager.getInstance().rejectCall();
        CustomActivityManager.showPreCallActivity(this);
    }

    private void acceptCall() {
        SipManager.getInstance().acceptCall();
    }

    private void muteCall() {
        callUserActionListener.onCallMute();
        muted = !muted;
        if (muted) {
            mute_btn.setText("UN_MUTE");
        } else {
            mute_btn.setText("MUTE");
        }
    }


    /**
     * ********************************************************************************************
     * <p>
     * INCOMING SIP EVENTS SECTION
     * <p>
     * *********************************************************************************************
     */

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CallCanceledEvent event) {
        Toast.makeText(this, "CANCELED by remote side", Toast.LENGTH_SHORT).show();
        closeActivityWithDelay();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CallFinishedEvent event) throws InterruptedException {
        Toast.makeText(this, "FINISHED by remote side", Toast.LENGTH_SHORT).show();
        closeActivityWithDelay();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CallRejectedEvent event) throws InterruptedException {
        Toast.makeText(this, "REJECTED by remote side", Toast.LENGTH_SHORT).show();
        closeActivityWithDelay();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CallAcceptedEvent event) {
        updateGuiForCallEstablished();
        Toast.makeText(this, "IN CALL", Toast.LENGTH_SHORT).show();
        updateCallDirection(SipManager.getInstance().getSipManagerState().toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbonentBusyEvent event) throws InterruptedException {
        Toast.makeText(this, "Abonent is BUSY", Toast.LENGTH_SHORT).show();
        closeActivityWithDelay();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbonentTemporaryUnavaliable event) throws InterruptedException {
        Toast.makeText(this, "Abonent is TEMPORARY UNAVALIABLE", Toast.LENGTH_SHORT).show();
        closeActivityWithDelay();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CallingTimeOutEvent event) throws InterruptedException {
        Toast.makeText(this, "Call was not accepted", Toast.LENGTH_SHORT).show();
        closeActivityWithDelay();
    }


    //automaticaly close this activity after few seconds
    private void closeActivityWithDelay() {
        int delaySeconds = 2;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                TestCallActivity.this.finish();
            }
        }, 1000 * delaySeconds);
    }


}
