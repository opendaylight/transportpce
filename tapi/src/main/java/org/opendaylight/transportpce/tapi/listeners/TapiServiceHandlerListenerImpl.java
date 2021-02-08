/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.ServiceRpcResultSh;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.TransportpceServicehandlerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiServiceHandlerListenerImpl implements TransportpceServicehandlerListener {

    private static final Logger LOG = LoggerFactory.getLogger(TapiServiceHandlerListenerImpl.class);
    private final DataBroker dataBroker;

    public TapiServiceHandlerListenerImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;

    }

    @Override
    public void onServiceRpcResultSh(ServiceRpcResultSh notification) {
        LOG.info("Avoid dataBroker error {}", dataBroker.getClass().getCanonicalName());
    }
}
