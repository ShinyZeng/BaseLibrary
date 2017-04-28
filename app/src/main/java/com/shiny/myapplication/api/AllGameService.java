/**
 * Copyright (C) © 2014 深圳市掌玩网络技术有限公司
 * MyApplication
 * AllGameService.java
 */
package com.shiny.myapplication.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * @author Administrator
 * @since 2017/4/5 18:14
 * @version 1.0
 *          <p>
 *          <strong>Features draft description.主要功能介绍</strong>
 *          </p>
 */
public interface AllGameService {
    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================
    // http://cmsns.cmgame.com/arena/getArenaGameList.do?
    // userID=3208442&token=ZRBLQURUQRSEONRUCIDPKOJYESFHARIK&pageNo=0&channel=Migu&pageSize=20&pid=13&versionName=1.8.1.0&version=1810
    @GET("arena/getArenaGameList.do")
    Call<String> getString(@Query("userID")int pUserId,@Query("token")String pToken,
                           @Query("pageNo")int pPageNo,@Query("channel")String pChannel,
                           @Query("pageSize")int pPageSize,@Query("pid")int pPid,
                           @Query("versionName")String pVersionName,@Query("version")int pVersion);
    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter &amp; Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

}
