/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.FixedFlexInterface;
import org.opendaylight.transportpce.common.fixedflex.FlexGridInterface;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev200827.network.nodes.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.R100G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev171215.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.EthAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.ethernet.interfaces.rev181019.ethernet.container.EthernetBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.MediaChannelTrailTerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.NetworkMediaChannelConnectionTerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev170626.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.media.channel.interfaces.rev181019.mc.ttp.container.McTtpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.media.channel.interfaces.rev181019.nmc.ctp.container.NmcCtpBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev181019.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.OtsAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUTTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.OtuAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.otu.container.OtuBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterface221 {
    private static final String MAPPING_ERROR_EXCEPTION_MESSAGE =
            "Unable to get mapping from PortMapping for node % and logical connection port %s";
    private final PortMapping portMapping;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private FixedFlexInterface fixedFlex;
    private FlexGridInterface flexGrid;
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterface221.class);


    public OpenRoadmInterface221(PortMapping portMapping, OpenRoadmInterfaces openRoadmInterfaces,
        FixedFlexInterface fixedFlex, FlexGridInterface flexGrid) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.fixedFlex = fixedFlex;
        this.flexGrid = flexGrid;
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
            .setFec(EthAttributes.Fec.Off)
            .setSpeed(Uint32.valueOf(100000));

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

    public List<String> createFlexOCH(String nodeId, String logicalConnPoint, Long bitmapIndex,
        BigDecimal centerFreq, BigDecimal slotWidth)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        List<String> interfacesCreated = new ArrayList<>();

        if (logicalConnPoint.contains("DEG")) {
            String mcInterfaceCreated = createMCInterface(nodeId, logicalConnPoint, centerFreq, slotWidth, bitmapIndex);
            interfacesCreated.add(mcInterfaceCreated);
        }
        String mcInterfaceCreated = createNMCInterface(nodeId, logicalConnPoint, centerFreq, slotWidth, bitmapIndex);
        interfacesCreated.add(mcInterfaceCreated);
        return interfacesCreated;
    }

    /**
     * This methods creates an OCH interface on the given termination point on
     * Roadm.
     *
     * @param nodeId node ID
     * @param logicalConnPoint logical connection point
     * @param waveNumber wavelength number of the OCH interface.
     *
     * @return Name of the interface if successful, otherwise return null.
     *
     * @throws OpenRoadmInterfaceException OpenRoadm interface exception
     */

    public List<String> createFlexOCH(String nodeId, String logicalConnPoint, Long waveNumber)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        List<String> interfacesCreated = new ArrayList<>();

        if (logicalConnPoint.contains("DEG")) {
            String mcInterfaceCreated = createMCInterface(nodeId, logicalConnPoint, waveNumber);
            interfacesCreated.add(mcInterfaceCreated);
        }
        String nmcInterfaceCreated = createNMCInterface(nodeId, logicalConnPoint, waveNumber);
        interfacesCreated.add(nmcInterfaceCreated);
        return interfacesCreated;
    }


    public String createMCInterface(String nodeId, String logicalConnPoint, Long waveNumber)
        throws OpenRoadmInterfaceException {

        // TODO : Check this method

        fixedFlex = fixedFlex.getFixedFlexWaveMapping(waveNumber.shortValue());

        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        // TODO : Check this method

        InterfaceBuilder mcInterfaceBldr = createGenericInterfaceBuilder(portMap,
                MediaChannelTrailTerminationPoint.class, logicalConnPoint + "-mc" + "-" + waveNumber)
            .setSupportingInterface(portMap.getSupportingOms());

        McTtpBuilder mcTtpBuilder = new McTtpBuilder()
            .setMinFreq(FrequencyTHz.getDefaultInstance(String.valueOf(fixedFlex.getStart())))
            .setMaxFreq(FrequencyTHz.getDefaultInstance(String.valueOf(fixedFlex.getStop())));

        // Create Interface1 type object required for adding as augmentation
        org.opendaylight.yang.gen.v1.http.org.openroadm.media.channel.interfaces.rev181019.Interface1Builder
            interface1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.media.channel.interfaces.rev181019.Interface1Builder()
                .setMcTtp(mcTtpBuilder.build());

        mcInterfaceBldr.addAugmentation(interface1Builder.build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, mcInterfaceBldr);
        return mcInterfaceBldr.getName();
    }

    public String createMCInterface(String nodeId, String logicalConnPoint,
        BigDecimal centerFrequency, BigDecimal slotWidth, Long bitmapIndex)
        throws OpenRoadmInterfaceException {

        // TODO : Check this method

        flexGrid = flexGrid.getFlexWaveMapping(centerFrequency.floatValue(), slotWidth.floatValue());

        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        // TODO : Check this method
        LOG.info("MC interface Freq Start {} and Freq End {} and center-Freq {}",
            flexGrid.getStart(), flexGrid.getStop(), centerFrequency);
        InterfaceBuilder mcInterfaceBldr = createGenericInterfaceBuilder(portMap,
            MediaChannelTrailTerminationPoint.class, logicalConnPoint + "-mc" + "-" + bitmapIndex)
                .setSupportingInterface(portMap.getSupportingOms());

        McTtpBuilder mcTtpBuilder = new McTtpBuilder()
            .setMinFreq(FrequencyTHz.getDefaultInstance(String.valueOf(flexGrid.getStart())))
            .setMaxFreq(FrequencyTHz.getDefaultInstance(String.valueOf(flexGrid.getStop())));

        // Create Interface1 type object required for adding as augmentation
        org.opendaylight.yang.gen.v1.http.org.openroadm.media.channel.interfaces.rev181019.Interface1Builder
            interface1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.media.channel.interfaces.rev181019.Interface1Builder()
                .setMcTtp(mcTtpBuilder.build());

        mcInterfaceBldr.addAugmentation(interface1Builder.build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, mcInterfaceBldr);
        return mcInterfaceBldr.getName();
    }

    public String createNMCInterface(String nodeId, String logicalConnPoint, Long waveNumber)
        throws OpenRoadmInterfaceException {

        // TODO : Check this method
        fixedFlex = fixedFlex.getFixedFlexWaveMapping(waveNumber.shortValue());

        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        LOG.info("Freq Start {} and Freq End {} and center-Freq {}",
            fixedFlex.getStart(), fixedFlex.getStop(), fixedFlex.getCenterFrequency());
        //TODO : Check this method
        InterfaceBuilder nmcInterfaceBldr = createGenericInterfaceBuilder(portMap,
            NetworkMediaChannelConnectionTerminationPoint.class, logicalConnPoint + "-nmc" + "-" + waveNumber);
        if (logicalConnPoint.contains("DEG")) {
            nmcInterfaceBldr.setSupportingInterface(logicalConnPoint + "-mc" + "-" + waveNumber);
        }

        NmcCtpBuilder nmcCtpIfBuilder = new NmcCtpBuilder()
            .setFrequency(FrequencyTHz.getDefaultInstance(String.valueOf(fixedFlex.getCenterFrequency())))
            .setWidth(FrequencyGHz.getDefaultInstance("40"));

        // Create Interface1 type object required for adding as augmentation
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.media.channel.interfaces.rev181019.Interface1Builder
            nmcCtpI1fBuilder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.network.media.channel.interfaces.rev181019
                    .Interface1Builder().setNmcCtp(nmcCtpIfBuilder.build());
        nmcInterfaceBldr.addAugmentation(nmcCtpI1fBuilder.build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, nmcInterfaceBldr);
        return nmcInterfaceBldr.getName();
    }

    public String createNMCInterface(String nodeId, String logicalConnPoint,
        BigDecimal centerFrequency, BigDecimal slotWidth, Long bitmapIndex)
        throws OpenRoadmInterfaceException {
        LOG.info("This is the central frequency {}", centerFrequency.floatValue());
        LOG.info("This is the slot-width {}", slotWidth.floatValue());
        // TODO : Check this method
        flexGrid = flexGrid.getFlexWaveMapping(centerFrequency.floatValue(), slotWidth.floatValue());
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        LOG.info("Freq Start {} and Freq End {} and center-Freq {}", flexGrid.getStart(),
            flexGrid.getStop(),centerFrequency);
        //TODO : Check this method
        InterfaceBuilder nmcInterfaceBldr = createGenericInterfaceBuilder(portMap,
            NetworkMediaChannelConnectionTerminationPoint.class, logicalConnPoint + "-nmc" + "-" + bitmapIndex);
        if (logicalConnPoint.contains("DEG")) {
            nmcInterfaceBldr.setSupportingInterface(logicalConnPoint + "-mc" + "-" + bitmapIndex);
        }
        NmcCtpBuilder nmcCtpIfBuilder;

        // From the logicalConnPoint, get the substring and for that associated substring find the mc-cap
        String mcLcp = "";
        if (logicalConnPoint.contains("-PP")) {
            mcLcp = logicalConnPoint.substring(0, logicalConnPoint.indexOf("-")) + "-PP";
        }
        else if (logicalConnPoint.contains("-TTP")) {
            mcLcp = logicalConnPoint.substring(0, logicalConnPoint.indexOf("-")) + "-TTP";
        }
        McCapabilities mcCapabilities = portMapping.getMcCapbilities(nodeId, mcLcp);
        if (mcCapabilities == null || mcCapabilities.getSlotWidthGranularity() == null
                || mcCapabilities.getSlotWidthGranularity().getValue() == null) {
            LOG.warn("Port mapping could not find {}", mcLcp);
            LOG.warn("Check the port mapping to verify {}", mcLcp);
            return nmcInterfaceBldr.getName();
        }
        double slotWidthGran = mcCapabilities.getSlotWidthGranularity().getValue().doubleValue();
        // Dead-band is constant (Ref: WP) In GHz
        double deadBand = 8;
        double guardBand = Math.ceil(deadBand / slotWidthGran) * slotWidthGran;
        nmcCtpIfBuilder = new NmcCtpBuilder()
                .setFrequency(FrequencyTHz.getDefaultInstance(String.valueOf(centerFrequency)))
                .setWidth(FrequencyGHz.getDefaultInstance(String.valueOf(slotWidth.doubleValue() - guardBand)));

        // Create Interface1 type object required for adding as augmentation
        org.opendaylight.yang.gen.v1.http.org.openroadm.network.media.channel.interfaces.rev181019.Interface1Builder
            nmcCtpI1fBuilder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.network.media.channel.interfaces.rev181019
                 .Interface1Builder().setNmcCtp(nmcCtpIfBuilder.build());
        nmcInterfaceBldr.addAugmentation(nmcCtpI1fBuilder.build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, nmcInterfaceBldr);
        return nmcInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint, Long waveNumber)
        throws OpenRoadmInterfaceException {
        // TODO : Check this method

        fixedFlex = fixedFlex.getFixedFlexWaveMapping(waveNumber.shortValue());

        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder()
                .setFrequency(FrequencyTHz.getDefaultInstance(String.valueOf(fixedFlex.getCenterFrequency())))
                .setRate(R100G.class)
                // TODO: Check if this the correct way to set modulation format here
                .setModulationFormat(ModulationFormat.DpQpsk)
                .setTransmitPower(new PowerDBm(new BigDecimal("-5")));

        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
            createOpenRoadmOchInterfaceName(logicalConnPoint, waveNumber));
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev181019.Interface1Builder
            ochIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev181019
            .Interface1Builder();
        ochInterfaceBldr.addAugmentation(ochIf1Builder.setOch(ocIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }

        return ochInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
        Long waveNumber, BigDecimal centerFrequency, BigDecimal slotWidth)
        throws OpenRoadmInterfaceException {
        // TODO : Check this method

        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder()
            .setFrequency(FrequencyTHz.getDefaultInstance(String.valueOf(centerFrequency)))
            .setRate(R100G.class)
            .setTransmitPower(new PowerDBm(new BigDecimal("-5")))
            .setModulationFormat(ModulationFormat.DpQpsk);

        // In Flex-grid, slot-width could be other than 50 GHz
        if ((centerFrequency != null) && (slotWidth != null)) {
            ocIfBuilder.setFrequency(FrequencyTHz.getDefaultInstance(String.valueOf(centerFrequency)));
            ocIfBuilder.setWidth(FrequencyGHz.getDefaultInstance(String.valueOf(slotWidth)));
        }
        else {
            // Use fixed grid to find the
            fixedFlex = fixedFlex.getFixedFlexWaveMapping(waveNumber.shortValue());
            ocIfBuilder.setFrequency(FrequencyTHz.getDefaultInstance(String.valueOf(fixedFlex.getCenterFrequency())));
        }
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format("Unable to get mapping from PortMapping for node %s and logical connection port %s",
                    nodeId, logicalConnPoint));
        }



        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
            createOpenRoadmOchInterfaceName(logicalConnPoint, waveNumber));
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev181019.Interface1Builder
            ochIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev181019
            .Interface1Builder();
        ochInterfaceBldr.addAugmentation(ochIf1Builder.setOch(ocIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ochInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack if xpdr node
        if (portMap.getLogicalConnectionPoint().contains(StringConstants.NETWORK_TOKEN)) {
            this.openRoadmInterfaces.postEquipmentState(nodeId, portMap.getSupportingCircuitPackName(), true);
        }

        return ochInterfaceBldr.getName();
    }

    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE,
                    nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOdu.class,
            logicalConnPoint + "-ODU");
        oduInterfaceBldr.setSupportingInterface(supportingOtuInterface);

        // ODU interface specific data
        // Set Opu attributes
        OpuBuilder opuBldr = new OpuBuilder()
                .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"));
        OduBuilder oduIfBuilder = new OduBuilder()
                .setRate(ODU4.class)
                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                .setOpu(opuBldr.build());

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOdu4Interface(String anodeId, String alogicalConnPoint, String supportingOtuInterface,
        String znodeId, String zlogicalConnPoint)
        throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, anodeId, alogicalConnPoint));
        }
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, znodeId, zlogicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + "-ODU");
        oduInterfaceBldr.setSupportingInterface(supportingOtuInterface);

        // ODU interface specific data
        // Set Opu attributes
        OpuBuilder opuBldr = new OpuBuilder()
            .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"));
        OduBuilder oduIfBuilder = new OduBuilder()
            .setRate(ODU4.class)
            .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
            .setOpu(opuBldr.build())
            .setTxSapi(portMapA.getLcpHashVal())
            .setTxDapi(portMapZ.getLcpHashVal())
            // Setting the expected Dapi and Sapi values
            .setExpectedDapi(portMapA.getLcpHashVal())
            .setExpectedSapi(portMapZ.getLcpHashVal());

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBldr);
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format("Unable to get mapping from PortMapping for node %s and logical connection port %s",
                    nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder otuInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOtu.class,
            logicalConnPoint + "-OTU");
        otuInterfaceBldr.setSupportingInterface(supportOchInterface);

        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder();
        otuIfBuilder.setFec(OtuAttributes.Fec.Scfec);
        otuIfBuilder.setRate(OTU4.class);

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder otuIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder();
        otuInterfaceBldr.addAugmentation(otuIf1Builder.setOtu(otuIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otuInterfaceBldr);
        return otuInterfaceBldr.getName();
    }

    public String createOpenRoadmOtu4Interface(String anodeId, String alogicalConnPoint, String asupportOchInterface,
                                               String znodeId, String zlogicalConnPoint)
        throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                    String.format("Unable to get mapping from PortMapping for node %s and logical connection port %s ",
                            anodeId, alogicalConnPoint));
        }
        // On the Zside
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                    String.format("Unable to get mapping from PortMapping for node %s and logical connection port %s ",
                            znodeId, zlogicalConnPoint));

        }
        // Create generic interface builder
        InterfaceBuilder otuInterfaceBldr = createGenericInterfaceBuilder(portMapA, OtnOtu.class,
                alogicalConnPoint + "-OTU");

        // Set the supporting interface data
        otuInterfaceBldr.setSupportingInterface(asupportOchInterface);

        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder()
                .setFec(OtuAttributes.Fec.Scfec)
                .setRate(OTU4.class)
                .setTxSapi(portMapA.getLcpHashVal())
                .setTxDapi(portMapZ.getLcpHashVal())
                // setting expected SAPI and DAPI values
                .setExpectedDapi(portMapA.getLcpHashVal())
                .setExpectedSapi(portMapZ.getLcpHashVal());

        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder otuIf1Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder();

        otuInterfaceBldr.addAugmentation(otuIf1Builder.setOtu(otuIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, otuInterfaceBldr);
        return otuInterfaceBldr.getName();

    }

    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, Long waveNumber) {
        return logicalConnectionPoint + "-" + waveNumber;
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, Class<? extends InterfaceType> type,
        String key) {
        return new InterfaceBuilder()
                .setDescription("  TBD   ")
                .setCircuitId("   TBD    ")
                .setSupportingCircuitPackName(portMap.getSupportingCircuitPackName())
                .setSupportingPort(portMap.getSupportingPort())
                .setAdministrativeState(AdminStates.InService)
                .setType(type)
                .setName(key)
                .withKey(new InterfaceKey(key));
    }

    public String createOpenRoadmOmsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        if (mapping.getSupportingOms() == null) {
            // Create generic interface
            InterfaceBuilder omsInterfaceBldr = createGenericInterfaceBuilder(mapping, OpenROADMOpticalMultiplex.class,
                "OMS-" + mapping.getLogicalConnectionPoint());
            if (mapping.getSupportingOts() != null) {
                omsInterfaceBldr.setSupportingInterface(mapping.getSupportingOts());
            } else {
                LOG.error("Unable to get ots interface from mapping {} - {}", nodeId,
                    mapping.getLogicalConnectionPoint());
                return null;
            }
            this.openRoadmInterfaces.postInterface(nodeId, omsInterfaceBldr);
            this.portMapping.updateMapping(nodeId, mapping);
            return omsInterfaceBldr.build().getName();
        } else {
            return mapping.getSupportingOms();
        }
    }

    public String createOpenRoadmOtsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        if (mapping.getSupportingOts() == null) {
            // Create generic interface
            InterfaceBuilder otsInterfaceBldr = createGenericInterfaceBuilder(mapping, OpticalTransport.class, "OTS-"
                + mapping.getLogicalConnectionPoint());
            // OTS interface augmentation specific data
            OtsBuilder otsIfBuilder = new OtsBuilder();
            otsIfBuilder.setFiberType(OtsAttributes.FiberType.Smf);

            // Create Interface1 type object required for adding as
            // augmentation
            org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.Interface1Builder
                otsIf1Builder = new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces
                .rev181019.Interface1Builder();
            otsInterfaceBldr.addAugmentation(otsIf1Builder.setOts(otsIfBuilder.build()).build());
            this.openRoadmInterfaces.postInterface(nodeId, otsInterfaceBldr);
            this.portMapping.updateMapping(nodeId, mapping);
            return otsInterfaceBldr.build().getName();
        } else {
            return mapping.getSupportingOts();
        }
    }

    public boolean isUsedByXc(String nodeId, String interfaceName, String xc,
        DeviceTransactionManager deviceTransactionManager) {
        InstanceIdentifier<RoadmConnections> xciid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(xc));
        LOG.info("reading xc {} in node {}", xc, nodeId);
        Optional<RoadmConnections> crossconnection = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, xciid, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (crossconnection.isPresent()) {
            RoadmConnections rc = crossconnection.get();
            LOG.info("xd {} found", xc);
            String supportedinter = null;
            if (!interfaceName.contains("nmc")) {
                supportedinter = interfaceName.replace("mc", "nmc");
            }
            if (rc.getSource().getSrcIf().equals(interfaceName)
                || rc.getDestination().getDstIf().equals(interfaceName)
                || rc.getSource().getSrcIf().equals(supportedinter)
                || rc.getDestination().getDstIf().equals(supportedinter)) {
                return true;
            }
        } else {
            LOG.info("xd {} not found !", xc);
        }
        return false;
    }

    public boolean isUsedByOtnXc(String nodeId, String interfaceName, String xc,
        DeviceTransactionManager deviceTransactionManager) {
        InstanceIdentifier<OduConnection> xciid = InstanceIdentifier.create(OrgOpenroadmDevice.class)
            .child(OduConnection.class, new OduConnectionKey(xc));
        LOG.info("reading xc {} in node {}", xc, nodeId);
        Optional<OduConnection> oduConnectionOpt = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, xciid, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (oduConnectionOpt.isPresent()) {
            OduConnection oduXc = oduConnectionOpt.get();
            LOG.info("xc {} found", xc);
            if (oduXc.getSource().getSrcIf().equals(interfaceName)
                || oduXc.getDestination().getDstIf().equals(interfaceName)) {
                return true;
            }
        } else {
            LOG.info("xc {} not found !", xc);
        }
        return false;
    }

    public String createOpenRoadmOtnOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                    String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(portMap, OtnOdu.class,
                logicalConnPoint + "-ODU4");
        oduInterfaceBldr.setSupportingInterface(supportingOtuInterface);

        // ODU interface specific data
        OduBuilder oduIfBuilder = new OduBuilder()
                .setRate(ODU4.class)
                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated);

        // Set Opu attributes
        OpuBuilder opuBldr = new OpuBuilder()
                .setPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                .setExpPayloadType(PayloadTypeDef.getDefaultInstance("21"));
        oduIfBuilder.setOduFunction(ODUTTP.class)
                .setOpu(opuBldr.build());

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder oduIf1Builder =
                new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOG.error("Error waiting post interface on device", e);
        }
        this.portMapping.updateMapping(nodeId, portMap);
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOtnOdu4Interface(String anodeId, String alogicalConnPoint,
        String asupportingOtuInterface, String znodeId, String zlogicalConnPoint)
        throws OpenRoadmInterfaceException {
        Mapping portMapA = portMapping.getMapping(anodeId, alogicalConnPoint);
        if (portMapA == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, anodeId, alogicalConnPoint));
        }
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, znodeId, zlogicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(portMapA, OtnOdu.class,
            alogicalConnPoint + "-ODU4");
        oduInterfaceBldr.setSupportingInterface(asupportingOtuInterface);

        // ODU interface specific data
        OduBuilder oduIfBuilder = new OduBuilder()
            .setRate(ODU4.class)
            .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
            .setTxSapi(portMapA.getLcpHashVal())
            .setTxDapi(portMapZ.getLcpHashVal())
            .setExpectedSapi(portMapZ.getLcpHashVal())
            .setExpectedDapi(portMapA.getLcpHashVal());


        // Set Opu attributes
        OpuBuilder opuBldr = new OpuBuilder()
            .setPayloadType(PayloadTypeDef.getDefaultInstance("21"))
            .setExpPayloadType(PayloadTypeDef.getDefaultInstance("21"));
        oduIfBuilder.setOduFunction(ODUTTP.class)
            .setOpu(opuBldr.build());

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBldr);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            LOG.error("Error waiting post interface on device", e);
        }
        this.portMapping.updateMapping(anodeId, portMapA);
        return oduInterfaceBldr.getName();
    }
}
