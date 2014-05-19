package rssReader;

import org.horrabin.horrorss.RssChannelBean;
import org.horrabin.horrorss.RssFeed;
import org.horrabin.horrorss.RssItemBean;
import org.horrabin.horrorss.RssParser;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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

    private final RssParser rss;

    public Feed(
            String configFileName,
            URL link,
            int checkInterval,
            String feedName,
            JabberClient jabber,
            String newsHub,
            Level logLevel, Properties config) throws ParserConfigurationException, NoSuchAlgorithmException, CloneNotSupportedException {
        this.configFileName = configFileName;
        this.link = link;
        this.checkInterval = checkInterval;
        this.feedName = feedName;
        this.jabber = jabber;
        this.newsHub = newsHub;
        this.logLevel = logLevel;
        this.config = config;
        this.isFirstRun = config.getProperty("firstrun").equals("true");
        this.rss = new RssParser();

        LOGGER.setLevel(this.logLevel);

        itemMap = new HashMap<String, Integer>(200);

        md = MessageDigest.getInstance("MD5");

        builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    }

    private Map<String, Integer> itemMap;
    private final URL link;
    private final int checkInterval;
    private final String feedName;
    private final JabberClient jabber;
    private final String newsHub;
    private final DocumentBuilder builder;
    private final Level logLevel;
    private final Properties config;
    private MessageDigest md;

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

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String getHash(String nods) throws CloneNotSupportedException {
        md.update(nods.getBytes());
//        MessageDigest tc1 = (MessageDigest) md.clone();
        byte[] digest = md.digest();
        String ret = bytesToHex(digest);
        md.reset();
        return ret;
    }

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
            } catch (Exception e) {
                LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
//                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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


    private int prevItemSize = 0;
    private int prevFailCount = 0;

    private void processFeed() throws Exception {

        String url = link.toString();

        List<RssItemBean> items  = new ArrayList<RssItemBean>(200);
        int numpages = Integer.parseInt(config.getProperty("pages"));
        int prewsize = 0;
        for(int i = 1; i <= numpages; i++)
        {
            List<RssItemBean> titm = loadItems(url + String.format("&page=%s", i));
            if (i>1)
            {
                if(titm.size() <= prewsize)
                {
                    items.addAll(titm);
                }
            }
            else
                items.addAll(titm);
            prewsize = titm.size();
        }

        if(items.size() > 0) // Костыль первый - если нет записей, то ничего и не делаем
        {
            if (items.size() >= prevItemSize) // Костыль второй - число записей не должно убывать
            {
                if(isFirstRun())
                {
                    int i;
                    setFirstRun(false);
                    itemMap.clear();
                    storeHash();

                    for (i =(items.size() -1 ); i >= 0 ; i--)
                    {
                        RssItemBean item = items.get(i);
                        String itm = item.getTitle();
                        itm = getHash(itm);
                        itemMap.put(itm,i);
                        printElement(item);
                    }
                    storeHash();
                }
                else
                {
                    setFirstRun(false);
                    Map<String, Integer> currentItemMap = new HashMap<String, Integer>(200);
                    int i;
                    for (i = (items.size() -1 ); i >=0 ; i--)
                    {
                        RssItemBean item = items.get(i);
                        String itm = item.getTitle();
                        itm = getHash(itm);
                        currentItemMap.put(itm,i);
                    }

                    LoadHashes();
                    i = 0;
                    Iterator it = currentItemMap.entrySet().iterator();
                    while (it.hasNext()){
                        Map.Entry pairs = (Map.Entry)it.next();

                        if(!itemMap.containsKey(pairs.getKey())){
                            itemMap.put((String)pairs.getKey(),(Integer)pairs.getValue());
                            RssItemBean item = items.get((Integer)pairs.getValue());
                            printElement(item);
                            i++;
                        }
                    }

                    if (i > 0){
                        LOGGER.info(String.format("%s - %s new of %s items",feedName, i ,items.size()));
                    }else{
                        LOGGER.fine(String.format("%s - %s new of %s items",feedName, i ,items.size()));
                    }

                    removeOldHash(currentItemMap);
                }
                prevItemSize = items.size();
            }
            else // Но не более числа раз, указанного в конфиге, иначе обнуляем
            {
                if(prevFailCount >= Integer.parseInt(config.getProperty("prevfailcount","4")))
                {
                    prevFailCount = 0;
                    prevItemSize = 0;
                }
                else
                    prevFailCount++;
            }
        }

    }

    private void removeOldHash(Map<String, Integer> currentItemMap) throws IOException {
        Iterator it;
        int i;List<String> hashesToDelete = new ArrayList<String>();

        it = itemMap.entrySet().iterator();
        while (it.hasNext()){
            Map.Entry pairs = (Map.Entry)it.next();

            if(!currentItemMap.containsKey(pairs.getKey()))
                hashesToDelete.add((String) pairs.getKey());
        }

        for(i = 0; i < hashesToDelete.size(); i++)
        {
            itemMap.remove(hashesToDelete.get(i));
        }

        if(hashesToDelete.size()>0){
            LOGGER.info(String.format("%s - removed %s old items",feedName, hashesToDelete.size()));
        }else{
            LOGGER.fine(String.format("%s - removed %s old items", feedName, hashesToDelete.size()));
        }
         


        storeHash();
    }

    private void LoadHashes() throws IOException, ParseException {
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader(config.getProperty("nodehashfile","hash.json")));
        JSONObject jItemMap = (JSONObject)obj;
        itemMap.clear();
        itemMap.putAll(jItemMap);
    }

    private void storeHash() throws IOException {
        JSONObject jMap = new JSONObject(itemMap);
        FileWriter fileWriter = new FileWriter(config.getProperty("nodehashfile","hash.json"));
        fileWriter.write(jMap.toJSONString());
        fileWriter.flush();
        fileWriter.close();
    }

    private List<RssItemBean> loadItems(String url) throws Exception {
        RssFeed feed = rss.load(url);

        RssChannelBean channel = feed.getChannel();
        LOGGER.finest(String.format("%s - Parse feed",feedName));
        LOGGER.finest(String.format("Feed Title: %s",channel.getTitle()));

        // Gets and iterate the items of the feed
        LOGGER.finest(String.format("%s - Get items",feedName));
        List<RssItemBean> ret = feed.getItems();

        LOGGER.finest(String.format("For url(%s) we have %s items",url ,ret.size()));

        return ret;
    }

    private void printElement(RssItemBean element) throws XMPPException, MalformedURLException, InterruptedException {
//        ConfigureForm form = new ConfigureForm(FormType.submit);
//        form.setPersistentItems(false);
//        form.setDeliverPayloads(true);
//        form.setAccessModel(AccessModel.open);

        LeafNode myNode = null;
        LOGGER.finest(String.format("%s - Parse item",feedName));
        NewsItem ni = new NewsItem(logLevel, element , newsHub);

        LOGGER.finest(String.format("%s - Get payload",feedName));
        PayloadItem p = ni.genPayload();

        try {
            SendPayload(p);
            Thread.sleep(100);
        } catch (Exception e){
            LOGGER.warning(String.format("%s - %s", feedName, e.toString()));
            if(e.toString().contains("item-not-found(404)")){
                ConfigureForm form = new ConfigureForm(FormType.submit);
                form.setAccessModel(AccessModel.open);
                form.setDeliverPayloads(true);
                form.setNotifyRetract(true);
                form.setPersistentItems(false);
                form.setPublishModel(PublishModel.open);
                form.setSubscribe(true);

                LeafNode leaf = (LeafNode) jabber.pmanager.createNode(newsHub, form);
                leaf.subscribe(jabber.getJid());

                LOGGER.finest(String.format("%s - Post",feedName));
                leaf.send(p);
            }   else if(e.toString().contains("Not connected to server"))
            {
                jabber.disconnect();
                Thread.sleep(3000);
                jabber.connect();
                Thread.sleep(3000);
                SendPayload(p);
                Thread.sleep(100);
            }

        }
    }

    private void SendPayload(PayloadItem p) throws XMPPException {
        LeafNode myNode;
        myNode = jabber.pmanager.getNode(newsHub);

        LOGGER.finest(String.format("%s - Post",feedName));
        myNode.send(p);
    }

//    private void printElement(Element element) throws XMPPException, MalformedURLException {
//
//        ConfigureForm form = new ConfigureForm(FormType.submit);
//        form.setPersistentItems(false);
//        form.setDeliverPayloads(true);
//        form.setAccessModel(AccessModel.open);
//
//        LeafNode myNode = jabber.pmanager.getNode(newsHub);
//
//        LOGGER.finest(String.format("%s - Parse item",feedName));
//        NewsItem ni = new NewsItem(logLevel, element,newsHub);
//
//        LOGGER.finest(String.format("%s - Get payload",feedName));
//        PayloadItem p = ni.genPayload();
//
//        LOGGER.finest(String.format("%s - Post",feedName));
//        myNode.send(p);
//    }

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

    @Override
    public String toString() {
        return "Feed [feedName=\"" + feedName + "\", link=\"" + link.toString() + "\", checkInterval="
                + checkInterval + ", newsHub=\"" + newsHub +
                "\"]";
    }
}
