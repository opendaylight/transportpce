/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.constraints.PceConstraints;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.networkanalyzer.MapUtils;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceOpticalNode;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.transportpce.pce.utils.NodeUtils;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev200529.OpenroadmNodeType;
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

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Build Link
        link = NodeUtils.createRoadmToRoadm("OpenROADM-3-2-DEG1",
                "OpenROADM-3-1-DEG1",
                "DEG1-TTP-TX", "DEG1-TTP-RX").build();

        //  Link link=genereateLinkBuilder();

        NodeBuilder node1Builder = NodeUtils.getNodeBuilder(NodeUtils.geSupportingNodes());
        node = node1Builder.setNodeId(new NodeId("test")).build();
        PceOpticalNode pceOpticalNode = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.SRG, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, GridConstant.SLOT_WIDTH_50,
                GridConstant.SLOT_WIDTH_50);
        PceOpticalNode pceOpticalNode2 = new PceOpticalNode(deviceNodeId, serviceType, portMapping, node,
                OpenroadmNodeType.SRG, StringConstants.OPENROADM_DEVICE_VERSION_2_2_1, GridConstant.SLOT_WIDTH_50,
                GridConstant.SLOT_WIDTH_50);

        pceLink = new PceLink(link, pceOpticalNode, pceOpticalNode2);
        pceLink.setClient("XPONDER-CLIENT");

        pceResult = new PceResult();
        pceResult.setRC("200");
        pceResult.setRate(Long.valueOf(1));
        pceResult.setServiceType(StringConstants.SERVICE_TYPE_100GE_T);
        Map<LinkId, PceLink> map = Map.of(new LinkId("OpenROADM-3-1-DEG1-to-OpenROADM-3-2-DEG1"), pceLink);
        pcePathDescription = new PcePathDescription(List.of(pceLink),
                map, pceResult);
    }

    // TODO fix opposite link
    @Test(expected = Exception.class)
    public void buildDescriptionsTest() {

        pcePathDescription.buildDescriptions();
        Assert.assertEquals(pcePathDescription.getReturnStructure().getMessage(), "No path available by PCE");
    }

    @Test
    public void mapUtil() {
        PceConstraints pceConstraintsCalc = new PceConstraintsCalc(
                    PceTestData.getPCERequest(),
                    new NetworkTransactionImpl(new RequestProcessor(getDataBroker())))
                .getPceHardConstraints();
        MapUtils.mapDiversityConstraints(List.of(node), List.of(link), pceConstraintsCalc);
        MapUtils.getSupLink(link);
        MapUtils.getAllSupNode(node);
        MapUtils.getSRLGfromLink(link);
    }

}
