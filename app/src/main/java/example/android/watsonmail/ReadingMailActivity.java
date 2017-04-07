package example.android.watsonmail;

import android.app.ProgressDialog;
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

import org.jsoup.Jsoup;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Queue;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMultipart;

public class ReadingMailActivity extends AppCompatActivity {

    private TextToSpeech tts;
    public static int no = 0;
    Queue<AsyncTask> taskQueue;
    Queue<StreamPlayer> streamQueue;
    Speak varSpeak;
    String TAG = "ReadingMailActivity";
    StreamPlayer streamPlayer;
    private ProgressDialog progressDialog;
    public static com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech mainService;
    public static Message[] messagefinal;
    public static boolean readingIndividualMails = false;
    public static boolean isAttachment = false;
    public static boolean wantToAnalayzeAttachment = false;
    String mailRecipient="",mailMessage = "",mailSubject="";
    static List<File> attachments;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.readingmailactivity);
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
        ReadingMails rm = new ReadingMails(this);
        rm.execute();
        mainService = initTextToSpeechService();
        if(mainService!=null)
        {
            taskQueue = new LinkedList<>();
            streamQueue = new LinkedList<>();
            varSpeak = new Speak();
            progressDialog = ProgressDialog.show(this,"Reading Mails","Please Wait ...",false,false);
            Log.e(TAG,"Please Wait");
            varSpeak.execute("Please Wait");
            taskQueue.add(varSpeak);
        }
        while(Configuration.loop1!=true);
        attachments= new ArrayList<File>();
        progressDialog.dismiss();
        readNext();
        new Fetching().execute();

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
            Toast.makeText(ReadingMailActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }


    private void exitFromApp() throws InterruptedException {
        Thread.sleep(5000);
        tts.stop();
        tts.shutdown();
        this.finishAffinity();
    }


    public void readMailIndividual(int number)
    {
        if(number>Configuration.currentPageMailCount)
        {
            Log.e(TAG,"Choice exceeds total number of mails");
            varSpeak = new Speak();
            varSpeak.execute("Your choice exceeds the total number of mails in this page. Please Try Again ");
            taskQueue.add(varSpeak);
            return;
        }
        else
        {
            Log.e(TAG,"Reading selected Mail");
            varSpeak = new Speak();
            varSpeak.execute("Reading mail number " + Integer.toString(number) + " on this page, You can reply to this mail or forward this mail");
            taskQueue.add(varSpeak);
            new readingIndivMails().execute();
        }
    }

    public static String getTextFromMessage(Message message) throws Exception {

        attachments.clear();
        if (message.isMimeType("text/plain")){
            return message.getContent().toString();
        }
        else if (message.isMimeType("multipart/*"))
        {
            String result = "";
            MimeMultipart mimeMultipart = (MimeMultipart)message.getContent();
            int count = mimeMultipart.getCount();
            for (int i = 0; i < count; i ++){
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType("text/plain"))
                {
                    result = result + bodyPart.getContent();
                    break;
                } else if (bodyPart.isMimeType("text/html") || bodyPart.isMimeType("text/javascript"))
                {
                    String html = (String) bodyPart.getContent();
                    result = result + Jsoup.parseBodyFragment(html).text();
                }
            }

            /*Check if there is attachment and corresspondingly make isAttachment = true / false */
            return result;
        }
        return "";
    }


    public void readCompleteMail(int number)
    {
        int index = (Configuration.currentPageNo-1)*5 + number-1;
        try {
            mailRecipient = (messagefinal[index].getFrom()[0]).toString();
            Log.e("From ",mailRecipient);
            varSpeak = new Speak();
            varSpeak.execute("Email is recieved from " +mailRecipient);
            taskQueue.add(varSpeak);

            mailSubject = messagefinal[index].getSubject();
            Log.e("Subject",mailSubject);
            varSpeak = new Speak();
            varSpeak.execute("Subject is " + mailSubject);
            taskQueue.add(varSpeak);


            mailMessage = getTextFromMessage(messagefinal[index]);
            Log.e("Content ",mailMessage);
            varSpeak = new Speak();
            varSpeak.execute("Body of Email is " + mailMessage);
            taskQueue.add(varSpeak);
            readingIndividualMails = true;

            if(isAttachment==true)
            {
                varSpeak = new Speak();
                varSpeak.execute("The Mail also consists of an attachment. Do you want to download it. Say Yes or no");
                taskQueue.add(varSpeak);
            }
            else
            {
                varSpeak = new Speak();
                varSpeak.execute("The complete mail has been read. Kindly Select what you want to do");
                taskQueue.add(varSpeak);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readNext()
    {
        varSpeak = new Speak();
        varSpeak.execute("Select the Mail you want to read out of the 5 mails on this page. You can say Next page and Previous page to change the page");
        taskQueue.add(varSpeak);
        int index = Configuration.currentMessageCount;
        int i;
        Configuration.startPagemailNo = index;
        for(i = index;i<index+5 && i<Configuration.messageCount;i++)
        {
            Log.e(TAG,"The value of the I is " + Integer.toString(i));
            Configuration.currentPageMailCount = i%5+1;
        }
        Configuration.endPagemailNo = i-1;
        if(i==Configuration.messageCount)
        {
            Configuration.finalPage = true;
        }
        if(i>5)
        {
            Configuration.startPage = false;
        }
        Configuration.currentMessageCount = i;
    }

    public void readPrevious()
    {
        varSpeak = new Speak();
        varSpeak.execute("Select the Mail you want to read out of the 5 mails on this page. You can say Next page and Previous page to change the page");
        taskQueue.add(varSpeak);
        int index = Configuration.currentMessageCount-1;
        index = index - Configuration.currentPageMailCount;
        int i;
        for(i = index-4;i<=index;i++)
        {
            Log.e(TAG,"The value of the I is " + Integer.toString(i));
        }
        Configuration.currentPageMailCount = 5;
        if(i==5)
        {
            Configuration.startPage = true;
        }
        if(i<=Configuration.messageCount-5)
        {
            Configuration.finalPage = false;
        }
        Configuration.currentMessageCount = index-4;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            if (resultCode == RESULT_OK && null != data) {
                int flag = 0;
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if(result.get(0).toLowerCase().equals("close") || result.get(0).toLowerCase().equals("log out"))
                {
                    Log.e(TAG,"I have chosen to close");
                    varSpeak = new Speak();
                    varSpeak.execute("Logging Out and Closing the application!");
                    taskQueue.add(varSpeak);
                    try {
                        exitFromApp();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    flag=1;
                }
                else if(result.get(0).toLowerCase().equals("back"))
                {
                    Log.e(TAG,"I have spoken to back");
                    Intent i = new Intent(ReadingMailActivity.this,AfterLoginActivity.class);
                    startActivity(i);
                    finish();
                }
                else if(isAttachment==true && readingIndividualMails==true)
                {
                    if(wantToAnalayzeAttachment!=true)
                    {
                        Log.e(TAG,"isAttachment = true and wantToAnalyze==false");
                        String status = result.get(0).toLowerCase();
                        if(status.contains("yes"))
                        {
                            //Code to download the attachment and analyzing part
                            wantToAnalayzeAttachment = true;
                        }
                        else if(status.contains("no"))
                        {
                            varSpeak = new Speak();
                            varSpeak.execute("The attachment will not be downloaded. Please carry on");
                            taskQueue.add(varSpeak);
                            wantToAnalayzeAttachment = false;
                        }
                        else
                        {
                            varSpeak = new Speak();
                            varSpeak.execute("Your Answer should be simple Yes or No. Please try again");
                            taskQueue.add(varSpeak);
                            wantToAnalayzeAttachment = false;
                        }
                    }
                    if(wantToAnalayzeAttachment==true)
                    {
                        //Code to download the attachment.
                        //if the file is .jpg or .png then use vision recognition and speak
                        //else if the file is .html, .pdf, .docx then use document converterand read
                        //else say Unsupported format. sorry
                    }


                }
                else
                {
                    isAttachment = false;
                    wantToAnalayzeAttachment = false;
                    if(readingIndividualMails==true)
                    {
                        String status = result.get(0);
                        if(status.toLowerCase().equals("forward"))
                        {
                            Log.e(TAG,"chosen to forward the mail");
                            Intent intent = new Intent(ReadingMailActivity.this,SendingActivity.class);
                            intent.putExtra("message",mailMessage);
                            intent.putExtra("subject",mailSubject);
                            intent.putExtra("forward","helloWorld");
                            startActivity(intent);
                            flag=1;

                        }
                        else if(status.toLowerCase().equals("reply"))
                        {
                            Log.e(TAG,"chosen to reply the mail");
                            Intent intent = new Intent(ReadingMailActivity.this,SendingActivity.class);
                            intent.putExtra("to",mailRecipient);
                            intent.putExtra("subject",mailSubject);
                            intent.putExtra("reply","helloWorld");
                            startActivity(intent);
                            flag=1;
                        }
                        else if(status.toLowerCase().equals("next"))
                        {
                            flag=1;
                            Log.e(TAG,"chosen to see the next mail");
                            if(this.no>Configuration.endPagemailNo)
                            {
                                varSpeak = new Speak();
                                varSpeak.execute("You are seeing the last mail on this page. Say next page to go on the next page or Please select other option");
                                taskQueue.add(varSpeak);
                            }
                            else
                            {
                                this.no++;
                                readingIndividualMails = true;
                                readMailIndividual(this.no);
                            }
                        }
                        else if(status.toLowerCase().equals("previous"))
                        {
                            Log.e(TAG,"chosen to see the previous mail");
                            flag=1;
                            if(this.no<Configuration.startPagemailNo)
                            {
                                varSpeak = new Speak();
                                varSpeak.execute("You are seeing the first mail on this page. Say previous page to go on the previous page or Please select other option");
                                taskQueue.add(varSpeak);
                            }
                            else
                            {
                                this.no--;
                                readingIndividualMails = true;
                                readMailIndividual(this.no);
                            }
                        }

                    }
                    String status = result.get(0);
                    if(status.toLowerCase().contains("next page"))
                    {
                        Log.e(TAG,"chosen to see the next page");
                        readingIndividualMails = false;
                        flag=1;
                        if(Configuration.finalPage==true)
                        {
                            varSpeak = new Speak();
                            varSpeak.execute("You are on the last page. select any one of the mail or go to previous page ");
                            taskQueue.add(varSpeak);
                        }
                        else
                        {
                            Configuration.currentPageNo++;
                            readNext();
                        }

                    }
                    else if(status.toLowerCase().contains("previous page "))
                    {
                        readingIndividualMails = false;
                        flag=1;
                        Log.e(TAG,"chosen to see the previous page");
                        if(Configuration.startPage==true)
                        {
                            varSpeak = new Speak();
                            varSpeak.execute("You are on the first page. select any one of the mail or go to next page ");
                            taskQueue.add(varSpeak);
                        }
                        else
                        {
                            Configuration.currentPageNo--;
                            readPrevious();
                        }
                    }
                    else if(status.toLowerCase().contains("first") || status.toLowerCase().contains("1"))
                    {
                        flag=1;
                        this.no = 1;
                        readMailIndividual(1);
                    }
                    else if(status.toLowerCase().contains("second") || status.toLowerCase().contains("2") || status.toLowerCase().equals("to") )
                    {
                        flag=1;
                        this.no = 2;
                        readMailIndividual(2);
                    }
                    else if(status.toLowerCase().contains("third") || status.toLowerCase().contains("3"))
                    {
                        flag=1;
                        this.no = 3;
                        readMailIndividual(3);
                    }
                    else if(status.toLowerCase().contains("fourth") || status.toLowerCase().contains("4"))
                    {
                        flag=1;
                        this.no = 4;
                        readMailIndividual(4);
                    }
                    else if(status.toLowerCase().contains("fifth") || status.toLowerCase().contains("5"))
                    {
                        flag=1;
                        this.no = 5;
                        readMailIndividual(5);
                    }
                }
                if(flag==0)
                {
                    Log.e(TAG,"input is invalid");
                    varSpeak = new Speak();
                    varSpeak.execute("Invalid input. Please Try Again");
                    taskQueue.add(varSpeak);
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


    public class readingIndivMails extends AsyncTask<Void,Void,Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
        @Override
        protected Void doInBackground(Void... params) {
            readCompleteMail(no);
            return null;
        }
    }


    public class Fetching extends AsyncTask<Void,Void,Void> {

        private Session session;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }

        @Override
        protected Void doInBackground(Void... params) {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", "465");
            session = Session.getDefaultInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(Configuration.EMAIL, Configuration.PASSWORD);
                        }
                    });
            try {
                Store store = session.getStore("imaps");
                store.connect("smtp.gmail.com", Configuration.EMAIL, Configuration.PASSWORD);
                Folder inbox = store.getFolder("inbox");
                inbox.open(Folder.READ_ONLY);
                messagefinal = inbox.getMessages();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
            return null;
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


