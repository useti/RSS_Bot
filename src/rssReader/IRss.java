package rssReader;

import org.jivesoftware.smackx.pubsub.PayloadItem;

import java.net.URL;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: ytihoglaz
 * Date: 21.08.13
 * Time: 15:27
 * To change this template use File | Settings | File Templates.
 */
public interface IRss {
    // Title
    public String getTitle();

    // Link
    public URL getLink();

    // PDate
    public Date getPDate();

    // Author
    public String getAuthor();

    // Description
    public String getDescription();

    public PayloadItem genPayload();
}
