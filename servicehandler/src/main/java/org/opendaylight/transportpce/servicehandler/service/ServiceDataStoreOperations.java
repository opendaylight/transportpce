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
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;

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
     *     unique name of the service
     * @return Optional of Services
     */
    Optional<Services> getService(String serviceName);

    /**
     * get all OR services.
     *
     * @return Optional of Services
     */
    Optional<ServiceList> getServices();

    /**
     * get temp service by common-id.
     *
     * @param commonId
     *     unique common-id of the service
     * @return Optional of Services
     */
    Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
        .Services> getTempService(String commonId);

    /**
     * deleteService service by name.
     *
     * @param serviceName
     *     unique name of the service
     * @return result of Delete operation
     */
    OperationResult deleteService(String serviceName);

    /**
     * deleteService service by common-id.
     *
     * @param commonId
     *     unique common-id of the service
     * @return result of Delete operation
     */
    OperationResult deleteTempService(String commonId);

    /**
     * modifyService service attributes.
     *
     * @param serviceName
     *     unique name of the service
     * @param operationalState
     *     operational state of service
     * @param administrativeState
     *     administrative state of service
     * @return result of modifyService operation
     */
    OperationResult modifyService(String serviceName, State operationalState, AdminStates administrativeState);

    /**
     * modify Temp Service.
     *
     * @param commonId unique common-id of the service
     * @param operationalState operational state of service
     * @param administrativeState administrative state of service
     * @return result of modifyTempService operation
     */
    OperationResult modifyTempService(String commonId, State operationalState, AdminStates administrativeState);

    /**
     * create new service entry.
     *
     * @param serviceCreateInput serviceCreateInput data for creation of service
     * @return result of createService operation
     */
    OperationResult createService(ServiceCreateInput serviceCreateInput);

    Optional<ServicePaths> getServicePath(String serviceName);

    /**
     * create new servicePath entry.
     *
     * @param serviceInput
     *     ServiceInput data for creation of service
     * @param outputFromPce
     *     output from pce request which is used as input for creating of service.
     * @return result of createServicePath operation
     */
    OperationResult createServicePath(ServiceInput serviceInput, PathComputationRequestOutput outputFromPce);

    /**
     * create new Temp service entry.
     *
     * @param tempServiceCreateInput tempServiceCreateInput data for creation of
     *                               service
     * @return result of createTempService operation
     */
    OperationResult createTempService(TempServiceCreateInput tempServiceCreateInput);

    Optional<ServicePathList> getServicePaths();

    OperationResult modifyServicePath(PathDescription pathDescription, String serviceName);

    /**
     * deleteServicePath by name.
     *
     * @param serviceName
     *     unique name of the service
     * @return result of Delete operation
     */
    OperationResult deleteServicePath(String serviceName);

    /**
     * All actions (createService|modifyService|deleteService) combined.
     * This method exists only for backwards compatibility. It will be deleted once refactoring is done.
     *
     * @param serviceName
     *     unique name of the service
     * @param input ServiceCreateInput data
     * @param output PathComputationRequestOutput data
     * @param choice 0:modify, 1:delete, 2:write
     * @return result of createService, deleteService, or modifyService operation
     */
    @Deprecated
    String writeOrModifyOrDeleteServiceList(String serviceName, ServiceCreateInput input,
                                            PathComputationRequestOutput output, int choice);

}
