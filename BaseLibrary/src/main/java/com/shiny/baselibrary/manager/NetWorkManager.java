package com.shiny.baselibrary.manager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

//@formatter:off
/**
 * @author Jin BinBin
 * @Create at 2014-10-11 下午4:11:45
 * @Version 1.0
 * <p><strong>Features draft description.管理网络连接情况</strong></p>
 */
//@formatter:on

public class NetWorkManager {

    // ===========================================================
    // Constants
    // +==========================================================

    private static final String          ACTION_PING_BAIDU     = "ping.baidu";
    private static final String          INTENT_HAS_NETWORK    = "ping_result";

    private static final String          TAG                   = "NetWorkManager";
    /**
     * 超时毫秒值， 测试过几次是3秒
     */
    private static final long            PING_TIMEOUT_MILLIS   = 4000;

    /**
     * 无网络
     */
    public static final int              NETWORK_TYPE_NONE     = 0;
    /**
     * 有线网络
     */
    public static final int              NETWORK_TYPE_ETHERNET = 1;
    /**
     * wifi网络
     */
    public static final int              NETWORK_TYPE_WIFI     = 2;
    /**
     * 2G网络
     */
    public static final int              NETWORK_TYPE_2G       = 3;
    /**
     * 3G网络
     */
    public static final int              NETWORK_TYPE_3G       = 4;
    /**
     * 4G网络
     */
    public static final int              NETWORK_TYPE_4G       = 5;
    /**
     * 拨号网络
     */
    public static final int              NETWORK_TYPE_PPPOE    = 6;
    /**
     * 网络无效
     */
    public static final int              NETWORK_TYPE_INVALID  = 7;

    public static final int              PASSWORD_TYPE_NO      = 0;
    public static final int              PASSWORD_TYPE_WEP     = 1;
    public static final int              PASSWORD_TYPE_WPA     = 2;

    private static final Object          mLock                 = new Object();

    public static final int              NO_NETWORK            = 0x1001;

    // TODO 代码在+-号之间编写
    // -==========================================================

    // ===========================================================
    // Fields
    // +==========================================================

    private static NetWorkManager        mNetWorkManager;

    private List<INetWorkChangeListener> mNetWorkChangeListeners;

    private ConnectivityManager          mConnectivityManager;

    private WifiManager                  mWifiManager;

    private NetWorkBroadcastReceiver     mNetWorkBroadcastReceiver;

    private Context                      mContext;

    private int                          mCurrentNetWorkType   = NETWORK_TYPE_NONE;

    /*
     * 2016年4月6日16:50:15 添加ping，验证连接上wifi，但是没网
     */
    private PingThread                   mPingThread;
                                         // -==========================================================

    // ===========================================================
    // Constructors
    // +==========================================================

    private NetWorkManager() {
    }

    // -==========================================================

    // ===========================================================
    // Getter & Setter
    // +==========================================================

    public static NetWorkManager getInstance() {
        synchronized (mLock) {
            if (mNetWorkManager == null) {
                mNetWorkManager = new NetWorkManager();
            }
        }
        return mNetWorkManager;
    }

    /**
     * 初始化
     * 
     * @param pContext
     */
    public void init(Context pContext) {
        if (mContext == null) {
            mContext = pContext.getApplicationContext();
            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            registNetWorkBroadcastReceiver(mContext);
            mCurrentNetWorkType = getNetworkType();
        }
    }

    public void uninit() {
        if (mContext != null) {
            unRegisterNetWorkBroadcastReceiver(mContext);
            clearListeners();
        }
        stopCheckNetworkUsable();
        mContext = null;
    }

    /**
     * 清空监听器
     */
    private synchronized void clearListeners() {
        if (mNetWorkChangeListeners != null) {
            mNetWorkChangeListeners.clear();
        }
    }

    /**
     * 反注册网络广播接受者
     *
     * @param pContext
     */
    private void unRegisterNetWorkBroadcastReceiver(Context pContext) {
        if (mNetWorkBroadcastReceiver != null) {
            pContext.getApplicationContext().unregisterReceiver(mNetWorkBroadcastReceiver);
            mNetWorkBroadcastReceiver = null;
        }
    }

    /**
     * 注册网络广播接受者
     *
     * @param pContext
     */
    private void registNetWorkBroadcastReceiver(Context pContext) {
        mNetWorkBroadcastReceiver = new NetWorkBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(ACTION_PING_BAIDU);
        pContext.getApplicationContext().registerReceiver(mNetWorkBroadcastReceiver, intentFilter);
    }

    // -==========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // +==========================================================

    // -==========================================================

    // ===========================================================
    // Methods
    // +==========================================================
    /**
     * 更新网络
     * 
     * @param type
     */
    private void onNetWorkChanged(int type) {
        Log.d(TAG,"onNetWorkChanged:" + hasNetwork());
        // if (type != NETWORK_TYPE_NONE) {
        // startCheckNetworkUsable();
        // }else{
        // stopCheckNetworkUsable();
        // }
        if (type != mCurrentNetWorkType) {
            mCurrentNetWorkType = type;
            notifNetworkChange(mCurrentNetWorkType);
        }
    }

    /**
     * 开始网络可用的检测
     */
    private void startCheckNetworkUsable() {
        stopCheckNetworkUsable();
        mPingThread = new PingThread();
        mPingThread.start();
    }

    /**
     * 停止网络可用的检测
     */
    private void stopCheckNetworkUsable() {
        if (mPingThread != null) {
            mPingThread.stopPing();
            mPingThread = null;
        }
    }

    /**
     * 切换wifi的时候,ping百度
     * 
     * @return 0 表示成功 ,1 表示失败 -1表示异常
     */
    private int pingBaidu(String str) {
        Process p;
        try {
            // ping -c 3 -w 1000 中 ，-c 是指ping的次数 3是指ping 3次 ，-t 1000
            // 以秒为单位指定超时间隔，是指超时时间为100秒
            Log.d(TAG,"PING START   ");
            p = Runtime.getRuntime().exec("ping -c 3 -t 200 " + str);
            int status = p.waitFor();
            Log.d(TAG,"PING status:" + status); // status状态码：0 success ；1 缺少权限
            return status;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * 通知网络更新
     *
     * @param pNetwokType
     */
    private synchronized void notifNetworkChange(int pNetwokType) {
        if (mNetWorkChangeListeners != null && !mNetWorkChangeListeners.isEmpty()) {
            for (int i = 0, size = mNetWorkChangeListeners.size(); i < size; i++) {
                mNetWorkChangeListeners.get(i).onNetWorkChanged(pNetwokType);
            }
        }
    }

    /**
     * 获取当前网络类型
     * 
     * @return
     */
    public int getCurrentNetWorkType() {
        return mCurrentNetWorkType;
    }

    /**
     * 获取wifi信息
     * 
     * @return
     */
    public WifiInfo getWifiInfo() {
        if (mWifiManager != null) {
            return mWifiManager.getConnectionInfo();
        }
        return null;
    }

    /**
     * 获取网络信息
     * 
     * @return
     */
    public NetworkInfo getNetworkInfo() {
        if (mConnectivityManager != null) {
            return mConnectivityManager.getActiveNetworkInfo();
        }
        return null;
    }

    /**
     * 获取ip地址
     * 
     * @return
     */
    public int getWifiIpAddress() {
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo != null) {
            return wifiInfo.getIpAddress();
        }
        return 0;
    }

    /**
     * 获取网络id地址
     * 
     * @return
     * @throws SocketException
     */
    public String getNetworkIpAddress() throws SocketException {
        String ipAddress = "0.0.0.0";
        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        while (netInterfaces.hasMoreElements()) {
            NetworkInterface intf = netInterfaces.nextElement();
            if (intf.getName().toLowerCase(Locale.getDefault()).equals("eth0")) {
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        ipAddress = inetAddress.getHostAddress().toString();
                        if (!ipAddress.contains("::")) {// ipV6的地址
                            break;
                        }
                    }
                }
            }
        }
        return ipAddress;
    }

    /**
     * 获取wifi的ip
     *
     * @return
     */
    public String getWifiIp() {
        int ip = getWifiIpAddress();
        if (ip != 0) {
            return getIpFromIntSigned(ip);
        }
        return "";
    }

    /**
     *
     * @param pIp
     * @return
     */
    public static String getIpFromIntSigned(int pIp) {
        String ip = "";
        for (int k = 0; k < 4; k++) {
            ip = ip + ((pIp >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    /**
     *
     * @param pIp
     * @return
     */
    public static String getIpFromLongUnsigned(long pIp) {
        String ip = "";
        for (int k = 3; k > -1; k--) {
            ip = ip + ((pIp >> k * 8) & 0xFF) + ".";
        }
        return ip.substring(0, ip.length() - 1);
    }

    /**
     *
     * @param pIpaddr
     * @return
     */
    public static long getUnsignedLongFromIp(String pIpaddr) {
        String[] a = pIpaddr.split("\\.");
        return (Integer.parseInt(a[0]) * 16777216 + Integer.parseInt(a[1]) * 65536 + Integer.parseInt(a[2]) * 256
            + Integer.parseInt(a[3]));
    }

    /**
     * 注册网络改变监听器
     * 
     * @param l
     */
    public synchronized void registerNetWorkChangeListener(INetWorkChangeListener l) {
        if (mNetWorkChangeListeners == null) {
            mNetWorkChangeListeners = new ArrayList<INetWorkChangeListener>();
        }
        mNetWorkChangeListeners.add(l);
        l.onNetWorkChanged(getNetworkType());
    }

    /**
     * 注销网络改变监听器
     * 
     * @param l
     */
    public synchronized void unregisterNetWorkChangeListener(INetWorkChangeListener l) {
        if (mNetWorkChangeListeners != null && !mNetWorkChangeListeners.isEmpty()
            && mNetWorkChangeListeners.contains(l)) {
            mNetWorkChangeListeners.remove(l);
        }
    }

    /**
     * 网络是否带劲
     *
     * @return true为有网络可用{@link NetworkUtils#NETWORK_TYPE_ETHERNET},
     */
    public boolean isNetworkGood() {
        return mCurrentNetWorkType == NETWORK_TYPE_WIFI || mCurrentNetWorkType == NETWORK_TYPE_ETHERNET;
    }

    /**
     * 当前网络是否为wifi
     *
     * @return
     */
    public boolean isWifiNetwork() {
        return mCurrentNetWorkType == NETWORK_TYPE_WIFI;
    }

    /**
     * 是否存在网络连接
     *
     * @return 有网络时返回true，其他返回false
     */
    public boolean hasNetwork() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null) {
                return false;
            } else {
                return info.isAvailable();
            }
        }
        return false;
    }

    /**
     * 当前网络是可用
     * 
     * @return
     */
    public boolean isNetworkAvailable() {
        if (mCurrentNetWorkType == NETWORK_TYPE_NONE || mCurrentNetWorkType == NETWORK_TYPE_INVALID) {
            return false;
        }
        return true;
    }

    /**
     * 获取当前网络名称
     * 
     * @return 网络名称
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public String getNetworkName() {
        int type = mCurrentNetWorkType;
        switch (type) {
            case NETWORK_TYPE_WIFI:
                WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
                if (null != wifiInfo) {
                    String ssid = wifiInfo.getSSID();
                    if (!TextUtils.isEmpty(ssid)) {
                        return ssid.replace("\"", "");
                    } else {
                        return "";
                    }
                }
                return "WIFI";
            case NETWORK_TYPE_ETHERNET:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                    NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
                    if (networkInfo != null && networkInfo.isConnected()) {
                        return networkInfo.getTypeName();
                    }
                }
                return "Ethernet";
            case NETWORK_TYPE_2G:
                return "2G";
            case NETWORK_TYPE_3G:
                return "3G";
            case NETWORK_TYPE_4G:
                return "4G";
        }
        return "Network Not Available";
    }

    /**
     * 获取当前网络类型
     * 
     * @return 网络类型, 值为{@link #NETWORK_TYPE_NONE},
     *         {@link #NETWORK_TYPE_ETHERNET}, {@link #NETWORK_TYPE_WIFI},
     *         {@link #NETWORK_TYPE_2G}, {@link #NETWORK_TYPE_3G},
     *         {@link #NETWORK_TYPE_4G} 中的一个
     */
    @SuppressLint("InlinedApi")
    private int getNetworkType() {

        NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobile = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            NetworkInfo ethernet = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            if (ethernet != null && ethernet.isConnected()) {
                return NETWORK_TYPE_ETHERNET;
            }
        }

        if (wifi != null && wifi.isConnected()) {
            return NETWORK_TYPE_WIFI;

        } else if (mobile != null && mobile.isConnected()) {
            TelephonyManager telephonyManager = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            int mobileNetType = telephonyManager.getNetworkType();
            switch (mobileNetType) {
                case TelephonyManager.NETWORK_TYPE_UMTS:
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                case TelephonyManager.NETWORK_TYPE_HSPA:
                case TelephonyManager.NETWORK_TYPE_EVDO_B:
                case TelephonyManager.NETWORK_TYPE_EHRPD:
                case TelephonyManager.NETWORK_TYPE_HSPAP:
                    return NETWORK_TYPE_3G;

                case TelephonyManager.NETWORK_TYPE_LTE:
                    return NETWORK_TYPE_4G;

                case TelephonyManager.NETWORK_TYPE_GPRS:
                case TelephonyManager.NETWORK_TYPE_EDGE:
                case TelephonyManager.NETWORK_TYPE_CDMA:
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                case TelephonyManager.NETWORK_TYPE_IDEN:
                default:
                    return NETWORK_TYPE_2G;
            }
        }
        return NETWORK_TYPE_NONE;
    }

    /**
     * 断开wifi
     *
     * @param ssid
     * @return
     */
    public boolean disconnectWifi(String ssid) {
        if (hasNetwork()) {
            List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
            for (WifiConfiguration configuration : list) {
                if (configuration.SSID.equals("\"" + ssid + "\"")) {
                    mWifiManager.disableNetwork(configuration.networkId);
                    return mWifiManager.disconnect();
                }
            }
        }
        return false;
    }

    /**
     * 连接wifi
     *
     * @param pSsid
     * @return
     */
    public boolean connectWifi(String pSsid) {
        List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : list) {
            if (configuration.SSID.equals("\"" + pSsid + "\"")) {
                configuration.status = WifiConfiguration.Status.ENABLED;
                return mWifiManager.enableNetwork(mWifiManager.addNetwork(configuration), true);
            }
        }
        return false;
    }

    /**
     * 连接wifi
     * 
     * @param pSsid
     * @param pPasswordType
     * @param pPassword
     * @return
     */
    public boolean connectWifi(String pSsid, int pPasswordType, String pPassword) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.allowedAuthAlgorithms.clear();
        wifiConfiguration.allowedGroupCiphers.clear();
        wifiConfiguration.allowedKeyManagement.clear();
        wifiConfiguration.allowedPairwiseCiphers.clear();
        wifiConfiguration.allowedProtocols.clear();
        wifiConfiguration.SSID = "\"" + pSsid + "\"";

        List<WifiConfiguration> wifiConfigurationList = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : wifiConfigurationList) {
            if (configuration.SSID.equals("\"" + pSsid + "\"")) {
                mWifiManager.removeNetwork(configuration.networkId);
                break;
            }
        }

        if (pPasswordType == PASSWORD_TYPE_NO) {
            wifiConfiguration.wepKeys[0] = null;
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.wepTxKeyIndex = 0;
        } else if (pPasswordType == PASSWORD_TYPE_WEP) {
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.wepKeys[0] = "\"" + pPassword + "\"";
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiConfiguration.wepTxKeyIndex = 0;
        } else if (pPasswordType == PASSWORD_TYPE_WPA) {
            wifiConfiguration.preSharedKey = "\"" + pPassword + "\"";
            wifiConfiguration.hiddenSSID = true;
            wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
        }

        if (mWifiManager.enableNetwork(mWifiManager.addNetwork(wifiConfiguration), true)) {
            mWifiManager.saveConfiguration();
            return true;
        }
        return false;
    }

    // -==========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // +==========================================================

    private class PingThread extends Thread {
        private boolean mStop = false;

        public void stopPing() {
            this.mStop = true;
        }

        @Override
        public void run() {
            super.run();
            while (!mStop) {
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                // 已经暂停的任务 不在做ping处理
                if (!mStop) {
                    int result = pingBaidu("www.baidu.com");
                    Log.d(TAG,"ping result " + result);
                    boolean hasNetwork = false;
                    if (result == 0) {
                        hasNetwork = true;
                    }
                    Log.d(TAG,"ping result " + hasNetwork);
                    Intent broadcastIntent = new Intent();
                    broadcastIntent.putExtra(INTENT_HAS_NETWORK, hasNetwork);
                    broadcastIntent.setAction(ACTION_PING_BAIDU);
                    mContext.sendBroadcast(broadcastIntent);
                } else {
                    Log.d(TAG,"ping thread stop!");
                }
            }
        }
    }

    //@formatter:off
    /**
     * @author Jin BinBin
     * @Create at 2014-10-11 下午4:19:00
     * @Version 1.0
     * <p><strong>Features draft description.网络状态监听器回调</strong></p>
     */
    //@formatter:on
    public interface INetWorkChangeListener {
        /**
         * 返回 当前的网络类型
         *
         * @param pNetworkType
         *            网络类型, 值为{@link #NETWORK_TYPE_NONE},
         *            {@link #NETWORK_TYPE_ETHERNET}, {@link #NETWORK_TYPE_WIFI}
         *            , {@link #NETWORK_TYPE_2G}, {@link #NETWORK_TYPE_3G},
         *            {@link #NETWORK_TYPE_4G} 中的一个
         */
        public void onNetWorkChanged(int pNetworkType);

    }

    //@formatter:off
    /**
     * @author Jin BinBin
     * @Create at 2014-10-30 上午11:14:01
     * @Version 1.0
     * <p><strong>Features draft description.主要功能介绍</strong></p>
     */
    //@formatter:on
    public class NetWorkBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                onNetWorkChanged(getNetworkType());
            } else if (action.equals(ACTION_PING_BAIDU)) {
                boolean hasNetwork = intent.getBooleanExtra(INTENT_HAS_NETWORK, false);
                Log.d(TAG,"ACTION_PING_BAIDU " + hasNetwork + " " + mCurrentNetWorkType);
                if (!hasNetwork && mCurrentNetWorkType != NETWORK_TYPE_NONE) {
                    onNetWorkChanged(NETWORK_TYPE_INVALID);
                } else {
                    onNetWorkChanged(getNetworkType());
                }
            }
        }
    }

    // -==========================================================
}
