package com.wearcast.app.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;

/** Bilibili endpoints, following the community documented API (reference: xtcqinghe/bac). */
public interface BiliApiService {

    @GET("x/web-interface/nav")
    Call<BiliRawResponse> getNav();

    @GET("x/web-interface/search/type")
    Call<BiliRawResponse> search(@Query("keyword") String keyword,
                                  @Query("search_type") String searchType,
                                  @Query("page") int page);

    @GET("x/web-interface/index/top/rcmd")
    Call<BiliRawResponse> getRecommend(@Query("ps") int pageSize,
                                        @Query("fresh_idx") int freshIndex);

    @GET("x/web-interface/view")
    Call<BiliRawResponse> getVideoDetail(@Query("bvid") String bvid);

    @GET("x/v2/reply")
    Call<BiliRawResponse> getComments(@Query("type") int type,
                                       @Query("oid") long oid,
                                       @Query("pn") int page);

    @GET("x/player/playurl")
    Call<BiliRawResponse> getPlayUrl(@Query("bvid") String bvid,
                                      @Query("cid") long cid,
                                      @Query("qn") int quality,
                                      @Query("fnval") int fnval,
                                      @Query("fourk") int fourk);

    @GET("x/v3/fav/folder/created/list-all")
    Call<BiliRawResponse> getFavFolders(@Query("up_mid") long mid);

    @GET("x/v3/fav/resource/list")
    Call<BiliRawResponse> getFavResources(@Query("media_id") long mediaId,
                                           @Query("pn") int page,
                                           @Query("ps") int pageSize,
                                           @Query("platform") String platform);
}
