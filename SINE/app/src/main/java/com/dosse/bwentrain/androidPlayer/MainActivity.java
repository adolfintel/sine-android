package com.dosse.bwentrain.androidPlayer;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings.Secure;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Button;

public class MainActivity extends AppCompatActivity  {
    private static PlayerController pc; //PlayerController interfaces with LibBWEntrainment to control the playback
    static{new IsochronicRenderer(null, null, 0).stopPlaying();} //preload IsochronicRenderer
    private Menu optionsMenu; //pointer to optionsMenu, used by AdsController to remove purchase option if app is licensed
    private int adsH;

    /**
     * checks license and loads ads if needed.
     * also, great example of overhead: 70% of the code is there only because android wants it
     * I also apologize for all the try-catches, but it seems to be quite unstable at times
     * @author dosse
     *
     */
    private class AdsController extends Thread{
        public void run(){
            while(optionsMenu==null) //wait for options menu to be created. this workaround is cheaper than an airPad 4
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {}
            //there you go, fuckers: the CHEAPEST, CRAPPIEST license check ever, because LVL doesn't let me check the license of other apps (read pro key) anymore. thanks google, you fucktards.
            if (getPackageManager().checkSignatures(getPackageName(), "com.dosse.bwentrain.androidPlayerKey")== PackageManager.SIGNATURE_MATCH) {
                removeBuyOption();
            }else{
                loadAds();
                //let's tell ad block users to support us
                try{
                    if(isPackageInstalled("tw.fatminmin.xposed.minminguard")||isPackageInstalled("org.adaway")||isPackageInstalled("org.adblockplus.android")){
                        try{openFileInput("minmin.fag");}catch (Throwable t) {
                            openFileOutput("minmin.fag", MODE_PRIVATE).close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    startActivity(new Intent(MainActivity.this, MinMinFaggot.class));
                                }
                            });
                        }
                    }
                }catch (Throwable t){Log.e("SINE","Can't check for ad blockers "+t);}
            }
            //and they complain about piracy in android...
        }
        public boolean isPackageInstalled(String p){
            try {
                getPackageManager().getPackageInfo(p, PackageManager.GET_META_DATA);
                return true;
            } catch (Throwable t) {
                return false;
            }

        }
        private void loadAds(){
            try{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            ((AdView)findViewById(R.id.banner)).getLayoutParams().height=adsH;
                            ((AdView)findViewById(R.id.banner)).loadAd(new AdRequest.Builder().build());
                        }catch(Throwable t){}
                    }
                });
            }catch(Throwable t){}
        }
        private void removeBuyOption(){
            try{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try{optionsMenu.getItem(0).setVisible(false);}catch(Throwable t){}
                    }
                });
            }catch(Throwable t){}
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //hide ads, will be shown again if the app is not licensed
        adsH=((AdView)findViewById(R.id.banner)).getLayoutParams().height;
        ((AdView)findViewById(R.id.banner)).getLayoutParams().height=0;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        new AdsController().start();
        try{
            openFileInput("material.run");
            //material.run exists, go straight to the app
        }catch(Throwable t){
            Intent i=new Intent(MainActivity.this,IntroActivity.class);
            try{
                openFileInput("first.run");
                //first.run exists, the user just updated the app, show update intro
                i.putExtra("update", true);
            }catch (Throwable t1){
                //first.run does not exist, show intro
                i.putExtra("update",false);
            }
            startActivity(i);
            try {
                openFileOutput("material.run", MODE_PRIVATE).close();
            } catch (Throwable t1) {
            }
        }
        super.onPostCreate(savedInstanceState);
    }

    @Override
    protected void onPause(){
        try{((AdView)findViewById(R.id.banner)).pause();}catch(Throwable t){}
        super.onPause();
    }
    @Override
    protected void onDestroy() {
        try{((AdView)findViewById(R.id.banner)).destroy();}catch(Throwable t){}
        super.onDestroy();
    }
    @Override
    protected void onResume() {
        try{((AdView)findViewById(R.id.banner)).resume();}catch(Throwable t){}
        TextView status=(TextView)findViewById(R.id.textView1);
        SeekBar bar=(SeekBar)findViewById(R.id.seekBar1);
        Button playPause=(Button)findViewById(R.id.button1);
        //add action listener for seek bar
        bar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!fromUser) return;
                pc.setPosition((float) progress / (float) seekBar.getMax());
            }
        });
        //add action listener for play/pause button
        playPause.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (pc.isPlaying()) pc.pause();
                else pc.play();
            }
        });
        if(pc==null){
            pc=new PlayerController(this,status, bar, playPause); //playercontroller not initialized, do it now
        }else{
            //playercontroller already initialized, give it pointers to the newly created views
            pc.setMainUI(this);
            pc.setStatusView(status);
            pc.setProgressView(bar);
            pc.setButton(playPause);
        }
        populatePresetList();
        super.onResume();
    }
    @Override
    //receives intents from DetailsActivity, and loads the selected preset. if no path is set, then the intent came from some other activity and does nothing
    public void onNewIntent(Intent i){
        String request=i.getStringExtra("path");
        if(request!=null){
            Preset p=loadPreset(request);
            if(p!=null) pc.setPreset(p); else Toast.makeText(getApplicationContext(), R.string.load_error, Toast.LENGTH_SHORT).show();
        }
        super.onNewIntent(i);
    }
    private Preset loadPreset(String f){
        InputStream p = null;
        Preset x=null;
        try {
            if(new File(f).exists()) p=new FileInputStream(f); else p=openFileInput(f);
            if(p==null) throw new Exception("Can't open "+f);
            //read xml document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(p);
            doc.getDocumentElement().normalize();
            //parse it
            x = new Preset(doc.getDocumentElement());
        } catch (Throwable t) {
            //corrupt or not a preset file
            Log.e("SINE",f+" invalid because "+t.toString());
        } finally{
            if(p!=null)
                try {
                    p.close();
                } catch (IOException e) {
                }
        }
        return x;
    }
    private void populatePresetList(){
        final ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
        ArrayList<String> fileList=new ArrayList<String>();
        String[] downloads=fileList(); //get all files downloaded from the preset sharing platform
        for(String s:downloads) //add all downloaded presets
            if(s.toLowerCase().endsWith(".sin")){
                fileList.add(s);
            }
        File sdcard = Environment.getExternalStorageDirectory(); //get all manually added files in the root of the sdcard
        File[] sd=sdcard.listFiles();
        if(sd!=null){
            for(File f:sd){ //add all manually added presets
                if(f.isFile()&&f.getName().toLowerCase().endsWith(".sin")) fileList.add(f.getAbsolutePath());
            }
        }
        //add link to preset sharing platform
        HashMap<String, String> siteLink = new HashMap<String, String>();
        siteLink.put("title",getString(R.string.download_presets));
        siteLink.put("author",getString(R.string.theyre_free));
        siteLink.put("description","");
        siteLink.put("path", getString(R.string.presets_url));
        list.add(siteLink);
        //generate items for ListView
        for(String f:fileList){
            Preset p=loadPreset(f);
            if(p==null) continue;
            HashMap<String, String> item = new HashMap<String, String>();
            item.put("title",p.getTitle());
            item.put("author",p.getAuthor());
            item.put("description",p.getDescription());
            item.put("length",""+Utils.toHMS(p.getLength()));
            item.put("loop",""+(p.loops()?Utils.toHMS(p.getLoop()):""));
            item.put("path", f);
            list.add(item);
        }

        //insert all into ListView
        ListView files=(ListView) findViewById(R.id.listView1);
        files.setAdapter(new SimpleAdapter(this, list,android.R.layout.simple_list_item_2, new String[] { "title","author" }, new int[] { android.R.id.text1,android.R.id.text2 }));
        
        //tap on item
        files.setOnItemClickListener(new OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                HashMap<String, String> item=list.get(position);
                if(item.get("path").startsWith("http://")){ //link
                    Intent i=new Intent(MainActivity.this, BrowserActivity.class);
                    i.putExtra("path",item.get("path"));
                    startActivity(i);
                }else{ //preset
                    Intent i=new Intent(MainActivity.this, DetailsActivity.class);
                    i.putExtra("title", item.get("title"));
                    i.putExtra("author", item.get("author"));
                    i.putExtra("description", item.get("description"));
                    i.putExtra("length",item.get("length"));
                    i.putExtra("loop",item.get("loop"));
                    i.putExtra("path", item.get("path"));
                    startActivity(i);
                }
            }});
        //long tap on item
        files.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view,
                                           int position, long id) {
                HashMap<String, String> item=list.get(position);
                final String path=item.get("path");
                if(path.startsWith("http://")) return true; //can't delete links
                //delete preset
                //ask for confirm
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which==DialogInterface.BUTTON_POSITIVE){	//delete confirmed
                            if(new File(path).exists()){ //delete file on sdcard
                                new File(path).delete();
                            } else {//delete from app data
                                deleteFile(path);
                            }
                            populatePresetList(); //repopulate preset list
                        }
                    }
                };
                builder.setMessage(getString(R.string.delete_confirm)+" \""+item.get("title")+"\"?").setPositiveButton(getString(R.string.yes), l).setNegativeButton(getString(R.string.no), l).show();
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        optionsMenu=menu;
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        try{
            if(id==R.id.purchase){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.purchase_url))));
            }
            if(id==R.id.playStore){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.playStore_url))));
            }
            if(id==R.id.fb){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.fb_url))));
            }
            if(id==R.id.tw){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tw_url))));
            }
            if(id==R.id.gp){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.gp_url))));
            }
            if(id==R.id.web){
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.website_url))));
            }
            if(id==R.id.about){
                startActivity(new Intent(this,AboutActivity.class));
            }
            if(id==R.id.exit){
                //cancel notifications and quit
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(0);
                System.exit(0);
            }
        }catch(Throwable t){
            //ignore all errors
        }
        return super.onOptionsItemSelected(item);
    }

}
