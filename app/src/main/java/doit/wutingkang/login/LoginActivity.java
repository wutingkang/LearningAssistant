package doit.wutingkang.login;

/**
 * Created by wutingkang on 2017/3/20.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.Timer;
import java.util.TimerTask;

import doit.wutingkang.mainface.R;
import temp.WifiListDialog;


public class LoginActivity extends AppCompatActivity {
    private TextView btnBack;
    private Button btnNeedLogin;
    private Button btnFreeLogin;
    private final static String NEED_lOGINWIFILIST = "需登录wifi名单";
    private final static String FREE_LOGINWIFILIST = "免登录Wifi名单";

    private EditText editName;
    private EditText editPassWord;
    private static Button btnLogin;
    private SharedPreferences spsLogin;
    private SharedPreferences.Editor spsEditorLogin;


    public static int UN_LOGIN = 0;
    public static int LOGINED = 1;
    public static int LOGIN_STATE = UN_LOGIN;

    private int UNCONNECTED = 0;
    private Timer mTimer;
    private MyTimerTask mTimerTask;
    private int MAX_LOGIN_COUNT = 12;//用来控制最大登录连接时间，超时后不再尝试（下面0.5s尝试一次，所以 6s）
    public static boolean mainIsConnecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        btnBack = (TextView)findViewById(R.id.exitLoginButton);
        btnNeedLogin = (Button)findViewById(R.id.needLoginButton);
        btnFreeLogin = (Button)findViewById(R.id.freeLoginButton);

        editName = (EditText) findViewById(R.id.main_et_name);
        editPassWord = (EditText) findViewById(R.id.main_et_password);
        btnLogin = (Button) findViewById(R.id.main_btn_save);

        spsLogin = LoginActivity.this.getSharedPreferences("loginInfo", this.MODE_PRIVATE);
        spsEditorLogin = spsLogin.edit();
        editName.setText(spsLogin.getString("userName", ""));
        editPassWord.setText(spsLogin.getString("passWord", ""));

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnNeedLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new WifiListDialog(LoginActivity.this).showWifiList(NEED_lOGINWIFILIST);
            }
        });

        btnFreeLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new WifiListDialog(LoginActivity.this).showWifiList(FREE_LOGINWIFILIST);
            }
        });

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //保存当前数据
                spsEditorLogin.putString("userName", editName.getText().toString());
                spsEditorLogin.putString("passWord", editPassWord.getText().toString());
                spsEditorLogin.apply();

                if (btnLogin.getText().equals("登录成功,再次点击退出") || btnLogin.getText().equals("连接到免登录网络,点击退出")){
                    if (isNetworkAvailable() && LOGIN_STATE == LOGINED)//并不能严格判断是否登出了，但判断又要发net请求，还不如直接发送登录请求
                        finish();
                    else
                        btnLogin.setText("已登出，请重新登录");
                }else {//登录的内容需放在else里， 否则finish（）之后还会执，不知道为什么
                    btnLogin.setText("登录中");

                    //判断wifi的状态，实现一键连接并登录
                    WifiManager manager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (! manager.isWifiEnabled()) {
                        mainIsConnecting = true;  //防止WifiInfoReceiver重复登陆
                        manager.setWifiEnabled(true);

                        //本来想打开wifi后,剩下的工作交给WifiInfoReceiver去做，但是这里是打开Mainactivity后点击登录按钮打开wifi开关的
                        //所以想要在UI界面中显示各种正确或错误的提示的话，必须在MainActivity中处理UI，在WifiInfoReceiver中不确定
                        //MainActivity是否开启，没开启时修改UI会出错
                    }

                    MAX_LOGIN_COUNT = 12;
                    mTimer = new Timer();
                    mTimerTask = new MyTimerTask();  // 新建一个任务
                    mTimer.schedule(mTimerTask, 0, 500); //每隔0.5执行一次，满足条件（连接上某个网络）后不执行
                }
            }
        });
    }

    public static boolean isNeedLoginWifi(Context context, String nowWifiName){
        //先从SharedPreferences中取出需登录wifi名单数组
        String wifiList[] = getWifiList(context, NEED_lOGINWIFILIST);
        String legalWifiName;

        if (null != wifiList){
            for (String wifiName : wifiList) {

                //因为nowWifiName 被 "" 包围着，(本想在存储的时候连冒号一起存的，但是在展示名单列表的时候还得去掉冒号，太麻烦了
                //只好牺牲一下sNeedLoginWifi()执行速度，放在这里添加 冒号。当然，若要追求验证的速度也可以放在存储的那边处理。）
                legalWifiName = "\"" + wifiName + "\"";

                if (nowWifiName.equals(legalWifiName)){
                    return true;
                }
            }
        }
        return false;
    }

    //与 isNeedLoginWifi() 的区别只在 getWifiList，其实也可以合并成只用一个，但是为了在其他地方的可读性高些，就不合并了
    public static boolean isFreeLoginWifi(Context context, String nowWifiName){
        //先从SharedPreferences中取出免登录wifi名单数组
        String wifiList[] = getWifiList(context, FREE_LOGINWIFILIST);
        String legalWifiName;

        if (null != wifiList){
            for (String wifiName : wifiList) {

                //因为nowWifiName 被 "" 包围着，(本想在存储的时候连冒号一起存的，但是在展示名单列表的时候还得去掉冒号，太麻烦了
                //只好牺牲一下sNeedLoginWifi()执行速度，放在这里添加 冒号。当然，若要追求验证的速度也可以放在存储的那边处理。）
                legalWifiName = "\"" + wifiName + "\"";

                if (nowWifiName.equals(legalWifiName)){
                    return true;
                }
            }
        }
        return false;
    }

    public static String[] getWifiList(Context context, final String typeOfWifiList)
    {
        SharedPreferences prefs = context.getSharedPreferences(typeOfWifiList, Context.MODE_PRIVATE);
        String[] resArray = new String[0]; //防止resArray为null导致错误

        try {
            JSONArray jsonArray = new JSONArray(prefs.getString(typeOfWifiList, "[]"));

            resArray = new String[jsonArray.length()];

            for (int i = 0; i < jsonArray.length(); i++) {
                resArray[i] = jsonArray.getString(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return resArray;
    }

    public static void setWifiList(Context context, final String typeOfWifiList, String[] StringArray) {
        SharedPreferences prefs = context.getSharedPreferences(typeOfWifiList, Context.MODE_PRIVATE);
        JSONArray jsonArray = new JSONArray();

        for (String str : StringArray) { //StringArray不能为null，但可以为StringArray[0]
            jsonArray.put(str);
        }

        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(typeOfWifiList,jsonArray.toString());
        editor.commit();
    }

    class MyTimerTask extends TimerTask{
        @Override
        public void run() {
            handlerMain.sendEmptyMessage(UNCONNECTED);
        }
    }

    //要是早点发现这个函数就好了，，，
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public Handler handlerMain = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UNCONNECTED) {

                //之前用getSSID（）判断的时候不知道为什么SSID已经合法了却还是报错： connect failed: ENETUNREACH (Network is unreachable)
                //所以还需isNetworkAvailable（）判断网络是否可连接，不知道为什么WifiInfoReceiver里不用判断。早发现就好了，，，

                if (isNetworkAvailable()){
                    WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();

                    //调试
                    System.out.print("wifi name:" +  wifiInfo.getSSID());

                    //以下判断有效是建立在不同种类的wifi名称不重复，还有不是特殊名称（如：<UNKNOWN SSID>）的情况下

                    //因为登录的用户名密码只对校园网有效，就直接根据wifi名称判断是否需要连接。
                    //增加了需登录的wifi名单增删改功能，避免随便向未知网络发送自己的用户名密码，提高安全性
                    if (isNeedLoginWifi(LoginActivity.this, wifiInfo.getSSID())){

                        btnLogin.setText("登录" + wifiInfo.getSSID() + "中...");

                        //也可以确定未登录再发送登录请求，但判断又要发net请求，还不如直接发送登录请求
                        new Thread(WifiInfoReceiver.initRunnable(handlerMain, LoginActivity.this,
                                editName.getText().toString(), editPassWord.getText().toString())).start();

                        if (null != mTimerTask)
                            mTimerTask.cancel();
                        if (null != mTimer)
                            mTimer.cancel();
                    } else  if (isFreeLoginWifi(LoginActivity.this, wifiInfo.getSSID())){//连到 免登录wifi名单 的网络提醒
                        LOGIN_STATE = LOGINED;
                        btnLogin.setText("连接到免登录网络,点击退出");

                        if (null != mTimerTask)
                            mTimerTask.cancel();
                        if (null != mTimer)
                            mTimer.cancel();
                    } else if ((! wifiInfo.getSSID().equals("<UNKNOWN SSID>")) &&
                              ((! wifiInfo.getSSID().equals("OX")))){
                        //因为isNetworkAvailable（）判断的是网络是否连接上，所以
                        //有可能是在数据打开的情况下打开wifi,这时还未来得及连上任何wifi，但网络状态却可用，注意括号里面有“！”

                        //后续可以添加提醒是否加入免/需登录wifi名单的功能
                        btnLogin.setText("连接到白名单之外的:" + wifiInfo.getSSID());
                        LOGIN_STATE = UN_LOGIN;
                    }

                } else {
                    //动态显示按钮样式
                    if (MAX_LOGIN_COUNT % 3 == 0)
                        btnLogin.setText("登录中...");
                    else if (MAX_LOGIN_COUNT % 3 == 1)
                        btnLogin.setText("登录中..");
                    else if (MAX_LOGIN_COUNT % 3 == 2)
                        btnLogin.setText("登录中.");
                }

                if (0 == --MAX_LOGIN_COUNT) {//连接超时
                    btnLogin.setText("连接超时，重新登录请点击");

                    if (null != mTimerTask)
                        mTimerTask.cancel();
                    if (null != mTimer)
                        mTimer.cancel();
                }

            }else if (msg.what == WifiInfoReceiver.LOGIN_SUCCESS) {
                btnLogin.setText("登录成功,再次点击退出");
            } else if (msg.what == WifiInfoReceiver.USERNAME_ERROR) {
                editName.setError("用户名错误");
            } else if (msg.what == WifiInfoReceiver.PASSWORD_ERROR) {
                editPassWord.setError("密码错误");
            }else if (msg.what == WifiInfoReceiver.FLOWLIMIT_ERROR){
                editPassWord.setError("本月流量已用完");
            }else if (msg.what == WifiInfoReceiver.UNSURE) {
                btnLogin.setText("可能登录上了，你试试呗~");
            }
        }
    };

}