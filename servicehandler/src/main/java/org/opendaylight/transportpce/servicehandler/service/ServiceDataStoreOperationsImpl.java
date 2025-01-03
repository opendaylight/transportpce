/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import com.google.common.util.concurrent.FluentFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.TempServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev220118.service.path.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true)
public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDataStoreOperationsImpl.class);
    private static final String CREATE_MSG = "create";
    private static final String DELETING_SERVICE_MSG = "Deleting '{}' Service";
    private DataBroker dataBroker;

    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        public static final String SUCCESSFUL_MESSAGE;
        public static final String SERVICE_NOT_FOUND;
        public static final String SERVICE_PATH_NOT_FOUND;

        // Static blocks are generated once and spare memory.
        static {
            SUCCESSFUL_MESSAGE = "Successful";
            SERVICE_NOT_FOUND = "Service not found";
            SERVICE_PATH_NOT_FOUND = "Service path not found";
        }

        public static String failedTo(String action, String serviceName) {
            return  "Failed to " + action + " service " + serviceName;
        }

        private LogMessages() {
        }
    }


    @Activate
    public ServiceDataStoreOperationsImpl(@Reference DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }

    @Override
    public void initialize() {
        initializeServiceList();
        initializeTempServiceList();
    }

    private void initializeServiceList() {
        try {
            LOG.info("initializing service registry");
            WriteTransaction transaction = this.dataBroker.newWriteOnlyTransaction();
            transaction.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServiceList.class).build(),
                new ServiceListBuilder().build());
            FluentFuture<? extends @NonNull CommitInfo> future = transaction.commit();
            future.get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("init failed: ", e);
        }
    }

    private void initializeTempServiceList() {
        try {
            LOG.info("initializing temp service registry");
            WriteTransaction transaction = this.dataBroker.newWriteOnlyTransaction();
            transaction.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(TempServiceList.class).build(),
                new TempServiceListBuilder().build());
            FluentFuture<? extends @NonNull CommitInfo> future = transaction.commit();
            future.get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("init failed: ", e);
        }
    }

    @Override
    public Optional<Services> getService(String serviceName) {
        try {
            return this.dataBroker.newReadOnlyTransaction()
                    .read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(ServiceList.class)
                            .child(Services.class, new ServicesKey(serviceName))
                            .build()
                        )
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Reading service {} failed:", serviceName, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ServiceList> getServices() {
        try {
            return this.dataBroker.newReadOnlyTransaction()
                    .read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(ServiceList.class).build())
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Reading services failed:", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
            .temp.service.list.Services> getTempService(String serviceName) {
        try {
            return this.dataBroker.newReadOnlyTransaction()
                    .read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(TempServiceList.class)
                            .child(
                                org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                                    .temp.service.list.Services.class,
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                                    .temp.service.list.ServicesKey(serviceName))
                            .build())
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Reading service {} failed:", serviceName, e);
        }
        return Optional.empty();
    }

    @Override
    public OperationResult deleteService(String serviceName) {
        LOG.debug(DELETING_SERVICE_MSG, serviceName);
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.delete(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceName))
                    .build());
            writeTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("deleteService : {}", LogMessages.failedTo("delete", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("delete", serviceName));
        }
    }

    @Override
    public OperationResult deleteTempService(String commonId) {
        LOG.debug(DELETING_SERVICE_MSG, commonId);
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.delete(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(TempServiceList.class)
                    .child(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                            .temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                            .temp.service.list.ServicesKey(commonId))
                    .build());
            writeTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("deleteTempService : {}", LogMessages.failedTo("delete Temp", commonId), e);
            return OperationResult.failed(LogMessages.failedTo("delete Temp", commonId));
        }
    }

    @Override
    public OperationResult modifyService(String serviceName, State operationalState, AdminStates administrativeState) {
        LOG.debug("Modifying '{}' Service", serviceName);
        Optional<Services> readService = getService(serviceName);
        if (readService.isEmpty()) {
            LOG.warn("modifyService: {}", LogMessages.SERVICE_NOT_FOUND);
            return OperationResult.failed(LogMessages.SERVICE_NOT_FOUND);
        }
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceName))
                    .build(),
                new ServicesBuilder(readService.orElseThrow())
                    .setOperationalState(operationalState)
                    .setAdministrativeState(administrativeState)
                    .build());
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("modifyService : {}", LogMessages.failedTo("modify", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("modify", serviceName));
        }
    }

    @Override
    public OperationResult modifyTempService(
            String serviceName, State operationalState, AdminStates administrativeState) {
        LOG.debug("Modifying '{}' Temp Service", serviceName);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                .temp.service.list.Services> readService = getTempService(serviceName);
        if (readService.isEmpty()) {
            LOG.warn("modifyTempService: {}", LogMessages.SERVICE_NOT_FOUND);
            return OperationResult.failed(LogMessages.SERVICE_NOT_FOUND);
        }
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(TempServiceList.class)
                    .child(
                        org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                            .temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                            .temp.service.list.ServicesKey(serviceName))
                    .build(),
                new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                        .temp.service.list.ServicesBuilder(readService.orElseThrow())
                    .setOperationalState(operationalState)
                    .setAdministrativeState(administrativeState)
                    .build());
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("modifyTempService : {}", LogMessages.failedTo("modify Temp", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("modify Temp", serviceName));
        }
    }

    @Override
    public OperationResult createService(ServiceCreateInput serviceCreateInput) {
        LOG.debug("Writing '{}' Service", serviceCreateInput.getServiceName());
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceCreateInput.getServiceName()))
                    .build(),
                ModelMappingUtils.mappingServices(serviceCreateInput, null));
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("createService : {}", LogMessages.failedTo(CREATE_MSG, serviceCreateInput.getServiceName()), e);
            return OperationResult.failed(LogMessages.failedTo(CREATE_MSG, serviceCreateInput.getServiceName()));
        }
    }

    @Override
    public OperationResult createTempService(
            TempServiceCreateInput tempServiceCreateInput,
            org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205
                    .service.path.rpc.result.PathDescription pathDescription) {
        LOG.debug("Writing '{}' Temp Service", tempServiceCreateInput.getCommonId());
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(TempServiceList.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                            .temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526
                            .temp.service.list.ServicesKey(tempServiceCreateInput.getCommonId()))
                    .build(),
                ModelMappingUtils.mappingServices(tempServiceCreateInput, pathDescription));
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("createTempService : {}",
                    LogMessages.failedTo("create Temp", tempServiceCreateInput.getCommonId()), e);
            return OperationResult.failed(LogMessages.failedTo("create Temp", tempServiceCreateInput.getCommonId()));
        }
    }

    @Override
    public Optional<ServicePathList> getServicePaths() {
        LOG.debug("Retrieving list of ServicePath...");
        try {
            return this.dataBroker.newReadOnlyTransaction()
                    .read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(ServicePathList.class).build())
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Reading service path list failed. Error={}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public Optional<ServicePaths> getServicePath(String serviceName) {
        LOG.debug("Retrieving service path of service {}", serviceName);
        try {
            return this.dataBroker.newReadOnlyTransaction()
                    .read(
                        LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(ServicePathList.class)
                            .child(ServicePaths.class, new ServicePathsKey(serviceName))
                            .build())
                    .get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Reading service path failed. Error={}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public OperationResult createServicePath(ServiceInput serviceInput, PathComputationRequestOutput outputFromPce) {
        LOG.debug("Writing '{}' ServicePath ", serviceInput.getServiceName());
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServicePathList.class)
                    .child(ServicePaths.class, new ServicePathsKey(serviceInput.getServiceName()))
                    .build(),
                ModelMappingUtils.mappingServicePaths(serviceInput, outputFromPce));
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("createServicePath : {}",
                    LogMessages.failedTo("create servicePath", serviceInput.getCommonId()), e);
            return OperationResult.failed(LogMessages.failedTo("create servicePath", serviceInput.getCommonId()));
        }
    }

    @Override
    public OperationResult modifyServicePath(PathDescription pathDescription, String serviceName) {
        LOG.debug("Updating servicePath because of a change in the openroadm-topology");
        Optional<ServicePaths> readServicePath = getServicePath(serviceName);
        if (readServicePath.isEmpty()) {
            LOG.warn("modifyServicePath: {}", LogMessages.SERVICE_PATH_NOT_FOUND);
            return OperationResult.failed(LogMessages.SERVICE_PATH_NOT_FOUND);
        }
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.merge(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServicePathList.class)
                    .child(ServicePaths.class, new ServicePathsKey(serviceName))
                    .build(),
                new ServicePathsBuilder()
                    .setServiceAEnd(readServicePath.orElseThrow().getServiceAEnd())
                    .setServiceHandlerHeader(readServicePath.orElseThrow().getServiceHandlerHeader())
                    .setServicePathName(readServicePath.orElseThrow().getServicePathName())
                    .setServiceZEnd(readServicePath.orElseThrow().getServiceZEnd())
                    .setSupportingServiceName(readServicePath.orElseThrow().getSupportingServiceName())
                    .setEquipmentSrgs(readServicePath.orElseThrow().getEquipmentSrgs())
                    .setFiberSpanSrlgs(readServicePath.orElseThrow().getFiberSpanSrlgs())
                    .setHardConstraints(readServicePath.orElseThrow().getHardConstraints())
                    .setLatency(readServicePath.orElseThrow().getLatency())
                    .setPathDescription(pathDescription)
                    .setPceRoutingMetric(readServicePath.orElseThrow().getPceRoutingMetric())
                    .setSoftConstraints(readServicePath.orElseThrow().getSoftConstraints())
                    .build());
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("modifyServicePath : {}", LogMessages.failedTo("modify service path", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("modify service path", serviceName));
        }
    }

    @Override
    public OperationResult deleteServicePath(String serviceName) {
        DataObjectIdentifier<ServicePaths> servicePathsIID = DataObjectIdentifier.builder(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName))
                .build();
        LOG.debug("Deleting service from {}", servicePathsIID);
        WriteTransaction servicePathsWriteTx = this.dataBroker.newWriteOnlyTransaction();
        servicePathsWriteTx.delete(LogicalDatastoreType.OPERATIONAL, servicePathsIID);
        try {
            servicePathsWriteTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("deleteServicePath : {}", LogMessages.failedTo("delete servicePath", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("delete servicePath", serviceName));
        }
    }

    /*
     * Write or Modify or Delete Service from/to SreviceList.
     *
     * @param serviceName Name of service
     *
     * @param input ServiceCreateInput
     *
     * @param output PathComputationRequestOutput
     *
     * @param choice 0 - Modify 1 - Delete 2 - Write
     *
     * @return String operations result, null if ok or not otherwise
     */
    @Deprecated
    @Override
    public String writeOrModifyOrDeleteServiceList(
            String serviceName, ServiceCreateInput input, PathComputationRequestOutput output, int choice) {
        LOG.debug("WriteOrModifyOrDeleting '{}' Service", serviceName);
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        Optional<Services> readService = getService(serviceName);
        /*
         * Write Service.
         */
        if (readService.isEmpty()) {
            if (choice != 2) {
                LOG.warn("writeOrModifyOrDeleteServiceList: {}", LogMessages.SERVICE_NOT_FOUND);
                return LogMessages.SERVICE_NOT_FOUND;
            }
            LOG.debug("Writing '{}' Service", serviceName);
            writeTx.put(
                LogicalDatastoreType.OPERATIONAL,
                DataObjectIdentifier.builder(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceName))
                    .build(),
                ModelMappingUtils.mappingServices(input, null));
            try {
                writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                return null;
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                LOG.error("writeOrModifyOrDeleteServiceList : {}", LogMessages.failedTo(CREATE_MSG, serviceName), e);
                return LogMessages.failedTo(CREATE_MSG, serviceName);
            }
        }

        /*
         * Modify / Delete Service.
         */
        DataObjectIdentifier<Services> iid = DataObjectIdentifier.builder(ServiceList.class)
                .child(Services.class, new ServicesKey(serviceName))
                .build();
        String action = null;
        switch (choice) {
            case 0 : /* Modify. */
                LOG.debug("Modifying '{}' Service", serviceName);
                writeTx.merge(
                    LogicalDatastoreType.OPERATIONAL,
                    iid,
                    new ServicesBuilder(readService.orElseThrow())
                        .setOperationalState(State.InService)
                        .setAdministrativeState(AdminStates.InService)
                        .build());
                action = "modifyService";
                break;
            case 1 : /* Delete */
                LOG.debug(DELETING_SERVICE_MSG, serviceName);
                writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                action = "deleteService";
                break;
            default:
                LOG.debug("No choice found");
                break;
        }
        try {
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("writeOrModifyOrDeleteServiceList : {}", LogMessages.failedTo(action, serviceName), e);
            return LogMessages.failedTo(action, serviceName);
        }

        return null;
    }
}
