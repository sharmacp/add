package com.rtspl.tallyviewer.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.rtspl.tallyviewer.R;
import com.rtspl.tallyviewer.activity.SplashActivity;
import com.rtspl.tallyviewer.dmanager.GoogleApi;
import com.rtspl.tallyviewer.useful.AppGlobal;

import java.util.Timer;
import java.util.TimerTask;


public class TimerService extends Service {
    public static final long SUBSEQUENT_INTERVAL = 1000 * 60 * 15;
    public static final long INITIAL_INTERVAL = 1000 * 60 * 10;
    Context context;

    private Handler mHandler = new Handler();
    private Timer timer = null;
    private TimerTask timerTask;

    /* Do not remove this empty constructor */
    public TimerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        /*NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? getNotificationChannel(notificationManager) : "";
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                // .setPriority(PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();

        startForeground(110, notification);*/
    }

    /*@RequiresApi(Build.VERSION_CODES.O)
    private String getNotificationChannel(NotificationManager notificationManager){
        String channelId = "channelid";
        String channelName = getResources().getString(R.string.app_name);
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }*/

    public TimerService(Context applicationContext) {
        super();
        context = applicationContext;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        startBackgroundReceiver();
        Log.d("Task Removed", "Task Removed");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        startBackgroundReceiver();
        Log.d("Timer Destroyed", "Timer Destroyed");
    }

    private void startBackgroundReceiver()
    {
        try {
            Log.d("Starting Broadcast", "Starting Broadcast");
            Intent broadcastIntent = new Intent(this, BroadcastReceiverRestarter.class);
            sendBroadcast(broadcastIntent);
            Log.d("Started Broadcast", "Started Broadcast");
            stopTimerTask();
            Log.d("Stopped Timer", "Stopped Timer");
        }
        catch(Exception ex){
            Log.d("Exception: ", ex.getMessage());
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.d("Timer Called", "Timer Called To Sync Drive Database To Local");
                AppGlobal.CurrentAppContext = getApplicationContext();
                TimerService.this.mHandler.post(new Runnable() {
                    public void run() {
                        if (AppGlobal.DatabaseExists() && !AppGlobal.SyncingManually) {
                            if(AppGlobal.GoogleApiObject == null){
                                AppGlobal.GoogleApiObject = new GoogleApi(AppGlobal.CurrentAppContext);
                            }
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    AppGlobal.SyncFromScheduler = true;
                                    AppGlobal.SyncDriveDatabaseToLocal();
                                    AppGlobal.SyncFromScheduler = false;
                                }
                            }).start();
                        }
                    }
                });
            }
        };
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.scheduleAtFixedRate(timerTask, INITIAL_INTERVAL, SUBSEQUENT_INTERVAL);
    }

    public void stopTimerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

}
