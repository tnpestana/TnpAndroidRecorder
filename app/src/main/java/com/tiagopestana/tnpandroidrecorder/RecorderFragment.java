package com.tiagopestana.tnpandroidrecorder;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RecorderFragment extends Fragment {

    private Button btnPlay, btnRec, btnSave;
    private MediaRecorder audioRecorder;
    private MediaPlayer mediaPlayer;
    private File outputFile;
    private Chronometer timer;
    long pauseOffset;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isSaved = false;
    Animation recordingAnimation;
    private static String audioFileName = "tnpRecording";
    private String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission
            .READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};

    public RecorderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_recorder, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Manage UI components here.
        btnRec = getView().findViewById(R.id.btnRec);
        btnPlay = getView().findViewById(R.id.btnPlay);
        btnSave = getView().findViewById(R.id.btnSave);
        timer = getView().findViewById(R.id.textView);

        recordingAnimation = new ScaleAnimation(1f, 0.9f, 1f, 0.9f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        recordingAnimation.setDuration(250);
        recordingAnimation.setInterpolator(new LinearInterpolator());
        recordingAnimation.setRepeatCount(Animation.INFINITE);
        recordingAnimation.setRepeatMode(Animation.REVERSE);

        if(isPlaying)
        {
            btnRec.setEnabled(false);
            btnPlay.setText(R.string.playButtonStop);
            btnPlay.setEnabled(true);
            btnSave.setEnabled(false);
            timer.setBase(SystemClock.elapsedRealtime() - savedInstanceState.getLong("time"));
            timer.start();
        }else if (isRecording) {
            btnPlay.setEnabled(false);
            btnPlay.setText(R.string.playButtonPlay);
            btnSave.setEnabled(false);
            btnRec.startAnimation(recordingAnimation);
            timer.setBase(SystemClock.elapsedRealtime() - savedInstanceState.getLong("time"));
            timer.start();
        } else {
            if(outputFile == null) {
                btnSave.setEnabled(false);
                btnPlay.setEnabled(false);
            } else {
                if(isSaved) btnSave.setEnabled(false);
                timer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            }
        }

        btnRec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    if (arePermissionsEnabled()) {
                        startRecording();
                        btnSave.setEnabled(false);
                        btnRec.startAnimation(recordingAnimation);
                    } else {
                        requestMultiplePermissions();
                    }
                } else {
                    stopRecording();
                    btnSave.setEnabled(true);
                    btnRec.clearAnimation();
                }
            }
        });

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isPlaying) {
                    startPlaying();
                } else {
                    stopPlaying();
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File publicFile;
                try {
                    publicFile = createAudioFile();
                    copyFile(outputFile, publicFile);
                    btnSave.setEnabled(false);
                    isSaved = true;
                    Toast.makeText(getContext(),
                            "File saved",
                            Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (outputFile!=null)
            if (outputFile.exists())
                if (outputFile.delete())
                    Log.i("onDestroy", "previous file deleted");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save current chronometer value
        outState.putLong("time", SystemClock.elapsedRealtime() - timer.getBase());
    }

    private void startRecording() {
        audioRecorder = new MediaRecorder();

        if (outputFile!=null)
            if (outputFile.exists())
                if (outputFile.delete())
                    Log.i("startRecording", "previous file deleted");

        try {
            outputFile = createInternalAudioFile(getContext());
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            audioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            audioRecorder.setOutputFile(outputFile.getAbsolutePath());
            audioRecorder.prepare();
            audioRecorder.start();
        } catch (IllegalStateException ise) {
            Log.w("startRecording", "audiorecorder.start IllegalStateExecution");
        } catch (IOException ioe) {
            Log.w("startRecording", "audiorecorder.start IOException");
        }

        isRecording = true;
        isSaved = false;
        btnPlay.setEnabled(false);
        btnSave.setEnabled(false);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();

        Toast.makeText(getContext(),
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
                Log.w("stopRecording", "audiorecorder.stop IllegalStateException");

            }
            audioRecorder = null;
            timer.stop();
            pauseOffset = SystemClock.elapsedRealtime() - timer.getBase();
            isRecording = false;

            btnPlay.setEnabled(true);
            btnSave.setEnabled(true);
        }
    }

    private void startPlaying() {
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
                pauseOffset = SystemClock.elapsedRealtime() - timer.getBase();
                btnPlay.setText(R.string.playButtonPlay);
                if(isSaved) {
                    btnSave.setEnabled(false);
                } else {
                    btnSave.setEnabled(true);
                }
                isPlaying = false;
            }
        });
        isPlaying = true;
        btnPlay.setText(R.string.playButtonStop);
        btnSave.setEnabled(false);
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
    }

    private void stopPlaying(){
        mediaPlayer.stop();
        timer.stop();
        pauseOffset = SystemClock.elapsedRealtime() - timer.getBase();
        btnPlay.setText(R.string.playButtonPlay);
        btnSave.setEnabled(true);
        isPlaying = false;
        if(isSaved) {
            btnSave.setEnabled(false);
        } else {
            btnSave.setEnabled(true);
        }
    }

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

    // Run-time request for permissions.
    private boolean arePermissionsEnabled() {
        for (String permission : permissions) {
            if (getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED)
                return false;
        }
        return true;
    }
    // Request all the permissions stored in the "permissions" array and store all the declined
    // permissions in a new "remainingPermissions" array.
    private void requestMultiplePermissions() {
        List<String> remainingPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (getActivity().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
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
                        new AlertDialog.Builder(getActivity())
                                .setMessage("I'm sorry but both permissions are needed for the " +
                                        "recording to work correctly, please consider accepting those.")
                                .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestMultiplePermissions();
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
