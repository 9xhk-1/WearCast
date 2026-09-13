package com.wearcast.app.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import com.wearcast.app.model.Comment;
import com.wearcast.app.model.CommentContent;
import com.wearcast.app.model.CommentMember;
import com.wearcast.app.model.FavFolder;
import com.wearcast.app.model.PlayUrlResult;
import com.wearcast.app.model.VideoDetail;
import com.wearcast.app.model.VideoItem;
import com.wearcast.app.model.VideoOwner;
import com.wearcast.app.model.VideoStat;

import retrofit2.Call;
import retrofit2.Response;

/** Pulls search/recommend/detail/comment/favorite data out of bilibili's raw JSON payloads. */
public class VideoRepository {

    private final BiliApiService service = ApiClient.get().service();

    public void search(String keyword, int page, final Callback2<List<VideoItem>> callback) {
        service.search(keyword, "video", page).enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                List<VideoItem> items = new ArrayList<>();
                JsonObject data = body.data.getAsJsonObject();
                if (data.has("result") && data.get("result").isJsonArray()) {
                    for (JsonElement el : data.getAsJsonArray("result")) {
                        JsonObject o = el.getAsJsonObject();
                        VideoItem item = new VideoItem();
                        item.bvid = getStr(o, "bvid");
                        item.aid = getLong(o, "aid");
                        item.title = getStr(o, "title");
                        item.author = getStr(o, "author");
                        item.cover = fixUrl(getStr(o, "pic"));
                        item.playCount = getLong(o, "play");
                        item.durationText = getStr(o, "duration");
                        item.description = getStr(o, "description");
                        items.add(item);
                    }
                }
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void recommend(int freshIndex, final Callback2<List<VideoItem>> callback) {
        service.getRecommend(12, freshIndex).enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                List<VideoItem> items = new ArrayList<>();
                JsonObject data = body.data.getAsJsonObject();
                if (data.has("item") && data.get("item").isJsonArray()) {
                    for (JsonElement el : data.getAsJsonArray("item")) {
                        JsonObject o = el.getAsJsonObject();
                        if (!o.has("bvid")) continue;
                        VideoItem item = new VideoItem();
                        item.bvid = getStr(o, "bvid");
                        item.aid = getLong(o, "aid");
                        item.title = getStr(o, "title");
                        if (o.has("owner")) {
                            item.author = getStr(o.getAsJsonObject("owner"), "name");
                        }
                        item.cover = fixUrl(getStr(o, "pic"));
                        if (o.has("stat")) {
                            item.playCount = getLong(o.getAsJsonObject("stat"), "view");
                        }
                        items.add(item);
                    }
                }
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getFavFolders(long mid, final Callback2<List<FavFolder>> callback) {
        service.getFavFolders(mid).enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                List<FavFolder> folders = new ArrayList<>();
                JsonElement dataEl = body.data;
                JsonArray arr = dataEl.isJsonArray() ? dataEl.getAsJsonArray() : null;
                if (arr != null) {
                    for (JsonElement el : arr) {
                        JsonObject o = el.getAsJsonObject();
                        FavFolder f = new FavFolder();
                        f.id = getLong(o, "id");
                        f.fid = getLong(o, "fid");
                        f.mid = getLong(o, "mid");
                        f.title = getStr(o, "title");
                        f.mediaCount = (int) getLong(o, "media_count");
                        folders.add(f);
                    }
                }
                callback.onSuccess(folders);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getFavResources(long mediaId, int page, final Callback2<List<VideoItem>> callback) {
        service.getFavResources(mediaId, page, 20, "web").enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                List<VideoItem> items = new ArrayList<>();
                JsonObject data = body.data.getAsJsonObject();
                if (data.has("medias") && data.get("medias").isJsonArray()) {
                    for (JsonElement el : data.getAsJsonArray("medias")) {
                        JsonObject o = el.getAsJsonObject();
                        VideoItem item = new VideoItem();
                        item.bvid = getStr(o, "bvid");
                        item.aid = getLong(o, "id");
                        item.title = getStr(o, "title");
                        if (o.has("upper")) {
                            item.author = getStr(o.getAsJsonObject("upper"), "name");
                        }
                        item.cover = fixUrl(getStr(o, "cover"));
                        if (o.has("cnt_info")) {
                            item.playCount = getLong(o.getAsJsonObject("cnt_info"), "play");
                        }
                        items.add(item);
                    }
                }
                callback.onSuccess(items);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getVideoDetail(String bvid, final Callback2<VideoDetail> callback) {
        service.getVideoDetail(bvid).enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                JsonObject o = body.data.getAsJsonObject();
                VideoDetail detail = new VideoDetail();
                detail.bvid = getStr(o, "bvid");
                detail.aid = getLong(o, "aid");
                detail.cid = getLong(o, "cid");
                detail.title = getStr(o, "title");
                detail.pic = fixUrl(getStr(o, "pic"));
                detail.desc = getStr(o, "desc");
                detail.duration = getLong(o, "duration");
                detail.pubdate = getLong(o, "pubdate");
                if (o.has("owner")) {
                    JsonObject ow = o.getAsJsonObject("owner");
                    VideoOwner owner = new VideoOwner();
                    owner.mid = getLong(ow, "mid");
                    owner.name = getStr(ow, "name");
                    owner.face = fixUrl(getStr(ow, "face"));
                    detail.owner = owner;
                }
                if (o.has("stat")) {
                    JsonObject st = o.getAsJsonObject("stat");
                    VideoStat stat = new VideoStat();
                    stat.view = getLong(st, "view");
                    stat.danmaku = getLong(st, "danmaku");
                    stat.reply = getLong(st, "reply");
                    stat.favorite = getLong(st, "favorite");
                    stat.coin = getLong(st, "coin");
                    stat.share = getLong(st, "share");
                    stat.like = getLong(st, "like");
                    detail.stat = stat;
                }
                callback.onSuccess(detail);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getComments(long oid, int page, final Callback2<List<Comment>> callback) {
        service.getComments(1, oid, page).enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                List<Comment> comments = new ArrayList<>();
                JsonObject data = body.data.getAsJsonObject();
                if (data.has("replies") && data.get("replies").isJsonArray()) {
                    for (JsonElement el : data.getAsJsonArray("replies")) {
                        JsonObject o = el.getAsJsonObject();
                        Comment c = new Comment();
                        c.rpid = getLong(o, "rpid");
                        c.like = getLong(o, "like");
                        c.ctime = getLong(o, "ctime");
                        if (o.has("member")) {
                            JsonObject m = o.getAsJsonObject("member");
                            CommentMember member = new CommentMember();
                            member.uname = getStr(m, "uname");
                            member.avatarUrl = fixUrl(getStr(m, "avatar"));
                            c.member = member;
                        }
                        if (o.has("content")) {
                            CommentContent content = new CommentContent();
                            content.message = getStr(o.getAsJsonObject("content"), "message");
                            c.content = content;
                        }
                        comments.add(c);
                    }
                }
                callback.onSuccess(comments);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    public void getPlayUrl(String bvid, long cid, int quality, final Callback2<PlayUrlResult> callback) {
        service.getPlayUrl(bvid, cid, quality, 0, 0).enqueue(new retrofit2.Callback<BiliRawResponse>() {
            @Override
            public void onResponse(Call<BiliRawResponse> call, Response<BiliRawResponse> response) {
                BiliRawResponse body = response.body();
                if (body == null || !body.isSuccess() || body.data == null) {
                    callback.onError(body != null ? body.message : "empty response");
                    return;
                }
                JsonObject o = body.data.getAsJsonObject();
                PlayUrlResult result = new PlayUrlResult();
                result.quality = (int) getLong(o, "quality");
                result.acceptQuality = new ArrayList<>();
                result.acceptDescription = new ArrayList<>();
                if (o.has("accept_quality")) {
                    for (JsonElement el : o.getAsJsonArray("accept_quality")) {
                        result.acceptQuality.add(el.getAsInt());
                    }
                }
                if (o.has("accept_description")) {
                    for (JsonElement el : o.getAsJsonArray("accept_description")) {
                        result.acceptDescription.add(el.getAsString());
                    }
                }
                result.durl = new ArrayList<>();
                if (o.has("durl")) {
                    for (JsonElement el : o.getAsJsonArray("durl")) {
                        JsonObject d = el.getAsJsonObject();
                        PlayUrlResult.Durl durl = new PlayUrlResult.Durl();
                        durl.order = getLong(d, "order");
                        durl.length = getLong(d, "length");
                        durl.url = getStr(d, "url");
                        result.durl.add(durl);
                    }
                }
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(Call<BiliRawResponse> call, Throwable t) {
                callback.onError(t.getMessage());
            }
        });
    }

    private static String getStr(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : null;
    }

    private static long getLong(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsLong() : 0L;
    }

    private static String fixUrl(String url) {
        if (url == null) return null;
        if (url.startsWith("//")) return "https:" + url;
        return url;
    }
}
