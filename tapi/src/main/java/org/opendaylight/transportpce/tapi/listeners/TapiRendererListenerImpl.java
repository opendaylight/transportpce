/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.listeners;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.RendererRpcResultSp;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.TransportpceRendererListener;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiRendererListenerImpl implements TransportpceRendererListener {

    private static final Logger LOG = LoggerFactory.getLogger(TapiRendererListenerImpl.class);
    private final DataBroker dataBroker;
    private Uuid serviceUuid;
    private RendererRpcResultSp serviceRpcResultSp;
    private final NetworkTransactionService networkTransactionService;

    public TapiRendererListenerImpl(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.networkTransactionService = new NetworkTransactionImpl(new RequestProcessor(this.dataBroker));
    }

    @Override
    public void onRendererRpcResultSp(RendererRpcResultSp notification) {
        if (compareServiceRpcResultSp(notification)) {
            LOG.warn("ServiceRpcResultSp already wired !");
            return;
        }
        serviceRpcResultSp = notification;
        int notifType = serviceRpcResultSp.getNotificationType().getIntValue();
        LOG.info("Renderer '{}' Notification received : {}", serviceRpcResultSp.getNotificationType().getName(),
                notification);
        /* service-implementation-request. */
        if (notifType == 3) {
            onServiceImplementationResult(notification);
        }
    }

    /**
     * Process service implementation result for serviceName.
     * @param notification RendererRpcResultSp
     */
    private void onServiceImplementationResult(RendererRpcResultSp notification) {
        switch (serviceRpcResultSp.getStatus()) {
            case Successful:
                onSuccededServiceImplementation();
                break;
            case Failed:
                onFailedServiceImplementation(notification.getServiceName());
                break;
            case  Pending:
                LOG.warn("Service Implementation still pending according to RpcStatusEx");
                break;
            default:
                LOG.warn("Service Implementation has an unknown RpcStatusEx code");
                break;
        }
    }

    /**
     * Process succeeded service implementation for service.
     */
    private void onSuccededServiceImplementation() {
        LOG.info("Service implemented !");
        // TODO: update Connections and Connectivity Service states
        ConnectivityService connectivityService = getConnectivityService(this.serviceUuid);
        if (connectivityService == null) {
            LOG.error("Couldnt retrieve service from datastore");
            return;
        }
        LOG.info("Connectivity service = {}", connectivityService.toString());
        // TODO --> this throws error because the renderer goes really fast. Is this normal??
        ConnectivityService updtConnServ = new ConnectivityServiceBuilder(connectivityService)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setLifecycleState(LifecycleState.INSTALLED)
            .setOperationalState(OperationalState.ENABLED)
            .build();
        for (Connection connection:updtConnServ.getConnection().values()) {
            updateConnectionState(connection.getConnectionUuid());
        }
        updateConnectivityService(updtConnServ);
    }

    /**
     * Process failed service implementation for serviceName.
     * @param serviceName String
     */
    private void onFailedServiceImplementation(String serviceName) {
        LOG.error("Renderer implementation failed !");
        LOG.info("PCE cancel resource done OK !");
        Uuid suuid = new Uuid(UUID.nameUUIDFromBytes(serviceName.getBytes(Charset.forName("UTF-8")))
                .toString());
        // get connections of connectivity service and remove them from tapi context and then remove
        //  service from context. The CEPs are maintained as they could be reused by another service
        ConnectivityService connService = getConnectivityService(suuid);
        if (connService == null) {
            LOG.error("Service doesnt exist in tapi context");
            return;
        }
        for (Connection connection:connService.getConnection().values()) {
            deleteConnection(connection.getConnectionUuid());
        }
        deleteConnectivityService(suuid);
    }

    @SuppressFBWarnings(
            value = "ES_COMPARING_STRINGS_WITH_EQ",
            justification = "false positives, not strings but real object references comparisons")
    private Boolean compareServiceRpcResultSp(RendererRpcResultSp notification) {
        if (serviceRpcResultSp == null) {
            return false;
        }
        if (serviceRpcResultSp.getNotificationType() != notification.getNotificationType()) {
            return false;
        }
        if (serviceRpcResultSp.getServiceName() != notification.getServiceName()) {
            return false;
        }
        if (serviceRpcResultSp.getStatus() != notification.getStatus()) {
            return false;
        }
        if (serviceRpcResultSp.getStatusMessage() != notification.getStatusMessage()) {
            return false;
        }
        return true;
    }

    private ConnectivityService getConnectivityService(Uuid suuid) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            // First read connectivity service with service uuid and update info
            InstanceIdentifier<ConnectivityService> connectivityServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(suuid))
                    .build();

            Optional<ConnectivityService> optConnServ =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectivityServIID).get();
            if (!optConnServ.isPresent()) {
                LOG.error("Connectivity service not found in tapi context");
                return null;
            }
            return optConnServ.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI connectivity", e);
            return null;
        }
    }

    private void updateConnectionState(Uuid connectionUuid) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            // First read connection with connection uuid and update info
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection> connectionIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                            .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection.class,
                        new ConnectionKey(connectionUuid))
                    .build();

            Optional<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection> optConn =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connectionIID).get();
            if (!optConn.isPresent()) {
                LOG.error("Connection not found in tapi context");
                return;
            }
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection
                newConnection = new ConnectionBuilder(optConn.get()).setLifecycleState(LifecycleState.INSTALLED)
                    .setOperationalState(OperationalState.ENABLED).build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connectionIID,
                    newConnection);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI connection merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI connection", e);
        }
    }

    private void updateConnectivityService(ConnectivityService updtConnServ) {
        // TODO: verify this is correct. Should we identify the context IID with the context UUID??
        try {
            // First read connectivity service with connectivity service uuid and update info
            InstanceIdentifier<ConnectivityService> connServIID =
                InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                    .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                    .child(ConnectivityService.class, new ConnectivityServiceKey(updtConnServ.getUuid()))
                    .build();

            Optional<ConnectivityService> optConnServ =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, connServIID).get();
            if (!optConnServ.isPresent()) {
                LOG.error("Connection not found in tapi context");
                return;
            }
            ConnectivityService newConnServ = new ConnectivityServiceBuilder(updtConnServ).build();
            // merge in datastore
            this.networkTransactionService.merge(LogicalDatastoreType.OPERATIONAL, connServIID,
                    newConnServ);
            this.networkTransactionService.commit().get();
            LOG.info("TAPI connectivity service merged successfully.");
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to merge TAPI connectivity service", e);
        }
    }

    private void deleteConnectivityService(Uuid suuid) {
        // First read connectivity service with service uuid and update info
        InstanceIdentifier<ConnectivityService> connectivityServIID =
            InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                .child(ConnectivityService.class, new ConnectivityServiceKey(suuid))
                .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectivityServIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI connectivity service", e);
        }
    }

    private void deleteConnection(Uuid connectionUuid) {
        // First read connectivity service with service uuid and update info
        InstanceIdentifier<org.opendaylight.yang.gen.v1
            .urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection> connectionIID =
            InstanceIdentifier.builder(Context.class).augmentation(Context1.class)
                .child(org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.context.ConnectivityContext.class)
                .child(org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection.class,
                    new org.opendaylight.yang.gen.v1.urn
                        .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectionKey(
                            connectionUuid))
                .build();
        try {
            this.networkTransactionService.delete(LogicalDatastoreType.OPERATIONAL, connectionIID);
            this.networkTransactionService.commit().get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to delete TAPI connection", e);
        }
    }

    public void setServiceUuid(Uuid serviceUuid) {
        this.serviceUuid = serviceUuid;
    }
}
