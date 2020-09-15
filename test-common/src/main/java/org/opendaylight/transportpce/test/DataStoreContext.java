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
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.dom.api.DOMDataBroker;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;

public interface DataStoreContext {

    DataBroker getDataBroker();

    DOMDataBroker getDOMDataBroker();

    NotificationService createNotificationService();

    NotificationPublishService createNotificationPublishService();

    EffectiveModelContext getSchemaContext();

    BindingNormalizedNodeSerializer getBindingToNormalizedNodeCodec();

    NotificationService getNotificationService();

    NotificationPublishService getNotificationPublishService();

}
