package rssReader;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public final class JabberClient {

    public static class Builder {

        public JabberClient build() {return new JabberClient(this);}

        private Builder(String addr) {this.addr = addr;}

        public Builder setPort(int port) {this.port = port; return this;}
        public Builder setUser(String user) {this.user = user; return this;}
        public Builder setPassword(String password) {this.password = password; return this;}
        public Builder setService(String service) {this.service = service; return this;}
        public Builder setResource(String resource) {this.resource = resource; return this;}

        private final String addr;
        private int port = 5222;
        private String user, password, service, resource;

    }

    public static Builder newBuilder(String addr) {return new Builder(addr);}

    private JabberClient(Builder bld) {
        this.pr = bld;
        conn_config = (pr.service != null) ?
                new ConnectionConfiguration(pr.addr, pr.port, pr.service) :
                new ConnectionConfiguration(pr.addr, pr.port);
    }


    public void setSelfSignedCertificateEnabled(boolean value) {
        conn_config.setSelfSignedCertificateEnabled(value);
    }
    public void setNotMatchingDomainCheckEnabled(boolean value) {
        conn_config.setNotMatchingDomainCheckEnabled(value);
    }
    public void setSASLAuthenticationEnabled(boolean value) {
        conn_config.setSASLAuthenticationEnabled(value);
    }

    public void setSASLPlain() {
        SASLAuthentication.supportSASLMechanism("PLAIN");
    }

    public void setSecurityMode(boolean value) {
        conn_config.setSecurityMode(value ? ConnectionConfiguration.SecurityMode.enabled :
                ConnectionConfiguration.SecurityMode.disabled);
    }
    public void setReconnectionAllowed(boolean value) {
        conn_config.setReconnectionAllowed(value);
    }
    public void setCompressionEnabled(boolean value) {
        conn_config.setCompressionEnabled(value);
    }


    public ConnectionConfiguration getJabberconfig() {
        return conn_config;
    }

    public XMPPConnection connect() throws XMPPException {
        conn = new XMPPConnection(conn_config);
        conn.connect();
        conn.login(pr.user, pr.password, "rss");
        pmanager = new PubSubManager(conn);
        return conn;
    }

    public void disconnect() {
        if (conn != null)
            conn.disconnect();
    }

    public void sendMessage(String to_jid, String msg) throws XMPPException {
        if (conn == null) throw new XMPPException("not connected");

        ChatManager chatmanager = conn.getChatManager();
        Chat newChat = chatmanager.createChat(to_jid, new MessageListener() {
            public void processMessage(Chat chat, Message message) {
            }
        });
        newChat.sendMessage(msg);
    }


    public String getJid (){
        return pr.user;
    }

    private final Builder pr;
    private final ConnectionConfiguration conn_config;
    public XMPPConnection conn;
    public PubSubManager pmanager;

}
