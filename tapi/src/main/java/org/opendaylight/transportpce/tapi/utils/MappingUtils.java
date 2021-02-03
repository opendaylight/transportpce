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
import java.util.Map.Entry;
import java.util.Set;
import org.opendaylight.transportpce.tapi.connectivity.ConnectivityUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceAEnd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.service.create.input.ServiceZEnd;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev181210.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.EndPointType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev180928.service._interface.points.ServiceEndPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtils.class);

    private static Map<Uuid, GenericServiceEndpoint> map = new HashMap<>();

    private MappingUtils() {
    }

    private static GenericServiceEndpoint createMapEntry(ServiceEndPoint sep) {
        if (sep.getEndPoint().getServiceEndPointType().equals(EndPointType.Aend)) {
            ServiceAEnd sepG = ConnectivityUtils.buildServiceAEnd(sep.getEndPoint().getNodeId().getValue(),
                    sep.getEndPoint().getClli(), sep.getEndPoint().getTxDirection().getPort().getPortDeviceName(),
                    sep.getEndPoint().getTxDirection().getPort().getPortName(),
                sep.getEndPoint().getRxDirection().getPort().getPortDeviceName(), sep.getEndPoint().getRxDirection()
                    .getPort().getPortName());
            return new GenericServiceEndpoint(sepG, ServiceEndpointType.SERVICEAEND);
        } else {
            ServiceZEnd sepG = ConnectivityUtils.buildServiceZEnd(sep.getEndPoint().getNodeId().getValue(),
                    sep.getEndPoint().getClli(), sep.getEndPoint().getTxDirection().getPort().getPortDeviceName(),
                    sep.getEndPoint().getTxDirection().getPort().getPortName(),
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
                LOG.info("uuid = {} \t service end point = {}", entry.getKey(), entry.getValue().getValue());
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

}
