/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceInventory {
    private static final String INSERT_ALARM_STRING =
        "insert into inv_alarm_info(nodeid, probablecause, direction,extension,location,"
            + "notificationid,type,raisetime,severity,circuitid,circuitpack,connection,degree,iface,"
            + "internallink,physicallink,service,shelf,sharedriskgroup,port,portcircuitpack, create_date, update_date) "
            + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final Logger LOG = LoggerFactory.getLogger(DeviceInventory.class);

    private final DataSource dataSource;
    private final INode inode;
    private final DeviceTransactionManager deviceTransactionManager;

    public DeviceInventory(DataSource dataSource, INode inode,
            DeviceTransactionManager deviceTransactionManager) {
        this.dataSource = dataSource;
        this.inode = inode;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public void init() {
        LOG.info("Initializing {}", DeviceInventory.class.getName());
    }

    public void initializeDevice(String deviceId) throws InterruptedException, ExecutionException {
        InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
        Optional<Info> infoOpt =
                this.deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, infoIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);

        Info deviceInfo;
        if (infoOpt.isPresent()) {
            deviceInfo = infoOpt.get();
        } else {
            LOG.warn("Could not get device info from DataBrooker");
            return;
        }
        LOG.info("Creating Device Inventory {}", deviceInfo);
        if (!this.inode.nodeExists(deviceId)) {
            LOG.info("Adding node {} to inventory", deviceId);
            this.inode.addNode(deviceInfo);
            this.inode.getRoadmShelves(deviceId);
            this.inode.getCircuitPacks(deviceId);
        }
    }

    /**
     * Stores the alarm into DB using {@link PreparedStatement}.
     *
     * @param alarmString an alarm string
     * @return number of rows inserted
     */
    public int storeAlarm(String alarmString) {
        String delimiter = "|";
        String[] splitAlarmString = alarmString.split(Pattern.quote(delimiter));
        int count = 0;
        try (Connection connection = this.dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_ALARM_STRING)) {
            LOG.debug("Inserting prepared stmt for {} query", INSERT_ALARM_STRING);
            SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date startTimetamp = new Date();
            String startTimetampStr = simpleDate.format(startTimetamp);

            for (int i = 0; i < 21; i++) {
                String value = (splitAlarmString.length >= (i + 1)) ? splitAlarmString[i] : "";
                LOG.debug("Setting parameter {}, to {} in the insert alarm query", i + 1, value);
                statement.setString(i + 1, value);
            }
            statement.setString(22, startTimetampStr);
            statement.setString(23, startTimetampStr);
            LOG.debug("Setting current time and edited time to {}", startTimetampStr);
            count = statement.executeUpdate();
            LOG.debug("Statment {}, returned {}", INSERT_ALARM_STRING, count);
            statement.clearParameters();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return count;
    }
}
