package com.example.pawan.madc;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.InflateException;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

public class Welcome extends AppCompatActivity {
    private TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        tv = (TextView) findViewById(R.id.tv);

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.splash);
        tv.startAnimation(anim);
        final Intent i = new Intent(Welcome.this, MainActivity.class);
        Thread timer = new Thread(){
            @Override
            public  void  run(){
                try{
                    sleep(5000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
                finally {
                    startActivity(i);
                    finish();
                }
            }
        };
        timer.start();
    }
}
