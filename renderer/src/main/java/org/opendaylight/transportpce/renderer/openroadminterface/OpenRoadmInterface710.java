/*
 * Copyright © 2021 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220922.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.TrailTraceOther.TimDetectMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.ParentOduAllocationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.parent.odu.allocation.trib.slots.choice.OpucnBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic14;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic24;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic28;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic36;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic48;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.ProvisionModeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R100G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R100GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R200GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R300GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R400GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Ofec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Off;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Rsfec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Scfec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.Otsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtsiGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev200529.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.otsi.attributes.FlexoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.otsi.container.OtsiBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUTTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUTTPCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexCbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexCbr400G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.otu.container.OtuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otsi.group.interfaces.rev200529.otsi.group.container.OtsiGroupBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOtsiOtsigroup;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterface710 {
    private static final String MODULATION_FMT_EXCEPTION_MESSAGE =
        "Unable to get the modulation format";
    private static final String RATE_EXCEPTION_MESSAGE =
        "Unable to get the rate";
    private static final String ODUC = "-ODUC";
    private static final List<String> SUPPORTED_ODUCN_RATES = List.of("1", "2", "3", "4");
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterface710.class);

    public OpenRoadmInterface710(PortMapping portMapping, OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEthInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder ethInterfaceBldr =
            createGenericInterfaceBuilder(portMap, EthernetCsmacd.VALUE, logicalConnPoint + "-ETHERNET")
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new Interface1Builder()
                        .setEthernet(
                            // Ethernet interface specific data
                            // We have to differentiate if-100GE vs if-400GE
                            // Default set to 400G
                            portMap.getSupportedInterfaceCapability().contains(If100GE.VALUE)
                                // There could be different client pluggables on either side QSFP28-LR4 or QSFP28-FR4
                                // LR4-requires FEC to off, while FR4 can accept even when FEC is off
                                ? new EthernetBuilder().setFec(Off.VALUE).setSpeed(Uint32.valueOf(100000)).build()
                                : new EthernetBuilder().setFec(Rsfec.VALUE).setSpeed(Uint32.valueOf(400000)).build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ethInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack
        openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        return ethInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        ModulationFormat modulationFormat = ModulationFormat.forName(spectrumInformation.getModulationFormat());
        if (modulationFormat == null) {
            modulationFormat =  ModulationFormat.DpQpsk;
        }
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr =
            createGenericInterfaceBuilder(
                    portMap, OpticalChannel.VALUE,
                    spectrumInformation.getIdentifierFromParams(logicalConnPoint))
                .addAugmentation(
                // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev200529
                            .Interface1Builder()
                        .setOch(
                            // OCH interface specific data
                            new OchBuilder()
                                .setFrequency(
                                    new FrequencyTHz(Decimal64.valueOf(spectrumInformation.getCenterFrequency())))
                                .setRate(R100G.VALUE)
                                .setTransmitPower(new PowerDBm(Decimal64.valueOf("-5")))
                                .setModulationFormat(modulationFormat)
                                .build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return ochInterfaceBldr.getName();
    }

    public String createOpenRoadmOtsiInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        ModulationFormat modulationFormat = ModulationFormat.forName(spectrumInformation.getModulationFormat());
        if (modulationFormat == null) {
            throw new OpenRoadmInterfaceException(MODULATION_FMT_EXCEPTION_MESSAGE);
        }
        // OTSI interface specific data
        OtsiBuilder otsiBuilder = new OtsiBuilder()
            .setFrequency(new FrequencyTHz(Decimal64.valueOf(spectrumInformation.getCenterFrequency())))
            .setTransmitPower(new PowerDBm(Decimal64.valueOf("-5")))
            .setProvisionMode(ProvisionModeType.Explicit)
            .setFec(Ofec.VALUE)
            .setModulationFormat(modulationFormat);
        // Use the rate to switch rather than modulation format
        int serviceRate = getServiceRate(modulationFormat, spectrumInformation);
        switch (serviceRate) {
            case 100:
                LOG.info("Given modulation format and spectral width 50GHz {} and thus rate is 100G",
                    modulationFormat);
                // TODO check if FOIC and Gbaud logs could not be rationalized
                LOG.info("FOIC is 1.4 for 31.6 Gbaud and rate is 100");
                otsiBuilder
                    .setOtsiRate(R100GOtsi.VALUE)
                    .setFlexo(new FlexoBuilder()
                        .setFoicType(Foic14.VALUE)
                        .setIid(new ArrayList<>(Arrays.asList(Uint8.valueOf(1))))
                        .build());
                break;
            case 200:
                LOG.info("Given modulation format is {} and thus rate is 200G", modulationFormat);
                FlexoBuilder flexoBuilder = new FlexoBuilder()
                    .setIid(new ArrayList<>(List.of(Uint8.valueOf(1), Uint8.valueOf(2))));
                if (modulationFormat == ModulationFormat.DpQam16) {
                    LOG.info("FOIC is 2.8 for 31.6 Gbaud and rate is 200");
                    // FOIC rate is different
                    flexoBuilder.setFoicType(Foic28.VALUE);
                } else {
                    // default is dp-qpsk for 200G under 63.1 GBaud
                    flexoBuilder.setFoicType(Foic24.VALUE);
                }
                otsiBuilder
                    .setOtsiRate(R200GOtsi.VALUE)
                    .setFlexo(flexoBuilder.build());
                break;
            case 300:
                LOG.info("Given modulation format is {} and thus rate is 300G", modulationFormat);
                otsiBuilder.setOtsiRate(R300GOtsi.VALUE)
                    .setFlexo(new FlexoBuilder()
                        .setFoicType(Foic36.VALUE)
                        .setIid(new ArrayList<>(List.of(Uint8.valueOf(1), Uint8.valueOf(2), Uint8.valueOf(3))))
                        .build());
                break;
            case 400:
                // Default baud-rate is 63.1 Gbaud
                LOG.info("Given modulation format is {} and thus rate is 400G", modulationFormat);
                otsiBuilder
                    .setModulationFormat(modulationFormat)
                    .setOtsiRate(R400GOtsi.VALUE)
                    .setFlexo(new FlexoBuilder()
                        .setFoicType(Foic48.VALUE)
                        .setIid(new ArrayList<>(
                            List.of(Uint8.valueOf(1), Uint8.valueOf(2), Uint8.valueOf(3), Uint8.valueOf(4))))
                        .build());
                break;
            default:
                LOG.error("Rate {} is unsupported", serviceRate);
                throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder otsiInterfaceBldr =
            createGenericInterfaceBuilder(
                    portMap, Otsi.VALUE,
                    spectrumInformation.getIdentifierFromParams(logicalConnPoint))
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http
                            .org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.Interface1Builder()
                        .setOtsi(otsiBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otsiInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return otsiInterfaceBldr.getName();
    }

    // This is a transponder use-case where the supporting port is just one, but YANG model
    // requires supporting port to be list
    public String createOpenRoadmOtsiGroupInterface(String nodeId, String logicalConnPoint,
            String supportingOtsiInterface, SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Check the modulation format
        ModulationFormat modulationFormat = ModulationFormat.forName(spectrumInformation.getModulationFormat());
        if (modulationFormat == null) {
            throw new OpenRoadmInterfaceException(MODULATION_FMT_EXCEPTION_MESSAGE);
        }
        int serviceRate = getServiceRate(modulationFormat, spectrumInformation);
        // Create an OTSI group object
        OtsiGroupBuilder otsiGroupBuilder = new OtsiGroupBuilder().setGroupId(Uint32.valueOf(1));
        switch (serviceRate) {
            case 100:
                otsiGroupBuilder.setGroupRate(R100GOtsi.VALUE);
                break;
            case 200:
                otsiGroupBuilder.setGroupRate(R200GOtsi.VALUE);
                break;
            case 300:
                otsiGroupBuilder.setGroupRate(R300GOtsi.VALUE);
                break;
            case 400:
                otsiGroupBuilder.setGroupRate(R400GOtsi.VALUE);
                break;
            default:
                LOG.error("Rate {} is not supported", serviceRate);
                throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }
        // Create generic interface
        InterfaceBuilder otsiGroupInterfaceBldr =
            createGenericInterfaceBuilder(
                    portMap, OtsiGroup.VALUE,
                    logicalConnPoint + String.join("-", "", "OTSIGROUP", serviceRate + "G"))
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOtsiInterface)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otsi.group.interfaces.rev200529
                            .Interface1Builder()
                        .setOtsiGroup(otsiGroupBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otsiGroupInterfaceBldr);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return otsiGroupInterfaceBldr.getName();
    }

    public String createOpenRoadmOchOtsiOtsigroupInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        if (portMap.getSupportedInterfaceCapability().contains(IfOCHOTU4ODU4.VALUE)) {
            // create OCH interface
            return createOpenRoadmOchInterface(nodeId, logicalConnPoint, spectrumInformation);
        }
        if (portMap.getSupportedInterfaceCapability().contains(IfOtsiOtsigroup.VALUE)) {
            // Create OTSi and OTSi-group and concat the names of the interface
            String interfaceOtsiName = createOpenRoadmOtsiInterface(nodeId, logicalConnPoint, spectrumInformation);
            // And Concat the two names for this interface
            return interfaceOtsiName + "#"
                + createOpenRoadmOtsiGroupInterface(nodeId, logicalConnPoint, interfaceOtsiName, spectrumInformation);
        }
        return null;
    }

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) throws OpenRoadmInterfaceException {
        Mapping mapping = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // OTU interface specific data
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.otu.container.OtuBuilder
                otuIfBuilder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.otu.container.OtuBuilder()
                .setFec(Scfec.VALUE)
                .setRate(OTU4.VALUE);
        if (apiInfoA != null) {
            otuIfBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            otuIfBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        InterfaceBuilder otuInterfaceBldr =
            createGenericInterfaceBuilder(mapping, OtnOtu.VALUE, logicalConnPoint + "-OTU4")
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportOchInterface)))
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.Interface1Builder()
                        .setOtu(otuIfBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otuInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return otuInterfaceBldr.getName();
    }


    public String createOpenRoadmOtucnInterface(String nodeId, String logicalConnPoint,
            String supportingOtsiGroupInterface, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ)
            throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Create an OTUCn object
        OtuBuilder otuBuilder = new OtuBuilder()
            .setRate(OTUCn.VALUE)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100));
        if (apiInfoA != null) {
            otuBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            otuBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        // Set the OTUCn rate for various rates
        String rate = supportingOtsiGroupInterface.substring(supportingOtsiGroupInterface.lastIndexOf('-') + 1);
        String otucnrate = null;
        switch (rate) {
            case "100G":
                otuBuilder.setOtucnNRate(Uint16.valueOf(1));
                otucnrate = "1";
                break;
            case "200G":
                otuBuilder.setOtucnNRate(Uint16.valueOf(2));
                otucnrate = "2";
                break;
            case "300G":
                otuBuilder.setOtucnNRate(Uint16.valueOf(3));
                otucnrate = "3";
                break;
            case "400G":
                otuBuilder.setOtucnNRate(Uint16.valueOf(4));
                otucnrate = "4";
                break;
            default:
                LOG.error("Rate {} is not supported", rate);
                throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }
        InterfaceBuilder otuInterfaceBuilder =
            createGenericInterfaceBuilder(mapping, OtnOtu.VALUE, logicalConnPoint + "-OTUC" + otucnrate)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOtsiGroupInterface)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.Interface1Builder()
                        .setOtu(otuBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otuInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (mapping.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, mapping.getSupportingCircuitPackName(), true);
        }
        this.portMapping.updateMapping(nodeId, mapping);
        return otuInterfaceBuilder.getName();
    }

    public String createOpenRoadmOtu4OtucnInterface(String nodeId, String logicalConnPoint,
            String supportingInterface, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Depending on OCH-OTU4-ODU4 interface or OTSi-OTSi-group, supporting interface should
        // reflect that
        if (portMap.getSupportedInterfaceCapability().contains(IfOCHOTU4ODU4.VALUE)) {
            // create OTU4 interface
            return createOpenRoadmOtu4Interface(nodeId, logicalConnPoint, supportingInterface, apiInfoA, apiInfoZ);
        }
        if (portMap.getSupportedInterfaceCapability().contains(IfOtsiOtsigroup.VALUE)) {
            // Create OTUCn
            return createOpenRoadmOtucnInterface(nodeId, logicalConnPoint, supportingInterface, apiInfoA, apiInfoZ);
        }
        return null;
    }

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(mapping, OtnOdu.VALUE, logicalConnPoint + "-ODU4");
        if (mapping.getSupportingOtu4() != null) {
            oduInterfaceBldr.setSupportingInterfaceList(new HashSet<>(Set.of(mapping.getSupportingOtu4())));
        }
        // Create an ODU4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODU4.VALUE)
            .setOduFunction(ODUTTP.VALUE)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setOpu(
                // OPU payload
                new OpuBuilder()
                    .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                    .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                    .build());
        if (apiInfoA != null) {
            oduBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            oduBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        oduInterfaceBldr
            .addAugmentation(
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                    .setOdu(oduBuilder.build())
                .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        // Since this is not a CTP, we can update the port-mapping
        LOG.info("{}-{} updating mapping with interface {}", nodeId, logicalConnPoint, oduInterfaceBldr.getName());
        this.portMapping.updateMapping(nodeId, mapping);
        return oduInterfaceBldr.getName();
    }


    public String createOpenRoadmOducnInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Used to find if the port is regen-type
        if (portMap.getXpdrType() == null) {
            throw new OpenRoadmInterfaceException(
                    OpenRoadmInterfaceException.mapping_xpdrtype_err(nodeId, logicalConnPoint));
        }
        if (portMap.getSupportingOtucn() == null) {
            throw new OpenRoadmInterfaceException("Missing supporting OTUCn interface on port-mapping");
        }
        String supportingOtucn = portMap.getSupportingOtucn();
        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);
        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }
        // set the common parameters
        OduBuilder oduBuilder = new OduBuilder()
                                    .setRate(ODUCn.VALUE)
                                    .setOducnNRate(Uint16.valueOf(oducnrate));

        if (portMap.getXpdrType() == XpdrNodeTypes.Regen) {
            LOG.info("Regen mode only supports not-terminated or monitored");
            oduBuilder.setMonitoringMode(MonitoringMode.NotTerminated)
                    .setOduFunction(ODUCTP.VALUE);
        } else {
            // if it is other than regen mode
            oduBuilder.setMonitoringMode(MonitoringMode.Terminated)
                    .setTimActEnabled(false)
                    .setOduFunction(ODUTTP.VALUE)
                    .setTimDetectMode(TimDetectMode.Disabled)
                    .setDegmIntervals(Uint8.valueOf(2))
                    .setDegthrPercentage(Uint16.valueOf(100))
                    .setOducnNRate(Uint16.valueOf(oducnrate))
                    .setOpu(
                            // OPU payload
                            new OpuBuilder()
                                    .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .setPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .build());
        }

        InterfaceBuilder oduInterfaceBuilder =
            createGenericInterfaceBuilder(portMap, OtnOdu.VALUE, logicalConnPoint + "-ODUC" + oducnrate)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOtucn)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                        .setOdu(oduBuilder.build())
                            .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return oduInterfaceBuilder.getName();
    }

    // Overloaded methods should be together
    // With SAPI and DAPI information
    public String createOpenRoadmOducnInterface(String anodeId, String alogicalConnPoint,
            String supportingOtucn, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);
        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(anodeId, alogicalConnPoint));
        }
        if (portMapA.getXpdrType() == null) {
            throw new OpenRoadmInterfaceException(
                    OpenRoadmInterfaceException.mapping_xpdrtype_err(anodeId, alogicalConnPoint));
        }
        // On the Zside
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(znodeId, zlogicalConnPoint));
        }
        // set the common parameters
        OduBuilder oduBuilder = new OduBuilder()
                .setRate(ODUCn.VALUE)
                .setOducnNRate(Uint16.valueOf(oducnrate));

        if (portMapA.getXpdrType() == XpdrNodeTypes.Regen) {
            LOG.info("Regen mode only supports not-terminated or monitored");
            oduBuilder.setMonitoringMode(MonitoringMode.NotTerminated)
                    // For regen-mode ODU-function is set to CTP
                    .setOduFunction(ODUCTP.VALUE);
        } else {
            // if it is other than regen mode
            oduBuilder.setMonitoringMode(MonitoringMode.Terminated)
                    .setTimActEnabled(false)
                    .setOduFunction(ODUTTP.VALUE)
                    .setTimDetectMode(TimDetectMode.Disabled)
                    .setDegmIntervals(Uint8.valueOf(2))
                    .setDegthrPercentage(Uint16.valueOf(100))
                    .setOducnNRate(Uint16.valueOf(oducnrate))
                    .setOpu(
                            // OPU payload
                            new OpuBuilder()
                                    .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .setPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .build())
                    .setTxSapi(portMapA.getLcpHashVal())
                    .setTxDapi(portMapZ.getLcpHashVal())
                    .setExpectedSapi(portMapZ.getLcpHashVal())
                    .setExpectedDapi(portMapZ.getLcpHashVal());
        }

        InterfaceBuilder oduInterfaceBuilder =
            createGenericInterfaceBuilder(portMapA, OtnOdu.VALUE, alogicalConnPoint + ODUC + oducnrate)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOtucn)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                        .setOdu(oduBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMapA.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(anodeId, portMapA.getSupportingCircuitPackName(), true);
        }
        return oduInterfaceBuilder.getName();
    }


    // This is only for transponder
    public String createOpenRoadmOduflexInterface(String nodeId, String logicalConnPoint,
            String supportingOducn)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Parent Odu-allocation
        // Set the trib-slot array
        Set<OpucnTribSlotDef> tribslots = new HashSet<>();
        // Here the int stream is based on rate
        // Get the rate, which can be 1, 2, 3 or 4 4=400G, 1=100G
        String rate = supportingOducn.substring(supportingOducn.length() - 1);
        IntStream.range(1, Integer.parseInt(rate) + 1)
            .forEach(a -> IntStream.range(1, 21)
                .forEach(b -> tribslots.add(OpucnTribSlotDef.getDefaultInstance(a + "." + b))));
        // Create an ODUFlex object
        OduBuilder oduBuilder = new OduBuilder()
            .setOduFunction(ODUTTPCTP.VALUE)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setParentOduAllocation(
                new ParentOduAllocationBuilder()
                    .setTribPortNumber(Uint16.valueOf(1))
                    .setTribSlotsChoice(new OpucnBuilder().setOpucnTribSlots(tribslots).build())
                    .build());
        // Build the OPU container to the ODU builder
        switch (rate) {
            case "1":
                oduBuilder
                    .setRate(ODU4.VALUE)
                    .setOpu(
                        new OpuBuilder()
                            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                            .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                            .build());
                logicalConnPoint += "-ODU4";
                break;
            case "4":
                oduBuilder
                    .setRate(ODUflexCbr.VALUE)
                    .setOduflexCbrService(ODUflexCbr400G.VALUE)
                    .setOpu(
                        new OpuBuilder()
                            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("32"))
                            .setPayloadType(PayloadTypeDef.getDefaultInstance("32"))
                            .build());
                logicalConnPoint += "-ODUFLEX";
                break;
            default:
                oduBuilder.setOpu(new OpuBuilder().build());
                break;
        }
        InterfaceBuilder oduflexInterfaceBuilder =
            createGenericInterfaceBuilder(portMap, OtnOdu.VALUE, logicalConnPoint)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOducn)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                        .setOdu(oduBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduflexInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        return oduflexInterfaceBuilder.getName();
    }

    // Overloaded methods should be together
    // This is only for transponder; with SAPI/DAPI information
    public String createOpenRoadmOduflexInterface(String anodeId, String alogicalConnPoint,
            String supportingOducn, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(anodeId, alogicalConnPoint));
        }
        // On the Zside
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(znodeId, zlogicalConnPoint));
        }
        // Parent Odu-allocation
        // Set the trib-slot array
        Set<OpucnTribSlotDef> tribslots = new HashSet<>();
        // Here the int stream is based on rate
        // Get the rate, which can be 1, 2, 3 or 4 4=400G, 1=100G
        String rate = supportingOducn.substring(supportingOducn.lastIndexOf('-') + 1);
        IntStream.range(1, Integer.parseInt(rate) + 1)
            .forEach(a -> IntStream.range(1, 21)
                .forEach(b -> tribslots.add(OpucnTribSlotDef.getDefaultInstance(a + "." + b))));
        // Create an ODUFlex object
        OduBuilder oduBuilder = new OduBuilder()
            .setOduFunction(ODUTTPCTP.VALUE)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            // TODO the following line seemed to come a bit early
            // so it is now commented out and the code was aligned with previous method
            //.setOpu(opuBuilder.build())
            .setParentOduAllocation(
                new ParentOduAllocationBuilder()
                    .setTribPortNumber(Uint16.valueOf(1))
                    .setTribSlotsChoice(new OpucnBuilder().setOpucnTribSlots(tribslots).build())
                    .build());
        switch (rate) {
            case "1":
                oduBuilder
                    .setRate(ODU4.VALUE)
                    .setOpu(
                        new OpuBuilder()
                            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                            .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                            .build());
                alogicalConnPoint += "-ODU4";
                break;
            case "4":
                oduBuilder
                    .setRate(ODUflexCbr.VALUE)
                    .setOduflexCbrService(ODUflexCbr400G.VALUE)
                    .setOpu(
                        new OpuBuilder()
                            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("32"))
                            .setPayloadType(PayloadTypeDef.getDefaultInstance("32"))
                            .build());
                alogicalConnPoint += "-ODUFLEX";
                break;
            default:
                oduBuilder.setOpu(new OpuBuilder().build());
                break;
        }
        InterfaceBuilder oduflexInterfaceBuilder =
            createGenericInterfaceBuilder(portMapA, OtnOdu.VALUE, alogicalConnPoint)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOducn)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                        .setOdu(oduBuilder.build())
                        .build());
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMapA.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(anodeId, portMapA.getSupportingCircuitPackName(), true);
        }
        return oduflexInterfaceBuilder.getName();
    }

    public String createOpenRoadmOdu4OducnOduflex(String nodeId, String logicalConnPoint,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Depending on OTU4 or OTUCn, supporting interface should
        // reflect that
        String interfaceOdu4OducnOduflex = null;
        // Depending on OTU4 or OTUCn, supporting interface should reflect that
        if (portMap.getSupportedInterfaceCapability().contains(IfOCHOTU4ODU4.VALUE)) {
            // create OTU4 interface
            return createOpenRoadmOdu4Interface(nodeId, logicalConnPoint, apiInfoA, apiInfoZ);
        }
        if (portMap.getSupportedInterfaceCapability().contains(IfOtsiOtsigroup.VALUE)) {
            // Create ODUCn and ODUFlex interface.
            String interfaceOducn = createOpenRoadmOducnInterface(nodeId, logicalConnPoint);
            return interfaceOducn + "#"
                + createOpenRoadmOduflexInterface(nodeId, logicalConnPoint, interfaceOducn);
        }
        return null;
    }

    public String createOpenRoadmOtnOducnInterface(String nodeId, String logicalConnPoint,
            String supportingOtucn)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Used to find if the port is regen-type
        if (portMap.getXpdrType() == null) {
            throw new OpenRoadmInterfaceException(
                    OpenRoadmInterfaceException.mapping_xpdrtype_err(nodeId, logicalConnPoint));
        }
        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);
        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }
        // set the common parameters
        OduBuilder oduBuilder = new OduBuilder()
                .setRate(ODUCn.VALUE)
                .setOducnNRate(Uint16.valueOf(oducnrate));

        if (portMap.getXpdrType() == XpdrNodeTypes.Regen) {
            LOG.info("Regen mode only supports not-terminated or monitored");
            oduBuilder.setMonitoringMode(MonitoringMode.NotTerminated)
                    .setOduFunction(ODUCTP.VALUE);
        } else {
            // if it is other than regen mode
            oduBuilder.setMonitoringMode(MonitoringMode.Terminated)
                    .setTimActEnabled(false)
                    .setOduFunction(ODUTTP.VALUE)
                    .setTimDetectMode(TimDetectMode.Disabled)
                    .setDegmIntervals(Uint8.valueOf(2))
                    .setDegthrPercentage(Uint16.valueOf(100))
                    .setOducnNRate(Uint16.valueOf(oducnrate))
                    .setOpu(
                            // OPU payload
                            new OpuBuilder()
                                    .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .setPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .build());
        }

        InterfaceBuilder oduInterfaceBuilder =
            createGenericInterfaceBuilder(portMap, OtnOdu.VALUE, logicalConnPoint + ODUC + oducnrate)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOtucn)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                        .setOdu(oduBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }
        // Update the port-mapping with the interface information
        this.portMapping.updateMapping(nodeId, portMap);
        return oduInterfaceBuilder.getName();
    }


    // With SAPI and DAPI information
    public String createOpenRoadmOtnOducnInterface(String anodeId, String alogicalConnPoint,
            String supportingOtucn, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(anodeId, alogicalConnPoint));
        }
        if (portMapA.getXpdrType() == null) {
            throw new OpenRoadmInterfaceException(
                    OpenRoadmInterfaceException.mapping_xpdrtype_err(anodeId, alogicalConnPoint));
        }
        // On the Zside
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(znodeId, zlogicalConnPoint));
        }
        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);
        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(RATE_EXCEPTION_MESSAGE);
        }

        // set the common parameters
        OduBuilder oduBuilder = new OduBuilder()
                .setRate(ODUCn.VALUE)
                .setOducnNRate(Uint16.valueOf(oducnrate));

        if (portMapA.getXpdrType() == XpdrNodeTypes.Regen) {
            LOG.info("Regen mode only supports not-terminated or monitored");
            oduBuilder.setMonitoringMode(MonitoringMode.NotTerminated)
                    // For regen-mode ODU-function is set to CTP
                    .setOduFunction(ODUCTP.VALUE);
        } else {
            // if it is other than regen mode
            oduBuilder.setMonitoringMode(MonitoringMode.Terminated)
                    .setTimActEnabled(false)
                    .setOduFunction(ODUTTP.VALUE)
                    .setTimDetectMode(TimDetectMode.Disabled)
                    .setDegmIntervals(Uint8.valueOf(2))
                    .setDegthrPercentage(Uint16.valueOf(100))
                    .setOducnNRate(Uint16.valueOf(oducnrate))
                    .setOpu(
                            // OPU payload
                            new OpuBuilder()
                                    .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .setPayloadType(PayloadTypeDef.getDefaultInstance("22"))
                                    .build())
                    .setTxSapi(portMapA.getLcpHashVal())
                    .setTxDapi(portMapZ.getLcpHashVal())
                    .setExpectedSapi(portMapZ.getLcpHashVal())
                    .setExpectedDapi(portMapZ.getLcpHashVal());
        }

        InterfaceBuilder oduInterfaceBuilder =
            createGenericInterfaceBuilder(portMapA, OtnOdu.VALUE, alogicalConnPoint + ODUC + oducnrate)
                .setSupportingInterfaceList(new HashSet<>(Set.of(supportingOtucn)))
                .addAugmentation(
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder()
                        .setOdu(oduBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMapA.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(anodeId, portMapA.getSupportingCircuitPackName(), true);
        }
        // Update the port-mapping with the interface information
        this.portMapping.updateMapping(anodeId, portMapA);
        return oduInterfaceBuilder.getName();
    }

    // This creates the name of the interface with slot numbers at the end
    public String createOpenRoadmOtsiInterfaceName(String logicalConnectionPoint, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR, logicalConnectionPoint, spectralSlotName);
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, InterfaceType type, String key) {
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder()
            .setDescription("  TBD   ")
            .setCircuitId("   TBD    ")
            .setSupportingCircuitPackName(portMap.getSupportingCircuitPackName())
            .setSupportingPort(portMap.getSupportingPort())
            .setAdministrativeState(AdminStates.InService)
            .setType(type)
            .setName(key)
            .withKey(new InterfaceKey(key));
    }

    private int getServiceRate(ModulationFormat modulationFormat, SpectrumInformation spectrumInformation) {
        switch (modulationFormat) {
            case DpQpsk:
            case DpQam16:
                // DpQpsk and DpQam16 are possible for both 31.6 or 63.1 GBaud, for which spectral width is different
                // Here take the difference of highest and lowest spectral numbers and determine the width
                double spectralWidth = (spectrumInformation.getHigherSpectralSlotNumber()
                    - spectrumInformation.getLowerSpectralSlotNumber() + 1) * GridConstant.GRANULARITY;
                LOG.info("The width with guard band {}", spectralWidth);
                if (spectralWidth == 50.0) {
                    LOG.info("The baud-rate is 31.6 GBaud");
                    return Map.of(
                            ModulationFormat.DpQpsk , 100,
                            ModulationFormat.DpQam16 , 200)
                        .get(modulationFormat);
                    // Based on roll-of-factor of 0.2, 50 - 12.5 = 37.5GHz translates to 31.6 GBaud
                }
                LOG.info("The baud-rate is 63.1 GBaud");
                Map<ModulationFormat, Integer> rateMap = Map.of(
                        ModulationFormat.DpQpsk , 200,
                        ModulationFormat.DpQam16 , 400);
                // Based on roll-of-factor of 0.2, 87.5 - 12.5 = 75GHz translates to 63.1 GBaud
                int rate = rateMap.get(modulationFormat);
                LOG.info("Given modulation format {} rate is {}", modulationFormat, rate);
                return rate;
            case DpQam8:
                LOG.info("Given modulation format DpQam8 rate is 300");
                return 300;
            default:
                LOG.error(RATE_EXCEPTION_MESSAGE + " for modulation format {}", modulationFormat);
                return 0;
        }
    }
}
