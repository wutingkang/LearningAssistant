package zyb.wutingkang.setClassRoomSite;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import zyb.wutingkang.mainface.R;


/**
 * Created by King_Tom_user_name on 2017/3/29.
 */

public class SetClassroomSite extends Activity{
    //声明一个SharedPreferences对象，用来保存教室经度纬度信息，
    //因为教学楼不一定只有一个，所以后可以改成添加多个位置信息
    public static SharedPreferences spsLocation = null;
    public static SharedPreferences.Editor spsEditorLocation = null;

    public static TextView tv1, tv2;
    private Button btnSetClassroomSite;

    private MyLocation classRoomLocation;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_classrom_site);
        btnSetClassroomSite = (Button)findViewById(R.id.idGetClassroomSite);
        tv1 = (TextView) findViewById(R.id.result1);
        tv2 = (TextView) this.findViewById(R.id.result2);

        spsLocation = SetClassroomSite.this.getSharedPreferences("LocationInfo", this.MODE_PRIVATE);
        spsEditorLocation = spsLocation.edit();
        if (null != spsLocation){
            tv1.setText("old经度：" + getDouble(spsLocation, "latitude", 0) +
                      "\nold纬度：" + getDouble(spsLocation, "longtitude", 0));
        }


        btnSetClassroomSite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                classRoomLocation = new MyLocation(SetClassroomSite.this, handlerLocation);

                classRoomLocation.getLocation(MyLocation.SHOWLOCATION);
            }
        });
    }

    public Handler handlerLocation = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            Bundle bundle = msg.getData();

           if (msg.what == MyLocation.SHOWLOCATION) {

                String strLocation;
                strLocation = bundle.getString("strLocation", "");
                if (null != strLocation){

                    double latitude, longitude;
                    latitude = bundle.getDouble("latitude", 0);
                    longitude = bundle.getDouble("longitude", 0);
                    tv1.setText("now经度：" + latitude + "\nnow纬度：" + longitude);

                    putDouble(spsEditorLocation, "latitude", latitude);
                    putDouble(spsEditorLocation, "longtitude", longitude);
                    spsEditorLocation.apply();

                    tv2.setText(strLocation + "\n\n\n                             设置成功！");
                }else{
                    tv2.setText("设置失败，请确保当前网络可用。");
                }
           }
        }
    };

    //SharedPreferences不能存储double数据，但是这样就可以了
    //参考：http://stackoverflow.com/questions/16319237/cant-put-double-sharedpreferences
    public static Editor putDouble(final Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    public static double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (MyLocation.mapManager != null) {
            MyLocation.mapManager.destroy();
        }
    }
}

