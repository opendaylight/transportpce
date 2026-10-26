/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.transportpce.common.device.DeviceReadException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class PathDeleteFailureMessagesTest {

    private static final String NODE = "ROADM-A1";

    @Mock
    private Logger log;

    private PathDeleteFailureMessages failureMessages;

    @BeforeEach
    void setup() {
        failureMessages = new PathDeleteFailureMessages(log);
    }

    @Test
    void logNodeFailureDescribesANodeThatCannotBeRead() {
        DeviceReadException cause = new DeviceReadException("Timed out waiting for device " + NODE);

        String result = failureMessages.logNodeFailure(NODE, cause);

        // Not "nothing to delete": the node may still hold the service path
        assertEquals("Could not read node " + NODE + " to find what to delete: Timed out waiting for device " + NODE,
            result);
        verify(log).error(result, cause);
    }

    @Test
    void logNodeFailureDescribesAFailedDelete() {
        OpenRoadmInterfaceException cause = new OpenRoadmInterfaceException("device rejected the delete");

        String result = failureMessages.logNodeFailure(NODE, cause);

        assertEquals("Failed to delete service path on node " + NODE + ": device rejected the delete", result);
        verify(log).error(result, cause);
    }
}
