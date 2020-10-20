/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.NotificationPublishService;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;

public abstract class AbstractTest {

    private static DataStoreContext dataStoreContextUtil = new DataStoreContextImpl();

    public static DataBroker getDataBroker() {
        return dataStoreContextUtil.getDataBroker();
    }

    public DataBroker getNewDataBroker() {
        DataStoreContext dataStoreContext = new DataStoreContextImpl();
        return dataStoreContext.getDataBroker();
    }

    public DOMDataBroker getDOMDataBroker() {
        return dataStoreContextUtil.getDOMDataBroker();
    }

    public static DataStoreContext getDataStoreContextUtil() {
        return dataStoreContextUtil;
    }

    public NotificationPublishService getNotificationPublishService() {
        return dataStoreContextUtil.getNotificationPublishService();
    }

    public NotificationService getNotificationService() {
        return dataStoreContextUtil.getNotificationService();
    }

}
