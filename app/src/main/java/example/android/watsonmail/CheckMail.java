package example.android.watsonmail;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

public class CheckMail extends AsyncTask<Void,Void,Void> {

    private Context context;
    private Session session;
    private TextToSpeech t1;
    private ProgressDialog progressDialog;

    public CheckMail(Context context){
        this.context = context;

    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(context,"Logging In","Please wait...",false,false);

    }
    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss();
        Configuration.loop = true;
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
                    //Authenticating the password
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(Configuration.EMAIL, Configuration.PASSWORD);
                    }
                });
        try {
            Configuration.loop = false;
            Store store = session.getStore("imaps");
            Log.e("my Configuration email",Configuration.EMAIL + " " + Configuration.PASSWORD);
            store.connect("smtp.gmail.com", Configuration.EMAIL, Configuration.PASSWORD);
            Configuration.authenticated = true;
            store.close();
        } catch (MessagingException e) {
            Configuration.authenticated = false;
            e.printStackTrace();
        }
        Configuration.loop=true;
        return null;
    }
}