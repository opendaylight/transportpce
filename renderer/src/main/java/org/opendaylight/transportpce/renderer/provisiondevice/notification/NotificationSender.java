/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.notification;

import java.util.Set;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSpBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class NotificationSender implements Notification {

    private final NotificationPublishService notificationPublishService;

    private static final Logger LOG = LoggerFactory.getLogger(NotificationSender.class);

    @Activate
    public NotificationSender(@Reference NotificationPublishService notificationPublishService) {
        this.notificationPublishService = notificationPublishService;
    }

    /**
     * Send renderer notification.
     */
    public void send(ServicePathNotificationTypes servicePathNotificationTypes,
                     String serviceName,
                     RpcStatusEx rpcStatusEx,
                     String message) {

        send(
            buildNotification(
                servicePathNotificationTypes,
                serviceName,
                rpcStatusEx,
                message,
                null,
                null,
                null,
                null
            )
        );
    }

    /**
     * Send renderer notification.
     * @param notification Notification
     */
    public void send(org.opendaylight.yangtools.yang.binding.Notification notification) {
        try {
            LOG.info("Sending notification {}", notification);
            notificationPublishService.putNotification(notification);
        } catch (InterruptedException e) {
            LOG.info("notification offer rejected: ", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Build notification containing path description information.
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     * @param pathDescription PathDescription
     * @return notification with RendererRpcResultSp type.
     */
    @Override
    public RendererRpcResultSp buildNotification(
            ServicePathNotificationTypes servicePathNotificationTypes,
            String serviceName,
            RpcStatusEx rpcStatusEx,
            String message,
            PathDescription pathDescription,
            Link notifLink,
            Set<String> supportedLinks,
            String serviceType) {

        RendererRpcResultSpBuilder builder =
            new RendererRpcResultSpBuilder()
                .setNotificationType(servicePathNotificationTypes).setServiceName(serviceName).setStatus(rpcStatusEx)
                .setStatusMessage(message)
                .setServiceType(serviceType);
        if (pathDescription != null) {
            builder
                .setAToZDirection(pathDescription.getAToZDirection())
                .setZToADirection(pathDescription.getZToADirection());
        }
        if (notifLink != null) {
            builder.setLink(notifLink);
        }
        if (supportedLinks != null) {
            builder.setLinkId(supportedLinks);
        }
        return builder.build();
    }
}
