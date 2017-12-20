/*
 * Copyright © 2017 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.inventory;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.inventory.listener.ClliNetworkChangeListener;
import org.opendaylight.transportpce.inventory.listener.DeviceListener;
import org.opendaylight.transportpce.inventory.listener.OverlayNetworkChangeListener;
import org.opendaylight.transportpce.inventory.listener.ServiceListener;
import org.opendaylight.transportpce.inventory.listener.UnderlayNetworkChangeListener;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ListenerProvider} registers {@link DataTreeChangeListener} for each
 * network layer.
 *
 */
public class ListenerProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ListenerProvider.class);

    private final DataBroker dataBroker;
    private final OverlayNetworkChangeListener overlayNetworkListener;
    private final UnderlayNetworkChangeListener underlayNetworkListener;
    private final ClliNetworkChangeListener clliNetworkChangeListener;
    private final ServiceListener serviceListener;
    private final DeviceListener deviceListener;

    /**
     * Constructor invoked by blueprint injects all dependencies.
     *
     * @param dataBroker the databroker
     * @param overlayNetworkListener the overlay network listener
     * @param underlayNetworkListener the underlay network listener
     * @param clliNetworkChangeListener the CLLI network changes listener
     * @param serviceListener the service listener
     * @param deviceListener the device listener
     */
    public ListenerProvider(DataBroker dataBroker, OverlayNetworkChangeListener overlayNetworkListener,
            UnderlayNetworkChangeListener underlayNetworkListener, ClliNetworkChangeListener clliNetworkChangeListener,
            ServiceListener serviceListener, DeviceListener deviceListener) {
        this.dataBroker = dataBroker;
        this.overlayNetworkListener = overlayNetworkListener;
        this.underlayNetworkListener = underlayNetworkListener;
        this.clliNetworkChangeListener = clliNetworkChangeListener;
        this.serviceListener = serviceListener;
        this.deviceListener = deviceListener;
    }

    /**
     * Invoked by blueprint, registers the listeners.
     */
    public void initialize() {
        LOG.debug("Registering listeners...");
        dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.OVERLAY_NETWORK_II),
                overlayNetworkListener);
        LOG.info("Overlay network change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.UNDERLAY_NETWORK_II),
                underlayNetworkListener);
        LOG.info("Underlay network change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, InstanceIdentifiers.CLLI_NETWORK_II),
                clliNetworkChangeListener);
        LOG.info("CLLI network change listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.CONFIGURATION, ServiceListener.SERVICES_II),
                serviceListener);
        LOG.info("Service path listener was successfully registered");
        dataBroker.registerDataTreeChangeListener(new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
                InstanceIdentifiers.NETCONF_TOPOLOGY_II.child(Node.class)), deviceListener);
        LOG.info("Device change listener was successfully registered");
    }

}
