/*
 * Copyright © 2026 NTT and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.tapisbi.service;

import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.tapi.sbi.controller.rev260806.controller.behaviour.settings.tapi.sbi.controllers.TapiSbiController;

/**
 * Service interface for discovering TAPI SBI controllers.
 * Handles topology and context data discovery from southbound controllers.
 */
public interface TapiSbiDiscoveryService {

    /**
     * Discovers topology and TAPI common-context data from a newly added controller.
     *
     * @param controller the controller entry from the config datastore
     */
    void discoverController(TapiSbiController controller);

    /**
     * Cleans up topology and TAPI common-context data associated with a removed controller.
     *
     * @param controllerId Unique identifier of the controller to cleanup
     */
    void cleanupController(String controllerId);

    /**
     * Re-discovers a controller.
     *
     * @param controllerId Unique identifier of the controller to re-discover
     */
    void rediscoverController(String controllerId);
}
