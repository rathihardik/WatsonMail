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
import android.widget.TextView;
import android.widget.Toast;
import com.ibm.watson.developer_cloud.android.library.audio.StreamPlayer;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;


public class SendingActivity extends AppCompatActivity {

    private TextToSpeech tts;
    Queue<AsyncTask> taskQueue ;
    Speak varSpeak;
    String TAG="SendingActivity";
    StreamPlayer streamPlayer;
    Queue<StreamPlayer> streamQueue;
    public static com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech mainService;
    private TextView To,Subject,Message;
    public String stringTo,stringSubject,stringMessage;
    public int numberOfClicks = 0;
    public boolean isForward=false,isReply = false, isNormal = true;
    public boolean isBackPressed = false, afterBackPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sendingactivity);
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
            varSpeak.execute("Please Wait");
            taskQueue.add(varSpeak);
        }
        numberOfClicks = 1;
        Bundle recieved = getIntent().getExtras();
        if(recieved!=null)
        {
            String checkforward = recieved.getString("forward","itIsNotforward");
            String checkReply = recieved.getString("reply","itIsNotReply");
            if(!checkforward.equals("itIsNotforward"))
            {
                String content = recieved.getString("message");
                setMessage(content);
                String subject = recieved.getString("subject");
                setSubject("Fwd : " + subject);
                isForward = true;
                isNormal = false;
            }
            else if(!checkReply.equals("itIsNotReply"))
            {
                String recipient = recieved.getString("to");
                setTo(recipient);
                String Subject = recieved.getString("subject");
                setSubject("Re : " + Subject);
                numberOfClicks = 3;
                varSpeak = new Speak();
                varSpeak.execute("Now please enter the message you want to send");
                taskQueue.add(varSpeak);
                isReply = true;
                isNormal = false;
            }
            else {
                setTo("");
                setSubject("");
                setMessage("");
            }
        }
        else
        {
            setTo("");
            setSubject("");
            setMessage("");
        }

        To = (TextView) findViewById(R.id.to);
        To.setText(stringTo);
        Subject  =(TextView)findViewById(R.id.subject);
        Subject.setText(stringSubject);
        Message = (TextView) findViewById(R.id.message);
        Message.setText(stringMessage);
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
            Toast.makeText(SendingActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    private void exitFromApp() throws InterruptedException {
        Thread.sleep(5000);
        tts.stop();
        tts.shutdown();
        this.finishAffinity();
    }


    public void setSubject(String str)
    {
        this.stringSubject = str;
    }

    public void setTo(String str)
    {
        this.stringTo = str;
    }

    public void setMessage(String str)
    {
        this.stringMessage = str;
    }

    private void sendEmail() {
        Log.e(TAG,"Inside sendEmail()");
        String email = To.getText().toString().trim();
        String subject = Subject.getText().toString().trim();
        String message = Message.getText().toString().trim();
        SendMail sendmail = new SendMail(this, email, subject, message);
        sendmail.execute();
    }
    

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(isBackPressed == true)
                {
                    if(afterBackPressed == false)
                    {
                        Log.e(TAG,"isBackPressed = true and afterBackPressed = false");
                        varSpeak = new Speak();
                        varSpeak.execute("Your mail will be discarded. Say confirm to proceed or Cancel to prevent discarding");
                        taskQueue.add(varSpeak);
                        afterBackPressed = true;
                    }
                    else
                    {
                        Log.e(TAG,"isBackPressed = true and afterBackPressed = true");
                        if(result.get(0).toLowerCase().contains("confirm"))
                        {
                            Log.e(TAG,"Spoken Confirm");
                            varSpeak = new Speak();
                            varSpeak.execute("Your mail is discarded. Going back to Main Page");
                            taskQueue.add(varSpeak);
                            Intent i = new Intent(SendingActivity.this,AfterLoginActivity.class);
                            startActivity(i);
                            finish();
                        }
                        else if(result.get(0).toLowerCase().contains("cancel"))
                        {
                            Log.e(TAG,"Spoken Cancel");
                            isBackPressed = false;
                            afterBackPressed = false;
                            varSpeak = new Speak();
                            varSpeak.execute("Your mail is not discarded. Please Continue");
                            taskQueue.add(varSpeak);
                        }
                        else
                        {
                            Log.e(TAG,"Invalid command");
                            varSpeak = new Speak();
                            varSpeak.execute("Please either confirm or cancel");
                            taskQueue.add(varSpeak);
                        }
                    }

                }
                else if(result.get(0).toLowerCase().equals("close") || result.get(0).toLowerCase().equals("log out"))
                {
                    isBackPressed = false;
                    afterBackPressed = false;
                    Log.e(TAG,"My choice is to close");
                    varSpeak = new Speak();
                    varSpeak.execute("Logging out and closing the application!");
                    taskQueue.add(varSpeak);
                    try {
                        exitFromApp();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                else if(result.get(0).toLowerCase().equals("back") || result.get(0).toLowerCase().equals("cancel"))
                {
                    Log.e(TAG,"My choice is to go back");
                    varSpeak = new Speak();
                    varSpeak.execute("Cancelling composing the mail");
                    taskQueue.add(varSpeak);
                    isBackPressed = true;
                }
                else {
                    isBackPressed = false;
                    afterBackPressed = false;
                    switch (numberOfClicks) {
                        case 1:
                            String stringTos;
                            stringTos = result.get(0).replaceAll("underscore", "_");
                            stringTos = stringTos.toLowerCase();
                            stringTos = stringTos.replaceAll(" ", "");
                            To.setText(stringTos);
                            setTo(stringTos);
                            if(isForward==false)
                            {
                                varSpeak = new Speak();
                                varSpeak.execute("Now please Enter the subject");
                                taskQueue.add(varSpeak);
                                numberOfClicks++;
                            }
                            else
                            {
                                numberOfClicks =  3;
                            }
                            break;
                        case 2:
                            if(isForward==false)
                            {
                                Subject.setText(result.get(0));
                                setSubject(result.get(0));
                                varSpeak = new Speak();
                                varSpeak.execute("Now please enter the message you want to send");
                                taskQueue.add(varSpeak);
                            }
                            numberOfClicks++;
                            break;
                        case 3:
                            if(isForward==false)
                            {
                                Message.setText(result.get(0));
                                setMessage(result.get(0));
                            }
                            varSpeak = new Speak();
                            varSpeak.execute("Please Confirm the mail. To : " + stringTo + ". Subject : " + stringSubject + ". Message : " + stringMessage + ". Speak Yes to confirm");
                            taskQueue.add(varSpeak);
                            numberOfClicks++;
                            break;
                        case 4: {
                            if (result.get(0).toLowerCase().equals("yes")) {
                                Log.e(TAG,"Spoken Yes to send the mail ");
                                varSpeak = new Speak();
                                varSpeak.execute("Sending the mail");
                                taskQueue.add(varSpeak);
                                sendEmail();
                                try {
                                    Thread.sleep(10000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if (Configuration.messageSent == 1) {
                                    varSpeak = new Speak();
                                    varSpeak.execute("Your message has been sent. Redirecting you to main page");
                                    taskQueue.add(varSpeak);
                                    Intent i = new Intent(SendingActivity.this, AfterLoginActivity.class);
                                    startActivity(i);
                                } else {
                                    varSpeak = new Speak();
                                    varSpeak.execute("I am facing a problem in sending the message. Check your network connection and the entered credentials and try again");
                                    taskQueue.add(varSpeak);
                                }
                            }
                            else if (result.get(0).toLowerCase().equals("no")) {
                                Log.e(TAG,"My choice is no");
                                varSpeak = new Speak();
                                varSpeak.execute("You are ready to compose again");
                                taskQueue.add(varSpeak);
                                To.setText("");
                                Subject.setText("");
                                Message.setText("");
                                numberOfClicks = 0;
                            } else {
                                Log.e(TAG,"Not confirmed yet");
                                varSpeak = new Speak();
                                varSpeak.execute("You have not confirmed yet. Say yes to confirm or no to compose again");
                                taskQueue.add(varSpeak);
                            }
                            break;
                        }
                        default:numberOfClicks=3;
                            break;
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
