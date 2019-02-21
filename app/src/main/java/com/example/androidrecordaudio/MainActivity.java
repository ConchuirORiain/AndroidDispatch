package com.example.androidrecordaudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Declare variables
    Button btnRecord,btnStopRecord,btnPlay,btnStop;
    String pathSave = "";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;

    final int REQUEST_PERMISSION_CODE =1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init View
        btnPlay = (Button)findViewById(R.id.btnPlay);
        btnStop = (Button)findViewById(R.id.btnStop);
        btnRecord = (Button)findViewById(R.id.btnStartRecord);
        btnStopRecord = (Button)findViewById(R.id.btnStopRecord);

        //REQUEST RUN TIME PERMISSIONS
        if(checkPermissionFromDevice()){
            btnRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pathSave = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"
                            + UUID.randomUUID().toString()+"_project_audio_.3gp";
                    setupMediaRecorder();
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        btnStopRecord.setEnabled(true);
                        btnPlay.setEnabled(false);
                        btnRecord.setEnabled(false);
                        btnStop.setEnabled(false);
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }

                    btnPlay.setEnabled(false);
                    btnStop.setEnabled(false);

                    Toast.makeText(MainActivity.this,"Recording...",Toast.LENGTH_SHORT).show();
                }
            });

            btnStopRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mediaRecorder.stop();
                    btnStopRecord.setEnabled(false);
                    btnPlay.setEnabled(true);
                    btnRecord.setEnabled(true);
                    btnStop.setEnabled(false);
                }
            });

            btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnStop.setEnabled(true);
                    btnStopRecord.setEnabled(false);
                    btnRecord.setEnabled(true);
                    mediaPlayer = new MediaPlayer();
                    try{
                        mediaPlayer.setDataSource(pathSave);
                        mediaPlayer.prepare();
                    }
                    catch(IOException e){
                        e.printStackTrace();
                    }

                    mediaPlayer.start();
                    Toast.makeText(MainActivity.this,"Playing...", Toast.LENGTH_SHORT).show();
                }
            });

            btnStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    btnStopRecord.setEnabled(false);
                    btnPlay.setEnabled(true);
                    btnRecord.setEnabled(true);
                    btnStop.setEnabled(false);

                    if(mediaPlayer != null){
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        setupMediaRecorder();
                    }
                }
            });
        }
        else{
            requestPermission();
        }
    }

    private void setupMediaRecorder(){
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(pathSave);
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        },
                REQUEST_PERMISSION_CODE);

    }

@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,@NonNull int[] grantResults){
        switch(requestCode){
            case REQUEST_PERMISSION_CODE:
            {
                   if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                       Toast.makeText(this,"Permission Granted", Toast.LENGTH_SHORT).show();
                   }
                   else{
                       Toast.makeText(this,"Permission Denied", Toast.LENGTH_SHORT).show();
                   }
            }
                break;
        }
}

    private boolean checkPermissionFromDevice() {
        int writeExternalStorageResult =
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int recordAudioResult =
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        return writeExternalStorageResult == PackageManager.PERMISSION_GRANTED &&
                recordAudioResult == PackageManager.PERMISSION_GRANTED;
    }
}
