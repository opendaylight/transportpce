/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.openroadminterface;

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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.R100G;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.XpdrNodeTypes;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.ODUTTPCTP;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.OTU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.OduFunctionIdentity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev171215.PayloadTypeDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.OduAttributes.MonitoringMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.Opu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.opu.OpuBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.OtuAttributes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.otu.container.OtuBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class OpenRoadmInterface221 {
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
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Post interface on the device
        InterfaceBuilder ethInterfaceBldr =
            createGenericInterfaceBuilder(mapping, EthernetCsmacd.VALUE, logicalConnPoint + "-ETHERNET")
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new Interface1Builder()
                        .setEthernet(
                            // Ethernet interface specific data
                            new EthernetBuilder()
                                .setFec(EthAttributes.Fec.Off)
                                .setSpeed(Uint32.valueOf(100000))
                                .build())
                        .build());
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
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        List<String> interfacesCreated = new ArrayList<>();
        String mcInterfaceCreated = "";
        if (logicalConnPoint.contains("DEG")) {
            mcInterfaceCreated = createMCInterface(nodeId, logicalConnPoint, spectrumInformation);
            interfacesCreated.add(mcInterfaceCreated);
        }
        interfacesCreated.add(createNMCInterface(nodeId, logicalConnPoint, spectrumInformation, mcInterfaceCreated));
        return interfacesCreated;
    }

    public String createMCInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        LOG.info("MC interface Freq Start {} and Freq End {} and center-Freq {}",
                spectrumInformation.getMinFrequency(), spectrumInformation.getMaxFrequency(),
                spectrumInformation.getCenterFrequency());
        InterfaceBuilder mcInterfaceBldr =
            createGenericInterfaceBuilder(
                    portMap,
                    MediaChannelTrailTerminationPoint.VALUE,
                    spectrumInformation.getIdentifierFromParams(logicalConnPoint, "mc"))
                .setSupportingInterface(portMap.getSupportingOms())
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.media.channel.interfaces.rev181019
                            .Interface1Builder()
                        .setMcTtp(
                            new McTtpBuilder()
                                .setMinFreq(new FrequencyTHz(Decimal64.valueOf(spectrumInformation.getMinFrequency())))
                                .setMaxFreq(new FrequencyTHz(Decimal64.valueOf(spectrumInformation.getMaxFrequency())))
                                .build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, mcInterfaceBldr);
        return mcInterfaceBldr.getName();
    }

    public String createNMCInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation, String mcName)
            throws OpenRoadmInterfaceException {
        LOG.info("The central frequency is {} and the nmc width is {}",
            spectrumInformation.getCenterFrequency(), spectrumInformation.getWidth());
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder nmcInterfaceBldr =
            createGenericInterfaceBuilder(
                portMap, NetworkMediaChannelConnectionTerminationPoint.VALUE,
                spectrumInformation.getIdentifierFromParams(logicalConnPoint, "nmc"));
        if (logicalConnPoint.contains("DEG")) {
            nmcInterfaceBldr.setSupportingInterface(mcName);
        }
        // Create Interface1 type object required for adding as augmentation
        nmcInterfaceBldr.addAugmentation(
            new org.opendaylight.yang.gen.v1.http.org.openroadm.network.media.channel.interfaces.rev181019
                     .Interface1Builder()
                .setNmcCtp(
                    new NmcCtpBuilder()
                        .setFrequency(new FrequencyTHz(Decimal64.valueOf(spectrumInformation.getCenterFrequency())))
                        .setWidth(new FrequencyGHz(Decimal64.valueOf(spectrumInformation.getWidth())))
                        .build())
                .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, nmcInterfaceBldr);
        return nmcInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterface(String nodeId, String logicalConnPoint,
            SpectrumInformation spectrumInformation)
            throws OpenRoadmInterfaceException {
        ModulationFormat modulationFormat = ModulationFormat.forName(spectrumInformation.getModulationFormat());
        if (modulationFormat == null) {
            modulationFormat = ModulationFormat.DpQpsk;
        }
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // Create generic interface
        InterfaceBuilder ochInterfaceBldr =
            createGenericInterfaceBuilder(
                    portMap,
                    OpticalChannel.VALUE,
                    spectrumInformation.getIdentifierFromParams(logicalConnPoint))
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev181019
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

    public String createOpenRoadmOdu4HOInterface(String nodeId, String logicalConnPoint, boolean isCTP,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ, String payloadType) throws OpenRoadmInterfaceException {
        Mapping mapping = portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(mapping, OtnOdu.VALUE, logicalConnPoint + "-ODU4");
        if (mapping.getSupportingOtu4() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingOtu4());
        }
        if (mapping.getSupportingEthernet() != null) {
            oduInterfaceBldr.setSupportingInterface(mapping.getSupportingEthernet());
        }
        if (isCTP) {
            // Create Interface1 type object required for adding as augmentation
            oduInterfaceBldr.addAugmentation(
                    createOdu4HOInterface1(ODUCTP.VALUE, MonitoringMode.Monitored, null, apiInfoA, apiInfoZ));
            // Post interface on the device
            openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
            return oduInterfaceBldr.getName();
        }
        // Create Interface1 type object required for adding as augmentation
        oduInterfaceBldr.addAugmentation(
                createOdu4HOInterface1(
                        // For TPDR it can be both CTP and TTP - For switch-ponder we still use TTP
                        mapping.getXpdrType() == XpdrNodeTypes.Tpdr ? ODUTTPCTP.VALUE : ODUTTP.VALUE,
                        MonitoringMode.Terminated,
                        new OpuBuilder()
                                .setPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                                .setExpPayloadType(PayloadTypeDef.getDefaultInstance(payloadType))
                                .build(),
                        apiInfoA, apiInfoZ));
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, oduInterfaceBldr);
        LOG.info("{}-{} updating mapping with interface {}", nodeId, logicalConnPoint, oduInterfaceBldr.getName());
        this.portMapping.updateMapping(nodeId, mapping);
        return oduInterfaceBldr.getName();
    }

    private Interface1 createOdu4HOInterface1(
            OduFunctionIdentity oduFunction, MonitoringMode monitoringMode, Opu opu,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) {
        OduBuilder oduIfBuilder = new OduBuilder()
                .setRate(ODU4.VALUE)
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
        return new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder()
                    .setOdu(oduIfBuilder.build())
                    .build();
    }



    public String createOpenRoadmOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(portMap, OtnOdu.VALUE, logicalConnPoint + "-ODU")
                .setSupportingInterface(supportingOtuInterface)
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder()
                        .setOdu(
                            // ODU interface specific data
                            new OduBuilder()
                                .setRate(ODU4.VALUE)
                                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                                .setOpu(
                                    new OpuBuilder()
                                        .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                                        .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                                        .build())
                                .build())
                        .build());
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
                OpenRoadmInterfaceException.mapping_msg_err(anodeId, alogicalConnPoint));
        }
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(znodeId, zlogicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(portMapA, OtnOdu.VALUE, alogicalConnPoint + "-ODU")
                .setSupportingInterface(supportingOtuInterface)
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder()
                        .setOdu(
                            new OduBuilder()
                                .setRate(ODU4.VALUE)
                                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                                .setOpu(
                                    // ODU interface specific data
                                    // Set Opu attributes
                                    new OpuBuilder()
                                        .setPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                                        .setExpPayloadType(PayloadTypeDef.getDefaultInstance("07"))
                                        .build())
                                .setTxSapi(portMapA.getLcpHashVal())
                                .setTxDapi(portMapZ.getLcpHashVal())
                                // Setting the expected Dapi and Sapi values
                                .setExpectedDapi(portMapA.getLcpHashVal())
                                .setExpectedSapi(portMapZ.getLcpHashVal())
                                .build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBldr);
        return oduInterfaceBldr.getName();
    }

    public String createOpenRoadmOtu4Interface(String nodeId, String logicalConnPoint, String supportOchInterface,
            AEndApiInfo apiInfoA, ZEndApiInfo apiInfoZ) throws OpenRoadmInterfaceException {
        Mapping mapping = this.portMapping.getMapping(nodeId, logicalConnPoint);
        if (mapping == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        // OTU interface specific data
        OtuBuilder otuIfBuilder = new OtuBuilder()
            .setFec(OtuAttributes.Fec.Scfec)
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
            createGenericInterfaceBuilder(mapping, OtnOtu.VALUE, logicalConnPoint + "-OTU")
                .setSupportingInterface(supportOchInterface)
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev181019.Interface1Builder()
                        .setOtu(otuIfBuilder.build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(nodeId, otuInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return otuInterfaceBldr.getName();
    }

    public String createOpenRoadmOchInterfaceName(String logicalConnectionPoint, String spectralSlotName) {
        return String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,logicalConnectionPoint, spectralSlotName);
    }

    private InterfaceBuilder createGenericInterfaceBuilder(Mapping portMap, InterfaceType type, String key) {
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
        if (mapping.getSupportingOms() != null) {
            return mapping.getSupportingOms();
        }
        // Create generic interface
        if (mapping.getSupportingOts() == null) {
            LOG.error("Unable to get ots interface from mapping {} - {}", nodeId, mapping.getLogicalConnectionPoint());
            return null;
        }
        InterfaceBuilder omsInterfaceBldr =
            createGenericInterfaceBuilder(
                    mapping, OpenROADMOpticalMultiplex.VALUE, "OMS-" + mapping.getLogicalConnectionPoint())
                .setSupportingInterface(mapping.getSupportingOts());
        this.openRoadmInterfaces.postInterface(nodeId, omsInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return omsInterfaceBldr.build().getName();
    }

    public String createOpenRoadmOtsInterface(String nodeId, Mapping mapping) throws OpenRoadmInterfaceException {
        if (mapping.getSupportingOts() != null) {
            return mapping.getSupportingOts();
        }
        // Create generic interface
        InterfaceBuilder otsInterfaceBldr =
            createGenericInterfaceBuilder(mapping, OpticalTransport.VALUE, "OTS-" + mapping.getLogicalConnectionPoint())
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019
                            .Interface1Builder()
                        // OTS interface augmentation specific data
                        .setOts(new OtsBuilder().setFiberType(OtsAttributes.FiberType.Smf).build())
                        .build());
        this.openRoadmInterfaces.postInterface(nodeId, otsInterfaceBldr);
        this.portMapping.updateMapping(nodeId, mapping);
        return otsInterfaceBldr.build().getName();
    }

    public boolean isUsedByXc(String nodeId, String interfaceName, String xc,
            DeviceTransactionManager deviceTransactionManager) {
        DataObjectIdentifier<RoadmConnections> xciid = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(RoadmConnections.class, new RoadmConnectionsKey(xc))
            .build();
        LOG.info("reading xc {} in node {}", xc, nodeId);
        Optional<RoadmConnections> crossconnection = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, xciid, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (crossconnection.isEmpty()) {
            LOG.info("xd {} not found !", xc);
            return false;
        }
        RoadmConnections rc = crossconnection.orElseThrow();
        LOG.info("xd {} found", xc);
        String supportedinter =
            interfaceName.contains("nmc")
                ? null
                : interfaceName.replace("mc", "nmc");
        if (rc.getSource().getSrcIf().equals(interfaceName)
                || rc.getDestination().getDstIf().equals(interfaceName)
                || rc.getSource().getSrcIf().equals(supportedinter)
                || rc.getDestination().getDstIf().equals(supportedinter)) {
            return true;
        }
        return false;
    }

    public boolean isUsedByOtnXc(String nodeId, String interfaceName, String xc,
            DeviceTransactionManager deviceTransactionManager) {
        DataObjectIdentifier<OduConnection> xciid = DataObjectIdentifier
            .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
            .child(OduConnection.class, new OduConnectionKey(xc))
            .build();
        LOG.info("reading xc {} in node {}", xc, nodeId);
        Optional<OduConnection> oduConnectionOpt = deviceTransactionManager.getDataFromDevice(nodeId,
            LogicalDatastoreType.CONFIGURATION, xciid, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (oduConnectionOpt.isEmpty()) {
            LOG.info("xc {} not found !", xc);
            return false;
        }
        OduConnection oduXc = oduConnectionOpt.orElseThrow();
        LOG.info("xc {} found", xc);
        if (oduXc.getSource().getSrcIf().equals(interfaceName)
                || oduXc.getDestination().getDstIf().equals(interfaceName)) {
            return true;
        }
        return false;
    }

    public String createOpenRoadmOtnOdu4Interface(String nodeId, String logicalConnPoint, String supportingOtuInterface)
            throws OpenRoadmInterfaceException {
        Mapping portMap = portMapping.getMapping(nodeId, logicalConnPoint);
        if (portMap == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(nodeId, logicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(portMap, OtnOdu.VALUE, logicalConnPoint + "-ODU4")
                .setSupportingInterface(supportingOtuInterface)
                .addAugmentation(
                // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019.Interface1Builder()
                        .setOdu(
                        // ODU interface specific data
                            new OduBuilder()
                                .setRate(ODU4.VALUE)
                                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                                .setOduFunction(ODUTTP.VALUE)
                                .setOpu(
                                    // Set Opu attributes
                                    new OpuBuilder()
                                        .setPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                                        .setExpPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                                        .build()
                                )
                                .build())
                        .build());
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
                OpenRoadmInterfaceException.mapping_msg_err(anodeId, alogicalConnPoint));
        }
        Mapping portMapZ = portMapping.getMapping(znodeId, zlogicalConnPoint);
        if (portMapZ == null) {
            throw new OpenRoadmInterfaceException(
                OpenRoadmInterfaceException.mapping_msg_err(znodeId, zlogicalConnPoint));
        }
        InterfaceBuilder oduInterfaceBldr =
            createGenericInterfaceBuilder(portMapA, OtnOdu.VALUE, alogicalConnPoint + "-ODU4")
                .setSupportingInterface(asupportingOtuInterface)
                .addAugmentation(
                    // Create Interface1 type object required for adding as augmentation
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev181019
                            .Interface1Builder()
                        .setOdu(
                        // ODU interface specific data
                            new OduBuilder()
                                .setRate(ODU4.VALUE)
                                .setMonitoringMode(OduAttributes.MonitoringMode.Terminated)
                                .setTxSapi(portMapA.getLcpHashVal())
                                .setTxDapi(portMapZ.getLcpHashVal())
                                .setExpectedSapi(portMapZ.getLcpHashVal())
                                .setExpectedDapi(portMapA.getLcpHashVal())
                                .setOduFunction(ODUTTP.VALUE)
                                .setOpu(
                                    // Set Opu attributes
                                    new OpuBuilder()
                                        .setPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                                        .setExpPayloadType(PayloadTypeDef.getDefaultInstance("21"))
                                        .build())
                                .build())
                        .build());
        // Post interface on the device
        openRoadmInterfaces.postInterface(anodeId, oduInterfaceBldr);
        this.portMapping.updateMapping(anodeId, portMapA);
        return oduInterfaceBldr.getName();
    }
}
