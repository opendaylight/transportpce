/*
 * Copyright © 2022 Fujitsu Network Communications, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.OpenroadmOperationalModes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.SpecificOperationalModes;

/**
 * Store operational mode catalog into MDSAL .
 */
public interface CatalogDataStoreOperations {

    /**
     * Store OpenroadmOperationalModes object in the config data store.
     */
    void addOpenroadmOperationalModesToCatalog(OpenroadmOperationalModes objToSave);

    /**
     * Store SpecificOperationalModes object in the config data store.
     */
    void addSpecificOperationalModesToCatalog(SpecificOperationalModes objToSave);

}
