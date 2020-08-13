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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev181130.degree.node.attributes.AvailableWavelengths;
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
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.service.types.rev200128.service.path.PathDescription;
import org.opendaylight.yangtools.yang.common.Uint32;

@Ignore
@RunWith(Parameterized.class)
public class NetworkModelWaveLengthServiceUseTest extends AbstractTest {

    private NetworkModelWavelengthService networkModelWavelengthService;
    private DeviceTransactionManager deviceTransactionManager;

    private TerminationPoint1 terminationPoint1;
    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1 terminatPoint2;
    private PathDescription pathDescription;
    private Node1 node1;
    private org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 node2;

    @Before
    public void setMountPoint() {
        MountPointServiceStub mountPointService = new MountPointServiceStub(new MountPointStub(this.getDataBroker()));
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(this.getDataBroker());
    }

    public NetworkModelWaveLengthServiceUseTest(PathDescription pathDescription, TerminationPoint1 terminationPoint1,
        Node1 node1,
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1 terminationPoint2,
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 node2) {
        this.pathDescription = pathDescription;
        this.terminationPoint1 = terminationPoint1;
        this.terminatPoint2 = terminationPoint2;
        this.node1 = node1;
        this.node2 = node2;
    }

    @Parameterized.Parameters
    public static Collection createParameters() {
        List<Object[]> parameters = new ArrayList<>();

        PathDescription pathDescription =
            ServiceDeleteDataUtils.createTransactionPathDescription(StringConstants.TTP_TOKEN);

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder()
            .setCtpAttributes((new CtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
            .setCpAttributes((new CpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
            .setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
            .setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(Map.of()).build())
            .setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(Map.of()).build())
            .setXpdrClientAttributes((new XpdrClientAttributesBuilder()).build())
            .setXpdrNetworkAttributes((new XpdrNetworkAttributesBuilder()).build())
            .setXpdrPortAttributes((new XpdrPortAttributesBuilder()).build());

        AvailableWavelengths aval = new AvailableWavelengthsBuilder().setIndex(Uint32.valueOf(20)).build();
        org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes.AvailableWavelengths avalSrg =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.srg.rev181130.srg.node.attributes
                    .AvailableWavelengthsBuilder().setIndex(Uint32.valueOf(20)).build();
        Node1Builder node1Builder = new Node1Builder()
            .setDegreeAttributes((new DegreeAttributesBuilder())
                .setAvailableWavelengths(
                    Map.of(aval.key(),aval))
                .build())
            .setSrgAttributes((new SrgAttributesBuilder())
                .setAvailableWavelengths(
                    Map.of(avalSrg.key(),avalSrg))
                .build());

        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder
            terminationPoint2Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1Builder();

        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder node2Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1Builder();

        for (OpenroadmNodeType nodeType : Arrays
            .asList(OpenroadmNodeType.XPONDER, OpenroadmNodeType.DEGREE, OpenroadmNodeType.SRG)) {
            node2Builder.setNodeType(nodeType);
            terminationPoint2Builder.setTpType(OpenroadmTpType.DEGREETXTTP);
            parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build(),
                terminationPoint2Builder.build(), node2Builder.build() });
        }

        for (OpenroadmTpType tpType : OpenroadmTpType.values()) {
            node2Builder.setNodeType(OpenroadmNodeType.DEGREE);
            terminationPoint2Builder.setTpType(tpType);
            parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build(),
                terminationPoint2Builder.build(), node2Builder.build() });
        }

        return parameters;
    }

    @Test
    public void freeWavelengthsTest() throws ExecutionException, InterruptedException {
        WaveLengthServiceUtils.putTerminationPoint1ToDatastore("node1" + StringConstants.TTP_TOKEN,
            StringConstants.TTP_TOKEN, this.terminationPoint1, this.deviceTransactionManager);
        WaveLengthServiceUtils.putTerminationPoint2ToDatastore("node1" + StringConstants.TTP_TOKEN,
            StringConstants.TTP_TOKEN, this.terminatPoint2, this.deviceTransactionManager);
        WaveLengthServiceUtils.putNode1ToDatastore("node1" + StringConstants.TTP_TOKEN, this.node1,
            this.deviceTransactionManager);
        WaveLengthServiceUtils.putNode2ToDatastore("node1" + StringConstants.TTP_TOKEN, this.node2,
            this.deviceTransactionManager);
        this.networkModelWavelengthService.useWavelengths(this.pathDescription);
        Node1 updatedNode1 = WaveLengthServiceUtils.getNode1FromDatastore("node1" + StringConstants.TTP_TOKEN,
            this.deviceTransactionManager);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1 updatedNode2 =
            WaveLengthServiceUtils.getNode2FromDatastore("node1" + StringConstants.TTP_TOKEN,
                this.deviceTransactionManager);
        TerminationPoint1 updatedTerminationPoint1 = WaveLengthServiceUtils
            .getTerminationPoint1FromDatastore("node1" + StringConstants.TTP_TOKEN, StringConstants.TTP_TOKEN,
                this.deviceTransactionManager);
        org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130
            .TerminationPoint1 updatedTerminationPoint2 = WaveLengthServiceUtils
            .getTerminationPoint2FromDatastore("node1" + StringConstants.TTP_TOKEN, StringConstants.TTP_TOKEN,
            this.deviceTransactionManager);

        switch (updatedTerminationPoint2.getTpType()) {
        //switch (((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.TerminationPoint1)
        //        updatedTerminationPoint1).getTpType()) {
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
        switch (updatedNode2.getNodeType()) {
        //switch (((org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev181130.Node1) updatedNode1)
        //        .getNodeType()) {
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
