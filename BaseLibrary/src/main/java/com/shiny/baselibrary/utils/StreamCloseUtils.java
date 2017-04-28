/**
 * Copyright (C) © 2014 深圳市掌玩网络技术有限公司
 * MyApplication
 * StreamCloseUtils.java
 */
package com.shiny.baselibrary.utils;


import android.util.Log;

import java.io.Closeable;
import java.io.IOException;


/**
 * @author shiny.zeng
 * @since 2017/4/5 15:45
 * @version 1.0
 * <p><strong>Features draft description.主要功能介绍</strong></p>
 */
public class StreamCloseUtils {
    // ===========================================================
    // Constants
    // ===========================================================


    // ===========================================================
    // Fields
    // ===========================================================


    // ===========================================================
    // Constructors
    // ===========================================================


    // ===========================================================
    // Getter &amp; Setter
    // ===========================================================


    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================
    /**
     * 通用close
     *
     * @param closeable
     */
    public static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            Log.w("","close.error " + e.toString());
        }
    }


    // ===========================================================
    // Methods
    // ===========================================================


    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

}
