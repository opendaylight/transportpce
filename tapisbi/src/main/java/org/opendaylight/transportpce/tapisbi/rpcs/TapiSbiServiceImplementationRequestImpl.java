/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.rpcs;

import com.google.common.util.concurrent.ListenableFuture;

import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapisbi.listener.TapiSbiRendererNotificationHandler;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TapiSbiServiceImplementationRequestImpl implements TapiSbiServiceImplementationRequest {
    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiServiceImplementationRequestImpl.class);
    private final TapiSbiRendererNotificationHandler sbiRendererListener;
    private final TapiPceNotificationHandler sbiPceListener;

    public TapiSbiServiceImplementationRequestImpl(TapiSbiRendererNotificationHandler rendererListener,
                TapiPceNotificationHandler pceListener) {
        this.sbiRendererListener = rendererListener;
        this.sbiPceListener = pceListener;

    }

    @Override
    public ListenableFuture<RpcResult<TapiSbiServiceImplementationRequestOutput>> invoke(
            TapiSbiServiceImplementationRequestInput input) {
        String serviceName = input.getServiceName();
        LOG.info("Calling RPC service impl request {}", serviceName);
     // TODO: provide implementation of this RPC
        LOG.debug("TapiSbiServiceDelete invoked leveraging {} and {}",
            sbiRendererListener.getClass(), sbiPceListener.getClass());

        return null;
    }

}