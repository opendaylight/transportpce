/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessServiceBuilder;

public class NbiNotificationsListenerImplTest extends AbstractTest {
    @Mock
    private Publisher<NotificationProcessService> publisherService;
    @Mock
    private Publisher<NotificationAlarmService> publisherAlarm;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void onPublishNotificationServiceTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
                Map.of("test", publisherAlarm));
        PublishNotificationProcessService notification = new PublishNotificationProcessServiceBuilder()
                .setPublisherName("test")
                .setCommonId("commonId")
                .setConnectionType(ConnectionType.Service)
                .setMessage("Service deleted")
                .setOperationalState(State.OutOfService)
                .setServiceName("service name")
                .build();
        listener.onPublishNotificationProcessService(notification);
        verify(publisherService, times(1)).sendEvent(any(), anyString());
    }

    @Test
    public void onPublishNotificationServiceWrongPublisherTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
                Map.of("test", publisherAlarm));
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
    public void onPublishNotificationAlarmServiceTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
                Map.of("test", publisherAlarm));
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
    public void onPublishNotificationAlarmServiceWrongPublisherTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
                Map.of("test", publisherAlarm));
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
}
