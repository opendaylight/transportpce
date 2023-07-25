/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import com.google.common.util.concurrent.ListenableFuture;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev230725.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev230725.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev230725.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev230725.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev211210.service.list.Services;

public interface RendererServiceOperations {

    ListenableFuture<ServiceImplementationRequestOutput> serviceImplementation(ServiceImplementationRequestInput input,
                                                                               boolean isTempService);

    ListenableFuture<ServiceDeleteOutput> serviceDelete(ServiceDeleteInput input, Services service);
}
