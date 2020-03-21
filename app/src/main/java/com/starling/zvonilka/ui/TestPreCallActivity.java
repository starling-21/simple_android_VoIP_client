package com.starling.zvonilka.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.starling.zvonilka.R;
import com.starling.zvonilka.sipua.SipEngine;
import com.starling.zvonilka.sipua.impl.SipManager;
import com.starling.zvonilka.sipua.services.SipReconnectScheduleUtil;
import com.starling.zvonilka.ui.managers.CustomActivityManager;


/**
 * Created by starling on 1/27/2018.
 * <p>
 * temporary activity for starting the call
 * must be replaced
 */

public class TestPreCallActivity extends AppCompatActivity implements View.OnClickListener {

    EditText call_to_editText;
    Button call_btn;
    Button call_22022_btn;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_pre_call_activity);

        call_to_editText = (EditText) findViewById(R.id.t1_call_to_textView);

        call_btn = (Button) findViewById(R.id.t1_call_btn);
        call_btn.setOnClickListener(this);

        call_22022_btn = (Button) findViewById(R.id.t2_call_btn);
        call_22022_btn.setOnClickListener(this);

    }

    @Override
    public void onResume() {
        super.onResume();
        SipEngine.startSipEngine(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.t1_call_btn) {
            startCall();
        }

        if (v.getId() == R.id.t2_call_btn) {
            startCallTo22022();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_star, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                CustomActivityManager.showSettingsActivity(this);
                return true;


            case R.id.reg_settings:
//                new Timer().schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        Logg.ing("RESTARTING TIMER");
//                SipReconnectScheduleUtil.createWebSocketReconnectScedulle(this);
//                SipEngine.startSipEngine(TestPreCallActivity.this);

//                    }
//                }, 0, 120000);//every 120 seconds
                break;

        }
        return true;
    }

    private void startCall() {
        String to = call_to_editText.getText().toString();
        CustomActivityManager.showCallActivity(this, to);
        SipManager.getInstance().call(to);
    }

    private void startCallTo22022() {
        CustomActivityManager.showCallActivity(this, "22022");
        SipManager.getInstance().call("22022");
    }


}
