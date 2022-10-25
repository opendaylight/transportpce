/*
 * Copyright © 2017 ATT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.kafka;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaPublisherImpl implements KafkaPublisher {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaPublisherImpl.class);
    private static final KafkaProducer<String, String> producer;
    private static volatile KafkaPublisher kafkaPublisher = null;
    private static boolean skipKafka = false;

    static {
        Properties props = new Properties();
        String propertyFileName = "kafka.properties";
        InputStream inputStream = KafkaPublisherImpl.class.getClassLoader().getResourceAsStream(propertyFileName);
        LOG.info("Trying to read kafka properties file {}", propertyFileName);

        try {
            if (inputStream != null) {
                props.load(inputStream);
            } else {
                LOG.warn("Kafka property file '{}' is empty", propertyFileName);
            }
        } catch (IOException e) {
            LOG.error("Kafka property file '{}' was not found in the classpath", propertyFileName, e);
        }
//        props.put("bootstrap.servers", "192.168.3.2:9092");
//        props.put("acks", "all");
//        props.put("retries", 0);
//        props.put("batch.size", 16384);
//        props.put("linger.ms", 1);
//        props.put("buffer.memory", 33554432);
//        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        Thread.currentThread().setContextClassLoader(null);
        if (props.getProperty("bootstrap.servers").equals("SKIP")) {
            skipKafka = true;
            producer = null;
            LOG.info("Bypssing Kafka messages since bootstrap.servers = SKIP");
        } else {
            producer = new KafkaProducer<>(props);
            LOG.info("Instantiate Kafka publisher with properties {}", props);
        }
    }

    //singleton kafkapublisher for other services usage
    public static KafkaPublisher getPublisher() {
        if (kafkaPublisher == null) {
            synchronized (KafkaPublisher.class) {
                if (kafkaPublisher == null) {
                    kafkaPublisher = new KafkaPublisherImpl();
                }
            }
        }
        return kafkaPublisher;
    }

    public String publishNotification(String topicName, String key, String message) {
        if (!skipKafka) {
            LOG.info("kakfaPublisher called: Topic {} Key {} Message {}", topicName, key, message);
            if (producer != null) {
                producer.send(new ProducerRecord<>(topicName, key, message));
            }
        } else {
            LOG.info("kakfaPublisher SKIPPED: Topic {} Key {} Message {}", topicName, key, message);
        }
        return "success";
    }

}
