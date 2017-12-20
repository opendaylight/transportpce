/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory;

import static org.opendaylight.transportpce.inventory.utils.StringUtils.getCurrentTimestamp;
import static org.opendaylight.transportpce.inventory.utils.StringUtils.prepareDashString;
import static org.opendaylight.transportpce.inventory.utils.StringUtils.prepareEmptyString;

import com.google.common.base.Preconditions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import javax.sql.DataSource;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.inventory.query.Queries;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelf.Slots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.shelves.Shelves;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class INode {
    private static final Logger LOG = LoggerFactory.getLogger(INode.class);

    private final DataSource dataSource;
    private final DeviceTransactionManager deviceTransactionManager;

    public INode(DataSource dataSource, DeviceTransactionManager deviceTransactionManager) {
        this.dataSource = dataSource;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public boolean addNode(Info deviceInfo) {
        boolean sqlResult = false;
        String query = Queries.getQuery().deviceInfoInsert().get();
        LOG.debug("Running {} query ", query);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            String[] prepareParameters = prepareDeviceInfoParameters(deviceInfo);
            for (int i = 0; i < prepareParameters.length; i++) {
                LOG.debug("Parameter {} has value {}", i + 1, prepareParameters[i]);
                preparedStatement.setString(i + 1, prepareParameters[i]);
            }
            int executeUpdate = preparedStatement.executeUpdate();
            LOG.info("#{} entries were added", executeUpdate);
            sqlResult = true;
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return sqlResult;
    }

    public boolean nodeExists(String nodeId) {
        String selectTableSQL = "select count(*) node_exists from inv_dev_info where node_id = ?";
        int nodeExists = 0;
        LOG.info("Checking if {} exists in DB", nodeId);
        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStmt = connection.prepareStatement(selectTableSQL)) {
            preparedStmt.setString(1, nodeId);
            try (ResultSet rs = preparedStmt.executeQuery()) {
                while (rs.next()) {
                    nodeExists = rs.getInt("node_exists");
                    LOG.debug("Found {} devices matching {}", nodeExists, nodeId);
                }
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return nodeExists == 0 ? false : true;
    }

    public void getRoadmShelves(String nodeId) throws InterruptedException, ExecutionException {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        java.util.Optional<OrgOpenroadmDevice> deviceObject = deviceTransactionManager.getDataFromDevice(nodeId,
                LogicalDatastoreType.OPERATIONAL, deviceIID, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        LOG.info("Shelves size {}", deviceObject.get().getShelves().size());
        try (Connection connection = dataSource.getConnection()) {
            Preconditions.checkNotNull(connection);
            for (int i = 0; i < deviceObject.get().getShelves().size(); i++) {
                Shelves shelve = deviceObject.get().getShelves().get(i);
                String shelfName = shelve.getShelfName();

                LOG.info("Getting Shelve Details of {}", shelfName);
                LOG.info("Slot Size {} ", shelve.getSlots().size());

                persistShelveSlots(nodeId, shelve, connection);

                persistShelves(nodeId, connection, shelve);
            }
        } catch (SQLException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }

    public void getCircuitPacks(String nodeId) throws InterruptedException, ExecutionException {
        InstanceIdentifier<OrgOpenroadmDevice> deviceIID = InstanceIdentifier.create(OrgOpenroadmDevice.class);
        java.util.Optional<OrgOpenroadmDevice> deviceObject =
                deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.OPERATIONAL, deviceIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (!deviceObject.isPresent()) {
            LOG.warn("Device object {} was not found", nodeId);
            return;
        }
        LOG.info("Circuit pack size {}", deviceObject.get().getCircuitPacks().size());

        try (Connection connection = dataSource.getConnection()) {
            Preconditions.checkNotNull(connection);
            for (int i = 0; i < deviceObject.get().getCircuitPacks().size(); i++) {
                CircuitPacks cp = deviceObject.get().getCircuitPacks().get(i);

                if (cp.getCpSlots() != null) {
                    persistCircuitPacksSlots(nodeId, cp, connection);
                }
                LOG.info("Everything {}", cp);
                LOG.info("CP is {}", cp);

                persistPorts(cp, connection);

                persistCircuitPacks(nodeId, connection, cp);
            }
        } catch (SQLException e1) {
            LOG.error(e1.getMessage(), e1);
        }
    }

    private void persistCircuitPacks(String nodeId, Connection connection, CircuitPacks cp) {
        String[] parameters = prepareCircuitPacksParameters(nodeId, cp);
        String query = Queries.getQuery().deviceCircuitPackInsert().get();
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            for (int j = 0; j < parameters.length; j++) {
                stmt.setString(j + 1, parameters[j]);
            }
            stmt.execute();
            stmt.clearParameters();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void persistShelves(String nodeId, Connection connection, Shelves shelve) {
        String[] shelvesParameter = prepareShelvesParameters(nodeId, shelve);
        String query = Queries.getQuery().deviceShelfInsert().get();
        try (PreparedStatement preparedStmt = connection.prepareStatement(query)) {
            for (int j = 0; j < shelvesParameter.length; j++) {
                preparedStmt.setString(j + 1, shelvesParameter[j]);
            }
            preparedStmt.execute();
            preparedStmt.clearParameters();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void persistShelveSlots(String nodeId, Shelves shelves, Connection connection) {
        String startTimetampStr = getCurrentTimestamp();
        for (int i = 0; i < shelves.getSlots().size(); i++) {
            Slots slot = shelves.getSlots().get(i);
            LOG.info("Getting Slot Details of {}", slot.getSlotName());
            String[] parameters = new String[] {nodeId, shelves.getShelfName(), slot.getSlotName(), slot.getLabel(),
                    slot.getProvisionedCircuitPack(), startTimetampStr, startTimetampStr};
            String query = Queries.getQuery().deviceShelfSlotInsert().get();
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setString(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void persistCircuitPacksSlots(String nodeId, CircuitPacks circuitPacks, Connection connection) {
        LOG.warn("CP slots are not persisted yet");
    }

    private void persistPorts(CircuitPacks circuitPacks, Connection connection) {
        LOG.warn("Ports are not persisted yet");
    }


    /**
     * Prepares parameters for device insert query.
     *
     * @param deviceInfo the device Info
     * @return String[] a string
     */
    private static String[] prepareDeviceInfoParameters(Info deviceInfo) {
        String startTimetampStr = getCurrentTimestamp();

        return new String[] {prepareDashString(deviceInfo.getNodeId()), prepareDashString(deviceInfo.getNodeNumber()),
            prepareDashString(deviceInfo.getNodeType().getName()), prepareDashString(deviceInfo.getClli()),
            prepareDashString(deviceInfo.getVendor()), prepareDashString(deviceInfo.getModel()),
            prepareDashString(deviceInfo.getSerialId()), prepareDashString(deviceInfo.getPrefixLength()),
            prepareDashString(deviceInfo.getDefaultGateway()), prepareDashString(deviceInfo.getSource().getName()),
            prepareDashString(deviceInfo.getCurrentIpAddress()),
            prepareDashString(deviceInfo.getCurrentPrefixLength()),
            prepareDashString(deviceInfo.getDefaultGateway()),
            prepareDashString(deviceInfo.getMacAddress().getValue()),
            prepareDashString(deviceInfo.getSoftwareVersion()), prepareDashString(deviceInfo.getTemplate()),
            prepareDashString(deviceInfo.getCurrentDatetime()),
            deviceInfo.getGeoLocation() != null ? prepareDashString(deviceInfo.getGeoLocation().getLatitude()) : "",
            deviceInfo.getGeoLocation() != null ? prepareDashString(deviceInfo.getGeoLocation().getLongitude()) : "",
            prepareDashString(deviceInfo.getMaxDegrees()), prepareDashString(deviceInfo.getMaxSrgs()), startTimetampStr,
            startTimetampStr};
    }

    /**
     * Prepares parameters for shelves.
     *
     * @param nodeId the node ID
     * @param shelve the shelves
     * @return String[] a string
     */
    private static String[] prepareShelvesParameters(String nodeId, Shelves shelve) {
        String startTimestamp = getCurrentTimestamp();

        return new String[] {nodeId, shelve.getShelfName(), shelve.getShelfType(), shelve.getRack(),
                shelve.getShelfPosition(), prepareEmptyString(shelve.getAdministrativeState()), shelve.getVendor(),
                shelve.getModel(), shelve.getSerialId(), shelve.getType(), shelve.getProductCode(),
                prepareEmptyString(shelve.getManufactureDate()), shelve.getClei(), shelve.getHardwareVersion(),
                prepareEmptyString(shelve.getOperationalState()), prepareEmptyString(shelve.getEquipmentState()),
                prepareEmptyString(shelve.getDueDate()), startTimestamp, startTimestamp};
    }

    private static String[] prepareCircuitPacksParameters(String nodeId, CircuitPacks cpack) {
        return new String[] {nodeId, cpack.getCircuitPackName(), cpack.getCircuitPackType(),
                cpack.getCircuitPackProductCode(), prepareEmptyString(cpack.getAdministrativeState()),
                cpack.getVendor(), cpack.getModel(), cpack.getSerialId(), cpack.getType(), cpack.getProductCode(),
                prepareEmptyString(cpack.getManufactureDate()), cpack.getClei(), cpack.getHardwareVersion(),
                prepareEmptyString(cpack.getOperationalState()), prepareEmptyString(cpack.getEquipmentState()),
                cpack.getCircuitPackMode(), cpack.getShelf(), cpack.getSlot(), cpack.getSubSlot(),
                cpack.getCircuitPackCategory().getType().getName(), cpack.getCircuitPackCategory().getExtension(),
                prepareEmptyString(cpack.getDueDate()), cpack.getParentCircuitPack().getCircuitPackName(),
                cpack.getParentCircuitPack().getCpSlotName()};
    }
}
