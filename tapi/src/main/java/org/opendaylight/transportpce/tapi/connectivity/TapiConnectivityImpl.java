/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.util.concurrent.ListenableFuture;
//import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.tapi.listeners.TapiPceNotificationHandler;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererNotificationHandler;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.validation.CreateConnectivityServiceValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
//import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.BandwidthProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionEndPointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectionEndPointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.GetConnectivityServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.get.connection.details.output.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.list.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.list.output.ServiceKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Rpc;
import org.opendaylight.yangtools.yang.common.ErrorTag;
import org.opendaylight.yangtools.yang.common.ErrorType;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top level service interface providing main TAPI Connectivity services.
 */
public class TapiConnectivityImpl implements TapiConnectivityService {
    private final NetworkTransactionService networkTransactionService;
    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityImpl.class);

    private OrgOpenroadmServiceService serviceHandler;
    private final TapiContext tapiContext;
    private final ConnectivityUtils connectivityUtils;
    private TapiPceNotificationHandler pceListenerImpl;
    private TapiRendererNotificationHandler rendererListenerImpl;

    public TapiConnectivityImpl(OrgOpenroadmServiceService serviceHandler, TapiContext tapiContext,
                                ConnectivityUtils connectivityUtils, TapiPceNotificationHandler pceListenerImpl,
                                TapiRendererNotificationHandler rendererListenerImpl,
                                NetworkTransactionService nts) {
        LOG.info("inside TapiImpl constructor");
        this.serviceHandler = serviceHandler;
        this.tapiContext = tapiContext;
        this.connectivityUtils = connectivityUtils;
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
        this.networkTransactionService = nts;
    }

    @Override
    public ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> createConnectivityService(
            CreateConnectivityServiceInput input) {
        // TODO: later version of TAPI models include Name as an input parameter in connectivity.yang
        LOG.info("RPC create-connectivity received: {}", input.getEndPoint());
        Uuid serviceUuid = new Uuid(UUID.randomUUID().toString());
        this.pceListenerImpl.setInput(input);
        this.pceListenerImpl.setServiceUuid(serviceUuid);
        this.rendererListenerImpl.setServiceUuid(serviceUuid);
        ListenableFuture<RpcResult<ServiceCreateOutput>> output = null;
        OperationResult validationResult = CreateConnectivityServiceValidation.validateCreateConnectivityServiceRequest(
                input);
        if (validationResult.isSuccess()) {
            LOG.info("input parameter of RPC create-connectivity are being handled");
            // check uuid of SIP in tapi context
            Map<ServiceInterfacePointKey, ServiceInterfacePoint> sipMap = this.tapiContext.getTapiContext()
                    .getServiceInterfacePoint();
            if (sipMap == null) {
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.RPC, "SIP list is empty")
                    .buildFuture();
            }
            if (sipMap.containsKey(new ServiceInterfacePointKey(input.getEndPoint().values().stream().findFirst()
                        .orElseThrow().getServiceInterfacePoint().getServiceInterfacePointUuid()))
                    && sipMap.containsKey(new ServiceInterfacePointKey(input.getEndPoint().values().stream().skip(1)
                        .findFirst().orElseThrow().getServiceInterfacePoint().getServiceInterfacePointUuid()))) {
                LOG.info("SIPs found in sipMap");
                // TODO: differentiate between OTN service and GbE service in TAPI
                ServiceCreateInput sci = this.connectivityUtils.createORServiceInput(input, serviceUuid);
                if (sci == null) {
                    return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                        .withError(ErrorType.RPC, "Couldnt map Service create input")
                        .buildFuture();
                }
                LOG.info("Service Create input = {}", sci);
                output = this.serviceHandler.serviceCreate(sci);
                if (!output.isDone()) {
                    return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                        .withError(ErrorType.RPC, "Service create RPC failed")
                        .buildFuture();
                }
            } else {
                LOG.error("Unknown UUID");
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.RPC, "SIPs do not exist in tapi context")
                    .buildFuture();
            }
        }
        try {
            if (output == null) {
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.RPC, "Failed to create service")
                    .buildFuture();
            }
            LOG.info("Service create request was successful");
            if (output.get().getResult().getConfigurationResponseCommon().getResponseCode()
                    .equals(ResponseCodes.RESPONSE_FAILED)) {
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed()
                    .withError(ErrorType.RPC, "Failed to create service")
                    .buildFuture();
            }
            LOG.info("Output of service request = {}", output.get().getResult());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error checking response code of service create", e);
        }
        // Connections and states should be created/updated when the pce and renderer are done :)
        Map<EndPointKey, EndPoint> endPointList = createEndPoints(input.getEndPoint());
        Name name = new NameBuilder()
            .setValueName("Connectivity Service Name")
            .setValue(serviceUuid.getValue())
            .build();
        ConnectivityConstraint conConstr = new ConnectivityConstraintBuilder()
            .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY)
            .setServiceLevel(input.getConnectivityConstraint().getServiceLevel()).build();

        ConnectivityService service = new ConnectivityServiceBuilder()
            .setUuid(serviceUuid)
            .setAdministrativeState(AdministrativeState.LOCKED)
            .setOperationalState(OperationalState.DISABLED)
            .setLifecycleState(LifecycleState.PLANNED)
            .setLayerProtocolName(input.getLayerProtocolName())
            .setConnectivityConstraint(conConstr)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setName(Map.of(name.key(), name))
            .setConnection(new HashMap<>())
            .setEndPoint(endPointList)
            .build();
        // add to tapi context
        this.tapiContext.updateConnectivityContext(Map.of(service.key(), service), new HashMap<>());
        LOG.info("Created locked service in Datastore. Waiting for PCE and Renderer to complete tasks...");
        // return ConnectivityServiceCreateOutput
        return RpcResultBuilder.success(new CreateConnectivityServiceOutputBuilder()
            .setService(new ServiceBuilder(service).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceDetailsOutput>> getConnectivityServiceDetails(
            GetConnectivityServiceDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid serviceUuid = input.getUuid();
        ConnectivityService service = this.tapiContext.getConnectivityService(serviceUuid);
        if (service == null) {
            LOG.error("Service {} doesnt exist in tapi context", input.getUuid());
            return RpcResultBuilder.<GetConnectivityServiceDetailsOutput>failed()
                .withError(ErrorType.RPC, "Service doesnt exist in datastore")
                .buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectivityServiceDetailsOutputBuilder().setService(
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.details.output.ServiceBuilder(
                    service).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UpdateConnectivityServiceOutput>> updateConnectivityService(
            UpdateConnectivityServiceInput input) {
        // TODO Auto-generated method stub. More complicated as it depends on what needs to be updated... left aside
        return RpcResultBuilder.<UpdateConnectivityServiceOutput>failed()
            .withError(ErrorType.RPC, ErrorTag.OPERATION_NOT_SUPPORTED, "RPC not implemented yet")
            .buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionDetailsOutput>> getConnectionDetails(
            GetConnectionDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid connectionUuid = input.getUuid();
        Connection connection = this.tapiContext.getConnection(connectionUuid);
        if (connection == null) {
            LOG.error("Connection {} doesnt exist in tapi context", input.getUuid());
            return RpcResultBuilder.<GetConnectionDetailsOutput>failed()
                .withError(ErrorType.RPC, "Connection doesnt exist in datastore")
                .buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectionDetailsOutputBuilder().setConnection(
                new ConnectionBuilder(connection).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> deleteConnectivityService(
            DeleteConnectivityServiceInput input) {
        //TODO Auto-generated method stub
        // TODO add try
        String serviceName = null;
        try {
            serviceName = getNameFromUuid(input.getUuid(), "Service").iterator().next();
        } catch (ExecutionException e) {
            LOG.error("Service {} to be deleted not found in the DataStore", e.getMessage());
            return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed()
                .withError(ErrorType.RPC, "Failed to delete Service")
                .buildFuture();
        }
        if (input.getUuid() != null) {
            try {
                Uuid serviceUuid = input.getUuid();
                this.tapiContext.deleteConnectivityService(serviceUuid);
                ListenableFuture<RpcResult<ServiceDeleteOutput>> output =
                    this.serviceHandler.serviceDelete(new ServiceDeleteInputBuilder()
                        .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                            .setServiceName(serviceName)
                            .setTailRetention(ServiceDeleteReqInfo.TailRetention.No)
                            .build())
                        .setSdncRequestHeader(new SdncRequestHeaderBuilder()
                            .setRequestId("request-1")
                            .setNotificationUrl("notification url")
                            .setRequestSystemId("appname")
                            .setRpcAction(RpcActions.ServiceDelete)
                            .build())
                        .build());
                RpcResult<ServiceDeleteOutput> rpcResult = output.get();
                if (!rpcResult.getResult().getConfigurationResponseCommon().getResponseCode()
                        .equals(ResponseCodes.RESPONSE_FAILED)) {
                    LOG.info("Service is being deleted and devices are being rolled back");
                    return RpcResultBuilder.success(new DeleteConnectivityServiceOutputBuilder().build()).buildFuture();
                }
                LOG.error("Failed to delete service. Deletion process failed");
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to delete service.", e);
            }
        }
        return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed()
            .withError(ErrorType.RPC, "Failed to delete Service")
            .buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceListOutput>> getConnectivityServiceList(
            GetConnectivityServiceListInput input) {
        // TODO Auto-generated method stub
        Map<ConnectivityServiceKey, ConnectivityService> connMap = this.tapiContext.getConnectivityServices();
        if (connMap == null) {
            LOG.error("No services in tapi context");
            return RpcResultBuilder.<GetConnectivityServiceListOutput>failed()
                .withError(ErrorType.RPC, "No services exist in datastore")
                .buildFuture();
        }

        Map<ServiceKey, Service> serviceMap = new HashMap<>();
        for (ConnectivityService connectivityService: connMap.values()) {
            Service service = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.get.connectivity.service.list.output.ServiceBuilder(
                    connectivityService).build();
            serviceMap.put(service.key(), service);
        }
        return RpcResultBuilder.success(new GetConnectivityServiceListOutputBuilder().setService(serviceMap)
            .build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionEndPointDetailsOutput>> getConnectionEndPointDetails(
            GetConnectionEndPointDetailsInput input) {
//        // TODO Auto-generated method stub
//        Uuid topoUuid = getUuidFromIput(input.getTopologyIdOrName());
//        Uuid nodeUuid = getUuidFromIput(input.getNodeIdOrName());
//        Uuid nepUuid = getUuidFromIput(input.getNepIdOrName());
//        Uuid cepUuid = getUuidFromIput(input.getCepIdOrName());
//        ConnectionEndPoint cep = this.tapiContext.getTapiCEP(topoUuid, nodeUuid, nepUuid, cepUuid);
//        if (cep == null) {
//            LOG.error("Cep doesnt exist in tapi context");
//            return RpcResultBuilder.<GetConnectionEndPointDetailsOutput>failed()
//                .withError(ErrorType.RPC, "No cep with given Uuid exists in datastore")
//                .buildFuture();
//        }
        LOG.error("Method getConnectionEndPointDetails not currently implemented");
        return null;
//        return RpcResultBuilder.success(new GetConnectionEndPointDetailsOutputBuilder().setConnectionEndPoint(
//            new ConnectionEndPointBuilder(cep).build()).build()).buildFuture();
    }

    public ImmutableClassToInstanceMap<Rpc<?, ?>> registerRPCs() {
        return ImmutableClassToInstanceMap.<Rpc<?, ?>>builder()
            .put(CreateConnectivityService.class, this::createConnectivityService)
            .put(GetConnectivityServiceDetails.class, this::getConnectivityServiceDetails)
            .put(UpdateConnectivityService.class, this::updateConnectivityService)
            .put(GetConnectionDetails.class, this::getConnectionDetails)
            .put(DeleteConnectivityService.class, this::deleteConnectivityService)
            .put(GetConnectivityServiceList.class, this::getConnectivityServiceList)
            .put(GetConnectionEndPointDetails.class, this::getConnectionEndPointDetails)
            .build();
    }

    private Map<EndPointKey, EndPoint> createEndPoints(
        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint> endPoints) {
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                .create.connectivity.service.input.EndPoint ep: endPoints.values()) {
            EndPoint endpoint = new EndPointBuilder()
                .setServiceInterfacePoint(new ServiceInterfacePointBuilder()
                    .setServiceInterfacePointUuid(ep.getServiceInterfacePoint().getServiceInterfacePointUuid())
                    .build())
                .setName(ep.getName())
                .setAdministrativeState(ep.getAdministrativeState())
                .setDirection(ep.getDirection())
                .setLifecycleState(ep.getLifecycleState())
                .setOperationalState(ep.getOperationalState())
                .setLayerProtocolName(ep.getLayerProtocolName())
             // TODO: implement bandwidth profile
                .setCapacity(new CapacityBuilder()
                    .setTotalSize(new TotalSizeBuilder().build())
//                  .setBandwidthProfile(new BandwidthProfileBuilder().build())
                    .build())
                .setProtectionRole(ep.getProtectionRole())
                .setRole(ep.getRole())
                .setLocalId(ep.getLocalId())
                .build();
            endPointMap.put(endpoint.key(), endpoint);
        }
        return endPointMap;
    }

    public List<String> getNameFromUuid(Uuid uuid, String typeOfNode) throws ExecutionException {
        Map<NameKey, Name> nameMap = new HashMap<>();
        if ("service".equals(typeOfNode)) {
            ConnectivityService conServ = null;
            InstanceIdentifier<ConnectivityService> nodeIID = InstanceIdentifier.builder(
                Context.class).augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity
                .rev221121.Context1.class).child(ConnectivityContext.class)
                .child(ConnectivityService.class, new ConnectivityServiceKey(uuid)).build();
            ListenableFuture<Optional<ConnectivityService>> conServFuture =
                this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL, nodeIID);
            try {
                conServ = conServFuture.get().orElseThrow();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                //TODO: investigate on how to throw Interrupted exception (generate a check violation error)
            } catch (ExecutionException e) {
                throw new ExecutionException("Unable to get from mdsal service: " + nodeIID
                    .firstKeyOf(ConnectivityService.class).getUuid().getValue(), e);
            } catch (NoSuchElementException e) {
                return null;
            }
            nameMap = conServ.getName();
        }

        List<String> nameList = new ArrayList<>();
        for (Map.Entry<NameKey, Name> entry : nameMap.entrySet()) {
            nameList.add(entry.getValue().getValueName());
        }
        return nameList;
    }
}
