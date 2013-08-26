package rssReader;

/**
 * Created with IntelliJ IDEA.
 * User: ytihoglaz
 * Date: 21.08.13
 * Time: 15:14
 * To change this template use File | Settings | File Templates.
 */

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import javax.xml.parsers.ParserConfigurationException;
import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    public static void main(String[] args) {
        XMPPConnection.DEBUG_ENABLED = true;
        new Main(args);
    }

    private Main(String[] argv) {
        URL u = null; // your feed url
        try {

            JabberClient jabber = JabberClient.newBuilder("xmpp.useti.ru")
                .setPassword("XXXXXXXX")
                .setPort(5222)
                .setUser("admin@useti.ru")
                //.setService(params.get("jservice").trim())
                .build();
            jabber.setSASLAuthenticationEnabled(true);
            jabber.setSASLPlain();
            jabber.setSelfSignedCertificateEnabled(true);

            try {
                jabber.connect();
            } catch (XMPPException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

            u = new URL("http://pipes.yahoo.com/pipes/pipe.run?_id=c9c967912bb49d8e0fe7c1967dd8b43a&_render=rss");
            Feed feed = new Feed(u,60,"MorningFun", jabber, "rss", java.util.logging.Level.FINE);
            feed.activate();
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
