package example.android.watsonmail;

import javax.mail.Message;

public class Configuration {

    public static String EMAIL ="helloworld071996@gmail.com";  //gmail address
    public static String PASSWORD ="rathi071996"; //password
    public static boolean authenticated = false;
    public static boolean loop = false;
    public static boolean loop1 = false;
    public static Message[] message;
    public static int messageCount=0;
    public static int currentMessageCount = 0;
    public static boolean startPage= true;
    public static boolean finalPage = false;
    public static int currentPageMailCount = 0;
    public static int currentPageNo = 1;
    public static int messageSent = 0;
    public static int startPagemailNo = 0;
    public static int endPagemailNo = 0;
    public static boolean noNetwork = false;
}