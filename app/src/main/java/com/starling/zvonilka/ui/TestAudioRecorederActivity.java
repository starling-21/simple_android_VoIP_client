package com.starling.zvonilka.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.starling.zvonilka.R;
import com.starling.zvonilka.media.AudioReceiver_1;
import com.starling.zvonilka.media.AudioRecorder_1;
import com.starling.zvonilka.media.AudioRecorder_2;
import com.starling.zvonilka.media.MediaLauncher;


/**
 * Created by starling on 2/10/2018.
 */

public class TestAudioRecorederActivity extends AppCompatActivity implements View.OnClickListener {

    AudioRecorder_1 recorder;
    AudioRecorder_2 recorder2;
    Button start_recording;
    Button stop_recording;
    Button start_listening;
    Button stop_listening;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_testaudiorecorederactivity);


        start_recording = (Button) findViewById(R.id.t4_start_rec_btn);
        start_recording.setOnClickListener(this);

        stop_recording = (Button) findViewById(R.id.t4_stop_rec_btn);
        stop_recording.setOnClickListener(this);

        start_listening = (Button) findViewById(R.id.t4_start_listen_btn);
        start_listening.setOnClickListener(this);

        stop_listening = (Button) findViewById(R.id.t4_stop_listen);
        stop_listening.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.t4_start_rec_btn) {
            startRecoring();
        }

        if (v.getId() == R.id.t4_stop_rec_btn) {
            stopRecording();
        }

        if (v.getId() == R.id.t4_start_listen_btn) {
            startAudioServerSocket();
        }

        if (v.getId() == R.id.t4_stop_listen) {
            stopAudioServerSocket();
        }

    }

    AudioReceiver_1 testNetAudioReceiver;
    private void stopAudioServerSocket() {
        testNetAudioReceiver.stopListening();
    }

    private void startAudioServerSocket() {
        testNetAudioReceiver = new AudioReceiver_1();
        testNetAudioReceiver.startListening();
    }


    MediaLauncher mediaStreamingLauncher;
    private void startRecoring() {
        Toast.makeText(this, "Recording started", Toast.LENGTH_LONG).show();
//        recorder2 = new AudioRecorder_2();
//        recorder2.startRecording();

//        Runnable run1 = new Runnable() {
//            @Override
//            public void run() {
//                dotest1();
//            }
//        };
//        new Thread(run1).start();

//        mediaStreamingLauncher = new MediaLauncher();
//        mediaStreamingLauncher.startStreaming(1);
    }

    private void stopRecording() {
        Toast.makeText(this, "Recording finished", Toast.LENGTH_LONG).show();
//        recorder2.stopRecording();

//        mediaStreamingLauncher.stopStreaming();
    }

    private void dotest1() {
        //test 1
        //RTPSessionTest test1 = new RTPSessionTest();

        //test 2
        //CCrtpReceiverTest test1 = new CCrtpReceiverTest();
        //CCRTPSenderTest test1 = new CCRTPSenderTest();
        //test1.runTest();


        //test 3
//        UnicastTest0 unicastTest0 = new UnicastTest0();
//        unicastTest0.runTest();
//        UnicastTest1 test1 = new UnicastTest1();
//        test1.runTest(new String[]{"1"});
    }
}
