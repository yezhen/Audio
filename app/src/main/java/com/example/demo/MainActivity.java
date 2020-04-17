package com.example.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.Manifest;
import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private void requestPermissions() {
        if (
                ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
        } else {
            initData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initData();
        } else {
            Toast.makeText(this, "用户拒绝了权限", Toast.LENGTH_SHORT).show();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void initData() {
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        ((Button) findViewById(R.id.star_service)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.d(TAG, "开启service");
                startService(new Intent(MainActivity.this, DemoService.class));
            }
        });
        ((Button) findViewById(R.id.start)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                sendBroadcast(new Intent("com.eostek.test.start"));
                Log.d(TAG, "停止录音");
            }
        });
        ((Button) findViewById(R.id.stop)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                sendBroadcast(new Intent("com.eostek.test.stop"));
                Log.d(TAG, " 停止录音");
            }
        });
        ((Button) findViewById(R.id.play)).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                sendBroadcast(new Intent("com.eostek.test.play"));
                Log.d(TAG, "播放录音");
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
    }
}
