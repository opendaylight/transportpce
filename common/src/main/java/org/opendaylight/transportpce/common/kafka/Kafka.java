package org.opendaylight.transportpce.common.kafka;

import java.util.Properties;
import org.apache.kafka.clients.producer.Producer;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Notification;

public interface Kafka {

    Producer<String, String> getProducer();

    void sendStream(String topicName, Notification notification);

    Properties getDefaultPorperties();

    void setDefaultPorperties(Properties defaultPorperties);

    void createTopic(String topicName);

}
