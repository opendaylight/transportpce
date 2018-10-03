/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.servicehandler.ModelMappingUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev170426.PathComputationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceListBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.ServicesKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServicePathList;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.service.path.list.ServicePathsKey;
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
        try {
            LOG.info("initializing service registry");
            WriteTransaction transaction = this.dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<ServiceList> iid = InstanceIdentifier.create(ServiceList.class);
            ServiceList initialRegistry = new ServiceListBuilder().build();
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, initialRegistry);
            Future<Void> future = transaction.submit();
            future.get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("init failed: {}", e.getMessage());
        }
    }

    @Override
    public Optional<Services> getService(String serviceName) {
        try {
            ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction();
            InstanceIdentifier<Services> iid = InstanceIdentifier
                    .create(ServiceList.class).child(Services.class, new ServicesKey(serviceName));
            Future<com.google.common.base.Optional<Services>> future
                    = readTx.read(LogicalDatastoreType.OPERATIONAL, iid);
            return future.get(Timeouts.DATASTORE_READ, TimeUnit.MILLISECONDS).toJavaUtil();
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
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class)
                    .child(Services.class, new ServicesKey(serviceName));
            writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
            writeTx.submit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to delete service " + serviceName + " from Service List";
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
                Services services = new ServicesBuilder(readService.get())
                        .setOperationalState(operationalState)
                        .setAdministrativeState(administrativeState)
                        .build();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, services);
                writeTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
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
    public OperationResult createService(ServiceCreateInput serviceCreateInput,
            PathComputationRequestOutput outputFromPce) {
        LOG.debug("Writing '{}' Service", serviceCreateInput.getServiceName());
        try {
            InstanceIdentifier<Services> iid = InstanceIdentifier
                    .create(ServiceList.class).child(Services.class,
                            new ServicesKey(serviceCreateInput.getServiceName()));
            Services service = ModelMappingUtils.mappingServices(serviceCreateInput, null);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
            writeTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to create service " + serviceCreateInput.getServiceName() + " to Service List";
            LOG.warn(message, e);
            return OperationResult.failed(message);
        }
    }

    @Override
    public OperationResult createServicePath(ServiceCreateInput serviceCreateInput,
            PathComputationRequestOutput outputFromPce) {
        LOG.debug("Writing '{}' Service", serviceCreateInput.getServiceName());
        try {
            InstanceIdentifier<ServicePaths> servicePathsIID = InstanceIdentifier
                    .create(ServicePathList.class)
                    .child(ServicePaths.class, new ServicePathsKey(serviceCreateInput.getServiceName()));
            ServicePaths servicePath =  ModelMappingUtils.mappingServicePaths(serviceCreateInput, null, outputFromPce);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            writeTx.put(LogicalDatastoreType.OPERATIONAL, servicePathsIID, servicePath);
            writeTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            return OperationResult.ok(SUCCESSFUL_MESSAGE);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            String message = "Failed to create servicePath " + serviceCreateInput.getServiceName()
                + " to ServicePath List";
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
            servicePathsWriteTx.submit().get(Timeouts.DATASTORE_DELETE, TimeUnit.MILLISECONDS);
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
     * @param serviceName
     *            Name of service
     * @param input
     *            ServiceCreateInput
     * @param output
     *            PathComputationRequestOutput
     * @param choice
     *            0 - Modify 1 - Delete 2 - Write
     * @return String operations result, null if ok or not otherwise
     */
    @Deprecated
    @Override
    public String writeOrModifyOrDeleteServiceList(String serviceName, ServiceCreateInput input,
                                                    PathComputationRequestOutput output, int choice) {
        LOG.debug("WriteOrModifyOrDeleting '{}' Service",serviceName);
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        String result = null;
        Optional<Services> readService = getService(serviceName);
        if (readService.isPresent()) {
            /*
             * Modify / Delete Service.
             */
            InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                    new ServicesKey(serviceName));
            ServicesBuilder service = new ServicesBuilder(readService.get());

            String action = null;
            switch (choice) {
                case 0: /* Modify. */
                    LOG.debug("Modifying '{}' Service", serviceName);
                    service.setOperationalState(State.InService).setAdministrativeState(State.InService);
                    writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid, service.build());
                    action = "modifyService";
                    break;

                case 1: /* Delete */
                    LOG.debug("Deleting '{}' Service", serviceName);
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    action = "deleteService";
                    break;

                default:
                    LOG.debug("No choice found");
                    break;

            }
            try {
                writeTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOG.error("Failed to {} service from Service List", action, e);
                result = "Failed to " + action + " service from Service List";
            }
        } else {
            if (choice == 2) { /* Write Service */
                LOG.debug("Writing '{}' Service", serviceName);
                InstanceIdentifier<Services> iid = InstanceIdentifier.create(ServiceList.class).child(Services.class,
                        new ServicesKey(serviceName));

                Services service = ModelMappingUtils.mappingServices(input, null);
                writeTx.put(LogicalDatastoreType.OPERATIONAL, iid, service);
                try {
                    writeTx.submit().get(Timeouts.DATASTORE_WRITE, TimeUnit.MILLISECONDS);
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
