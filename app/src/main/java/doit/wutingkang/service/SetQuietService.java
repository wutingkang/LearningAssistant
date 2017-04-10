package doit.wutingkang.service;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.IBinder;

import java.util.Timer;
import java.util.TimerTask;

import temp.DataBase;
import temp.ShareMethod;


public class SetQuietService extends Service {

	//保存时间，timeForClass[day][row][time]表示第day+1个tab选项卡中的第row+1个行中第time+1个表示时间的字符串
	String[][][] timeForClass = new String[7][12][2];

	//取得数据库，并定义保存每张表数据的cursor集合
	DataBase db = new DataBase(SetQuietService.this);
	Cursor[] cursor = new Cursor[7];

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
		final AudioManager audioManager = (AudioManager)getSystemService(Service.AUDIO_SERVICE);
		//获取手机之前设置好的铃声模式
		final int primaryRingerMode = audioManager.getRingerMode();

		//每隔一分钟从数据库中取以此数据，获得一次当前的时间，并与用用户输入的上下课时间比较，如果相等，则执行相应的静音或者恢复铃声操作
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {

			//取出数据库中每日的数据，保存在cursor数组中
			for(int i = 0; i < 7; i++){
				cursor[i] = db.select(i);
			}

			//从数据库取出用户输入的上课和下课时间，用来设置上课自动静音
			for(int day = 0; day < 7; day++){
				for(int row = 0; row < 12; row++){
					cursor[day].moveToPosition(row);
					for(int time = 0; time < 2; time++){
						timeForClass[day][row][time] = cursor[day].getString(time+5);
					}
					if(! timeForClass[day][row][0].equals(""))
						timeForClass[day][row][0] = timeForClass[day][row][0].substring(timeForClass[day][row][0].indexOf(":")+2);
				}
			}

			//获取当前的是星期几
			int currentDay = ShareMethod.getWeekDay();
			for(int j = 0; j < 12; j++){
				//获取手机当前的铃声模式
				int currentRingerMode = audioManager.getRingerMode();

				//上课时间到
				if(timeForClass[currentDay][j][0].equals(ShareMethod.getTime24()) && currentRingerMode != AudioManager.RINGER_MODE_VIBRATE){
					audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
					//System.out.println("class is on");
				}

				//下课时间到
				/*
				* 开启InClassRoomQuietService后，只要进入教室的范围就绝对静音，
				* 尽管课程表有课间恢复铃声的功能，但因为有时候即使翘课了，但是换个教室继续学习，课间的时候还在教室内，不能随便回复铃声。
				* 所以课程表恢复铃声的功能只在 *没有开启InClassRoomQuietService* 或者 *开启了InClassRoomQuietService但教室位置外* 的时候起作用。
				* 这时候 IS_IN_CLASSROOM 都为 false;
				* */
				if(false == InClassRoomQuietService.IS_IN_CLASSROOM &&
		  		   timeForClass[currentDay][j][1].equals(ShareMethod.getTime24()) && currentRingerMode == AudioManager.RINGER_MODE_VIBRATE){
					audioManager.setRingerMode(primaryRingerMode);
   				    //System.out.println("class is over");
				}

			}

			}
		}, 0, 60000);
		return super.onStartCommand(intent, flags, startId);
	}
}
