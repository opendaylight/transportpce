/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.utils;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.PathTopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.ServicePathNotificationTypes;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MockedNotificationServiceWrapper {

    private static final Logger LOG = LoggerFactory.getLogger(MockedNotificationServiceWrapper.class);
    private final Notification publishedNotification;
    private final NotificationPublishService notificationPublishService;
    private Boolean rendererFailed = false;

    public MockedNotificationServiceWrapper(NotificationPublishService notificationPublishService) {
        this.publishedNotification = null;
        this.notificationPublishService = notificationPublishService;
    }

    public NotificationPublishService getMockedNotificationService() throws InterruptedException {
        final NotificationPublishService mockedNotificationService = mock(NotificationPublishService.class);
        doAnswer(invocation -> {
            final Object notif = invocation.getArguments()[0];
            LOG.info("notif received : {}", notif);
            assertTrue(Notification.class.isAssignableFrom(notif.getClass()));
            if (this.rendererFailed) {
                LOG.info("putting failed renderer notification");
                RendererRpcResultSp serviceRpcResultSp = new RendererRpcResultSpBuilder()
                        .setNotificationType(ServicePathNotificationTypes.ServiceImplementationRequest)
                        .setServiceName("service 1").setStatus(RpcStatusEx.Failed).setStatusMessage("Renderer Failed")
                        .setPathTopology(new PathTopologyBuilder().build()).build();
                MockedNotificationServiceWrapper.this.notificationPublishService.putNotification(serviceRpcResultSp);
                this.rendererFailed = false;
            } else {
                MockedNotificationServiceWrapper.this.notificationPublishService.putNotification((Notification) notif);
            }
            return null;
        }).when(mockedNotificationService).putNotification(any(Notification.class));
        return mockedNotificationService;
    }

    public void setRendererFailed() {
        this.rendererFailed = true;
    }
}
