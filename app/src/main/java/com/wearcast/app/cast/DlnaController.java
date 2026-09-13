package com.wearcast.app.cast;

import android.util.Log;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import com.wearcast.app.network.Callback2;

/** Sends AVTransport / RenderingControl SOAP actions to a DlnaDevice (play/pause/seek/volume). */
public class DlnaController {

    private static final String TAG = "DlnaController";
    private static final MediaType SOAP_MEDIA_TYPE = MediaType.parse("text/xml; charset=\"utf-8\"");
    private static final String AVT_SERVICE = "urn:schemas-upnp-org:service:AVTransport:1";
    private static final String RC_SERVICE = "urn:schemas-upnp-org:service:RenderingControl:1";

    private final OkHttpClient http = new OkHttpClient();
    private final DlnaDevice device;

    public DlnaController(DlnaDevice device) {
        this.device = device;
    }

    public void setUrlAndPlay(String mediaUrl, String title, final Callback2<Void> callback) {
        String metadata = buildDidl(mediaUrl, title);
        String body = "<InstanceID>0</InstanceID>" +
                "<CurrentURI>" + escapeXml(mediaUrl) + "</CurrentURI>" +
                "<CurrentURIMetaData>" + escapeXml(metadata) + "</CurrentURIMetaData>";
        sendAction(device.avTransportControlUrl, AVT_SERVICE, "SetAVTransportURI", body, new Callback2<String>() {
            @Override
            public void onSuccess(String result) {
                play(callback);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }

    public void play(final Callback2<Void> callback) {
        sendAction(device.avTransportControlUrl, AVT_SERVICE, "Play",
                "<InstanceID>0</InstanceID><Speed>1</Speed>", voidCallback(callback));
    }

    public void pause(final Callback2<Void> callback) {
        sendAction(device.avTransportControlUrl, AVT_SERVICE, "Pause",
                "<InstanceID>0</InstanceID>", voidCallback(callback));
    }

    public void stop(final Callback2<Void> callback) {
        sendAction(device.avTransportControlUrl, AVT_SERVICE, "Stop",
                "<InstanceID>0</InstanceID>", voidCallback(callback));
    }

    /** relativeSeconds may be negative for rewind. */
    public void seekRelative(String targetHms, final Callback2<Void> callback) {
        sendAction(device.avTransportControlUrl, AVT_SERVICE, "Seek",
                "<InstanceID>0</InstanceID><Unit>REL_TIME</Unit><Target>" + targetHms + "</Target>",
                voidCallback(callback));
    }

    public void setVolume(int volumePercent, final Callback2<Void> callback) {
        if (device.renderingControlControlUrl == null) {
            callback.onError("device does not support volume control");
            return;
        }
        sendAction(device.renderingControlControlUrl, RC_SERVICE, "SetVolume",
                "<InstanceID>0</InstanceID><Channel>Master</Channel><DesiredVolume>" + volumePercent + "</DesiredVolume>",
                voidCallback(callback));
    }

    public void getPositionInfo(final Callback2<PositionInfo> callback) {
        sendAction(device.avTransportControlUrl, AVT_SERVICE, "GetPositionInfo",
                "<InstanceID>0</InstanceID>", new Callback2<String>() {
                    @Override
                    public void onSuccess(String xml) {
                        PositionInfo info = new PositionInfo();
                        info.relTime = extractTag(xml, "RelTime");
                        info.trackDuration = extractTag(xml, "TrackDuration");
                        callback.onSuccess(info);
                    }

                    @Override
                    public void onError(String message) {
                        callback.onError(message);
                    }
                });
    }

    public static class PositionInfo {
        public String relTime;
        public String trackDuration;
    }

    private Callback2<String> voidCallback(final Callback2<Void> callback) {
        return new Callback2<String>() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(null);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        };
    }

    private void sendAction(String controlUrl, String serviceType, String action, String argsXml, final Callback2<String> callback) {
        if (controlUrl == null) {
            callback.onError("missing control url");
            return;
        }
        String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
                "s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">" +
                "<s:Body><u:" + action + " xmlns:u=\"" + serviceType + "\">" + argsXml + "</u:" + action + ">" +
                "</s:Body></s:Envelope>";

        RequestBody requestBody = RequestBody.create(soapBody, SOAP_MEDIA_TYPE);
        Request request = new Request.Builder()
                .url(controlUrl)
                .addHeader("SOAPACTION", "\"" + serviceType + "#" + action + "\"")
                .addHeader("Content-Type", "text/xml; charset=\"utf-8\"")
                .post(requestBody)
                .build();

        http.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(okhttp3.Call call, Response response) throws IOException {
                try {
                    String body = response.body() != null ? response.body().string() : "";
                    if (!response.isSuccessful()) {
                        Log.w(TAG, action + " failed: " + response.code() + " " + body);
                        callback.onError("HTTP " + response.code());
                        return;
                    }
                    callback.onSuccess(body);
                } finally {
                    response.close();
                }
            }
        });
    }

    private static String extractTag(String xml, String tag) {
        if (xml == null) return null;
        Pattern pattern = Pattern.compile("<" + tag + ">([^<]*)</" + tag + ">");
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String buildDidl(String mediaUrl, String title) {
        return "<DIDL-Lite xmlns=\"urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
                "xmlns:upnp=\"urn:schemas-upnp-org:metadata-1-0/upnp/\">" +
                "<item id=\"0\" parentID=\"-1\" restricted=\"1\">" +
                "<dc:title>" + escapeXml(title) + "</dc:title>" +
                "<upnp:class>object.item.videoItem</upnp:class>" +
                "<res protocolInfo=\"http-get:*:video/mp4:*\">" + escapeXml(mediaUrl) + "</res>" +
                "</item></DIDL-Lite>";
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
