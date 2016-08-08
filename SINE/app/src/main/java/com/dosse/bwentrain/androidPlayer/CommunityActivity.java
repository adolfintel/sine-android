package com.dosse.bwentrain.androidPlayer;

import android.app.NotificationManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.DownloadListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class CommunityActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{
    private WebView w;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, null, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView view = (NavigationView) findViewById(R.id.nav_view);
        view.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (w!=null&&w.canGoBack()) {
                w.goBack();
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        final WebView w = (WebView) findViewById(R.id.browser);
        this.w=w;
        w.getSettings().setSupportZoom(false);
        w.getSettings().setJavaScriptEnabled(true);
        w.setWebViewClient(new WebViewClient() {
            private boolean firstLoad = true;

            public void onLoadResource(WebView view, String url) {
                if (firstLoad) {
                    w.setVisibility(View.INVISIBLE);
                } // show loading animation
            }

            public void onPageFinished(WebView view, String url) {
                if (firstLoad) {
                    w.setVisibility(View.VISIBLE);
                    firstLoad = false;
                } // hide loading animation
            }

            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Toast.makeText(getApplicationContext(),getString(R.string.load_error_community),Toast.LENGTH_SHORT).show();
                if(firstLoad) finish();
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.toLowerCase().startsWith("http://sine.adolfintel.com/forum") || url.toLowerCase().startsWith("http://isochronic.io/forum")) return false;
                if (url.toLowerCase().startsWith("http://sine.adolfintel.com/goto.php") || url.toLowerCase().startsWith("http://sine.adolfintel.com/presets.php") || url.toLowerCase().startsWith("http://isochronic.io/goto.php") || url.toLowerCase().startsWith("http://isochronic.io/presets.php")) { //link to a preset, or to the preset page
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
                        Intent i = new Intent(CommunityActivity.this, BrowserActivity.class);
                        i.putExtra("path", newUrl);
                        startActivity(i);
                    } catch (Throwable t) {
                        Toast.makeText(getApplicationContext(), "" + t, Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), getString(R.string.community_invalid_presetId), Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                //unknown url, open in browser
                Toast.makeText(getApplicationContext(), getString(R.string.community_unknown_url), Toast.LENGTH_SHORT).show();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
                return true;
            }
        });
        w.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //shitty workaround to prevent auto zooming on some devices
                w.zoomOut();
                return false;
            }
        });
        w.loadUrl(getIntent().getStringExtra("path"));
        w.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(final String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                if(!url.toLowerCase().endsWith(".sin")){ Toast.makeText(getApplicationContext(), getString(R.string.unknown_download), Toast.LENGTH_SHORT).show(); return;} //only .sin presets can be downloaded
                Thread t = new Thread() {
                    public void run() {
                        try {
                            URL u = new URL(url);
                            String name = u.getFile();
                            name = name.substring(name.lastIndexOf("/") + 1, name.length());
                            URLConnection c = u.openConnection();
                            c.connect();
                            InputStream in = new BufferedInputStream(u.openStream());
                            FileOutputStream out = getApplicationContext().openFileOutput(name, MODE_PRIVATE);
                            for (; ; ) {
                                byte[] buff = new byte[1024];
                                try {
                                    int l = in.read(buff);
                                    out.write(buff, 0, l);
                                } catch (Exception e) {
                                    break;
                                }
                            }
                            in.close();
                            out.flush();
                            out.close();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), getString(R.string.download_success), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (Throwable t) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), getString(R.string.download_fail), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                };
                t.start();
                try {
                    t.join();
                } catch (InterruptedException e) {
                }
            }
        });
        super.onPostCreate(savedInstanceState);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig){
        Bundle b=new Bundle();
        w.saveState(b);
        super.onConfigurationChanged(newConfig);
        w.restoreState(b);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.nav_home){
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        }
        if(id==R.id.nav_presets){
            startActivity(new Intent(this,BrowserActivity.class).putExtra("path", getString(R.string.presets_url)));
        }
        if(id==R.id.nav_settings){
            startActivity(new Intent(this,SettingsActivity.class));
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume(){
        super.onResume();
        ((NavigationView) findViewById(R.id.nav_view)).getMenu().getItem(2).setChecked(true);
    }
}
