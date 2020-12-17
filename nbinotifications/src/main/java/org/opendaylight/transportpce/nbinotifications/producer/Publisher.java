/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.producer;

import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.utils.NbiNotificationsUtils;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.NotificationService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Publisher {
    private static final Logger LOG = LoggerFactory.getLogger(Publisher.class);

    private final String id;
    private final KafkaProducer<String, String> producer;
    private final AdminClient client;
    private final JsonStringConverter<NotificationService> serializer;

    public Publisher(String id, JsonStringConverter<NotificationService> serializer) {
        this.serializer = serializer;
        Properties propsProducer = NbiNotificationsUtils.loadProperties("publisher.properties");
        propsProducer.put(ProducerConfig.CLIENT_ID_CONFIG, id);
        producer = new KafkaProducer<>(propsProducer);
        Properties propsClient = new Properties();
        propsClient.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                propsProducer.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        Thread.currentThread().setContextClassLoader(null);
        client = KafkaAdminClient.create(propsClient);
        this.id = id;
    }

    // Exist for tests only
    public Publisher(String id, KafkaProducer<String, String> producer, AdminClient client,
            JsonStringConverter<NotificationService> serializer) {
        this.producer = producer;
        this.client = client;
        this.id = id;
        this.serializer = serializer;
    }

    public void createTopic(String topicName, int numberPartitions) {
        LOG.info("CreateTopic request '{}' ", topicName);
        CreateTopicsResult createTopicsResult =
                client.createTopics(Collections.singletonList(new NewTopic(topicName, numberPartitions, (short) 1)));
        try {
            createTopicsResult.all().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Creating topic '{}' was interrupted or aborted", topicName);
        }
    }

    public void close() {
        producer.close();
        client.close();
    }

    public void deleteTopic(String topicName) {
        LOG.info("DeleteTopic request '{}' ", topicName);
        DeleteTopicsResult deleteTopicResult = client.deleteTopics(Collections.singletonList(topicName));
        try {
            deleteTopicResult.all().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Deleting topic '{}' was interrupted or aborted", topicName);
        }
    }

    public void sendEvent(NotificationService notificationService) {
        try {
            LOG.info("SendEvent request to topic '{}' ", notificationService.getConnectionType().getName());
            InstanceIdentifier<NotificationService> iid = InstanceIdentifier.builder(NotificationService.class).build();
            producer.send(new ProducerRecord<>(notificationService.getConnectionType().getName(), id,
                    serializer.createJsonStringFromDataObject(iid, notificationService)));
            producer.flush();
        } catch (IOException e) {
            LOG.warn("Cannot send event {}", notificationService, e);
        }
    }
}
