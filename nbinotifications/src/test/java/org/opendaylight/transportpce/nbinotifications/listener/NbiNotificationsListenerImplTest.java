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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsImpl;
import org.opendaylight.transportpce.nbinotifications.producer.Publisher;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.NotificationTapiService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationAlarmServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishTapiNotificationService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishTapiNotificationServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class NbiNotificationsListenerImplTest extends AbstractTest {
    private NbiNotificationsImpl nbiNotificationsImpl;
    public static NetworkTransactionService networkTransactionService;
    private TopicManager topicManager;

    @Mock
    private Publisher<NotificationProcessService> publisherService;
    @Mock
    private Publisher<NotificationAlarmService> publisherAlarm;
    @Mock
    private Publisher<NotificationTapiService> publisherTapiService;

    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        MockitoAnnotations.openMocks(this);
        topicManager = TopicManager.getInstance();
        networkTransactionService = new NetworkTransactionImpl(
            new RequestProcessor(getDataStoreContextUtil().getDataBroker()));
        JsonStringConverter<NotificationProcessService> converter = new JsonStringConverter<>(
            getDataStoreContextUtil().getBindingDOMCodecServices());
        JsonStringConverter<NotificationAlarmService> converterAlarm = new JsonStringConverter<>(
            getDataStoreContextUtil().getBindingDOMCodecServices());
        JsonStringConverter<NotificationTapiService> converterTapi = new JsonStringConverter<>(
            getDataStoreContextUtil().getBindingDOMCodecServices());
        topicManager.setTapiConverter(converterTapi);
        NotificationServiceDataUtils.createTapiContext(networkTransactionService);

        nbiNotificationsImpl = new NbiNotificationsImpl(converter, converterAlarm, converterTapi,
            "localhost:8080", networkTransactionService, topicManager);
    }

    @Test
    public void onPublishNotificationServiceTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
                Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
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
    public void onPublishNotificationAlarmServiceTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
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
    public void onPublishNotificationAlarmServiceWrongPublisherTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
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
    public void onPublishTapiNotificationServiceTest() throws InterruptedException, ExecutionException {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
            Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        this.topicManager.setNbiNotificationsListener(listener);
        CreateNotificationSubscriptionServiceInputBuilder builder
            = NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder();
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
            nbiNotificationsImpl.createNotificationSubscriptionService(builder.build());
        PublishTapiNotificationService notification
            = new PublishTapiNotificationServiceBuilder(NotificationServiceDataUtils.buildReceivedTapiAlarmEvent())
                .setTopic(result.get().getResult().getSubscriptionService().getSubscriptionFilter()
                    .getRequestedObjectIdentifier().get(0).getValue())
                .build();
        listener.onPublishTapiNotificationService(notification);
        verify(publisherTapiService, times(1)).sendEvent(any(), eq(notification.getTopic()));
    }

    @Test
    public void onPublishTapiNotificationServiceTestWrongPublisherTest() {
        NbiNotificationsListenerImpl listener = new NbiNotificationsListenerImpl(Map.of("test", publisherService),
            Map.of("test", publisherAlarm), Map.of("test", publisherTapiService));
        this.topicManager.setNbiNotificationsListener(listener);
        CreateNotificationSubscriptionServiceInputBuilder builder
            = NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder();
        ListenableFuture<RpcResult<CreateNotificationSubscriptionServiceOutput>> result =
            nbiNotificationsImpl.createNotificationSubscriptionService(builder.build());
        PublishTapiNotificationService notification
            = new PublishTapiNotificationServiceBuilder(NotificationServiceDataUtils.buildReceivedTapiAlarmEvent())
                .setTopic(UUID.randomUUID().toString())
                .build();
        listener.onPublishTapiNotificationService(notification);
        verify(publisherTapiService, times(0)).sendEvent(any(), eq(notification.getTopic()));
    }
}
