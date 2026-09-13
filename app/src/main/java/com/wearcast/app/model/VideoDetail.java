package com.wearcast.app.model;

import java.util.List;

public class VideoDetail {
    public String bvid;
    public long aid;
    public long cid;
    public String title;
    public String pic;
    public String desc;
    public long duration;
    public long pubdate;
    public VideoOwner owner;
    public VideoStat stat;
    public List<VideoPage> pages;
}
