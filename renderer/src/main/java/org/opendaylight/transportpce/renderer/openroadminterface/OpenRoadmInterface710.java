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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210426.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.TrailTraceOther.TimDetectMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.ParentOduAllocationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.attributes.rev200327.parent.odu.allocation.parent.odu.allocation.trib.slots.choice.OpucnBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.Foic48;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev200529.ProvisionModeType;
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


public class OpenRoadmInterface710 {
    private static final String MAPPING_ERROR_EXCEPTION_MESSAGE =
        "Unable to get mapping from PortMapping for node % and logical connection port %s";
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;


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
        // TODO : Check this method
        ModulationFormat modulationFormat = ModulationFormat.DpQam16;
        Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
            .forName(spectrumInformation.getModulationFormat());
        if (optionalModulationFormat.isPresent()) {
            modulationFormat =  optionalModulationFormat.get();
        }
        // Set the Flexo values
        FlexoBuilder flexoBuilder = new FlexoBuilder()
            .setFoicType(Foic48.class)
            .setIid(new ArrayList<>(Arrays.asList(Uint8.valueOf(1), Uint8.valueOf(2),
                Uint8.valueOf(3), Uint8.valueOf(4))));

        // OTSI interface specific data
        OtsiBuilder  otsiBuilder = new OtsiBuilder()
            .setFrequency(new FrequencyTHz(spectrumInformation.getCenterFrequency()))
            .setTransmitPower(new PowerDBm(new BigDecimal("-5")))
            .setModulationFormat(modulationFormat)
            .setOtsiRate(R400GOtsi.class)
            .setProvisionMode(ProvisionModeType.Explicit)
            .setFec(Ofec.class)
            .setFlexo(flexoBuilder.build());
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
            String supportingOtsiInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // Create an OTSI group object
        OtsiGroupBuilder otsiGroupBuilder = new OtsiGroupBuilder()
            .setGroupId(Uint32.valueOf(1))
            .setGroupRate(R400GOtsi.class);

        // Create generic interface
        InterfaceBuilder otsiGroupInterfaceBldr = createGenericInterfaceBuilder(portMap, OtsiGroup.class,
            logicalConnPoint + "-OTSI-GROUP");

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
            String supportingOtsiGroupInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        // Create an OTUCn object
        MaintLoopbackBuilder maintLoopbackBuilder = new MaintLoopbackBuilder();
        maintLoopbackBuilder.setEnabled(false);
        OtuBuilder otuBuilder = new OtuBuilder()
            .setRate(OTUCn.class)
            .setOtucnNRate(Uint16.valueOf(4))
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setMaintLoopback(maintLoopbackBuilder.build());

        InterfaceBuilder otuInterfaceBuilder = createGenericInterfaceBuilder(portMap, OtnOtu.class,
            logicalConnPoint + "-OTUC4");

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
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }

        return otuInterfaceBuilder.getName();

    }

    // Adding method to have SAPI/DAPI information for the OTUCn
    public String createOpenRoadmOtucnInterface(String anodeId, String alogicalConnPoint,
            String supportingOtsiGroupInterface, String znodeId, String zlogicalConnPoint)
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
        // Create an OTUCn object
        MaintLoopbackBuilder maintLoopbackBuilder = new MaintLoopbackBuilder();
        maintLoopbackBuilder.setEnabled(false);
        OtuBuilder otuBuilder = new OtuBuilder()
            .setRate(OTUCn.class)
            .setOtucnNRate(Uint16.valueOf(4))
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setMaintLoopback(maintLoopbackBuilder.build())
            .setTxSapi(portMapA.getLcpHashVal())
            .setTxDapi(portMapZ.getLcpHashVal())
            // setting expected SAPI and DAPI values
            .setExpectedDapi(portMapA.getLcpHashVal())
            .setExpectedSapi(portMapZ.getLcpHashVal());

        InterfaceBuilder otuInterfaceBuilder = createGenericInterfaceBuilder(portMapA, OtnOtu.class,
            alogicalConnPoint + "-OTUC4");

        // Create a list
        List<String> listSupportingOtsiGroupInterface = new ArrayList<>();
        listSupportingOtsiGroupInterface.add(supportingOtsiGroupInterface);

        otuInterfaceBuilder.setSupportingInterfaceList(listSupportingOtsiGroupInterface);
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.Interface1Builder otuIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev200529.Interface1Builder();

        otuInterfaceBuilder.addAugmentation(otuIf1Builder.setOtu(otuBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, otuInterfaceBuilder);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMapA.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(anodeId, portMapA.getSupportingCircuitPackName(), true);
        }

        return otuInterfaceBuilder.getName();

    }

    public String createOpenRoadmOducnInterface(String nodeId, String logicalConnPoint,
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
        // Maint test signal
        MaintTestsignalBuilder maintTestsignal = new MaintTestsignalBuilder()
            // PRBS value should be PRBS31 if enabled is true
            .setTestPattern(TestPattern.PRBS31)
            .setEnabled(false);

        // Create an ODUC4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOducnNRate(Uint16.valueOf(4))
            .setOduFunction(ODUTTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOpu(opuBuilder.build())
            .setMaintTestsignal(maintTestsignal.build());

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMap, OtnOdu.class,
            logicalConnPoint + "-ODUC4");

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
        // Maint test signal
        MaintTestsignalBuilder maintTestsignal = new MaintTestsignalBuilder()
            // PRBS value should be PRBS31 if enabled is true
            .setTestPattern(TestPattern.PRBS31)
            .setEnabled(false);

        // Create an ODUC4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOducnNRate(Uint16.valueOf(4))
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
            .setExpectedDapi(portMapZ.getLcpHashVal())
            .setMaintTestsignal(maintTestsignal.build());

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + "-ODUC4");

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
        // Maint test signal
        MaintTestsignalBuilder maintTestsignal = new MaintTestsignalBuilder()
            // PRBS value should be PRBS31 if enabled is true
            .setTestPattern(TestPattern.PRBS31)
            .setEnabled(false);

        // Create an ODUC4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOducnNRate(Uint16.valueOf(4))
            .setOduFunction(ODUTTP.class)
            .setMonitoringMode(MonitoringMode.Terminated)
            .setTimActEnabled(false)
            .setTimDetectMode(TimDetectMode.Disabled)
            .setDegmIntervals(Uint8.valueOf(2))
            .setDegthrPercentage(Uint16.valueOf(100))
            .setOpu(opuBuilder.build())
            .setMaintTestsignal(maintTestsignal.build());

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMap, OtnOdu.class,
            logicalConnPoint + "-ODUC4");

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
        // Maint test signal
        MaintTestsignalBuilder maintTestsignal = new MaintTestsignalBuilder()
            // PRBS value should be PRBS31 if enabled is true
            .setTestPattern(TestPattern.PRBS31)
            .setEnabled(false);

        // Create an ODUC4 object
        OduBuilder oduBuilder = new OduBuilder()
            .setRate(ODUCn.class)
            .setOducnNRate(Uint16.valueOf(4))
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
            .setExpectedDapi(portMapZ.getLcpHashVal())
            .setMaintTestsignal(maintTestsignal.build());

        InterfaceBuilder oduInterfaceBuilder = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + "-ODUC4");

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
            .setOpu(opuBuilder.build())
            .setMaintTestsignal(maintTestsignal.build())
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

}
