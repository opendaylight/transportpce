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
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.SpectrumInformation;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev211004.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210927.mapping.Mapping;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUTTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.OduFunctionIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.Opu;
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
    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterface221.class);


    public OpenRoadmInterface221(PortMapping portMapping, OpenRoadmInterfaces openRoadmInterfaces) {
        this.portMapping = portMapping;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }

    public String createOpenRoadmEthInterface(String nodeId, String logicalConnPoint)
        throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        // Ethernet interface specific data
        EthernetBuilder ethIfBuilder = new EthernetBuilder()
            .setFec(EthAttributes.Fec.Off)
            .setSpeed(Uint32.valueOf(100000));

        InterfaceBuilder ethInterfaceBldr = createGenericInterfaceBuilder(mapping, EthernetCsmacd.class,
            logicalConnPoint + "-ETHERNET");
        // Create Interface1 type object required for adding as augmentation
        Interface1Builder ethIf1Builder = new Interface1Builder();
        ethInterfaceBldr.addAugmentation(ethIf1Builder.setEthernet(ethIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, ethInterfaceBldr);

        // Post the equipment-state change on the device circuit-pack
        openRoadmInterfaces.postEquipmentState(nodeId, mapping.getSupportingCircuitPackName(), true);
        this.portMapping.updateMapping(nodeId, mapping);
        return ethInterfaceBldr.getName();
    }

    public List<String> createFlexOCH(String nodeId, String logicalConnPoint, SpectrumInformation spectrumInformation)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }

        List<String> interfacesCreated = new ArrayList<>();
        String mcInterfaceCreated = "";
        if (logicalConnPoint.contains("DEG")) {
            mcInterfaceCreated = createMCInterface(nodeId, logicalConnPoint, spectrumInformation);
            interfacesCreated.add(mcInterfaceCreated);
        }
        String nmcInterfaceCreated = createNMCInterface(nodeId, logicalConnPoint, spectrumInformation,
                mcInterfaceCreated);
        interfacesCreated.add(nmcInterfaceCreated);
        return interfacesCreated;
    }

    public String createMCInterface(String nodeId, String logicalConnPoint,
        SpectrumInformation spectrumInformation)
        throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        // TODO : Check this method
        LOG.info("MC interface Freq Start {} and Freq End {} and center-Freq {}",
                spectrumInformation.getMinFrequency(), spectrumInformation.getMaxFrequency(),
                spectrumInformation.getCenterFrequency());
        InterfaceBuilder mcInterfaceBldr = createGenericInterfaceBuilder(portMap,
            MediaChannelTrailTerminationPoint.class,
            spectrumInformation.getIdentifierFromParams(logicalConnPoint, "mc"))
                .setSupportingInterface(portMap.getSupportingOms());

        McTtpBuilder mcTtpBuilder = new McTtpBuilder()
            .setMinFreq(new FrequencyTHz(spectrumInformation.getMinFrequency()))
            .setMaxFreq(new FrequencyTHz(spectrumInformation.getMaxFrequency()));

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

    public String createNMCInterface(String nodeId, String logicalConnPoint,
        SpectrumInformation spectrumInformation, String mcName)
        throws OpenRoadmInterfaceException {
        LOG.info("This is the central frequency {}", spectrumInformation.getCenterFrequency());
        LOG.info("This is the nmc width {}", spectrumInformation.getWidth());
        // TODO : Check this method
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        //TODO : Check this method
        String nmcName = spectrumInformation.getIdentifierFromParams(logicalConnPoint, "nmc");
        InterfaceBuilder nmcInterfaceBldr = createGenericInterfaceBuilder(portMap,
            NetworkMediaChannelConnectionTerminationPoint.class, nmcName);
        if (logicalConnPoint.contains("DEG")) {
            nmcInterfaceBldr.setSupportingInterface(mcName);
        }

        NmcCtpBuilder nmcCtpIfBuilder = new NmcCtpBuilder()
                .setFrequency(new FrequencyTHz(spectrumInformation.getCenterFrequency()))
                .setWidth(new FrequencyGHz(spectrumInformation.getWidth()));

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

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
        SpectrumInformation spectrumInformation)
        throws OpenRoadmInterfaceException {
        // TODO : Check this method
        ModulationFormat modulationFormat = ModulationFormat.DpQpsk;
        Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
                .forName(spectrumInformation.getModulationFormat());
        if (optionalModulationFormat.isPresent()) {
            modulationFormat =  optionalModulationFormat.get();
        }
        // OCH interface specific data
        OchBuilder ocIfBuilder = new OchBuilder()
            .setFrequency(new FrequencyTHz(spectrumInformation.getCenterFrequency()))
            .setRate(R100G.class)
            .setTransmitPower(new PowerDBm(new BigDecimal("-5")))
            .setModulationFormat(modulationFormat);
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                String.format("Unable to get mapping from PortMapping for node %s and logical connection port %s",
                    nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr = createGenericInterfaceBuilder(portMap, OpticalChannel.class,
            spectrumInformation.getIdentifierFromParams(logicalConnPoint));
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

    public String createOpenRoadmOdu4HOInterface(String nodeId, String logicalConnPoint, boolean isCTP,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                    String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr = createGenericInterfaceBuilder(mapping, OtnOdu.class,
            logicalConnPoint + "-ODU4");
        if (mapping.getSupportingOtu4() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingOtu4());
        }
        if (mapping.getSupportingEthernet() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingEthernet());
        }
        // ODU interface specific data
        // Set Opu attributes
        Class<? extends OduFunctionIdentity> oduFunction;
        MonitoringMode monitoringMode;
        Opu opu = null;
        if (isCTP) {
            oduFunction = ODUCTP.class;
            monitoringMode = MonitoringMode.Monitored;
        } else {
            oduFunction = ODUTTP.class;
            monitoringMode = MonitoringMode.Terminated;
            opu = new OpuBuilder()
                .setPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                .setExpPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                .build();
        }
        OduBuilder oduIfBuilder = new OduBuilder()
                .setRate(ODU4.class)
                .setOduFunction(oduFunction)
                .setMonitoringMode(monitoringMode)
                .setOpu(opu);
        if (apiInfoA != null) {
            oduIfBuilder.setTxSapi(apiInfoA.getSapi())
                .setTxDapi(apiInfoA.getDapi())
                .setExpectedSapi(apiInfoA.getExpectedSapi())
                .setExpectedDapi(apiInfoA.getExpectedDapi());
        }
        if (apiInfoZ != null) {
            oduIfBuilder.setTxSapi(apiInfoZ.getSapi())
                .setTxDapi(apiInfoZ.getDapi())
                .setExpectedSapi(apiInfoZ.getExpectedSapi())
                .setExpectedDapi(apiInfoZ.getExpectedDapi());
        }
        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder oduIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder();
        oduInterfaceBldr.addAugmentation(oduIf1Builder.setOdu(oduIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        if (!isCTP) {
            LOG.info("{}-{} updating mapping with interface {}", nodeId, logicalConnPoint, oduInterfaceBldr.getName());
            this.portMapping.updateMapping(nodeId, mapping);
        }
        return oduInterfaceBldr.getName();
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

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) throws OpenRoadmInterfaceException {

        Mapping mapping = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                String.format(MAPPING_ERROR_EXCEPTION_MESSAGE, nodeId, logicalConnPoint));
        }
        InterfaceBuilder otuInterfaceBldr = createGenericInterfaceBuilder(mapping, OtnOtu.class,
            logicalConnPoint + "-OTU");
        otuInterfaceBldr.setSupportingInterface(supportOchInterface);

        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder()
            .setFec(OtuAttributes.Fec.Scfec)
            .setRate(OTU4.class);
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

        // Create Interface1 type object required for adding as augmentation
        // TODO look at imports of different versions of class
        org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder otuIf1Builder =
            new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder();
        otuInterfaceBldr.addAugmentation(otuIf1Builder.setOtu(otuIfBuilder.build()).build());

        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otuInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return otuInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,logicalConnectionPoint, spectralSlotName);
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
        this.portMapping.updateMapping(anodeId, portMapA);
        return oduInterfaceBldr.getName();
    }
}
