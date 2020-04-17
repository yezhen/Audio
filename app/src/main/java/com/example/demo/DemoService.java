package com.example.demo;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.NoiseSuppressor;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

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

public class DemoService extends Service {
    private static final String TAG = "life";
    private HotKeyReceiver mHotKeyReceiver;
    private boolean isRecording;
    // pcm文件
    private File file;
    private AudioAec mAudio;
    private AudioRecord mAudioRecord;
    private AcousticEchoCanceler m_canceler = null;
    private int mId;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x001:
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            StartRecord();
                            Log.e(TAG, "start");
                        }
                    });
                    thread.start();
                    Log.d(TAG, "开始录音");

//                    mAudio.StartRecorderAndPlayer();
                    break;
                case 0x002:
                    isRecording = false;
                    Log.d(TAG, "停止录音");
                    break;
                case 0x003:
                    Log.d(TAG, "播放录音");
                    PlayRecord();

                    break;
                default:
                    break;
            }
        }
    };

    public DemoService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //am broadcast -a com.eostek.test.start
        //am broadcast -a com.eostek.test.stop
        //am broadcast -a com.eostek.test.play
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.eostek.test.start");
        filter.addAction("com.eostek.test.stop");
        filter.addAction("com.eostek.test.play");
        mHotKeyReceiver = new HotKeyReceiver();
        registerReceiver(mHotKeyReceiver, filter);
        mAudio = new AudioAec();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mHotKeyReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class HotKeyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.eostek.test.start".equals(action)) {
                mHandler.sendEmptyMessage(0x001);
            } else if ("com.eostek.test.stop".equals(action)) {
                mHandler.sendEmptyMessage(0x002);
            } else if ("com.eostek.test.play".equals(action)) {
                mHandler.sendEmptyMessage(0x003);
            }
        }
    }


    // 开始录音
    public void StartRecord() {
        Log.i(TAG, "开始录音");
        // 16K采集率 16000 44100
        int frequency = 44100;
        // 格式
        int channelConfiguration = AudioFormat.CHANNEL_OUT_STEREO;
        // 16Bit
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        // 生成PCM文件
        file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/reverseme.pcm");
        Log.i(TAG, "生成文件");
        // 如果存在，就先删除再创建
        if (file.exists())
            file.delete();
        Log.i(TAG, "删除文件");
        try {
            file.createNewFile();
            Log.i(TAG, "创建文件");
        } catch (IOException e) {
            Log.i(TAG, "未能创建");
            throw new IllegalStateException("未能创建" + file.toString());
        }
        try {
            // 输出流
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, frequency, channelConfiguration,
                    audioEncoding, bufferSize);
            mId = mAudioRecord.getAudioSessionId();
            //short[] buffer = new short[bufferSize];
            byte[] buffer = new byte[bufferSize];
            mAudioRecord.startRecording();
            Log.i(TAG, "开始录音");
            isRecording = true;
            while (isRecording) {
                int bufferReadResult = mAudioRecord.read(buffer, 0, bufferSize);
                for (int i = 0; i < bufferReadResult; i++) {
                    //dos.writeShort(buffer[i]);
                    dos.writeByte(buffer[i]);
                }
            }
            mAudioRecord.stop();
            dos.close();
        } catch (Throwable t) {
            Log.e(TAG, "录音失败");
        }
    }

    private static boolean isNSAvailable() {
        return NoiseSuppressor.isAvailable();
    }

    private static boolean isAECAailable() {
        return AcousticEchoCanceler.isAvailable();
    }

    // 播放文件
    public void PlayRecord() {
        if (file == null) {
            return;
        }

        initAEC();
        initNSA();

        // 读取文件
        int musicLength = (int) (file.length());
        int m_bufferSizeInBytes = 1024 * 4;
        byte[] music = new byte[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readByte();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, 44100,
                    AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, m_bufferSizeInBytes,
                    AudioTrack.MODE_STREAM, mId);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Exception e) {
            Log.e(TAG, "播放失败");
            e.printStackTrace();
            Log.e(TAG, ">>>>>>>>>>播放失败");
        }
    }

    private void initNSA() {
        if (isNSAvailable()) {
            NoiseSuppressor noiseSuppressor = NoiseSuppressor
                    .create(mId);
            if (noiseSuppressor == null) {
                Log.d(TAG, "噪声消除不能使用");
            } else {
                int resultCode = noiseSuppressor.setEnabled(true);
                if (AudioEffect.SUCCESS == resultCode) {
                    Log.d(TAG, ">>>>>>>>噪声消除使能成功");
                }
            }
        }else{
            Log.d(TAG, "噪声消除不支持");
        }
    }

    private void initAEC() {
        if (isAECAailable()) {
            AcousticEchoCanceler acousticEchoCanceler = AcousticEchoCanceler
                    .create(mId);
            if (acousticEchoCanceler == null) {
                Log.d(TAG, "回声消除不能使用");
                Toast.makeText(DemoService.this, "回声消除不能使用", Toast.LENGTH_SHORT).show();

            } else {
                int resultCode = acousticEchoCanceler.setEnabled(true);
                if (AudioEffect.SUCCESS == resultCode) {
                    Log.d(TAG, "回声消除使能成功");
                    Toast.makeText(DemoService.this, "回声消除使能成功", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private boolean initAEC(int audioSession) {
        if (m_canceler != null) {
            return false;
        }
        m_canceler = AcousticEchoCanceler.create(audioSession);
        m_canceler.setEnabled(true);
        return m_canceler.getEnabled();
    }

    private boolean setAECEnabled(boolean enable) {
        if (null == m_canceler) {
            Log.d(TAG, "setAECEnabled>>>>>false");
            return false;
        }
        Log.d(TAG, "setAECEnabled>>>>>true");
        m_canceler.setEnabled(enable);
        return m_canceler.getEnabled();
    }
}
