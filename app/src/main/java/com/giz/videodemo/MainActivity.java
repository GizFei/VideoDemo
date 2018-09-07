package com.giz.videodemo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public static final int UPDATE_UI = 1;
    public static final int REQUEST_VIDEO = 0;

    private CustomVideoView mVideoView;
    private RelativeLayout mControllerLayout;
    private ImageView mPauseImg, mScreenZoomImg, mVolumeImg;
    private TextView mCurrentTimeTv, mTotalTimeTv;
    private SeekBar mPlaySeekBar, mVolumeSeekBar;
    private RelativeLayout mVideoLayout;
    private AudioManager mAudioManager;
    private FrameLayout mVolumeProgressLayout, mBrightnessProgressLayout;
    private SeekBar mFloatingVolumeSeekBar, mBrightnessSeekBar;
    private Button mVideoButton;

    private int screenWidth, screenHeight;
    private boolean isFullScreen = false; // 是否全屏
    private boolean isAdjust = false;
    private int threshold = 54;  // 临界值
    private float mBrightness; // 当前亮度
    private boolean isControllerShown = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)){

            }else{
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1);
            }
        }

        initUI();
        setPlayerEvent();

//        String path = Environment.getExternalStorageDirectory().getAbsolutePath() +
//                "/update/videoDemo.mp4";
          // 本地视频播放
//        mVideoView.setVideoPath(path);
//        mVideoView.start();
//        UIHandler.sendEmptyMessage(UPDATE_UI);

//        // 网络播放
//        //mVideoView.setVideoURI(Uri.parse("https://www.imooc.com/video/14021"));
//
//        // 使用MediaController控制视频播放
//        MediaController controller = new MediaController(this);
//        // 设置VideoView与MediaController建立关联
//        mVideoView.setMediaController(controller);
//        // 设置MediaController与VideoView建立关联
//        controller.setMediaPlayer(mVideoView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == REQUEST_VIDEO){
                Uri uri = data.getData();

                mVideoView.setVideoURI(uri);
                mPauseImg.setImageResource(R.drawable.pause_btn_style);
                mVideoView.start();
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        UIHandler.removeMessages(UPDATE_UI);
    }

    @Override
    public void onBackPressed() {
        if(isFullScreen){
            mScreenZoomImg.performClick();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setPlayerEvent() {
        mPauseImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mVideoView.isPlaying()){
                    mPauseImg.setImageResource(R.drawable.play_btn_style);
                    mVideoView.pause();
                    UIHandler.removeMessages(UPDATE_UI);
                }else{
                    mPauseImg.setImageResource(R.drawable.pause_btn_style);
                    mVideoView.start();
                    UIHandler.sendEmptyMessage(UPDATE_UI);
                }
            }
        });

        mPlaySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateTextViewWithTimeFormat(mCurrentTimeTv, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                UIHandler.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                // 跳转
                mVideoView.seekTo(progress);
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        });

        mVolumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 设置当前设备的音量
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // 横竖屏切换
        mScreenZoomImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFullScreen){
                    mScreenZoomImg.setImageResource(R.drawable.ic_zoom);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }else{
                    mScreenZoomImg.setImageResource(R.drawable.ic_zoom_small);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });

        // 控制VideoView的手势事件
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            float lastX = 0, lastY = 0;
            boolean isOneClick = false; // 是否只是点击一下而已
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()){
                    // 手指落下屏幕的那一刻（只会调用一次）
                    case MotionEvent.ACTION_DOWN:
                        lastX = x;
                        lastY = y;
                        isOneClick = true;
                        break;
                    case MotionEvent.ACTION_MOVE:  // 手指在屏幕上移动（调用多次）
                        isOneClick = false;
                        float deltaX = x - lastX;
                        float deltaY = y - lastY;
                        float absDeltaX = Math.abs(deltaX);
                        float absDeltaY = Math.abs(deltaY);
                        if(absDeltaX > threshold && absDeltaY > threshold){
                            if(absDeltaX < absDeltaY){
                                isAdjust = true;  // 此次手势有效
                            }else{
                                isAdjust = false;
                            }
                        }else if(absDeltaX < threshold && absDeltaY > threshold){
                            isAdjust = true;
                        }else if(absDeltaX > threshold && absDeltaY < threshold){
                            isAdjust = false;
                        }

                        if(isAdjust && isFullScreen) {
                            if (x < screenHeight / 2) {
                                changeBrightness(-deltaY);
                            } else {
                                changeVolume(-deltaY);
                            }
                        }
                        lastX = x;
                        lastY = y;
                        break;
                    case MotionEvent.ACTION_UP:
                        // 手指离开屏幕
                        if(isOneClick){
                            if(isControllerShown){
                                mControllerLayout.setVisibility(View.GONE);
                            }else{
                                mControllerLayout.setVisibility(View.VISIBLE);
                            }
                            isControllerShown = !isControllerShown;
                        }
                        mVolumeProgressLayout.setVisibility(View.GONE);
                        mBrightnessProgressLayout.setVisibility(View.GONE);
                        break;
                }

                return true;
            }
        });
    }

    private void changeVolume(float deltaY){
        int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        int index = (int)(deltaY / screenHeight * max * 5);
        int volume = Math.max(current + index, 0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        mVolumeProgressLayout.setVisibility(View.VISIBLE);
        mFloatingVolumeSeekBar.setProgress(volume);

        mVolumeSeekBar.setProgress(volume);
    }

    private void changeBrightness(float deltaY){
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        mBrightness = attrs.screenBrightness;
        float index = deltaY / screenHeight;
        mBrightness += index;
        if(mBrightness > 1.0f)
            mBrightness = 1.0f;
        if(mBrightness < 0f)
            mBrightness = 0f;
        attrs.screenBrightness = mBrightness;

        getWindow().setAttributes(attrs);
        mBrightnessProgressLayout.setVisibility(View.VISIBLE);
        mBrightnessSeekBar.setProgress((int)(mBrightness * 100));
    }

    private void initUI(){
        PixelUtil.initContext(this);
        mVideoView = findViewById(R.id.videoView);
        mControllerLayout = findViewById(R.id.controller_layout);
        mPauseImg = findViewById(R.id.pause_img);
        mScreenZoomImg = findViewById(R.id.screen_img);
        mVolumeImg = findViewById(R.id.volume_img);
        mCurrentTimeTv = findViewById(R.id.time_current_tv);
        mTotalTimeTv = findViewById(R.id.time_total_tv);
        mPlaySeekBar = findViewById(R.id.play_seekbar);
        mVolumeSeekBar = findViewById(R.id.volume_seekbar);
        mVideoLayout = findViewById(R.id.videoLayout);
        mVolumeProgressLayout = findViewById(R.id.volume_progress);
        mFloatingVolumeSeekBar = mVolumeProgressLayout.findViewById(R.id.volume_progress_bar);
        mBrightnessProgressLayout = findViewById(R.id.brightness_progress);
        mBrightnessSeekBar = mBrightnessProgressLayout.
                findViewById(R.id.brightness_progress_bar);

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE); // 获取音频服务
        int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVolumeSeekBar.setMax(streamMaxVolume);
        mFloatingVolumeSeekBar.setMax(streamMaxVolume);
        mVolumeSeekBar.setProgress(streamVolume);
        mFloatingVolumeSeekBar.setProgress(streamVolume);

        mBrightnessSeekBar.setMax(100);
        try {
            WindowManager.LayoutParams attrs = getWindow().getAttributes();
            attrs.screenBrightness = Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS) / 250.0f;
            getWindow().setAttributes(attrs);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        mVideoButton = findViewById(R.id.video_choose);
        mVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("video/*");
                startActivityForResult(intent, REQUEST_VIDEO);
            }
        });
    }

    private void updateTextViewWithTimeFormat(TextView textView, int millisecond){
        int second = millisecond / 1000;
        int hh = second / 3600;
        int mm = second % 3600 / 60;
        int ss = second % 60;
        String str = "";

        if(hh != 0){
            str = String.format(Locale.CHINA, "%02d:%02d:%02d", hh, mm, ss);
        }else{
            str = String.format(Locale.CHINA, "%02d:%02d", mm, ss);
        }

        textView.setText(str);
    }

    // 定时器
    private Handler UIHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == UPDATE_UI){
                int currentPosition = mVideoView.getCurrentPosition();  // 当前时间
                int totalDuration = mVideoView.getDuration();  //  总时长

                updateTextViewWithTimeFormat(mTotalTimeTv, totalDuration);
                mPlaySeekBar.setMax(totalDuration);

                updateTextViewWithTimeFormat(mCurrentTimeTv, currentPosition);
                mPlaySeekBar.setProgress(currentPosition);

                UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500); // 自动刷新
            }
        }
    };

    private void setVideoViewScale(int width, int height){
        ViewGroup.LayoutParams params =  mVideoView.getLayoutParams();
        params.width = width;
        params.height = height;
        mVideoView.setLayoutParams(params);

        ViewGroup.LayoutParams params1 = mVideoLayout.getLayoutParams();
        params1.width = width;
        params1.height = height;
        mVideoLayout.setLayoutParams(params1);
    }

    /**
     * 监听屏幕方向的改变
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 屏幕横屏
        if(getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE){
            getSupportActionBar().hide();
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mVolumeImg.setVisibility(View.VISIBLE);
            mVolumeSeekBar.setVisibility(View.VISIBLE);
            isFullScreen = true;
            // 移除半屏状态
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else{ // 竖屏
            getSupportActionBar().show();
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,
                    PixelUtil.px2dp(240f));
            mVolumeImg.setVisibility(View.GONE);
            mVolumeSeekBar.setVisibility(View.GONE);
            isFullScreen = false;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }
}
