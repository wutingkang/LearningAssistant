package zyb.wutingkang.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import zyb.wutingkang.setClassRoomSite.MyLocation;
import zyb.wutingkang.setClassRoomSite.SetClassroomSite;

import static temp.ShareMethod.getSumMinute;
import static zyb.wutingkang.setClassRoomSite.MyLocation.context;

/**
 * Created by King_Tom_user_name on 2017/4/1.
 */

public class InClassRoomQuietService extends Service {
    public static boolean IS_IN_CLASSROOM = false; //初始值为真，确保关闭此服务时，SetQuietService也能开启
    private boolean SERVICE_OPEN_NETWORK = false; //标示是否是服务自己打开网络，如果不是这在使用完网络后不能关闭网络
    private AudioManager audioManager;
    private int primaryRingerMode;
    private final double MAX_CLASSROOM_RADIUS = 80.0;//默认教室最大半径（米），可以添加让用户自选的动能

    @Override
    public IBinder onBind(Intent arg0){
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        //声明一个获取系统音频服务的类的对象
        audioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
        //获取手机之前设置好的铃声模式
        primaryRingerMode = audioManager.getRingerMode();

        //每隔10分钟获取一次当前位置信息，如果是在教室范围则静音，不在则恢复原来的铃声
        //Timer优化参考：http://blog.csdn.net/dj0379/article/details/50877746
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                //一个普通的线程，那么如果此时又需要用到looper，那么必须在run方法的第一行写loop.prepare();
                //参考：http://www.cnblogs.com/xpxpxp2046/archive/2012/04/13/2445395.html
                //      http://blog.csdn.net/lmj623565791/article/details/38377229/
                Looper.prepare();

                //考虑到翘课的情况，，，只要出了教室的范围就恢复铃声，用一个变量提示SetQuietService是否控制手机静音

                //默认半夜时间（23：00-7：00）不执行，省电，也可以考虑添加让用户设定不执行的时间段的功能
                //因为是十分钟执行一次，并不能一定准确撞上07：00，所以不能用 ShareMethod.getTime24()
                if ((getSumMinute() > 7 * 60) && (getSumMinute() < 23 * 60)){//不知道跟7*60用变量表示相比那个效率更高些

                    //判断网络状况，确保获取地理位置前已经开启网络。如果是飞行模式就不打开了，反正也不会有来电铃声，但是闹铃就不好说了
                    if (! isAirplaneModeOn()){

                        if (! isNetworkAvailable()){
                            //不是飞行模式且网络不可用就只是打开移动网络就好了，wifi不一定到处有。
                            //不用接下来马上继续获取地理位置，免得网络还未来得及打开时一直提示网络未连接

                            SERVICE_OPEN_NETWORK = false; //记得初始化
                            if (false == getMobileDataState(context, null)){
                                setMobileData(context, true);
                                SERVICE_OPEN_NETWORK = true;//打开成功与否不好说，但至少是service有打开的动作
                            }

                            //暂停3秒左右,确保网络完全开启
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e) {
                            }
                        }

                        //获取当前位置经纬度并处理
                        MyLocation nowLocation = new MyLocation(InClassRoomQuietService.this, handlerLocation);
                        nowLocation.getLocation(MyLocation.LATITUDE_AND_LONGITUDE);
                    }
                }

                Looper.loop();
            }
        }, 0, 600000);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * @return 是否处于飞行模式
     */
    private boolean isAirplaneModeOn() {
        // 返回值是1时表示处于飞行模式
        int modeIdx = Settings.System.getInt(InClassRoomQuietService.this.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0);
        boolean isEnabled = (modeIdx == 1);
        return isEnabled;
    }


    public Handler handlerLocation = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (msg.what == MyLocation.LATITUDE_AND_LONGITUDE) {
                double classroomLatitude = 0, classroonLongitude = 0;

                //获取教室地理位置，启动此服务前已经确保设置了教室的地理位置
                SharedPreferences spsLocation = SetClassroomSite.spsLocation;
                if (null != spsLocation){
                    classroomLatitude = SetClassroomSite.getDouble(spsLocation, "latitude", 0);
                    classroonLongitude = SetClassroomSite.getDouble(spsLocation, "longtitude", 0);
                }

                //获取当前地理位置
                Bundle bundle = msg.getData();
                double nowLatitude, nowLongitude;
                nowLatitude = bundle.getDouble("latitude", 0);
                nowLongitude = bundle.getDouble("longitude", 0);


                //获取手机当前的铃声模式
                int currentRingerMode = audioManager.getRingerMode();
                //获取当前位置与教室位置的距离
                double distance = getDistence(classroomLatitude, classroonLongitude, nowLatitude, nowLongitude);
                if (distance < MAX_CLASSROOM_RADIUS){ //教室范围内
                    IS_IN_CLASSROOM = true;

                    if(currentRingerMode != AudioManager.RINGER_MODE_VIBRATE){
                        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }
                }else{
                    IS_IN_CLASSROOM = false;

                    if(currentRingerMode == AudioManager.RINGER_MODE_VIBRATE){
                        audioManager.setRingerMode(primaryRingerMode);
                    }
                }


                //如果是service自己打开的网络，一定要记得关掉
                if (true == SERVICE_OPEN_NETWORK){
                    if (true == getMobileDataState(context, null)){
                        setMobileData(context, false);
                    }
                }
            }
        }
    };


    // 计算两点距离,单位为: 米
    private final double EARTH_RADIUS = 6378137.0;
    private double getDistence(double lat_a, double lng_a, double lat_b, double lng_b) {
        double radLat1 = (lat_a * Math.PI / 180.0);
        double radLat2 = (lat_b * Math.PI / 180.0);
        double a = radLat1 - radLat2;
        double b = (lng_a - lng_b) * Math.PI / 180.0;
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return s;
    }

    /**
     *开启和关闭移动数据网络有两种方法：
     * 1. 通过操作系统的数据库改变APN（网络接入点），从而实现开启和关闭移动数据网络，
     * 但是4.0 以后的 Android 不在提供普通应用程序对 APN(Access Point Name) 修改的权限，如果强制修改的话，
     * 会报安全异常直接挂掉的。除非有当前设备系统应用的签名，然后打包进你修改 APN 的应用里面。
     *
     * 2. 打开移动网络比较麻烦，系统没有直接提供开放的方法，只在ConnectivityManager类中有一个不可见的
     * setMobileDataEnabled方法，查看源代码发现，它是调用IConnectivityManager类中的setMobileDataEnabled(boolean)方法。
     * 由于方法不可见，只能采用反射来调用通过反射调用系统（ConnectivityManager）的setMoblieDataEnabled方法，通过操作该方法开启和关闭系统移动数据，
     * 同时也可以通过反射调用getMoblieDataEnabled方法获取当前的开启和关闭状态。
     *
     * 参考博客：
     * http://blog.csdn.net/fangzhibin4712/article/details/26563285
     * http://blog.csdn.net/way_ping_li/article/details/8493700
     */
    public static void setMobileData(Context pContext, boolean pBoolean) {

        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            Class ownerClass = mConnectivityManager.getClass();

            Class[] argsClass = new Class[1];
            argsClass[0] = boolean.class;

            Method method = ownerClass.getMethod("setMobileDataEnabled", argsClass);

            method.invoke(mConnectivityManager, pBoolean);

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            //System.out.println("移动数据设置错误: " + e.toString());
        }
    }

    /**
     * 返回手机移动数据的状态
     * @param pContext
     * @param arg 默认填null
     * @return true 连接 false 未连接
     */
    public static boolean getMobileDataState(Context pContext, Object[] arg) {

        try {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) pContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            Class ownerClass = mConnectivityManager.getClass();

            Class[] argsClass = null;
            if (arg != null) {
                argsClass = new Class[1];
                argsClass[0] = arg.getClass();
            }

            Method method = ownerClass.getMethod("getMobileDataEnabled", argsClass);

            Boolean isOpen = (Boolean) method.invoke(mConnectivityManager, arg);

            return isOpen;

        } catch (Exception e) {
            // TODO: handle exception
            //System.out.println("得到移动数据状态出错");
            return false;
        }

    }

    //要是早点发现这个函数就好了，，，
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        IS_IN_CLASSROOM = false;//关闭此服务时要确保SetQuietService也能开启

        if (MyLocation.mapManager != null) {
            MyLocation.mapManager.destroy();
        }
    }
}
