package com.starling.zvonilka.sipua.impl;

import android.content.Context;

import com.starling.zvonilka.utils.Logg;
import com.starling.zvonilka.utils.SharedPrefSingletoon;

import java.util.Random;

/**
 * Created by starling on 1/15/2018.
 */

public class SipProfile {

    private Context context;

    private String localIp;
    private int localSipPort = 1111;

    //    private String transport = "udp";
    private String transport = "tcp";


    SharedPrefSingletoon sharedPrefSingletoon;


    public SipProfile(Context context) {
        this.context = context;
        sharedPrefSingletoon = SharedPrefSingletoon.getInstance(context);
    }

    public String getLocalIp() {
        return localIp;
    }


    /**
     * update local sip port (it is imposible to set sip socket as reusable)
     */
    public void generateNewLocalSipPort() {
        int minSipLocalPort = 10000;
        int maxSipLocalPort = 65534;
        Random r = new Random();
        this.localSipPort  = r.nextInt(maxSipLocalPort - minSipLocalPort + 1) + minSipLocalPort;
    }

    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }


    public int getLocalSipPort() {
        //return Integer.valueOf(sharedPrefSingletoon.getString(SharedPrefSingletoon.Key.SIP_LOCAL_PORT));
        return localSipPort;
    }

    public void setLocalSipPort(int localSipPort) {
        this.localSipPort = localSipPort;
    }


    public String getLocalEndpoint() {
        return localIp + ":" + getLocalSipPort();
    }

    /**
     * return sip servier IP address
     *
     * @return
     */
    public String getRemoteIp() {
        return sharedPrefSingletoon.getString(SharedPrefSingletoon.Key.SIP_SERVER_IP);
    }

    /**
     * return remote SIP-server port for sip communications
     *
     * @return
     */
    public int getRemotePort() {
        return Integer.valueOf(sharedPrefSingletoon.getString(SharedPrefSingletoon.Key.SIP_SERVER_PORT));
    }

    public String getRemoteEndpoint() {
        return getRemoteIp() + ":" + getRemotePort();
    }

    public String getSipUserName() {
        return sharedPrefSingletoon.getString(SharedPrefSingletoon.Key.SIP_USERNAME);
    }

    public String getSipPassword() {
        return sharedPrefSingletoon.getString(SharedPrefSingletoon.Key.SIP_PASSWORD);
    }

    public String getTransport() {
        return transport;
    }

}
