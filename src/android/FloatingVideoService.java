package com.plugin.floatv1.floatingwindow;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
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
import android.widget.Toast;

import com.zhongzilian.chestnutapp.R;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by noah chen on 2022/1/5.
 */

public class FloatingVideoService extends Service {
    public static boolean isStarted = false;
    public static String videoUrl;
    public static String videoUrl_old;
    public static LocalDateTime beginPlayer;
    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;

    public static MediaPlayer mediaPlayer;
    public static View displayView;

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

    public  int getVideoDuration(){
      return mediaPlayer.getDuration();
    }

    public static void hideVideo() {
      mediaPlayer.pause();
      videoUrl = "";
      displayView.setVisibility(View.GONE);    // 隐藏 view
    }

    public static void showVideo(String video_url, Context this_context) {
        try {
          displayView.setVisibility(View.VISIBLE); // 显示 view
          videoUrl = video_url;
          videoUrl_old = videoUrl;
          mediaPlayer.reset();
          mediaPlayer.setDataSource(this_context, Uri.parse(videoUrl));
          mediaPlayer.prepareAsync();
        }
        catch (IOException e) {
          mediaPlayer.release();
         // Toast.makeText(this_context, "无法打开视频源", Toast.LENGTH_LONG).show();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
   // @RequiresApi(api = Build.VERSION_CODES.O)
    private void showFloatingWindow() {
        if (Settings.canDrawOverlays(this)) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            displayView = layoutInflater.inflate(R.layout.video_display, null);
            displayView.setOnTouchListener(new FloatingOnTouchListener());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            SurfaceView surfaceView = displayView.findViewById(R.id.video_display_surfaceview);
            final SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mediaPlayer.setDisplay(surfaceHolder);
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
                    mediaPlayer.start();
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