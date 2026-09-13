package com.wearcast.app.model;

import com.google.gson.annotations.SerializedName;

/** Lightweight representation of a video used in list screens (search / recommend / favorites). */
public class VideoItem {
    @SerializedName(value = "bvid")
    public String bvid;

    @SerializedName(value = "aid")
    public long aid;

    @SerializedName(value = "title")
    public String title;

    @SerializedName(value = "author", alternate = {"owner_name", "name"})
    public String author;

    @SerializedName(value = "pic", alternate = {"cover"})
    public String cover;

    @SerializedName(value = "play", alternate = {"cnt_info_play"})
    public long playCount;

    @SerializedName(value = "duration")
    public String durationText;

    @SerializedName(value = "description", alternate = {"intro"})
    public String description;

    public String cleanTitle() {
        if (title == null) return "";
        return title.replaceAll("<[^>]+>", "");
    }
}
