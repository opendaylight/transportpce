/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.listener;

import java.util.List;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.tapisbi.service.TapiSbiDiscoveryService;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.TapiSbiControllers;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250530.ControllerBehaviourSettings;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.concepts.Registration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listener for TAPI SBI Controller configuration changes.
 * Detects when controllers are added or deleted and triggers appropriate actions.
 */
public class TapiSbiControllerListener implements DataTreeChangeListener<TapiSbiController> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiSbiControllerListener.class);

    private final DataBroker dataBroker;
    private final TapiSbiDiscoveryService discoveryService;
    private Registration listenerRegistration;

    public TapiSbiControllerListener(DataBroker dataBroker, TapiSbiDiscoveryService discoveryService) {
        this.dataBroker = dataBroker;
        this.discoveryService = discoveryService;
        LOG.info("TapiSbiControllerListener created");
    }

    /**
     * Registers this listener to monitor TAPI SBI controller configuration changes.
     */
    public void register() {
        DataObjectReference<TapiSbiController> controllersPath =
            DataObjectReference.builder(ControllerBehaviourSettings.class)
                .augmentation(org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                    .tapi.sbi.controller.rev260806.ControllerBehaviourSettings1.class)
                .child(TapiSbiControllers.class)
                .child(TapiSbiController.class)
                .build();

        DataTreeIdentifier<TapiSbiController> treeId =
            DataTreeIdentifier.of(LogicalDatastoreType.CONFIGURATION, controllersPath);

        listenerRegistration = dataBroker.registerTreeChangeListener(treeId, this);
        LOG.info("TapiSbiControllerListener registered for controller configuration changes");
    }

    /**
     * Unregisters this listener.
     */
    public void unregister() {
        if (listenerRegistration != null) {
            listenerRegistration.close();
            LOG.info("TapiSbiControllerListener unregistered");
        }
    }

    @Override
    public void onDataTreeChanged(List<DataTreeModification<TapiSbiController>> changes) {
        LOG.info("Received {} controller configuration change(s)", changes.size());

        for (DataTreeModification<TapiSbiController> change : changes) {
            DataObjectModification<TapiSbiController> rootNode = change.getRootNode();

            switch (rootNode.modificationType()) {
                case WRITE:
                    handleControllerWrite(rootNode);
                    break;
                case DELETE:
                    handleControllerDelete(rootNode);
                    break;
                default:
                    LOG.warn("Unexpected modification type: {}", rootNode.modificationType());
                    break;
            }
        }
    }

    /**
     * Handles new controller addition triggered by configure-tapi-sbi-controller RPC.
     * Retrieves the stored configuration (with decrypted password) and initiates discovery.
     */
    private void handleControllerWrite(DataObjectModification<TapiSbiController> modification) {
        TapiSbiController controller = modification.dataAfter();
        if (controller == null) {
            LOG.warn("Controller data after write is null");
            return;
        }
        LOG.info("New TAPI SBI controller added: {}, starting discovery", controller.getControllerId());
        try {
            discoveryService.discoverController(controller);
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.error("Failed to start discovery for controller: {}", controller.getControllerId(), e);
        }
    }

    /**
     * Handles controller deletion triggered by delete-tapi-sbi-controller RPC.
     * Cleans up topology and services associated with the removed controller.
     */
    private void handleControllerDelete(DataObjectModification<TapiSbiController> modification) {
        TapiSbiController controller = modification.dataBefore();
        if (controller == null) {
            LOG.warn("Controller data before delete is null");
            return;
        }
        LOG.info("TAPI SBI controller deleted: {}, starting cleanup", controller.getControllerId());
        try {
            discoveryService.cleanupController(controller.getControllerId());
            LOG.info("Cleanup completed for controller: {}", controller.getControllerId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            LOG.error("Failed to cleanup controller: {}", controller.getControllerId(), e);
        }
    }
}
