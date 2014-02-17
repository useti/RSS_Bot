package rssReader;

/**
 * Created with IntelliJ IDEA.
 * User: ytihoglaz
 * Date: 21.08.13
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */

import org.jivesoftware.smack.XMPPException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        //XMPPConnection.DEBUG_ENABLED = true;
        try {
            new Main(args);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final static Logger LOGGER = Logger.getLogger(Main.class.getName());

    private final Properties config;
    private final Level logLevel;


    private Main(String[] argv) throws IOException {

        config = new Properties();
        config.load(new FileInputStream("config.properties"));

        logLevel = Level.parse(config.getProperty("loginfo"));

        LOGGER.setLevel(logLevel);


        try {
            JabberClient jabber = JabberClient.newBuilder(config.getProperty("srv"))
                .setPassword(config.getProperty("pwd"))
                .setPort(5222)
                .setUser(config.getProperty("jid"))
                .setResource(config.getProperty("nodename"))
                //.setService(params.get("jservice").trim())
                .build();
            jabber.setSASLAuthenticationEnabled(true);
            jabber.setSASLPlain();
            jabber.setSelfSignedCertificateEnabled(true);

            try {
                jabber.connect();
                while (!jabber.conn.isConnected())
                {
                    LOGGER.fine("Wait connection ...");
                    Thread.sleep(1000);
                }
            } catch (XMPPException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            URL feed_url = new URL(config.getProperty("url"));
            int update_interval = Integer.parseInt(config.getProperty("interval"));
            Feed feed = new Feed(
                    "config.properties", feed_url,
                    update_interval,
                    config.getProperty("nodename"),
                    jabber,
                    config.getProperty("nodename"),
                    logLevel,
                    config);
            feed.activate();
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
}
