/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import com.google.common.util.concurrent.FluentFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.transportpce.servicehandler.ServiceInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev200128.service.path.rpc.result.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsAToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsAToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsZToA;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.PcePathDescriptionElementsZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.LifecycleState;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev181130.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev181130.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.TempServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.Node;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDataStoreOperationsImpl.class);
    private DataBroker dataBroker;

    // This is class is public so that these messages can be accessed from Junit (avoid duplications).
    public static final class LogMessages {

        public static final String SUCCESSFUL_MESSAGE;
        public static final String SERVICE_NOT_FOUND;

        // Static blocks are generated once and spare memory.
        static {
            SUCCESSFUL_MESSAGE = "Successful";
            SERVICE_NOT_FOUND = "Service not found";
        }

        public static String failedTo(String action, String serviceName) {
            return  "Failed to " + action + " service " + serviceName;
        }

        private LogMessages() {
        }
    }


    public ServiceDataStoreOperationsImpl(DataBroker dataBroker) {
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
            InstanceIdentifier<ServiceList> iid = InstanceIdentifier.create(ServiceList.class);
            ServiceList initialRegistry = new ServiceListBuilder().build();
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, initialRegistry);
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
            InstanceIdentifier<TempServiceList> iid = InstanceIdentifier.create(TempServiceList.class);
            TempServiceList initialRegistry = new TempServiceListBuilder().build();
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, initialRegistry);
            FluentFuture<? extends @NonNull CommitInfo> future = transaction.commit();
            future.get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("init failed: ", e);
        }
    }

    @Override
    public Optional<Services> getService(String serviceName) {
        try {
            ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<Services> iid =
                    InstanceIdentifier.create(ServiceList.class).child(Services.class, new ServicesKey(serviceName));
            Future<java.util.Optional<Services>> future =
                    readTx.read(LogicalDatastoreType.OPERATIONAL, iid);
            return future.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Reading service {} failed:", serviceName, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<ServiceList> getServices() {
        // get all services from datastore and return them
        try {
            ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<ServiceList> iid = InstanceIdentifier.create(ServiceList.class);
            Future<java.util.Optional<ServiceList>> future =
                    readTx.read(LogicalDatastoreType.OPERATIONAL, iid);
            return future.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Reading services failed", e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
        .Services> getTempService(String serviceName) {
        try {
            ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class).child(org.opendaylight.yang.gen.v1
                        .http.org.openroadm.service.rev190531.temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                            .ServicesKey(serviceName));
            Future<java.util.Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531
                .temp.service.list.Services>> future =  readTx.read(LogicalDatastoreType.OPERATIONAL, iid);
            return future.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Reading service {} failed:", serviceName, e);
        }
        return Optional.empty();
    }

    @Override
    public OperationResult deleteService(String serviceName) {
        LOG.debug("Deleting '{}' Service", serviceName);
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<Services> iid =
                    InstanceIdentifier.create(ServiceList.class).child(Services.class, new ServicesKey(serviceName));
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
            writeTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("deleteService : {}", LogMessages.failedTo("delete", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("delete", serviceName));
        }
    }

    @Override
    public OperationResult deleteTempService(String commonId) {
        LOG.debug("Deleting '{}' Service", commonId);
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class).child(org.opendaylight.yang.gen.v1
                        .http.org.openroadm.service.rev190531.temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                            .ServicesKey(commonId));
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
            writeTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("deleteTempService : {}", LogMessages.failedTo("delete Temp", commonId), e);
            return OperationResult.failed(LogMessages.failedTo("delete Temp", commonId));
        }
    }

    // to modify service Path elements based on topology update
    @Override
    public OperationResult modifyServiceNM(String serviceName, State operationalState, AdminStates administrativeState,
                                           LifecycleState lifecycleState, List<PcePathDescriptionElementsAToZ> atozList,
                                           List<PcePathDescriptionElementsZToA> ztoaList) {
        LOG.debug("Modifying '{}' Service", serviceName);
        Optional<Services> readService = getService(serviceName);
        if (readService.isPresent()) {
            try {
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                        .child(Services.class, new ServicesKey(serviceName));
                Services services = new ServicesBuilder(readService.get()).setOperationalState(operationalState)
                        .setAdministrativeState(administrativeState)
                        .setLifecycleState(lifecycleState)
                        .setPcePathDescriptionElementsAToZ(atozList)
                        .setPcePathDescriptionElementsZToA(ztoaList)
                        .build();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
                writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                LOG.warn("modifyServiceNM : {}", LogMessages.failedTo("modify", serviceName), e);
                return OperationResult.failed(LogMessages.failedTo("modify", serviceName));
            }
        } else {
            LOG.warn("modifyServiceNM : {}", LogMessages.failedTo("find in DS", serviceName));
            return OperationResult.failed(LogMessages.failedTo("find in DS", serviceName));
        }
    }

    // to modify service Path elements based on topology update
    @Override
    public OperationResult modifyTempServiceNM(String serviceName, State operationalState,
                                               AdminStates administrativeState,
                                               List<PcePathDescriptionElementsAToZ> atozList,
                                               List<PcePathDescriptionElementsZToA> ztoaList) {
        LOG.debug("Modifying '{}' Temp Service", serviceName);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> readService = getTempService(serviceName);
        if (readService.isPresent()) {
            try {
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                        .Services> iid = InstanceIdentifier.create(TempServiceList.class)
                        .child(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                                .Services.class, new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531
                                .temp.service.list.ServicesKey(serviceName));
                org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                        .Services services = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp
                        .service.list.ServicesBuilder(readService.get()).setOperationalState(operationalState)
                        .setAdministrativeState(administrativeState)
                        .setPcePathDescriptionElementsAToZ(atozList)
                        .setPcePathDescriptionElementsZToA(ztoaList)
                        .build();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
                writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                LOG.warn("modifyTempServiceNM : {}", LogMessages.failedTo("modify Temp", serviceName), e);
                return OperationResult.failed(LogMessages.failedTo("modify Temp", serviceName));
            }
        } else {
            LOG.warn("modifyTempServiceNM : {}", LogMessages.failedTo("find in DS", serviceName));
            return OperationResult.failed(LogMessages.failedTo("find in DS", serviceName));
        }
    }

    @Override
    public OperationResult modifyService(String serviceName, State operationalState, AdminStates administrativeState,
                                         LifecycleState lifecycleState) {
        LOG.debug("Modifying '{}' Service", serviceName);
        Optional<Services> readService = getService(serviceName);
        if (!readService.isPresent()) {
            LOG.warn("modifyService: {}", LogMessages.SERVICE_NOT_FOUND);
            return OperationResult.failed(LogMessages.SERVICE_NOT_FOUND);
        }
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceName));
            Services services = new ServicesBuilder(readService.get()).setOperationalState(operationalState)
                    .setAdministrativeState(administrativeState)
                    .setLifecycleState(lifecycleState)
                    .build();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("modifyService : {}", LogMessages.failedTo("modify", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("modify", serviceName));
        }
    }

    @Override
    public OperationResult modifyTempService(String serviceName, State operationalState,
        AdminStates administrativeState) {
        LOG.debug("Modifying '{}' Temp Service", serviceName);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
            .Services> readService = getTempService(serviceName);
        if (!readService.isPresent()) {
            LOG.warn("modifyTempService: {}", LogMessages.SERVICE_NOT_FOUND);
            return OperationResult.failed(LogMessages.SERVICE_NOT_FOUND);
        }
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                            .Services.class, new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531
                                .temp.service.list.ServicesKey(serviceName));
            org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services services = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp
                    .service.list.ServicesBuilder(readService.get()).setOperationalState(operationalState)
                        .setAdministrativeState(administrativeState)
                        .build();
            writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("modifyTempService : {}", LogMessages.failedTo("modify Temp", serviceName), e);
            return OperationResult.failed(LogMessages.failedTo("modify Temp", serviceName));
        }
    }

    @Override
    public OperationResult createService(ServiceCreateInput serviceCreateInput, PathDescription pathDescription) {
        LOG.debug("Writing '{}' Service", serviceCreateInput.getServiceName());
        LOG.info("Path description of service = {}", pathDescription);
        List<AToZ> atozpathdesc = pathDescription.getAToZDirection().getAToZ();
        List<ZToA> ztoapathdesc = pathDescription.getZToADirection().getZToA();
        List<PcePathDescriptionElementsAToZ> atoz = getElemtListAZ(atozpathdesc);
        List<PcePathDescriptionElementsZToA> ztoa = getElemtListZA(ztoapathdesc);
        try {
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceCreateInput.getServiceName()));
            Services service = ModelMappingUtils.mappingServices(serviceCreateInput, null, atoz, ztoa);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("createService : {}", LogMessages.failedTo("create", serviceCreateInput.getServiceName()), e);
            return OperationResult.failed(LogMessages.failedTo("create", serviceCreateInput.getServiceName()));
        }
    }

    @Override
    public OperationResult createTempService(TempServiceCreateInput tempServiceCreateInput,
                                             PathDescription pathDescription) {
        LOG.debug("Writing '{}' Temp Service", tempServiceCreateInput.getCommonId());
        List<AToZ> atozpathdesc = pathDescription.getAToZDirection().getAToZ();
        List<ZToA> ztoapathdesc = pathDescription.getZToADirection().getZToA();
        List<PcePathDescriptionElementsAToZ> atoz = getElemtListAZ(atozpathdesc);
        List<PcePathDescriptionElementsZToA> ztoa = getElemtListZA(ztoapathdesc);
        try {
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                            .Services.class, new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp
                                .service.list.ServicesKey(tempServiceCreateInput.getCommonId()));
            org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.temp.service.list
                .Services service = ModelMappingUtils.mappingServices(tempServiceCreateInput, atoz, ztoa);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("createTempService : {}",
                    LogMessages.failedTo("create Temp", tempServiceCreateInput.getCommonId()), e);
            return OperationResult.failed(LogMessages.failedTo("create Temp",
                    tempServiceCreateInput.getCommonId()));
        }
    }

    @Override
    public OperationResult createServicePath(ServiceInput serviceInput, PathComputationRequestOutput outputFromPce) {
        LOG.debug("Writing '{}' ServicePath ", serviceInput.getServiceName());
        try {
            InstanceIdentifier<ServicePaths> servicePathsIID = InstanceIdentifier.create(ServicePathList.class)
                    .child(ServicePaths.class, new ServicePathsKey(serviceInput.getServiceName()));
            ServicePaths servicePath = ModelMappingUtils.mappingServicePaths(serviceInput, outputFromPce);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, servicePathsIID, servicePath);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(LogMessages.SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            LOG.warn("createServicePath : {}",
                    LogMessages.failedTo("create servicePath", serviceInput.getCommonId()), e);
            return OperationResult.failed(LogMessages.failedTo("create servicePath", serviceInput.getCommonId()));
        }
    }

    @Override
    public OperationResult deleteServicePath(String serviceName) {
        InstanceIdentifier<ServicePaths> servicePathsIID = InstanceIdentifier.create(ServicePathList.class)
                .child(ServicePaths.class, new ServicePathsKey(serviceName));
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
    public String writeOrModifyOrDeleteServiceList(String serviceName, ServiceCreateInput input,
            PathComputationRequestOutput output, int choice) {
        LOG.debug("WriteOrModifyOrDeleting '{}' Service", serviceName);
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        Optional<Services> readService = getService(serviceName);


        /*
         * Write Service.
         */
        if (!readService.isPresent()) {
            if (choice != 2) {
                LOG.warn("writeOrModifyOrDeleteServiceList: {}", LogMessages.SERVICE_NOT_FOUND);
                return LogMessages.SERVICE_NOT_FOUND;
            }

            LOG.debug("Writing '{}' Service", serviceName);
            List<PcePathDescriptionElementsAToZ> atoz = new ArrayList<>();
            List<PcePathDescriptionElementsZToA> ztoa = new ArrayList<>();
            try {
                if (output.getResponseParameters().getPathDescription() != null) {
                    org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.response
                            .parameters.sp.response.parameters.PathDescription pathDescription = output
                            .getResponseParameters().getPathDescription();
                    if (pathDescription.getAToZDirection().getAToZ() != null
                            && pathDescription.getZToADirection().getZToA() != null) {
                        List<AToZ> atozpathdesc = pathDescription.getAToZDirection().getAToZ();
                        List<ZToA> ztoapathdesc = pathDescription.getZToADirection().getZToA();
                        atoz = getElemtListAZ(atozpathdesc);
                        ztoa = getElemtListZA(ztoapathdesc);

                    }
                }
            } catch (NullPointerException e) {
                LOG.warn("Path description is null.", e);
            }

            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceName));
            Services service = ModelMappingUtils.mappingServices(input, null, atoz, ztoa);
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
            try {
                writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                return null;
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                LOG.error("writeOrModifyOrDeleteServiceList : {}", LogMessages.failedTo("create", serviceName), e);
                return LogMessages.failedTo("create", serviceName);
            }
        }

        /*
         * Modify / Delete Service.
         */
        InstanceIdentifier<Services> iid =
                InstanceIdentifier.create(ServiceList.class).child(Services.class, new ServicesKey(serviceName));
        ServicesBuilder service = new ServicesBuilder(readService.get());
        String action = null;
        switch (choice) {
            case 0 : /* Modify. */
                LOG.debug("Modifying '{}' Service", serviceName);
                service.setOperationalState(State.InService).setAdministrativeState(AdminStates.InService);
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, service.build());
                action = "modifyService";
                break;
            case 1 : /* Delete */
                LOG.debug("Deleting '{}' Service", serviceName);
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

    private List<PcePathDescriptionElementsZToA> getElemtListZA(List<ZToA> ztoapathdesc) {
        List<PcePathDescriptionElementsZToA> list = new ArrayList<>();
        String resourceType;
        for (ZToA ztoares:ztoapathdesc) {
            resourceType = ztoares.getResource().getResource().implementedInterface().getSimpleName();
            switch (resourceType) {
                case "TerminationPoint":
                    TerminationPoint tp = (TerminationPoint) ztoares.getResource().getResource();
                    list.add(new PcePathDescriptionElementsZToABuilder()
                            .setElementIdentifier(tp.getTpNodeId() + "-" + tp.getTpId())
                            .setOperationalState(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531
                                    .State.InService).build());
                    break;
                case "Node":
                    Node node = (Node) ztoares.getResource().getResource();
                    list.add(new PcePathDescriptionElementsZToABuilder()
                            .setElementIdentifier(node.getNodeId())
                            .setOperationalState(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531
                                    .State.InService).build());
                    break;
                case "Link":
                    Link link = (Link) ztoares.getResource().getResource();
                    list.add(new PcePathDescriptionElementsZToABuilder()
                            .setElementIdentifier(link.getLinkId())
                            .setOperationalState(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531
                                    .State.InService).build());
                    break;
                default:
                    LOG.warn("Resource type not defined {}", resourceType);
            }
        }
        return list;
    }

    private List<PcePathDescriptionElementsAToZ> getElemtListAZ(List<AToZ> atozpathdesc) {
        List<PcePathDescriptionElementsAToZ> list = new ArrayList<>();
        String resourceType;
        for (AToZ atozres:atozpathdesc) {
            resourceType = atozres.getResource().getResource().implementedInterface().getSimpleName();
            switch (resourceType) {
                case "TerminationPoint":
                    TerminationPoint tp = (TerminationPoint) atozres.getResource().getResource();
                    list.add(new PcePathDescriptionElementsAToZBuilder()
                            .setElementIdentifier(tp.getTpNodeId() + "-" + tp.getTpId())
                            .setOperationalState(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531
                                    .State.InService).build());
                    break;
                case "Node":
                    Node node = (Node) atozres.getResource().getResource();
                    list.add(new PcePathDescriptionElementsAToZBuilder()
                            .setElementIdentifier(node.getNodeId())
                            .setOperationalState(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531
                                    .State.InService).build());
                    break;
                case "Link":
                    Link link = (Link) atozres.getResource().getResource();
                    list.add(new PcePathDescriptionElementsAToZBuilder()
                            .setElementIdentifier(link.getLinkId())
                            .setOperationalState(org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev190531
                                    .State.InService).build());
                    break;
                default:
                    LOG.warn("Resource type not defined {}", resourceType);
            }
        }
        return list;
    }
}
