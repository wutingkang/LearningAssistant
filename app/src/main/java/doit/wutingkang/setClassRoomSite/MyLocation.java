package doit.wutingkang.setClassRoomSite;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import com.baidu.mapapi.BMapManager;
import com.baidu.mapapi.GeoPoint;
import com.baidu.mapapi.LocationListener;
import com.baidu.mapapi.MKAddrInfo;
import com.baidu.mapapi.MKBusLineResult;
import com.baidu.mapapi.MKDrivingRouteResult;
import com.baidu.mapapi.MKEvent;
import com.baidu.mapapi.MKGeneralListener;
import com.baidu.mapapi.MKLocationManager;
import com.baidu.mapapi.MKPoiResult;
import com.baidu.mapapi.MKSearch;
import com.baidu.mapapi.MKSearchListener;
import com.baidu.mapapi.MKSuggestionResult;
import com.baidu.mapapi.MKTransitRouteResult;
import com.baidu.mapapi.MKWalkingRouteResult;

/**
 * Created by King_Tom_user_name on 2017/4/1.
 */

public class MyLocation {
    public static Context context;
    public static Handler myHandler;
    public static BMapManager mapManager = null;
    public static MKLocationManager locationManager;

    public static Message myMessage;
    public static Bundle myBundle;

    public static int LATITUDE_AND_LONGITUDE = 0;
    public static int SHOWLOCATION = 1;
    public static int LOCATION_TYPE = LATITUDE_AND_LONGITUDE;

    public MyLocation(Context context, Handler hander){ //要加public，否则外包的InClassRoomQuietService不能使用
        LOCATION_TYPE = LATITUDE_AND_LONGITUDE;
        this.context = context;
        this.myHandler = hander;
        this.myMessage = Message.obtain();
        this.myBundle = new Bundle();
    }

    //获取位置信息
    public void getLocation(int location_type){
        LOCATION_TYPE = location_type;

        mapManager = new BMapManager(context);//用MyApplication.getContext()会报错
        locationManager = mapManager.getLocationManager();
        mapManager.init("53351EE4BDE7BD870F41A0B4AF1480F1CA97DAF9",
                new MyLocation.MyMKGeneralListener());
        locationManager.setNotifyInternal(20, 5);

        // 注册位置更新事件
        locationManager.requestLocationUpdates(new MyLocation.MyLocationListener());
        mapManager.start();
    }


    // 常用事件监听，用来处理通常的网络错误，授权验证错误等
    static class MyMKGeneralListener implements MKGeneralListener {

        @Override
        public void onGetNetworkState(int arg0) {
            if (arg0 == MKEvent.ERROR_NETWORK_CONNECT){
                //可以改成提示打开网络
                Toast.makeText(context, "网络未连接！无法获取位置信息。",
                        Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onGetPermissionState(int arg0) {
            if (arg0 == MKEvent.ERROR_PERMISSION_DENIED) {
                Toast.makeText(context, "API KEY 错误，请检查！",
                        Toast.LENGTH_LONG).show();
            }
        }

    }


    // 定位自己的位置，只定位一次,
    static class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location arg0) {
            //两种方式都需要经纬度信息
            myBundle.putDouble("latitude", arg0.getLatitude());
            myBundle.putDouble("longitude", arg0.getLongitude());

            if (LOCATION_TYPE == LATITUDE_AND_LONGITUDE){
                myMessage.what = LATITUDE_AND_LONGITUDE;
                myMessage.setData(myBundle);
                myHandler.sendMessage(myMessage);

                System.out.println("in1经度：" + arg0.getLatitude() + "in1,纬度：" + arg0.getLongitude());

                if (null != mapManager){
                    mapManager.stop();
                }

            }else if (LOCATION_TYPE == SHOWLOCATION){
                int jingdu = (int) (arg0.getLatitude() * 1000000);
                int weidu = (int) (arg0.getLongitude() * 1000000);

                MKSearch search = new MKSearch();
                search.init(mapManager, new MyLocation.MyMKSearchListener());
                search.reverseGeocode(new GeoPoint(jingdu, weidu));
            }
        }
    }

    static class MyMKSearchListener implements MKSearchListener {

        @Override
        public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
            myMessage.what = SHOWLOCATION;

            String strLocation;

            if (arg0 == null) {
                strLocation = null;
            } else {
                GeoPoint point = arg0.geoPt;
                strLocation = "地址：\n" + arg0.strAddr + "\n坐标：\n"+ point.getLatitudeE6() + "," + point.getLongitudeE6();
            }

            myBundle.putString("strLocation", strLocation);
            myMessage.setData(myBundle);
            myHandler.sendMessage(myMessage);

            if (null != mapManager){
                mapManager.stop();
            }
        }

        @Override
        public void onGetDrivingRouteResult(MKDrivingRouteResult arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetPoiResult(MKPoiResult arg0, int arg1, int arg2) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetTransitRouteResult(MKTransitRouteResult arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetWalkingRouteResult(MKWalkingRouteResult arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
            // TODO Auto-generated method stub

        }

    }
}
