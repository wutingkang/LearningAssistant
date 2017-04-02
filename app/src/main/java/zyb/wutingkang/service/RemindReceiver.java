package zyb.wutingkang.service;

import java.util.Calendar;
import java.util.Date;
import temp.DataBase;
import temp.ShareMethod;
import zyb.wutingkang.edits.RemindActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;

public class RemindReceiver extends BroadcastReceiver {

	//定义保存每张表数据的cursor集合
	Cursor[] cursor = new Cursor[7];
	//保存时间，timeForClass[day][row][hm]表示第day+1个tab选项卡中的第row+1个行中用户输入的第一个（即课程开始）时间拆分为时和分
	//hm为0时表示时，1表示分，2时代表时和分的组合，即未拆分前的字符串
	String[][][] timeForClass = new String[7][12][3];
	//将temp数组中的字符串转化为相应的正数，这里去掉了时和分的组合
	int[][][] startTime = new int[7][12][2];
	private int advanceTime;

	@Override
	public void onReceive(Context arg0, Intent arg1) {

		DataBase db = new DataBase(arg0);
		//取出数据库中每日的数据，保存在cursor数组中
		for(int i = 0; i < 7; i++){
			cursor[i]=db.select(i);
		}

		//从数据库取出用户输入的上课的时和分，用来设置课前提醒
		for(int day = 0; day < 7; day++){
			for(int row = 0; row < 12; row++){
				cursor[day].moveToPosition(row);
				timeForClass[day][row][2] = cursor[day].getString(5);
				if(! timeForClass[day][row][2].equals("")){
					timeForClass[day][row][2] = timeForClass[day][row][2].substring(timeForClass[day][row][2].indexOf(":")+2);
					timeForClass[day][row][0] = timeForClass[day][row][2].substring(0, timeForClass[day][row][2].indexOf(":"));
					timeForClass[day][row][1] = timeForClass[day][row][2].substring(timeForClass[day][row][2].indexOf(":")+1);
				}
				else{
					timeForClass[day][row][0] = timeForClass[day][row][1] = "0";
				}
				for(int hm = 0; hm < 2; hm++){
					startTime[day][row][hm] = Integer.parseInt(timeForClass[day][row][hm]);
				}
			}
		}

		//从该context中启动提醒的activity，根据SDK文档的说明，需要加上addFlags()一句
		Intent remindIntent = new Intent(arg0, RemindActivity.class);
		remindIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		//获取提前提醒的时间值,如果没有获取到则取默认值30分钟
		//int advanceTime = arg1.getIntExtra("anvance_remindtime", 20);
		//这里模式一定要设置为MODE_MULTI_PROCESS，否则即使相应的xml文件中数据有更新，RemindReceiver中也不能获取更新后的数据，而是一直获取上次的数据， 除非清空缓存
		SharedPreferences pre = arg0.getSharedPreferences("time", Context.MODE_MULTI_PROCESS);
		advanceTime = pre.getInt("time_choice", 30);
		int currentday = ShareMethod.getWeekDay();
		//System.out.println(advanceTime);

		Calendar c = Calendar.getInstance();
		//获取当前的时和分
		int currentHourOfDay = c.get(Calendar.HOUR_OF_DAY);
		int currentMinute = c.get(Calendar.MINUTE);

		//定义一个标志位，用来排除掉重复的提醒
		boolean flag = true;
		//循环判断当天的课前提醒
		for(int i = 0; i < 12; i++){
			if(! (startTime[currentday][i][0] == 0 && startTime[currentday][i][1] == 0)){
				//将calendar的时和分设置为提醒时候的时和分
				c.set(Calendar.HOUR_OF_DAY, startTime[currentday][i][0]);
				c.set(Calendar.MINUTE, startTime[currentday][i][1]);
				long remind_time = c.getTimeInMillis()- advanceTime * 60 * 1000;
				Date date = new Date(remind_time);
				c.setTime(date);

				//获取设置的提醒的时和分
				int hourOfDay = c.get(Calendar.HOUR_OF_DAY);
				int minute = c.get(Calendar.MINUTE);

				//如果到了设定的提醒时间，就启动提醒的activity
				if(hourOfDay == currentHourOfDay && minute == currentMinute){
					if(flag){
						arg0.startActivity(remindIntent);
						//System.out.println("time remind" + i);
						flag = false;
					}
				}else{
					flag = true;
				}
			}
		}
	}
}
