/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.crossconnect.CrossConnectImpl;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yangtools.yang.common.Decimal64;

class PowerMgmtTest {
    private OpenRoadmInterfaces openRoadmInterfaces;
    private CrossConnect crossConnect;
    private DeviceTransactionManager deviceTransactionManager;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;

    @BeforeEach
    void setUp() {
        mock(DataBroker.class);
        this.openRoadmInterfaces = mock(OpenRoadmInterfaces.class);
        this.crossConnect = mock((CrossConnectImpl.class));
        this.deviceTransactionManager = mock(DeviceTransactionManager.class);
        this.portMapping = mock(PortMapping.class);
        this.powerMgmt = new PowerMgmtImpl(this.openRoadmInterfaces, this.crossConnect,
                this.deviceTransactionManager, this.portMapping, 1000, 1000);
    }

    @Test
    void testSetPowerWhenMappingReturnNull() {
        when(this.portMapping.getNode(anyString())).thenReturn(null);
        boolean output = this.powerMgmt.setPower(OlmPowerServiceRpcImplUtil.getServicePowerSetupInput());
        assertEquals(false, output);
    }

    @Test
    void testSetPowerForTransponderAEnd() throws OpenRoadmInterfaceException {
        when(this.portMapping.getNode("xpdr-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-A", OpenroadmNodeVersion._121,
                    List.of("network-A")));
        when(this.portMapping.getNode("roadm-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                    List.of("srg1-A", "deg2-A")));
        Interface interfOch = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(new Interface1Builder()
                        .setOch(new OchBuilder().build())
                        .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(matches("xpdr-A"), anyString())).thenReturn(Optional.of(interfOch));
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                                .Interface1Builder()
                                .setOts(new OtsBuilder()
                                        .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                                        .build())
                                .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(matches("roadm-A"), anyString())).thenReturn(Optional.of(interfOts));
        MockedStatic<PowerMgmtVersion121> pmv121 = mockStatic(PowerMgmtVersion121.class);
        pmv121.when(() -> PowerMgmtVersion121.setTransponderPower(anyString(), anyString(), any(), any(), any()))
                .thenReturn(true);
        Map<String, Double> powerRangeMap = new HashMap<>();
        powerRangeMap.put("MaxTx", 0.1);
        powerRangeMap.put("MinTx", -5.1);
        pmv121.when(() -> PowerMgmtVersion121.getXponderPowerRange(anyString(), anyString(), anyString(), any()))
                .thenReturn(powerRangeMap);
        when(this.crossConnect.setPowerLevel(anyString(), anyString(), any(), anyString())).thenReturn(true);

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputForTransponder();
        boolean result = this.powerMgmt.setPower(input);
        assertEquals(true, result);
    }

    @Test
    void testSetPowerForTransponderZEnd() {
        when(this.portMapping.getNode("xpdr-C"))
            .thenReturn(OlmPowerServiceRpcImplUtil
                .getMappingNodeTpdr("xpdr-C", OpenroadmNodeVersion._121, List.of("client-C")));

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("xpdr-C", "network-C", "client-C");
        boolean result = this.powerMgmt.setPower(input);
        assertEquals(true, result);
    }

    @Test
    void testSetPowerForRoadmAEnd() throws OpenRoadmInterfaceException {
        when(this.portMapping.getNode("roadm-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                        List.of("srg1-A", "deg2-A")));
        when(this.deviceTransactionManager.getDataFromDevice(anyString(), any(), any(), anyLong(), any()))
            .thenReturn(Optional.empty());
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                            .Interface1Builder()
                    .setOts(new OtsBuilder()
                            .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                            .build())
                    .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(anyString(), anyString())).thenReturn(Optional.of(interfOts));
        when(this.crossConnect.setPowerLevel(anyString(), anyString(), any(), anyString())).thenReturn(true);

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("roadm-A", "srg1-A", "deg2-A");
        boolean result = this.powerMgmt.setPower(input);
        assertEquals(true, result);
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.Power.getName()),
                    eq(Decimal64.valueOf("-3.00")), matches("srg1-A-deg2-A-761:768"));
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.GainLoss.getName()),
                    eq(Decimal64.valueOf("-3.00")), matches("srg1-A-deg2-A-761:768"));
    }

    @Test
    void testSetPowerForRoadmZEnd() {
        when(this.portMapping.getNode("roadm-C"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-C", OpenroadmNodeVersion._121,
                    List.of("deg1-C", "srg1-C")));
        when(this.deviceTransactionManager.getDataFromDevice(anyString(), any(), any(), anyLong(), any()))
            .thenReturn(Optional.empty());
        when(this.crossConnect.setPowerLevel(anyString(), anyString(), any(), anyString())).thenReturn(true);

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("roadm-C", "deg1-C", "srg1-C");
        boolean result = this.powerMgmt.setPower(input);
        assertEquals(true, result);
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-C"), matches(OpticalControlMode.Power.getName()), isNull(),
                    matches("deg1-C-srg1-C-761:768"));
    }

    @Test
    void testSetPowerForTransponderWhenNoTransponderPort() throws OpenRoadmInterfaceException {
        when(this.portMapping.getNode("xpdr-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-A", OpenroadmNodeVersion._121,
                    List.of("network-A")));
        when(this.portMapping.getNode("roadm-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                    List.of("srg1-A", "deg2-A")));
        Interface interfOch = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(new Interface1Builder()
                        .setOch(new OchBuilder().build())
                        .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(matches("xpdr-A"), anyString())).thenReturn(Optional.of(interfOch));
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                                .Interface1Builder()
                                .setOts(new OtsBuilder()
                                        .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                                        .build())
                                .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(matches("roadm-A"), anyString())).thenReturn(Optional.of(interfOts));
        try (MockedStatic<PowerMgmtVersion121> pmv121 = mockStatic(PowerMgmtVersion121.class)) {
            pmv121.when(() -> PowerMgmtVersion121.setTransponderPower(anyString(), anyString(),
                            any(), any(), any()))
                    .thenReturn(true);
            pmv121.when(() -> PowerMgmtVersion121.getXponderPowerRange(anyString(), anyString(),
                            anyString(), any()))
                    .thenReturn(new HashMap<>());

            when(this.crossConnect
                    .setPowerLevel(anyString(), anyString(), any(), anyString()))
                .thenReturn(true);


            ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputForTransponder();
            boolean result = this.powerMgmt.setPower(input);
            assertEquals(true, result);
            pmv121.verify(() -> PowerMgmtVersion121.setTransponderPower(matches("xpdr-A"),
                    anyString(), eq(new BigDecimal("-5")), any(), any()));
            verify(this.crossConnect, times(1))
                .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.GainLoss.getName()),
                        eq(Decimal64.valueOf("-3.00")), matches("srg1-A-deg2-A-761:768"));
        }
    }

    @Test
    void testSetPowerForTransponderAEndWithRoadmPort() throws OpenRoadmInterfaceException {
        when(this.portMapping.getNode("xpdr-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeTpdr("xpdr-A", OpenroadmNodeVersion._121,
                    List.of("network-A")));
        when(this.portMapping.getNode("roadm-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                    List.of("srg1-A", "deg2-A")));
        Interface interfOch = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(new Interface1Builder()
                        .setOch(new OchBuilder().build())
                        .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(matches("xpdr-A"), anyString())).thenReturn(Optional.of(interfOch));
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                                .Interface1Builder()
                                .setOts(new OtsBuilder()
                                        .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                                        .build())
                                .build())
                .build();
        when(this.openRoadmInterfaces.getInterface(matches("roadm-A"), anyString())).thenReturn(Optional.of(interfOts));
        try (MockedStatic<PowerMgmtVersion121> pmv121 = mockStatic(PowerMgmtVersion121.class)) {

            pmv121.when(() -> PowerMgmtVersion121.setTransponderPower(anyString(), anyString(), any(), any(), any()))
                    .thenReturn(true);
            Map<String, Double> powerRangeMapTpdrTx = new HashMap<>();
            powerRangeMapTpdrTx.put("MaxTx", 0.1);
            powerRangeMapTpdrTx.put("MinTx", -5.1);
            pmv121.when(() -> PowerMgmtVersion121.getXponderPowerRange(anyString(), anyString(), anyString(), any()))
                    .thenReturn(powerRangeMapTpdrTx);
            Map<String, Double> powerRangeMapSrgRx = new HashMap<>();
            powerRangeMapSrgRx.put("MaxRx", -4.2);
            powerRangeMapSrgRx.put("MinRx", -22.2);
            pmv121.when(() -> PowerMgmtVersion121.getSRGRxPowerRange(anyString(), anyString(), any(), anyString(),
                        anyString()))
                    .thenReturn(powerRangeMapSrgRx);
            when(this.crossConnect.setPowerLevel(anyString(), anyString(), any(), anyString())).thenReturn(true);

            ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputForTransponder();
            boolean result = this.powerMgmt.setPower(input);
            assertEquals(true, result);
            pmv121.verify(() -> PowerMgmtVersion121.setTransponderPower(matches("xpdr-A"),
                    anyString(), eq(new BigDecimal("-4.20000000000000017763568394002504646778106689453125")),
                    any(), any()));
        }
    }

    @Test
    void testSetPowerWithoutNode() {
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil.getServicePowerSetupInputWthoutNode();
        boolean result = this.powerMgmt.setPower(input);
        assertEquals(false, result);
        verifyNoInteractions(this.crossConnect);
    }

    @Test
    void testSetPowerForBadNodeType() {
        when(this.portMapping.getNode("ila node")).thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeIla());

        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("ila node", "rx-port", "tx-port");
        boolean result = this.powerMgmt.setPower(input);
        assertEquals(true, result);
        verifyNoInteractions(this.crossConnect);
        verifyNoInteractions(this.openRoadmInterfaces);
    }


    @Test
    void testPowerTurnDownWhenSuccess() {
        when(this.crossConnect
                .setPowerLevel(anyString(), anyString(), any(), anyString()))
            .thenReturn(true);
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        boolean result = this.powerMgmt.powerTurnDown(input);
        assertEquals(true, result);
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-C"), matches(OpticalControlMode.Off.getName()), isNull(), anyString());
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.Power.getName()),
                    eq(Decimal64.valueOf("-60")), anyString());
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.Off.getName()), isNull(), anyString());
    }

    @Test
    void testPowerTurnDownWhenFailure() {
        when(this.crossConnect.setPowerLevel(anyString(), anyString(), any(), anyString())).thenReturn(false);
        ServicePowerTurndownInput input = OlmPowerServiceRpcImplUtil.getServicePowerTurndownInput();
        boolean result = this.powerMgmt.powerTurnDown(input);
        assertEquals(false, result);
        verify(this.crossConnect, times(2))
                .setPowerLevel(anyString(), any(), any(), anyString());
    }

    @Test
    void testSetPowerForRoadmAEndGainLossFailure() throws OpenRoadmInterfaceException {
        when(this.portMapping.getNode("roadm-A"))
            .thenReturn(OlmPowerServiceRpcImplUtil.getMappingNodeRdm("roadm-A", OpenroadmNodeVersion._121,
                        List.of("srg1-A", "deg2-A")));
        when(this.deviceTransactionManager.getDataFromDevice(anyString(), any(), any(), anyLong(), any()))
            .thenReturn(Optional.empty());
        Interface interfOts = new InterfaceBuilder()
                .setName("interface name")
                .addAugmentation(
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014
                            .Interface1Builder()
                    .setOts(new OtsBuilder()
                            .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("6")))
                            .build())
                    .build())
                .build();
        when(this.crossConnect.setPowerLevel(anyString(), matches(OpticalControlMode.Power.getName()),any(),
                anyString()))
            .thenReturn(true);
        when(this.openRoadmInterfaces.getInterface(anyString(), anyString())).thenReturn(Optional.of(interfOts));
        when(this.crossConnect.setPowerLevel(anyString(), matches(OpticalControlMode.GainLoss.getName()),any(),
                anyString()))
            .thenReturn(false);
        ServicePowerSetupInput input = OlmPowerServiceRpcImplUtil
                .getServicePowerSetupInputForOneNode("roadm-A", "srg1-A", "deg2-A");
        boolean result = this.powerMgmt.setPower(input);
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.Power.getName()),
                    eq(Decimal64.valueOf("-3.00")), matches("srg1-A-deg2-A-761:768"));
        verify(this.crossConnect, times(1))
            .setPowerLevel(matches("roadm-A"), matches(OpticalControlMode.GainLoss.getName()),
                    eq(Decimal64.valueOf("-3.00")), matches("srg1-A-deg2-A-761:768"));
        assertEquals(false, result);
    }


}
