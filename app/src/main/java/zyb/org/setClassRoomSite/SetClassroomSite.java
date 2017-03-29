package zyb.org.setClassRoomSite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import zyb.org.androidschedule.R;

import android.location.Location;
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
 * Created by King_Tom_user_name on 2017/3/29.
 */

public class SetClassroomSite extends Activity{
    //声明一个SharedPreferences对象，用来保存教室经度纬度信息
    private SharedPreferences spsSwitch = null;
    private SharedPreferences.Editor spsEditorSwitch = null;

    private TextView tv1, tv2;
    private BMapManager mapManager;
    private MKLocationManager locationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_classrom_site);
        tv1 = (TextView) findViewById(R.id.result1);
        tv2 = (TextView) this.findViewById(R.id.result2);

        mapManager = new BMapManager(this);
        locationManager = mapManager.getLocationManager();

        mapManager.init("53351EE4BDE7BD870F41A0B4AF1480F1CA97DAF9",
                new MyMKGeneralListener());

        locationManager.setNotifyInternal(20, 5);

        // 注册位置更新事件
        locationManager.requestLocationUpdates(new MyLocationListener());

        mapManager.start();

    }

    // 定位自己的位置，只定位一次
    class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location arg0) {

            double jingdu1 = arg0.getLatitude();
            double weidu1 = arg0.getLongitude();

            int jingdu = (int) (arg0.getLatitude() * 1000000);
            int weidu = (int) (arg0.getLongitude() * 1000000);
            tv1.setText("经度：" + jingdu1 + ",纬度：" + weidu1);
            System.out.println("经度：" + jingdu1 + ",纬度：" + weidu1);
            MKSearch search = new MKSearch();
            search.init(mapManager, new MyMKSearchListener());
            search.reverseGeocode(new GeoPoint(jingdu, weidu));
        }

    }

    class MyMKSearchListener implements MKSearchListener {

        @Override
        public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
            if (arg0 == null) {
                tv2.setText("没有获取想要的位置");
            } else {
                GeoPoint point = arg0.geoPt;
                tv2.setText("地址：" + arg0.strAddr + ",坐标："
                        + point.getLatitudeE6() + "," + point.getLongitudeE6());
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

    // 常用事件监听，用来处理通常的网络错误，授权验证错误等
    class MyMKGeneralListener implements MKGeneralListener {

        @Override
        public void onGetNetworkState(int arg0) {
            if (arg0 == MKEvent.ERROR_NETWORK_CONNECT)
                Toast.makeText(SetClassroomSite.this, "您的网络出错啦！",
                        Toast.LENGTH_LONG).show();
        }

        @Override
        public void onGetPermissionState(int arg0) {

            if (arg0 == MKEvent.ERROR_PERMISSION_DENIED) {
                Toast.makeText(SetClassroomSite.this, "API KEY 错误，请检查！",
                        Toast.LENGTH_LONG).show();
            }
        }

    }

    public void onDestroy() {
        super.onDestroy();
        if (mapManager != null) {
            mapManager.destroy();

        }
    }

}
