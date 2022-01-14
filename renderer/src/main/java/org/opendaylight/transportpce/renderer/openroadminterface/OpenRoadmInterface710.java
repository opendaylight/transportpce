/*
 * Copyright © 2021 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220114.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.TrailTraceOther.TimDetectMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.ParentOduAllocationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.parent.odu.allocation.trib.slots.choice.OpucnBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic24;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic36;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic48;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.ProvisionModeType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R200GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R300GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.R400GOtsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Ofec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev200529.Rsfec;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev191129.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev200529.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.Otsi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev191129.OtsiGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.loopback.rev191129.maint.loopback.MaintLoopbackBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.testsignal.rev200529.maint.testsignal.MaintTestsignal.TestPattern;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.testsignal.rev200529.maint.testsignal.MaintTestsignalBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.otsi.attributes.FlexoBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.otsi.container.OtsiBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUTTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUTTPCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexCbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.ODUflexCbr400G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OTUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev200327.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.otu.container.OtuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otsi.group.interfaces.rev200529.otsi.group.container.OtsiGroupBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.opendaylight.yangtools.yang.common.Uint8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterface710 {
    private static final String MAPPING_ERROR_EXCEPTION_MESSAGE =
        "Unable to get mapping from PortMapping for node % and logical connection port %s";
    private static final String MODULATION_FMT_EXCEPTION_MESSAGE =
        "Unable to get the modulation format";
    private static final String RATE_EXCEPTION_MESSAGE =
        "Unable to get the rate";
    private static final String ODUC = "-ODUC";
    private static final List<String> SUPPORTED_ODUCN_RATES = new ArrayList<>() {
        {
            add("2");
            add("3");
            add("4");
        }
    };
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
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
            .setFec(Rsfec.class)
            .setSpeed(Uint32.valueOf(400000));

        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(portMap, EthernetCsmacd.class,
            logicalConnPoint + "-ETHERNET");
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ethInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack
        openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);

        return ethInterfaceBldr.getName();
    }


    public String createOpenRoadmOtsiInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {

        ModulationFormat modulationFormat;
        Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
            .forName(spectrumInformation.getModulationFormat());
        if (optionalModulationFormat.isPresent()) {
            modulationFormat =  optionalModulationFormat.get();
        }
        else {
            throw new OpenRoadmInterfaceException(
                String.format(MODULATION_FMT_EXCEPTION_MESSAGE));
        }

        // OTSI interface specific data
        OtsiBuilder  otsiBuilder = new OtsiBuilder()
            .setFrequency(new FrequencyTHz(spectrumInformation.getCenterFrequency()))
            .setTransmitPower(new PowerDBm(new BigDecimal("-5")))
            .setProvisionMode(ProvisionModeType.Explicit)
            .setFec(Ofec.class)
            .setModulationFormat(modulationFormat);

        // Set the Flexo values
        FlexoBuilder flexoBuilder = new FlexoBuilder();
        boolean rateNotFound = false;
        // Use the rate to switch rather than modulation format
        int serviceRate = getServiceRate(modulationFormat, spectrumInformation);
        switch (serviceRate) {
            case 200:
                LOG.info("Given modulation format is {} and thus rate is 200G", modulationFormat);
                flexoBuilder.setFoicType(Foic24.class)
                    .setIid(new ArrayList<>(Arrays.asList(Uint8.valueOf(1), Uint8.valueOf(2))));
                otsiBuilder.setOtsiRate(R200GOtsi.class)
                    .setFlexo(flexoBuilder.build());
                break;
            case 300:
                LOG.info("Given modulation format is {} and thus rate is 300G", modulationFormat);
                flexoBuilder.setFoicType(Foic36.class)
                    .setIid(new ArrayList<>(Arrays.asList(Uint8.valueOf(1), Uint8.valueOf(2),
                        Uint8.valueOf(3))));
                otsiBuilder.setOtsiRate(R300GOtsi.class)
                    .setFlexo(flexoBuilder.build());
                break;
            case 400:
                // Default baud-rate is 63.1 Gbaud
                LOG.info("Given modulation format is {} and thus rate is 400G", modulationFormat);
                flexoBuilder.setFoicType(Foic48.class)
                    .setIid(new ArrayList<>(Arrays.asList(Uint8.valueOf(1), Uint8.valueOf(2),
                        Uint8.valueOf(3), Uint8.valueOf(4))));
                otsiBuilder.setModulationFormat(modulationFormat)
                    .setOtsiRate(R400GOtsi.class)
                    .setFlexo(flexoBuilder.build());
                break;
            default:
                LOG.error("Rate {} is unsupported", serviceRate);
                rateNotFound = true;
                break;
        }

        if (rateNotFound) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder otsiInterfaceBldr = createGenericInterfaceBuilder(portMap, Otsi.class,
            spectrumInformation.getIdentifierFromParams(logicalConnPoint));
        // Create Interface1 type object required for adding as augmentation
        org.opendaylight.yang.gen.v1.http
            .org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.Interface1Builder otsiIf1Builder =
            new org.opendaylight.yang.gen.v1.http
                .org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.Interface1Builder();

        otsiInterfaceBldr.addAugmentation(otsiIf1Builder.setOtsi(otsiBuilder.build()).build());

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
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // Check the modulation format
        ModulationFormat modulationFormat;
        Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
            .forName(spectrumInformation.getModulationFormat());
        if (optionalModulationFormat.isPresent()) {
            modulationFormat =  optionalModulationFormat.get();
        }
        else {
            throw new OpenRoadmInterfaceException(
                String.format(MODULATION_FMT_EXCEPTION_MESSAGE));
        }
        int serviceRate = getServiceRate(modulationFormat, spectrumInformation);
        // Create an OTSI group object
        OtsiGroupBuilder otsiGroupBuilder = new OtsiGroupBuilder()
            .setGroupId(Uint32.valueOf(1));
        boolean rateNotFound = false;
        switch (serviceRate) {
            case 200:
                otsiGroupBuilder.setGroupRate(R200GOtsi.class);
                break;
            case 300:
                otsiGroupBuilder.setGroupRate(R300GOtsi.class);
                break;
            case 400:
                otsiGroupBuilder.setGroupRate(R400GOtsi.class);
                break;
            default:
                LOG.error("Rate {} is not supported", serviceRate);
                rateNotFound = true;
                break;
        }
        if (rateNotFound) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        // Create generic interface
        InterfaceBuilder otsiGroupInterfaceBldr = createGenericInterfaceBuilder(portMap, OtsiGroup.class,
            logicalConnPoint + "-OTSIGROUP-" + serviceRate + "G");
        // Create a list
        List<String> listSupportingOtsiInterface = new ArrayList<>();
        listSupportingOtsiInterface.add(supportingOtsiInterface);
        otsiGroupInterfaceBldr.setSupportingInterfaceList(listSupportingOtsiInterface);

        org.opendaylight.yang.gen.v1.http.org.openroadm.otsi.group.interfaces.rev200529.Interface1Builder
            otsiGroupIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otsi.group.interfaces.rev200529.Interface1Builder();
        otsiGroupInterfaceBldr.addAugmentation(otsiGroupIf1Builder.setOtsiGroup(otsiGroupBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otsiGroupInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }

        return otsiGroupInterfaceBldr.getName();
    }

    public String createOpenRoadmOtucnInterface(String nodeId, String logicalConnPoint,
            String supportingOtsiGroupInterface, AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ)
            throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // Create an OTUCn object
        MaintLoopbackBuilder maintLoopbackBuilder = new MaintLoopbackBuilder();
        maintLoopbackBuilder.setEnabled(false);
        OtuBuilder otuBuilder = new OtuBuilder()
            .setRate(OTUCn.class)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setMaintLoopback(maintLoopbackBuilder.build());
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
        boolean rateNotFound = false;
        switch (rate) {
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
                rateNotFound = true;
                break;
        }
        if (rateNotFound) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        InterfaceBuilder otuInterfaceBuilder = createGenericInterfaceBuilder(mapping, OtnOtu.class,
            logicalConnPoint + "-OTUC" + otucnrate);

        // Create a list
        List<String> listSupportingOtsiGroupInterface = new ArrayList<>();
        listSupportingOtsiGroupInterface.add(supportingOtsiGroupInterface);

        otuInterfaceBuilder.setSupportingInterfaceList(listSupportingOtsiGroupInterface);
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.Interface1Builder otuIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.Interface1Builder();

        otuInterfaceBuilder.addAugmentation(otuIf1Builder.setOtu(otuBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otuInterfaceBuilder);
        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (mapping.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, mapping.getSupportingCircuitPackName(), true);
        }
        this.portMapping.updateMapping(nodeId, mapping);
        return otuInterfaceBuilder.getName();
    }

    public String createOpenRoadmOducnInterface(String nodeId, String logicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // Create ODUcn object
        // Start with OPU object
        // OPU payload
        OpuBuilder opuBuilder = new OpuBuilder()
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
            .setPayloadType(PayloadTypeDef.getDefaultInstance("22"));

        // Create an ODUCn object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOduFunction(ODUTTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOducnNRate(Uint16.valueOf(4))
            .setOpu(opuBuilder.build());

        // Create a list
        String supportingOtucn;
        List<String> listSupportingOtucnInterface = new ArrayList<>();
        if (mapping.getSupportingOtucn() != null) {
            listSupportingOtucnInterface.add(mapping.getSupportingOtucn());
            supportingOtucn = mapping.getSupportingOtucn();
        }
        else {
            throw new OpenRoadmInterfaceException(
                String.format("Missing supporting OTUCn interface on port-mapping"));
        }

        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);
        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        oduBuilder.setOducnNRate(Uint16.valueOf(oducnrate));

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(mapping, OtnOdu.class,
            logicalConnPoint + "-ODUC" + oducnrate);

        oduInterfaceBuilder.setSupportingInterfaceList(listSupportingOtucnInterface);
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();

        oduInterfaceBuilder.addAugmentation(oduIf1Builder.setOdu(oduBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBuilder);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (mapping.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, mapping.getSupportingCircuitPackName(), true);
        }

        return oduInterfaceBuilder.getName();
    }

    // With SAPI and DAPI information
    public String createOpenRoadmOducnInterface(String anodeId, String alogicalConnPoint,
            String supportingOtucn, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    anodeId, alogicalConnPoint));
        }
        // On the Zside
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    znodeId, zlogicalConnPoint));

        }
        // Create ODUcn object
        // Start with OPU object
        // OPU payload
        OpuBuilder opuBuilder = new OpuBuilder()
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
            .setPayloadType(PayloadTypeDef.getDefaultInstance("22"));

        // Create an ODUC4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOduFunction(ODUTTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOpu(opuBuilder.build())
            .setTxSapi(portMapA.getLcpHashVal())
            .setTxDapi(portMapZ.getLcpHashVal())
            .setExpectedSapi(portMapZ.getLcpHashVal())
            .setExpectedDapi(portMapZ.getLcpHashVal());

        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);

        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        oduBuilder.setOducnNRate(Uint16.valueOf(oducnrate));

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + ODUC + oducnrate);

        // Create a list
        List<String> listSupportingOtucnInterface = new ArrayList<>();
        listSupportingOtucnInterface.add(supportingOtucn);

        oduInterfaceBuilder.setSupportingInterfaceList(listSupportingOtucnInterface);
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();

        oduInterfaceBuilder.addAugmentation(oduIf1Builder.setOdu(oduBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBuilder);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMapA.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(anodeId, portMapA.getSupportingCircuitPackName(), true);
        }

        return oduInterfaceBuilder.getName();
    }

    public String createOpenRoadmOtnOducnInterface(String nodeId, String logicalConnPoint,
        String supportingOtucn)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // Create ODUcn object
        // Start with OPU object
        // OPU payload
        OpuBuilder opuBuilder = new OpuBuilder()
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
            .setPayloadType(PayloadTypeDef.getDefaultInstance("22"));

        // Create an ODUC4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOduFunction(ODUTTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOpu(opuBuilder.build());

        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);

        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        oduBuilder.setOducnNRate(Uint16.valueOf(oducnrate));

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMap, OtnOdu.class,
            logicalConnPoint + ODUC + oducnrate);

        // Create a list
        List<String> listSupportingOtucnInterface = new ArrayList<>();
        listSupportingOtucnInterface.add(supportingOtucn);

        oduInterfaceBuilder.setSupportingInterfaceList(listSupportingOtucnInterface);
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();

        oduInterfaceBuilder.addAugmentation(oduIf1Builder.setOdu(oduBuilder.build()).build());

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
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    anodeId, alogicalConnPoint));
        }
        // On the Zside
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    znodeId, zlogicalConnPoint));

        }
        // Create ODUcn object
        // Start with OPU object
        // OPU payload
        OpuBuilder opuBuilder = new OpuBuilder()
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("22"))
            .setPayloadType(PayloadTypeDef.getDefaultInstance("22"));

        // Create an ODUCn object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOduFunction(ODUTTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOpu(opuBuilder.build())
            .setTxSapi(portMapA.getLcpHashVal())
            .setTxDapi(portMapZ.getLcpHashVal())
            .setExpectedSapi(portMapZ.getLcpHashVal())
            .setExpectedDapi(portMapZ.getLcpHashVal());

        // Set the ODUCn rate from OTUCn interface naming convention
        String oducnrate = supportingOtucn.substring(supportingOtucn.length() - 1);

        // check if the oducnrate is a valid value and if it is invalid, then throw error
        if (!SUPPORTED_ODUCN_RATES.contains(oducnrate)) {
            throw new OpenRoadmInterfaceException(
                String.format(RATE_EXCEPTION_MESSAGE));
        }

        oduBuilder.setOducnNRate(Uint16.valueOf(oducnrate));

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + ODUC + oducnrate);

        // Create a list
        List<String> listSupportingOtucnInterface = new ArrayList<>();
        listSupportingOtucnInterface.add(supportingOtucn);

        oduInterfaceBuilder.setSupportingInterfaceList(listSupportingOtucnInterface);
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();

        oduInterfaceBuilder.addAugmentation(oduIf1Builder.setOdu(oduBuilder.build()).build());

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

    // This is only for transponder
    public String createOpenRoadmOduflexInterface(String nodeId, String logicalConnPoint,
            String supportingOducn)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // OPU payload
        OpuBuilder opuBuilder = new OpuBuilder()
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("32"))
            .setPayloadType(PayloadTypeDef.getDefaultInstance("32"));

        // Parent Odu-allocation
        // Set the trib-slot array
        List<OpucnTribSlotDef> tribslots = new ArrayList<>();
        IntStream.range(1, 5).forEach(a -> IntStream.range(1, 21).forEach(b -> tribslots.add(
            OpucnTribSlotDef.getDefaultInstance(a + "." + b))));

        ParentOduAllocationBuilder parentOduAllocationBuilder = new ParentOduAllocationBuilder()
            .setTribPortNumber(Uint16.valueOf(1))
            .setTribSlotsChoice(new OpucnBuilder().setOpucnTribSlots(tribslots).build());

        // Create an ODUFlex object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUflexCbr.class)
            .setOduflexCbrService(ODUflexCbr400G.class)
            .setOduFunction(ODUTTPCTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOpu(opuBuilder.build())
            .setParentOduAllocation(parentOduAllocationBuilder.build());

        InterfaceBuilder oduflexInterfaceBuilder = createGenericInterfaceBuilder(portMap, OtnOdu.class,
            logicalConnPoint + "-ODUFLEX");

        List<String> listSupportingOtucnInterface = new ArrayList<>();
        listSupportingOtucnInterface.add(supportingOducn);

        oduflexInterfaceBuilder.setSupportingInterfaceList(listSupportingOtucnInterface);


        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder
                oduflexIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();

        oduflexInterfaceBuilder.addAugmentation(oduflexIf1Builder.setOdu(oduBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduflexInterfaceBuilder);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }

        return oduflexInterfaceBuilder.getName();
    }

    // This is only for transponder; with SAPI/DAPI information
    public String createOpenRoadmOduflexInterface(String anodeId, String alogicalConnPoint,
            String supportingOducn, String znodeId, String zlogicalConnPoint)
            throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    anodeId, alogicalConnPoint));
        }
        // On the Zside
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    znodeId, zlogicalConnPoint));

        }
        // OPU payload
        OpuBuilder opuBuilder = new OpuBuilder()
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("32"))
            .setPayloadType(PayloadTypeDef.getDefaultInstance("32"));

        // Maint test signal
        MaintTestsignalBuilder maintTestsignal = new MaintTestsignalBuilder()
            // PRBS value should be PRBS31 if enabled is true
            .setTestPattern(TestPattern.PRBS31)
            .setEnabled(false);

        // Parent Odu-allocation
        // Set the trib-slot array
        List<OpucnTribSlotDef> tribslots = new ArrayList<>();
        IntStream.range(1, 5).forEach(a -> IntStream.range(1, 21).forEach(b -> tribslots.add(
            OpucnTribSlotDef.getDefaultInstance(a + "." + b))));

        ParentOduAllocationBuilder parentOduAllocationBuilder = new ParentOduAllocationBuilder()
            .setTribPortNumber(Uint16.valueOf(1))
            .setTribSlotsChoice(new OpucnBuilder().setOpucnTribSlots(tribslots).build());

        // Create an ODUFlex object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUflexCbr.class)
            .setOduflexCbrService(ODUflexCbr400G.class)
            .setOduFunction(ODUTTPCTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setTxSapi(portMapA.getLcpHashVal())
            .setTxDapi(portMapZ.getLcpHashVal())
            .setExpectedSapi(portMapZ.getLcpHashVal())
            .setExpectedDapi(portMapA.getLcpHashVal())
            .setOpu(opuBuilder.build())
            .setMaintTestsignal(maintTestsignal.build())
            .setParentOduAllocation(parentOduAllocationBuilder.build());

        InterfaceBuilder oduflexInterfaceBuilder = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + "-ODUFLEX");

        List<String> listSupportingOtucnInterface = new ArrayList<>();
        listSupportingOtucnInterface.add(supportingOducn);

        oduflexInterfaceBuilder.setSupportingInterfaceList(listSupportingOtucnInterface);


        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder
                oduflexIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev200529.Interface1Builder();

        oduflexInterfaceBuilder.addAugmentation(oduflexIf1Builder.setOdu(oduBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduflexInterfaceBuilder);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMapA.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(anodeId, portMapA.getSupportingCircuitPackName(), true);
        }

        return oduflexInterfaceBuilder.getName();
    }

    // This creates the name of the interface with slot numbers at the end
    public String createOpenRoadmOtsiInterfaceName(String logicalConnectionPoint, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,logicalConnectionPoint, spectralSlotName);
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, Class<? extends InterfaceType> type,
            String key) {
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

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = "UPM_UNCALLED_PRIVATE_METHOD",
        justification = "call in call() method")
    private int getServiceRate(ModulationFormat modulationFormat, SpectrumInformation spectrumInformation) {

        switch (modulationFormat) {
            case DpQpsk:
                LOG.info("Given modulation format is {} and thus rate is 200G", modulationFormat);
                return 200;
            case DpQam8:
                LOG.info("Given modulation format is {} and thus rate is 300G", modulationFormat);
                return 300;
            case DpQam16:
                // Based on roll-of-factor of 0.5, 87.5 - 12.5 = 75GHz translates to 63.1 GBaud
                LOG.info("Given modulation format is {} and thus rate is 400G", modulationFormat);
                return 400;
            default:
                LOG.error("Modulation format is required to select the rate");
                break;
        }
        return 0;
    }

}
