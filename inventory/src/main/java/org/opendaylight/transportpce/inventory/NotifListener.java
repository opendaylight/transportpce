/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory;

import com.google.common.base.Preconditions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.opendaylight.transportpce.inventory.query.Queries;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.Edit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifListener implements OrgOpenroadmDeviceListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotifListener.class);

    private final DataSource dataSource;
    private final String deviceId;

    public NotifListener(DataSource dataSource, String deviceId) {
        this.dataSource = dataSource;
        this.deviceId = deviceId;
    }

    @Override
    public void onChangeNotification(ChangeNotification notification) {
        LOG.info("notification received: {}", notification.toString());
        try (Connection connection = dataSource.getConnection()) {
            Preconditions.checkNotNull(connection);
            String target = null;
            String infobef = null;
            String infoaft = null;
            String operation = null;
            for (Edit edit:notification.getEdit()) {
                // Get name of the interface changed. We will need to
                // have the target type and resource name separate
                //edit.getTarget().firstKeyOf(Interface.class).getName());
                // if there is a need to do something else with the information coming
                //from the device, take a look at the device listener to see how to access
                //each of the parameters of the notification
                target = edit.getTarget().toString();
                infobef = edit.getBefore();
                infoaft = edit.getAfter();
                operation = edit.getOperation().toString();
            }
            String datastoreMod = notification.getDatastore().getName();
            String changedBy = notification.getChangedBy().getServerOrUser().toString();
            String notifDate = notification.getChangeTime().getValue();
            Object[] parameters =
                    new Object[]{deviceId, notifDate, datastoreMod, target, infobef, infoaft, operation, changedBy};
            String query = Queries.getQuery().deviceNotificationInsert().get();
            LOG.info("Running {} query ", query);
            // y a parte actualizar la base de datos con la nueva info??
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                LOG.info("Preparing statement for query of notification");
                for (int j = 0; j < parameters.length; j++) {
                    stmt.setObject(j + 1, parameters[j]);
                }
                stmt.execute();
                stmt.clearParameters();
            } catch (SQLException e) {
                LOG.error("Execution of query failed!!");
                LOG.error(e.getMessage(), e);
            }
        } catch (SQLException e1) {
            LOG.error("Connection to database failed!!");
            LOG.error(e1.getMessage(), e1);
        }
    }

    @Override
    public void onOtdrScanResult(OtdrScanResult notification) {
    }
}
