package com.example.insultgenerator;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private final HashMap<String, CheckBox> checkHash = new HashMap<>();
    private MyHandler handler;
    private EditText inputInterval;
    private TextView textInsultField;
    private ClipboardPoster clipboardPoster;
    private boolean[] banks;
    private boolean isAutoGenRunning = false;

    /**
     * App is opened from not running
     *
     * @param savedInstanceState: instance state that holds saved state info
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // initialize banks
        banks = new boolean[WordBank.NUM_BANKS];
        if (savedInstanceState != null) {
            banks[WordBank.SHAKESPEARE] = savedInstanceState.getBoolean(Constants.KEYS.SHAKESPEARE);
        } else {
            for (int i = 0; i < WordBank.NUM_BANKS; i++) {
                banks[i] = false;
            }
        }

        setContentView(R.layout.activity_main);
        // set background
//        getWindow().setBackgroundDrawableResource(R.drawable.background);
        // Hide the status bar.
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        // get Handler for communication from Generator Service
        handler = new MyHandler(this);

        // put all CheckBox references in a Hash Table for easy batch operations
        checkHash.put(Constants.KEYS.AUTO_CLIP, (CheckBox) findViewById(R.id.checkAutoClipboard));
        checkHash.put(Constants.KEYS.AUTO_GEN, (CheckBox) findViewById(R.id.checkAutoGenerate));
        checkHash.put(Constants.KEYS.MIX, (CheckBox) findViewById(R.id.checkMix));
        checkHash.put(Constants.KEYS.SHAKESPEARE, (CheckBox) findViewById(R.id.checkShakespeare));

        // tag the race checkboxes with bank index for easy reference
        checkHash.get(Constants.KEYS.SHAKESPEARE).setTag(WordBank.SHAKESPEARE);

        // clipboard Poster for copy-to-clipboard button
        clipboardPoster = new ClipboardPoster(this);

        // Interval label and input references
        inputInterval = findViewById(R.id.inputInterval);
        textInsultField = findViewById(R.id.insultField);

        //create notification channel for autogen foreground service
        createAutoGeneratorNotificationChannel();

        System.out.println("Reached end of onCreate()");
    }

    /**
     * App is starting after creation or after being stopped
     */
    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("Reached end of onStart()");
    }

    /**
     * App is running on screen
     */
    @Override
    protected void onResume() {
        super.onResume();
        // ensure the UI reflects what we last knew about the AutoGenerator
        serviceUpdateUI();
        System.out.println("Reached end of onResume()");
    }

    /**
     * App is visible, but interrupted (such as opening the app drawer)
     */
    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("Reached end of onPause");
    }

    /**
     * App is minimized (such as hitting Home or opening another app), or app is shutting down
     */
    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("Reached end of onStop");
    }

    /**
     * App is destroyed by System, shutting down, or Swiped Away in App Drawer
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("Reached end of onDestroy()");
    }

    /**
     * Restore UI states when running App is reopened
     *
     * @param savedInstanceState: instance state that holds saved state info
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        for (String key : checkHash.keySet()) {
            checkHash.get(key).setChecked(savedInstanceState.getBoolean(key));
        }

        isAutoGenRunning = savedInstanceState.getBoolean(Constants.KEYS.SERVICE_RUNNING);
        inputInterval.setText(savedInstanceState.getCharSequence(Constants.KEYS.INTERVAL_INPUT));
        textInsultField.setText(savedInstanceState.getCharSequence(Constants.KEYS.INSULT_FIELD));
    }

    /**
     * Save current UI states when App is hidden or minimized
     *
     * @param outState: instance state to hold state info
     */
    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        for (String key : checkHash.keySet()) {
            outState.putBoolean(key, checkHash.get(key).isChecked());
        }

        outState.putCharSequence(Constants.KEYS.INTERVAL_INPUT, inputInterval.getText());
        outState.putCharSequence(Constants.KEYS.INSULT_FIELD, textInsultField.getText());
        outState.putBoolean(Constants.KEYS.SERVICE_RUNNING, isAutoGenRunning);

        super.onSaveInstanceState(outState);
    }

    /**
     * Stop autogenerator, or generate single insult or start autogenerator
     *
     * @param view: the big generate button at the bottom of the main UI
     */
    @SuppressWarnings("ConstantConditions")
    public void onGenerate(View view) {
        // if running, stop it
        if (isAutoGenRunning) {
            stopService(new Intent(this, GeneratorService.class));
            isAutoGenRunning = false;
        } else {
            // check autoGen box: are we generating only 1 insult, or starting the AutoGenerator?
            isAutoGenRunning = checkHash.get(Constants.KEYS.AUTO_GEN).isChecked();
            generate(isAutoGenRunning);
        }
        // update UI to reflect AutoGen status
        serviceUpdateUI();
    }

    /**
     * Posts string from insultField to clipboard and notifies user via Toast popup
     *
     * @param view: the copy-to-clipboard button
     */
    public void onPostToClipboard(View view) {
        clipboardPoster.postToClipboard(textInsultField.getText().toString());
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
    }

    /**
     * Check which Bank CheckBox was (un)checked, enable/disable corresponding word bank
     *
     * @param view: the CheckBox that was (un)checked
     */
    @SuppressWarnings("unused")
    public void onUpdateBank(View view) {
        banks[(int) view.getTag()] = ((CheckBox) view).isChecked();
    }

    /**
     * start/update Generator Service for single or constant creation of insults
     * if no values are entered, defaults will be used
     */
    @SuppressWarnings("ConstantConditions")
    private void generate(boolean autoGenerate) {
        double interval;
        int period;
        Intent intent = new Intent(this, GeneratorService.class);

        // pack word banks to use, messenger for reporting back insult to UI, options, and period
        intent.putExtra(Constants.KEYS.BANKS, banks);
        intent.putExtra(Constants.KEYS.MESSENGER, new Messenger(handler));
        if (checkHash.get(Constants.KEYS.MIX).isChecked())
            intent.putExtra(Constants.KEYS.MIX, true);
        if (checkHash.get(Constants.KEYS.AUTO_CLIP).isChecked())
            intent.putExtra(Constants.KEYS.AUTO_CLIP, true);
        // if autogenerating, retrieve, filter, package input from Interval
        // rewrite filtered value back
        if (autoGenerate) {
            interval = filterIntervalInput(inputInterval.getText().toString());
            inputInterval.setText(String.valueOf(interval));
            period = (int) (interval * Constants.MS_IN_SECONDS);
            intent.putExtra(Constants.KEYS.PERIOD, period);
            intent.putExtra(Constants.KEYS.AUTO_GEN, true);

            // If autogenerating and API >= 26 'Oreo', launch as foreground service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                startForegroundService(intent);
            else
                startService(intent);
        } else {
            startService(intent);
        }
    }


    private void createAutoGeneratorNotificationChannel() {
        NotificationChannel channel;

        // create notification channel if running on api 26 'Oreo' or newer (required), else don't
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channel = new NotificationChannel(Constants.NOTIFICATION.GENERATED_INSULTS_CHANNEL,
                    getString(R.string.channel_insult_name),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_insult_description));
            channel.enableLights(false);
            channel.enableVibration(false);


            // register channel with system, cannot change importance after this
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    /**
     * checks service flag, updates GUI to reflect status
     */
    @SuppressWarnings("ConstantConditions")
    private void serviceUpdateUI() {
        CheckBox checkBox;
        CheckBox autoGen = checkHash.get(Constants.KEYS.AUTO_GEN);
        CheckBox autoClip = checkHash.get(Constants.KEYS.AUTO_CLIP);
        CheckBox mix = checkHash.get(Constants.KEYS.MIX);
        LinearLayout wordBank = findViewById(R.id.listWordBank);

        if (isAutoGenRunning) {
            // disabled check boxes
            for (String key : checkHash.keySet()) {
                checkBox = checkHash.get(key);
                if (checkBox != null) {
                    checkBox.setClickable(false);
                }
            }

            // change generate button to STOP
            ((Button) findViewById(R.id.buttonGenerate)).setText(R.string.button_stop);

            // fade to half transparency
            wordBank.setAlpha(Constants.HALF_FLOAT);
            autoClip.setAlpha(Constants.HALF_FLOAT);
            autoGen.setAlpha(Constants.HALF_FLOAT);
            mix.setAlpha(Constants.HALF_FLOAT);

            //lock interval input
            inputInterval.setEnabled(false);

        } else {
            // enable check boxes
            for (String key : checkHash.keySet()) {
                checkBox = checkHash.get(key);
                if (checkBox != null) {
                    checkBox.setClickable(true);
                }
            }

            // set generate button to Generate
            ((Button) findViewById(R.id.buttonGenerate)).setText(R.string.button_generate);

            // unfade to no transparency
            wordBank.setAlpha(Constants.FULL_FLOAT);
            autoClip.setAlpha(Constants.FULL_FLOAT);
            autoGen.setAlpha(Constants.FULL_FLOAT);
            mix.setAlpha(Constants.FULL_FLOAT);

            // unlock interval input
            inputInterval.setEnabled(true);
        }
    }

    /**
     * restrict input within bounds and toss disallowed characters, interpret input as a double
     * Allowed characters are: numbers, periods. All periods after the first one are discarded.
     * Everything else is discarded.
     *
     * @return double: seconds interpreted from input
     */
    private double filterIntervalInput(String rawIntervalText) {
        double interval;
        String intervalText = "";
        String[] intervalTextArr;

        // if something was entered
        if (rawIntervalText != null && !rawIntervalText.equals("")) {
            // discard all non-numbers, split into chunks surrounded by periods
            intervalTextArr =
                    rawIntervalText.replaceAll("[^\\d.]", "").split("\\.");
            // put chunks back together, throwing away all periods except first one
            for (int i = 0; i < intervalTextArr.length; i++) {
                intervalText = intervalText.concat(intervalTextArr[i]);
                if (0 == i)
                    intervalText = intervalText.concat(".");
            }
            // if something left after tossing invalid characters, restrict remaining number within
            //  bounds
            if (!intervalText.equals("")) {
                interval = Double.parseDouble(intervalText);
                if (interval < Constants.PERIOD_MINIMUM_SEC)
                    interval = Constants.PERIOD_MINIMUM_SEC;
                else if (interval > Constants.PERIOD_MAXIMUM_SEC)
                    interval = Constants.PERIOD_MAXIMUM_SEC;
            } else
                interval = Constants.PERIOD_DEFAULT_SEC;
        } else
            interval = Constants.PERIOD_DEFAULT_SEC;
        return interval;
    }

    /**
     * Handler class for receiving messages from generator service
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mainActivityWeakReference;

        MyHandler(MainActivity mainActivity) {
            mainActivityWeakReference = new WeakReference<>(mainActivity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mainActivityWeakReference.get();
            if (mainActivity != null) {
                // display generated insult
                mainActivity.textInsultField.setText((CharSequence) msg.obj);
            }
        }
    }
}
