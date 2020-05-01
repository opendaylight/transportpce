/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapi.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.equipment.types.rev181130.OpticTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.node.types.rev181130.NodeIdType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.EthernetEncodingType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.MappingModeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.RxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.endpoint.TxDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.lgx.LgxBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.service.port.PortBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.format.rev190531.ServiceFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.AdministrativeState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.CapacityUnit;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LayerProtocolName;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.LifecycleState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.OperationalState;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.TotalSizeBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.AvailableCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.capacity.pac.TotalPotentialCapacityBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.Sip;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.get.service._interface.point.list.output.SipBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.Name;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.global._class.NameBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210.create.connectivity.service.output.Service;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.EndPointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.service._interface.points.ServiceEndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.service._interface.points.ServiceEndPointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.service._interface.points.service.end.point.EndPoint;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.service._interface.points.service.end.point.EndPointBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);

    private static Map<Uuid, GenericServiceEndpoint> map = new HashMap<Uuid, GenericServiceEndpoint>();
    private static Map<String, Uuid> mapHelper = new HashMap<String, Uuid>();
    private static Map<Uuid, Service> mapService = new HashMap<Uuid, Service>();
    private static Map<Uuid, Sip> mapSip = new HashMap<Uuid, Sip>();

    private MappingUtils() {
    }

    private static GenericServiceEndpoint createMapEntry(ServiceEndPoint sep) {
        if (sep.getEndPoint().getServiceEndPointType().equals(EndPointType.Aend)) {
            ServiceAEnd sepG = TapiUtils.buildServiceAEnd(sep.getEndPoint().getNodeId().getValue(), sep.getEndPoint()
                .getClli(), sep.getEndPoint().getTxDirection().getPort().getPortDeviceName(), sep.getEndPoint()
                    .getTxDirection().getPort().getPortName(),
                sep.getEndPoint().getRxDirection().getPort().getPortDeviceName(), sep.getEndPoint().getRxDirection()
                    .getPort().getPortName());
            return new GenericServiceEndpoint(sepG, ServiceEndpointType.SERVICEAEND);
        } else {
            ServiceZEnd sepG = TapiUtils.buildServiceZEnd(sep.getEndPoint().getNodeId().getValue(), sep.getEndPoint()
                .getClli(), sep.getEndPoint().getTxDirection().getPort().getPortDeviceName(), sep.getEndPoint()
                    .getTxDirection().getPort().getPortName(),
                sep.getEndPoint().getRxDirection().getPort().getPortDeviceName(), sep.getEndPoint().getRxDirection()
                    .getPort().getPortName());
            return new GenericServiceEndpoint(sepG, ServiceEndpointType.SERVICEZEND);
        }
    }

    public static void addMapSEP(ServiceEndPoint sep) {
        Uuid uuid = sep.getUuid();
        if (!map.containsKey(uuid)) {
            LOG.info("adding new entry {} into static map", uuid);
            map.put(uuid, MappingUtils.createMapEntry(sep));
        } else {
            LOG.error("service-end-point entry already exist. Please DELETE it before PUT it...");
        }
    }

    public static void deleteMapEntry(Uuid uuid) {
        if (map.containsKey(uuid)) {
            LOG.info("map size = {}", Integer.toString(map.size()));
            map.remove(uuid);
            LOG.info("map size = {}", Integer.toString(map.size()));
        } else {
            LOG.info("No existing entry with uuid {}", uuid);
        }
    }

    public static void afficheMap() {
        if (!map.isEmpty()) {
            Set<Entry<Uuid, GenericServiceEndpoint>> entries = map.entrySet();
            LOG.info("Displaying the static map...");
            for (Entry<Uuid, GenericServiceEndpoint> entry : entries) {
                LOG.info("uuid = {} \t service end point = {}", entry.getKey().toString(), entry.getValue().getValue()
                    .toString());
            }
        } else {
            LOG.info("Static map is empty - Nothing to display");
        }

    }

    public static void deleteMap() {
        if (!map.isEmpty()) {
            map.clear();
        } else {
            LOG.info("Static map is already empty - Nothing to delete");
        }
    }

    public static Map<Uuid, GenericServiceEndpoint> getMap() {
        return map;
    }

    public static Map<String, Uuid> getMapHelper() {
        return mapHelper;
    }

    public static Map<Uuid, Service> getMapService() {
        return mapService;
    }

    public static Map<Uuid, Sip> getMapSip() {
        return mapSip;
    }

    public static void updateMap(List<String> goodTpList) {
        // Todo: check if all goodTplist have a sip --> uuid in the map. Otherwise update is needed.
        // A and Z endpoints needed for each xpdr node ?? Right now only one direction to simplify
        for (String tpSIP:goodTpList) {
            Uuid uuid = new Uuid(UUID.nameUUIDFromBytes(tpSIP.getBytes()).toString());
            if (!mapHelper.containsKey(tpSIP)) {
                mapHelper.put(tpSIP, uuid);
            }
            if (!mapSip.containsKey(uuid)) {
                mapSip.put(uuid, buildSIP(uuid));
            }
        }
    }

    private static Sip buildSIP(Uuid uuid) {
        List<Name> nameList = new ArrayList<>();
        nameList.add(new NameBuilder().setValueName(getNodeOfSIP(uuid))
                .setValueName(UUID.nameUUIDFromBytes(MappingUtils.getNodeOfSIP(uuid).getBytes())
                        .toString())
                .build());


        return new SipBuilder().setUuid(uuid)
                .setAdministrativeState(AdministrativeState.UNLOCKED)
                .setLayerProtocolName(LayerProtocolName.ETH)
                .setLifecycleState(LifecycleState.INSTALLED)
                .setOperationalState(OperationalState.ENABLED)
                .setAvailableCapacity(new AvailableCapacityBuilder()
                        .setTotalSize(new TotalSizeBuilder()
                                .setUnit(CapacityUnit.GBPS)
                                .setValue(Uint64.valueOf(100))
                                .build())
                        .build())
                .setTotalPotentialCapacity(new TotalPotentialCapacityBuilder()
                        .setTotalSize(new TotalSizeBuilder()
                                .setUnit(CapacityUnit.GBPS)
                                .setValue(Uint64.valueOf(100))
                                .build())
                        .build())
                .setSupportedLayerProtocolQualifier(null)
                .setName(nameList)
                .build();
    }

    public static void createSEPsFromConnectivityRequest(
            List<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210
                    .create.connectivity.service.input.EndPoint> endPoint) {
        int prevEndpointtype = 0;
        int newEndpointtype = 0;
        for (org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev181210
                     .create.connectivity.service.input.EndPoint ep:endPoint) {
            Uuid uuid = ep.getServiceInterfacePoint().getServiceInterfacePointUuid();
            if (getMapHelper().containsValue(uuid)) {
                // todo: this only works when 2 sips are provided in the service request
                if (!map.containsKey(uuid)) {
                    switch (prevEndpointtype) {
                        case 1:
                            newEndpointtype = 2;
                            break;
                        case 2:
                            newEndpointtype = 1;
                            break;
                        default:
                            newEndpointtype = (int) (Math.random() * 2 + 1);
                    }
                    LOG.info("adding new entry {} into static map", uuid);
                    map.put(uuid, MappingUtils.createMapEntry(buildSEP(uuid, newEndpointtype)));
                    prevEndpointtype = newEndpointtype;
                } else {
                    prevEndpointtype = map.get(uuid).getType().getIntValue();
                    LOG.error("service-end-point entry already exist. Please DELETE it before PUT it...");
                }
            } else {
                LOG.error("SIP with uuid {} doesnt exist in topology", uuid);
            }
        }
    }

    private static ServiceEndPoint buildSEP(Uuid uuid, int newEndpointtype) {
        RxDirection rxDirection = new RxDirectionBuilder().setPort(new PortBuilder()
                .setPortDeviceName("ROUTER_SNJSCAMCJP8_000000.00_00").setPortType("router").setPortRack("000000.00")
                .setPortShelf("00").setPortName("Gigabit Ethernet_Rx.ge-5/0/0.0").build()).setLgx(new LgxBuilder()
                .setLgxDeviceName("LGX Panel_SNJSCAMCJP8_000000.00_00").setLgxPortName("LGX Back.4")
                .setLgxPortRack("000000.00").setLgxPortShelf("00").build()).build();
        TxDirection txDirection = new TxDirectionBuilder().setPort(new PortBuilder()
                .setPortDeviceName("ROUTER_SNJSCAMCJP8_000000.00_00").setPortType("router").setPortRack("000000.00")
                .setPortShelf("00").setPortName("Gigabit Ethernet_Tx.ge-5/0/0.0").build()).setLgx(new LgxBuilder()
                .setLgxDeviceName("LGX Panel_SNJSCAMCJP8_000000.00_00").setLgxPortName("LGX Back.3")
                .setLgxPortRack("000000.00").setLgxPortShelf("00").build()).build();
        EndPoint endPoint = new EndPointBuilder()
                .setNodeId(NodeIdType.getDefaultInstance(getNodeOfSIP(uuid)))
                .setClli("CLLI-" + getNodeOfSIP(uuid))
                .setEthernetEncoding(EthernetEncodingType._10GBASER)
                .setMappingMode(MappingModeType.GFPF)
                .setOduServiceRate(null)
                .setOpticType(OpticTypes.Gray)
                .setOtherServiceFormatAndRate("Ethernet100")
                .setOtuServiceRate(null)
                .setRouter(null)
                .setRxDirection(rxDirection)
                .setServiceEndPointType(EndPointType.forValue(newEndpointtype)) // decide how to build this
                .setServiceFormat(ServiceFormat.Ethernet)
                .setServiceRate(Uint32.valueOf(100))
                .setSubrateEthSla(null)
                .setTxDirection(txDirection)
                .setUserLabel("")
                .build();

        return new ServiceEndPointBuilder().setUuid(uuid).setEndPoint(endPoint).build();
    }

    public static String getNodeOfSIP(Uuid uuid) {
        String node = null;
        for (Map.Entry entry: mapHelper.entrySet()) {
            if (uuid.equals(entry.getValue())) {
                String key = entry.getKey().toString();
                node = key.split("-")[0];
                LOG.info("Node from uuid {} is: {}", uuid, node);
                break; //breaking because its one to one map
            }
        }
        return node;
    }


}
