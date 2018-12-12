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
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.renderer.stub.MountPointServiceStub;
import org.opendaylight.transportpce.renderer.stub.MountPointStub;
import org.opendaylight.transportpce.renderer.utils.ServiceDeleteDataUtils;
import org.opendaylight.transportpce.renderer.utils.WaveLengthServiceUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.degree.rev171215.degree.used.wavelengths.UsedWavelengthsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.Node1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.Node1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.TerminationPoint1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.DegreeAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.DegreeAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.SrgAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.CpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.CtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.PpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.RxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.TxTtpAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrClientAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrNetworkAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.XpdrPortAttributesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network.node.termination.point.pp.attributes.UsedWavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev171215.OpenroadmNodeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev171215.OpenroadmTpType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.xponder.rev171215.xpdr.port.connection.attributes.WavelengthBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.PathDescription;

@RunWith(Parameterized.class)
public class NetworkModelWaveLengthServiceFreeTest extends AbstractTest {

    private static final Long WAVE_LENGTH = 20L;
    private NetworkModelWavelengthService networkModelWavelengthService;
    private DeviceTransactionManager deviceTransactionManager;
    private TerminationPoint1 terminationPoint1;
    private PathDescription pathDescription;
    private Node1 node1;

    public NetworkModelWaveLengthServiceFreeTest(PathDescription pathDescription, TerminationPoint1 terminationPoint1,
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
        terminationPoint1Builder.setCtpAttributes((new CtpAttributesBuilder()).setUsedWavelengths(Collections
            .singletonList((new UsedWavelengthsBuilder()).setIndex(WAVE_LENGTH).build())).build());
        terminationPoint1Builder.setCpAttributes((new CpAttributesBuilder()).setUsedWavelengths(Collections
            .singletonList((new org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev171215.network
                .node.termination.point.cp.attributes.UsedWavelengthsBuilder())
                .setIndex(WAVE_LENGTH).build())).build());
        terminationPoint1Builder.setTxTtpAttributes((new TxTtpAttributesBuilder()).setUsedWavelengths(Collections
            .singletonList((new UsedWavelengthsBuilder()).setIndex(WAVE_LENGTH).build())).build());
        terminationPoint1Builder.setRxTtpAttributes((new RxTtpAttributesBuilder()).setUsedWavelengths(Collections
            .singletonList((new UsedWavelengthsBuilder()).setIndex(WAVE_LENGTH).build())).build());
        terminationPoint1Builder.setPpAttributes((new PpAttributesBuilder()).setUsedWavelength(Collections
            .singletonList((new UsedWavelengthBuilder()).setIndex(WAVE_LENGTH).build())).build());
        terminationPoint1Builder.setXpdrClientAttributes((new XpdrClientAttributesBuilder())
            .setWavelength((new WavelengthBuilder()).setIndex(WAVE_LENGTH).build()).build());
        terminationPoint1Builder.setXpdrNetworkAttributes((new XpdrNetworkAttributesBuilder())
            .setWavelength((new WavelengthBuilder()).setIndex(WAVE_LENGTH).build()).build());
        terminationPoint1Builder.setXpdrPortAttributes((new XpdrPortAttributesBuilder())
            .setWavelength((new WavelengthBuilder()).setIndex(WAVE_LENGTH).build()).build());

        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setDegreeAttributes((new DegreeAttributesBuilder()).setAvailableWavelengths(new ArrayList<>())
            .build());
        node1Builder.setSrgAttributes((new SrgAttributesBuilder()).setAvailableWavelengths(new ArrayList<>()).build());

        for (OpenroadmNodeType nodeType : Arrays.asList(OpenroadmNodeType.XPONDER, OpenroadmNodeType.DEGREE,
            OpenroadmNodeType.SRG)) {
            node1Builder.setNodeType(nodeType);
            terminationPoint1Builder.setTpType(OpenroadmTpType.DEGREETXTTP);
            parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build() });
        }

        for (OpenroadmTpType tpType : OpenroadmTpType.values()) {
            node1Builder.setNodeType(OpenroadmNodeType.DEGREE);
            node1Builder.setDegreeAttributes(null);
            terminationPoint1Builder.setTpType(tpType);
            parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build() });
        }

        node1Builder.setNodeType(OpenroadmNodeType.SRG);
        node1Builder.setDegreeAttributes((new DegreeAttributesBuilder()).setAvailableWavelengths(new ArrayList<>())
            .build());
        node1Builder.setSrgAttributes(null);
        terminationPoint1Builder.setTpType(OpenroadmTpType.DEGREETXTTP);
        parameters.add(new Object[] { pathDescription, terminationPoint1Builder.build(), node1Builder.build() });

        return parameters;
    }

    @Before
    public void setMountPoint() {
        MountPointService mountPointService = new MountPointServiceStub(new MountPointStub(this.getDataBroker()));
        this.deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointService, 3000);
        networkModelWavelengthService = new NetworkModelWavelengthServiceImpl(this.getDataBroker());
    }

    @Test
    public void freeWavelengthsTest() throws ExecutionException, InterruptedException {
        WaveLengthServiceUtils.putTerminationPoint1ToDatastore("node1" + StringConstants.TTP_TOKEN,
        		StringConstants.TTP_TOKEN,
            this.terminationPoint1, this.deviceTransactionManager);
        WaveLengthServiceUtils.putNode1ToDatastore("node1" + StringConstants.TTP_TOKEN, this.node1,
            this.deviceTransactionManager);
        this.networkModelWavelengthService.freeWavelengths(this.pathDescription);
        Node1 updatedNode1 = WaveLengthServiceUtils.getNode1FromDatastore("node1" + StringConstants.TTP_TOKEN,
            this.deviceTransactionManager);
        TerminationPoint1 updatedTerminationPoint1 =
            WaveLengthServiceUtils.getTerminationPoint1FromDatastore("node1" + StringConstants.TTP_TOKEN,
            		StringConstants.TTP_TOKEN, this.deviceTransactionManager);
        switch (updatedTerminationPoint1.getTpType()) {
            case DEGREETXRXCTP:
            case DEGREETXCTP:
            case DEGREERXCTP:
                Assert.assertNull(updatedTerminationPoint1.getCtpAttributes());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case SRGTXCP:
            case SRGRXCP:
            case SRGTXRXCP:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getCpAttributes());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case DEGREETXRXTTP:
            case DEGREETXTTP:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getTxTtpAttributes());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case DEGREERXTTP:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getRxTtpAttributes());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case SRGRXPP:
            case SRGTXPP:
            case SRGTXRXPP:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getPpAttributes());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case XPONDERCLIENT:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNull(updatedTerminationPoint1.getXpdrClientAttributes());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case XPONDERNETWORK:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrNetworkAttributes());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
            case XPONDERPORT:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNull(updatedTerminationPoint1.getXpdrPortAttributes());
                break;
            default:
                Assert.assertFalse(updatedTerminationPoint1.getCtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getCpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getTxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getRxTtpAttributes().getUsedWavelengths().isEmpty());
                Assert.assertFalse(updatedTerminationPoint1.getPpAttributes().getUsedWavelength().isEmpty());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrClientAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrNetworkAttributes().getWavelength());
                Assert.assertNotNull(updatedTerminationPoint1.getXpdrPortAttributes().getWavelength());
                break;
        }
        switch (updatedNode1.getNodeType()) {
            case DEGREE:
                Assert.assertEquals(1, updatedNode1.getDegreeAttributes().getAvailableWavelengths().size());
                Assert.assertEquals(WAVE_LENGTH,
                    updatedNode1.getDegreeAttributes().getAvailableWavelengths().get(0).getIndex());
                Assert.assertTrue(updatedNode1.getSrgAttributes().getAvailableWavelengths().isEmpty());
                break;
            case SRG:
                Assert.assertEquals(1, updatedNode1.getSrgAttributes().getAvailableWavelengths().size());
                Assert.assertEquals(WAVE_LENGTH,
                    updatedNode1.getSrgAttributes().getAvailableWavelengths().get(0).getIndex());
                Assert.assertTrue(updatedNode1.getDegreeAttributes().getAvailableWavelengths().isEmpty());
                break;
            default:
                Assert.assertTrue(updatedNode1.getDegreeAttributes().getAvailableWavelengths().isEmpty());
                Assert.assertTrue(updatedNode1.getSrgAttributes().getAvailableWavelengths().isEmpty());
                break;
        }
    }
}
