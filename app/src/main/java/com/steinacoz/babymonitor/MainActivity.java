package com.steinacoz.babymonitor;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.*;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.enums.PNStatusCategory;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;

import java.util.Arrays;


public class MainActivity extends Activity {

    //private MediaPlayer mediaPlayer;
    // private SurfaceHolder vidHolder;
    private WebView webSurface;
    String vidAddress = "http://192.168.43.253/view";

    PubnubConfig pubnubConfig;
    PubNub pubnub;

    //create textViews widgets
    TextView tempTxtview, humidTxtview, statusTxtview, noiseTxtview, pulseTxtview;

    ToggleButton musicbtn, modebtn;
    Button fotobtn;


    PubConSubThread pubConSubThread;// new PubConSubThread();
    PubPublishThread pubPublishThread;// new PubPublishThread();

    //pubnub channels

    String MODE = "MODE";
    String SENSOR_DATA = "SENSOR_DATA";
    String SENSORS_TEMP = "SENSORS_TEMP";
    String SENSORS_HUMID = "SENSORS_HUMID";
    String SENSORS_PULSE = "SENSORS_PULSE";
    String BATTERY = "BATTERY";
    String NOISE = "NOISE";
    String FEEDBACKS = "FEEDBACKS";
    String MUSIC = "MUSIC";
    String PHOTO = "PHOTO";

    //payloads
    String music_on = "music on";
    String music_off = "music_off";

    String mode_change_auto = "auto mode";
    String mode_change_manual = "manual mode";

    String sensors_Data = "get data";

    String MOTION_PIR = "MOTION DETECTED";

    String take_photo = "take photo";

    // message and channel that will be used during publishing
    String payload;
    String channel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize Pubnub Configuration and PubNub
        pubnubConfig = new PubnubConfig();
        pubnub = new PubNub(pubnubConfig.pConfig());

        tempTxtview = (TextView) findViewById(R.id.temp_txtView);
        humidTxtview = (TextView) findViewById(R.id.humid_textView);
        statusTxtview = (TextView) findViewById(R.id.status_txtView);
        noiseTxtview = (TextView) findViewById(R.id.cry_txtView);
        pulseTxtview = (TextView) findViewById(R.id.pulse_txtView);


        musicbtn = (ToggleButton) findViewById(R.id.musicbtn);
        modebtn = (ToggleButton) findViewById(R.id.modebtn);
        fotobtn = (Button) findViewById(R.id.fotobtn);


        webSurface = (WebView) findViewById(R.id.webView);
        webSurface.setWebChromeClient(new WebChromeClient());
        webSurface.getSettings().setPluginState(WebSettings.PluginState.ON_DEMAND);
        webSurface.getSettings().setJavaScriptEnabled(true);
        webSurface.loadUrl(vidAddress);

        //subscribe to pubnub channels
        pubnubSubscribe();



        musicbtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                musicbtnChanged(b);
            }
        });

        modebtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                modebtnChanged(b);
            }
        });

        fotobtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeFoto();
            }
        });


    }

    void pubnubSubscribe(){

        pubPublishThread = new PubPublishThread();
        pubConSubThread = new PubConSubThread();


        try {

            pubConSubThread.start();
            pubPublishThread.start();
        }catch (Exception e){
            statusTxtview.setText(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    //this method is called from the PubConSub thread
    public void updateStatusTxtView(String message){
        statusTxtview.setText(message);
    }

    /**this method is called from the Pubpublish thread
     and updates the status textView **/
    public void updatePubTextView(int colour, String publishStatus){
        statusTxtview.setTextColor(colour);
        statusTxtview.setText(publishStatus);
    }

    /**this method is called from the PubConSub thread
     and updates the connect textView **/
    public void updateConnectTextView(int colour, String connectStatus){
        statusTxtview.setTextColor(colour);
        statusTxtview.setText(connectStatus);
    }

    public void updateNoiseTextView(String noise){
        noiseTxtview.setText(noise);
    }

    public void updatePulseTextView(String pulse){
        pulseTxtview.setText(pulse);
    }





    /**this method is called from the Pubpublish thread
     and updates the amb. temp textView**/
    public void updateTempTextView(String value){
        tempTxtview.setText(value);
        }


    /**this method is called from the Pubpublish thread
     and updates the status textView **/
    public void updateHumdityTextView(String value){
        humidTxtview.setText(value);
    }


    void modebtnChanged(boolean chk) {
        if (chk) {
            pubPublishThread.pubnubPublish(mode_change_auto, MODE);
        } else {
            pubPublishThread.pubnubPublish(mode_change_manual, MODE);
        }
    }

    void musicbtnChanged(boolean chk) {
        if (chk) {
            pubPublishThread.pubnubPublish(music_on, MUSIC);
        } else {
            pubPublishThread.pubnubPublish(music_off, MUSIC);
        }
    }

    void takeFoto(){
        fotobtn.setBackgroundColor(Color.BLUE);
        pubPublishThread.pubnubPublish(take_photo, PHOTO);
    }


    //subcribe class outside UI thread
    public class PubConSubThread extends Thread {
        public static final String Tag = "Pubnub Connect Subscribe Thread";
        private static final int DELAY = 5000;

        PubPublishThread pp = new PubPublishThread();

        //MainActivity mainActivity = new MainActivity();
        private String updateToast (String tt){
            return tt;
        }

        @Override
        public void run() {
            try {
                pubnubConSubscribe();
            }catch (Exception e){
                final String err = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        //pubnub connect and subscribe
        private void pubnubConSubscribe(){

            connectProgress(Color.RED, "Connecting");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "inside pubnubConSubscribe method", Toast.LENGTH_SHORT).show();
                }
            });


            //final String direction = "direction";


            pubnub.addListener(new SubscribeCallback() {
                @Override
                public void status(PubNub pubnub, PNStatus status) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "inside status pubnub addlistener", Toast.LENGTH_SHORT).show();
                        }
                    });

                    if (status.getCategory() == PNStatusCategory.PNUnexpectedDisconnectCategory){
                        connectProgress(Color.RED, "Disconnected");
                        pubnub.reconnect();
                    }else if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                        pp.pubnubPublish(sensors_Data, SENSOR_DATA);
                        if (status.getCategory() == PNStatusCategory.PNConnectedCategory){
                            connectProgress(Color.GREEN, "Connected");
                            //pubnub.subscribe().channels(Arrays.asList(FEEDBACKS, SENSOR_DATA, SENSORS_HUMID, SENSORS_SOIL, SENSORS_TEMP, CAMERA, IRRIGATE )).execute();

                        }else if (status.getCategory() == PNStatusCategory.PNReconnectedCategory){
                            pp.pubnubPublish(sensors_Data, SENSOR_DATA);
                            connectProgress(Color.GREEN, "Reconnected");
                            // pubnub.subscribe().channels(Arrays.asList(FEEDBACKS, SENSOR_DATA, SENSORS_HUMID, SENSORS_SOIL, SENSORS_TEMP, CAMERA, IRRIGATE )).execute();

                        }else if (status.getCategory() == PNStatusCategory.PNDecryptionErrorCategory){
                            pubnub.reconnect();

                        }else if (status.getCategory() == PNStatusCategory.PNTimeoutCategory){
                            connectProgress(Color.RED, "Network Timeout");
                            pubnub.reconnect();

                        }else {
                            pubnub.reconnect();

                            connectProgress(Color.RED, "No Connection");
                        }
                    }
                }

                @Override
                public void message(PubNub pubnub, final PNMessageResult message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (message.getChannel().equalsIgnoreCase(FEEDBACKS)) {
                                //updateStatusTxtView(message.getMessage().getAsString());

                            }else if (message.getChannel().equalsIgnoreCase(SENSORS_TEMP)){
                                updateTempTextView(message.getMessage().getAsString());

                            }else if (message.getChannel().equalsIgnoreCase(SENSOR_DATA)){
                                updateStatusTxtView(message.getMessage().getAsString());

                            }else if (message.getChannel().equalsIgnoreCase(SENSORS_HUMID)){
                                updateHumdityTextView(message.getMessage().getAsString());

                            }else if (message.getChannel().equalsIgnoreCase(PHOTO)){
                                updateStatusTxtView(message.getMessage().getAsString());

                            }else if (message.getChannel().equalsIgnoreCase(NOISE)){
                                updateNoiseTextView(message.getMessage().getAsString());

                            }else if (message.getChannel().equalsIgnoreCase(SENSORS_PULSE)){
                                updatePulseTextView(message.getMessage().getAsString());
                            }


                        }
                    });
                }

                @Override
                public void presence(PubNub pubnub, PNPresenceEventResult presence) {

                }
            });

            pubnub.subscribe().channels(Arrays.asList(FEEDBACKS, SENSOR_DATA, SENSORS_HUMID, SENSORS_TEMP, MODE, PHOTO, MUSIC, SENSORS_PULSE, BATTERY, NOISE )).execute();
        }

        private void connectProgress(int colour, String connectStatus){
            final int col = colour;
            final String cStatus = connectStatus;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateConnectTextView(col, cStatus);
                }
            });

        }


    }

    //publish class thread
    public class PubPublishThread extends Thread {
        public static final String Tag = "Pubnub Publish Thread";
        private static final int DELAY = 5000;

        MainActivity mainActivity = new MainActivity();


        @Override
        public void run() {
            super.run();

        }



        public void pubnubPublish(String payload, String channel){

            try {
                pubnub.publish().message(payload).channel(channel).async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (!status.isError()) {
                            publishProgress(Color.GREEN, "publish success");
                        } else {
                            publishProgress(Color.RED, status.getCategory().toString());
                            status.retry();
                        }

                    }
                });
            }catch (Exception e){
                final String err = e.getMessage();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, err, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        private void publishProgress(final int colour, final String publishStatus){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updatePubTextView(colour, publishStatus);
                }
            });
        }
    }






}
