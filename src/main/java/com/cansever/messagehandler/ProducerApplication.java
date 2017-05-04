package com.cansever.messagehandler;

/**
 * User: TTACANSEVER
 */

import com.cansever.messagehandler.producer.ProducerThread;
import com.cansever.messagehandler.rabbit.MessageBackupStateListener;
import com.cansever.messagehandler.task.MessageProcessorProducerTask;
import com.google.common.collect.Sets;
import org.pcap4j.core.BpfProgram;
import org.pcap4j.core.NotOpenException;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProducerApplication {

    public static Set<String> BACKUP_ENABLED_USERS_SET = Sets.newConcurrentHashSet();

    public static Map<String, String> propertiesMap = new HashMap<String, String>();

    public static ExecutorService executorService;
    public static String port;
    public static XmlPullParserFactory factory;

    public static void main(String[] args) throws PcapNativeException, NotOpenException, IOException, XmlPullParserException {

        if (args.length < 2) {
            System.out.println("usage: java -jar [network interface name (must)] [port (must)]");
            System.exit(-1);
        }

        readProperties();
        System.out.println(BACKUP_ENABLED_USERS_SET.size() + " users enabled message backup feature...");
        new MessageBackupStateListener().start();

        PcapHandle.Builder builder = new PcapHandle.Builder(args[0]);
        final PcapHandle handle = builder
                .promiscuousMode(PromiscuousMode.PROMISCUOUS)
                .timeoutMillis(5000)
                .bufferSize(128 * 1024 * 1024)
                .snaplen(65536)
                .build();

        port = args[1].trim();
        handle.setFilter("port " + port, BpfProgram.BpfCompileMode.OPTIMIZE);

        try {
            factory = XmlPullParserFactory.newInstance(MXParser.class.getName(), null);
            factory.setNamespaceAware(true);
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

        executorService = Executors.newFixedThreadPool(Integer.parseInt(propertiesMap.get("executor.thread.pool")));
        new ProducerThread(propertiesMap.get("kafka.bootstrapServers"), propertiesMap.get("kafka.messageHistoryTopic")).start();

        while (true) {
            try {
                Packet packet = handle.getNextPacket();
                if (packet != null) {
                    TcpPacket tcpPacket = packet.get(TcpPacket.class);
                    if (tcpPacket != null) {
                        Packet payload = tcpPacket.getPayload();
                        if (payload != null) {
                            String srcPort = tcpPacket.getHeader().getSrcPort().valueAsString();
                            String stanza = new String(payload.getRawData());
                            if(stanza.startsWith("message")) {
                                stanza = "<".concat(stanza);
                            }
                            executorService.execute(new MessageProcessorProducerTask(stanza, !(port.trim().equals(srcPort))));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void readProperties() throws IOException {
        InputStream in = null;
        try {
            in = new FileInputStream("conf/handler.properties");
            Properties prop = new Properties();
            prop.load(in);

            for (String property : prop.stringPropertyNames()) {
                String value = prop.getProperty(property);
                propertiesMap.put(property, value);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(in != null) {
                in.close();
            }
        }
    }

}
