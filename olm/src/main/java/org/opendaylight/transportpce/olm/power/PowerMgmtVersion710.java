/*
 * Copyright © 2020 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.power;

import com.google.common.util.concurrent.FluentFuture;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.OpticalControlMode;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.PowerDBm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529.otsi.container.OtsiBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public final class PowerMgmtVersion710 {
    private static final Logger LOG = LoggerFactory.getLogger(PowerMgmtVersion710.class);

    private PowerMgmtVersion710() {
    }

    /**
     * This method provides Transponder transmit power range.
     *
     * @param circuitPackName
     *            Transponder circuitPack name
     * @param portName
     *            Transponder port name
     * @param deviceId
     *            Node Id of a device
     * @param deviceTransactionManager
     *            Device transaction manager to read device data
     * @return HashMap holding Min and Max transmit power for given port
     */
    public static Map<String, Double> getXponderPowerRange(String circuitPackName, String portName, String deviceId,
            DeviceTransactionManager deviceTransactionManager) {
        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
                .child(Ports.class, new PortsKey(portName));
        LOG.debug("Fetching logical Connection Point value for port {} at circuit pack {}", portName, circuitPackName);
        Optional<Ports> portObject =
                deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, portIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!portObject.isPresent()) {
            return new HashMap<>();
        }
        Ports port = portObject.get();
        if (port.getTransponderPort() == null || port.getTransponderPort().getPortPowerCapabilityMaxTx() == null) {
            LOG.warn("Logical Connection Point value missing for {} {}", circuitPackName, port.getPortName());
            return new HashMap<>();
        }
        Map<String, Double> powerRangeMap = new HashMap<>();
        powerRangeMap.put("MaxTx", port.getTransponderPort().getPortPowerCapabilityMaxTx().getValue().doubleValue());
        powerRangeMap.put("MinTx", port.getTransponderPort().getPortPowerCapabilityMinTx().getValue().doubleValue());
        return powerRangeMap;
    }

    /**
     * This method provides Transponder transmit power range.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf- node
     * @param srgId
     *            SRG Id connected to transponder
     * @param deviceTransactionManager
     *            Device transaction manager to read device data
     * @param circuitPackName
     *            SRG circuitpack name
     * @param portName
     *            SRG port name
     * @return HashMap holding Min and Max transmit power for given port
     */
    public static Map<String, Double> getSRGRxPowerRange(String nodeId, String srgId,
            DeviceTransactionManager deviceTransactionManager,
            String circuitPackName, String portName) {
        LOG.debug("Coming inside SRG power range");
        LOG.debug("Mapping object exists.");
        InstanceIdentifier<Ports> portIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName))
                .child(Ports.class, new PortsKey(portName));
        LOG.debug("Fetching logical Connection Point value for port {} at circuit pack {}{}", portName,
                circuitPackName, portIID);
        Optional<Ports> portObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, portIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!portObject.isPresent()) {
            LOG.info("Port not found");
            return new HashMap<>();
        }
        Ports port = portObject.get();
        if (port.getRoadmPort() == null) {
            LOG.warn("Roadm ports power value is missing for {} {}", circuitPackName, port.getPortName());
            return new HashMap<>();
        }
        LOG.debug("Port found on the node ID");
        Map<String, Double> powerRangeMap = new HashMap<>();
        powerRangeMap.put("MinRx", port.getRoadmPort().getPortPowerCapabilityMinRx().getValue().doubleValue());
        powerRangeMap.put("MaxRx", port.getRoadmPort().getPortPowerCapabilityMaxRx().getValue().doubleValue());
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
     * @param deviceTransactionManager
     *            Device Transaction Manager
     * @param interfaceObj
     *            Interface object
     *
     * @return true/false based on status of operation
     */
    public static boolean setTransponderPower(String nodeId, String interfaceName, BigDecimal txPower,
            DeviceTransactionManager deviceTransactionManager,
            Interface interfaceObj) {
        LOG.debug("Setting target-power for transponder nodeId: {} InterfaceName: {}",
                nodeId, interfaceName);
        InterfaceBuilder otsiInterfaceBuilder = new InterfaceBuilder(interfaceObj);
        OtsiBuilder otsiBuilder = new OtsiBuilder(otsiInterfaceBuilder.augmentation(Interface1.class).getOtsi());
        otsiBuilder.setTransmitPower(new PowerDBm(txPower));
        otsiInterfaceBuilder.addAugmentation(new Interface1Builder().setOtsi(otsiBuilder.build()).build());
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(nodeId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                LOG.error("Transaction for device {} was not found during transponder power setup for Node:", nodeId);
                return false;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get transaction for device {} during transponder power setup!", nodeId, e);
            return false;
        }
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        deviceTx.merge(LogicalDatastoreType.CONFIGURATION, interfacesIID, otsiInterfaceBuilder.build());
        FluentFuture<? extends @NonNull CommitInfo> commit =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Transponder Power update is committed");
            return true;
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Setting transponder power failed: ", e);
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
     * @param crossConnect
     *            cross connect.
     * @param deviceTransactionManager
     *            Device Transaction Manager.
     *
     * @return true/false based on status of operation.
     */
    public static boolean setPowerLevel(String deviceId, OpticalControlMode mode, BigDecimal powerValue,
            String connectionNumber, CrossConnect crossConnect,
            DeviceTransactionManager deviceTransactionManager) {
        @SuppressWarnings("unchecked") Optional<RoadmConnections> rdmConnOpt =
            (Optional<RoadmConnections>) crossConnect.getCrossConnect(deviceId, connectionNumber);
        if (!rdmConnOpt.isPresent()) {
            LOG.warn("Roadm-Connection is null in set power level ({})", connectionNumber);
            return false;
        }
        RoadmConnectionsBuilder rdmConnBldr = new RoadmConnectionsBuilder(rdmConnOpt.get());
        rdmConnBldr.setOpticalControlMode(mode);
        if (powerValue != null) {
            rdmConnBldr.setTargetOutputPower(new PowerDBm(powerValue));
        }
        RoadmConnections newRdmConn = rdmConnBldr.build();
        Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (!deviceTxOpt.isPresent()) {
                LOG.error("Transaction for device {} was not found!", deviceId);
                return false;
            }
            deviceTx = deviceTxOpt.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Unable to get transaction for device {}!", deviceId, e);
            return false;
        }
        // post the cross connect on the device
        InstanceIdentifier<RoadmConnections> roadmConnIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(RoadmConnections.class, new RoadmConnectionsKey(connectionNumber));
        deviceTx.merge(LogicalDatastoreType.CONFIGURATION, roadmConnIID, newRdmConn);
        FluentFuture<? extends @NonNull CommitInfo> commit =
            deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            commit.get();
            LOG.info("Roadm connection power level successfully set ");
            return true;
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Failed to post {}", newRdmConn, ex);
        }
        return false;
    }

}
