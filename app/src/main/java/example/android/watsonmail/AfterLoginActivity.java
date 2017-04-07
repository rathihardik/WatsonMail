package example.android.watsonmail;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;


public class AfterLoginActivity extends AppCompatActivity {

    private TextToSpeech tts;
    StreamPlayer streamPlayer;
    Queue<StreamPlayer> streamQueue;
    public static com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech mainService;
    private boolean statusConfirm = false;
    private int statusConfirmValue=-1;
    Queue<AsyncTask> taskQueue ;
    Speak varSpeak;
    String TAG = "AfterLoginActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_login_activity);
        tts = new android.speech.tts.TextToSpeech(this, new android.speech.tts.TextToSpeech.OnInitListener() {
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
        if(mainService!=null)
        {
            taskQueue = new LinkedList<>();
            streamQueue = new LinkedList<>();
            varSpeak = new Speak();
            varSpeak.execute("You are successfully logged in your account. Kindly tell me what you want to do ? Say Compose to compose a mail or say read to read mails from inbox");
            taskQueue.add(varSpeak);
        }
    }

    private com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech initTextToSpeechService() {
        final com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech service = new com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech();
        service.setUsernameAndPassword("6b77f8c6-f1c1-45f0-a26c-6ff54616ef25", "gtCyLt7DOOFw");
        return service;
    }

    private void speak(String text){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        }else{
            tts.speak(text, TextToSpeech.QUEUE_ADD, null);
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
            Toast.makeText(AfterLoginActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    private void exitFromApp() throws InterruptedException {
        Thread.sleep(5000);
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
                if(result.get(0).toLowerCase().equals("close"))
                {
                    Log.e(TAG,"I just said Close");
                    varSpeak = new Speak();
                    varSpeak.execute("Closing the application!");
                    taskQueue.add(varSpeak);
                    try {
                        exitFromApp();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else if(result.get(0).toLowerCase().equals("back") || result.get(0).toLowerCase().equals("log out") )
                {
                    varSpeak = new Speak();
                    varSpeak.execute("Logging out successfully.");
                    taskQueue.add(varSpeak);
                    Log.e(TAG,"I just said back or logout");
                    Intent i = new Intent(AfterLoginActivity.this,MainActivity.class);
                    startActivity(i);
                    finish();
                }
                else {
                    if(statusConfirm==true)
                    {
                        String status = result.get(0);
                        boolean flag1 = status.toLowerCase().contains("confirm") || status.toLowerCase().contains("proceed");
                        if(flag1==true)
                        {
                            if(statusConfirmValue==0)
                            {
                                Log.e(TAG,"Confirmed to Composing mail");
                                varSpeak = new Speak();
                                varSpeak.execute("You have confirmed to compose the mail. Redirecting to composing mail");
                                taskQueue.add(varSpeak);
                                Intent hello = new Intent(AfterLoginActivity.this,SendingActivity.class);
                                startActivity(hello);
                            }
                            else if(statusConfirmValue==1)
                            {
                                Log.e(TAG,"Confirmed to Reading mail");
                                varSpeak = new Speak();
                                varSpeak.execute("You have confirmed to Read your mails. Redirecting to Read your mails");
                                taskQueue.add(varSpeak);
                                Intent intent = new Intent(AfterLoginActivity.this,ReadingMailActivity.class);
                                startActivity(intent);
                            }
                        }
                        else
                        {
                            Log.e(TAG,"Not Confirmed yet");
                            varSpeak = new Speak();
                            varSpeak.execute("You have not confirmed yet. Kindly select your choice again");
                            taskQueue.add(varSpeak);
                            statusConfirm=false;
                            statusConfirmValue=-1;
                        }
                    }
                    else
                    {
                        String output;
                        output= result.get(0);
                        if(output.toLowerCase().contains("compose"))
                        {
                            Log.e(TAG,"Choice is to compose a mail");
                            varSpeak = new Speak();
                            varSpeak.execute("You have chosen to compose a mail. Say confirm to proceed or say cancel to choose again");
                            taskQueue.add(varSpeak);
                            statusConfirm = true;
                            statusConfirmValue = 0;
                        }
                        else if(output.toLowerCase().contains("read"))
                        {
                            Log.e(TAG,"Choice is to read a mail");
                            varSpeak = new Speak();
                            varSpeak.execute("You have chosen to read your mails. Say confirm to proceed or say cancel to choose again");
                            taskQueue.add(varSpeak);
                            statusConfirm = true;
                            statusConfirmValue = 1;
                        }
                        else
                        {
                            Log.e(TAG,"It is not valid");
                            varSpeak = new Speak();
                            varSpeak.execute("You can only Compose or Read a Mail. Kindly Select one of these options");
                            taskQueue.add(varSpeak);
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
                streamPlayer.playStream(mainService.synthesize(str, Voice.EN_MICHAEL).execute());
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
