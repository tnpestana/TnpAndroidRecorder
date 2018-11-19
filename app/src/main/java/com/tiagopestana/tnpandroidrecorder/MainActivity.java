package com.tiagopestana.tnpandroidrecorder;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Button btnPlay, btnRec;
    private MediaRecorder audioRecorder;
    private File outputFile;
    private Chronometer timer;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnRec = (Button) findViewById(R.id.btnRec);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnPlay.setEnabled(false);

        timer = (Chronometer) findViewById(R.id.textView);

        try {
            outputFile = createAudioFile(this, "demo");
        } catch (IOException e) {
            e.printStackTrace();
        }

        btnRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isRecording)
                    requestAudioPermissions();
                else
                    stopRecording();
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playRecording();
            }
        });
    }

    private static File createAudioFile(Context context, String audioName) throws IOException {
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        return File.createTempFile(audioName, ".3pg", storageDir);
    }

    private void startRecording()
    {
        audioRecorder = new MediaRecorder();

        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        audioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        audioRecorder.setOutputFile(outputFile.getAbsolutePath());

        try {
            audioRecorder.prepare();
            audioRecorder.start();
        } catch (IllegalStateException ise) {
            // handle
        } catch (IOException ioe) {
            // handle
        }

        isRecording = true;
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();

        Toast.makeText(getApplicationContext(),
                "Recording started",
                Toast.LENGTH_LONG).show();
    }

    private void stopRecording() {
        if (audioRecorder != null) {
            try {
                audioRecorder.stop();
                audioRecorder.reset();
                audioRecorder.release();
            } catch (IllegalStateException ise) {
                Toast.makeText(getApplicationContext(),
                        "audio recorder.stop() -> IllegalStateException",
                        Toast.LENGTH_LONG).show();
            }

            audioRecorder = null;
            btnPlay.setEnabled(true);
            timer.stop();
            isRecording = true;
            Toast.makeText(getApplicationContext(),
                    "Recording stopped",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void playRecording() {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(outputFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();
            Toast.makeText(getApplicationContext(),
                    "Playing recorded file",
                    Toast.LENGTH_LONG).show();
        } catch (IOException ioe) {
            // lidar
        }
    }

    //Requesting run-time permissions

    //Create placeholder for user's consent to record_audio permission.
    //This will be used in handling callback
    private final int MY_PERMISSIONS_RECORD_AUDIO = 1;

    private void requestAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            //When permission is not granted by user, show them message why this permission is needed.
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this,
                        "Please grant permissions to record audio",
                        Toast.LENGTH_LONG).show();

                //Give user option to still opt-in the permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);

            } else {
                // Show user dialog to grant permission to record audio
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        MY_PERMISSIONS_RECORD_AUDIO);
            }
        }
        //If permission is granted, then go ahead recording audio
        else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {

            //Go ahead with recording audio now
            startRecording();
        }
    }

    //Handling callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    startRecording();
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this,
                            "Permissions Denied to record audio",
                            Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}
