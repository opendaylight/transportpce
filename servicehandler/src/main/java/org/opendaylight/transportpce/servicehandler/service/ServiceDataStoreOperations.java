/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import java.util.Optional;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev171017.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;

/**
 * OpenROADM Service operations API providing basic operations on services.
 */
public interface ServiceDataStoreOperations {

    /**
     * initialize services DataStore.
     */
    void initialize();

    /**
     * get service by name.
     *
     * @param serviceName
     *   unique name of the service
     * @return Optional of Services
     */
    Optional<Services> getService(String serviceName);

    /**
     * deleteService service by name.
     *
     * @param serviceName
     *   unique name of the service
     * @return result of Delete operation
     */
    OperationResult deleteService(String serviceName);

    /**
     * modifyService service attributes.
     *
     * @param serviceName
     *   unique name of the service
     * @param operationalState
     *   operational state of service
     * @param administrativeState
     *   administrative state of service
     * @return result of modifyService operation
     */
    OperationResult modifyService(String serviceName, State operationalState, State administrativeState);

    /**
     * create new service entry.
     *
     * @param serviceCreateInput
     *   serviceCreateInput data for creation of service
     * @param outputFromPce
     *   output from pce request which is used as input for creating of service.
     * @return result of createService operation
     */
    OperationResult createService(ServiceCreateInput serviceCreateInput, PathComputationRequestOutput outputFromPce);

    /**
     * create new servicePath entry.
     *
     * @param serviceCreateInput
     *   serviceCreateInput data for creation of service
     * @param outputFromPce
     *   output from pce request which is used as input for creating of service.
     * @return result of createServicePath operation
     */
    OperationResult createServicePath(ServiceCreateInput serviceCreateInput,
        PathComputationRequestOutput outputFromPce);

    /**
     * deleteServicePath by name.
     *
     * @param serviceName
     *   unique name of the service
     * @return result of Delete operation
     */
    OperationResult deleteServicePath(String serviceName);

    /**
     * All actions (createService|modifyService|deleteService) combined.
     * This method exists only for backwards compatibility. It will be deleted once refactoring is done.
     *
     * @param serviceName
     *   unique name of the service
     * @param input ServiceCreateInput data
     * @param output PathComputationRequestOutput data
     * @param choice 0:modify, 1:delete, 2:write
     * @return result of createService, deleteService, or modifyService operation
     */
    @Deprecated
    String writeOrModifyOrDeleteServiceList(String serviceName, ServiceCreateInput input,
                                            PathComputationRequestOutput output, int choice);

}
