package com.wearcast.app.model;

import java.util.List;

public class PlayUrlResult {
    public int quality;
    public List<Integer> acceptQuality;
    public List<String> acceptDescription;
    public List<Durl> durl;

    public static class Durl {
        public long order;
        public long length;
        public String url;
    }
}
