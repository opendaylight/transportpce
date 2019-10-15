/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.stub.MountPointStub;
import org.opendaylight.transportpce.renderer.utils.ServiceDeleteDataUtils;
import org.opendaylight.transportpce.renderer.utils.WaveLengthServiceUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev181130.networks.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev181130.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev191009.service.path.PathDescription;

@RunWith(Parameterized.class)
public class NetworkModelWaveLengthServiceUseTest extends AbstractTest {

    private NetworkModelWavelengthService networkModelWavelengthService;
    private DeviceTransactionManager deviceTransactionManager;

    private TerminationPoint1 terminationPoint1;
    private PathDescription pathDescription;
    private Node1 node1;

    @Before
    public void setMountPoint() {
        MountPointServiceStub mountPointService = new MountPointServiceStub(new MountPointStub(this.getDataBroker()));
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(this.getDataBroker());
    }

    public NetworkModelWaveLengthServiceUseTest(PathDescription pathDescription, TerminationPoint1 terminationPoint1,
        Node1 node1) {
        this.pathDescription = pathDescription;
        this.terminationPoint1 = terminationPoint1;
        this.node1 = node1;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        List<Object[]> parameters = new ArrayList<>();

        PathDescription pathDescription =
            ServiceDeleteDataUtils.createTransactionPathDescription(StringConstants.TTP_TOKEN);

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder();
        terminationPoint1Builder
            .setCtpAttributes((new CtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build());
        terminationPoint1Builder
            .setCpAttributes((new CpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build());
        terminationPoint1Builder
            .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build());
        terminationPoint1Builder
            .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(new ArrayList<>()).build());
        terminationPoint1Builder.setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(new ArrayList<>())
            .build());
        terminationPoint1Builder.setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build());
        terminationPoint1Builder.setXpdrNetworkAttributes((new XpdrNetworkAttributesBuilder()).build());
        terminationPoint1Builder.setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build());

        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setDegreeAttributes((new DegreeAttributesBuilder())
            .setAvailableWavelengths(Collections.singletonList(new AvailableWavelengthsBuilder().setIndex(20L).build()))
            .build());
        node1Builder.setSrgAttributes((new SrgAttributesBuilder()).setAvailableWavelengths(Collections.singletonList(
            new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                .AvailableWavelengthsBuilder().setIndex(20L).build())).build());

        for (OpenroadmNodeType nodeType : Arrays
            .asList(OpenroadmNodeType.XPONDER, OpenroadmNodeType.DEGREE, OpenroadmNodeType.SRG)) {
            node1Builder.setNodeType(nodeType);
            terminationPoint1Builder.setTpType(OpenroadmTpType.DEGREETXTTP);
            parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build() });
        }

        for (OpenroadmTpType tpType : OpenroadmTpType.values()) {
            node1Builder.setNodeType(OpenroadmNodeType.DEGREE);
            terminationPoint1Builder.setTpType(tpType);
            parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build() });
        }

        return parameters;
    }

    @Test
    public void freeWavelengthsTest() throws ExecutionException, InterruptedException {
        WaveLengthServiceUtils
            .putTerminationPoint1ToDatastore("node1" + StringConstants.TTP_TOKEN, StringConstants.TTP_TOKEN,
                this.terminationPoint1, this.deviceTransactionManager);
        WaveLengthServiceUtils.putNode1ToDatastore("node1" + StringConstants.TTP_TOKEN, this.node1,
            this.deviceTransactionManager);
        this.networkModelWavelengthService.useWavelengths(this.pathDescription);
        Node1 updatedNode1 = WaveLengthServiceUtils.getNode1FromDatastore("node1" + StringConstants.TTP_TOKEN,
            this.deviceTransactionManager);
        TerminationPoint1 updatedTerminationPoint1 = WaveLengthServiceUtils
            .getTerminationPoint1FromDatastore("node1" + StringConstants.TTP_TOKEN, StringConstants.TTP_TOKEN,
                this.deviceTransactionManager);

        switch (updatedTerminationPoint1.getTpType()) {
            case DEGREETXRXCTP:
            case DEGREETXCTP:
            case DEGREERXCTP:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case SRGTXCP:
            case SRGRXCP:
            case SRGTXRXCP:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case DEGREETXRXTTP:
            case DEGREETXTTP:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case DEGREERXTTP:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case SRGRXPP:
            case SRGTXPP:
            case SRGTXRXPP:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case XPONDERCLIENT:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case XPONDERNETWORK:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case XPONDERPORT:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes());
                break;
            default:
                Assert.assertTrue(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertTrue(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
        }
        switch (updatedNode1.getNodeType()) {
            case DEGREE:
                Assert.assertNull(updatedNode1.getDegreeAttributes());
                Assert.assertFalse(updatedNode1.getSrgAttributes().getAvailableWavelengths().isEmpty());
                break;
            case SRG:
                Assert.assertNull(updatedNode1.getSrgAttributes());
                Assert.assertFalse(updatedNode1.getDegreeAttributes().getAvailableWavelengths().isEmpty());
                break;
            default:
                Assert.assertFalse(updatedNode1.getDegreeAttributes().getAvailableWavelengths().isEmpty());
                Assert.assertFalse(updatedNode1.getSrgAttributes().getAvailableWavelengths().isEmpty());
                break;
        }

    }
}
