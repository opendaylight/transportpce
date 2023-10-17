/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230726.PublishTapiNotificationServiceBuilder;

public class NbiNotificationsHandlerTest extends AbstractTest {
    @Mock
    private Publisher<NotificationProcessService> publisherService;
    @Mock
    private Publisher<NotificationAlarmService> publisherAlarm;
    @Mock
    private Publisher<NotificationTapiService> publisherTapiService;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void onPublishNotificationServiceTest() {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        PublishNotificationProcessService notification = new PublishNotificationProcessServiceBuilder()
                .setPublisherName("test")
                .setCommonId("commonId")
                .setConnectionType(ConnectionType.Service)
                .setIsTempService(false)
                .setMessage("Service deleted")
                .setOperationalState(State.OutOfService)
                .setServiceName("service name")
                .build();
        listener.onPublishNotificationProcessService(notification);
        verify(publisherService, times(1)).sendEvent(any(), anyString());
    }

    @Test
    void onPublishNotificationServiceWrongPublisherTest() {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        PublishNotificationProcessService notification = new PublishNotificationProcessServiceBuilder()
                .setPublisherName("wrongPublisher")
                .setCommonId("commonId")
                .setConnectionType(ConnectionType.Service)
                .setMessage("Service deleted")
                .setOperationalState(State.OutOfService)
                .setServiceName("service name")
                .build();
        listener.onPublishNotificationProcessService(notification);
        verify(publisherService, times(0)).sendEvent(any(), anyString());
    }

    @Test
    void onPublishNotificationAlarmServiceTest() {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        PublishNotificationAlarmService notification = new PublishNotificationAlarmServiceBuilder()
                .setPublisherName("test")
                .setConnectionType(ConnectionType.Service)
                .setMessage("The service is now inService")
                .setOperationalState(State.OutOfService)
                .setServiceName("service name")
                .build();
        listener.onPublishNotificationAlarmService(notification);
        verify(publisherAlarm, times(1)).sendEvent(any(), anyString());
    }

    @Test
    void onPublishNotificationAlarmServiceWrongPublisherTest() {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        PublishNotificationAlarmService notification = new PublishNotificationAlarmServiceBuilder()
                .setPublisherName("wrongPublisher")
                .setConnectionType(ConnectionType.Service)
                .setMessage("The service is now inService")
                .setOperationalState(State.OutOfService)
                .setServiceName("service name")
                .build();
        listener.onPublishNotificationAlarmService(notification);
        verify(publisherAlarm, times(0)).sendEvent(any(), anyString());
    }

    @Test
    void onPublishTapiNotificationServiceTest() throws ExecutionException, InterruptedException {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));

        PublishTapiNotificationService notification
            = new PublishTapiNotificationServiceBuilder(NotificationServiceDataUtils.buildReceivedTapiAlarmEvent())
                .setTopic("test")
                .build();
        listener.onPublishTapiNotificationService(notification);
        verify(publisherTapiService, times(1)).sendEvent(any(), anyString());
    }

    @Test
    void onPublishTapiNotificationServiceTestWrongPublisherTest() {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
            Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        PublishTapiNotificationService notification
            = new PublishTapiNotificationServiceBuilder(NotificationServiceDataUtils.buildReceivedTapiAlarmEvent())
                .setTopic(UUID.randomUUID().toString())
                .build();
        listener.onPublishTapiNotificationService(notification);
        verify(publisherTapiService, times(0)).sendEvent(any(), eq(notification.getTopic()));
    }

    @Test
    void getTapiPublisherFromTopicTest() {
        NbiNotificationsHandler listener = new NbiNotificationsHandler(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        assertNull(listener.getTapiPublisherFromTopic("toto"));
        assertEquals(publisherTapiService, listener.getTapiPublisherFromTopic("test"));
    }
}
