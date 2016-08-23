package com.cansever.messagehandler.message;

import org.xmpp.packet.Message;
import org.xmpp.packet.PacketExtension;

/**
 * User: TTACANSEVER
 */
public class MessageObj {

    private String msgId;
    private String username;
    private String jid;
    private String stanza;
    private long sentTime;

    public MessageObj(Message message) {
        PacketExtension ext = message.getExtension("backup", "tims:xmpp:backup");
        if(message.getFrom() != null) {
            if (ext != null && "in".equals(ext.getElement().getTextTrim())) {
                this.username = message.getFrom().getNode();
                this.jid = message.getTo().toBareJID();
            } else {
                this.username = message.getTo().getNode();
                this.jid = message.getFrom().toBareJID();
            }
        }
        PacketExtension timeExtension = message.getExtension("sentTime", "tims:xmpp:messageExtensions");
        long time;
        if(timeExtension != null) {
            time = Long.parseLong(timeExtension.getElement().getTextTrim());
        } else {
            time = System.currentTimeMillis();
        }
        this.sentTime = time;
        this.stanza = message.toXML();
        this.msgId = message.getID().replace("_".concat(this.username), "");
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public String getStanza() {
        return stanza;
    }

    public void setStanza(String stanza) {
        this.stanza = stanza;
    }

    public long getSentTime() {
        return sentTime;
    }

    public void setSentTime(long sentTime) {
        this.sentTime = sentTime;
    }
}
