package com.dosse.bwentrain.androidPlayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.InputStream;

public class PresetImportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preset_import);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Uri u=getIntent().getData();
        if(u==null){Toast.makeText(getApplicationContext(),"ERROR 1",Toast.LENGTH_SHORT).show(); finish();}
        Toast.makeText(getApplicationContext(),getString(R.string.preset_importing)+" "+u,Toast.LENGTH_SHORT).show();
        InputStream in=null;
        FileOutputStream out=null;
        try{
            in=getContentResolver().openInputStream(u);
            out=openFileOutput(u.getLastPathSegment().toLowerCase().endsWith(".sin")?u.getLastPathSegment():(u.getLastPathSegment()+".sin"),MODE_PRIVATE);
        }catch(Throwable t){
            Toast.makeText(getApplicationContext(),"ERROR 2",Toast.LENGTH_SHORT).show();
            finish();
        }
        try{
            byte[] buff=new byte[1024];
            int l;
            for(;;){
                l=in.read(buff);
                out.write(buff,0,l);
            }
        }catch(Throwable t){
        }
        try{
            out.flush();
            out.close();
            in.close();
        }catch(Throwable t){
        }
        Intent i=new Intent(this,MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

}
