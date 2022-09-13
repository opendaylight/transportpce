/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.utils;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationPublishServiceMock implements NotificationPublishService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationPublishServiceMock.class);

    @Override
    public void putNotification(Notification<?> notification) throws InterruptedException {
        LOG.info("putNotification");
    }

    @Override
    public ListenableFuture<?> offerNotification(Notification<?> notification) {
        LOG.info("offerNotification");
        throw new UnsupportedOperationException("offerNotification is not implemented");
    }

    @Override
    public ListenableFuture<?> offerNotification(Notification<?> notification, int timeout, TimeUnit unit)
            throws InterruptedException {
        LOG.info("offerNotification");
        throw new UnsupportedOperationException("offerNotification is not implemented");
    }

}
