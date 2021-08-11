/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.resource.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.Lgx;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.Port;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.PublishNotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.process.service.ServiceAEnd;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev211013.notification.process.service.ServiceZEnd;

//This class is a temporary workaround while waiting jackson
//support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
public class PublishNotificationProcessServiceModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public PublishNotificationProcessServiceModule() {
        super(PackageVersion.VERSION);
        addSerializer(PublishNotificationProcessService.class, new PublishNotificationProcessServiceSerializer());
        addSerializer(Lgx.class, new LgxSerializer());
        addSerializer(Port.class, new PortSerializer());
        addSerializer(RxDirection.class, new RxDirectionSerializer());
        addSerializer(TxDirection.class, new TxDirectionSerializer());
        addSerializer(ServiceAEnd.class, new ServiceAEndSerializer());
        addSerializer(ServiceZEnd.class, new ServiceZEndSerializer());
    }

}
