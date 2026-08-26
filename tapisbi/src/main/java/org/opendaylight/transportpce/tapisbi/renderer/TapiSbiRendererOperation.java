/*
 * Copyright © 2026 Orange and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.renderer;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapisbi.rev260410.TapiSbiServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.service.list.Services;

public interface TapiSbiRendererOperation {

    ListenableFuture<TapiSbiServiceImplementationRequestOutput> serviceImplementation(
            TapiSbiServiceImplementationRequestInput input, boolean isTempService);

    ListenableFuture<TapiSbiServiceDeleteOutput> serviceDelete(TapiSbiServiceDeleteInput input, Services service);
}
