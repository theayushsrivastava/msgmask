package com.c2mtechnology.msgmask;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import com.cunoraz.gifview.library.GifView;
import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        createNotificationChannel();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this,MainActivity.class));
                finish();
            }
        },1000);
    }

    private void createNotificationChannel()
    {
        String channelID = "default";
        CharSequence name = getString(R.string.channel1);
        String description = getString(R.string.channel1_des);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + getPackageName() + "/" + R.raw.iphone_ding);

            NotificationChannel channel = new NotificationChannel(channelID,name, NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setBypassDnd(true);

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setDescription(description);

//            channel.setSound(sound,attributes);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}