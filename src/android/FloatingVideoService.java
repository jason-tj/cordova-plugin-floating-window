package com.plugin.floatv1.floatingwindow;

import android.app.Activity;
import android.app.Service;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.zhongzilian.chestnutapp.MainActivity;
import com.zhongzilian.chestnutapp.R;

import org.apache.cordova.CordovaInterface;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by noah chen on 2022/1/5.
 */

public class FloatingVideoService extends Service  {
  public static boolean isStarted = false;
  public static String videoUrl;
  public static String videoUrl_old;
  public static long times_old;
  public static int times_cur = 0;
  public static LocalDateTime beginPlayer;
  private WindowManager windowManager;
  public static WindowManager.LayoutParams layoutParams;

  public static MediaPlayer mediaPlayer;
  public static View displayView;
  public static Context this_context;
  public static CordovaInterface this_cordova;
  public static View this_view;
  public static RelativeLayout video_display_relativeLayout;


  @Override
  public void onCreate() {
    super.onCreate();
    isStarted = true;
    windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    layoutParams = new WindowManager.LayoutParams();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    } else {
      layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
    }
    layoutParams.format = PixelFormat.RGBA_8888;
    layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
    layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    layoutParams.width = 800;
    layoutParams.height = 450;
    layoutParams.x = 300;
    layoutParams.y = 300;

    mediaPlayer = new MediaPlayer();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    showFloatingWindow();
    return super.onStartCommand(intent, flags, startId);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public static void hideVideo()
  {
    long cur_times = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();

    videoUrl = "-1";
    times_old = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();
    mediaPlayer.pause();
    mediaPlayer.reset();

    displayView.setVisibility(View.GONE);    // 隐藏 view
    displayView.destroyDrawingCache();
    displayView.clearAnimation();
    displayView.cancelLongPress();
    displayView.clearFocus();

    isStarted = false;

    FloatingWindowPlugin.callJS(""+cur_times);

    Intent it = new Intent(this_cordova.getActivity().getBaseContext(), FloatingVideoService.class);
    this_cordova.getActivity().getBaseContext().stopService(it);
  }

  public  static   void closeVideo() {
    video_display_relativeLayout.postInvalidate();
    video_display_relativeLayout.post(new Runnable(){
      @RequiresApi(api = Build.VERSION_CODES.M)
      @Override
      public void run() {
        Intent it = new Intent(this_cordova.getActivity().getBaseContext(), FloatingVideoService.class);
        this_cordova.getActivity().getBaseContext().stopService(it);
        video_display_relativeLayout.setVisibility(View.GONE);    // 隐藏 view
        isStarted = false;
        long cur_times = mediaPlayer.getTimestamp().getAnchorMediaTimeUs();
        FloatingWindowPlugin.callJS(""+cur_times);
        videoUrl = "-1";
        mediaPlayer.pause();
        mediaPlayer.reset();
      }
    });

  }

  public static void showVideo(){
    try {

      isStarted = true;
      videoUrl_old = videoUrl;
      mediaPlayer.reset();
      mediaPlayer.setDataSource(this_context, Uri.parse(videoUrl));
      mediaPlayer.prepare(); //.prepareAsync(); //
      mediaPlayer.start();
      mediaPlayer.seekTo(times_cur); //毫秒,跳到当前时间播放
      FloatingWindowPlugin.callJS("-1");
      //this_cordova.getActivity().finish();//关闭主窗口,回到手机的首页

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  // @RequiresApi(api = Build.VERSION_CODES.O)
  private void showFloatingWindow() {
    if (Settings.canDrawOverlays(this)) {
      LayoutInflater layoutInflater = LayoutInflater.from(this);
      displayView = layoutInflater.inflate(R.layout.video_display, null);
      video_display_relativeLayout = displayView.findViewById(R.id.video_display_relativeLayout);

      displayView.setVisibility(View.VISIBLE); // 显示 view
      displayView.setOnTouchListener(new FloatingOnTouchListener());
      mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
      SurfaceView surfaceView = displayView.findViewById(R.id.video_display_surfaceview);
      final SurfaceHolder surfaceHolder = surfaceView.getHolder();
      surfaceHolder.addCallback(new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
          mediaPlayer.setDisplay(surfaceHolder);
          showVideo();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }
      });
      mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {

        }
      });

      ImageView closeImageView =   displayView.findViewById(R.id.iv_close_window);
      closeImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {
          // 关闭悬浮窗事件
          hideVideo();
        }
      });


      // 关闭悬浮窗并且回到主窗口事件
      ImageView  goMainImageView =   displayView.findViewById(R.id.iv_zoom_main_btn);
      goMainImageView.setOnClickListener(  new  ImageView.OnClickListener() {
        @Override
        public void onClick(View v) {

          /**将被挤压到后台的本应用重新置顶到最前端
           * 当自己的应用在后台时，将它切换到前台来*/
          FloatingSystemHelper.setTopApp(this_cordova.getActivity().getBaseContext());
          FloatingWindowPlugin.callJS("-2");
        }
      });

      windowManager.addView(displayView, layoutParams);
    }
  }



  private class FloatingOnTouchListener implements View.OnTouchListener {
    private int x;
    private int y;

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      switch (event.getAction()) {
        case MotionEvent.ACTION_CANCEL:
          //isStarted = false; //todo
          break;
        case MotionEvent.ACTION_DOWN:
          x = (int) event.getRawX();
          y = (int) event.getRawY();
          break;
        case MotionEvent.ACTION_MOVE:
          int nowX = (int) event.getRawX();
          int nowY = (int) event.getRawY();
          int movedX = nowX - x;
          int movedY = nowY - y;
          x = nowX;
          y = nowY;
          layoutParams.x = layoutParams.x + movedX;
          layoutParams.y = layoutParams.y + movedY;
          windowManager.updateViewLayout(view, layoutParams);
          break;

        default:
          break;
      }
      return true;
    }
  }
}
