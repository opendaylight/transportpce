/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;

@Ignore
public class ServiceDataStoreOperationsImplTest extends AbstractTest {

    ServiceDataStoreOperationsImpl serviceDataStoreOperations;
    NetworkTransactionImpl networkTransaction = Mockito.mock(NetworkTransactionImpl.class);
    OrgOpenroadmDevice orgOpenroadmDevice = Mockito.mock(OrgOpenroadmDevice.class);

    @Before
    public void setUp() {
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(networkTransaction);
    }

    // TODO: fix augmentation issue
    @Test
    public void createXMLFromDeviceTest() throws GnpyException {
        serviceDataStoreOperations.createXMLFromDevice(this.getDataStoreContextUtil(),
                orgOpenroadmDevice, "some-output");
    }

    @Test
    public void writeStringFile() throws GnpyException {
        serviceDataStoreOperations.writeStringFile("filename","data");
    }
}
