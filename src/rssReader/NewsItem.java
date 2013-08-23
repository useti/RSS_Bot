package rssReader;

import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.w3c.dom.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ytihoglaz
 * Date: 21.08.13
 * Time: 17:22
 * To change this template use File | Settings | File Templates.
 */
public class NewsItem implements IRss {
    public NewsItem(Element item, String newsHub) throws MalformedURLException {
        this.item = item;
        this.newsHub = newsHub;

        this.title = getElementValue(this.item, "title");
        this.link = new URL(getElementValue(this.item, "link"));
        this.pDate = new Date(Date.parse(getElementValue(this.item,"pubDate")));
        this.author = getElementValue(this.item,"dc:creator");
        this.description =  getElementValue(this.item,"description");

        System.out.println("Title: " + getElementValue(this.item,"title"));
        System.out.println("Link: " + getElementValue(this.item,"link"));
        System.out.println("Publish Date: " + getElementValue(this.item,"pubDate"));
        System.out.println("author: " + getElementValue(this.item,"dc:creator"));
        System.out.println("comments: " + getElementValue(this.item,"wfw:comment"));
        System.out.println("description: " + getElementValue(this.item,"description"));
        System.out.println();
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

//        TagNode t = cleaner.clean(title);
//        String clean_title = t.toString();
//
//        t = cleaner.clean(author);
//        String clean_author = t.toString();
//
        TagNode t = cleaner.clean(link.toString());
        String clean_link = cleaner.getInnerHtml(t);
//
//        t = cleaner.clean(pDate.toString());
//        String clean_pDate = t.toString();

        t = cleaner.clean(description);
        String clean_description = cleaner.getInnerHtml(t);

        PayloadItem p = new PayloadItem(
            newsHub + "-" + System.currentTimeMillis(),
                new SimplePayload(
                        "post",
                        "pubsub:" + newsHub + ":post",
                        "<post xmlns='pubsub:" + newsHub + ":post'>" +
                                "<title>" + title.replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</title>"+
                                "<author>" + author.replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</author> " +
                                "<link>" + clean_link.toString().replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</link>" +
                                "<pDate>"+ pDate.toString().replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) +"</pDate> " +
                                "<description>" + clean_description.replaceAll( "&([^;]+(?!(?:\\w|;)))", "&amp;$1" ) + "</description> " +
                                "</post>"));

        return p;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
