/*
 * Copyright © 2020 Orange.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.crossconnect;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.binding.api.MountPointService;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPorts;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.degree.ConnectionPortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.otn.renderer.nodes.Nodes;
import org.opendaylight.yangtools.util.concurrent.FluentFutures;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;

public class CrossConnectImpl221Test {
    private CrossConnectImpl221 crossConnectImpl221 = null;
    private DeviceTransactionManager deviceTransactionManager = null;

    private MountPointService mountPointServiceMock = mock(MountPointService.class);
    private MountPoint mountPointMock = mock(MountPoint.class);
    private DataBroker dataBrokerMock = mock(DataBroker.class);
    private ReadWriteTransaction rwTransactionMock = mock(ReadWriteTransaction.class);

    @Before
    public void setup() {
        deviceTransactionManager = mock(DeviceTransactionManager.class);
        crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);

        //mock responses for deviceTransactionManager calls
        InstanceIdentifier<RoadmConnections> deviceIID =
                InstanceIdentifier
                        .create(OrgOpenroadmDevice.class)
                        .child(RoadmConnections.class, new RoadmConnectionsKey("1"));

        Mockito.when(deviceTransactionManager.getDataFromDevice("deviceId",
                LogicalDatastoreType.CONFIGURATION, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
            .thenReturn(Optional.of(mock(RoadmConnections.class)));
    }

    @Test
    public void getCrossConnectTest() {
        Optional<RoadmConnections> res =
                crossConnectImpl221.getCrossConnect("deviceId", "1");
        Assert.assertTrue("Optional object should have a value", res.isPresent());
    }

    @Test
    public void postCrossConnectTest() {
        Mockito.when(mountPointServiceMock.getMountPoint(any())).thenReturn(Optional.of(mountPointMock));
        Mockito.when(mountPointMock.getService(any())).thenReturn(Optional.of(dataBrokerMock));
        Mockito.when(dataBrokerMock.newReadWriteTransaction()).thenReturn(rwTransactionMock);
        Mockito.when(rwTransactionMock.commit()).thenReturn(FluentFutures.immediateNullFluentFuture());
        deviceTransactionManager = new DeviceTransactionManagerImpl(mountPointServiceMock, 3000);
        crossConnectImpl221 = new CrossConnectImpl221(deviceTransactionManager);
        SpectrumInformation spectrumInformation = new SpectrumInformation();
        spectrumInformation.setWaveLength(Uint32.valueOf(1));
        spectrumInformation.setLowerSpectralSlotNumber(761);
        spectrumInformation.setHigherSpectralSlotNumber(768);
        Optional<String> res = crossConnectImpl221.postCrossConnect("deviceId", "srcTp", "destTp", spectrumInformation);
        Assert.assertEquals(res.get(), "srcTp-destTp-761:768");
    }

    @Test(expected = NullPointerException.class)
    public void setPowerLevelTest() {
        InstanceIdentifier<RoadmConnections> deviceIID =
                InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(RoadmConnections.class, new RoadmConnectionsKey("1"));
        Mockito.when(deviceTransactionManager.getDataFromDevice("deviceId",
                LogicalDatastoreType.OPERATIONAL, deviceIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
            .thenReturn(Optional.of(mock(RoadmConnections.class)));

        Mockito.when(deviceTransactionManager.getDeviceTransaction("deviceId"))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(mock(DeviceTransaction.class))));
        boolean res = crossConnectImpl221
                .setPowerLevel("deviceId", OpticalControlMode.GainLoss, new BigDecimal(100), "1");

        Assert.assertTrue("set Level should be true", true);
    }

    @Test(expected = NullPointerException.class)
    public void postOtnCrossConnect() {
        NodesBuilder nodesBldr = new NodesBuilder().withKey(new NodesKey("nodeId")).setNodeId("nodeId");
        Nodes nodes = Mockito.mock(Nodes.class);
        Mockito.when(nodes.getNodeId()).thenReturn("nodeId");
        Mockito.when(deviceTransactionManager.getDeviceTransaction(any()))
            .thenReturn(CompletableFuture.completedFuture(Optional.of(mock(DeviceTransaction.class))));
        Optional<String> res = crossConnectImpl221.postOtnCrossConnect(List.of("src1", "src2"), nodes);
        Assert.assertTrue("Optional value should have a value", res.isPresent());
    }


    private ConnectionPorts getConnectionPorts(String c1, String p1) {
        return new ConnectionPortsBuilder().setCircuitPackName(c1).setPortName(p1).build();
    }
}
