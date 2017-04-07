package example.android.watsonmail;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;

public class ReadingMails extends AsyncTask<Void,Void,Void> {


    private Context context;
    private Session session;
    private ProgressDialog progressDialog;
    public ReadingMails(Context context1){
        this.context = context1;
    }
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = ProgressDialog.show(context,"Fetching Mails","Please Wait ...",false,false);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        progressDialog.dismiss();
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
            Configuration.messageCount = inbox.getMessageCount();
            Log.e("Total Messages:- ",Integer.toString(Configuration.messageCount));
            Configuration.message = inbox.getMessages();
            Configuration.loop1 = true;
            inbox.close(true);
            store.close();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
