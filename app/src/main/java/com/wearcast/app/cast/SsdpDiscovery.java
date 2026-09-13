package com.wearcast.app.cast;

import android.net.wifi.WifiManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Minimal SSDP client: multicasts M-SEARCH for MediaRenderer devices and fetches/parses each
 * device description document to resolve the AVTransport / RenderingControl control URLs.
 */
public class SsdpDiscovery {

    private static final String TAG = "SsdpDiscovery";
    private static final String MULTICAST_ADDRESS = "239.255.255.250";
    private static final int MULTICAST_PORT = 1900;
    private static final String SEARCH_TARGET = "urn:schemas-upnp-org:device:MediaRenderer:1";

    public interface Listener {
        void onDeviceFound(DlnaDevice device);
        void onFinished();
    }

    private final OkHttpClient http = new OkHttpClient();
    private volatile boolean cancelled = false;

    public void cancel() {
        cancelled = true;
    }

    public void discover(WifiManager wifiManager, final Listener listener, int timeoutMs) {
        WifiManager.MulticastLock lock = wifiManager != null ? wifiManager.createMulticastLock("wearcast-ssdp") : null;
        if (lock != null) {
            lock.setReferenceCounted(true);
            lock.acquire();
        }
        Set<String> seen = new HashSet<>();
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(timeoutMs);
            String message = "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: " + MULTICAST_ADDRESS + ":" + MULTICAST_PORT + "\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 3\r\n" +
                    "ST: " + SEARCH_TARGET + "\r\n\r\n";
            byte[] buffer = message.getBytes();
            InetAddress address = InetAddress.getByName(MULTICAST_ADDRESS);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, MULTICAST_PORT);
            socket.send(packet);

            long deadline = System.currentTimeMillis() + timeoutMs;
            byte[] recvBuffer = new byte[4096];
            while (!cancelled && System.currentTimeMillis() < deadline) {
                try {
                    DatagramPacket response = new DatagramPacket(recvBuffer, recvBuffer.length);
                    socket.receive(response);
                    String data = new String(response.getData(), 0, response.getLength());
                    String location = extractHeader(data, "LOCATION");
                    if (location == null) continue;
                    if (!seen.add(location)) continue;
                    DlnaDevice device = fetchDeviceDescription(location);
                    if (device != null) {
                        listener.onDeviceFound(device);
                    }
                } catch (java.net.SocketTimeoutException timeout) {
                    break;
                } catch (IOException e) {
                    Log.w(TAG, "receive error", e);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "discover failed", e);
        } finally {
            if (socket != null) socket.close();
            if (lock != null && lock.isHeld()) lock.release();
            listener.onFinished();
        }
    }

    private static String extractHeader(String data, String header) {
        String[] lines = data.split("\r\n");
        for (String line : lines) {
            int idx = line.indexOf(':');
            if (idx > 0 && line.substring(0, idx).trim().equalsIgnoreCase(header)) {
                return line.substring(idx + 1).trim();
            }
        }
        return null;
    }

    private DlnaDevice fetchDeviceDescription(String location) {
        try {
            Request request = new Request.Builder().url(location).get().build();
            try (Response response = http.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) return null;
                InputStream stream = response.body().byteStream();
                return parseDescription(stream, location);
            }
        } catch (Exception e) {
            Log.w(TAG, "fetch description failed: " + location, e);
            return null;
        }
    }

    private DlnaDevice parseDescription(InputStream stream, String location) throws Exception {
        URL base = new URL(location);
        String baseUrl = base.getProtocol() + "://" + base.getHost() + ":" + (base.getPort() == -1 ? base.getDefaultPort() : base.getPort());

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(stream, "UTF-8");

        DlnaDevice device = new DlnaDevice();
        device.location = location;
        String currentTag = null;
        String currentServiceType = null;
        String currentControlUrl = null;

        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                currentTag = parser.getName();
                if ("service".equals(currentTag)) {
                    currentServiceType = null;
                    currentControlUrl = null;
                }
            } else if (event == XmlPullParser.TEXT) {
                String text = parser.getText();
                if (text != null) text = text.trim();
                if (!isEmpty(text)) {
                    if ("friendlyName".equals(currentTag) && device.friendlyName == null) {
                        device.friendlyName = text;
                    } else if ("UDN".equals(currentTag) && device.uuid == null) {
                        device.uuid = text;
                    } else if ("serviceType".equals(currentTag)) {
                        currentServiceType = text;
                    } else if ("controlURL".equals(currentTag)) {
                        currentControlUrl = text;
                        if (currentServiceType != null && currentServiceType.contains("AVTransport")) {
                            device.avTransportControlUrl = resolveUrl(baseUrl, currentControlUrl);
                        } else if (currentServiceType != null && currentServiceType.contains("RenderingControl")) {
                            device.renderingControlControlUrl = resolveUrl(baseUrl, currentControlUrl);
                        }
                    }
                }
            }
            event = parser.next();
        }
        if (device.uuid == null) device.uuid = location;
        if (device.friendlyName == null) device.friendlyName = base.getHost();
        return device.avTransportControlUrl != null ? device : null;
    }

    private static String resolveUrl(String baseUrl, String path) {
        if (path == null) return null;
        if (path.startsWith("http")) return path;
        if (!path.startsWith("/")) path = "/" + path;
        return baseUrl + path;
    }

    private static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }
}
