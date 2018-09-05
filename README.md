## 运用到的知识

关于VideoView的操作这里就不列出来了。

关于AudioManager的操作不列出来了。

关于WindowManager的操作不列出来了。

[TOC]

#### 读取手机存储的权限

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
```

#### 屏幕旋转时不重启应用

```xml
<activity android:name=".MainActivity"
          android:configChanges="orientation|screenSize|keyboard|keyboardHidden">
</activity>
```

#### pixel转dp（写在一个静态类中了）

```java
public class PixelUtil {

    private static Context sContext;

    public static void initContext(Context context){
        sContext = context;
    }

    public static int px2dp(float value) throws NullPointerException{
        if(sContext == null){
            throw new NullPointerException("No context in PixelUtil. Please use method " + "<b>initContext</b> first.");
        }
        final DisplayMetrics metrics = sContext.getResources().getDisplayMetrics();
        return (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, metrics) + 0.5f);
    }
}
```

#### 自定义VideoView类，重写onMeasure方法，用于适应横竖屏切换时的调整

```java
int defaultWidth =  1920;
int defaultHeight = 1080;
@Override
protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    int width = getDefaultSize(defaultWidth, widthMeasureSpec);
    int height = getDefaultSize(defaultHeight, heightMeasureSpec);
    setMeasuredDimension(width, height);
}
```

#### 获得手机内部存储的绝对地址

```java
Environment.getExternalStorageDirectory().getAbsolutePath();
```

#### 进度条拖动事件

```java
SeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, 
                                  boolean fromUser) {
        // 拖动时
		updateTextViewWithTimeFormat(mCurrentTimeTv, progress);
    }
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
        // 按下进度条时
		UIHandler.removeMessages(UPDATE_UI);
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
        // 松开进度条时
		int progress = seekBar.getProgress();
		// 跳转
		mVideoView.seekTo(progress);
		UIHandler.sendEmptyMessage(UPDATE_UI);
	}
});
```

#### 改变屏幕的方向

```java
setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 横屏
setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);// 竖屏
```

#### 获取屏幕的宽度和高度（像素）

```java
screenWidth = getResources().getDisplayMetrics().widthPixels;
screenHeight = getResources().getDisplayMetrics().heightPixels;
```

#### 定时器（Handler）

```java
// UPDATE_UI是一个自定义的int变量
// 开始这个定时器的方法：UIHandler.sendEmptyMessage(UPDATE_UI);
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

			UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500); // 自动刷新(500是间隔时长)
		}
	}
};
```

#### 监听屏幕方向的改变事件

```java
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
		// 移除半屏状态，这两句重要
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}else{ // 竖屏
		getSupportActionBar().show();
		setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,
				PixelUtil.px2dp(240f));
		mVolumeImg.setVisibility(View.GONE);
		mVolumeSeekBar.setVisibility(View.GONE);
		isFullScreen = false;
        // 移除全屏状态
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
    }
```



