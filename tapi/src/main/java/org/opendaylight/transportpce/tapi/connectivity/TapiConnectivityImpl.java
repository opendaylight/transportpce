/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.connectivity;

import com.google.common.util.concurrent.ListenableFuture;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.tapi.listeners.TapiPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiRendererListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.TapiServiceHandlerListenerImpl;
import org.opendaylight.transportpce.tapi.utils.TapiContext;
import org.opendaylight.transportpce.tapi.validation.CreateConnectivityServiceValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.OrgOpenroadmServiceService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.delete.input.ServiceDeleteReqInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.delete.input.ServiceDeleteReqInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.BandwidthProfileBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.tapi.context.ServiceInterfacePointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.cep.list.ConnectionEndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.CapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connection.details.output.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connection.end.point.details.output.ConnectionEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service.list.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service.list.output.ServiceKey;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top level service interface providing main TAPI Connectivity services.
 */
public class TapiConnectivityImpl implements TapiConnectivityService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiConnectivityImpl.class);

    private OrgOpenroadmServiceService serviceHandler;
    private final TapiContext tapiContext;
    private final ConnectivityUtils connectivityUtils;
    private TapiPceListenerImpl pceListenerImpl;
    private TapiRendererListenerImpl rendererListenerImpl;
    private TapiServiceHandlerListenerImpl serviceHandlerListenerImpl;

    public TapiConnectivityImpl(OrgOpenroadmServiceService serviceHandler, TapiContext tapiContext,
                                ConnectivityUtils connectivityUtils, TapiPceListenerImpl pceListenerImpl,
                                TapiRendererListenerImpl rendererListenerImpl,
                                TapiServiceHandlerListenerImpl serviceHandlerListenerImpl) {
        LOG.info("inside TapiImpl constructor");
        this.serviceHandler = serviceHandler;
        this.tapiContext = tapiContext;
        this.connectivityUtils = connectivityUtils;
        this.pceListenerImpl = pceListenerImpl;
        this.rendererListenerImpl = rendererListenerImpl;
        this.serviceHandlerListenerImpl = serviceHandlerListenerImpl;
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
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                    "SIP list is empty").buildFuture();
            }
            if (sipMap.containsKey(new ServiceInterfacePointKey(input.getEndPoint().values().stream().findFirst().get()
                    .getServiceInterfacePoint().getServiceInterfacePointUuid()))
                    && sipMap.containsKey(new ServiceInterfacePointKey(input.getEndPoint().values().stream().skip(1)
                    .findFirst().get().getServiceInterfacePoint().getServiceInterfacePointUuid()))) {
                LOG.info("SIPs found in sipMap");
                ServiceCreateInput sci = this.connectivityUtils.createORServiceInput(input, serviceUuid);
                if (sci == null) {
                    return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                        "Couldnt map Service create input").buildFuture();
                }
                LOG.info("Service Create input = {}", sci);
                output = this.serviceHandler.serviceCreate(sci);
                if (!output.isDone()) {
                    return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                        "Service create RPC failed").buildFuture();
                }
            } else {
                LOG.error("Unknown UUID");
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                    "SIPs do not exist in tapi context").buildFuture();
            }
        }
        try {
            if (output == null) {
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                    "Failed to create service").buildFuture();
            }
            LOG.info("Service create request was successful");
            if (output.get().getResult().getConfigurationResponseCommon().getResponseCode()
                    .equals(ResponseCodes.RESPONSE_FAILED)) {
                return RpcResultBuilder.<CreateConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                    "Failed to create service").buildFuture();
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
        ConnectivityService service = new ConnectivityServiceBuilder()
            .setUuid(serviceUuid)
            .setAdministrativeState(AdministrativeState.LOCKED)
            .setOperationalState(OperationalState.DISABLED)
            .setLifecycleState(LifecycleState.PLANNED)
            .setServiceLayer(input.getConnectivityConstraint().getServiceLayer())
            .setServiceLevel(input.getConnectivityConstraint().getServiceLevel())
            .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY)
            .setConnectivityDirection(ForwardingDirection.BIDIRECTIONAL)
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

    private Map<EndPointKey, EndPoint> createEndPoints(
            Map<org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPointKey,
                org.opendaylight.yang.gen.v1.urn
                    .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint> endPoints) {
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        for (org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint ep:
                endPoints.values()) {
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
                .setCapacity(new CapacityBuilder()
                    .setTotalSize(new TotalSizeBuilder().build())
                    .setBandwidthProfile(new BandwidthProfileBuilder().build()) // TODO: implement bandwidth profile
                    .build())
                .setProtectionRole(ep.getProtectionRole())
                .setRole(ep.getRole())
                .setLocalId(ep.getLocalId())
                .build();
            endPointMap.put(endpoint.key(), endpoint);
        }
        return endPointMap;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceDetailsOutput>> getConnectivityServiceDetails(
            GetConnectivityServiceDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid serviceUuid = new Uuid(input.getServiceIdOrName());
        ConnectivityService service = this.tapiContext.getConnectivityService(serviceUuid);
        if (service == null) {
            LOG.error("Service {} doesnt exist in tapi context", input.getServiceIdOrName());
            return RpcResultBuilder.<GetConnectivityServiceDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                    "Service doesnt exist in datastore").buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectivityServiceDetailsOutputBuilder().setService(
            new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service.details.output.ServiceBuilder(
                    service).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UpdateConnectivityServiceOutput>> updateConnectivityService(
            UpdateConnectivityServiceInput input) {
        // TODO Auto-generated method stub. More complicated as it depends on what needs to be updated... left aside
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionDetailsOutput>> getConnectionDetails(
            GetConnectionDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid connectionUuid = new Uuid(UUID.nameUUIDFromBytes(input.getConnectionIdOrName()
            .getBytes(Charset.forName("UTF-8"))).toString());
        Connection connection = this.tapiContext.getConnection(connectionUuid);
        if (connection == null) {
            LOG.error("Connection {} doesnt exist in tapi context", input.getConnectionIdOrName());
            return RpcResultBuilder.<GetConnectionDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                    "Connection doesnt exist in datastore").buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectionDetailsOutputBuilder().setConnection(
                new ConnectionBuilder(connection).build()).build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> deleteConnectivityService(
            DeleteConnectivityServiceInput input) {
        //TODO Auto-generated method stub
        // TODO add try
        Uuid serviceUuid = new Uuid(input.getServiceIdOrName());
        this.tapiContext.deleteConnectivityService(serviceUuid);
        ListenableFuture<RpcResult<ServiceDeleteOutput>> output =
            this.serviceHandler.serviceDelete(new ServiceDeleteInputBuilder()
                .setServiceDeleteReqInfo(new ServiceDeleteReqInfoBuilder()
                    .setServiceName(input.getServiceIdOrName())
                    .setTailRetention(ServiceDeleteReqInfo.TailRetention.No)
                    .build())
                .setSdncRequestHeader(new SdncRequestHeaderBuilder()
                    .setRequestId("request-1")
                    .setRpcAction(RpcActions.ServiceDelete)
                    .setNotificationUrl("notification url")
                    .setRequestSystemId("appname")
                    .build())
                .build());
        if (output == null) {
            return RpcResultBuilder.<DeleteConnectivityServiceOutput>failed().withError(RpcError.ErrorType.RPC,
                "Failed to delete Link").buildFuture();
        }
        LOG.info("Service is being deleted and devices are being rolled back");
        return RpcResultBuilder.success(new DeleteConnectivityServiceOutputBuilder().build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceListOutput>> getConnectivityServiceList(
            GetConnectivityServiceListInput input) {
        // TODO Auto-generated method stub
        Map<ConnectivityServiceKey, ConnectivityService> connMap = this.tapiContext.getConnectivityServices();
        if (connMap == null) {
            LOG.error("No services in tapi context");
            return RpcResultBuilder.<GetConnectivityServiceListOutput>failed().withError(RpcError.ErrorType.RPC,
                    "No services exist in datastore").buildFuture();
        }

        Map<ServiceKey, Service> serviceMap = new HashMap<>();
        for (ConnectivityService connectivityService: connMap.values()) {
            Service service = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service.list.output.ServiceBuilder(
                    connectivityService).build();
            serviceMap.put(service.key(), service);
        }
        return RpcResultBuilder.success(new GetConnectivityServiceListOutputBuilder().setService(serviceMap)
            .build()).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionEndPointDetailsOutput>> getConnectionEndPointDetails(
            GetConnectionEndPointDetailsInput input) {
        // TODO Auto-generated method stub
        Uuid topoUuid = new Uuid(UUID.nameUUIDFromBytes(input.getTopologyIdOrName()
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nodeUuid = new Uuid(UUID.nameUUIDFromBytes(input.getNodeIdOrName()
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid nepUuid = new Uuid(UUID.nameUUIDFromBytes(input.getNepIdOrName()
            .getBytes(Charset.forName("UTF-8"))).toString());
        Uuid cepUuid = new Uuid(UUID.nameUUIDFromBytes(input.getCepIdOrName()
            .getBytes(Charset.forName("UTF-8"))).toString());
        ConnectionEndPoint cep = this.tapiContext.getTapiCEP(topoUuid, nodeUuid, nepUuid, cepUuid);
        if (cep == null) {
            LOG.error("Cep doesnt exist in tapi context");
            return RpcResultBuilder.<GetConnectionEndPointDetailsOutput>failed().withError(RpcError.ErrorType.RPC,
                    "No cep with given Uuid exists in datastore").buildFuture();
        }
        return RpcResultBuilder.success(new GetConnectionEndPointDetailsOutputBuilder().setConnectionEndPoint(
            new ConnectionEndPointBuilder(cep).build()).build()).buildFuture();
    }
}
