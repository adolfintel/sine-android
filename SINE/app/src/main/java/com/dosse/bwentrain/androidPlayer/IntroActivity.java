package com.dosse.bwentrain.androidPlayer;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class IntroActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        ((Button)findViewById(R.id.letsgo)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
                    startActivity(new Intent(IntroActivity.this, Intro2Activity.class));
                }
                finish();
            }
        });
        if(getIntent().getBooleanExtra("update",false)){
            ((TextView)findViewById(R.id.textView2)).setText(getString(R.string.intro1u));
            ((TextView)findViewById(R.id.textView4)).setText(getString(R.string.intro2u));
        }

    }

}
