package com.cansever.messagehandler.rabbit;

import com.cansever.messagehandler.ProducerApplication;
import com.rabbitmq.client.*;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * User: TTACANSEVER
 */
public class MessageBackupStateListener extends Thread {

    private static Logger log = Logger.getLogger(MessageBackupStateListener.class);

    public void run(){

        try {
            String rabbitmqHost = ProducerApplication.propertiesMap.get("backup.rabbitmq.host");
            final String exchangeName = ProducerApplication.propertiesMap.get("backup.rabbitmq.state.exchange");
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(rabbitmqHost);
            factory.setUsername(ProducerApplication.propertiesMap.get("backup.rabbitmq.username"));
            factory.setPassword(ProducerApplication.propertiesMap.get("backup.rabbitmq.password"));
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(exchangeName, "fanout");
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, exchangeName, "");

            log.info("Waiting for messages from " + exchangeName);

            DefaultConsumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, "UTF-8");
                    log.info("Received(" + exchangeName + "): " + message);
                    String[] values = message.split(",,,");
                    String username = values[0];
                    int state = Integer.parseInt(values[1]);
                    if(state == 1) {
                        ProducerApplication.BACKUP_ENABLED_USERS_SET.add(username);
                        log.info(username + " added to backup enabled set...");
                    } else {
                        ProducerApplication.BACKUP_ENABLED_USERS_SET.remove(username);
                        log.info(username + " removed from backup enabled set...");
                    }
                }
            };
            channel.basicConsume(queueName, true, consumer);
            log.info("MessageBackupStateListener attached to RabbitMQ " + rabbitmqHost + " for listening " + exchangeName);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }


}
