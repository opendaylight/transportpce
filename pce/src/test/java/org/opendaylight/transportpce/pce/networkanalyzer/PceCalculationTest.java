/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.networkanalyzer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.pce.constraints.PceConstraintsCalc;
import org.opendaylight.transportpce.pce.utils.PceTestData;
import org.opendaylight.transportpce.pce.utils.PceTestUtils;
import org.opendaylight.transportpce.pce.utils.TransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.pce.rev240205.PathComputationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.mapping.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev240315.network.nodes.NodeInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;

@ExtendWith(MockitoExtension.class)
public class PceCalculationTest extends AbstractTest {

    private PceCalculation pceCalculation;
    private PceConstraintsCalc pceConstraintsCalc;
    private PceResult pceResult = new PceResult();
    private Mapping mapping;

    @Mock
    private PortMapping portMapping;

    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        pceResult.setRC("200");
        PceTestUtils.writeNetworkIntoDataStore(getDataBroker(), getDataStoreContextUtil(),
                TransactionUtils.getNetworkForSpanLoss());
        pceConstraintsCalc = new PceConstraintsCalc(PceTestData.getPCERequest(),
                new NetworkTransactionImpl(getDataBroker()));
        mapping = new MappingBuilder().setLogicalConnectionPoint("logicalConnectionPoint").setPortQual("xpdr-client")
            .build();
        NodeInfo info = new NodeInfoBuilder().setNodeType(NodeTypes.Xpdr).build();
        Nodes node = new NodesBuilder().withKey(new NodesKey("node")).setNodeId("node").setNodeInfo(info).build();
        lenient().when(portMapping.getMapping(anyString(), anyString())).thenReturn(mapping);
        when(portMapping.getNode(anyString())).thenReturn(node);
    }

    @Test
    void testPceCalculationValues() {
        pceCalculation = new PceCalculation(
            PceTestData.getPCERequest(),
            new NetworkTransactionImpl(getDataBroker()),
            pceConstraintsCalc.getPceHardConstraints(),
            pceConstraintsCalc.getPceSoftConstraints(),
            pceResult,
            portMapping);
        pceCalculation.retrievePceNetwork();
        assertEquals(StringConstants.SERVICE_TYPE_100GE_T, pceCalculation.getServiceType());
        assertNotNull(pceCalculation.getReturnStructure());
        assertNull(pceCalculation.getaendPceNode());
        assertNull(pceCalculation.getzendPceNode());
    }

    @Test
    void testPceCalculationValues2() {
        pceCalculation = new PceCalculation(
                PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral(),
                new NetworkTransactionImpl(getDataBroker()),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
                pceResult, portMapping);
        pceCalculation.retrievePceNetwork();
        assertEquals(StringConstants.SERVICE_TYPE_100GE_T, pceCalculation.getServiceType());
        assertNotNull(pceCalculation.getReturnStructure());
        assertNull(pceCalculation.getaendPceNode());
        assertNull(pceCalculation.getzendPceNode());
    }

    @Test
    void testPceCalculationValues42() {
        PathComputationRequestInput input = PceTestData.getPathComputationRequestInputWithCoRoutingOrGeneral2();
        pceConstraintsCalc = new PceConstraintsCalc(input,
                new NetworkTransactionImpl(getDataBroker()));
        pceCalculation = new PceCalculation(
                PceTestData.getPCE_test3_request_54(),
                new NetworkTransactionImpl(getDataBroker()),
                pceConstraintsCalc.getPceHardConstraints(),
                pceConstraintsCalc.getPceSoftConstraints(),
                pceResult, portMapping);
        pceCalculation.retrievePceNetwork();
        assertEquals(StringConstants.SERVICE_TYPE_100GE_T, pceCalculation.getServiceType());
        assertNotNull(pceCalculation.getReturnStructure());
        assertNull(pceCalculation.getaendPceNode());
        assertNull(pceCalculation.getzendPceNode());
    }
}
