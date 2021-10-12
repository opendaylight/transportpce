/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.inventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING",
    justification = "TODO review the SQL statement generation process")
public class INode {
    private static final Logger LOG = LoggerFactory.getLogger(INode.class);

    private final DataSource dataSource;
    private final DeviceTransactionManager deviceTransactionManager;
    //private final INode221 inode221;
    private final INode121 inode121;

    public INode(DataSource dataSource, DeviceTransactionManager deviceTransactionManager,
        INode121 inode121
        //, INode221 inode221
    ) {
        this.dataSource = dataSource;
        this.deviceTransactionManager = deviceTransactionManager;
        this.inode121 = inode121;
        //this.inode221 = inode221;
    }

    public boolean addNode(String deviceId, String openROADMversion) {
        //boolean sqlResult = false;
        return inode121.addNode(deviceId);

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
            LOG.error("Something wrong when fetching node in DB", e);
        }
        return nodeExists != 0;
    }

    public boolean dataExists(String tableName, String searchKeys) {
        String selectTableSQL = "select count(*) data_exists from " + tableName + " where " + searchKeys;
        int dataExists = 0;
        LOG.info("Checking if {} exists in DB", searchKeys);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStmt = connection.prepareStatement(selectTableSQL)) {

            try (ResultSet rs = preparedStmt.executeQuery()) {
                while (rs.next()) {
                    dataExists = rs.getInt("data_exists");
                    LOG.debug("Found {} devices matching {}", dataExists, searchKeys);
                }
            }
        } catch (SQLException e) {
            LOG.error("Something wrong when fetching data in DB", e);
        }
        return dataExists != 0;
    }

  /*  public void getRoadmShelves(String nodeId, String openRoadmVersion)
        throws InterruptedException, ExecutionException {

        LOG.info("ROADMSHELVES");
        if (openRoadmVersion.equalsIgnoreCase(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1)) {
            inode121.getRoadmShelves(nodeId);
        }
        else if (openRoadmVersion.equalsIgnoreCase(StringConstants.OPENROADM_DEVICE_VERSION_2_2)) {
            inode22.getRoadmShelves(nodeId);
        }
        return;
    }

    public void getCircuitPacks(String nodeId, String openRoadmVersion)
        throws InterruptedException, ExecutionException {

        LOG.info("ROADMCircuitPacks");
        if (openRoadmVersion.equalsIgnoreCase(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1)) {
            inode121.getCircuitPacks(nodeId);
        }
        else if (openRoadmVersion.equalsIgnoreCase(StringConstants.OPENROADM_DEVICE_VERSION_2_2)) {
            inode22.getCircuitPacks(nodeId);
        }
        return;
    }
*/


}
