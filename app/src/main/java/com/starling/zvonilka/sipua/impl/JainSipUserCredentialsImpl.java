package com.starling.zvonilka.sipua.impl;


import android.gov.nist.javax.sip.clientauthutils.UserCredentials;

/**
 * Created by starling on 1/3/2018.
 * jain sip user credential implementation, neede for JainSipAccountManagerImpl
 */

public class JainSipUserCredentialsImpl implements UserCredentials {
    private String userName;
    private String sipDomain;
    private String password;

    public JainSipUserCredentialsImpl(String userName, String sipDomain, String password)
    {
        this.userName = userName;
        this.sipDomain = sipDomain;
        this.password = password;
    }

    public String getPassword()
    {
        return password;
    }

    public String getSipDomain()
    {
        return sipDomain;
    }

    public String getUserName()
    {
        return userName;
    }

}