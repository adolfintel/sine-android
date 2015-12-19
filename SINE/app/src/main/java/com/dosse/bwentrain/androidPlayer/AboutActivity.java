package com.dosse.bwentrain.androidPlayer;

import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Timer;
import java.util.TimerTask;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.os.Build;

public class AboutActivity extends AppCompatActivity {

    private Timer t;
    private static float[] hsv=new float[]{210,0.85f,0.58f};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ((Button)findViewById(R.id.bugReport)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try{Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("message/rfc822");
                intent.putExtra(Intent.EXTRA_EMAIL, getString(R.string.reportBugAddress));
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.reportBugSubject));
                intent.putExtra(Intent.EXTRA_TEXT, "\n\n\n\n\n\n---------------------------\nDevice model: "+Build.MODEL+"\nSDK Version: "+Build.VERSION.SDK_INT+"\nSINE Version: "+getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);

                startActivity(Intent.createChooser(intent, getString(R.string.reportBug)));}catch(Throwable t){}
            }
        });
    }

    @Override
    protected void onResume(){
        //technicolor
        if(t!=null){ t.cancel(); t=null;}
        t=new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hsv[0]=(hsv[0]+0.15f)%360;
                        RelativeLayout container=(RelativeLayout)findViewById(R.id.aboutRelativeLayout);
                        if(container!=null)container.setBackgroundColor(Color.HSVToColor(hsv));
                    }
                });
            }
        }, 0, 20);
        new EasterEgg();
        super.onResume();
    }

    @Override
    protected void onStop(){
        if(t!=null){ t.cancel(); t=null;}
        super.onStop();
    }

    //this class contains code for the application's easter egg. it is deliberately obfuscated. does not work on older versions of android
    private class EasterEgg{
        private int steam=R.id.imageView1;
        private final String hk47="You've got me burning, You've got me burning, You've got me burning, in the third degree.data:audio/mpeg;base64,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".substring(89);
        public EasterEgg(){
            ImageView bennett=(ImageView) findViewById(steam);
            bennett.setOnClickListener(new OnClickListener() {
                int arnold=0;
                long fucking=0;
                boolean scwarzenegger=false;
                @Override
                public void onClick(View v) {
                    if(isPissed(scwarzenegger)){
                        terminate("YOU");
                    }else{
                        if(System.nanoTime()-fucking<=(Long.MAX_VALUE&0x11E1A300L)) arnold++; else arnold=Integer.MAX_VALUE&1;
                        fucking=System.nanoTime();
                        if(arnold==(0x56000-0x55FFE)){
                            scwarzenegger=true;
                        }
                    }
                }
            });
        }
        private boolean isPissed(Object person){
            return (person instanceof Boolean)?((Boolean)person).booleanValue():false;
        }
        private void terminate(Object person){
            hsv[0]=(hsv[0]+30)%360;
            final MediaPlayer p=MediaPlayer.create(getApplicationContext(), Uri.parse(hk47));
            if(p==null) return;
            p.setOnCompletionListener(new OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    p.release();
                };
            });
            p.start();
        }
    }

}
