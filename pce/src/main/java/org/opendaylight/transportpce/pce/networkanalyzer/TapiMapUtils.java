/*
 * Copyright © 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.networkutils.rev250902.OtnLinkType;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Context;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.LAYERPROTOCOLQUALIFIER;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.Context1;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.Connection;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.connectivity.context.ConnectionKey;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.context.ConnectivityContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU0;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU2E;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.ODUTYPEODUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTU4;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.OTUTYPEOTUCN;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.Link;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.context.TopologyContext;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.Topology;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.context.TopologyKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TapiMapUtils {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(TapiMapUtils.class);
    private final DataBroker dataBroker;
    private final NetworkTransactionService networkTransactionService;
    public static final ImmutableMap<String, OtnLinkType> SERV_TYPE_OTN_LINK_TYPE =
        ImmutableMap.<String, OtnLinkType>builder()
            .put(StringConstants.SERVICE_TYPE_ODU4, OtnLinkType.OTU4)
            .put(StringConstants.SERVICE_TYPE_ODUC4, OtnLinkType.OTUC4)
            .put(StringConstants.SERVICE_TYPE_ODUC3, OtnLinkType.OTUC3)
            .put(StringConstants.SERVICE_TYPE_ODUC2, OtnLinkType.OTUC2)
            .put(StringConstants.SERVICE_TYPE_1GE, OtnLinkType.ODU4)
            .put(StringConstants.SERVICE_TYPE_10GE, OtnLinkType.ODU4)
            //StringConstants.SERVICE_TYPE_400GE_M, OtnLinkType.OTUC8,
            //StringConstants.SERVICE_TYPE_400GE_S, OtnLinkType.OTUC8,
            //StringConstants.SERVICE_TYPE_800GE_M, OtnLinkType.OTUC16,
            //StringConstants.SERVICE_TYPE_800GE_S, OtnLinkType.OTUC16,
            .put(StringConstants.SERVICE_TYPE_100GE_M, OtnLinkType.ODU4)
            .put(StringConstants.SERVICE_TYPE_100GE_S, OtnLinkType.ODU4)
            .build();

    public static final ImmutableMap<OtnLinkType, LAYERPROTOCOLQUALIFIER> LPN_OR_TAPI =
        ImmutableMap.<OtnLinkType, LAYERPROTOCOLQUALIFIER>builder()
            .put(OtnLinkType.ODU4, ODUTYPEODU4.VALUE)
            .put(OtnLinkType.ODU2e, ODUTYPEODU2E.VALUE)
            .put(OtnLinkType.ODU0, ODUTYPEODU0.VALUE)
            .put(OtnLinkType.ODUC4, ODUTYPEODUCN.VALUE)
            .put(OtnLinkType.ODUC3, ODUTYPEODUCN.VALUE)
            .put(OtnLinkType.ODUC2, ODUTYPEODUCN.VALUE)
            .put(OtnLinkType.OTUC4, OTUTYPEOTUCN.VALUE)
            .put(OtnLinkType.OTUC3, OTUTYPEOTUCN.VALUE)
            .put(OtnLinkType.OTUC2, OTUTYPEOTUCN.VALUE)
            //OtnLinkType.OTUC8, OTUTYPEOTUCN.VALUE,
            //OtnLinkType.OTUC16, OTUTYPEOTUCN.VALUE,
            .put(OtnLinkType.OTU4, OTUTYPEOTU4.VALUE)
            .build();
    public static final ImmutableList<String> AUTHORIZED_SUFFIX_PREFIX = ImmutableList.<String>builder()
        .add("TOP").add("XC").add("eODU").add("iODU").add("OTSi_MEDIA_CHANNEL").add("OTS").add("iOTU")
        .build();

    public TapiMapUtils(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
        this.networkTransactionService = new NetworkTransactionImpl(this.dataBroker);
    }

    public static Set<String> getSRLG(Link link) {
        // List<RiskCharacteristic> rc = new ArrayList<>();
        Set<String> riskIdList = null;
        if (link.getRiskCharacteristic() != null && !link.getRiskCharacteristic().values().stream()
                .filter(rc -> rc.getRiskCharacteristicName().equalsIgnoreCase("SRLG")).collect(Collectors.toList())
                    .isEmpty()
                && link.getRiskCharacteristic().values().stream()
                    .filter(rc -> rc.getRiskCharacteristicName().equalsIgnoreCase("SRLG")).findFirst().orElseThrow()
                    .getRiskIdentifierList() != null) {
            riskIdList = link.getRiskCharacteristic().values().stream()
                .filter(rc -> rc.getRiskCharacteristicName().equalsIgnoreCase("SRLG")).findFirst().orElseThrow()
                .getRiskIdentifierList();
        } else {
            LOG.debug("TapiMapUtils : No SRLG available in the risk-characteristic of the link {} named {}",
                link.getUuid().toString(), link.getName().toString());
        }
        return riskIdList;
    }

    public static Double getAvailableBandwidth(Link link) {
        if (link.getAvailableCapacity().getTotalSize().getValue() == null) {
            LOG.warn("TapiMapUtils: no Available Bandwidth available for link {{} named {}", link.getUuid().toString(),
                link.getName().toString());
            return null;
        }
        return link.getAvailableCapacity().getTotalSize().getValue().doubleValue();
    }

    public static Double getUsedBandwidth(Link link) {
        if (link.getTotalPotentialCapacity().getTotalSize().getValue() == null
            || link.getAvailableCapacity().getTotalSize().getValue() == null) {
            LOG.warn("TapiMapUtils: incomplete Bandwidth information for link {{} named {}", link.getUuid().toString(),
                link.getName().toString());
            return null;
        }
        return ((link.getTotalPotentialCapacity().getTotalSize().getValue().doubleValue()
            - link.getAvailableCapacity().getTotalSize().getValue().doubleValue()));
    }

    public Uuid extractOppositeLink(Uuid tapiTopoUuid, String linkName, String srcNepCepName, String destNepCepName,
            boolean isOTN) {

        String suffix = "";
        for (int i = 0; i < 100; i++) {
            if (linkName.split("\\+")[i] != null && !linkName.split("\\+")[i].equals("")) {
                suffix = linkName.split("\\+")[i];
            } else {
                i = 100;
            }
        }

        String middleItem = "";
        middleItem = linkName.split(srcNepCepName + "+")[1];
        middleItem = (middleItem == null || middleItem.equals("")) ? linkName.split(srcNepCepName)[1] : middleItem;
        middleItem =
            (middleItem.split("+" + destNepCepName)[0] == null || middleItem.split("+" + destNepCepName)[0].equals(""))
                ? middleItem.split(destNepCepName)[0]
                : middleItem.split("+" + destNepCepName)[0];
        String prefix = linkName.split("\\+")[0];
        Map<Integer, String> nameMap = new HashMap<>(Map.of(
            0, prefix, 1, destNepCepName, 2, middleItem, 3, srcNepCepName, 4, suffix));
        String oppLinkName = "";
        for (Map.Entry<Integer, String> entry : nameMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().equals("")) {
                oppLinkName = (oppLinkName.equals(""))
                    ? entry.getValue() : String.join("+", oppLinkName, entry.getValue());
            }
        }
        Uuid linkOppositeUuid = new Uuid(UUID.nameUUIDFromBytes(oppLinkName.getBytes(StandardCharsets.UTF_8))
            .toString());
        if (isOTN) {
            // We have an OTN link (Connection)
            Optional<Connection> optConn;
            try {
                optConn = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(Context.class).augmentation(Context1.class)
                                .child(ConnectivityContext.class)
                                .child(Connection.class, new ConnectionKey(linkOppositeUuid))
                            .build())
                    .get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to find in Tapi Context/connectivity-context opposite connection {}", oppLinkName, e);
                return null;
            }
            if (!optConn.isPresent()) {
                LOG.error("Failed to find in Tapi Context/connectivity-context opposite connection {}", oppLinkName);
                return null;
            } else {
                return linkOppositeUuid;
            }
        } else {
            // We have a Photonic Link
            Optional<org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121.topology.Link> optLink;
            try {
                optLink = this.networkTransactionService.read(LogicalDatastoreType.OPERATIONAL,
                        DataObjectIdentifier.builder(Context.class)
                            .augmentation(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                 .Context1.class)
                            .child(TopologyContext.class)
                            .child(Topology.class, new TopologyKey(tapiTopoUuid))
                            .child(org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                .topology.Link.class,
                                new org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.topology.rev221121
                                .topology.LinkKey(linkOppositeUuid))
                            .build())
                    .get();
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to find in Topology {} opposite link {}", tapiTopoUuid, oppLinkName, e);
                return null;
            }
            if (!optLink.isPresent()) {
                LOG.error("Failed to find in Topology {} opposite link {}", tapiTopoUuid, oppLinkName);
                return null;
            } else {
                return linkOppositeUuid;
            }
        }
    }

}
