/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.common;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.NotificationService;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

public interface DataStoreContext {

    DataBroker getDataBroker();

    DOMDataBroker getDOMDataBroker();

    NotificationService createNotificationService();

    NotificationPublishService createNotificationPublishService();

    SchemaContext getSchemaContext();

    BindingNormalizedNodeCodecRegistry getBindingToNormalizedNodeCodec();

    NotificationService getNotificationService();

    NotificationPublishService getNotificationPublishService();

}
