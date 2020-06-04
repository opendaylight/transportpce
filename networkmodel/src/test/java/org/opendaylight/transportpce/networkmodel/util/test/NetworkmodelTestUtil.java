/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.util.test;


import java.util.ArrayList;
import java.util.List;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200429.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PortQual;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev181019.SupportedIfCapability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NetworkmodelTestUtil {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkmodelTestUtil.class);

    public static Nodes createMappingForRdm(String nodeId, String clli, int degNb, int srgNb) {
        List<Mapping> mappingList = new ArrayList<>();
        createDegreeMappings(mappingList, 1, degNb);
        createSrgMappings(mappingList, 1, srgNb);
        Nodes mappingNode = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Rdm).setNodeClli(clli).build())
            .setMapping(mappingList)
            .build();
        return mappingNode;
    }

    public static Nodes createMappingForXpdr(String nodeId, String clli, int networkPortNb, int clientPortNb,
        XpdrNodeTypes xpdrNodeType) {
        List<Mapping> mappingList = new ArrayList<>();
        createXpdrMappings(mappingList, networkPortNb, clientPortNb, xpdrNodeType);
        Nodes mappingNode = new NodesBuilder()
            .setNodeId(nodeId)
            .setNodeInfo(new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).setNodeClli(clli).build())
            .setMapping(mappingList)
            .build();
        LOG.info("mapping = {}", mappingNode.toString());
        return mappingNode;
    }

    private static List<Mapping> createDegreeMappings(List<Mapping> mappingList, int degNbStart, int degNbStop) {
        for (int i = degNbStart; i <= degNbStop; i++) {
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("DEG" + i + "-TTP-TXRX")
                .setPortDirection("bidirectional")
                .setSupportingPort("L1")
                .setSupportingCircuitPackName(i + "/0")
                .setSupportingOts("OTS-DEG" + i + "-TTP-TXRX")
                .setSupportingOms("OMS-DEG" + i + "-TTP-TXRX")
                .build();
            mappingList.add(mapping);
        }
        return mappingList;
    }

    private static List<Mapping> createSrgMappings(List<Mapping> mappingList, int srgNbStart, int srgNbStop) {
        for (int i = srgNbStart; i <= srgNbStop; i++) {
            for (int j = 1; j <= 4; j++) {
                Mapping mapping = new MappingBuilder()
                    .setLogicalConnectionPoint("SRG" + i + "-PP" + j + "-TXRX")
                    .setPortDirection("bidirectional")
                    .setSupportingPort("C" + j)
                    .setSupportingCircuitPackName(3 + i + "/0")
                    .build();
                mappingList.add(mapping);
            }
        }
        return mappingList;
    }

    private static List<Mapping> createXpdrMappings(List<Mapping> mappingList, int networkPortNb, int clientPortNb,
        XpdrNodeTypes xpdrNodeType) {
        for (int i = 1; i <= networkPortNb; i++) {
            List<Class<? extends SupportedIfCapability>> supportedIntf = new ArrayList<>();
            supportedIntf.add(IfOCH.class);
            MappingBuilder mappingBldr = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-NETWORK" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("1")
                .setSupportedInterfaceCapability(supportedIntf)
                .setConnectionMapLcp("XPDR1-CLIENT" + i)
                .setPortQual(PortQual.XpdrNetwork.getName())
                .setSupportingCircuitPackName("1/0/" + i + "-PLUG-NET");
            if (xpdrNodeType != null) {
                mappingBldr.setXponderType(xpdrNodeType);
            }
            mappingList.add(mappingBldr.build());
        }
        for (int i = 1; i <= clientPortNb; i++) {
            List<Class<? extends SupportedIfCapability>> supportedIntf = new ArrayList<>();
            supportedIntf.add(If100GE.class);
            Mapping mapping = new MappingBuilder()
                .setLogicalConnectionPoint("XPDR1-CLIENT" + i)
                .setPortDirection("bidirectional")
                .setSupportingPort("C1")
                .setSupportedInterfaceCapability(supportedIntf)
                .setConnectionMapLcp("XPDR1-NETWORK" + i)
                .setPortQual(PortQual.XpdrClient.getName())
                .setSupportingCircuitPackName("1/0/" + i + "-PLUG-CLIENT")
                .build();
            mappingList.add(mapping);
        }
        return mappingList;
    }

    private NetworkmodelTestUtil() {
    }
}
