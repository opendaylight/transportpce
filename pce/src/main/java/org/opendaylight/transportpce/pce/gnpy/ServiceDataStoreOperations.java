/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ServiceDataStoreOperations {

    String createJsonStringFromDataObject(InstanceIdentifier<GnpyApi> id, GnpyApi object) throws GnpyException;

    void writeStringFile(String jsonString, String fileName) throws GnpyException;
}
