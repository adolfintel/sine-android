package com.dosse.bwentrain.androidPlayer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class DetailsActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //play was pressed, send intent to MainActivity
                Intent i = new Intent(DetailsActivity.this, MainActivity.class);
                i.putExtra("path", getIntent().getStringExtra("path"));
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(i);
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        //get data from intent (description, length and loop marker) and show it
        ((TextView)findViewById(R.id.textView1)).setText(getIntent().getStringExtra("description"));
        ((TextView)findViewById(R.id.textView0)).setText(getIntent().getStringExtra("title"));
        String l=getIntent().getStringExtra("loop");
        ((TextView)findViewById(R.id.textView2)).setText(getIntent().getStringExtra("length")+(!l.isEmpty()?(", "+getString(R.string.loops_after)+" "+l):""));
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id==android.R.id.home){
            return super.onOptionsItemSelected(item);
        }
        if(id==R.id.delete){
            //ask for confirm
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            DialogInterface.OnClickListener l = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if(which==DialogInterface.BUTTON_POSITIVE){	//delete confirmed
                        if(new File(getIntent().getStringExtra("path")).exists()){ //delete file on sdcard
                            new File(getIntent().getStringExtra("path")).delete();
                        } else {//delete from app data
                            deleteFile(getIntent().getStringExtra("path"));
                        }
                        finish(); //terminate this activity
                    }
                }
            };
            builder.setMessage(getString(R.string.delete_confirm)+" \""+getIntent().getStringExtra("title")+"\"?").setPositiveButton(getString(R.string.yes), l).setNegativeButton(getString(R.string.no), l).show();
        }
        return true;
    }

}
