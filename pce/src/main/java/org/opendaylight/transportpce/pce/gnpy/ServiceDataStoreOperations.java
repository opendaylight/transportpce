/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import org.opendaylight.transportpce.common.DataStoreContext;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev190103.GnpyApi;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public interface ServiceDataStoreOperations {

    void createXMLFromDevice(DataStoreContext dataStoreContextUtil, OrgOpenroadmDevice device, String output)
        throws GnpyException;

    String createJsonStringFromDataObject(InstanceIdentifier<GnpyApi> id, GnpyApi object) throws GnpyException;

    void writeStringFile(String jsonString, String fileName) throws GnpyException;
}
