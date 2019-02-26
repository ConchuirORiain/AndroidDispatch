package com.example.androidrecordaudio;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneHelper;
import com.ibm.watson.developer_cloud.android.library.audio.MicrophoneInputStream;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.android.library.audio.utils.ContentType;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Declare variables
    Button btnRecord,btnStopRecord,btnPlay,btnStop,submit;
    String pathSave = "";
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    TextView transcript;
    AudioRecord audioRecord;

    final int SAMPLING_RATE = 32000;
    final int REQUEST_PERMISSION_CODE =1000;

    SpeechToText speechService;
    MicrophoneInputStream capture;
    MicrophoneHelper microphoneHelper;

    TextToSpeech textService;
    EditText input;
    StreamPlayer player = new StreamPlayer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Request variables
        speechService = initSpeechToTextService();
        transcript = (TextView) findViewById(R.id.transcript);
        microphoneHelper = new MicrophoneHelper(this);
        input = findViewById(R.id.inputText);
        textService = initTextToSpeechService();


        //add the request to the RequestQueue

        //Init View
        submit = (Button)findViewById(R.id.submit);
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
                   // setupMediaRecorder();
                   // try {
                   //     mediaRecorder.prepare();
                   //     mediaRecorder.start();
                  //      btnStopRecord.setEnabled(true);
                  //      btnPlay.setEnabled(false);
                  //      btnRecord.setEnabled(false);
                  //      btnStop.setEnabled(false);
                  //  }
                  //  catch (IOException e){
                 //       e.printStackTrace();
                //    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnRecord.setEnabled(false);
                            btnPlay.setEnabled(false);
                            btnStop.setEnabled(false);
                            btnStopRecord.setEnabled(true);
                        }
                    });


                    Toast.makeText(MainActivity.this,"Recording...",Toast.LENGTH_SHORT).show();


                    capture = microphoneHelper.getInputStream(true);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                speechService.recognizeUsingWebSocket(getRecognizeOptions(capture),
                                        new MicrophoneRecognizeDelegate());
                            }
                            catch(Exception e){
                                showError(e);
                            }
                        }
                    }).start();

                }
            });

            btnStopRecord.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // mediaRecorder.stop();
                    btnStopRecord.setEnabled(false);
                    btnPlay.setEnabled(true);
                    btnRecord.setEnabled(true);
                    btnStop.setEnabled(false);
                    microphoneHelper.closeInputStream();
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

            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(input.length() != 0)
                    new SynthesisTask().execute(input.getText().toString());
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
        mediaRecorder.setAudioSamplingRate(SAMPLING_RATE);
        mediaRecorder.setOutputFile(pathSave);
    }

    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
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
        int internetAccessResult =
                ContextCompat.checkSelfPermission(this,Manifest.permission.INTERNET);
        return writeExternalStorageResult == PackageManager.PERMISSION_GRANTED &&
                recordAudioResult == PackageManager.PERMISSION_GRANTED &&
                internetAccessResult == PackageManager.PERMISSION_GRANTED;
    }

    private void showTranscription(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                transcript.setText(text);
            }
        });
    }

    private class MicrophoneRecognizeDelegate extends BaseRecognizeCallback {
        @Override
        public void onTranscription(SpeechRecognitionResults speechResults) {
            System.out.println(speechResults);
            if (speechResults.getResults() != null && !speechResults.getResults().isEmpty()) {
                String text = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();
                showTranscription(text);
            }
        }
        @Override
        public void onError(Exception e) {
            try {
                // This is critical to avoid hangs
                // (see https://github.com/watson-developer-cloud/android-sdk/issues/59)
                capture.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            showError(e);
        }
        @Override
        public void onDisconnected(){
            btnRecord.setEnabled(true);
        }
    }

    private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
        return new RecognizeOptions.Builder()
                .audio(captureStream)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }

    private SpeechToText initSpeechToTextService() {
        IamOptions iamOptions = new IamOptions.Builder().apiKey("MlQXA7DTolsGoSCn2pDASfWEFZ6be5hancjsjElj3k3A").build();
        SpeechToText service = new SpeechToText(iamOptions);
        service.setEndPoint(getString(R.string.speech_text_url));
        return service;
    }

    private TextToSpeech initTextToSpeechService() {
        String apiKey = getString(R.string.text_speech_iam_apikey);
        IamOptions iamOptions = new IamOptions.Builder().apiKey(apiKey).build();
        TextToSpeech service = new TextToSpeech(iamOptions);
        service.setEndPoint(getString(R.string.text_speech_url));
        return service;
    }

    private void showError(final Exception e) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    private class SynthesisTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            SynthesizeOptions synthesizeOptions = new SynthesizeOptions.Builder()
                    .text(params[0])
                    .voice(SynthesizeOptions.Voice.EN_US_LISAVOICE)
                    .accept(SynthesizeOptions.Accept.AUDIO_WAV)
                    .build();
            player.playStream(textService.synthesize(synthesizeOptions).execute());
            return "Did synthesize";
        }
    }


}