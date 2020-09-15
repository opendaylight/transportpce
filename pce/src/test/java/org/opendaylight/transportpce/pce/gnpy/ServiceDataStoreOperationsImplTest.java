/*
 * Copyright © 2020 Orange Labs, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opendaylight.mdsal.binding.dom.codec.spi.BindingDOMCodecServices;
import org.opendaylight.transportpce.test.AbstractTest;

public class ServiceDataStoreOperationsImplTest extends AbstractTest {

    private ServiceDataStoreOperationsImpl serviceDataStoreOperations;
    private BindingDOMCodecServices bindingDOMCodecServices = Mockito.mock(BindingDOMCodecServices.class);

    @Before
    public void setUp() throws GnpyException {
        serviceDataStoreOperations = new ServiceDataStoreOperationsImpl(bindingDOMCodecServices);
    }

    @Test
    public void writeStringFile() throws GnpyException {
        serviceDataStoreOperations.writeStringFile("filename","data");
    }
}
