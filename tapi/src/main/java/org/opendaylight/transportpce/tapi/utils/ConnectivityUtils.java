/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.HashMap;
import java.util.Map;
import org.opendaylight.transportpce.servicehandler.service.ServiceHandlerOperations;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev181130.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.ConnectionType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.RpcActions;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.sdnc.request.header.SdncRequestHeaderBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
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
//import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev190531.network.topology.AToZ;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev190531.network.topology.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.PathDescription;
//import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description
// .ztoa.direction.ZToA;
//import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev201210.path.description
// .ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.service.path.list.ServicePaths;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.context.ConnectivityServiceBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPoint;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.connectivity.service.EndPointKey;
import org.opendaylight.yangtools.yang.common.Uint32;

public final class ConnectivityUtils {

    private static final String LGX_PORT_NAME = "Some lgx-port-name";
    private static final String PORT_TYPE = "some port type";
    private static final String LGX_DEVICE_NAME = "Some lgx-device-name";
    private static final String PORT_RACK_VALUE = "000000.00";
    private static final String DSR = "DSR";
    private static final String OTSI = "OTSi";
    private static final String E_OTSI = "eOTSi";
    private static final String I_OTSI = "iOTSi";
    private static final String RDM_INFRA = "ROADM-infra";
    private static final String PHTNC_MEDIA = "Photonic Media";
    private final ServiceHandlerOperations serviceHandler;

    public ConnectivityUtils(ServiceHandlerOperations serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    public static ServiceCreateInput buildServiceCreateInput(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        ServiceCreateInputBuilder builtInput = new ServiceCreateInputBuilder();
        ServiceAEnd serviceAEnd = getServiceAEnd(sepA, sepZ);
        ServiceZEnd serviceZEnd = getServiceZEnd(sepA, sepZ);
        if (serviceAEnd != null && serviceZEnd != null) {
            builtInput.setCommonId("commonId");
            builtInput.setConnectionType(ConnectionType.Service);
            builtInput.setCustomer("Customer");
            builtInput.setServiceName("service test");
            builtInput.setServiceAEnd(serviceAEnd);
            builtInput.setServiceZEnd(serviceZEnd);
            builtInput.setSdncRequestHeader(new SdncRequestHeaderBuilder().setRequestId("request-1")
                .setRpcAction(RpcActions.ServiceCreate).setNotificationUrl("notification url").setRequestSystemId(
                    "appname")
                .build());
        } else {
            return null;
        }
        return builtInput.build();
    }

    public static ServiceAEnd buildServiceAEnd(String nodeid, String clli, String txPortDeviceName,
        String txPortName, String rxPortDeviceName, String rxPortName) {
        return new ServiceAEndBuilder()
                .setClli(clli)
                .setNodeId(new NodeIdType(nodeid))
                .setOpticType(OpticTypes.Gray)
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(txPortDeviceName)
                                .setPortName(txPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .setRxDirection(new RxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(rxPortDeviceName)
                                .setPortName(rxPortName)
                        .setPortRack(PORT_RACK_VALUE)
                        .setPortShelf("00")
                        .setPortType(PORT_TYPE)
                        .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .build();
    }

    public static ServiceZEnd buildServiceZEnd(String nodeid, String clli, String txPortDeviceName,
        String txPortName, String rxPortDeviceName, String rxPortName) {
        return  new ServiceZEndBuilder().setClli(clli).setNodeId(new NodeIdType(nodeid))
                .setOpticType(OpticTypes.Gray)
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setTxDirection(new TxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(txPortDeviceName)
                                .setPortName(txPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .setRxDirection(new RxDirectionBuilder()
                        .setPort(new PortBuilder()
                                .setPortDeviceName(rxPortDeviceName)
                                .setPortName(rxPortName)
                                .setPortRack(PORT_RACK_VALUE)
                                .setPortShelf("00")
                                .setPortType(PORT_TYPE)
                                .build())
                        .setLgx(new LgxBuilder()
                                .setLgxDeviceName(LGX_DEVICE_NAME)
                                .setLgxPortName(LGX_PORT_NAME)
                                .setLgxPortRack(PORT_RACK_VALUE)
                                .setLgxPortShelf("00")
                                .build())
                        .build())
                .build();
    }

    private static ServiceAEnd getServiceAEnd(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        if (sepA.getType().equals(ServiceEndpointType.SERVICEAEND)) {
            return new ServiceAEndBuilder(sepA.getValue()).build();
        } else if (sepZ.getType().equals(ServiceEndpointType.SERVICEAEND)) {
            return new ServiceAEndBuilder(sepZ.getValue()).build();
        } else {
            return null;
        }
    }

    private static ServiceZEnd getServiceZEnd(GenericServiceEndpoint sepA, GenericServiceEndpoint sepZ) {
        if (sepA.getType().equals(ServiceEndpointType.SERVICEZEND)) {
            return new ServiceZEndBuilder(sepA.getValue()).build();
        } else if (sepZ.getType().equals(ServiceEndpointType.SERVICEZEND)) {
            return new ServiceZEndBuilder(sepZ.getValue()).build();
        } else {
            return null;
        }
    }

    public ConnectivityService mapORServiceToTapiConnectivity(Service service) {
        // Get service path with the description in OR based models.
        ServicePaths servicePaths = this.serviceHandler.getServicePathDescription(service.getServiceName());
        PathDescription pathDescription = servicePaths.getPathDescription();
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd serviceAEnd
                = service.getServiceAEnd();
        EndPoint endPoint1 = mapServiceAEndPoint(serviceAEnd, pathDescription);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd serviceZEnd
                = service.getServiceZEnd();
        EndPoint endPoint2 = mapServiceZEndPoint(serviceZEnd, pathDescription);
        Map<EndPointKey, EndPoint> endPointMap = new HashMap<>();
        endPointMap.put(endPoint1.key(), endPoint1);
        endPointMap.put(endPoint2.key(), endPoint2);
        // Start of connectivity Service builder
        ConnectivityServiceBuilder conSerBldr = new ConnectivityServiceBuilder().setEndPoint(endPointMap);

        return conSerBldr.build();
    }

    private EndPoint mapServiceZEndPoint(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceZEnd
                    serviceZEnd, PathDescription pathDescription) {
        EndPointBuilder endPointBuilder = new EndPointBuilder();
        // 1. Service Format: ODU, OTU, ETH
        // ServiceFormat serviceFormat = serviceZEnd.getServiceFormat();
        // 2. Path description: get the z end point complete id and build the corresponding TAPI conversion
        // 3. Build the SIP name
        // String sipId = getSipIdFromZend(pathDescription.getZToADirection().getZToA());

        // SIP recognition --> need to match SIP name. SIP name =

        return endPointBuilder.build();
    }

    private EndPoint mapServiceAEndPoint(
            org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.ServiceAEnd
                    serviceAEnd, PathDescription pathDescription) {
        EndPointBuilder endPointBuilder = new EndPointBuilder();
        // SIP recognition --> need to match SIP name

        return endPointBuilder.build();
    }
/*
    private String getSipIdFromZend(Map<ZToAKey, ZToA> mapztoa) {


        return null;
    }

    private String getSipIdFromAend(Map<AToZKey, AToZ> mapatoz) {

        return null;
    }

 */
}
