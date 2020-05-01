package org.opendaylight.transportpce.common.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaImpl implements Kafka {

    private Producer<String, JsonNode> producer = null;
    private Properties defaultPorperties;
    private static final Logger LOG = LoggerFactory.getLogger(KafkaImpl.class);

    public KafkaImpl() {
        LOG.info("New kafka producer created!!");
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", StringSerializer.class);
        props.put("value.serializer", JsonSerializer.class);
        setDefaultPorperties(props);
        this.producer = new KafkaProducer<>(getDefaultPorperties());
        // createTopic("serviceCreation");
    }

    @Override
    public Producer getProducer() {
        return this.producer;
    }

    @Override
    public void sendStream(String topicName, Notification notification) {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.valueToTree(notification);
        LOG.info("Json object message to send {}", jsonNode.toString());
        this.producer.send(new ProducerRecord<>(topicName, jsonNode));
    }

    @Override
    public Properties getDefaultPorperties() {
        return defaultPorperties;
    }

    @Override
    public void setDefaultPorperties(Properties defaultPorperties) {
        this.defaultPorperties = defaultPorperties;
    }

    @Override
    public void createTopic(String topicName) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        try (AdminClient adminClient = AdminClient.create(props)) {
            try {
                // Define topic
                NewTopic newTopic = new NewTopic(topicName, 1, (short)1);
                // Create topic, which is async call.
                final CreateTopicsResult createTopicsResult = adminClient.createTopics(Collections.singleton(newTopic));
                // Since the call is Async, Lets wait for it to complete.
                LOG.info("New topic {} created = {}", topicName, createTopicsResult.values().get(topicName).get());
            } catch (InterruptedException | ExecutionException e) {
                if (!(e.getCause() instanceof TopicExistsException)) {
                    LOG.error("Couldnt create topic: {}", e.getMessage());
                }
            }
        }
    }

    // Todo --> function to add new topic to kafka server
    // Todo --> the message send to kafka should be json, not string
}
