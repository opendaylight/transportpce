/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.RequestProcessor;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev210701.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class PceCalculationTest extends AbstractTest {

    private PceCalculation pceCalculation;
    private PceConstraintsCalc pceConstraintsCalc;
    private PceResult pceResult = new PceResult();
    private Mapping mapping;

    @Mock
    private PortMapping portMapping;

    // setup object
    @Before
    public void setUp() throws ExecutionException, InterruptedException {
        pceResult.setRC("200");
        PceTestUtils.writeNetworkIntoDataStore(this.getDataBroker(), this.getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData.getPCERequest(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())));
        mapping = new MappingBuilder().setLogicalConnectionPoint("logicalConnectionPoint").setPortQual("xpdr-client")
            .build();
        NodeInfo info = new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).build();
        Nodes node = new NodesBuilder().withKey(new NodesKey("node")).setNodeId("node").setNodeInfo(info).build();
        when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        when(portMapping.getNode(anyString())).thenReturn(node);
    }

    @Test
    public void testPceCalculationValues() {
        pceCalculation = new PceCalculation(
            PceTestData.getPCERequest(),
            new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
            pceConstraintsCalc.getPceHardConstraints(),
            pceConstraintsCalc.getPceSoftConstraints(),
            pceResult,
            portMapping);
        pceCalculation.retrievePceNetwork();
        Assert.assertEquals(StringConstants.SERVICE_TYPE_100GE_T, pceCalculation.getServiceType());
        Assert.assertNotNull(pceCalculation.getReturnStructure());

        Assert.assertNull(pceCalculation.getaendPceNode());
        Assert.assertNull(pceCalculation.getzendPceNode());
    }

    @Test
    public void testPceCalculationValues2() {
        pceCalculation = new PceCalculation(
                PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
                pceResult, portMapping);
        pceCalculation.retrievePceNetwork();
        Assert.assertEquals(StringConstants.SERVICE_TYPE_100GE_T, pceCalculation.getServiceType());
        Assert.assertNotNull(pceCalculation.getReturnStructure());

        Assert.assertNull(pceCalculation.getaendPceNode());
        Assert.assertNull(pceCalculation.getzendPceNode());
    }

    @Test
    public void testPceCalculationValues42() {
        PathComputationRequestInput input = PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral2();
        pceConstraintsCalc = new PceConstraintsCalc(input,
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())));
        pceCalculation = new PceCalculation(
                PceTestData.getPCE_test3_request_54(),
                new NetworkTransactionImpl(new RequestProcessor(this.getDataBroker())),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
                pceResult, portMapping);
        pceCalculation.retrievePceNetwork();
        Assert.assertEquals(StringConstants.SERVICE_TYPE_100GE_T, pceCalculation.getServiceType());
        Assert.assertNotNull(pceCalculation.getReturnStructure());

        Assert.assertNull(pceCalculation.getaendPceNode());
        Assert.assertNull(pceCalculation.getzendPceNode());
    }
}
