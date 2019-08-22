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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev190624.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.TempServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePathsKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceDataStoreOperationsImpl implements ServiceDataStoreOperations {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceDataStoreOperationsImpl.class);
    private static final String SUCCESSFUL_MESSAGE = "Successful";
    private DataBroker dataBroker;

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
            LOG.warn("init failed: {}", e.getMessage());
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
            LOG.warn("init failed: {}", e.getMessage());
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
    public Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
        .Services> getTempService(String serviceName) {
        try {
            ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class).child(org.opendaylight.yang.gen.v1
                        .http.org.openroadm.service.rev161014.temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                            .ServicesKey(serviceName));
            Future<java.util.Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014
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
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to delete service " + serviceName + " from Service List";
            LOG.warn(message, e);
            return OperationResult.failed(message);
        }
    }

    @Override
    public OperationResult deleteTempService(String commonId) {
        LOG.debug("Deleting '{}' Service", commonId);
        try {
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class).child(org.opendaylight.yang.gen.v1
                        .http.org.openroadm.service.rev161014.temp.service.list.Services.class,
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                            .ServicesKey(commonId));
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
            writeTx.commit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to delete service " + commonId + " from Service List";
            LOG.warn(message, e);
            return OperationResult.failed(message);
        }
    }

    @Override
    public OperationResult modifyService(String serviceName, State operationalState, State administrativeState) {
        LOG.debug("Modifying '{}' Service", serviceName);
        Optional<Services> readService = getService(serviceName);
        if (readService.isPresent()) {
            try {
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                        .child(Services.class, new ServicesKey(serviceName));
                Services services = new ServicesBuilder(readService.get()).setOperationalState(operationalState)
                        .setAdministrativeState(administrativeState)
                        .build();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
                writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                return OperationResult.ok(SUCCESSFUL_MESSAGE);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                String message = "Failed to modify service " + serviceName + " from Service List";
                LOG.warn(message, e);
                return OperationResult.failed(message);
            }
        } else {
            String message = "Service " + serviceName + " is not present!";
            LOG.warn(message);
            return OperationResult.failed(message);
        }
    }

    @Override
    public OperationResult modifyTempService(String serviceName, State operationalState, State administrativeState) {
        LOG.debug("Modifying '{}' Temp Service", serviceName);
        Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
            .Services> readService = getTempService(serviceName);
        if (readService.isPresent()) {
            try {
                WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
                InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                    .Services> iid = InstanceIdentifier.create(TempServiceList.class)
                        .child(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                                .Services.class, new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014
                                    .temp.service.list.ServicesKey(serviceName));
                org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                    .Services services = new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp
                        .service.list.ServicesBuilder(readService.get()).setOperationalState(operationalState)
                            .setAdministrativeState(administrativeState)
                            .build();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
                writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                return OperationResult.ok(SUCCESSFUL_MESSAGE);
            } catch (TimeoutException | InterruptedException | ExecutionException e) {
                String message = "Failed to modify temp service " + serviceName + " from Temp Service List";
                LOG.warn(message, e);
                return OperationResult.failed(message);
            }
        } else {
            String message = "Temp Service " + serviceName + " is not present!";
            LOG.warn(message);
            return OperationResult.failed(message);
        }
    }

    @Override
    public OperationResult createService(ServiceCreateInput serviceCreateInput) {
        LOG.debug("Writing '{}' Service", serviceCreateInput.getServiceName());
        try {
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceCreateInput.getServiceName()));
            Services service = ModelMappingUtils.mappingServices(serviceCreateInput, null);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to create service " + serviceCreateInput.getServiceName() + " to Service List";
            LOG.warn(message, e);
            return OperationResult.failed(message);
        }
    }

    @Override
    public OperationResult createTempService(TempServiceCreateInput tempServiceCreateInput) {
        LOG.debug("Writing '{}' Temp Service", tempServiceCreateInput.getCommonId());
        try {
            InstanceIdentifier<org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                .Services> iid = InstanceIdentifier.create(TempServiceList.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                            .Services.class, new org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp
                                .service.list.ServicesKey(tempServiceCreateInput.getCommonId()));
            org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.temp.service.list
                .Services service = ModelMappingUtils.mappingServices(tempServiceCreateInput);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
            writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to create Temp service " + tempServiceCreateInput.getCommonId()
                + " to TempService List";
            LOG.warn(message, e);
            return OperationResult.failed(message);
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
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to create servicePath " + serviceInput.getCommonId() + " to ServicePath List";
            LOG.warn(message, e);
            return OperationResult.failed(message);
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
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            String message = "Unable to delete service path " + serviceName;
            LOG.error(message, e);
            return OperationResult.failed(message);
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
        String result = null;
        Optional<Services> readService = getService(serviceName);
        if (readService.isPresent()) {
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
                    service.setOperationalState(State.InService).setAdministrativeState(State.InService);
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
                LOG.error("Failed to {} service from Service List", action, e);
                result = "Failed to " + action + " service from Service List";
            }
        } else {
            /*
             * Write Service.
             */
            if (choice == 2) {
                LOG.debug("Writing '{}' Service", serviceName);
                InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                        .child(Services.class, new ServicesKey(serviceName));
                Services service = ModelMappingUtils.mappingServices(input, null);
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
                try {
                    writeTx.commit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
                    result = null;
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    LOG.error("Failed to createService service to Service List", e);
                    result = "Failed to createService service to Service List";
                }
            } else {
                LOG.info("Service is not present ! ");
                result = "Service is not present ! ";
            }
        }
        return result;
    }
}
