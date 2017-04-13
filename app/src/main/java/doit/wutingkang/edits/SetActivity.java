package doit.wutingkang.edits;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import doit.wutingkang.service.InClassRoomQuietService;
import doit.wutingkang.service.RemindReceiver;
import doit.wutingkang.setClassRoomSite.SetClassroomSite;
import doit.wutingkang.version.VersionActivity;
import temp.MyApplication;
import doit.wutingkang.learningAssistant.R;

public class SetActivity extends Activity {

	//声明一个SharedPreferences对象，用来保存switch组件的开关信息
	private SharedPreferences spsSwitch = null;
	private SharedPreferences.Editor spsEditorSwitch = null;

	//声明一个SharedPreferences对象，用来保存TimeChoice的值
	private SharedPreferences spsTimeChoice = null;
	private SharedPreferences.Editor spsEditorTimeChoice = null;

	//声明一个AlarmManager对象，用来开启课前提醒服务
	private AlarmManager alarmManager = null;
	//声明一个PendingIntent对象，用来指定alarmManager要启动的组件
	private PendingIntent piRemind = null;
	private Intent alarm_receiver = null;

	//定义单选列表对话狂的id，该对话框用于显示课前提醒时间的可选项
	final int SINGLE_DIALOG = 0x113;
	//定义选中的时间
	private int choicedTime = 0;

	private Switch switchQuiet;
	private Switch switchRemind;
	private Switch switchInClassQuiet;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set);
		//将该activity加入到MyApplication对象实例容器中
		MyApplication.getInstance().addActivity(this);

		//声明一个获取系统音频服务的类的对象
		final AudioManager audioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
		//从MainAcivity中获取原始设置的铃声模式
		Intent intent = getIntent();
		final int primaryRingerMode = intent.getIntExtra("mode_ringer", AudioManager.RINGER_MODE_NORMAL);
		//获取系统的闹钟定时服务
		alarmManager = (AlarmManager)getSystemService(Service.ALARM_SERVICE);

		//指定alarmManager要启动的组件
		alarm_receiver = new Intent(SetActivity.this, RemindReceiver.class);
//		alarm_receiver.putExtra("anvance_remindtime", choicedTime);
		piRemind = PendingIntent.getBroadcast(SetActivity.this, 0, alarm_receiver, 0);

		//取出各组件
		TextView btnBack = (TextView)findViewById(R.id.backtoMainButton);
		switchQuiet = (Switch)findViewById(R.id.switch_quiet);
		switchRemind = (Switch)findViewById(R.id.switch_remind);
		switchInClassQuiet = (Switch)findViewById(R.id.switch_in_class_quiet);

		//这里模式一定要设置为MODE_MULTI_PROCESS，否则即使相应的xml文件中数据有更新，RemindReceiver中也不能获取更新后的数据，而是一直获取上次的数据， 除非清空缓存
		this.spsTimeChoice = SetActivity.this.getSharedPreferences("time", Context.MODE_MULTI_PROCESS);
		this.spsEditorTimeChoice = spsTimeChoice.edit();

		//指定该SharedPreferences数据可以跨进称调用
		this.spsSwitch = SetActivity.this.getSharedPreferences("switch", Context.MODE_MULTI_PROCESS);
		this.spsEditorSwitch = spsSwitch.edit();
		//每次创建该activity时，从preferences中读取开关信息的数据
		Boolean quietStatus = spsSwitch.getBoolean("switch_quiet", false);
		Boolean remindStatus = spsSwitch.getBoolean("switch_remind", false);
		Boolean inClassQuietStatus = spsSwitch.getBoolean("switch_in_class_quiet", false);
		switchQuiet.setChecked(quietStatus);
		switchRemind.setChecked(remindStatus);
		switchInClassQuiet.setChecked(inClassQuietStatus);

		//为返回按钮绑定监听器
		btnBack.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
//				Intent intent = new Intent(Set.this,MainActivity.class);
//				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//刷新
//				startActivity(intent);
			}
		});

		//为自动静音开关按钮绑定监听器
		switchQuiet.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				//启动自动静音的service
				Intent intent = new Intent();
				intent.setAction("doit.wutingkang.service.QUIET_SERVICE");

				if(isChecked){
					if(startService(intent) != null)
						Toast.makeText(SetActivity.this, "成功开启，上课期间的来电将自动转为振动模式", Toast.LENGTH_SHORT).show();
					else{
						Toast.makeText(SetActivity.this, "未能成功开启，请重新尝试", Toast.LENGTH_SHORT).show();
						switchQuiet.setChecked(false);
					}
				}
				else{
					if(stopService(intent)){
						Toast.makeText(SetActivity.this, "成功关闭，恢复到原来的响铃模式", Toast.LENGTH_SHORT).show();
					}
					else{
						Toast.makeText(SetActivity.this, "未能成功关闭，请重新尝试", Toast.LENGTH_SHORT).show();
						switchQuiet.setChecked(true);
					}
					audioManager.setRingerMode(primaryRingerMode);
				}
				//将开关信息数据保存进preferences中
				SetActivity.this.spsEditorSwitch.putBoolean("switch_quiet", isChecked);
				spsEditorSwitch.commit();
			}
		});

		//为课前提醒开关按钮绑定监听器
		switchRemind.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if(isChecked){
					showDialog(SINGLE_DIALOG);
				}
				else{
					alarmManager.cancel(piRemind);
				}
				//将开关信息数据保存进preferences中
				SetActivity.this.spsEditorSwitch.putBoolean("switch_remind", isChecked);
				spsEditorSwitch.commit();
			}
		});

		//开启开关后，就算当前没有连接网络也会自动联网获取地理位置，因为半夜一般不在教室，这段时间可以过滤掉
		switchInClassQuiet.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
             //必须是在已经设置教室位置的情况下才执行
             SharedPreferences spsLocation = SetActivity.this.getSharedPreferences("LocationInfo", SetActivity.this.MODE_PRIVATE);
             if (null != spsLocation){
                 if (0 != SetClassroomSite.getDouble(spsLocation, "latitude", 0)){

                     //启动自动静音的service
                     Intent intent = new Intent();
                     intent.setAction("doit.wutingkang.service.InClassRoomQuietService");

                     if(isChecked){
                         if(startService(intent) != null)
                             Toast.makeText(SetActivity.this, "成功开启，进入教室后会自动静音", Toast.LENGTH_SHORT).show();
                         else{
                             Toast.makeText(SetActivity.this, "未能成功开启，请重新尝试", Toast.LENGTH_SHORT).show();
                             switchInClassQuiet.setChecked(false);
                         }
                     }
                     else{
                         if(stopService(intent))
                             Toast.makeText(SetActivity.this, "成功关闭，进入教室不会自动静音", Toast.LENGTH_SHORT).show();
                         else{
                             Toast.makeText(SetActivity.this, "未能成功关闭，请重新尝试", Toast.LENGTH_SHORT).show();
                             switchInClassQuiet.setChecked(true);
                         }
						 audioManager.setRingerMode(primaryRingerMode);

						 InClassRoomQuietService.IS_IN_CLASSROOM = false;//关闭此服务时要确保SetQuietService也能开启
                     }

                     //将开关信息数据保存进preferences中
                     SetActivity.this.spsEditorSwitch.putBoolean("switch_in_class_quiet", isChecked);
                     spsEditorSwitch.commit();

                 } else {
                     switchInClassQuiet.setChecked(false);
                     Toast.makeText(SetActivity.this, "请先设置教室位置", Toast.LENGTH_SHORT).show();
                 }
             } else {
                 switchInClassQuiet.setChecked(false);
                 Toast.makeText(SetActivity.this, "请先设置教室位置", Toast.LENGTH_SHORT).show();
             }
			}
		});
	}

	@Override
	//该方法返回的Dialog将被showDialog()方法回调
	protected Dialog onCreateDialog(int id, Bundle args) {
		//判断生成何种类型的对话框
		if(id == SINGLE_DIALOG){
			Builder builder = new Builder(this);
			// 设置对话框的标题
			builder.setTitle("选择课前提醒时间");
			// 为对话框设置多个列表，参数-1表示默认不选中任何选项
			builder.setSingleChoiceItems(R.array.set_remind, -1, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog,
									int which){
					switch (which){
						case 0:
							choicedTime = 5;
							break;
						case 1:
							choicedTime = 10;
							break;
						case 2:
							choicedTime = 20;
							break;
						case 3:
							choicedTime = 30;
							break;
						case 4:
							choicedTime = 40;
							break;
						case 5:
							choicedTime = 50;
							break;
						case 6:
							choicedTime = 60;
							break;
					}
				}
			});
			// 添加一个“确定”按钮，用于关闭该对话框
			builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
//					System.out.println("SetActivity:" + choicedTime);
					if(choicedTime == 0){
						Toast.makeText(SetActivity.this, "请选择课前提醒的时间", Toast.LENGTH_SHORT).show();
						switchRemind.setChecked(false);
					}else{
						SetActivity.this.spsEditorTimeChoice.putInt("choicedTime", choicedTime);
						spsEditorTimeChoice.commit();
						//从当前时间开始，每隔一分钟启动一次pi指定的组件，即发送一次广播
						alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 60000, piRemind);
						Toast.makeText(SetActivity.this, "设置成功，系统将在课前" + choicedTime + "分钟提醒您", Toast.LENGTH_LONG).show();
					}
				}
			});
			//添加一个“取消”按钮
			builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					switchRemind.setChecked(false);
				}
			});
			// 创建对话框
			return builder.create();
		}
		else
			return null;
	}


	public void clickVision(View v){
		Intent intent = new Intent(SetActivity.this, VersionActivity.class);
		startActivity(intent);
	}

	public void clickSetClassroomSite(View v){
		Intent intent = new Intent(SetActivity.this, SetClassroomSite.class);
		startActivity(intent);
	}
}
