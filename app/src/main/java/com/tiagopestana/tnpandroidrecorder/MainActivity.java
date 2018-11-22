package com.tiagopestana.tnpandroidrecorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button btnPlay, btnRec, btnSave;
    private MediaRecorder audioRecorder;
    private MediaPlayer mediaPlayer;
    private File outputFile;
    private Chronometer timer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    Animation recordingAnimation;

    private static String audioFileName = "tnpRecording";

    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission
            .READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    // ##############    Lifecycle    ##############
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRec = findViewById(R.id.btnRec);
        btnPlay = findViewById(R.id.btnPlay);
        btnSave = findViewById(R.id.btnSave);
        btnPlay.setEnabled(false);
        btnSave.setEnabled(false);

        timer = findViewById(R.id.textView);
        
        recordingAnimation = new AlphaAnimation(1, 0.6f);
        recordingAnimation.setDuration(250);
        recordingAnimation.setInterpolator(new LinearInterpolator());
        recordingAnimation.setRepeatCount(Animation.INFINITE);
        recordingAnimation.setRepeatMode(Animation.REVERSE);

        // ##############    onClick Listeners    ##############
        btnRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    if (arePermissionsEnabled()) {
                        startRecording();
                        btnRec.startAnimation(recordingAnimation);
                    } else {
                        requestMultiplePermissions();
                    }
                } else {
                    stopRecording();
                    btnRec.clearAnimation();
                }
            }
        });
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecording();
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File publicFile;
                try {
                    publicFile = createAudioFile();
                    copyFile(outputFile, publicFile);
                    Toast.makeText(getApplicationContext(),
                            "File saved",
                            Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (outputFile!=null)
            if (outputFile.exists())
                if (outputFile.delete())
                    Log.w("startRecording", "previous file deleted");
        super.onDestroy();
    }

    // ##############    Recorder Management    ##############
    private void startRecording() {
        audioRecorder = new MediaRecorder();

        if (outputFile!=null)
            if (outputFile.exists())
                if (outputFile.delete())
                    Log.w("startRecording", "previous file deleted");


        try {
            outputFile = createInternalAudioFile(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        audioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        audioRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            audioRecorder.prepare();
            audioRecorder.start();
        } catch (IllegalStateException ise) {
            Log.w("startRecording", "audiorecorder.start IllegalStateExecution");
        } catch (IOException ioe) {
            Log.w("startRecording", "audiorecorder.start IOException");
        }

        isRecording = true;
        Toast.makeText(getApplicationContext(),
                "Recording started",
                Toast.LENGTH_LONG).show();

        btnPlay.setEnabled(false);
        btnSave.setEnabled(false);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
    }

    private void stopRecording() {
        if (audioRecorder != null) {
            try {
                audioRecorder.stop();
                audioRecorder.reset();
                audioRecorder.release();
            } catch (IllegalStateException ise) {
                Log.w("stopRecording", "audiorecorder.stop IllegalStateException");

            }

            audioRecorder = null;
            btnPlay.setEnabled(true);
            timer.stop();
            isRecording = false;

            btnPlay.setEnabled(true);
            btnSave.setEnabled(true);
        }
    }

    private void playRecording() {
        if(!isPlaying) {
            mediaPlayer = new MediaPlayer();
            try {
                mediaPlayer.setDataSource(outputFile.getAbsolutePath());
                mediaPlayer.prepare();
                mediaPlayer.start();

            } catch (IOException ioe) {
                Log.w("playRecording", "mediaplayer.start IllegalStateException");
            }
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    timer.stop();
                    btnPlay.setText(R.string.playButtonPlay);
                    btnSave.setEnabled(true);
                }
            });
            isPlaying = true;
            btnPlay.setText(R.string.playButtonStop);
            btnSave.setEnabled(false);
            timer.setBase(SystemClock.elapsedRealtime());
            timer.start();
        } else
        {
            mediaPlayer.stop();
            timer.stop();
            btnPlay.setText(R.string.playButtonPlay);
            btnSave.setEnabled(true);
            isPlaying = false;
        }
    }

    // ##############    File Management    ##############
    // Create file in internal storage so it can be played by the app.
    private static File createInternalAudioFile(Context context) throws IOException {
        return File.createTempFile(audioFileName, ".mp3", context.getFilesDir());
    }
    // Create public file in Music directory.
    private static File createAudioFile() {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        String timeStamp = new SimpleDateFormat("_yyyy-MM-dd_HH'h'mm'm'ss's'", Locale.US).format(new Date());
        return new File(storageDir, audioFileName + timeStamp + ".mp3");
    }

    public static void copyFile(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out.
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    // ##############    Permission Management    ##############
    // Run-time request for permissions.
    private boolean arePermissionsEnabled() {
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
    // Request all the permissions stored in the "permissions" array and store all the declined
    // permissions in a new "remainingPermissions" array.
    private void requestMultiplePermissions() {
        List<String> remainingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                remainingPermissions.add(permission);
            }
        }
        requestPermissions(remainingPermissions.toArray(new String[0]), 101);
    }
    // Handling callback.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (shouldShowRequestPermissionRationale(permissions[i])) {
                        new AlertDialog.Builder(this)
                                .setMessage("I'm sorry but both permissions are needed for the " +
                                        "recording to work correctly, please consider accepting those.")
                                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        MainActivity.this.requestMultiplePermissions();
                                    }
                                })
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .create()
                                .show();
                    }
                    return;
                }
            }
            // If all permissions are granted we force the recording to start by performing a second
            // click. It would possibly be better if we called startRecording() to avoid additional
            // permission checking.
            btnRec.performClick();
        }
    }
}