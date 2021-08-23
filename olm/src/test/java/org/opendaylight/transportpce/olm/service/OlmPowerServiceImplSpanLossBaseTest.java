/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.DeviceTransactionManagerImpl;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.MappingUtilsImpl;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.mapping.PortMappingImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl121;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl221;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl710;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.power.PowerMgmtImpl;
import org.opendaylight.transportpce.olm.util.OlmPowerServiceRpcImplUtil;
import org.opendaylight.transportpce.olm.util.OlmTransactionUtils;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
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
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmPowerServiceImplSpanLossBaseTest extends AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerServiceImplSpanLossBaseTest.class);
    private DeviceTransactionManager deviceTransactionManager;
    private OpenRoadmInterfaces openRoadmInterfaces;
    private PortMapping portMapping;
    private PowerMgmt powerMgmt;
    private MappingUtils mappingUtils;
    private OpenRoadmInterfacesImpl221 openRoadmInterfacesImpl221;
    private OpenRoadmInterfacesImpl121 openRoadmInterfacesImpl121;
    private OpenRoadmInterfacesImpl710 openRoadmInterfacesImpl710;
    private OlmPowerService olmPowerService;
    private DataBroker dataBroker;

    @Before
    public void setUp() throws OpenRoadmInterfaceException {
        this.openRoadmInterfacesImpl121 = Mockito.mock(OpenRoadmInterfacesImpl121.class);
        this.openRoadmInterfacesImpl221 = Mockito.mock(OpenRoadmInterfacesImpl221.class);
        this.mappingUtils = Mockito.mock(MappingUtilsImpl.class);
        this.portMapping = Mockito.mock(PortMappingImpl.class);
        this.deviceTransactionManager = Mockito.mock(DeviceTransactionManagerImpl.class);
        this.powerMgmt = Mockito.mock(PowerMgmtImpl.class);
        this.openRoadmInterfaces = new OpenRoadmInterfacesImpl(this.deviceTransactionManager, this.mappingUtils,
                this.openRoadmInterfacesImpl121, this.openRoadmInterfacesImpl221, this.openRoadmInterfacesImpl710);
        this.olmPowerService = new OlmPowerServiceImpl(getDataBroker(), this.powerMgmt,
                this.deviceTransactionManager, this.portMapping, this.mappingUtils, this.openRoadmInterfaces);
        this.dataBroker = getDataBroker();
        MockitoAnnotations.initMocks(this);
        Mockito.doReturn(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1).when(this.mappingUtils)
                .getOpenRoadmVersion(Mockito.anyString());

        Mockito.when(this.portMapping.getMapping("ROADM-A1", "DEG2-TTP-TXRX"))
                .thenReturn(OlmTransactionUtils.getMapping1());
        Mockito.when(this.portMapping.getMapping("ROADM-C1", "DEG1-TTP-TXRX"))
                .thenReturn(OlmTransactionUtils.getMapping2());

        InstanceIdentifier<CurrentPmList> iidCurrentPmList = InstanceIdentifier.create(CurrentPmList.class);
        Mockito.when(this.deviceTransactionManager.getDataFromDevice("ROADM-A1", LogicalDatastoreType.OPERATIONAL,
                iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)).thenReturn(OlmTransactionUtils.getCurrentPmListA());
        Mockito.when(this.deviceTransactionManager.getDataFromDevice("ROADM-C1", LogicalDatastoreType.OPERATIONAL,
                iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)).thenReturn(OlmTransactionUtils.getCurrentPmListC());

        InstanceIdentifier<Interface> interfacesIIDA = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey("OTS-DEG2-TTP-TXRX"));
        InstanceIdentifier<Interface> interfacesIIDC = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey("OTS-DEG1-TTP-TXRX"));
        Optional<Interface> interfaceA = Optional.of(new InterfaceBuilder().setName("OTS-DEG2-TTP-TXRX").build());
        Optional<Interface> interfaceC = Optional.of(new InterfaceBuilder().setName("OTS-DEG1-TTP-TXRX").build());
        Mockito.when(this.deviceTransactionManager.getDataFromDevice("ROADM-A1", LogicalDatastoreType.CONFIGURATION,
                interfacesIIDA, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)).thenReturn(interfaceA);
        Mockito.when(this.deviceTransactionManager.getDataFromDevice("ROADM-C1", LogicalDatastoreType.CONFIGURATION,
                interfacesIIDC, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT)).thenReturn(interfaceC);

        Ots otsValue = new OtsBuilder().setSpanLossTransmit(new RatioDB(new BigDecimal(25)))
                .setSpanLossReceive(new RatioDB(new BigDecimal(25))).build();
        Interface1 ots = new Interface1Builder().setOts(otsValue).build();
        Interface interA = new InterfaceBuilder().setName("OTS-DEG2-TTP-TXRX").setType(OpticalTransport.class)
                .addAugmentation(ots).build();
        Interface interC = new InterfaceBuilder().setName("OTS-DEG1-TTP-TXRX").setType(OpticalTransport.class)
                .addAugmentation(ots).build();
        Optional<Interface> interOptA = Optional.of(interA);
        Optional<Interface> interOptC = Optional.of(interC);
        Mockito.when(this.openRoadmInterfacesImpl221.getInterface("ROADM-A1", "OTS-DEG2-TTP-TXRX"))
                .thenReturn(interOptA);
        Mockito.when(this.openRoadmInterfacesImpl221.getInterface("ROADM-C1", "OTS-DEG1-TTP-TXRX"))
                .thenReturn(interOptC);

        InterfaceBuilder ifABldr = new InterfaceBuilder();
        Mockito.doNothing().when(this.openRoadmInterfacesImpl221).postInterface("ROADM-A1", ifABldr);
        Mockito.doNothing().when(this.openRoadmInterfacesImpl221).postInterface("ROADM-C1", ifABldr);

    }

    @Test
    public void testCalculateSpanlossBaseLink() {
        // initialise and store openroadm-topology in datastore
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network> ietfNetworkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, overlayTopologyKey).build();
        Network openroadmTopology = OlmTransactionUtils.getNetworkForSpanLoss();
        OlmTransactionUtils.writeTransaction(this.dataBroker, ietfNetworkIID, openroadmTopology);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !", e);
        }

        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInputLink();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);

        Assert.assertEquals("Success", output.getResult());
        Assert.assertEquals("ROADM-A1-to-ROADM-C1", output.getSpans().get(0).getLinkId().getValue());
        Assert.assertEquals("14.6", output.getSpans().get(0).getSpanloss());
    }

    @Test
    public void testCalculateSpanlossBaseAll() {

        // initialise and store openroadm-topology in datastore
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));
        InstanceIdentifier<Network> ietfNetworkIID = InstanceIdentifier.builder(Networks.class)
                .child(Network.class, overlayTopologyKey).build();
        Network openroadmTopology = OlmTransactionUtils.getNetworkForSpanLoss();
        OlmTransactionUtils.writeTransaction(this.dataBroker, ietfNetworkIID, openroadmTopology);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.error("Write transaction failed !", e);
        }

        CalculateSpanlossBaseInput input = OlmPowerServiceRpcImplUtil.getCalculateSpanlossBaseInputAll();
        CalculateSpanlossBaseOutput output = this.olmPowerService.calculateSpanlossBase(input);

        Assert.assertEquals("Success", output.getResult());
        Assert.assertEquals("ROADM-A1-to-ROADM-C1", output.getSpans().get(0).getLinkId().getValue());
        Assert.assertEquals("14.6", output.getSpans().get(0).getSpanloss());
    }
}
