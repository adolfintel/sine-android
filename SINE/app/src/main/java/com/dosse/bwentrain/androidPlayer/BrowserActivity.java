package com.dosse.bwentrain.androidPlayer;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

public class BrowserActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        NavigationView view = (NavigationView) findViewById(R.id.nav_view);
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        view.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        view.getMenu().getItem(1).setChecked(true);
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

    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        final WebView w = (WebView) findViewById(R.id.browser);
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
                Toast.makeText(getApplicationContext(), getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
                finish();
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.toLowerCase().startsWith("http://sine.adolfintel.com/forum")||url.toLowerCase().startsWith("http://isochronic.io/forum")||url.toLowerCase().startsWith("https://isochronic.io/forum")) {
                    Intent i = new Intent(BrowserActivity.this, CommunityActivity.class);
                    i.putExtra("path", url);
                    startActivity(i);
                    return true;
                }
                return false;
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
    public boolean onNavigationItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.nav_home){
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
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
    public void onResume(){
        super.onResume();
        ((NavigationView) findViewById(R.id.nav_view)).getMenu().getItem(1).setChecked(true);
    }
}
