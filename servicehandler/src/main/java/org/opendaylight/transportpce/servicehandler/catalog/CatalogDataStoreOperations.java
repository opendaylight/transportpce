/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler.catalog;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.catalog.OpenroadmOperationalModes;

/**
 * Store operational mode catalog into MDSAL .
 */
public interface CatalogDataStoreOperations {
    void addOpenroadmOperationalModesToCatalog(OpenroadmOperationalModes objToSave, DataBroker db);

    void addSpecificOperationalModesToCatalog(org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog
                                                      .rev211210.operational.mode.catalog.SpecificOperationalModes
                                                      objToSave, DataBroker db);

}
