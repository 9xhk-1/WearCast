package com.wearcast.app.cast;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Remembers the last device the user picked, scoped to the current Wi-Fi network (BSSID), so
 * the app can silently reconnect on the same LAN and only prompt for a device the first time or
 * when the user explicitly asks to switch.
 */
public class CastManager {

    private static final String PREFS = "wearcast_cast";
    private static CastManager instance;

    private final SharedPreferences prefs;
    private final WifiManager wifiManager;
    private DlnaDevice connectedDevice;

    private CastManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    public static synchronized CastManager get(Context context) {
        if (instance == null) {
            instance = new CastManager(context);
        }
        return instance;
    }

    private String networkKey() {
        try {
            WifiInfo info = wifiManager.getConnectionInfo();
            String bssid = info != null ? info.getBSSID() : null;
            return bssid != null ? bssid : "default";
        } catch (SecurityException e) {
            return "default";
        }
    }

    public void rememberDevice(DlnaDevice device) {
        connectedDevice = device;
        prefs.edit()
                .putString(networkKey() + "_uuid", device.uuid)
                .putString(networkKey() + "_name", device.friendlyName)
                .putString(networkKey() + "_location", device.location)
                .putString(networkKey() + "_av", device.avTransportControlUrl)
                .putString(networkKey() + "_rc", device.renderingControlControlUrl)
                .apply();
    }

    public DlnaDevice getRememberedDevice() {
        String key = networkKey();
        String uuid = prefs.getString(key + "_uuid", null);
        if (uuid == null) return null;
        DlnaDevice device = new DlnaDevice();
        device.uuid = uuid;
        device.friendlyName = prefs.getString(key + "_name", null);
        device.location = prefs.getString(key + "_location", null);
        device.avTransportControlUrl = prefs.getString(key + "_av", null);
        device.renderingControlControlUrl = prefs.getString(key + "_rc", null);
        return device;
    }

    public void forgetDevice() {
        String key = networkKey();
        prefs.edit()
                .remove(key + "_uuid")
                .remove(key + "_name")
                .remove(key + "_location")
                .remove(key + "_av")
                .remove(key + "_rc")
                .apply();
        connectedDevice = null;
    }

    public DlnaDevice getConnectedDevice() {
        if (connectedDevice == null) {
            connectedDevice = getRememberedDevice();
        }
        return connectedDevice;
    }

    public void setConnectedDevice(DlnaDevice device) {
        this.connectedDevice = device;
    }
}
