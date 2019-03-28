/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.inventory.listener.ClliNetworkChangeListener;
import org.opendaylight.transportpce.inventory.listener.DeviceConfigListener;
import org.opendaylight.transportpce.inventory.listener.DeviceListener;
import org.opendaylight.transportpce.inventory.listener.OverlayNetworkChangeListener;
import org.opendaylight.transportpce.inventory.listener.UnderlayNetworkChangeListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ListenerProvider} registers {@link DataTreeChangeListener} for each network layer.
 */
public class ListenerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerProvider.class);

    private final DataBroker dataBroker;
    private final OverlayNetworkChangeListener overlayNetworkListener;
    private final UnderlayNetworkChangeListener underlayNetworkListener;
    private final ClliNetworkChangeListener clliNetworkChangeListener;
    private final DeviceListener deviceListener;
    private final DeviceConfigListener deviceConfigListener;

    /**
     * Constructor invoked by blueprint injects all dependencies.
     *
     * @param dataBroker dataBroker
     * @param overlayNetworkListener  overlay-network Listener
     * @param underlayNetworkListener underlay-network Listener
     * @param clliNetworkChangeListener clli Network change Listener
     * @param deviceListener device listener
     * @param deviceConfigListener device config listener
     */
    public ListenerProvider(DataBroker dataBroker, OverlayNetworkChangeListener overlayNetworkListener,
        UnderlayNetworkChangeListener underlayNetworkListener, ClliNetworkChangeListener clliNetworkChangeListener,
        DeviceListener deviceListener, DeviceConfigListener deviceConfigListener) {

        this.dataBroker = dataBroker;
        this.overlayNetworkListener = overlayNetworkListener;
        this.underlayNetworkListener = underlayNetworkListener;
        this.clliNetworkChangeListener = clliNetworkChangeListener;
        this.deviceListener = deviceListener;
        this.deviceConfigListener = deviceConfigListener;
    }

    /**
     * Invoked by blueprint, registers the listeners.
     */
    public void initialize() {
        LOG.debug("Registering listeners...");
        dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OVERLAY_NETWORK_II),
                overlayNetworkListener);
        LOG.info("Overlay network change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.UNDERLAY_NETWORK_II),
                underlayNetworkListener);
        LOG.info("Underlay network change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.CLLI_NETWORK_II),
                clliNetworkChangeListener);
        LOG.info("CLLI network change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), deviceListener);
        LOG.info("Device change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                DataTreeIdentifier.create(LogicalDatastoreType.CONFIGURATION,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), deviceConfigListener);
        LOG.info("Device config change listener was successfully registered");
    }

}
