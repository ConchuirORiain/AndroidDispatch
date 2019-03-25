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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.CameraPosition;
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
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationListener;
import android.Manifest;

import static com.ibm.watson.developer_cloud.http.HttpHeaders.USER_AGENT;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,LocationListener {

    //Declare variables
    Button btnListen,btnStopListen,submit;
    String pathSave = "";
    MediaRecorder mediaRecorder;
    TextView transcript;

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
    boolean calling;
    boolean recording;
    boolean receiving;
    Context mContext;
    private GoogleMap mMap;
    private GoogleMap mgoogleMap;
    CameraPosition cameraPosition;
    LocationManager locationManager;
    Location locationCurrent;


    //static final String watsonUrl = getString(R.string.watson_assistant_url);
    //static final String POST_PARAMS = ""

//    Scene mScene = new Scene((ViewGroup) findViewById(R.id.home_layout), (ViewGroup) findViewById(R.id.inner_scene));
   // Transition fadeTransition = TransitionInflater.from(this).inflateTransition(R.transition.fade_transition);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = getApplicationContext();


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Request variables
        speechService = initSpeechToTextService();
        transcript = (TextView) findViewById(R.id.transcript);
        microphoneHelper = new MicrophoneHelper(this);
        input = findViewById(R.id.inputText);
        textService = initTextToSpeechService();


        //add the request to the RequestQueue
        initialRequest = true;
        calling = true;
        recording = false;
        receiving = false;
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
                    makeCall();


                }
            });

            btnStopListen.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   // mediaRecorder.stop();
                    //sendMessage();
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


    //requests permission for internet,storage,recording access
    private void requestPermission(){
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET
        },
                REQUEST_PERMISSION_CODE);

    }

    // just displays whether permission was granted or denied
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

    //returns true if the device has all permissions needed
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

    //should show the transcription of your speech, no longer works properly after connected to assistant
    //Also will terminate the call after a certain period of inactivity
    private void showTranscription(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                priorText = text;
                new CountDownTimer(1000,1000){
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
                priorText = text;
                transcript.setText(text);
            }
        });
    }

    //This makes the microphone available for speech to text
    private void makeCall(){
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

    //how speech to text is requested from ibm
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
            sendMessage();  //after speech is translated, automatically send it to watson
            Log.d("MainActivity", "context: " + mContext.toString());
        }
    }
    //speech to text builder
    private RecognizeOptions getRecognizeOptions(InputStream captureStream) {
        return new RecognizeOptions.Builder()
                .audio(captureStream)
                .contentType(ContentType.OPUS.toString())
                .model("en-US_BroadbandModel")
                .interimResults(true)
                .inactivityTimeout(2000)
                .build();
    }
    //initialising our speech to text service
    private SpeechToText initSpeechToTextService() {
        String apiKey = getString(R.string.speech_text_iam_apikey);
        IamOptions iamOptions = new IamOptions.Builder().apiKey(apiKey).build();
        SpeechToText service = new SpeechToText(iamOptions);
        service.setEndPoint(getString(R.string.speech_text_url));
        return service;
    }
    //initialising text to speech service
    private TextToSpeech initTextToSpeechService() {
        Log.d("test", "in init t2s");
        String apiKey = getString(R.string.text_speech_iam_apikey);
        IamOptions iamOptions = new IamOptions.Builder().apiKey(apiKey).build();
        TextToSpeech service = new TextToSpeech(iamOptions);
        service.setEndPoint(getString(R.string.text_speech_url));
        return service;
    }
    //initialising assistant
    private Assistant initAssistant() {
        Assistant assistant = new Assistant("2019-20-03",
                new IamOptions.Builder().apiKey(getString(R.string.watson_assistant_apikey)).build());
        assistant.setEndPoint(getString(R.string.watson_assistant_url));
        return assistant;
    }
    //sends text to watson and receives text, then should play it (doesn't play initial watson message for some reason)
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
                        // loop through the rponses and concatenate the text
                        String fullText = "";
                        for(int i = 0; i < response.getOutput().getGeneric().size(); i++){
                            fullText += response.getOutput().getGeneric().get(i).getText();
                        }
                        Log.d("fulltext", fullText);
                        input.setText(fullText);//response.getOutput().getGeneric().get(0).getText());
                        // speak the message
                        if(input.getText().equals("I found no incidents matching that description. Please wait while I transfer you to an operator"))
                            calling = false;
                        new SynthesisTask().execute(fullText);//response.getOutput().getGeneric().get(0).getText());
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

    //this is text to speech function (call by saying "new SynthesisTask().execute("the string you want converted to speech")"
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

    @Override
    public void onMapReady(GoogleMap googleMap) {

        LatLng myLatLng;
        MapsInitializer.initialize(mContext);
        mgoogleMap = googleMap;
        mgoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        cameraPosition = CameraPosition.builder().target(new LatLng(53.3498, -6.2603)).zoom(11).bearing(0).build();
        mgoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
                // locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, this);
                locationCurrent = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                onLocationChanged(locationCurrent);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
            }
            catch(SecurityException e) {
                Log.i("LOCATION", "onCreateView: Cannot aquire location");
            }
            LatLng latLng = new LatLng(locationCurrent.getLatitude(),locationCurrent.getLongitude());
            CameraPosition cameraPosition= new CameraPosition(latLng,15,0,0);
            CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);
            mgoogleMap.animateCamera(cameraUpdate);

//
//            if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
//                    PackageManager.PERMISSION_GRANTED &&
//                    ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
//                            PackageManager.PERMISSION_GRANTED) {
            mgoogleMap.setMyLocationEnabled(true);
            mgoogleMap.getUiSettings().setMyLocationButtonEnabled(true);

//            }


        }
// else {
//            requestPermissions(new String[]{
//                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
//        }
//        mMap = googleMap;
//
//        // Add a marker in Sydney, Australia, and move the camera.
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }


    public void onLocationChanged(Location location) {
        UpdateMap(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


    public void UpdateMap(Location location) {

        if(mgoogleMap != null) {
            LatLng latLngUpdate = new LatLng(location.getLatitude(), location.getLongitude());
            mgoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngUpdate , mgoogleMap.getCameraPosition().zoom));
        }
    }





}
