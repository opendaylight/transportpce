/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.fd.honeycomb.transportpce.test.common;

import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface DataStoreContext {

//    DataBroker getDataBroker();
//
//    DOMDataBroker getDOMDataBroker();
//
//    NotificationService createNotificationService();
//
//    NotificationPublishService createNotificationPublishService();

    SchemaContext getSchemaContext();

    BindingNormalizedNodeCodecRegistry getBindingToNormalizedNodeCodec();

//    NotificationService getNotificationService();
//
//    NotificationPublishService getNotificationPublishService();

}
