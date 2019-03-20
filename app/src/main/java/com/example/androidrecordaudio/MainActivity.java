package com.example.androidrecordaudio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.ibm.watson.developer_cloud.assistant.v2.Assistant;
import com.ibm.watson.developer_cloud.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageContext;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageInput;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageResponse;
import com.ibm.watson.developer_cloud.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.http.ServiceCall;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.AddWordsOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.RecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;


import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.UUID;

import static com.ibm.watson.developer_cloud.http.HttpHeaders.USER_AGENT;

public class MainActivity extends AppCompatActivity {

    //Declare variables
    Button btnListen,btnStopListen,submit;
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
    String priorText;

    Assistant watsonAssistant;
    private SessionResponse watsonAssistantSession;
    boolean initialRequest = false;
    Context mContext;
    MessageContext watsonContext;


    //static final String watsonUrl = getString(R.string.watson_assistant_url);
    //static final String POST_PARAMS = ""

//    Scene mScene = new Scene((ViewGroup) findViewById(R.id.home_layout), (ViewGroup) findViewById(R.id.inner_scene));
   // Transition fadeTransition = TransitionInflater.from(this).inflateTransition(R.transition.fade_transition);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();

        //Request variables
        speechService = initSpeechToTextService();
        transcript = (TextView) findViewById(R.id.transcript);
        microphoneHelper = new MicrophoneHelper(this);
        input = findViewById(R.id.inputText);
        textService = initTextToSpeechService();


        //add the request to the RequestQueue
        initialRequest = true;
        watsonAssistant = initAssistant();

        //Init View
        submit = (Button)findViewById(R.id.submit);
        btnListen = (Button)findViewById(R.id.btnStartListen);
        btnStopListen = (Button)findViewById(R.id.btnStopListen);

        //REQUEST RUN TIME PERMISSIONS
        if(checkPermissionFromDevice()){

            btnListen.setOnClickListener(new View.OnClickListener() {
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
                    //TransitionManager.go(mScene, fadeTransition);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            btnListen.setEnabled(false);
                            btnStopListen.setEnabled(true);
                        }
                    });


                    Toast.makeText(MainActivity.this,"Listening...",Toast.LENGTH_SHORT).show();


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

            btnStopListen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // mediaRecorder.stop();
                    sendMessage();
                    btnStopListen.setEnabled(false);
                    btnListen.setEnabled(true);
                    microphoneHelper.closeInputStream();
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
                priorText = text;
                new CountDownTimer(2000,1000){
                    public void onTick(long millisUntilFinished){
                    }

                    @Override
                    public void onFinish() {
                        Log.i("Sequence", priorText);
                        if(priorText.equals(text))
                        {
                            Log.i("Sequence 2", priorText);
                            btnStopListen.setEnabled(false);
                            btnListen.setEnabled(true);
                            microphoneHelper.closeInputStream();
                        }
                    }
                }.start();
                transcript.setText(text);
                priorText = text;
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
            btnListen.setEnabled(true);
            sendMessage();
            Log.d("MainActivity", "context: " + mContext.toString());
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
        String apiKey = getString(R.string.speech_text_iam_apikey);
        IamOptions iamOptions = new IamOptions.Builder().apiKey(apiKey).build();
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

    private Assistant initAssistant() {
        Assistant assistant = new Assistant("2019-20-03",
                new IamOptions.Builder().apiKey(getString(R.string.watson_assistant_apikey)).build());
        assistant.setEndPoint(getString(R.string.watson_assistant_url));
        return assistant;
    }

    private void sendMessage() {

        final String inputmessage = this.transcript.getText().toString();
        Thread thread = new Thread(new Runnable() {
            public void run() {
                try {
                    if (watsonAssistantSession == null) {
                        ServiceCall<SessionResponse> call =
                                watsonAssistant.createSession(new CreateSessionOptions.Builder().assistantId(mContext.getString(R.string.watson_assistant_id)).build());
                        watsonAssistantSession = call.execute();
                    }
                    MessageInput messageInput;

                    if(initialRequest){
                        messageInput = new MessageInput.Builder()
                                .text("")
                                .build();
                        initialRequest = false;
                    }
                    else {
                        messageInput = new MessageInput.Builder()
                                .text(inputmessage)
                                .build();
                    }
                    MessageOptions options = new MessageOptions.Builder()
                            .assistantId(mContext.getString(R.string.watson_assistant_id))
                            .input(messageInput)
                            .sessionId(watsonAssistantSession.getSessionId())
                            .build();
                    MessageResponse response = watsonAssistant.message(options).execute();
                    Log.i("MainActivity", "run: "+response);
                    final Message outMessage = new Message();
                    if (response != null &&
                            response.getOutput() != null &&
                            !response.getOutput().getGeneric().isEmpty() &&
                            "text".equals(response.getOutput().getGeneric().get(0).getResponseType())) {
                        input.setText(response.getOutput().getGeneric().get(0).getText());
                        // speak the message
                        new SynthesisTask().execute(input.getText().toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

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
            try {
                player.playStream(textService.synthesize(synthesizeOptions).execute());
            }
            catch(Exception e){
                showError(e);
            }
            return "Did synthesize";
        }
    }

    /*private static void sendPost() throws IOException{
        URL obj = new URL(watsonUrl);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", USER_AGENT);
        // For POST only - START
        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();
        // For POST only - END

        int responseCode = con.getResponseCode();
        System.out.println("POST Response Code :: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // print result
            System.out.println(response.toString());
        } else {
            System.out.println("POST request not worked");
        }
    }*/

}
