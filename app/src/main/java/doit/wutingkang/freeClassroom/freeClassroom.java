package doit.wutingkang.freeClassroom;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

import doit.wutingkang.learningAssistant.R;

/**
 * Created by King_Tom_user_name on 2017/4/2.
 */

public class FreeClassroom extends AppCompatActivity {
    private WebView myWebView;
    private TextView backButton;
    private Button changeSchool;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freeclassroom);

        backButton = (TextView) findViewById(R.id.backWebButton);
        changeSchool = (Button)findViewById(R.id.changSchoolButton);
        myWebView = (WebView) findViewById(R.id.free_classroom);


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        changeSchool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (changeSchool.getText().equals("查询本部空闲教室")){
                    myWebView.loadUrl("http://jwxt.bupt.edu.cn/zxqDtKxJas.jsp");
                    changeSchool.setText("查询宏福空闲教室");
                }else {
                    myWebView.loadUrl("http://jwxt.bupt.edu.cn/hfxqDtKxJas.jsp");
                    changeSchool.setText("查询本部空闲教室");
                }

            }
        });

        // 设置WebView的客户端
        myWebView.setWebViewClient(new WebViewClient(){
            //覆盖shouldOverrideUrlLoading 方法
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        WebSettings webSettings = myWebView.getSettings();
        // 让WebView能够执行javaScript
        webSettings.setJavaScriptEnabled(true);
        // 让JavaScript可以自动打开windows
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        // 设置缓存
        webSettings.setAppCacheEnabled(false);

        //打开页面时， 自适应屏幕：
        webSettings.setUseWideViewPort(true);//设置此属性，可任意比例缩放
        webSettings.setLoadWithOverviewMode(true);

        // 支持缩放(适配到当前屏幕)
        webSettings.setBuiltInZoomControls(true);
        webSettings.setSupportZoom(true);

        // 支持内容重新布局,一共有四种方式
        // 默认的是NARROW_COLUMNS
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        // 设置可以被显示的屏幕控制
        webSettings.setDisplayZoomControls(true);
        // 设置默认字体大小
        webSettings.setDefaultFontSize(12);
    }

    @Override
    public void onStart(){
        super.onStart();
        myWebView.loadUrl("http://jwxt.bupt.edu.cn/zxqDtKxJas.jsp");
    }
}
