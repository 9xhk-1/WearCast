package com.wearcast.app.cast;

import java.io.Serializable;

/** A DLNA/UPnP renderer discovered on the local network. */
public class DlnaDevice implements Serializable {
    public String uuid;
    public String friendlyName;
    public String location;
    public String avTransportControlUrl;
    public String renderingControlControlUrl;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DlnaDevice)) return false;
        DlnaDevice other = (DlnaDevice) obj;
        return uuid != null && uuid.equals(other.uuid);
    }

    @Override
    public int hashCode() {
        return uuid == null ? 0 : uuid.hashCode();
    }
}
