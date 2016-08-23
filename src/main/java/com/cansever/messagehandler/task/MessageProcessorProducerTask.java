package com.cansever.messagehandler.task;

import com.cansever.messagehandler.ProducerApplication;
import com.cansever.messagehandler.XmppPacketReader;
import com.cansever.messagehandler.producer.ProducerThread;
import org.dom4j.Element;
import org.xmpp.packet.Message;

import java.io.StringReader;

/**
 * User: TTACANSEVER
 */
public class MessageProcessorProducerTask extends Thread {

    private XmppPacketReader packetReader;
    private String stanza;
    private boolean inMessage;

    public MessageProcessorProducerTask(String stanza, boolean inMessage) {
        this.stanza = stanza;
        this.inMessage = inMessage;
        packetReader = new XmppPacketReader();
        packetReader.setXPPFactory(ProducerApplication.factory);
    }

    public void run(){
        try {
            Element element = packetReader.read(new StringReader(stanza)).getRootElement();
            String tag = element.getName();
            if("message".equals(tag)) {
                Message message = new Message(element);
                if(message.getFrom() == null) {
                    return;
                }
                ProducerThread.MESSAGE_QUEUE.offer(message);
            }
        } catch (Exception e) {
            //
        }
    }

}
