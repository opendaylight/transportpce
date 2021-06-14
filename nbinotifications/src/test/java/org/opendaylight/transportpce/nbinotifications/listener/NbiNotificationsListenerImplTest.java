/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.producer.PublisherAlarm;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210628.PublishNotificationServiceBuilder;

public class NbiNotificationsListenerImplTest extends AbstractTest {
    @Mock
    private Publisher publisher;
    @Mock
    private PublisherAlarm publisherAlarm;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void onPublishNotificationServiceTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisher),
                Map.of("test", publisherAlarm));
        PublishNotificationService notification = new PublishNotificationServiceBuilder().setTopic("test")
                .setCommonId("commonId").setConnectionType(ConnectionType.Service).setMessage("Service deleted")
                .setOperationalState(State.OutOfService).setServiceName("service name").build();
        listener.onPublishNotificationService(notification);
        verify(publisher, times(1)).sendEvent(any());
    }

    @Test
    public void onPublishNotificationServiceWrongTopicTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisher),
                Map.of("test", publisherAlarm));
        PublishNotificationService notification = new PublishNotificationServiceBuilder().setTopic("wrongtopic")
                .setCommonId("commonId").setConnectionType(ConnectionType.Service).setMessage("Service deleted")
                .setOperationalState(State.OutOfService).setServiceName("service name").build();
        listener.onPublishNotificationService(notification);
        verify(publisher, times(0)).sendEvent(any());
    }
}
