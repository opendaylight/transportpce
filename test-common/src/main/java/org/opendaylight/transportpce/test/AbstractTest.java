/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.transportpce.test.common.DataStoreContext;
import org.opendaylight.transportpce.test.common.DataStoreContextImpl;

public abstract class AbstractTest {

    private final DataStoreContext dataStoreContextUtil;

    protected AbstractTest() {
        dataStoreContextUtil = new DataStoreContextImpl();
    }

    public DataBroker getDataBroker() {
        return dataStoreContextUtil.getDataBroker();
    }

    public DOMDataBroker getDOMDataBroker() {
        return dataStoreContextUtil.getDOMDataBroker();
    }

    public DataStoreContext getDataStoreContextUtil() {
        return dataStoreContextUtil;
    }

    public NotificationPublishService getNotificationPublishService() {
        return dataStoreContextUtil.getNotificationPublishService();
    }

    public NotificationService getNotificationService() {
        return dataStoreContextUtil.getNotificationService();
    }

}
