package doit.wutingkang.learningAssistant;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;

import doit.wutingkang.edits.SetActivity;
import doit.wutingkang.freeClassroom.FreeClassroom;
import doit.wutingkang.login.LoginActivity;
import temp.DataBase;
import temp.MyApplication;
import temp.MainDialog;
import temp.ShareMethod;

public class MainActivity extends AppCompatActivity {

	public ListView listViews[] = new ListView[7];
	private TabHost tabHost = null;
	private TextView btnExit = null;
	private Button btnLogin = null;
    private Button btnFreeClassroom = null;
	private TextView btnSet = null;
	public static DataBase dataBase;
	public Cursor[] cursor = new Cursor[7];
	public SimpleCursorAdapter adapter;
	private SharedPreferences spsFirstRun;

	//定义手势检测器实例
	private GestureDetector gestureDetector = null;
	//定义手势动作两点之间的最小距离
	private final int FLIP_DISTANCE = 200;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//将该activity加入到MyApplication对象实例容器中
		MyApplication.getInstance().addActivity(this);

		dataBase = new DataBase(MainActivity.this);
		spsFirstRun = getSharedPreferences("firstStart", Context.MODE_PRIVATE);
		/*
		 * 判断程序是否第一次运行，如果是创建数据库表
		 */
		if(spsFirstRun.getBoolean("firstStart", true)){
			SingleInstance.createTable();
			(spsFirstRun.edit()).putBoolean("firstStart", false).commit();
//			finish();
		}


		btnExit = (TextView)findViewById(R.id.exitButton);
		btnLogin = (Button) findViewById(R.id.loginButton);
        btnFreeClassroom = (Button) findViewById(R.id.freeClassroomButton);
		btnSet = (TextView)findViewById(R.id.setButton);
		listViews[0] = (ListView)findViewById(R.id.list0);
		listViews[1] = (ListView)findViewById(R.id.list1);
		listViews[2] = (ListView)findViewById(R.id.list2);
		listViews[3] = (ListView)findViewById(R.id.list3);
		listViews[4] = (ListView)findViewById(R.id.list4);
		listViews[5] = (ListView)findViewById(R.id.list5);
		listViews[6] = (ListView)findViewById(R.id.list6);
		tabHost = (TabHost)findViewById(R.id.tabhost);
		//创建手势检测器
		gestureDetector = new GestureDetector(this, new DetectorGestureListener());

		//在配置任何的TabSpec之前，必须在TabHost上调用该方法
		tabHost.setup();

		//为主界面注册七个选项卡
		TabHost.TabSpec  spec = null;
		addCard(spec,"tag1",R.id.list0,"日");
		addCard(spec,"tag2",R.id.list1,"一");
		addCard(spec,"tag3",R.id.list2,"二");
		addCard(spec,"tag4",R.id.list3,"三");
		addCard(spec,"tag5",R.id.list4,"四");
		addCard(spec,"tag6",R.id.list5,"五");
		addCard(spec,"tag7",R.id.list6,"六");

		//修改tabHost选项卡中的字体的颜色
		TabWidget tabWidget = tabHost.getTabWidget();
		for(int i=0;i<tabWidget.getChildCount();i++){
			TextView tv = (TextView)tabWidget.getChildAt(i).findViewById(android.R.id.title);
			tv.setTextColor(0xff004499);
		}

		//设置打开时默认的选项卡是当天的选项卡
		tabHost.setCurrentTab(ShareMethod.getWeekDay());

		//用适配器为各选项卡添加所要显示的内容
		for(int i=0;i<7;i++){
			cursor[i]= MainActivity.dataBase.select(i);
			listViews[i].setAdapter(adapter(i));
		}

		//声明一个获取系统音频服务的类的对象
		final AudioManager audioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
		//获取手机之前设置好的铃声模式,该数据将用来传递给activity_set
		final int primaryRingerMode = audioManager.getRingerMode();

		//为退出按钮绑定监听器
		btnExit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//创建AlertDialog.Builder对象，该对象是AlterDialog的创建器，AlterDialog用来创建弹出对话框
				final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
				exit(builder);
			}
		});

		btnLogin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, LoginActivity.class);
				startActivity(intent);
			}
		});

        btnFreeClassroom.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, FreeClassroom.class);
                startActivity(intent);
            }
        });

		//为设置按钮绑定监听器
		btnSet.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainActivity.this, SetActivity.class);
				//将orgRingerMode数据传给activity_set
				intent.putExtra("mode_ringer", primaryRingerMode);
				startActivity(intent);
			}
		});

		for( int day=0;day<7;day++){
			//为七个ListView绑定触碰监听器，将其上的触碰事件交给GestureDetector处理
			//此监听器是必须的，不然滑动手势只在ListView下的空白区域有效，而在ListView上无效
			listViews[day].setOnTouchListener(new OnTouchListener() {

				@Override
				public boolean onTouch(View v, MotionEvent event)   {
					return gestureDetector.onTouchEvent(event);
				}
			});
			//为每个ListView的每个item绑定监听器，点击则弹出由AlertDialog创建的列表对话框进行选择
			listViews[day].setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View arg1,
										final int id, long arg3) {
					final int currentDay= tabHost.getCurrentTab();
					final int n = id;
					final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
					builder.setIcon(R.drawable.ic_launcher);
					builder.setTitle("选择");
					TextView tv = (TextView)arg1.findViewById(R.id.ltext0);


					//Log.i("Test",(tv.getText().toString().equals(""))+"");
					//如果课程栏目为空就启动添加对话框
					if((tv.getText()).toString().equals("")){
						//通过数组资源为对话框中的列表添加选项内容，这里只有一个选项
						builder.setItems(R.array.edit_options1, new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								//如果单击了该列表项，则跳转到编辑课程信息的界面
								if(which == 0){
									new MainDialog(MainActivity.this).add(currentDay,n);
								}
							}
						});
						builder.create().show();
					}
					//否则启动修改对话框，或直接删除数据
					else{
						builder.setItems(R.array.edit_options2, new DialogInterface.OnClickListener() {

							@SuppressWarnings("deprecation")
							@Override
							public void onClick(DialogInterface dialog, int which) {
								//如果单击了该列表项，则跳转到编辑课程信息的界面
								if(which == 0){//修改
									new MainDialog(MainActivity.this).modify(currentDay,n);
								}
								if(which == 1){//删除
									cursor[currentDay].moveToPosition(n);
									int n1=Integer.parseInt(cursor[currentDay].getString(7));//课程的总节数
									int n2=Integer.parseInt(cursor[currentDay].getString(8));//选中的为该课程的第几节
									switch(n2){
										case 0:
											for(int m=0;m<n1;m++){
												MainActivity.dataBase.deleteData(currentDay,n+m+1);
											}
											break;

										case 1:
											MainActivity.dataBase.deleteData(currentDay,n);
											for(int m=1;m<n1;m++){
												MainActivity.dataBase.deleteData(currentDay,n+m);
											}
											break;
										case 2:
											MainActivity.dataBase.deleteData(currentDay,n-1);
											MainActivity.dataBase.deleteData(currentDay,n);
											for(int m=2;m<n1;m++){
												MainActivity.dataBase.deleteData(currentDay,n+m-1);
											}
											break;
										case 3:
											for(int m=n2;m>=0;m--){
												MainActivity.dataBase.deleteData(currentDay,n-m+1);
											}
											break;
										default:
											break;
									}
									cursor[currentDay].requery();
									listViews[currentDay].invalidate();
								}
							}
						});
						builder.create().show();
					}
				}
			});
		}

	}
	//内部类，实现GestureDetector.OnGestureListener接口
	class DetectorGestureListener implements GestureDetector.OnGestureListener{

		@Override
		public boolean onDown(MotionEvent e) {
			return false;
		}

		//当用户在触屏上“滑过”时触发此方法
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
							   float velocityY) {
			int i = tabHost.getCurrentTab();
			//第一个触点事件的X坐标值减去第二个触点事件的X坐标值超过FLIP_DISTANCE，也就是手势从右向左滑动
			if(e1.getX() - e2.getX() > FLIP_DISTANCE){
				if(i<6)
					tabHost.setCurrentTab(i+1);
				//	float currentX = e2.getX();
				//	listViews[i].setRight((int) (inialX - currentX));
				return true;
			}

			//第二个触点事件的X坐标值减去第一个触点事件的X坐标值超过FLIP_DISTANCE，也就是手势从左向右滑动
			else if(e2.getX() - e1.getX() > FLIP_DISTANCE){
				if(i>0)
					tabHost.setCurrentTab(i-1);
				return true;
			}
			return false;
		}

		@Override
		public void onLongPress(MotionEvent e) {

		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
								float distanceX, float distanceY) {
			return false;
		}

		@Override
		public void onShowPress(MotionEvent e) {

		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			return false;
		}

	}


	//覆写Activity中的onTouchEvent方法，将该Activity上的触碰事件交给GestureDetector处理
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	//设置菜单按钮
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	//当点击菜单中的“退出”键时，弹出提示是否退出的对话框
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//创建AlertDialog.Builder对象，该对象是AlterDialog的创建器，AlterDialog用来创建弹出对话框
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if(item.getItemId() == R.id.menu_exit){
			exit(builder);
			return true;
		}
		if(item.getItemId() == R.id.menu_settings){
			Intent intent = new Intent(MainActivity.this, SetActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}


	//子 方法:为主界面添加选项卡
	public void addCard(TabHost.TabSpec spec,String tag,int id,String name){
		spec = tabHost.newTabSpec(tag);
		spec.setContent(id);
		spec.setIndicator(name);
		tabHost.addTab(spec);
	}
	//子方法：用来弹出是否退出程序的对话框，并执行执行是否退出操作
	public void exit(AlertDialog.Builder builder){
		//为弹出的对话框设置标题和内容
		builder.setIcon(R.drawable.ic_launcher);
		builder.setTitle("退出程序");
		builder.setMessage("确定要退出学习助手吗？");
		//设置左边的按钮为“确定”键，并且其绑定监听器，点击后退出
		builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				//退出应用程序，即销毁地所有的activity
				MyApplication.getInstance().exitApp();
			}
		});
		//设置右边的按钮为“取消”键，并且其绑定监听器，点击后仍然停留在当前界面
		builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}
		});
		//创建并显示弹出的对话框
		builder.create().show();
	}
	/*
	 * 为每一个list提供数据适配器
	 */
	@SuppressWarnings("deprecation")
	public SimpleCursorAdapter adapter(int i){
		return new SimpleCursorAdapter(this, R.layout.list_v2,cursor[i],new String[]{"_id","classes","location",
				"teacher","zhoushu"},new int[]{R.id.number,R.id.ltext0,R.id.ltext1,R.id.ltext6,R.id.ltext7} );
	}

	/*
	 * 第一次运行时创建数据库表
	 */
	static class SingleInstance{
		static SingleInstance si;
		private SingleInstance(){
			for(int i=0;i<7;i++){
				dataBase.createTable(i);
			}
		}
		static SingleInstance createTable(){
			if(si==null)
				return si=new SingleInstance();
			return null;
		}
	}
}
