/*
 * Copyright © 2021 Nokia, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.local._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.local._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ProtectionRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.constraint.RequestedCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.ConnectivityConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.input.EndPointKey;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;

public final class TapiConnectivityDataUtils {
    public static CreateConnectivityServiceInput buildConnServiceCreateInput() {

        EndPoint endPoint1 = getEndPoint1Builder().build();
        EndPoint endPoint2 = getEndPoint2Builder().build();
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        endPointMap.put(endPoint1.key(), endPoint1);
        endPointMap.put(endPoint2.key(), endPoint2);

        CreateConnectivityServiceInputBuilder inputBuilder = new CreateConnectivityServiceInputBuilder()
            .setEndPoint(endPointMap)
            .setConnectivityConstraint(new ConnectivityConstraintBuilder().setServiceLayer(LayerProtocolName.DSR)
                .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).setServiceLevel("some service-level")
                .setRequestedCapacity(new RequestedCapacityBuilder()
                    .setTotalSize(new TotalSizeBuilder().setUnit(CapacityUnit.GBPS)
                        .setValue(Uint64.valueOf(10)).build()).build()).build())
            .setState("some state");

        return inputBuilder.build();
    }

    public static DeleteConnectivityServiceInput buildConnServiceDeleteInput() {
        DeleteConnectivityServiceInputBuilder inputBuilder = new DeleteConnectivityServiceInputBuilder()
            .setServiceIdOrName(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString());

        return inputBuilder.build();
    }

    private static EndPointBuilder getEndPoint2Builder() {
        Name name = new NameBuilder().setValueName("OpenROADM node id").setValue("SPDR-SC1-XPDR1").build();
        return new EndPointBuilder().setLayerProtocolName(LayerProtocolName.DSR)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setDirection(PortDirection.BIDIRECTIONAL)
            .setRole(PortRole.SYMMETRIC)
            .setProtectionRole(ProtectionRole.NA)
            .setLocalId("SPDR-SC1-XPDR1")
            .setName(Map.of(name.key(), name))
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(
                new Uuid("25812ef2-625d-3bf8-af55-5e93946d1c22")).build());
    }

    private static EndPointBuilder getEndPoint1Builder() {
        Name name = new NameBuilder().setValueName("OpenROADM node id").setValue("SPDR-SA1-XPDR1").build();
        return new EndPointBuilder().setLayerProtocolName(LayerProtocolName.DSR)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setDirection(PortDirection.BIDIRECTIONAL)
            .setRole(PortRole.SYMMETRIC)
            .setProtectionRole(ProtectionRole.NA)
            .setLocalId("SPDR-SA1-XPDR1")
            .setName(Map.of(name.key(), name))
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(
                new Uuid("c14797a0-adcc-3875-a1fe-df8949d1a2d7")).build());
    }

    public static ServiceCreateInput buildServiceCreateInput() {
        ServiceAEnd serviceAEnd = getServiceAEndBuild().build();
        ServiceZEnd serviceZEnd = getServiceZEndBuild().build();

        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString())
            .setServiceAEnd(serviceAEnd)
            .setServiceZEnd(serviceZEnd)
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build());

        return builtInput.build();
    }

    public static ServiceAEndBuilder getServiceAEndBuild() {
        return new ServiceAEndBuilder()
            .setClli("NodeSA").setServiceFormat(ServiceFormat.Ethernet).setServiceRate(Uint32.valueOf(10))
            .setNodeId(new NodeIdType("SPDR-SA1"))
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection());
    }

    public static ServiceZEndBuilder getServiceZEndBuild() {
        return new ServiceZEndBuilder()
            .setClli("NodeSC").setServiceFormat(ServiceFormat.Ethernet).setServiceRate(Uint32.valueOf(10))
            .setNodeId(new NodeIdType("SPDR-SC1"))
            .setTxDirection(getTxDirection())
            .setRxDirection(getRxDirection());
    }

    private static TxDirection getTxDirection() {
        return new TxDirectionBuilder().setPort(new PortBuilder().setPortDeviceName("device name")
            .setPortName("port name").setPortRack("port rack").setPortShelf("port shelf")
            .setPortSlot("port slot").setPortSubSlot("port subslot").setPortType("port type").build())
            .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name").setLgxPortName("lgx port name")
                .setLgxPortRack("lgx port rack").setLgxPortShelf("lgx port shelf").build())
            .build();
    }

    private static RxDirection getRxDirection() {
        return new RxDirectionBuilder()
            .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                .setPortSubSlot("port subslot").setPortType("port type").build())
            .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                .setLgxPortShelf("lgx port shelf").build())
            .build();
    }

    public static Map<ConnectivityServiceKey, ConnectivityService> createConnService() {
        EndPoint endPoint1 = getEndPoint1Builder().build();
        EndPoint endPoint2 = getEndPoint2Builder().build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint
            endPoint11 = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder(endPoint1).build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint
            endPoint12 = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder(endPoint2).build();

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint>
                endPointMap = new HashMap<>();
        endPointMap.put(endPoint11.key(), endPoint11);
        endPointMap.put(endPoint12.key(), endPoint12);

        Map<ConnectionKey, Connection> connectionMap = new HashMap<>();
        Connection connection = new ConnectionBuilder().setConnectionUuid(new Uuid(UUID.randomUUID().toString()))
            .build();
        connectionMap.put(connection.key(), connection);

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name name =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder()
                .setValueName("Connectivity Service Name").setValue("service 1")
                .build();
        ConnectivityService connServ = new ConnectivityServiceBuilder()
            .setAdministrativeState(AdministrativeState.LOCKED)
            .setOperationalState(OperationalState.DISABLED)
            .setLifecycleState(LifecycleState.PLANNED)
            .setUuid(new Uuid(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString()))
            .setServiceLayer(LayerProtocolName.DSR)
            .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY)
            .setConnectivityDirection(ForwardingDirection.BIDIRECTIONAL)
            .setName(Map.of(name.key(), name))
            .setConnection(connectionMap)
            .setEndPoint(endPointMap)
            .build();
        Map<ConnectivityServiceKey, ConnectivityService> connMap = new HashMap<>();
        connMap.put(connServ.key(), connServ);
        return connMap;
    }

    public static DeleteConnectivityServiceInput buildConnServiceDeleteInput1() {
        DeleteConnectivityServiceInputBuilder inputBuilder = new DeleteConnectivityServiceInputBuilder()
            .setServiceIdOrName("random-service");
        return inputBuilder.build();
    }

    private TapiConnectivityDataUtils() {
    }
}
