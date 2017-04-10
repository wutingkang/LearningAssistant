package temp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import doit.wutingkang.login.LoginActivity;
import doit.wutingkang.mainface.R;

/**
 * Created by King_Tom_user_name on 2017/4/9.
 */

public class WifiListDialog {

    private View view;
    private Context context;
    private LayoutInflater inflater;
    private AlertDialog.Builder builder;
    private EditText etNewWifiName;
    private String wifiList[] = null;

    public WifiListDialog(Context context){
        this.context = context;
    }


    /*
    * 点击wifi名单按钮，弹出 增改删除 “wifi名单” 对话框,需要登录和无需登录共用此对话框，用typeOfWifiList区分
    */
    public void showWifiList(final String typeOfWifiList){
        //填装对话框的view
        inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.wifi_list, null);

        wifiList = LoginActivity.getWifiList(context, typeOfWifiList);

        //wifi名称的输入框
        etNewWifiName = (EditText)view.findViewById(R.id.new_wifi_name);

        builder = new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_launcher)
                .setTitle(typeOfWifiList)
                .setView(view)
                .setPositiveButton("确认",new DialogInterface.OnClickListener(){

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        String newWifiName;

                        if(! (newWifiName = etNewWifiName.getText().toString()).equals("")){

                            if(false) { //可以添加名称重复提醒功能
                                Toast.makeText(context, "该wifi名称已添加", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            else {
                                if (null != wifiList){
                                    //就算wifiList.length为0也可以添加
                                    String newWifiList[] = new String[wifiList.length + 1];
                                    int i = 0;

                                    for (; i < wifiList.length; i++) {
                                        newWifiList[i] = wifiList[i];
                                    }
                                    newWifiList[i] = newWifiName;

                                    LoginActivity.setWifiList(context, typeOfWifiList, newWifiList);
                                }else {
                                    Toast.makeText(context, "添加出错了！", Toast.LENGTH_SHORT).show();
                                }
                            }

                        } else{
                            Toast.makeText(context, "wifi名称不能为空！", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                });

            //wifi名单
            if (null != wifiList && 0 != wifiList.length){
                builder.setItems(wifiList, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) //点击后对话框就会消失掉
                    {
                        deleteAndModifyWifiName(which, typeOfWifiList);
                    }
                });
            }

        builder.create().show();
    }

    public void deleteAndModifyWifiName(final int which, final String typeOfWifiList){
        //填装对话框的view
        inflater = LayoutInflater.from(context);
        view = inflater.inflate(R.layout.wifi_list, null);

        wifiList = LoginActivity.getWifiList(context, typeOfWifiList);

        //wifi名称的输入框
        etNewWifiName = (EditText)view.findViewById(R.id.new_wifi_name);

        etNewWifiName.setText(wifiList[which]);

        builder = new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_launcher)
                .setTitle("编辑wifi名称")
                .setView(view)
                .setPositiveButton("修改",new DialogInterface.OnClickListener(){

                    @SuppressWarnings("deprecation")
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        String newWifiName;

                        if(! (newWifiName = etNewWifiName.getText().toString()).equals("")){

                            if(false) { //可以添加名称重复提醒功能
                                Toast.makeText(context, "该wifi名称已添加", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            else {
                                wifiList[which] = newWifiName;
                                LoginActivity.setWifiList(context, typeOfWifiList, wifiList);
                            }

                        } else{
                            Toast.makeText(context, "wifi名称不能为空！", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                })
                .setNegativeButton("删除", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                        if (null != wifiList){ //能进入到这wifiList.length至少为1，但保险起见还是检验一下
                            if (1 == wifiList.length){ //只有一个wifi名称的情况
                                LoginActivity.setWifiList(context, typeOfWifiList, new String[0]);
                            }else{
                                String newWifiList[] = new String[wifiList.length - 1];
                                int newIndex = 0;

                                for (int i = 0; i < wifiList.length; i++) {
                                    if (i != which){
                                        newWifiList[newIndex++] = wifiList[i];
                                    }
                                }

                                LoginActivity.setWifiList(context, typeOfWifiList, newWifiList);
                            }
                        }else {
                            Toast.makeText(context, "删除出错！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
        builder.create().show();
    }

}
