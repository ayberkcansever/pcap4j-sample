package com.cansever.messagehandler.producer;

import com.cansever.messagehandler.message.MessageObj;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.log4j.Logger;
import org.xmpp.packet.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: TTACANSEVER
 */
public class ProducerThread extends Thread {

    private static Logger log = Logger.getLogger(ProducerThread.class);

    public static LinkedBlockingQueue<Message> MESSAGE_QUEUE = new LinkedBlockingQueue<Message>(1000000);

    private final KafkaProducer producer;
    private final String topic;
    private final Properties props = new Properties();
    private Schema schema;

    public ProducerThread(String bootstrapServers, String topic) {
        this.topic = topic;
        this.props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        this.props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        this.props.put("acks", "1");
        this.props.put("retries", "3");
        this.props.put("linger.ms", 5);
        this.props.put("block.on.buffer.full", false);
        this.props.put("bootstrap.servers", bootstrapServers);
        this.producer = new KafkaProducer<String, byte[]>(this.props);
        try {
            schema = new Schema.Parser().parse(new File("Message.avsc"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (true) {
            try {
                Message message = MESSAGE_QUEUE.take();
                MessageObj messageObj = new MessageObj(message);

                GenericRecord record = new GenericData.Record(schema);
                record.put("msgId", messageObj.getMsgId());
                record.put("username", messageObj.getUsername());
                record.put("jid", messageObj.getJid());
                record.put("sentTime", messageObj.getSentTime());
                record.put("stanza", messageObj.getStanza());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                DatumWriter<GenericRecord> writer = new GenericDatumWriter<GenericRecord>(schema);
                Encoder encoder = EncoderFactory.get().binaryEncoder(out, null);
                writer.write(record, encoder);
                encoder.flush();
                out.close();
                producer.send(new ProducerRecord<String, byte[]>(this.topic, out.toByteArray()));
            } catch (Exception e) {
                log.error(e);
                e.printStackTrace();
            }
        }
    }

}
