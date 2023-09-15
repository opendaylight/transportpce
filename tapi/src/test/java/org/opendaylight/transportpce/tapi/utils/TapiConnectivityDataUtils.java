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
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev210528.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.RxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.endpoint.TxDirectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev230526.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev191129.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.ServiceCreateInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceAEndBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev230526.service.create.input.ServiceZEndBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.CAPACITYUNITGBPS;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Direction;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.ForwardingDirection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.PortRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.local._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.CreateConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceInput;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.DeleteConnectivityServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ProtectionRole;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.ServiceType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.constraint.RequestedCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectivityServiceKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectionBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.ConnectivityConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.end.point.ServiceInterfacePointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.EndPointKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.TopologyConstraint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.TopologyConstraintBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.create.connectivity.service.input.TopologyConstraintKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;

public final class TapiConnectivityDataUtils {
    public static CreateConnectivityServiceInput buildConnServiceCreateInput() {

        EndPoint endPoint1 = getEndPoint1Builder().build();
        EndPoint endPoint2 = getEndPoint2Builder().build();
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        endPointMap.put(endPoint1.key(), endPoint1);
        endPointMap.put(endPoint2.key(), endPoint2);

        return new CreateConnectivityServiceInputBuilder()
            .setEndPoint(endPointMap)
            .setLayerProtocolName(LayerProtocolName.DSR)
            .setConnectivityConstraint(new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121
                        .create.connectivity.service.input.ConnectivityConstraintBuilder()
                    .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY).setServiceLevel("some service-level")
                    .setRequestedCapacity(new RequestedCapacityBuilder()
                    .setTotalSize(new TotalSizeBuilder().setUnit(CAPACITYUNITGBPS.VALUE)
                        .setValue(Decimal64.valueOf("10")).build()).build()).build())
            .setState(AdministrativeState.UNLOCKED)
            .setTopologyConstraint(getTopoConstraintMap())
            .build();
    }

    public static DeleteConnectivityServiceInput buildConnServiceDeleteInput() {
        return new DeleteConnectivityServiceInputBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString()))
            .build();
    }

    private static EndPointBuilder getEndPoint2Builder() {
        Name name = new NameBuilder().setValueName("OpenROADM node id").setValue("SPDR-SC1-XPDR1").build();
        return new EndPointBuilder().setLayerProtocolName(LayerProtocolName.DSR)
            .setAdministrativeState(AdministrativeState.UNLOCKED)
            .setOperationalState(OperationalState.ENABLED)
            .setDirection(Direction.BIDIRECTIONAL)
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
            .setDirection(Direction.BIDIRECTIONAL)
            .setRole(PortRole.SYMMETRIC)
            .setProtectionRole(ProtectionRole.NA)
            .setLocalId("SPDR-SA1-XPDR1")
            .setName(Map.of(name.key(), name))
            .setServiceInterfacePoint(new ServiceInterfacePointBuilder().setServiceInterfacePointUuid(
                new Uuid("c14797a0-adcc-3875-a1fe-df8949d1a2d7")).build());
    }

    public static ServiceCreateInput buildServiceCreateInput() {
        return new ServiceCreateInputBuilder()
            .setCommonId("commonId")
            .setConnectionType(ConnectionType.Service)
            .setCustomer("Customer")
            .setServiceName(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString())
            .setServiceAEnd(getServiceAEndBuild().build())
            .setServiceZEnd(getServiceZEndBuild().build())
            .setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request 1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").build())
            .build();
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

    private static Map<TxDirectionKey, TxDirection> getTxDirection() {
        return Map.of(new TxDirectionKey(Uint8.ZERO), new TxDirectionBuilder()
            .setPort(new PortBuilder().setPortDeviceName("device name")
            .setPortName("port name").setPortRack("port rack").setPortShelf("port shelf")
            .setPortSlot("port slot").setPortSubSlot("port subslot").setPortType("port type").build())
            .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name").setLgxPortName("lgx port name")
                .setLgxPortRack("lgx port rack").setLgxPortShelf("lgx port shelf").build())
            .setIndex(Uint8.ZERO)
            .build());
    }

    private static Map<RxDirectionKey, RxDirection> getRxDirection() {
        return Map.of(new RxDirectionKey(Uint8.ZERO), new RxDirectionBuilder()
            .setPort(new PortBuilder().setPortDeviceName("device name").setPortName("port name")
                .setPortRack("port rack").setPortShelf("port shelf").setPortSlot("port slot")
                .setPortSubSlot("port subslot").setPortType("port type").build())
            .setLgx(new LgxBuilder().setLgxDeviceName("lgx device name")
                .setLgxPortName("lgx port name").setLgxPortRack("lgx port rack")
                .setLgxPortShelf("lgx port shelf").build())
            .setIndex(Uint8.ZERO)
            .build());
    }

    private static Map<TopologyConstraintKey, TopologyConstraint> getTopoConstraintMap() {
        Map<TopologyConstraintKey, TopologyConstraint> topoConstraintMap = new HashMap<>();
        TopologyConstraint topoConstraint = new TopologyConstraintBuilder()
            .setLocalId("localIdTopoConstraint").build();
        topoConstraintMap.put(topoConstraint.key(), topoConstraint);
        return topoConstraintMap;
    }

    public static Map<ConnectivityServiceKey, ConnectivityService> createConnService() {
        EndPoint endPoint1 = getEndPoint1Builder().build();
        EndPoint endPoint2 = getEndPoint2Builder().build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint
            endPoint11 = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointBuilder(endPoint1).build();

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint
            endPoint12 = new org.opendaylight.yang.gen.v1.urn
                .onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointBuilder(endPoint2).build();

        Map<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPointKey,
            org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.service.EndPoint>
                endPointMap = new HashMap<>();
        endPointMap.put(endPoint11.key(), endPoint11);
        endPointMap.put(endPoint12.key(), endPoint12);

        Map<ConnectionKey, Connection> connectionMap = new HashMap<>();
        Connection connection = new ConnectionBuilder().setConnectionUuid(new Uuid(UUID.randomUUID().toString()))
            .build();
        connectionMap.put(connection.key(), connection);

        org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.Name name =
            new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.global._class.NameBuilder()
                .setValueName("Connectivity Service Name").setValue("service 1")
                .build();
        ConnectivityConstraint conCons = new ConnectivityConstraintBuilder()
            .setServiceLevel(null)
            .setServiceType(ServiceType.POINTTOPOINTCONNECTIVITY)
            .build();
        ConnectivityService connServ = new ConnectivityServiceBuilder()
            .setAdministrativeState(AdministrativeState.LOCKED)
            .setOperationalState(OperationalState.DISABLED)
            .setLifecycleState(LifecycleState.PLANNED)
            .setUuid(new Uuid(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString()))
            .setLayerProtocolName(LayerProtocolName.DSR)
            .setConnectivityConstraint(conCons)
            .setDirection(ForwardingDirection.BIDIRECTIONAL)
            .setName(Map.of(name.key(), name))
            .setConnection(connectionMap)
            .setEndPoint(endPointMap)
            .build();
        Map<ConnectivityServiceKey, ConnectivityService> connMap = new HashMap<>();
        connMap.put(connServ.key(), connServ);
        return connMap;
    }

    public static DeleteConnectivityServiceInput buildConnServiceDeleteInput1() {
        return new DeleteConnectivityServiceInputBuilder()
            .setUuid(new Uuid(UUID.nameUUIDFromBytes("service 1".getBytes(StandardCharsets.UTF_8)).toString()))
            .build();
    }

    private TapiConnectivityDataUtils() {
    }
}
