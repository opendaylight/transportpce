/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import com.google.common.util.concurrent.ListenableFuture;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.util.OlmUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014.och.container.OchBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PowerMgmt {
    private static final Logger LOG = LoggerFactory.getLogger(PowerMgmt.class);
    private static final long DATA_STORE_READ_TIMEOUT = 120;
    private final DataBroker db;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final CrossConnect crossConnect;
    private final DeviceTransactionManager deviceTransactionManager;

    public PowerMgmt(DataBroker db, OpenRoadmInterfaces openRoadmInterfaces,
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
    public Boolean setPower(ServicePowerSetupInput input) {
        LOG.info("Olm-setPower initiated");
        for (int i = 0; i < input.getNodes().size(); i++) {
            String nodeId = input.getNodes().get(i).getNodeId();
            String srcTpId =  input.getNodes().get(i).getSrcTp();
            String destTpId = input.getNodes().get(i).getDestTp();
            Optional<Nodes> inputNodeOptional = OlmUtils.getNode(nodeId, this.db);
            // If node type is transponder
            if (inputNodeOptional.isPresent()
                    && (inputNodeOptional.get().getNodeType() != null)
                    && inputNodeOptional.get().getNodeType().equals(NodeTypes.Xpdr)) {

                Nodes inputNode = inputNodeOptional.get();
                LOG.info("Getting data from input node {}", inputNode.getNodeType());
                LOG.info("Getting mapping data for node is {}", inputNode.getMapping().stream().filter(o -> o.key()
                        .equals(new MappingKey(destTpId))).findFirst().toString());
                // If its A-End transponder
                if (destTpId.toLowerCase().contains("network")) {
                    java.util.Optional<Mapping> mappingObject = inputNode.getMapping().stream().filter(o -> o.key()
                            .equals(new MappingKey(destTpId))).findFirst();
                    if (mappingObject.isPresent()) {
                        Map<String, Double> txPowerRangeMap = getXponderPowerRange(mappingObject.get()
                                .getSupportingCircuitPackName(), mappingObject.get().getSupportingPort(), nodeId);
                        if (!txPowerRangeMap.isEmpty()) {
                            LOG.info("Transponder range exists for nodeId: {}", nodeId);
                            String srgId =  input.getNodes().get(i + 1).getSrcTp();
                            String nextNodeId = input.getNodes().get(i + 1).getNodeId();

                            Map<String, Double> rxSRGPowerRangeMap = getSRGRxPowerRange(nextNodeId, srgId);
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
                                String interfaceName = destTpId + "-" + input.getWaveNumber();
                                if (setTransponderPower(nodeId, interfaceName, new BigDecimal(powerValue))) {
                                    LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                                    try {
                                        LOG.info("Now going in sleep mode");
                                        Thread.sleep(120000);
                                    } catch (InterruptedException e) {
                                        LOG.info("Transponder warmup failed for OCH connection: {}", interfaceName, e);
                                    }
                                } else {
                                    LOG.info("Transponder OCH connection: {} power update failed ", interfaceName);
                                }
                            } else {
                                LOG.info("SRG Power Range not found");
                            }
                        } else {
                            LOG.info("Tranponder range not available seting to default "
                                    + "power for nodeId: {}", nodeId);
                            String interfaceName = destTpId + "-" + input.getWaveNumber();
                            if (setTransponderPower(nodeId, interfaceName, new BigDecimal(-5))) {
                                LOG.info("Transponder OCH connection: {} power updated ", interfaceName);
                                try {
                                    Thread.sleep(120000);
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
                    && (inputNodeOptional.get().getNodeType() != null)
                    && inputNodeOptional.get().getNodeType().equals(NodeTypes.Rdm)) {
                // If Degree is transmitting end then set power
                Nodes inputNode = inputNodeOptional.get();
                LOG.info("This is a roadm device ");
                String connectionNumber = srcTpId + "-" + destTpId + "-" + input.getWaveNumber();
                LOG.info("Connection number is {}", connectionNumber);
                if (destTpId.toLowerCase().contains("deg")) {
                    Optional<Mapping> mappingObjectOptional = inputNode.getMapping().stream().filter(o -> o.key()
                            .equals(new MappingKey(destTpId))).findFirst();
                    if (mappingObjectOptional.isPresent()) {
                        LOG.info("Dest point is Degree {}", mappingObjectOptional.get());
                        Mapping portMapping = mappingObjectOptional.get();
                        Optional<Interface> interfaceOpt;
                        try {
                            interfaceOpt =
                                this.openRoadmInterfaces.getInterface(nodeId, portMapping.getSupportingOts());
                        } catch (OpenRoadmInterfaceException ex) {
                            LOG.error("Failed to get interface {} from node {}!", portMapping.getSupportingOts(),
                                    nodeId, ex);
                            return false;
                        }
                        if (interfaceOpt.isPresent()) {
                            BigDecimal spanLossTx = interfaceOpt.get().augmentation(Interface1.class).getOts()
                                    .getSpanLossTransmit().getValue();
                            LOG.info("Spanloss TX is {}", spanLossTx);
                            BigDecimal powerValue = BigDecimal.valueOf(Math.min(spanLossTx.doubleValue() - 9, 2));
                            LOG.info("Power Value is {}", powerValue);
                            try {
                                Boolean setXconnPowerSuccessVal = setPowerLevel(nodeId,
                                        OpticalControlMode.Power, powerValue, connectionNumber);
                                LOG.info("Success Value is {}", setXconnPowerSuccessVal);
                                if (setXconnPowerSuccessVal) {
                                    LOG.info("Roadm-connection: {} updated ");
                                    //The value recommended by the white paper is 20 seconds and not 60.
                                    //TODO - commented code because one vendor is not supporting
                                    //GainLoss with target-output-power
                                    Thread.sleep(60000);
                                    setPowerLevel(nodeId, OpticalControlMode.GainLoss, powerValue,
                                            connectionNumber);
                                } else {
                                    LOG.info("Set Power failed for Roadm-connection: {} on Node: {}", connectionNumber,
                                            nodeId);
                                    return false;
                                }
                            } catch (InterruptedException e) {
                                LOG.error("Olm-setPower wait failed {}", e);
                                return false;
                            }
                        } else {
                            LOG.error("Interface {} on node {} is not present!", portMapping.getSupportingOts(),
                                    nodeId);
                            return false;
                        }
                    }
                  // If Drop node leave node is power mode
                } else if (destTpId.toLowerCase().contains("srg")) {
                    LOG.info("Setting power at drop node");
                    setPowerLevel(nodeId, OpticalControlMode.Power, null, connectionNumber);
                }
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
        LOG.info("Olm-powerTurnDown initiated");
        /*Starting with last element into the list Z -> A for
          turning down A -> Z */
        for (int i = input.getNodes().size() - 1; i >= 0; i--) {
            String nodeId = input.getNodes().get(i).getNodeId();
            String srcTpId =  input.getNodes().get(i).getSrcTp();
            String destTpId = input.getNodes().get(i).getDestTp();
            Long wlNumber = input.getWaveNumber();
            String srcInterfaceName = srcTpId + "-" + wlNumber;
            //if (!setInterfaceOutOfService(nodeId, srcTpId, srcInterfaceName, deviceDb)) {
            //    LOG.warn("Out of service status update failed for interface {} ", srcInterfaceName);
            //    return false;
            //}
            String destInterfaceName = destTpId + "-" + wlNumber;
            //if (!setInterfaceOutOfService(nodeId, destTpId, destInterfaceName, deviceDb)) {
            //    LOG.warn("Out of service status update failed for interface {} ", destInterfaceName);
            //    return false;
            //}
            String connectionNumber =  srcTpId + "-" + destTpId + "-" + wlNumber;
            if (destTpId.toLowerCase().contains("srg")) {
                setPowerLevel(nodeId, OpticalControlMode.Off, null, connectionNumber);
            } else if (destTpId.toLowerCase().contains("deg")) {
                try {
                    if (!setPowerLevel(nodeId, OpticalControlMode.Power, new BigDecimal(-60),
                            connectionNumber)) {
                        LOG.warn("Power down failed for Roadm-connection: {}", connectionNumber);
                        return false;
                    }
                    Thread.sleep(20000);
                    if (!setPowerLevel(nodeId, OpticalControlMode.Off, null, connectionNumber)) {
                        LOG.warn("Setting power-control mode off failed for Roadm-connection: {}", connectionNumber);
                        return false;
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    LOG.error("Olm-powerTurnDown wait failed {}",e);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * This method updates interface administrative state to
     * outOfService.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param tpId
     *            Termination point of mounted netconf - node
     * @param interfaceName
     *            Name of interface which needs status update
     * @return true/false based on status of operation
     */
    private Boolean setInterfaceOutOfService(String nodeId, String tpId, String interfaceName) {
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier
                .create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        Optional<Interface> nodeInterfaceOpt;
        try {
            nodeInterfaceOpt = this.openRoadmInterfaces.getInterface(nodeId, interfaceName);
        } catch (OpenRoadmInterfaceException ex) {
            LOG.error("Failed to get interface {} from node {}!", interfaceName, nodeId, ex);
            return false;
        }
        if (nodeInterfaceOpt.isPresent()) {
            InterfaceBuilder intfBuilder = new InterfaceBuilder(nodeInterfaceOpt.get());
            intfBuilder.setAdministrativeState(AdminStates.OutOfService);
            Future<Optional<DeviceTransaction>> deviceTxFuture =
                this.deviceTransactionManager.getDeviceTransaction(nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.get();
                } else {
                    LOG.error("Transaction for device {} was not found!", nodeId);
                    return false;
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Unable to get transaction for device {}!", nodeId, e);
                return false;
            }
            deviceTx.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, intfBuilder.build());
            ListenableFuture<Void> submit = deviceTx.submit(Timeouts.DEVICE_WRITE_TIMEOUT,
                    Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            try {
                submit.get();
                LOG.info("Successfully posted interface {}", interfaceName);
                return true;
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Failed to post {} ", interfaceName, ex);
                return false;
            }
        } else {
            LOG.error("Interface {} on node {} is not present!", interfaceName, nodeId);
            return false;
        }
    }

    /**
     * This method provides Transponder transmit power range.
     *
     * @param circuitPackName
     *            Transponder circuitPack name
     * @param portName
     *            Transponder port name
     * @param deviceId
     *            Databroker for the given device
     * @return HashMap holding Min and Max transmit power for given port
     */
    private Map<String, Double> getXponderPowerRange(String circuitPackName, String portName, String deviceId) {
        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
                .child(Ports.class, new PortsKey(portName));
        Map<String, Double> powerRangeMap = new HashMap<>();
        LOG.info("Fetching logical Connection Point value for port {} at circuit pack {}", portName, circuitPackName);
        Optional<Ports> portObject =
                this.deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, portIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (portObject.isPresent()) {
            Ports port = portObject.get();
            if (port.getTransponderPort() != null && port.getTransponderPort().getPortPowerCapabilityMaxTx() != null
                && port.getTransponderPort().getPortPowerCapabilityMinTx() != null) {
                powerRangeMap.put("MaxTx", port.getTransponderPort().getPortPowerCapabilityMaxTx().getValue()
                        .doubleValue());
                powerRangeMap.put("MinTx", port.getTransponderPort().getPortPowerCapabilityMinTx().getValue()
                        .doubleValue());
            } else {
                LOG.warn("Port {} of ciruit-pack {} has no power capability values",
                    port.getPortName(), circuitPackName);
            }
        }
        return powerRangeMap;
    }

    /**
     * This method provides Transponder transmit power range.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param srgId
     *            SRG Id connected to transponder
     * @return HashMap holding Min and Max transmit power for given port
     */
    private Map<String, Double> getSRGRxPowerRange(String nodeId, String srgId) {
        Map<String, Double> powerRangeMap = new HashMap<>();
        LOG.info("Coming inside Xpdr power range");
        Optional<Mapping> mappingSRGOptional = OlmUtils.getNode(nodeId, this.db).flatMap(node -> node.getMapping()
                .stream().filter(o -> o.key()
                        .equals(new MappingKey(srgId))).findFirst());
        if (mappingSRGOptional.isPresent()) {
            LOG.info("Mapping object exists.");
            String circuitPackName = mappingSRGOptional.get().getSupportingCircuitPackName();
            String portName = mappingSRGOptional.get().getSupportingPort();
            InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
                    .child(Ports.class, new PortsKey(portName));

            LOG.info("Fetching logical Connection Point value for port {} at circuit pack {}{}", portName,
                    circuitPackName, portIID);
            Optional<Ports> portObject =
                    this.deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, portIID,
                            Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
            if (portObject.isPresent()) {
                Ports port = portObject.get();
                if (port.getRoadmPort() != null) {
                    LOG.info("Port found on the node ID");
                    powerRangeMap.put("MinRx", port.getRoadmPort()
                            .getPortPowerCapabilityMinRx().getValue().doubleValue());
                    powerRangeMap.put("MaxRx", port.getRoadmPort()
                            .getPortPowerCapabilityMaxRx().getValue().doubleValue());
                    return powerRangeMap;
                } else {
                    LOG.warn("Roadm ports power value is missing for {} {}", circuitPackName, port.getPortName());
                }
            } else {
                LOG.info("Port not found");
            }

        } else {
            LOG.info("Port mapping not found for nodeId: {} and srgId: {} ",
                    nodeId, srgId);
        }
        return powerRangeMap;

    }

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
     * @return true/false based on status of operation
     */
    private boolean setTransponderPower(String nodeId, String interfaceName, BigDecimal txPower) {
        LOG.info("Setting target-power for transponder nodeId: {} InterfaceName: {}",
                nodeId, interfaceName);
        Optional<Interface> interfaceOptional;
        try {
            interfaceOptional = this.openRoadmInterfaces.getInterface(nodeId, interfaceName);
        } catch (OpenRoadmInterfaceException ex) {
            LOG.error("Failed to get interface {} from node {}!", interfaceName, nodeId, ex);
            return false;
        }
        if (interfaceOptional.isPresent()) {
            InterfaceBuilder ochInterfaceBuilder =
                    new InterfaceBuilder(interfaceOptional.get());
            OchBuilder ochBuilder = new OchBuilder(ochInterfaceBuilder.augmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014
                            .Interface1.class).getOch());
            ochBuilder.setTransmitPower(new PowerDBm(txPower));
            ochInterfaceBuilder.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.interfaces.rev161014
                            .Interface1.class, new Interface1Builder().setOch(ochBuilder.build()).build());

            Future<Optional<DeviceTransaction>> deviceTxFuture =
                this.deviceTransactionManager.getDeviceTransaction(nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.get();
                } else {
                    LOG.error("Transaction for device {} was not found!", nodeId);
                    return false;
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Unable to get transaction for device {}!", nodeId, e);
                return false;
            }

            InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(Interface.class, new InterfaceKey(interfaceName));
            deviceTx.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, ochInterfaceBuilder.build());
            ListenableFuture<Void> submit = deviceTx.submit(Timeouts.DEVICE_WRITE_TIMEOUT,
                    Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            try {
                submit.get();
                LOG.info("Power update is submitted");
                return true;
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Setting transponder power failed {}", e);
            }
        } else {
            LOG.error("Interface {} on node {} is not present!", interfaceName, nodeId);
        }
        return false;
    }

    /**
     * This method does an edit-config on roadm connection subtree for a given
     * connection number in order to set power level for use by the optical
     * power control.
     *
     * @param deviceId
     *            Device id.
     * @param mode
     *            Optical control modelcan be off, power or gainLoss.
     * @param powerValue
     *            Power value in DBm.
     * @param connectionNumber
     *            Name of the cross connect.
     * @return true/false based on status of operation.
     */
    private boolean setPowerLevel(String deviceId, OpticalControlMode mode, BigDecimal powerValue,
            String connectionNumber) {
        Optional<RoadmConnections> rdmConnOpt = this.crossConnect.getCrossConnect(deviceId, connectionNumber);
        if (rdmConnOpt.isPresent()) {
            RoadmConnectionsBuilder rdmConnBldr = new RoadmConnectionsBuilder(rdmConnOpt.get());
            rdmConnBldr.setOpticalControlMode(mode);
            if (powerValue != null) {
                rdmConnBldr.setTargetOutputPower(new PowerDBm(powerValue));
            }
            RoadmConnections newRdmConn = rdmConnBldr.build();

            Future<Optional<DeviceTransaction>> deviceTxFuture =
                    this.deviceTransactionManager.getDeviceTransaction(deviceId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.get();
                } else {
                    LOG.error("Transaction for device {} was not found!", deviceId);
                    return false;
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Unable to get transaction for device {}!", deviceId, e);
                return false;
            }

            // post the cross connect on the device
            InstanceIdentifier<RoadmConnections> roadmConnIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber));
            deviceTx.put(LogicalDatastoreType.CONFIGURATION, roadmConnIID, newRdmConn);
            ListenableFuture<Void> submit = deviceTx.submit(Timeouts.DEVICE_WRITE_TIMEOUT,
                    Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
            try {
                submit.get();
                LOG.info("Roadm connection power level successfully set ");
                return true;
            } catch (InterruptedException | ExecutionException ex) {
                LOG.warn("Failed to post {}", newRdmConn, ex);
            }

        } else {
            LOG.warn("Roadm-Connection is null in set power level ({})", connectionNumber);
        }
        return false;
    }

}
