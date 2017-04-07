package example.android.watsonmail;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    StreamPlayer streamPlayer = null;
    private static String TAG = "MainActivity";
    public static TextToSpeech mainService;
    Queue<AsyncTask> taskQueue ;
    Queue<StreamPlayer> streamQueue;
    Speak varSpeak;
    private int numberOfClicks = 0;
    String emailId = "",password = "";
    private int statusConfirm = 0;
    Context context;
    private android.speech.tts.TextToSpeech tts;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        context = this;
        tts = new android.speech.tts.TextToSpeech(context, new android.speech.tts.TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.US);
                    if (result == android.speech.tts.TextToSpeech.LANG_MISSING_DATA || result == android.speech.tts.TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "This Language is not supported");
                    }
                    if(Configuration.noNetwork==true)
                    {
                        speak("Please check your network connection and Try again later ");
                        Log.e(TAG,"No Network Connection");
                        try {
                            exitFromApp();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    Log.e("TTS", "Initilization Failed!");
                }
            }
        });
        mainService = initTextToSpeechService();
        if(mainService==null)
        {
            Log.e(TAG,"No Network Connection");
            onDestroy();
        }
        else
        {
            taskQueue = new LinkedList<>();
            streamQueue = new LinkedList<>();
            varSpeak = new Speak();
            taskQueue.add(varSpeak);
            varSpeak.execute("Welcome to Watson Mail. Please Enter your Email and Password to Login");

        }
    }

    private TextToSpeech initTextToSpeechService() {
        final com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech service = new com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech();
        service.setUsernameAndPassword("6b77f8c6-f1c1-45f0-a26c-6ff54616ef25", "gtCyLt7DOOFw");
        return service;
    }

    private void speak(String text){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_ADD, null, null);
        }else{
            tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_ADD, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

    }
    public void layoutClicked(View view)
    {
        Log.e(TAG,"Layout is Clicked");
        while(taskQueue.isEmpty()!=true)
        {
            AsyncTask s = taskQueue.poll();
            s.cancel(true);
            StreamPlayer sm = streamQueue.poll();
            sm.interrupt();;
        }
        listen();
    }

    private void listen(){
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your Choice");

        try {
            startActivityForResult(i, 100);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    private void exitFromApp() throws InterruptedException {
        try {
            Thread.sleep(8000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        tts.stop();
        tts.shutdown();
        this.finishAffinity();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(result.get(0).toLowerCase().equals("close") || result.get(0).toLowerCase().equals("back"))
                {
                    Log.e(TAG,"Either said Close or Back");
                    varSpeak = new Speak();
                    varSpeak.execute("Closing the application!");
                    taskQueue.add(varSpeak);
                    try {
                        exitFromApp();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    Configuration.loop=false;
                    if(statusConfirm==1)
                    {
                        String status = result.get(0);
                        if(status.toLowerCase().contains("confirm"))
                        {

                            emailId = emailId.replaceAll(" ","");
                            password = password.replaceAll(" ","");
                            emailId = emailId.replaceAll("dot",".");
                            Configuration.EMAIL = emailId;
                            Configuration.PASSWORD = password;
                            Log.e("my email id is ",emailId);
                            Log.e("my password is ",password);
                            varSpeak = new Speak();
                            varSpeak.execute("You have confirmed your credentials. Kindly let me log in to your mail");
                            taskQueue.add(varSpeak);
                            Log.e(TAG,"statusConfirm is 1 and I have spoken confirm");
                            Context context = this;
                            CheckMail sm = new CheckMail(context);
                            sm.execute();
                            while(Configuration.loop!=true);
                            if(Configuration.authenticated==true)
                            {
                                Log.e(TAG,"Authentication successful");
                                Configuration.loop = false;
                                varSpeak = new Speak();
                                taskQueue.add(varSpeak);
                                Intent intent = new Intent(MainActivity.this,AfterLoginActivity.class);
                                startActivity(intent);
                                finish();
                            }
                            else
                            {
                                Log.e(TAG,"Authentication unsuccessful");
                                varSpeak = new Speak();
                                varSpeak.execute("The Credentials are Invalid. Kindly enter the credentials again");
                                taskQueue.add(varSpeak);
                                Configuration.authenticated = false;
                                Configuration.loop = false;
                                statusConfirm=0;
                                numberOfClicks = 0;

                            }
                        }
                        else if(status.toLowerCase().contains("cancel"))
                        {
                            Log.e(TAG,"I have spoken cancel");
                            varSpeak = new Speak();
                            varSpeak.execute("Kindly Input the credentials again");
                            taskQueue.add(varSpeak);
                            Configuration.authenticated = false;
                            Configuration.loop = false;
                            numberOfClicks = 0;
                            statusConfirm = 0;
                        }
                        else
                        {
                            Log.e(TAG,"Not spoken cancel and confirm both");
                            varSpeak = new Speak();
                            varSpeak.execute("Please confirm your credentials or Cancel it to enter again");
                            taskQueue.add(varSpeak);
                        }
                    }
                    else
                    {
                        switch(numberOfClicks)
                        {
                            case 0 : //email id not yet entered;
                            {
                                String output;
                                output= result.get(0);
                                output = output.replaceAll(" ","");
                                varSpeak = new Speak();
                                varSpeak.execute("Your emailId is " + output + ". Now please enter the password");
                                taskQueue.add(varSpeak);
                                emailId = output;
                                numberOfClicks++;
                                break;
                            }
                            case 1 :
                            {
                                String output;
                                output= result.get(0);
                                output = output.replaceAll(" ","");
                                varSpeak = new Speak();
                                varSpeak.execute("Entered password is " + output + ". Say confirm to proceed with these credentials or cancel to enter credentials again");
                                taskQueue.add(varSpeak);
                                password = output;
                                numberOfClicks++;
                                statusConfirm = 1;
                                break;
                            }
                        }
                    }
                }
            }
            else
            {
                Log.e(TAG,"Didn't recognize");
                varSpeak = new Speak();
                varSpeak.execute("I didn't get you. Can you please repeat what you just said");
                taskQueue.add(varSpeak);
            }
        }
    }

    class Speak extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            try {
                String str = params[0];
                str = str.replaceAll("<","");
                str = str.replaceAll(">","");
                Log.e(TAG,str);
                streamPlayer = new StreamPlayer();
                streamQueue.add(streamPlayer);
                streamPlayer.playStream(mainService.synthesize(str, Voice.EN_LISA).execute());
            } catch (Exception e) {
                e.printStackTrace();
                Configuration.noNetwork = true;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            taskQueue.poll();
            streamQueue.poll();
        }
    }
}


