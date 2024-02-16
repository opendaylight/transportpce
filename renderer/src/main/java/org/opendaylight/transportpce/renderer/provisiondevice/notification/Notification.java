/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.notification;

import java.util.Set;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.renderer.rpc.result.sp.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.RpcStatusEx;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.ServicePathNotificationTypes;

public interface Notification {

    /**
     * Send renderer notification.
     */
    void send(
        ServicePathNotificationTypes servicePathNotificationTypes,
        String serviceName,
        RpcStatusEx rpcStatusEx,
        String message
    ) ;

    /**
     * Send renderer notification.
     */
    void send(org.opendaylight.yangtools.yang.binding.Notification notification);

    /**
     * Build notification containing path description information.
     *
     * @param servicePathNotificationTypes ServicePathNotificationTypes
     * @param serviceName String
     * @param rpcStatusEx RpcStatusEx
     * @param message String
     * @param pathDescription PathDescription
     * @return notification with RendererRpcResultSp type.
     */
    RendererRpcResultSp buildNotification(
        ServicePathNotificationTypes servicePathNotificationTypes,
        String serviceName,
        RpcStatusEx rpcStatusEx,
        String message,
        PathDescription pathDescription,
        Link notifLink,
        Set<String> supportedLinks,
        String serviceType);

}
