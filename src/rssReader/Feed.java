package rssReader;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: ytihoglaz
 * Date: 21.08.13
 * Time: 15:55
 * To change this template use File | Settings | File Templates.
 */
public class Feed implements Runnable{
    public Feed(URL link, int checkInterval, String feedName, JabberClient jabber, String newsHub, Level loglevel) throws ParserConfigurationException {
        this.link = link;
        this.checkInterval = checkInterval;
        this.feedName = feedName;
        this.jabber = jabber;
        this.newsHub = newsHub;
        this.loglevel = loglevel;
        this.isFirstRun = true;
        LOGGER.setLevel(this.loglevel);

        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private final URL link;
    private final int checkInterval;
    private final String feedName;
    private final JabberClient jabber;
    private final String newsHub;
    private final DocumentBuilder builder;
    private final Level loglevel;

    private final static Logger LOGGER = Logger.getLogger(Feed.class.getName());

    private boolean isFirstRun;
    private Element lastNode;

    public void activate() {
        new Thread(this, feedName ).start();
        LOGGER.info(String.format("Feed %s activated",link.toString()));
    }

    @Override
    public void run() {
        while (true)
        {
            try {
                LOGGER.info("Start process feed");
                processFeed();
                LOGGER.info(String.format("Feed %s processed, sleep to %s seconds",link.toString(),checkInterval));
                Thread.sleep(checkInterval * 1000);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } catch (XMPPException e) {
                LOGGER.warning(String.format("XMPP error %s",e));
                //e.printStackTrace();
                LOGGER.info("Reconnecting");
                jabber.disconnect();
                LOGGER.info("Disconnected");
                try {
                    LOGGER.info("Connecting");
                    jabber.connect();
                    LOGGER.info("Connected");
                } catch (XMPPException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void processFeed() throws ParserConfigurationException, IOException, SAXException, XMPPException {

        Document doc = builder.parse(link.openStream());
        LOGGER.info("Parse feed");

        NodeList nodes = doc.getElementsByTagName("item");
        LOGGER.info("Get items");

        int current = -1;
        if (!isFirstRun){
            int i;
            for( i = 0;i<nodes.getLength();i++) {
                Element element = (Element)nodes.item(i);
                if (element.isEqualNode(lastNode)){
                    current = i;
                    break;
                }
            }
            if (current < 0)
                current = nodes.getLength();
        } else {
            if (current < 0)
                current = nodes.getLength();
        }

        LOGGER.info(String.format("%s new of %s items",current,nodes.getLength()));

        isFirstRun = false;

        if (nodes.getLength() > 0)
        {
            lastNode = (Element)nodes.item(0);

            for(int i=0;i<current;i++){
                Element element = (Element)nodes.item(i);
                LOGGER.info(String.format("Post item to %s",newsHub));
                printElement(element);
            }
        }

    }

    private void printElement(Element element) throws XMPPException, MalformedURLException {

        ConfigureForm form = new ConfigureForm(FormType.submit);
        form.setPersistentItems(false);
        form.setDeliverPayloads(true);
        form.setAccessModel(AccessModel.open);

        LeafNode myNode = jabber.pmanager.getNode(newsHub);

        LOGGER.info("Parse item");
        NewsItem ni = new NewsItem(element,newsHub);

        LOGGER.info("Get payload");
        PayloadItem p = ni.genPayload();

        LOGGER.info("Post");
        myNode.send(p);
    }

    private String getCharacterDataFromElement(Element e) {
        try {
            Node child = e.getFirstChild();
            if(child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
        }
        catch(Exception ex) {

        }
        return "";
    } //private String getCharacterDataFromElement

    protected float getFloat(String value) {
        if(value != null && !value.equals("")) {
            return Float.parseFloat(value);
        }
        return 0;
    }

    protected String getElementValue(Element parent,String label) {
        return getCharacterDataFromElement((Element)parent.getElementsByTagName(label).item(0));
    }
}
