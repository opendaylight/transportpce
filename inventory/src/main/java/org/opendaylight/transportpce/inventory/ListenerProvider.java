/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.inventory.listener.ClliNetworkChangeListener;
import org.opendaylight.transportpce.inventory.listener.DeviceConfigListener;
import org.opendaylight.transportpce.inventory.listener.DeviceListener;
import org.opendaylight.transportpce.inventory.listener.OverlayNetworkChangeListener;
import org.opendaylight.transportpce.inventory.listener.UnderlayNetworkChangeListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.Registration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ListenerProvider} registers {@link DataTreeChangeListener} for each network layer.
 */
@Component
public class ListenerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerProvider.class);
    private List<Registration> listeners = new ArrayList<>();

    /**
     * Constructor invoked by blueprint injects all dependencies.
     *
     * @param dataBroker dataBroker
     * @param dataSource dataSource
     * @param deviceTransactionManager deviceTransactionManager
     */
    @Activate
    public ListenerProvider(@Reference DataBroker dataBroker,
            @Reference DataSource dataSource,
            @Reference DeviceTransactionManager deviceTransactionManager) {

        LOG.debug("Registering listeners...");
        OverlayNetworkChangeListener overlayNetworkListener = new OverlayNetworkChangeListener();
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(
                        LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OVERLAY_NETWORK_II.toLegacy()),
                overlayNetworkListener));
        LOG.info("Overlay network change listener was successfully registered");
        UnderlayNetworkChangeListener underlayNetworkListener = new UnderlayNetworkChangeListener();
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.UNDERLAY_NETWORK_II),
                underlayNetworkListener));
        LOG.info("Underlay network change listener was successfully registered");
        ClliNetworkChangeListener clliNetworkChangeListener = new ClliNetworkChangeListener();
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.CLLI_NETWORK_II),
                clliNetworkChangeListener));
        LOG.info("CLLI network change listener was successfully registered");
        INode121 inode121 = new INode121(dataSource, deviceTransactionManager);
        INode inode = new INode(dataSource, inode121);
        DeviceInventory deviceInventory = new DeviceInventory(dataSource, inode);
        DeviceListener deviceListener = new DeviceListener(deviceInventory);
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), deviceListener));
        LOG.info("Device change listener was successfully registered");
        DeviceConfigListener deviceConfigListener = new DeviceConfigListener(deviceInventory);
        listeners.add(dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), deviceConfigListener));
        LOG.info("Device config change listener was successfully registered");
    }

    @Deactivate
    public void close() {
        listeners.forEach(lis -> lis.close());
        listeners.clear();
    }
}
