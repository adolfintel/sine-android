package com.dosse.bwentrain.androidPlayer;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.dosse.bwentrain.core.Envelope;
import com.dosse.bwentrain.core.Preset;
import com.dosse.bwentrain.renderers.isochronic.IsochronicRenderer;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class MainActivity extends AppCompatActivity  implements NavigationView.OnNavigationItemSelectedListener {
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

        NavigationView view = (NavigationView) findViewById(R.id.nav_view);
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        view.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        view.getMenu().getItem(0).setChecked(true);

        //hide ads, will be shown again if the app is not licensed
        adsH=((AdView)findViewById(R.id.banner)).getLayoutParams().height;
        ((AdView)findViewById(R.id.banner)).getLayoutParams().height=0;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        new AdsController().start();
        try {
            if (getIntent().getData() != null) { //called from an url
                Uri u = getIntent().getData();
                if(u==null) throw new Exception();
                String url = u.getScheme() + ":" + u.getEncodedSchemeSpecificPart();
                if (url.toLowerCase().startsWith("http://sine.adolfintel.com/forum") || url.toLowerCase().startsWith("http://isochronic.io/forum"))
                    startActivity(new Intent(this, CommunityActivity.class).putExtra("path", url)); //throw the url to the community activity
                else if (url.toLowerCase().startsWith("http://sine.adolfintel.com/goto.php") || url.toLowerCase().startsWith("http://sine.adolfintel.com/presets.php") || url.toLowerCase().startsWith("http://isochronic.io/goto.php") || url.toLowerCase().startsWith("http://isochronic.io/presets.php")) { //link to a preset, or to the preset page
                    //attempt to convert to mobile and localized link
                    try {
                        String newUrl = getString(R.string.presets_url);
                        //find preset id (if present) and add it to the new url
                        String q = new URL(url).getQuery();
                        if (q != null) {
                            String[] query = q.split("\\?");
                            for (String s : query) {
                                String[] ss = s.split("=");
                                if (ss[0].trim().equalsIgnoreCase("id")) {
                                    newUrl += "%26id=" + Integer.parseInt(ss[1].trim());
                                    break;
                                }
                            }
                        }
                        Intent i = new Intent(this, BrowserActivity.class);
                        i.putExtra("path", newUrl);
                        startActivity(i);
                    } catch (Throwable t) {
                        Toast.makeText(getApplicationContext(), getString(R.string.community_invalid_presetId), Toast.LENGTH_SHORT).show();
                    }
                }else throw new Exception(); //ignore unknown urls
            } else throw new Exception();
        }catch(Throwable t){
        }
        try{
            openFileInput("material.run");
            //material.run exists, go straight to the app
        }catch(Throwable t){
            Intent i=new Intent(MainActivity.this,IntroActivity.class);
            try{
                openFileInput("first.run");
                //first.run exists, the user just updated the app to the material design version, show update intro
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
        ((NavigationView) findViewById(R.id.nav_view)).getMenu().getItem(0).setChecked(true);
        super.onResume();
    }
    @Override
    //receives intents from DetailsActivity, and loads the selected preset. if no path is set, then the intent came from some other activity and does nothing
    public void onNewIntent(Intent i){
        String request=i.getStringExtra("path");
        if(request!=null){
            Preset p=loadPreset(request);
            if(pc==null){ //I got some bug reports where the PlayerController was null. This should not be possible, and I was not able to replicate the bug so I'll just try to restart the activity. Hopefully it will fix the bug?
                startActivity(new Intent(this,MainActivity.class).putExtra("path",request).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
                finish();
            }
            if(p!=null) pc.setPreset(p); else Toast.makeText(getApplicationContext(), R.string.load_error, Toast.LENGTH_SHORT).show();
            try { //try to disable power saving again, in case the user denied the first prompt
                Intent intent = new Intent();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }catch(Throwable t){}
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
            if(getSharedPreferences("SINE",Context.MODE_PRIVATE).getBoolean("noise_switch",false)){
                //if noise is disabled, remove it
                Envelope e=x.getNoiseEnvelope();
                while(e.getPointCount()!=1)e.removePoint(1);
                e.setVal(0,0);
            }
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

        //generate items for ListView
        //add link to preset sharing platform
        HashMap<String, String> siteLink = new HashMap<String, String>();
        siteLink.put("title", getString(R.string.download_presets));
        siteLink.put("author", getString(R.string.theyre_free));
        siteLink.put("description", "");
        siteLink.put("path", getString(R.string.presets_url));
        list.add(siteLink);

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
            if(id==R.id.settings){
                startActivity(new Intent(this,SettingsActivity.class));
            }
            if(id==R.id.about){
                startActivity(new Intent(this,AboutActivity.class));
            }
            if(id==R.id.exit){
                //cancel notifications, kill service and quit
                ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancel(0);
                if(pc!=null)pc.killService();
                Thread.sleep(100);
                System.exit(0);
            }
        }catch(Throwable t){
            //ignore all errors
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.nav_presets){
            startActivity(new Intent(this, BrowserActivity.class).putExtra("path", getString(R.string.presets_url)));
        }
        if(id==R.id.nav_community){
            startActivity(new Intent(this,CommunityActivity.class).putExtra("path",getString(R.string.forum_url)));
        }
        if(id==R.id.nav_settings){
            startActivity(new Intent(this,SettingsActivity.class));
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
