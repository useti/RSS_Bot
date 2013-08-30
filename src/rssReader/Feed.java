package rssReader;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.w3c.dom.ls.LSOutput;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
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
    private final String configFileName;

    public Feed(
            String configFileName,
            URL link,
            int checkInterval,
            String feedName,
            JabberClient jabber,
            String newsHub,
            Level logLevel, Properties config) throws ParserConfigurationException {
        this.configFileName = configFileName;
        this.link = link;
        this.checkInterval = checkInterval;
        this.feedName = feedName;
        this.jabber = jabber;
        this.newsHub = newsHub;
        this.logLevel = logLevel;
        this.config = config;
        this.isFirstRun = config.getProperty("firstrun").equals("true");
        LOGGER.setLevel(this.logLevel);

        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private final URL link;
    private final int checkInterval;
    private final String feedName;
    private final JabberClient jabber;
    private final String newsHub;
    private final DocumentBuilder builder;
    private final Level logLevel;
    private final Properties config;

    private final static Logger LOGGER = Logger.getLogger(Feed.class.getName());

    public boolean isFirstRun() throws ClassNotFoundException, SAXException, InstantiationException, IllegalAccessException, IOException {
        return isFirstRun;
    }

    public void setFirstRun(boolean firstRun) {
        isFirstRun = firstRun;


        config.setProperty("firstrun", firstRun ? "true" : "false" );

        try {
            config.store(new FileOutputStream(configFileName), null);
        } catch (IOException e) {
            LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
            //e.printStackTrace();
        }
    }

    private boolean isFirstRun;

    public Element getLastNode() throws IOException, SAXException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        if (lastNode == null)
        {
            File fXmlFile = new File(config.getProperty("lastnodepath","lastnode.xml"));
            Document doc = builder.parse(fXmlFile);

            lastNode =  doc.getDocumentElement();

            //writeNode(lastNode, "testOutput.xml");

            return lastNode;
        }
        return lastNode;
    }

    public void setLastNode(Element node) throws InstantiationException, IllegalAccessException, ClassNotFoundException, IOException {
        writeNode(node, config.getProperty("lastnodepath","lastnode.xml"));
        this.lastNode = node;
    }

    private void writeNode(Element node, String filename) throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException {
        // Get a factory (DOMImplementationLS) for creating a Load and Save object.
        org.w3c.dom.ls.DOMImplementationLS impl =
                (org.w3c.dom.ls.DOMImplementationLS)
                        org.w3c.dom.bootstrap.DOMImplementationRegistry.newInstance().getDOMImplementation("LS");

        // Use the factory to create an object (LSSerializer) used to
        // write out or save the document.
        org.w3c.dom.ls.LSSerializer writer = impl.createLSSerializer();
        DOMConfiguration conf = writer.getDomConfig();
        //conf.setParameter("format-pretty-print", Boolean.TRUE);

        // Use the LSSerializer to write out or serialize the document to a String.

        LSOutput lsOutput = impl.createLSOutput();
        lsOutput.setEncoding("UTF-8");
        Writer fileWriter = new FileWriter(filename);
        lsOutput.setCharacterStream(fileWriter);

        writer.write(node,lsOutput);
    }

    private Element lastNode;

    public void activate() {
        new Thread(this, feedName ).start();
        LOGGER.info(String.format("%s - Feed %s activated",feedName,link.toString()));
    }

    @Override
    public void run() {
        while (true)
        {
            try {
                LOGGER.fine(String.format("%s - Start process feed",feedName));
                processFeed();
                LOGGER.fine(String.format("%s - Feed %s processed, sleep to %s seconds",feedName,link.toString(),checkInterval));
                Thread.sleep(checkInterval * 1000);
            }
            catch (InterruptedException e) {
                LOGGER.warning(String.format("%s - %s",feedName,e.toString()));
            } catch (ParserConfigurationException e) {
                LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
            } catch (IOException e) {
                LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
            } catch (SAXException e) {
                LOGGER.warning(e.toString());
            } catch (XMPPException e) {
                LOGGER.warning(String.format("%s - XMPP error %s",feedName,e));
                //e.printStackTrace();
                reconnect();
            } catch (ClassNotFoundException e) {
                LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
            } catch (InstantiationException e) {
                LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
            } catch (IllegalAccessException e) {
                LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
                reconnect();
            }
        }
    }

    private void reconnect() {
        LOGGER.severe(String.format("%s - Reconnecting",feedName));
        jabber.disconnect();
        LOGGER.severe(String.format("%s - Disconnected",feedName));
        try {
            LOGGER.severe(String.format("%s - Connecting",feedName));
            jabber.connect();
            LOGGER.severe(String.format("%s - Connected",feedName));
        } catch (XMPPException e1) {
            LOGGER.warning(String.format("%s - %s", e1.toString()));
        }
    }

    private void processFeed() throws ParserConfigurationException, IOException, SAXException, XMPPException, IllegalAccessException, ClassNotFoundException, InstantiationException {

        HttpURLConnection connection = (HttpURLConnection) link.openConnection();
        connection.addRequestProperty("User-Agent", "Mozilla/4.76");

        Document doc = builder.parse(connection.getInputStream());

        LOGGER.finest(String.format("%s - Parse feed",feedName));

        NodeList nodes = doc.getElementsByTagName("item");
        LOGGER.finest(String.format("%s - Get items",feedName));

        int current = -1;
        if (!isFirstRun()){
            int i;
            Element ln = getLastNode();
            for( i = 0;i<nodes.getLength();i++) {
                Element element = (Element)nodes.item(i);
                if (element.isEqualNode(ln)){
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

        LOGGER.info(String.format("%s - %s new of %s items",feedName,current,nodes.getLength()));

        if (nodes.getLength() > 0)
        {
            setFirstRun(false);

            for(int i=(current-1);i>=0;i--){
                try {
                    Element element = (Element)nodes.item(i);
                    LOGGER.fine(String.format("%s - Post item to %s", feedName, newsHub));
                    printElement(element);
                }catch (Exception e){
                    LOGGER.warning(String.format("%s -%s",feedName,e.toString()));
                }
            }
            setLastNode((Element) nodes.item(0));
        }

    }

    private void printElement(Element element) throws XMPPException, MalformedURLException {

        ConfigureForm form = new ConfigureForm(FormType.submit);
        form.setPersistentItems(false);
        form.setDeliverPayloads(true);
        form.setAccessModel(AccessModel.open);

        LeafNode myNode = jabber.pmanager.getNode(newsHub);

        LOGGER.finest(String.format("%s - Parse item",feedName));
        NewsItem ni = new NewsItem(logLevel, element,newsHub);

        LOGGER.finest(String.format("%s - Get payload",feedName));
        PayloadItem p = ni.genPayload();

        LOGGER.finest(String.format("%s - Post",feedName));
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
            LOGGER.warning(String.format("%s - %s", feedName, ex.toString()));
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
