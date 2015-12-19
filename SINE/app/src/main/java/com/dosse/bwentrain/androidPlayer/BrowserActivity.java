package com.dosse.bwentrain.androidPlayer;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.DownloadListener;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.os.Build;

public class BrowserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }
    @Override
    protected void onPostCreate(Bundle savedInstanceState){
        final WebView w = (WebView) findViewById(R.id.browser);
        w.getSettings().setSupportZoom(false);
        w.getSettings().setJavaScriptEnabled(true);
        w.setWebViewClient(new WebViewClient() {
            private boolean firstLoad=true;
            public void onLoadResource (WebView view, String url){
                if(firstLoad){w.setVisibility(View.INVISIBLE);} // show loading animation
            }

            public void onPageFinished(WebView view, String url) {
                if(firstLoad){w.setVisibility(View.VISIBLE); firstLoad=false;} // hide loading animation
            }

            public void onReceivedError(WebView view, int errorCode,String description, String failingUrl) {
                Toast.makeText(getApplicationContext(),getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
                finish();
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
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


}
