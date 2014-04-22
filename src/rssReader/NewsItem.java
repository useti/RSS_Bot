package rssReader;

import org.horrabin.horrorss.RssItemBean;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.w3c.dom.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: ytihoglaz
 * Date: 21.08.13
 * Time: 17:22
 * To change this template use File | Settings | File Templates.
 */
public class NewsItem implements IRss {
    public NewsItem(Level logLevel, Element item, String newsHub) throws MalformedURLException {
        this.logLevel = logLevel;
        LOGGER.setLevel(this.logLevel);
        this.item = item;
        this.newsHub = newsHub;

        String ttitle = getElementValue(this.item, "title");
        this.title = ttitle.substring(0,ttitle.indexOf("&nbsp;")) + " " + ttitle.substring(ttitle.indexOf("&nbsp;")+6);
        this.link = new URL(getElementValue(this.item, "link"));
        this.pDate = new Date(Date.parse(getElementValue(this.item,"pubDate")));
        this.author = getElementValue(this.item,"dc:creator");
        this.description =  getElementValue(this.item,"description");

        LOGGER.config(
                "Title: " + getElementValue(this.item,"title") +
                "\nLink: " + getElementValue(this.item,"link") +
                "\nPublish Date: " + getElementValue(this.item,"pubDate") +
                "\nAuthor: " + getElementValue(this.item,"dc:creator") +
                "\nComments: " + getElementValue(this.item,"wfw:comment") +
                "\nDescription: " + getElementValue(this.item,"description"));
    }

    public NewsItem(Level logLevel, RssItemBean item, String newsHub) throws MalformedURLException {
        this.logLevel = logLevel;
        LOGGER.setLevel(this.logLevel);
//        this.item = item;
        this.newsHub = newsHub;

        String ttitle = item.getTitle();
        if(ttitle.contains("&nbsp;"))
        {
            this.title = ttitle.substring(0,ttitle.indexOf("&nbsp;")) + " " + ttitle.substring(ttitle.indexOf("&nbsp;")+6);
        }
        else
        {
            this.title = ttitle;
        }
        this.link = new URL(item.getLink());
        this.pDate = item.getPubDate();
        this.author = item.getAuthor() == null ? "" :item.getAuthor();
        this.description = item.getDescription()==null ? item.getLink() : item.getDescription();

        LOGGER.config(
                "Title: " + title +
                        "\nLink: " + link +
                        "\nPublish Date: " + pDate +
                        "\nAuthor: " + author +
                        "\nComments: " + "" +
                        "\nDescription: " + description);
        this.item = null;
    }

    private final static Logger LOGGER = Logger.getLogger(NewsItem.class.getName());
    private final Level logLevel;

    private String getCharacterDataFromElement(Element e) {
        try {
            Node child = e.getFirstChild();
            if(child instanceof CharacterData) {
                CharacterData cd = (CharacterData) child;
                return cd.getData();
            }
        }
        catch(Exception ex) {
            //LOGGER.severe(ex.toString());
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

    private final Element item;
    private final String title;
    private final URL link;
    private final Date pDate;
    private final String author;
    private final String description;
    private final String newsHub;

    @Override
    public String toString() {
        return "NewsItem [ title="+title+" , link="+
                link+" , pDate="+pDate.toString()+" , author="+
                author+", description="+description+" , newsHub=" +
                newsHub+ " ]";
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public URL getLink() {
        return link;
    }

    @Override
    public Date getPDate() {
        return pDate;
    }

    @Override
    public String getAuthor() {
        return author;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public PayloadItem genPayload() {
        HtmlCleaner cleaner = new HtmlCleaner();

        CleanerProperties props = cleaner.getProperties();

        props.setRecognizeUnicodeChars(true);

        props.setAdvancedXmlEscape(true);

        props.setTranslateSpecialEntities(true);
        props.setTransSpecialEntitiesToNCR(true);

        TagNode t = cleaner.clean(String.format("<a href=\"%s\">%s</a>",link.toString(),link.toString()));
        String clean_link = cleaner.getInnerHtml(t);

        t = cleaner.clean(description);
        String clean_description = cleaner.getInnerHtml(t);

        String clean_author = author == null ? "": author;

        PayloadItem p = new PayloadItem(
            newsHub + "-" + System.currentTimeMillis(),
                new SimplePayload(
                        "post",
                        "pubsub:" + newsHub + ":post",
                        "<post xmlns='pubsub:" + newsHub + ":post'>" +
                                "<title>" + title.replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</title>"+
                                "<author>" + clean_author.replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</author> " +
                                "<link>" + clean_link.toString().replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</link>" +
                                "<pDate>"+ pDate.toString().replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) +"</pDate> " +
                                "<description>" + clean_description.replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</description> " +
                                "</post>"));

        return p;
    }
}
