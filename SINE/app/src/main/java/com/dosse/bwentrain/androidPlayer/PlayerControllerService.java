package com.dosse.bwentrain.androidPlayer;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.IRenderer;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.dosse.bwentrain.sound.backends.android.AndroidSoundBackend;

/**
 * Created by Federico on 2016-11-03.
 */

public class PlayerControllerService extends Service{

    private static IRenderer p;
    private static Thread intentSender=null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if(p!=null){p.stopPlaying(); p=null;}
        if(intentSender!=null){intentSender.interrupt(); intentSender=null;}
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        if(intentSender==null||!intentSender.isAlive()){
            intentSender=new Thread(){
                public void run(){
                    for(;;){
                        try{sleep(200);}catch(Throwable t){return;}
                        Intent report=new Intent("com.dosse.bwenrtain.androidPlayer.SineSvcReport");
                        report.putExtra("isPresetLoaded",p!=null);
                        report.putExtra("isPlaying",p!=null&&p.isPlaying());
                        report.putExtra("position",p!=null?p.getPosition():0);
                        report.putExtra("length",p!=null?p.getLength():0);
                        report.putExtra("title",p!=null?p.getPreset().getTitle():"");
                        LocalBroadcastManager.getInstance(PlayerControllerService.this).sendBroadcast(report);
                    }
                }
            };
            intentSender.start();
        }
        String cmd=intent.getStringExtra("command");
        if(cmd.equals("setPreset")){
            if(p!=null){p.stopPlaying();}
            try {
                p=new IsochronicRenderer((Preset)(intent.getSerializableExtra("preset")), new AndroidSoundBackend(44100,1), -1);
                p.play();
            } catch (Exception e) {
            }
        }
        if(cmd.equals("setPosition")&&p!=null){
            float f=intent.getFloatExtra("position",0);
            p.setPosition((f<0?0:f>1?1:f)*p.getLength());
        }
        if(cmd.equals("pause")&&p!=null){
            p.pause();
        }
        if(cmd.equals("play")&&p!=null){
            p.play();
        }
        return START_NOT_STICKY;
    }
}
