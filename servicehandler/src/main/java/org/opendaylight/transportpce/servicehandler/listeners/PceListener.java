/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.listeners;

import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.transportpce.servicehandler.service.ServiceDataStoreOperations;

public interface PceListener {

    void setInput(ServiceInput serviceInput);

    void setServiceReconfigure(Boolean serv);

    void setserviceDataStoreOperations(ServiceDataStoreOperations serviceData);

    void setTempService(Boolean tempService);

    void setServiceFeasiblity(Boolean serviceFeasiblity);
}
