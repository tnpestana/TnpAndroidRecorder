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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Button btnPlay, btnRec;
    private MediaRecorder audioRecorder;
    private MediaPlayer mediaPlayer;
    private File outputFile;
    private Chronometer timer;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    Animation alphaAnimation;

    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission
            .READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRec = findViewById(R.id.btnRec);
        btnPlay = findViewById(R.id.btnPlay);
        btnPlay.setEnabled(false);

        timer = findViewById(R.id.textView);

        // create recording button animations
        alphaAnimation = new AlphaAnimation(1, 0.6f); // Change alpha from fully visible to invisible
        alphaAnimation.setDuration(250); // duration - half a second
        alphaAnimation.setInterpolator(new LinearInterpolator()); // do not alter animation rate
        alphaAnimation.setRepeatCount(Animation.INFINITE); // Repeat animation infinitely
        alphaAnimation.setRepeatMode(Animation.REVERSE); // Reverse animation at the end so the button will fade back in

        btnRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    if (arePermissionsEnabled()) {
                        startRecording();
                        btnRec.startAnimation(alphaAnimation);
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
    }

    private void startRecording() {
        audioRecorder = new MediaRecorder();
        try {
            outputFile = createAudioFile(this, "tnpRecording");
        } catch (IOException e) {
            e.printStackTrace();
        }
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
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

            Toast.makeText(getApplicationContext(),
                    "Recording stopped",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playRecording() {
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(outputFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            isPlaying = true;
        } catch (IOException ioe) {
            Log.w("playRecording", "mediaplayer.start IllegalStateException");
        }

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                timer.stop();
            }
        });

        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
        Toast.makeText(getApplicationContext(),
                "Playing recorded file",
                Toast.LENGTH_LONG).show();
    }

    // Audio file
    private static File createAudioFile(Context context, String audioName) throws IOException {
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        return File.createTempFile(audioName, ".3pg", storageDir);
    }

    // Requesting run-time permissions.
    // Check if all necessary permissions are granted.
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
        requestPermissions(remainingPermissions.toArray(new String[remainingPermissions.size()]), 101);
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