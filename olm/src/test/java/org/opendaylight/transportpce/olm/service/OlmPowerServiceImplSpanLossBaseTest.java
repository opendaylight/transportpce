/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.OlmTransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;

@ExtendWith(MockitoExtension.class)
class OlmPowerServiceImplSpanLossBaseTest extends AbstractTest {

    @Mock
    private DeviceTransactionManager deviceTransactionManager;
    @Mock
    private PortMapping portMapping;
    @Mock
    private PowerMgmt powerMgmt;
    @Mock
    private MappingUtils mappingUtils;
    @Mock
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    @Mock
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    @Mock
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;

    private OpenRoadmInterfaces openRoadmInterfaces;
    private OlmPowerService olmPowerService;
    private DataBroker dataBroker;

    @BeforeEach
    void setUp() throws OpenRoadmInterfaceException {
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(this.deviceTransactionManager, this.mappingUtils,
                this.openRoadmInterfacesImpl121, this.openRoadmInterfacesImpl221, this.openRoadmInterfacesImpl710);
        this.olmPowerService = new OlmPowerServiceImpl(getDataBroker(), this.powerMgmt,
                this.deviceTransactionManager, this.portMapping, this.mappingUtils, this.openRoadmInterfaces);
        this.dataBroker = getDataBroker();
        doReturn(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1)
            .when(this.mappingUtils).getOpenRoadmVersion(anyString());
        Nodes nodes1 = Mockito.mock(Nodes.class);
        Nodes nodes2 = Mockito.mock(Nodes.class);

        when(this.portMapping.getNode("ROADM-A1")).thenReturn(nodes1);
        when(this.portMapping.getNode("ROADM-C1")).thenReturn(nodes2);
        when(nodes1.getMapping()).thenReturn(OlmTransactionUtils.getMappingMap1());
        when(nodes2.getMapping()).thenReturn(OlmTransactionUtils.getMappingMap2());

        DataObjectIdentifier<CurrentPmList> iidCurrentPmList = DataObjectIdentifier
                .builder(CurrentPmList.class)
                .build();
        when(this.deviceTransactionManager.getDataFromDevice("ROADM-A1", LogicalDatastoreType.OPERATIONAL,
                iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
            .thenReturn(OlmTransactionUtils.getCurrentPmListA());
        when(this.deviceTransactionManager.getDataFromDevice("ROADM-C1", LogicalDatastoreType.OPERATIONAL,
                iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT))
            .thenReturn(OlmTransactionUtils.getCurrentPmListC());

        Ots otsValue = new OtsBuilder()
            .setSpanLossTransmit(new RatioDB(Decimal64.valueOf("25")))
            .setSpanLossReceive(new RatioDB(Decimal64.valueOf("25")))
            .build();
        Interface1 ots = new Interface1Builder().setOts(otsValue).build();
        Interface interA = new InterfaceBuilder().setName("OTS-DEG2-TTP-TXRX").setType(OpticalTransport.VALUE)
                .addAugmentation(ots).build();
        Interface interC = new InterfaceBuilder().setName("OTS-DEG1-TTP-TXRX").setType(OpticalTransport.VALUE)
                .addAugmentation(ots).build();
        Optional<Interface> interOptA = Optional.of(interA);
        Optional<Interface> interOptC = Optional.of(interC);
        when(this.openRoadmInterfacesImpl221.getInterface("ROADM-A1", "OTS-DEG2-TTP-TXRX")).thenReturn(interOptA);
        when(this.openRoadmInterfacesImpl221.getInterface("ROADM-C1", "OTS-DEG1-TTP-TXRX")).thenReturn(interOptC);

        doNothing().when(this.openRoadmInterfacesImpl221).postInterface(anyString(), any());

    }

    @Test
    void testCalculateSpanlossBaseLink() {
        // initialise and store openroadm-topology in datastore
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY));
        DataObjectIdentifier<Network> ietfNetworkIID = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, overlayTopologyKey)
                .build();
        Network openroadmTopology = OlmTransactionUtils.getNetworkForSpanLoss();
        OlmTransactionUtils.writeTransaction(this.dataBroker, ietfNetworkIID, openroadmTopology);
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInputLink();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
            assertEquals("Success", output.getResult());
            assertEquals("ROADM-A1-to-ROADM-C1", output.getSpans().get(0).getLinkId().getValue());
            assertEquals("14.6", output.getSpans().get(0).getSpanloss());
        });
    }

    @Test
    void testCalculateSpanlossBaseAll() {
        // initialise and store openroadm-topology in datastore
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY));
        DataObjectIdentifier<Network> ietfNetworkIID = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, overlayTopologyKey)
                .build();
        Network openroadmTopology = OlmTransactionUtils.getNetworkForSpanLoss();
        OlmTransactionUtils.writeTransaction(this.dataBroker, ietfNetworkIID, openroadmTopology);
        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInputAll();

        await().atMost(Duration.ofSeconds(1)).untilAsserted(() -> {
            CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);
            assertEquals("Success", output.getResult());
            assertEquals("ROADM-A1-to-ROADM-C1", output.getSpans().get(0).getLinkId().getValue());
            assertEquals("14.6", output.getSpans().get(0).getSpanloss());
        });
    }
}
