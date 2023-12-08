/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.transportpce.common.mapping.connectionmap.storage.Storage;

public interface Factory {

    /**
     * Factory method creating a storage object.
     */
    Storage storage(DataBroker dataBroker);

}
