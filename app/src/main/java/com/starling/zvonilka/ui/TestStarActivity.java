package com.starling.zvonilka.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.starling.zvonilka.R;
import com.starling.zvonilka.sipua.impl.CustomSipEventProcessor;
import com.starling.zvonilka.sipua.impl.SipManager;

import org.greenrobot.eventbus.EventBus;

public class TestStarActivity extends AppCompatActivity implements View.OnClickListener {

    Button reg_btn;
    Button unreg_btn;

    Button invite_btn;
    Button cancel_btn;

    Button call_accept_btn;
    Button reject_btn;

    Button busy_btn;
    Button bye_btn;

    Button clear_btn;


    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_start_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textView = (TextView) findViewById(R.id.textView);
        textView.setMovementMethod(new ScrollingMovementMethod());


        reg_btn = (Button) findViewById(R.id.reg_btn);
        reg_btn.setOnClickListener(this);

        unreg_btn = (Button) findViewById(R.id.unreg_btn);
        unreg_btn.setOnClickListener(this);

        invite_btn = (Button) findViewById(R.id.invite_btn);
        invite_btn.setOnClickListener(this);

        reject_btn = (Button) findViewById(R.id.call_reject_btn);
        reject_btn.setOnClickListener(this);

        call_accept_btn = (Button) findViewById(R.id.call_accept_btn);
        call_accept_btn.setOnClickListener(this);

        clear_btn = (Button) findViewById(R.id.clear_btn);
        clear_btn.setOnClickListener(this);

        cancel_btn = (Button) findViewById(R.id.cancel_btn);
        cancel_btn.setOnClickListener(this);

        busy_btn = (Button) findViewById(R.id.busy_btn);
        busy_btn.setOnClickListener(this);

        bye_btn = (Button) findViewById(R.id.bye_btn);
        bye_btn.setOnClickListener(this);


    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        initSipEngine();
        register();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_star, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.reg_btn:
                register();
                break;
            case R.id.unreg_btn:
                unregister();
                break;
            case R.id.call_reject_btn:
                reject_call();
                break;
            case R.id.invite_btn:
                sendInvite();
                break;
            case R.id.clear_btn:
                textView.setText("");
                SipManager.getInstance().setInitialized(false);
                break;
            case R.id.call_accept_btn:
                accept_call();
                break;
            case R.id.cancel_btn:
                cancelCall();
                break;
            case R.id.busy_btn:
                sendBusy();
                break;
            case R.id.bye_btn:
                sendBye();
                break;
        }
    }

    private void accept_call() {
        //TODO implement in SipManager
        SipManager manager = SipManager.getInstance();
        manager.acceptCall();
    }

    private void reject_call() {
        SipManager sipManager = SipManager.getInstance();
//        sipManager.sendDecline();
    }

    private void sendInvite() {
        SipManager manager = SipManager.getInstance();
        manager.call("12345");
    }

    private void unregister() {
        Toast.makeText(this, "Unregistering", Toast.LENGTH_SHORT).show();
        SipManager manager = SipManager.getInstance();
        manager.unregister();
    }

    private void register() {
        Toast.makeText(this, "Registering", Toast.LENGTH_SHORT).show();
        //TODO reg sip here
        SipManager manager = SipManager.getInstance();
        manager.register();
    }

    private void cancelCall() {
        SipManager sipManager = SipManager.getInstance();
        sipManager.cancelCalling();
    }

    private void sendBusy() {
        SipManager sipManager = SipManager.getInstance();
//        sipManager.sendBusyHere();
    }

    private void sendBye() {
        SipManager sipManager = SipManager.getInstance();
        sipManager.hangUp();
    }

    private void initSipEngine() {
        SipManager sipManager = SipManager.getInstance();
        CustomSipEventProcessor sipEventProcessor = new CustomSipEventProcessor(this);
        sipManager.addCustomSipEventListener(sipEventProcessor);
    }

}
