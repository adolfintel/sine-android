package com.dosse.bwentrain.androidPlayer;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.dosse.bwentrain.sound.backends.android.AndroidSoundBackend;
/**
 * this class interfaces with the PlayerControllerService to control the playback.
 * shows status and receives events from MainActivity.
 * This used to do what PlayerControllerService does now, so now it's mostly a shim
 * @author dosse
 *
 */
public class PlayerController extends BroadcastReceiver {

	private TextView status; private SeekBar bar; private Button playPause; private Activity mainUI;
	private WakeLock lock=null;

	private boolean isPlaying=false;

	public PlayerController(Activity mainUI,TextView status, SeekBar bar, Button playPause){
		setStatusView(status);
		setProgressView(bar);
		setButton(playPause);
		setMainUI(mainUI);
		IntentFilter f=new IntentFilter();
		f.addAction("com.dosse.bwenrtain.androidPlayer.SineSvcReport");
		LocalBroadcastManager.getInstance(mainUI).registerReceiver(this,f);
		PowerManager pm= (PowerManager) mainUI.getSystemService(Context.POWER_SERVICE);
		final WakeLock lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SINE");
	}
	
    
	public void setStatusView(TextView status){this.status=status;}
	public void setProgressView(SeekBar bar){this.bar=bar; bar.setMax(1000);}
	public void setButton(Button playPause){this.playPause=playPause;}
	
	public void setPreset(Preset preset){
		Intent i = new Intent(mainUI, PlayerControllerService.class);
		i.putExtra("command","setPreset");
		i.putExtra("preset",preset);
		mainUI.startService(i);
	}
	
	public void play(){
		Intent i = new Intent(mainUI, PlayerControllerService.class);
		i.putExtra("command","play");
		mainUI.startService(i);
	}
	public void pause(){
		Intent i = new Intent(mainUI, PlayerControllerService.class);
		i.putExtra("command","pause");
		mainUI.startService(i);
	}
	public boolean isPlaying(){
		return isPlaying;
	}
	public void setMainUI(Activity mainUI) {
		this.mainUI=mainUI;
	}
	public void setPosition(float f){
		Intent i = new Intent(mainUI, PlayerControllerService.class);
		i.putExtra("command","setPosition");
		i.putExtra("position",f);
		mainUI.startService(i);
	}

	public void killService(){
		Intent i = new Intent(mainUI, PlayerControllerService.class);
		mainUI.stopService(i);
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		final boolean isPlaying=intent.getBooleanExtra("isPlaying",false);
		this.isPlaying=isPlaying;
		final boolean isPresetLoaded=intent.getBooleanExtra("isPresetLoaded",false);
		final float position=intent.getFloatExtra("position",0);
		final float length=intent.getFloatExtra("length",0);
		final String presetTitle=intent.getStringExtra("title");

		if(!mainUI.hasWindowFocus()) return;
		mainUI.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				//update status and stuff
				TextView status=PlayerController.this.status;
				SeekBar bar=PlayerController.this.bar;
				Button playPause=PlayerController.this.playPause;
				Activity mainUI=PlayerController.this.mainUI;
				if(!isPresetLoaded){
					if(!bar.isEnabled()) return; //already disabled
					bar.setEnabled(false);
					bar.setProgress(0);
					status.setText("");
					playPause.setEnabled(false);
					playPause.setBackgroundResource(R.drawable.play_disable);
				}else{
					bar.setEnabled(true);
					if(!bar.isPressed())bar.setProgress((int) (bar.getMax()*(position/length)));
					status.setText(Utils.toHMS(position)+"/"+Utils.toHMS(length));
					playPause.setEnabled(true);
					if(isPlaying) playPause.setBackgroundResource(R.drawable.pause); else playPause.setBackgroundResource(R.drawable.play);
				}
				//show persistent notification and acquire wakelock while playing
				NotificationManager notifManager = (NotificationManager) mainUI.getSystemService(Context.NOTIFICATION_SERVICE);
				if (isPresetLoaded&&isPlaying) {
					if(lock!=null) {
						if (lock.isHeld()) return; //already showing notification and holding wakelock
						if (!lock.isHeld()) lock.acquire();
					}
					PendingIntent intent = PendingIntent.getActivity(mainUI.getApplicationContext(), 0,new Intent(mainUI.getApplicationContext(),MainActivity.class), 0);
					Notification.Builder builder = new Notification.Builder(mainUI.getApplicationContext());
					builder.setContentTitle(mainUI.getTitle());
					builder.setContentText(presetTitle);
					builder.setContentIntent(intent);
					builder.setSmallIcon(R.drawable.logo_notif);
					builder.setOngoing(true);
					Notification notification=null;
					if(Build.VERSION.SDK_INT<16){
						notification=builder.getNotification();
					}else{
						notification = builder.build();
					}
					notifManager.notify(0, notification);
				} else {
					if(lock!=null){
						if(!lock.isHeld()) return; //already removed notification and wakelock
						if(lock.isHeld()) lock.release();
					}
					notifManager.cancel(0);
				}
			}
		});
	}

}
