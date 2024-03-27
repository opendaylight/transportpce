/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.networkanalyzer.MapUtils;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.utils.NodeUtils;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;

public class PcePathDescriptionTests extends AbstractTest {

    private PcePathDescription pcePathDescription;
    private PceResult pceResult;
    private PceLink pceLink = null;
    private Link link = null;
    private Node node = null;
    private String deviceNodeId = "device node";
    private String serviceType = "100GE";
    @Mock
    private PortMapping portMapping;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Build Link
        link = NodeUtils
            .createRoadmToRoadm("OpenROADM-3-2-DEG1", "OpenROADM-3-1-DEG1", "DEG1-TTP-TX", "DEG1-TTP-RX")
            .build();

        //  Link link=genereateLinkBuilder();

        NodeBuilder node1Builder = NodeUtils.getNodeBuilder(NodeUtils.geSupportingNodes());
        node = node1Builder.setNodeId(new NodeId("test")).build();
        PceOpticalNode pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.SRG, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                GridConstant.SLOT_WIDTH_50,
                GridConstant.SLOT_WIDTH_50);
        PceOpticalNode pceOpticalNode2 = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.SRG, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1,
                GridConstant.SLOT_WIDTH_50,
                GridConstant.SLOT_WIDTH_50);

        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode2);
        pceLink.setClientA("XPONDER-CLIENT");

        pceResult = new PceResult();
        pceResult.success();
        pceResult.setRate(Long.valueOf(1));
        pceResult.setServiceType(StringConstants.SERVICE_TYPE_100GE_T);
        pceResult.setMaxFreq(new BigDecimal("195.900"));
        pceResult.setMinFreq(new BigDecimal("191.101"));
        Map<LinkId, PceLink> map = Map.of(
            new LinkId("OpenROADM-3-2-DEG1-DEG1-TTP-TXtoOpenROADM-3-1-DEG1-DEG1-TTP-RX"), pceLink,
            new LinkId("OpenROADM-3-1-DEG1-DEG1-TTP-RXtoOpenROADM-3-2-DEG1-DEG1-TTP-TX"), pceLink);
        pcePathDescription = new PcePathDescription(List.of(pceLink), map, pceResult);
    }

    @Test
    void buildDescriptionsTest() {
        pcePathDescription.buildDescriptions();
        assertEquals(pcePathDescription.getReturnStructure().getMessage(), "Path is calculated by PCE");
    }

    @Test
    void mapUtil() {
        PceConstraints pceConstraintsCalc = new PceConstraintsCalc(
            PceTestData.getPCERequest(), new NetworkTransactionImpl(getDataBroker())).getPceHardConstraints();
        MapUtils.mapDiversityConstraints(List.of(node), List.of(link), pceConstraintsCalc);
        MapUtils.getSupLink(link);
        MapUtils.getAllSupNode(node);
        MapUtils.getSRLGfromLink(link);
    }
}
