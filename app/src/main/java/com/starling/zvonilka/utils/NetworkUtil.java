package com.starling.zvonilka.utils;

import android.accounts.NetworkErrorException;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.Enumeration;

import static android.content.Context.WIFI_SERVICE;

/**
 * Created by starling on 3/6/2018.
 */

public class NetworkUtil {

    /**
     * returns local ip
     * @return
     */
    public static String getLocalIP(Context context) throws NetworkErrorException {
        try {
            for (Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = (NetworkInterface) en.nextElement();
                for (Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {

                        if (isNetworkAvailable(context)) {
                            String ipAddress = inetAddress.getHostAddress().toString();
                            Log.i("IP address", "" + ipAddress);
                            return ipAddress;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("GetIP exception:", ex.toString());
        }
        throw new NetworkErrorException("retrieving local IP address error");
    }


    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


    public static boolean haveNetworkConnection(Context context) {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {

            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())

                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }


    /**
     * retreiving local IP address
     * WIFI address is first
     * @param context
     * @return IP address string
     * @throws NetworkErrorException
     */
    public static String getLocalIpAddress(Context context) throws NetworkErrorException {
        boolean WIFI = false;
        boolean MOBILE = false;

        String ipAddressString = "";

        ConnectivityManager CM = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] networkInfo = CM.getAllNetworkInfo();

        for (NetworkInfo netInfo : networkInfo) {
            if (netInfo.getTypeName().equalsIgnoreCase("WIFI") && netInfo.isConnected()) {
                WIFI = true;
            }
            if (netInfo.getTypeName().equalsIgnoreCase("MOBILE") && netInfo.isConnected()) {
                MOBILE = true;
            }
        }


        if (WIFI == true) {// &&
                //SharedPrefSingletoon.getInstance(context).getBoolean(SharedPrefSingletoon.Key.PREFER_WIFI)) {

            WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

            // Convert little-endian to big-endianif needed
            if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
                ipAddress = Integer.reverseBytes(ipAddress);
            }

            byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();
            try {
                ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
                return ipAddressString;
            } catch (UnknownHostException ex) {
                Log.e("WI-FI IP", "Unable to get host address.");
            }
        }

        if (MOBILE == true) {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                    NetworkInterface networkinterface = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = networkinterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()) {
                            return inetAddress.getHostAddress().toString();
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e("Current IP", ex.toString());
            }
        }
        throw new NetworkErrorException("retrieving local IP address error");
    }

}
