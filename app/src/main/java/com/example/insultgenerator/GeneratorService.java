package com.example.insultgenerator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class GeneratorService extends Service {
    private ServiceHandler serviceHandler;
    private Messenger messenger;
    private ClipboardPoster clipboardPoster;

    @Override
    public void onCreate() {
        clipboardPoster = new ClipboardPoster(getApplicationContext());
        HandlerThread thread = new HandlerThread("GeneratorService",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        Toast.makeText(this, R.string.genServiceInitialized,
                Toast.LENGTH_SHORT).show();

        Looper serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg;
        Notification notification;
        Bundle bundle = new Bundle();
        boolean autogen = intent.getBooleanExtra(Constants.KEYS.AUTO_GEN, false);

        //get UI Thread messenger for sending insults back to UI
        messenger = intent.getParcelableExtra(Constants.KEYS.MESSENGER);

        //make new msg
        msg = serviceHandler.obtainMessage();
        //set first int as startId
        msg.arg1 = startId;
        //set sec int as period for autogen
        msg.arg2 = intent.getIntExtra(Constants.KEYS.PERIOD, 3000);
        //set obj as new generator
        msg.obj = new WordBank(intent.getBooleanArrayExtra(Constants.KEYS.BANKS));
        //include autoClipboard, autoGenerate, and Mix options
        bundle.putBoolean(Constants.KEYS.MIX,
                intent.getBooleanExtra(Constants.KEYS.MIX, false));
        bundle.putBoolean(Constants.KEYS.AUTO_CLIP,
                intent.getBooleanExtra(Constants.KEYS.AUTO_CLIP, false));
        bundle.putBoolean(Constants.KEYS.AUTO_GEN, autogen);
        msg.setData(bundle);
        if (autogen) {
            Toast.makeText(this, R.string.genServiceAutoStart, Toast.LENGTH_SHORT).show();
            notification = createNotification(getText(R.string.autogenerator_notification_message));
            startForeground(Constants.NOTIFICATION.ID, notification);
        }
        //send msg to handler
        serviceHandler.sendMessage(msg);

        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        //no binding
        return null;
    }

    @Override
    public void onDestroy() {
        serviceHandler.removeCallbacksAndMessages(null);
        stopForeground(true);
        Toast.makeText(this, R.string.genServiceStopped,
                Toast.LENGTH_SHORT).show();
    }

    private Notification createNotification(CharSequence insult) {
        Intent intent;
        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;

        intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder = new NotificationCompat.Builder(this,
                Constants.NOTIFICATION.GENERATED_INSULTS_CHANNEL)
                .setSmallIcon(R.drawable.generator_service_icon_small)
                .setContentTitle(getString(R.string.autogenerator_notification_name))
                .setContentText("\"" + insult + "\"")
                .setStyle(new NotificationCompat.BigTextStyle().bigText("\"" + insult + "\""))
                .setTicker(getString(R.string.autogenerator_notification_message))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                // Set content that will fire when the user taps the notification
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setCategory(Notification.CATEGORY_SERVICE);

        return builder.build();
    }

    private void updateNotification(CharSequence insult) {
        Notification notification = createNotification(insult);
        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        manager.notify(Constants.NOTIFICATION.ID, notification);
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            final WordBank wordBank = (WordBank) msg.obj;
            final boolean mix = msg.getData().getBoolean(Constants.KEYS.MIX);
            final boolean autoClipboard = msg.getData().getBoolean(Constants.KEYS.AUTO_CLIP);
            final boolean autoGenerate = msg.getData().getBoolean(Constants.KEYS.AUTO_GEN);
            final int period = msg.arg2;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    CharSequence insult = wordBank.generateInsult(mix);
                    Message result = obtainMessage();

                    result.obj = insult;
                    assert messenger != null;
                    try {
                        messenger.send(result);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        System.err.println("error: Could not communicate with UI Thread");
                    }
                    if (autoClipboard) clipboardPoster.postToClipboard(insult);
                    if (autoGenerate) {
                        updateNotification(insult);
                        postDelayed(this, period);
                    } else {
                        serviceHandler.removeCallbacksAndMessages(null);
                    }
                }
            };

            try {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException();
                }
                post(runnable);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
