package com.dosse.bwentrain.androidPlayer;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.dosse.bwentrain.sound.backends.android.AndroidSoundBackend;
/**
 * this class interfaces with LibBWEntrainment to control the playback.
 * shows status and receives events from MainActivity
 * @author dosse
 *
 */
public class PlayerController {
	private IRenderer p;
	private TextView status; private SeekBar bar; private Button playPause; private Activity mainUI;
	private Timer t;

	public PlayerController(Activity mainUI,TextView status, SeekBar bar, Button playPause){
		setStatusView(status);
		setProgressView(bar);
		setButton(playPause);
		setMainUI(mainUI);
		t=new Timer();
		PowerManager pm= (PowerManager) mainUI.getSystemService(Context.POWER_SERVICE);
		final WakeLock lock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SINE");
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if(!PlayerController.this.mainUI.hasWindowFocus()) return;
				PlayerController.this.mainUI.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						//update status and stuff
						TextView status=PlayerController.this.status;
						SeekBar bar=PlayerController.this.bar;
						Button playPause=PlayerController.this.playPause;
						Activity mainUI=PlayerController.this.mainUI;
						if(p==null){
							if(!bar.isEnabled()) return; //already disabled
							bar.setEnabled(false);
							bar.setProgress(0);
							status.setText("");
							playPause.setEnabled(false);
							playPause.setBackgroundResource(R.drawable.play_disable);
						}else{
							bar.setEnabled(true);
							if(!bar.isPressed())bar.setProgress((int) (bar.getMax()*(p.getPosition()/p.getLength())));
							status.setText(Utils.toHMS(p.getPosition())+"/"+Utils.toHMS(p.getLength()));
							playPause.setEnabled(true);
							if(p.isPlaying()) playPause.setBackgroundResource(R.drawable.pause); else playPause.setBackgroundResource(R.drawable.play);
						}
						//show persistent notification and acquire wakelock while playing
						NotificationManager notifManager = (NotificationManager) mainUI.getSystemService(Context.NOTIFICATION_SERVICE);
						if (p!=null&&p.isPlaying()) {
							if(lock.isHeld()) return; //already showing notification and holding wakelock
							if(!lock.isHeld()) lock.acquire();
							PendingIntent intent = PendingIntent.getActivity(mainUI.getApplicationContext(), 0,new Intent(mainUI.getApplicationContext(),MainActivity.class), 0);
							Notification.Builder builder = new Notification.Builder(mainUI.getApplicationContext());
							builder.setContentTitle(mainUI.getTitle());
							builder.setContentText(p.getPreset().getTitle());
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
							if(!lock.isHeld()) return; //already removed notification and wakelock
							if(lock.isHeld()) lock.release();
							notifManager.cancel(0);
						}
					}
				});
			}
		}, 0, 100);
	}
	
    
	public void setStatusView(TextView status){this.status=status;}
	public void setProgressView(SeekBar bar){this.bar=bar; bar.setMax(1000);}
	public void setButton(Button playPause){this.playPause=playPause;}
	
	public void setPreset(Preset preset){
		if(p!=null){p.stopPlaying();}
		try {
			p=new IsochronicRenderer(preset, new AndroidSoundBackend(44100,1), -1);
			p.play();
		} catch (Exception e) {
		}
	}
	
	public void play(){
		if(p!=null) p.play();
	}
	public void pause(){
		if(p!=null) p.pause();
	}
	public boolean isPlaying(){
		if(p!=null) return p.isPlaying(); else return false;
	}
	public void setMainUI(Activity mainUI) {
		this.mainUI=mainUI;
	}
	public void setPosition(float f){
		if(p==null) return;
		p.setPosition((f<0?0:f>1?1:f)*p.getLength());
	}
	
}
