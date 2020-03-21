package com.starling.zvonilka.sipua.impl;


import android.gov.nist.javax.sip.clientauthutils.AccountManager;
import android.gov.nist.javax.sip.clientauthutils.UserCredentials;
import android.javax.sip.ClientTransaction;

/**
 * Created by starling on 1/3/2018.
 * class implement server credential challange for anauthorized requests
 */

public class JainSipAccountManagerImpl implements AccountManager {

    String Username;
    String Password;
    String RemoteIp;

    public JainSipAccountManagerImpl(String username, String RemoteIp, String password) {
        this.Username = username;
        this.Password = password;
        this.RemoteIp = RemoteIp;

    }

    @Override
    public UserCredentials getCredentials(ClientTransaction clientTransaction, String s) {
        return new JainSipUserCredentialsImpl(Username, RemoteIp, Password);
    }
}