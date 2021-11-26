/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.listener;

import static org.junit.Assert.assertEquals;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Application;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.junit.Test;
import org.opendaylight.transportpce.dmaap.client.resource.EventsApiStub;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.NbiNotificationsListener;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.PublishNotificationProcessServiceBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.notification.process.service.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev210813.notification.process.service.ServiceZEndBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.LoggerFactory;

public class NbiNotificationsListenerImplTest extends JerseyTest {
    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        return new ResourceConfig(EventsApiStub.class);
    }

    @Test
    public void onPublishNotificationServiceTest() {
        Logger logger = (Logger) LoggerFactory.getLogger(NbiNotificationsListenerImpl.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        NbiNotificationsListener listener = new NbiNotificationsListenerImpl("http://localhost:9998", null, null);
        PublishNotificationProcessService notification = new PublishNotificationProcessServiceBuilder()
                .setCommonId("CommonId")
                .setMessage("Service implemented")
                .setOperationalState(State.InService)
                .setPublisherName("publisher")
                .setConnectionType(ConnectionType.Service)
                .setServiceAEnd(new ServiceAEndBuilder()
                        .setClli("clli")
                        .setNodeId(new NodeIdType("nodeidtype"))
                        .setServiceFormat(ServiceFormat.Ethernet)
                        .setServiceRate(Uint32.valueOf(100))
                        .setRxDirection(Map.of(new RxDirectionKey(Uint8.ZERO),
                            new RxDirectionBuilder().setIndex(Uint8.ZERO).build()))
                        .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO),
                            new TxDirectionBuilder().setIndex(Uint8.ZERO).build()))
                        .build())
                .setServiceZEnd(new ServiceZEndBuilder()
                        .setClli("clli")
                        .setNodeId(new NodeIdType("nodeidtype"))
                        .setServiceFormat(ServiceFormat.Ethernet)
                        .setServiceRate(Uint32.valueOf(100))
                        .setRxDirection(Map.of(new RxDirectionKey(Uint8.ZERO),
                            new RxDirectionBuilder().setIndex(Uint8.ZERO).build()))
                        .setTxDirection(Map.of(new TxDirectionKey(Uint8.ZERO),
                            new TxDirectionBuilder().setIndex(Uint8.ZERO).build()))
                        .build())
                .build();
        listener.onPublishNotificationProcessService(notification);
        // as onPublishNotificationService is a void method, we check log message to be sure everything went well
        List<ILoggingEvent> logsList = listAppender.list;
        assertEquals("Response received CreatedEvent [serverTimeMs=1, count=1]", logsList.get(1).getFormattedMessage());

    }
}
