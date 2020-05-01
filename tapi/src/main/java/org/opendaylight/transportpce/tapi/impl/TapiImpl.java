/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.impl;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.opendaylight.transportpce.common.OperationResult;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.kafka.Kafka;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.transportpce.tapi.listeners.ORNetworkModelListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.ORPceListenerImpl;
import org.opendaylight.transportpce.tapi.listeners.ORRendererListenerImpl;
import org.opendaylight.transportpce.tapi.topology.MongoDbDataStoreService;
import org.opendaylight.transportpce.tapi.utils.GenericServiceEndpoint;
import org.opendaylight.transportpce.tapi.utils.ServiceEndpointType;
import org.opendaylight.transportpce.tapi.utils.TapiUtils;
import org.opendaylight.transportpce.tapi.validation.CreateConnectivityServiceValidation;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev181130.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.GetServiceInterfacePointListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.TapiCommonService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.UpdateServiceInterfacePointInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.UpdateServiceInterfacePointOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectionEndPointDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.GetConnectivityServiceListOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.TapiConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.UpdateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.UpdateConnectivityServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.Service;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.ServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.CreateNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.DeleteNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.DeleteNotificationSubscriptionServiceOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceDetailsOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetNotificationSubscriptionServiceListOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.GetSupportedNotificationTypesOutput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.Notification;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.NotificationType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.TapiNotificationService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.UpdateNotificationSubscriptionServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev181210.UpdateNotificationSubscriptionServiceOutput;
import org.opendaylight.yangtools.yang.common.RpcError;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Top level service interface providing main TAPI Connectivity services.
 */
public class TapiImpl implements TapiConnectivityService, TapiCommonService, TapiNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(TapiImpl.class);
    private static boolean validation = false;
    private ServiceHandlerOperations serviceHandler;
    private Kafka kafka;
    private MongoDbDataStoreService mongoDbDataStoreService;
    private ORRendererListenerImpl orRendererListener;
    private ORPceListenerImpl orPceListener;
    private ORNetworkModelListenerImpl orNetworkModelListener;

    public TapiImpl(ServiceHandlerOperations serviceHandler, Kafka kafka,
                    MongoDbDataStoreService mongoDbDataStoreService, ORNetworkModelListenerImpl orNetworkModelListener,
                    ORPceListenerImpl orPceListener, ORRendererListenerImpl orRendererListener) {
        LOG.info("inside TapiImpl constructor");
        this.serviceHandler = serviceHandler;
        this.mongoDbDataStoreService = mongoDbDataStoreService;
        this.kafka = kafka;
        this.orNetworkModelListener = orNetworkModelListener;
        this.orPceListener = orPceListener;
        this.orRendererListener = orRendererListener;
    }

    @Override
    public ListenableFuture<RpcResult<CreateConnectivityServiceOutput>> createConnectivityService(
            CreateConnectivityServiceInput input) {
        LOG.info("RPC create-connectivity received");
        LOG.info(input.getEndPoint().toString());
        OperationResult validationResult = CreateConnectivityServiceValidation.validateCreateConnectivityServiceRequest(
                input);
        if (validationResult.isSuccess()) {
            /*
            LOG.info("input parameter of RPC create-connectivity are being handled");
            // check uuid of SIP in the map
            MappingUtils.createSEPsFromConnectivityRequest(input.getEndPoint());
            Map<Uuid, GenericServiceEndpoint> map = MappingUtils.getMap();
            LOG.info("Map uuid-endpoints = {}", map);
            */
            List<Sip> tapiSips = this.mongoDbDataStoreService.getTapiSips();
            /*
            if (map.containsKey(input.getEndPoint().get(0).getServiceInterfacePoint().getServiceInterfacePointUuid())
                && map.containsKey(input.getEndPoint().get(1).getServiceInterfacePoint()
                    .getServiceInterfacePointUuid())) {

             */
            Uuid sip1 = input.getEndPoint().get(0).getServiceInterfacePoint().getServiceInterfacePointUuid();
            Uuid sip2 = input.getEndPoint().get(1).getServiceInterfacePoint().getServiceInterfacePointUuid();
            if (containsSip(tapiSips, sip1) && containsSip(tapiSips, sip2)) {
                // Mapping with OpenROADM
                // todo create generic end points from TAPI input
                /*
                ServiceCreateInput sci = TapiUtils.buildServiceCreateInput(map.get(input.getEndPoint().get(0)
                    .getServiceInterfacePoint()
                    .getServiceInterfacePointUuid()), map.get(input.getEndPoint().get(1).getServiceInterfacePoint()
                        .getServiceInterfacePointUuid()));

                */
                // todo -> might need to check wheter it is a wavlength service or ethernet service request
                ServiceCreateInput sci = TapiUtils.buildServiceCreateInput(buildGenericAEnd(input.getEndPoint().get(0)),
                        buildGenericZEnd(input.getEndPoint().get(1)));
                ServiceCreateOutput output = this.serviceHandler.serviceCreate(sci);
                LOG.info("Output of service creation = {}", output.getConfigurationResponseCommon().toString());
                // Check current output. The ack indicator will tell us whether the service is being implemented or not.
                if (!output.getConfigurationResponseCommon().getResponseCode().equals(ResponseCodes.RESPONSE_FAILED)) {
                    LayerProtocolName layerProtocolName = getServiceLayer(sip1, sip2, tapiSips);
                    if (layerProtocolName != null) {
                        List<EndPoint> endPointList = new ArrayList<>();
                        EndPoint endPoint1 = createEndpoint(input.getEndPoint().get(0));
                        EndPoint endPoint2 = createEndpoint(input.getEndPoint().get(1));
                        endPointList.add(endPoint1);
                        endPointList.add(endPoint2);
                        // todo -> good implementation. Temporary solution for ETH service. We will need to add
                        // connections
                        // with their details to the connection service of tapi
                        // todo --> only one top connection because it is ETH service
                        /*
                        Connection connection = new ConnectionBuilder().setConnectionUuid((new Uuid(UUID
                                .nameUUIDFromBytes().toString()))).build();

                        connectionList.add(connection);
                        */
                        List<Name> servicenameList = new ArrayList<>();
                        Name serviceName = new NameBuilder().setValueName("Service name").setValueName("Test service 1")
                                .build();
                        servicenameList.add(serviceName);
                        Uuid serviceUuid = new Uuid(UUID.randomUUID().toString());
                        this.orRendererListener.setServiceUuid(serviceUuid.getValue());
                        this.orPceListener.setServiceUuid(serviceUuid.getValue());
                        List<Connection> connectionList = new ArrayList<>();
                        Service tapiService = new ServiceBuilder().setUuid(serviceUuid).setEndPoint(endPointList)
                                .setConnection(connectionList).setName(servicenameList)
                                .setAdministrativeState(AdministrativeState.LOCKED)
                                .setServiceLayer(layerProtocolName)
                                .setOperationalState(OperationalState.DISABLED)
                                .setLifecycleState(LifecycleState.PLANNED)
                                .setConnectivityDirection(ForwardingDirection.BIDIRECTIONAL)
                                .build();
                        this.mongoDbDataStoreService.addService(tapiService);
                        CreateConnectivityServiceOutput out1 = new CreateConnectivityServiceOutputBuilder()
                                .setService(tapiService)
                                .build();
                        sendTapiNotification(NotificationType.OBJECTCREATION, out1.getService().getUuid());
                        return RpcResultBuilder.success(out1).buildFuture();
                    } else {
                        LOG.error("Sips dont belong to same protocol layer!");
                        // todo return error
                        CreateConnectivityServiceOutput out1 = new CreateConnectivityServiceOutputBuilder()
                                .build();
                        return RpcResultBuilder.success(out1).withError(RpcError.ErrorType.RPC,
                                "SIPs dont belong to the same layer").buildFuture();
                    }
                } else {
                    LOG.error("Service creation failed!");
                    // todo return error
                    CreateConnectivityServiceOutput out1 = new CreateConnectivityServiceOutputBuilder()
                            .build();
                    return RpcResultBuilder.success(out1).withError(RpcError.ErrorType.RPC,
                            "Service Handler failed creating service").buildFuture();
                }
            } else {
                LOG.error("Unknown UUID");
                // todo return error
                CreateConnectivityServiceOutput out1 = new CreateConnectivityServiceOutputBuilder()
                        .build();
                return RpcResultBuilder.success(out1).withError(RpcError.ErrorType.RPC,
                        "SIPs not recognized").buildFuture();
            }
        } else {
            LOG.error("Malformed connectivity seervice request");
            // todo return error
            CreateConnectivityServiceOutput out1 = new CreateConnectivityServiceOutputBuilder()
                    .build();
            return RpcResultBuilder.success(out1).withError(RpcError.ErrorType.RPC,
                    "Malformed create connectivity service request input").buildFuture();
        }
        // TODO maybe we need to wait until the service is created to send this response back
        // Todo -> enpoint sip uuid must be equal to the sips. And the endpoint??
        /*
        LOG.info("Validation with success??? {}", validation);
        List<EndPoint> endPointList = new ArrayList<>();
        EndPoint endpoint1 = new EndPointBuilder()
            .setLocalId(UUID.randomUUID().toString())
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(new Uuid(UUID
                .randomUUID().toString())).build())
            .build();
        EndPoint endpoint2 = new EndPointBuilder()
            .setLocalId(UUID.randomUUID().toString())
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(new Uuid(UUID
                .randomUUID().toString())).build())
            .build();
        endPointList.add(endpoint1);
        endPointList.add(endpoint2);
        List<Connection> connectionList = new ArrayList<>();
        Connection connection1 = new ConnectionBuilder().setConnectionUuid(new Uuid(UUID.randomUUID().toString()))
            .build();
        connectionList.add(connection1);
        ConnectivityService service = new ConnectivityServiceBuilder().build();
        List<Name> serviceNameList = new ArrayList<>();
        Name serviceName = new NameBuilder().setValueName("Service Name").setValue("service test").build();
        serviceNameList.add(serviceName);
        */
        // Map<Uuid, Service> map = MappingUtils.getMapService();
        // bmap.put(output.getService().getUuid(), output.getService());
    }

    private LayerProtocolName getServiceLayer(Uuid sip1, Uuid sip2, List<Sip> tapiSips) {
        LayerProtocolName l1 = null;
        LayerProtocolName l2 = null;
        for (Sip sip:tapiSips) {
            if (Objects.equals(sip.getUuid(), sip1)) {
                l1 = sip.getLayerProtocolName();
            }
            if (Objects.equals(sip.getUuid(), sip2)) {
                l2 = sip.getLayerProtocolName();
            }
        }
        if (l1 != null && l2 != null) {
            if (l1.equals(l2)) {
                return l1;
            } else {
                LOG.error("Sips dont exist in the same layer");
                return null;
            }
        } else {
            LOG.error("Layer of sips doesnt exist");
            return null;
        }
    }

    private EndPoint createEndpoint(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create
                                            .connectivity.service.input.EndPoint endPoint) {
        EndPointBuilder endPointBuilder =  new EndPointBuilder();
        endPointBuilder.setLayerProtocolName(endPoint.getLayerProtocolName());
        endPointBuilder.setLocalId(endPoint.getLocalId());
        endPointBuilder.setDirection(endPoint.getDirection());
        endPointBuilder.setRole(endPoint.getRole());
        endPointBuilder.setProtectionRole(endPoint.getProtectionRole());
        endPointBuilder.setOperationalState(endPoint.getOperationalState());
        endPointBuilder.setAdministrativeState(endPoint.getAdministrativeState());
        endPointBuilder.setServiceInterfacePoint(endPoint.getServiceInterfacePoint());
        return endPointBuilder.build();
    }

    private GenericServiceEndpoint buildGenericZEnd(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi
                                                            .connectivity.rev181210.create.connectivity
                                                            .service.input.EndPoint endPoint) {
        ServiceZEnd sep = new ServiceZEndBuilder()
                .setNodeId(new NodeIdType(endPoint.getLocalId()))
                .setClli("CLLI-" + endPoint.getLocalId())
                .setOpticType(OpticTypes.Gray).setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Long.valueOf(100))
                .setTxDirection(new TxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("Some tx port")
                        .setPortName("Some port name").setPortRack("000000.00").setPortShelf("00")
                        .setPortType("some port type").build()).setLgx(new LgxBuilder()
                        .setLgxDeviceName("Some lgx-device-name").setLgxPortName("Some lgx-port-name")
                        .setLgxPortRack("000000.00").setLgxPortShelf("00").build()).build())
                .setRxDirection(new RxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("Some rx port")
                        .setPortName("Some port name").setPortRack("000000.00").setPortShelf("00")
                        .setPortType("some port type").build()).setLgx(new LgxBuilder()
                        .setLgxDeviceName("Some lgx-device-name").setLgxPortName("Some lgx-port-name")
                        .setLgxPortRack("000000.00").setLgxPortShelf("00").build()).build())
                .build();
        ServiceEndpointType sepType = ServiceEndpointType.SERVICEZEND;
        return new GenericServiceEndpoint(sep, sepType);
    }

    private GenericServiceEndpoint buildGenericAEnd(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi
                                                            .connectivity.rev181210.create.connectivity
                                                            .service.input.EndPoint endPoint) {
        ServiceAEnd sep = new ServiceAEndBuilder().setNodeId(new NodeIdType(endPoint.getLocalId()))
                .setClli("CLLI-" + endPoint.getLocalId())
                .setOpticType(OpticTypes.Gray).setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Long.valueOf(100))
                .setTxDirection(new TxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("Some tx port")
                        .setPortName("Some port name").setPortRack("000000.00").setPortShelf("00")
                        .setPortType("some port type").build()).setLgx(new LgxBuilder()
                        .setLgxDeviceName("Some lgx-device-name").setLgxPortName("Some lgx-port-name")
                        .setLgxPortRack("000000.00").setLgxPortShelf("00").build()).build())
                .setRxDirection(new RxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("Some rx port")
                        .setPortName("Some port name").setPortRack("000000.00").setPortShelf("00")
                        .setPortType("some port type").build()).setLgx(new LgxBuilder()
                        .setLgxDeviceName("Some lgx-device-name").setLgxPortName("Some lgx-port-name")
                        .setLgxPortRack("000000.00").setLgxPortShelf("00").build()).build())
                .build();
        ServiceEndpointType sepType = ServiceEndpointType.SERVICEAEND;
        return new GenericServiceEndpoint(sep, sepType);
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceDetailsOutput>> getConnectivityServiceDetails(
            GetConnectivityServiceDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<UpdateConnectivityServiceOutput>> updateConnectivityService(
            UpdateConnectivityServiceInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionDetailsOutput>> getConnectionDetails(
            GetConnectionDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<DeleteConnectivityServiceOutput>> deleteConnectivityService(
            DeleteConnectivityServiceInput input) {
        /*
        LOG.info("RPC delete-connectivity received");
        SdncRequestHeader srh = new SdncRequestHeaderBuilder()
                .setRequestId("request1")
                .setRpcAction(RpcActions.ServiceDelete)
                .setRequestSystemId("appname")
                .setNotificationUrl("http://localhost:8585/NotificationServer/notify")
                .build();
        ServiceDeleteReqInfo sdri = new ServiceDeleteReqInfoBuilder()
                .setServiceName(input.getServiceIdOrName())
                .setTailRetention(ServiceDeleteReqInfo.TailRetention.No)
                .build();
        ServiceDeleteInput inputSh = new ServiceDeleteInputBuilder()
                .setSdncRequestHeader(srh)
                .setServiceDeleteReqInfo(sdri)
                .build();
        this.serviceHandler.serviceDelete(inputSh);

        */
        // todo delete from mongo db also
        // to continue...
        /*
        ConnectivityService service = new ConnectivityServiceBuilder().build();
        DeleteConnectivityServiceOutput output = new DeleteConnectivityServiceOutputBuilder()
                .setService(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity
                        .rev181210.delete.connectivity.service.output.ServiceBuilder(service)
                        .setUuid(new Uuid(UUID.randomUUID().toString())).build()).build();
        */
        // return RpcResultBuilder.success(output).buildFuture();
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectivityServiceListOutput>> getConnectivityServiceList(
            GetConnectivityServiceListInput input) {
        // TODO Auto-generated method stub
        // Todo -> conflict with type of service (one is tapi create, the other one is tapi output).
        /*
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get.connectivity.service
                .list.output.Service> serviceList = new ArrayList<>();
        Iterator it = MappingUtils.getMapService().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            serviceList.add((org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.get
                    .connectivity.service.list.output.Service) pair.getValue());
        }

         */
        List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210
                .get.connectivity.service.list.output.Service> serviceList = this.mongoDbDataStoreService
                .getTapiServices("optical-topo");
        GetConnectivityServiceListOutput output = new GetConnectivityServiceListOutputBuilder()
                .setService(serviceList).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<GetConnectionEndPointDetailsOutput>> getConnectionEndPointDetails(
            GetConnectionEndPointDetailsInput input) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointDetailsOutput>> getServiceInterfacePointDetails(
            GetServiceInterfacePointDetailsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetServiceInterfacePointListOutput>> getServiceInterfacePointList(
            GetServiceInterfacePointListInput input) {
        // List<Sip> sipList = new ArrayList<>();
        /*
        Iterator it = MappingUtils.getMapSip().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            sipList.add((Sip) pair.getValue());
        }
         */

        GetServiceInterfacePointListOutput output = new GetServiceInterfacePointListOutputBuilder()
                .setSip(this.mongoDbDataStoreService.getTapiSips()).build();
        return RpcResultBuilder.success(output).buildFuture();
    }

    @Override
    public ListenableFuture<RpcResult<UpdateServiceInterfacePointOutput>> updateServiceInterfacePoint(
            UpdateServiceInterfacePointInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetSupportedNotificationTypesOutput>> getSupportedNotificationTypes(
            GetSupportedNotificationTypesInput input) {
        return null;
    }

    @Override
    public ListenableFuture<
            RpcResult<CreateNotificationSubscriptionServiceOutput>> createNotificationSubscriptionService(
            CreateNotificationSubscriptionServiceInput input) {
        // Todo --> here we will need to create a topic in kafka for this subscription if it doesnt exist
        return null;
    }

    @Override
    public ListenableFuture<
            RpcResult<UpdateNotificationSubscriptionServiceOutput>> updateNotificationSubscriptionService(
            UpdateNotificationSubscriptionServiceInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<
            DeleteNotificationSubscriptionServiceOutput>> deleteNotificationSubscriptionService(
            DeleteNotificationSubscriptionServiceInput input) {
        // Todo --> delete topic if there are no subscriptions to that topic
        return null;
    }

    @Override
    public ListenableFuture<
            RpcResult<GetNotificationSubscriptionServiceDetailsOutput>> getNotificationSubscriptionServiceDetails(
            GetNotificationSubscriptionServiceDetailsInput input) {
        return null;
    }

    @Override
    public ListenableFuture<
            RpcResult<GetNotificationSubscriptionServiceListOutput>> getNotificationSubscriptionServiceList(
            GetNotificationSubscriptionServiceListInput input) {
        return null;
    }

    @Override
    public ListenableFuture<RpcResult<GetNotificationListOutput>> getNotificationList(GetNotificationListInput input) {
        return null;
    }

    private void sendTapiNotification(NotificationType notificationType, Uuid uuid) {
        Notification notification = new NotificationBuilder().setNotificationType(notificationType)
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setUuid(uuid)
                .setAdditionalText("Service created :)")
                .build();
        /*
        this.kafkaProd.getProducer().send(new ProducerRecord<String, String>("test", notification.toString(),
                notification.toString()));

         */
        this.kafka.sendStream("test", notification);
        // notificationPublishService.putNotification(notification);
    }

    public boolean containsSip(final List<Sip> list, final Uuid sipUuid) {
        return list.stream().map(Sip::getUuid).anyMatch(sipUuid::equals);
    }
}
