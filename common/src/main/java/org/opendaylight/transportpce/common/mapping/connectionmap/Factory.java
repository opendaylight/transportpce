/*
 * Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping.connectionmap;

import org.opendaylight.mdsal.binding.api.DataBroker;
import org.slf4j.Logger;

public interface Factory {

    /**
     * Factory method creating a storage object.
     */
    Storage storage(DataBroker dataBroker, Logger log);

}
