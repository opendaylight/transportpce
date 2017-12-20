/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory.listener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.inventory.query.Queries;
import org.opendaylight.transportpce.inventory.query.QueryUtils;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.service.list.Services;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ServiceListener} listens on {@link ServiceList} changes.
 *
 */
public class ServiceListener implements DataTreeChangeListener<ServiceList> {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceListener.class);
    public static final InstanceIdentifier<ServiceList> SERVICES_II =
            InstanceIdentifier.create(ServiceList.class);
    /**
     * Implementation for the {@link ChangeHandler} stores a newly added {@link Services}.
     */
    private static final ChangeHandler CREATE_HANDLER = (services, connection) -> {
        try (PreparedStatement stmt = connection.prepareStatement(Queries.getQuery().serviceCreate().get())) {
            QueryUtils.setCreateServiceParameters(stmt, services);
            stmt.executeUpdate();
            stmt.clearParameters();
        } catch (SQLException e) {
            LOG.error("Could not insert service path {}", services);
        }
    };

    /**
     * Implementation for the {@link ChangeHandler} removes the deleted {@link Services}.
     */
    private static final ChangeHandler DELETE_HANDLER = (services, connection) -> {
        try (PreparedStatement stmt = connection.prepareStatement(Queries.getQuery().serviceDelete().get())) {
            stmt.setString(1, services.getServiceName());
            stmt.executeUpdate();
            stmt.clearParameters();
        } catch (SQLException e) {
            LOG.error("Could not delete service path {}", services);
            LOG.error(e.getMessage(), e);
        }
    };

    private final DataSource dataSource;

    /**
     * Default constructor invoked by bluprint injects all required dependencies.
     *
     * @param dataSource reference for {@link DataSource}
     */
    public ServiceListener(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<ServiceList>> changes) {
        LOG.debug("Service path list changed ...");
        List<DataTreeModification<ServiceList>> createItems =
                changes.stream().filter(ServiceListener::writeFilter).collect(Collectors.toList());
        List<DataTreeModification<ServiceList>> deleteItems =
                changes.stream().filter(ServiceListener::deleteFilter).collect(Collectors.toList());
        try (Connection connection = dataSource.getConnection()) {
            handleModification(createItems, CREATE_HANDLER, connection);
            handleModification(deleteItems, DELETE_HANDLER, connection);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Handles the change with {@link ChangeHandler}.
     *
     * @param changeList list of modifications
     * @param handleChange concrete implementation of {@link ChangeHandler}
     * @param connection {@link DataSource} we don't want to create each time a new connection
     */
    private void handleModification(List<DataTreeModification<ServiceList>> changeList, ChangeHandler handleChange,
            Connection connection) {
        for (DataTreeModification<ServiceList> change : changeList) {
            ServiceList serviceList = change.getRootNode().getDataAfter();
            for (Services service : serviceList.getServices()) {
                handleChange.handleChange(service, connection);
            }
        }
    }

    /**
     * Returns the filter for {@link ModificationType#WRITE} type change.
     *
     * @see #buildFilter(DataTreeModification, ModificationType)
     * @param modification write modification object
     * @return if the object was created
     */
    private static boolean writeFilter(DataTreeModification<ServiceList> modification) {
        return buildFilter(modification, ModificationType.WRITE);
    }

    /**
     * Returns the filter for {@link ModificationType#DELETE} type change.
     *
     * @see #deleteFilter(DataTreeModification)
     * @param modification delete modification
     * @return if the object was deleted
     */
    private static boolean deleteFilter(DataTreeModification<ServiceList> modification) {
        return buildFilter(modification, ModificationType.DELETE);
    }

    /**
     * Generalizes the creation of filter used in
     * {@link #onDataTreeChanged(Collection)} method.
     *
     * @param modification a modification
     * @param modificationType a modification type
     * @return boolean modification status
     */
    private static boolean buildFilter(DataTreeModification<ServiceList> modification,
            ModificationType modificationType) {
        return (modification.getRootNode().getDataAfter() != null
                && modificationType.equals(modification.getRootNode().getModificationType()));
    }

    /**
     * ChangeHandler interface is responsible for inserts or deletes in the DB based
     * on the implementation.
     *
     */
    private interface ChangeHandler {

        /**
         * Method will handle the changed {@link Services}.
         *
         * @param services changed object
         * @param connection {@link Connection} should be created in the caller method and
         *        not closed in the implementation
         */
        void handleChange(Services services, Connection connection);
    }

}
