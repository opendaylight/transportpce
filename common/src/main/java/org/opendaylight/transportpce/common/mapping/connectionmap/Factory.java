/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;

/**
 * Creates {@link Storage} instances for saving and reading OpenROADM connection maps.
 */
public interface Factory {

    /**
     * Creates a storage instance bound to the given data broker.
     *
     * @param dataBroker broker used to read and write connection maps to the datastore
     * @return a storage instance ready to save and read connection maps
     */
    Storage storage(DataBroker dataBroker);

}
