/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.utils;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.nbinotifications.listener.NbiNotificationsListenerImpl;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.TapiNotificationSerializer;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.Notification;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TopicManager {

    private static final Logger LOG = LoggerFactory.getLogger(TopicManager.class);
    private static TopicManager instance = new TopicManager();

    private Map<String, Publisher<Notification>> tapiPublisherMap = new HashMap<>();
    private String publisherServer;
    private JsonStringConverter<Notification> tapiConverter;
    private NbiNotificationsListenerImpl nbiNotificationsListener;
    private Map<String, Publisher<NotificationAlarmService>> alarmPublisherMap = new HashMap<>();
    private Map<String, Publisher<NotificationProcessService>> processPublisherMap = new HashMap<>();
    private JsonStringConverter<NotificationProcessService> processConverter;
    private JsonStringConverter<NotificationAlarmService> alarmConverter;
    private int calledSetConverter = 0;

    private TopicManager() {
    }

    public static TopicManager getInstance() {
        return instance;
    }

    public void setNbiNotificationsListener(NbiNotificationsListenerImpl nbiNotificationsListener) {
        this.nbiNotificationsListener = nbiNotificationsListener;
    }

    public void setProcessConverter(JsonStringConverter<NotificationProcessService> processConverter) {
        this.processConverter = processConverter;
    }

    public void setAlarmConverter(JsonStringConverter<NotificationAlarmService> alarmConverter) {
        this.alarmConverter = alarmConverter;
    }

    public void setTapiConverter(JsonStringConverter<Notification> tapiConverter) {
        this.tapiConverter = tapiConverter;
        this.calledSetConverter++;
    }

    public void setPublisherServer(String publisherServer) {
        this.publisherServer = publisherServer;
    }

    public void addProcessTopic(String topic) {
        LOG.info("Adding process topic: {}", topic);
        processPublisherMap.put(topic, new Publisher<>(topic, publisherServer, processConverter,
            NotificationServiceSerializer.class));
        if (this.nbiNotificationsListener != null) {
            this.nbiNotificationsListener.setPublishersServiceMap(processPublisherMap);
        }
    }

    public void addAlarmTopic(String topic) {
        LOG.info("Adding process topic: {}", topic);
        alarmPublisherMap.put(topic, new Publisher<>(topic, publisherServer, alarmConverter,
                NotificationAlarmServiceSerializer.class));
        if (this.nbiNotificationsListener != null) {
            this.nbiNotificationsListener.setPublishersAlarmMap(alarmPublisherMap);
        }
    }

    public void addTapiTopic(String topic) {
        LOG.info("Adding tapi topic: {}", topic);
        LOG.info("converter: {}", tapiConverter);
        LOG.info("called setConverter: {}", calledSetConverter);
        tapiPublisherMap.put(topic, new Publisher<>(topic, publisherServer, tapiConverter,
                TapiNotificationSerializer.class));
        if (this.nbiNotificationsListener != null) {
            this.nbiNotificationsListener.setTapiPublishersMap(tapiPublisherMap);
        }
    }

    public Map<String, Publisher<Notification>> getTapiTopicMap() {
        return this.tapiPublisherMap;
    }

    public Map<String, Publisher<NotificationAlarmService>> getAlarmTopicMap() {
        return this.alarmPublisherMap;
    }

    public Map<String, Publisher<NotificationProcessService>> getProcessTopicMap() {
        return this.processPublisherMap;
    }
}
