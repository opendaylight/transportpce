/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.util.OlmUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210315.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.types.rev191129.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings("DM_CONVERT_CASE")
public class PowerMgmtImpl implements PowerMgmt {
    private static final Logger LOG = LoggerFactory.getLogger(PowerMgmtImpl.class);
    private final DataBroker db;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final CrossConnect crossConnect;
    private final DeviceTransactionManager deviceTransactionManager;
    private static final BigDecimal DEFAULT_TPDR_PWR_100G = new BigDecimal(-5);
    private static final BigDecimal DEFAULT_TPDR_PWR_400G = new BigDecimal(0);


    public PowerMgmtImpl(DataBroker db, OpenRoadmInterfaces openRoadmInterfaces,
                         CrossConnect crossConnect, DeviceTransactionManager deviceTransactionManager) {
        this.db = db;
        this.openRoadmInterfaces = openRoadmInterfaces;
        this.crossConnect = crossConnect;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    /**
     * This methods measures power requirement for turning up a WL
     * from the Spanloss at OTS transmit direction and update
     * roadm-connection target-output-power.
     *
     * @param input
     *            Input parameter from the olm servicePowerSetup rpc
     *
     * @return true/false based on status of operation.
     */
    //TODO Need to Case Optical Power mode/NodeType in case of 2.2 devices
    //@SuppressFBwarnings("DM_CONVERT_CASE")
    public Boolean setPower(ServicePowerSetupInput input) {
        LOG.info("Olm-setPower initiated for input {}", input);
        int lowerSpectralSlotNumber = input.getLowerSpectralSlotNumber().intValue();
        int higherSpectralSlotNumber = input.getHigherSpectralSlotNumber().intValue();
        String spectralSlotName = String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber),
                String.valueOf(higherSpectralSlotNumber));
        for (int i = 0; i < input.getNodes().size(); i++) {
            String nodeId = input.getNodes().get(i).getNodeId();
            String srcTpId =  input.getNodes().get(i).getSrcTp();
            String destTpId = input.getNodes().get(i).getDestTp();
            Optional<Nodes> inputNodeOptional = OlmUtils.getNode(nodeId, this.db);
            // If node type is transponder
            if (inputNodeOptional.isPresent()
                    && (inputNodeOptional.get().getNodeInfo().getNodeType() != null)
                    && inputNodeOptional.get().getNodeInfo().getNodeType().equals(NodeTypes.Xpdr)
                    && destTpId != null) {

                Nodes inputNode = inputNodeOptional.get();
                OpenroadmNodeVersion openroadmVersion = inputNode.getNodeInfo().getOpenroadmVersion();
                LOG.info("Getting data from input node {}", inputNode.getNodeInfo().getNodeType());
                LOG.info("Getting mapping data for node is {}",
                        inputNode.nonnullMapping().values().stream().filter(o -> o.key()
                         .equals(new MappingKey(destTpId))).findFirst().toString());
                // If its A-End transponder
                if (destTpId.toLowerCase().contains("network")) {
                    java.util.Optional<Mapping> mappingObject = inputNode.nonnullMapping()
                            .values().stream().filter(o -> o.key()
                            .equals(new MappingKey(destTpId))).findFirst();
                    boolean setTpdrPowerResult;
                    if (mappingObject.isPresent()) {
                        String circuitPackName = mappingObject.get().getSupportingCircuitPackName();
                        String portName = mappingObject.get().getSupportingPort();
                        Map<String, Double> txPowerRangeMap = new HashMap<>();
                        switch (openroadmVersion.getIntValue()) {
                            case 1:
                                txPowerRangeMap = PowerMgmtVersion121.getXponderPowerRange(circuitPackName, portName,
                                    nodeId, deviceTransactionManager);
                                break;
                            case 2:
                                txPowerRangeMap = PowerMgmtVersion221.getXponderPowerRange(circuitPackName, portName,
                                    nodeId, deviceTransactionManager);
                                break;
                            case 3:
                                txPowerRangeMap = PowerMgmtVersion710.getXponderPowerRange(circuitPackName, portName,
                                    nodeId, deviceTransactionManager);
                                break;
                            default:
                                LOG.error("Unrecognized OpenRoadm version");
                        }
                        if (!txPowerRangeMap.isEmpty()) {
                            LOG.info("Transponder range exists for nodeId: {}", nodeId);
                            OpenroadmNodeVersion rdmOpenroadmVersion = openroadmVersion;
                            String srgId =  input.getNodes().get(i + 1).getSrcTp();
                            String nextNodeId = input.getNodes().get(i + 1).getNodeId();
                            Optional<Nodes> inputNextOptional = OlmUtils.getNode(nextNodeId, this.db);
                            if (inputNextOptional.isPresent()) {
                                rdmOpenroadmVersion = inputNextOptional.get()
                                        .getNodeInfo().getOpenroadmVersion();
                            }
                            Map<String, Double> rxSRGPowerRangeMap = new HashMap<>();
                            Optional<Mapping> mappingObjectSRG = OlmUtils.getNode(nextNodeId, db)
                                    .flatMap(node -> node.nonnullMapping().values()
                                            .stream().filter(o -> o.key()
                                                    .equals(new MappingKey(srgId))).findFirst());

                            if (mappingObjectSRG.isPresent()) {
                                switch (rdmOpenroadmVersion.getIntValue()) {
                                    case 1:
                                        rxSRGPowerRangeMap = PowerMgmtVersion121.getSRGRxPowerRange(nextNodeId, srgId,
                                                deviceTransactionManager, mappingObjectSRG.get()
                                                        .getSupportingCircuitPackName(),
                                                mappingObjectSRG.get().getSupportingPort());
                                        break;
                                    case 2:
                                        rxSRGPowerRangeMap = PowerMgmtVersion221.getSRGRxPowerRange(nextNodeId, srgId,
                                                deviceTransactionManager, mappingObjectSRG.get()
                                                        .getSupportingCircuitPackName(),
                                                mappingObjectSRG.get().getSupportingPort());
                                        break;
                                    case 3:
                                        rxSRGPowerRangeMap = PowerMgmtVersion710.getSRGRxPowerRange(nextNodeId, srgId,
                                                deviceTransactionManager, mappingObjectSRG.get()
                                                        .getSupportingCircuitPackName(),
                                                mappingObjectSRG.get().getSupportingPort());
                                        break;
                                    default:
                                        LOG.error("Unrecognized OpenRoadm version");
                                        return false;
                                }
                            }
                            double powerValue = 0;
                            if (!rxSRGPowerRangeMap.isEmpty()) {
                                LOG.info("SRG Rx Power range exists for nodeId: {}", nodeId);
                                if (txPowerRangeMap.get("MaxTx")
                                        <= rxSRGPowerRangeMap.get("MaxRx")) {
                                    powerValue = txPowerRangeMap.get("MaxTx");
                                } else if (rxSRGPowerRangeMap.get("MaxRx")
                                        < txPowerRangeMap.get("MaxTx")) {
                                    powerValue = rxSRGPowerRangeMap.get("MaxRx");
                                }
                                LOG.info("Calculated Transponder Power value is {}" , powerValue);
                                String interfaceName = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,
                                        destTpId, spectralSlotName);
                                if (callSetTransponderPower(nodeId, interfaceName, new BigDecimal(powerValue),
                                        openroadmVersion)) {
                                    LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                                    try {
                                        LOG.info("Now going in sleep mode");
                                        Thread.sleep(OlmUtils.OLM_TIMER_1);
                                    } catch (InterruptedException e) {
                                        LOG.info("Transponder warmup failed for OCH connection: {}", interfaceName, e);
                                    }
                                } else {
                                    LOG.info("Transponder OCH connection: {} power update failed ", interfaceName);
                                }
                            } else {
                                LOG.info("SRG Power Range not found, setting the Transponder range to default");
                                String interfaceName = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,
                                        destTpId, spectralSlotName);
                                if (openroadmVersion.getIntValue() == 3) {
                                    setTpdrPowerResult = callSetTransponderPower(nodeId, interfaceName,
                                            DEFAULT_TPDR_PWR_400G, openroadmVersion);
                                } else {
                                    setTpdrPowerResult = callSetTransponderPower(nodeId, interfaceName,
                                            DEFAULT_TPDR_PWR_100G, openroadmVersion);
                                }
                                if (setTpdrPowerResult) {
                                    LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                                    try {
                                        Thread.sleep(OlmUtils.OLM_TIMER_1);
                                    } catch (InterruptedException e) {
                                        // TODO Auto-generated catch block
                                        LOG.info("Transponder warmup failed for OCH connection: {}", interfaceName, e);
                                    }
                                } else {
                                    LOG.info("Transponder OCH connection: {} power update failed ", interfaceName);
                                }
                            }
                        } else {
                            LOG.info("Tranponder range not available setting to default power for nodeId: {}", nodeId);
                            String interfaceName = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,
                                    destTpId, spectralSlotName);
                            if (openroadmVersion.getIntValue() == 3) {
                                setTpdrPowerResult = callSetTransponderPower(nodeId, interfaceName,
                                        DEFAULT_TPDR_PWR_400G, openroadmVersion);
                            } else {
                                setTpdrPowerResult = callSetTransponderPower(nodeId, interfaceName,
                                        DEFAULT_TPDR_PWR_100G, openroadmVersion);
                            }
                            if (setTpdrPowerResult) {
                                LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                                try {
                                    Thread.sleep(OlmUtils.OLM_TIMER_1);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    LOG.info("Transponder warmup failed for OCH connection: {}", interfaceName, e);
                                }
                            } else {
                                LOG.info("Transponder OCH connection: {} power update failed ", interfaceName);
                            }
                        }
                    } else {
                        LOG.info("Mapping object not found for nodeId: {}", nodeId);
                        return false;
                    }
                } else {
                    LOG.info("{} is a drop node. Net power settings needed", nodeId);
                }
            } else if (inputNodeOptional.isPresent()
                    && (inputNodeOptional.get().getNodeInfo().getNodeType() != null)
                    && inputNodeOptional.get().getNodeInfo().getNodeType().equals(NodeTypes.Rdm)) {
                // If Degree is transmitting end then set power
                Nodes inputNode = inputNodeOptional.get();
                OpenroadmNodeVersion openroadmVersion = inputNode.getNodeInfo().getOpenroadmVersion();
                LOG.info("This is a roadm {} device", openroadmVersion.getName());
                String connectionNumber = String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,srcTpId, destTpId,
                        spectralSlotName);
                LOG.info("Connection number is {}", connectionNumber);
                if (destTpId.toLowerCase().contains("deg")) {
                    Optional<Mapping> mappingObjectOptional = inputNode.nonnullMapping()
                            .values().stream().filter(o -> o.key()
                            .equals(new MappingKey(destTpId))).findFirst();
                    if (mappingObjectOptional.isPresent()) {
                        BigDecimal spanLossTx = null;
                        LOG.info("Dest point is Degree {}", mappingObjectOptional.get());
                        Mapping portMapping = mappingObjectOptional.get();
                        // debut reprise
                        if (openroadmVersion.getIntValue() == 1) {
                            Optional<Interface> interfaceOpt;
                            try {
                                interfaceOpt =
                                        this.openRoadmInterfaces.getInterface(nodeId, portMapping.getSupportingOts());
                            } catch (OpenRoadmInterfaceException ex) {
                                LOG.error("Failed to get interface {} from node {}!", portMapping.getSupportingOts(),
                                        nodeId, ex);
                                return false;
                            } catch (IllegalArgumentException ex) {
                                LOG.error("Failed to get non existing interface {} from node {}!",
                                    portMapping.getSupportingOts(), nodeId);
                                return false;
                            }
                            if (interfaceOpt.isPresent()) {
                                if (interfaceOpt.get().augmentation(Interface1.class).getOts()
                                    .getSpanLossTransmit() != null) {
                                    spanLossTx = interfaceOpt.get().augmentation(Interface1.class).getOts()
                                            .getSpanLossTransmit().getValue();
                                    LOG.info("Spanloss TX is {}", spanLossTx);
                                } else {
                                    LOG.error("interface {} has no spanloss value", interfaceOpt.get().getName());
                                }
                            } else {
                                LOG.error("Interface {} on node {} is not present!", portMapping.getSupportingOts(),
                                    nodeId);
                                return false;
                            }
                        } else if (openroadmVersion.getIntValue() == 2) {
                            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp
                                .Interface> interfaceOpt;
                            try {
                                interfaceOpt =
                                        this.openRoadmInterfaces.getInterface(nodeId, portMapping.getSupportingOts());
                            } catch (OpenRoadmInterfaceException ex) {
                                LOG.error("Failed to get interface {} from node {}!", portMapping.getSupportingOts(),
                                        nodeId, ex);
                                return false;
                            } catch (IllegalArgumentException ex) {
                                LOG.error("Failed to get non existing interface {} from node {}!",
                                    portMapping.getSupportingOts(), nodeId);
                                return false;
                            }
                            if (interfaceOpt.isPresent()) {
                                if (interfaceOpt.get().augmentation(org.opendaylight.yang.gen.v1.http.org
                                        .openroadm.optical.transport.interfaces.rev181019.Interface1.class).getOts()
                                        .getSpanLossTransmit() != null) {
                                    spanLossTx = interfaceOpt.get().augmentation(org.opendaylight.yang.gen.v1.http.org
                                            .openroadm.optical.transport.interfaces.rev181019.Interface1.class).getOts()
                                            .getSpanLossTransmit().getValue();
                                    LOG.info("Spanloss TX is {}", spanLossTx);
                                } else {
                                    LOG.error("interface {} has no spanloss value", interfaceOpt.get().getName());
                                }
                            } else {
                                LOG.error("Interface {} on node {} is not present!", portMapping.getSupportingOts(),
                                    nodeId);
                                return false;
                            }
                        }

                        if (spanLossTx == null || spanLossTx.intValue() <= 0 || spanLossTx.intValue() > 28) {
                            LOG.error(
                                "Power Value is null: spanLossTx null or out of openROADM range ]0,28] {}", spanLossTx);
                            return false;
                        }
                        BigDecimal powerValue = spanLossTx.subtract(BigDecimal.valueOf(9));
                        powerValue = powerValue.min(BigDecimal.valueOf(2));
                        //we work at constant power spectral density (50 GHz channel width @-20dBm=37.5GHz)
                        // 87.5 GHz channel width @-20dBm=75GHz
                        if (input.getWidth() != null && GridConstant.WIDTH_80.equals(input.getWidth().getValue())) {
                            powerValue = powerValue.add(BigDecimal.valueOf(3));
                        }
                        LOG.info("Power Value is {}", powerValue);
                        try {
                            Boolean setXconnPowerSuccessVal = crossConnect.setPowerLevel(nodeId,
                                OpticalControlMode.Power.getName(), powerValue, connectionNumber);
                            LOG.info("Success Value is {}", setXconnPowerSuccessVal);
                            if (setXconnPowerSuccessVal) {
                                LOG.info("Roadm-connection: {} updated ", connectionNumber);
                                //The value recommended by the white paper is 20 seconds and not 60.
                                //TODO - commented code because one vendor is not supporting
                                //GainLoss with target-output-power
                                Thread.sleep(OlmUtils.OLM_TIMER_2);
                                crossConnect.setPowerLevel(nodeId, OpticalControlMode.GainLoss.getName(), powerValue,
                                        connectionNumber);
                            } else {
                                LOG.info("Set Power failed for Roadm-connection: {} on Node: {}", connectionNumber,
                                        nodeId);
                                return false;
                            }
                        } catch (InterruptedException e) {
                            LOG.error("Olm-setPower wait failed :", e);
                            return false;
                        }
                    }
                    // If Drop node leave node is power mode
                } else if (destTpId.toLowerCase().contains("srg")) {
                    LOG.info("Setting power at drop node");
                    crossConnect.setPowerLevel(nodeId, OpticalControlMode.Power.getName(), null, connectionNumber);
                }
            } else {
                LOG.error("OLM-PowerMgmtImpl : Error with node type for node {}", nodeId);
            }
        }
        return true;
    }

    /**
     * This methods turns down power a WL by performing
     * following steps:
     *
     * <p>
     * 1. Pull interfaces used in service and change
     * status to outOfService
     *
     * <p>
     * 2. For each of the ROADM node set target-output-power
     * to -60dbm, wait for 20 seconds, turn power mode to off
     *
     * <p>
     * 3. Turn down power in Z to A direction and A to Z
     *
     * @param input
     *            Input parameter from the olm servicePowerTurndown rpc
     *
     * @return true/false based on status of operation
     */
    public Boolean powerTurnDown(ServicePowerTurndownInput input) {
        LOG.info("Olm-powerTurnDown initiated for input {}", input);
        /*Starting with last element into the list Z -> A for
          turning down A -> Z */
        int lowerSpectralSlotNumber = input.getLowerSpectralSlotNumber().intValue();
        int higherSpectralSlotNumber = input.getHigherSpectralSlotNumber().intValue();
        String spectralSlotName = String.join(GridConstant.SPECTRAL_SLOT_SEPARATOR,
                String.valueOf(lowerSpectralSlotNumber),
                String.valueOf(higherSpectralSlotNumber));
        for (int i = input.getNodes().size() - 1; i >= 0; i--) {
            String nodeId = input.getNodes().get(i).getNodeId();
            String srcTpId =  input.getNodes().get(i).getSrcTp();
            String destTpId = input.getNodes().get(i).getDestTp();
            String connectionNumber =  String.join(GridConstant.NAME_PARAMETERS_SEPARATOR,srcTpId, destTpId,
                    spectralSlotName);
            if (destTpId.toLowerCase().contains("srg")) {
                crossConnect.setPowerLevel(nodeId, OpticalControlMode.Off.getName(), null, connectionNumber);
            } else if (destTpId.toLowerCase().contains("deg")) {
                try {
                    if (!crossConnect.setPowerLevel(nodeId, OpticalControlMode.Power.getName(), new BigDecimal(-60),
                            connectionNumber)) {
                        LOG.warn("Power down failed for Roadm-connection: {}", connectionNumber);
                        return false;
                    }
                    Thread.sleep(OlmUtils.OLM_TIMER_2);
                    if (! crossConnect.setPowerLevel(nodeId, OpticalControlMode.Off.getName(), null,
                        connectionNumber)) {
                        LOG.warn("Setting power-control mode off failed for Roadm-connection: {}", connectionNumber);
                        return false;
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Olm-powerTurnDown wait failed: ",e);
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * This method does an edit-config on roadm connection subtree for a given
     * connection number in order to set power level for use by the optical
     * power control.
     *
     * @param inputNode
     *            PortMapping network node.
     * @param destTpId
     *            Destination termination point.
     * @param srgId
     *            SRG Id to which network port is connected to.
     * @param nextNodeId
     *            Next roadm connect.
     * @param waveLength
     *            WaveLength number part of request
     * @return true/false based on status of operation.
     */
    /*private boolean setTransponderPowerTx(Nodes inputNode, String destTpId, String srgId,
                                          String nextNodeId, Long waveLength) {
        Map<String, Double> txPowerRangeMap = null;
        Map<String, Double> rxSRGPowerRangeMap = null;
        OpenroadmVersion openroadmVersion;
        Optional<Mapping> mappingObject = inputNode.getMapping().stream().filter(o -> o.key()
                .equals(new MappingKey(destTpId))).findFirst();
        String nodeId = inputNode.getNodeId();
        if (mappingObject.isPresent()) {
            String circuitPackName = mappingObject.get().getSupportingCircuitPackName();
            String portName = mappingObject.get().getSupportingPort();
            openroadmVersion = inputNode.getNodeInfo().getOpenroadmVersion();
            if (openroadmVersion.getIntValue() == 1) {
                txPowerRangeMap = PowerMgmtVersion121.getXponderPowerRange(circuitPackName, portName,
                        nodeId, deviceTransactionManager);
            } else if (openroadmVersion.getIntValue() == 2) {
                txPowerRangeMap = PowerMgmtVersion221.getXponderPowerRange(circuitPackName, portName,
                        nodeId, deviceTransactionManager);
            }
            LOG.info("Transponder power range is fine");
            if (!txPowerRangeMap.isEmpty()) {
                LOG.info("Transponder power range is not null {}, {}", nextNodeId,srgId);
                //Transponder range is not empty then check SRG Range

                Optional<Mapping> mappingObjectSRG = OlmUtils.getNode(nextNodeId, db)
                        .flatMap(node -> node.getMapping()
                                .stream().filter(o -> o.key()
                                        .equals(new MappingKey(srgId))).findFirst());
                if (mappingObjectSRG.isPresent()) {
                    LOG.info("Transponder range exists for nodeId: {}", nodeId);
                    if (openroadmVersion.getIntValue() == 1) {
                        rxSRGPowerRangeMap = PowerMgmtVersion121.getSRGRxPowerRange(nextNodeId, srgId,
                                deviceTransactionManager, mappingObjectSRG.get().getSupportingCircuitPackName(),
                                mappingObjectSRG.get().getSupportingPort());
                    } else if (openroadmVersion.getIntValue() == 2) {
                        rxSRGPowerRangeMap = PowerMgmtVersion221.getSRGRxPowerRange(nextNodeId, srgId,
                                deviceTransactionManager, mappingObjectSRG.get().getSupportingCircuitPackName(),
                                mappingObjectSRG.get().getSupportingPort());
                    }
                }
                double powerValue = 0;
                if (!rxSRGPowerRangeMap.isEmpty()) {
                    LOG.debug("SRG Rx Power range exists for nodeId: {}", nodeId);
                    if (txPowerRangeMap.get("MaxTx")
                            <= rxSRGPowerRangeMap.get("MaxRx")) {
                        powerValue = txPowerRangeMap.get("MaxTx");
                    } else if (rxSRGPowerRangeMap.get("MaxRx")
                            < txPowerRangeMap.get("MaxTx")) {
                        powerValue = rxSRGPowerRangeMap.get("MaxRx");
                    }
                    LOG.debug("Calculated Transponder Power value is {}" , powerValue);
                    String interfaceName = destTpId + "-" + waveLength;
                    if (callSetTransponderPower(nodeId,interfaceName,new BigDecimal(powerValue),
                            openroadmVersion)) {
                        LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                        try {
                            LOG.info("Now going in sleep mode");
                            Thread.sleep(90000);
                            return true;
                        } catch (InterruptedException e) {
                            LOG.info("Transponder warmup failed for OCH connection: {}", interfaceName, e);
                            return false;
                        }
                    } else {
                        LOG.info("Transponder OCH connection: {} power update failed ", interfaceName);
                        return false;
                    }
                } else {
                    LOG.info("Transponder Range exists but SRG Power Range not found");
                    return false;
                }
            } else {
                LOG.info("Tranponder range not available seting to default power for nodeId: {}", nodeId);
                String interfaceName = destTpId + "-" + waveLength;
                if (callSetTransponderPower(nodeId,interfaceName,new BigDecimal(-5),
                        openroadmVersion)) {
                    LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                    try {
                        Thread.sleep(OlmUtils.OLM_TIMER_1);
                        return true;
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        LOG.info("Transponder warmup failed for OCH connection: {}", interfaceName, e);
                        return false;
                    }
                } else {
                    LOG.info("Transponder OCH connection: {} power update failed ", interfaceName);
                    return false;
                }
            }
        } else {
            LOG.info("Mapping object not found for nodeId: {}", nodeId);
            return false;
        }
    }*/

    /**
     * This method retrieves transponder OCH interface and
     * sets power.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param interfaceName
     *            OCH interface name carrying WL
     * @param txPower
     *            Calculated transmit power
     * @param openroadmVersion
     *            Version of openRoadm device software
     * @return true/false based on status of operation
     */
    private boolean callSetTransponderPower(String nodeId, String interfaceName, BigDecimal txPower,
                                            OpenroadmNodeVersion openroadmVersion) {
        boolean powerSetupResult = false;
        try {
            switch (openroadmVersion.getIntValue()) {
                case 1:
                    Optional<Interface> interfaceOptional121;
                    interfaceOptional121 = openRoadmInterfaces.getInterface(nodeId, interfaceName);
                    if (!interfaceOptional121.isPresent()) {
                        LOG.error("Interface {} on node {} is not present!", interfaceName, nodeId);
                        return false;
                    }
                    powerSetupResult = PowerMgmtVersion121.setTransponderPower(nodeId, interfaceName,
                                txPower, deviceTransactionManager, interfaceOptional121.get());
                    break;
                case 2:
                    Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp
                            .Interface> interfaceOptional221;
                    interfaceOptional221 = openRoadmInterfaces.getInterface(nodeId, interfaceName);
                    if (!interfaceOptional221.isPresent()) {
                        LOG.error("Interface {} on node {} is not present!", interfaceName, nodeId);
                        return false;
                    }
                    powerSetupResult = PowerMgmtVersion221.setTransponderPower(nodeId, interfaceName,
                                txPower, deviceTransactionManager, interfaceOptional221.get());
                    break;
                case 3:
                    Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp
                            .Interface> interfaceOptional710;
                    interfaceOptional710 = openRoadmInterfaces.getInterface(nodeId, interfaceName);
                    if (!interfaceOptional710.isPresent()) {
                        LOG.error("Interface {} on node {} is not present!", interfaceName, nodeId);
                        return false;
                    }
                    powerSetupResult = PowerMgmtVersion710.setTransponderPower(nodeId, interfaceName,
                            txPower, deviceTransactionManager, interfaceOptional710.get());
                    break;
                default:
                    LOG.error("Unrecognized OpenRoadm version");
                    return false;
            }
        } catch (OpenRoadmInterfaceException ex) {
            LOG.error("Failed to get interface {} from node {}!", interfaceName, nodeId, ex);
            return false;
        }
        if (powerSetupResult) {
            LOG.debug("Transponder power set up completed successfully for nodeId {} and interface {}",
                    nodeId,interfaceName);
        } else {
            LOG.debug("Transponder power setup failed for nodeId {} on interface {}",
                    nodeId, interfaceName);
        }
        return powerSetupResult;
    }

    /*
     * This method retrieves transponder OCH interface and
     * sets power.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param interfaceName
     *            OCH interface name carrying WL
     * @param openroadmVersion
     *            Version of openRoadm device software
     * @param wavelength
     *            Wavelength Number    *
     * @return true/false based on status of operation
     */
    /*private boolean callSetRoadmPowerTx(String nodeId, String interfaceName,
                                        OpenroadmVersion openroadmVersion,
                                        Long wavelength, String connectionNumber) {
        if (interfaceName == null) {
            crossConnect.setPowerLevel(nodeId,
                    OpticalControlMode.Power , null,connectionNumber);
            return true;
        }
        try {
            if (openroadmVersion.getIntValue() == 1) {
                Optional<Interface> interfaceOpt;
                interfaceOpt = openRoadmInterfaces.getInterface(nodeId, interfaceName);
                if (interfaceOpt.isPresent()) {
                    BigDecimal spanLossTx = interfaceOpt.get().augmentation(Interface1.class).getOts()
                            .getSpanLossTransmit().getValue();
                    LOG.debug("Spanloss TX is {}", spanLossTx);
                    BigDecimal powerValue = BigDecimal.valueOf(Math.min(spanLossTx.doubleValue() - 9, 2));
                    LOG.debug("Power Value is {}", powerValue);
                    Boolean setXconnPowerSuccessVal = crossConnect.setPowerLevel(nodeId,
                            OpticalControlMode.Power, powerValue,connectionNumber);
                    if (setXconnPowerSuccessVal) {
                        LOG.info("Roadm-connection: {} updated ");
                        //TODO - commented code because one vendor is not supporting
                        //GainLoss with target-output-power
                        Thread.sleep(OlmUtils.OLM_TIMER_2);
                        crossConnect.setPowerLevel(nodeId,
                                OpticalControlMode.GainLoss, powerValue,connectionNumber);
                        return true;
                    } else {
                        LOG.info("Set Power failed for Roadm-connection: {} on Node: {}", connectionNumber, nodeId);
                        return false;
                    }
                } else {
                    LOG.error("Interface {} on node {} is not present!", interfaceName, nodeId);
                    return false;
                }
            } else if (openroadmVersion.getIntValue() == 2) {
                Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces
                        .grp.Interface> interfaceOpt;
                interfaceOpt = openRoadmInterfaces.getInterface(nodeId, interfaceName);
                if (interfaceOpt.isPresent()) {
                    BigDecimal spanLossTx = interfaceOpt.get().augmentation(org.opendaylight.yang.gen.v1.http
                            .org.openroadm.optical.transport.interfaces.rev181019.Interface1.class).getOts()
                            .getSpanLossTransmit().getValue();
                    LOG.debug("Spanloss TX is {}", spanLossTx);
                    BigDecimal powerValue = BigDecimal.valueOf(Math.min(spanLossTx.doubleValue() - 9, 2));
                    LOG.debug("Power Value is {}", powerValue);
                    Boolean setXconnPowerSuccessVal = crossConnect.setPowerLevel(nodeId,
                            OpticalControlMode.Power, powerValue,connectionNumber);
                    if (setXconnPowerSuccessVal) {
                        LOG.info("Roadm-connection: {} updated ");
                        //TODO - commented code because one vendor is not supporting
                        //GainLoss with target-output-power
                        Thread.sleep(OlmUtils.OLM_TIMER_2);
                        crossConnect.setPowerLevel(nodeId,
                                OpticalControlMode.GainLoss, powerValue,connectionNumber);
                        return true;
                    } else {
                        LOG.info("Set Power failed for Roadm-connection: {} on Node: {}", connectionNumber, nodeId);
                        return false;
                    }
                }
            }
        } catch (OpenRoadmInterfaceException | InterruptedException ex) {
            LOG.error("Error during power setup on Roadm nodeId: {} for connection: {}", nodeId, connectionNumber, ex);
            return false;
        }
        return false;
    }*/

}
