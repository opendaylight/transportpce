/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.Protected;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.Restorable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.Unprotected;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.UnprotectedDiverselyRouted;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.resiliency.ServiceResiliencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.resiliency.service.resiliency.CoupledServiceBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

@ExtendWith(MockitoExtension.class)
public class CheckCoherencyServiceResiliencyTest {

    @Test
    void testCheckWhenResiliencyNull() {
        ComplianceCheckResult result = ServicehandlerServiceResiliencyCheck
            .check(new ServiceResiliencyBuilder().setRevertive(true).build());

        assertFalse(result.hasPassed());
        assertEquals(ServicehandlerServiceResiliencyCheck.LOG_RESILIENCY_NULL, result.getMessage());
    }

    @Test
    void testCheckWhenUnprotectedResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(Unprotected.VALUE);

        assertFalse(ServicehandlerServiceResiliencyCheck.check(input.setRevertive(true).build()).hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setWaitToRestore(Uint64.valueOf(1)).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setHoldoffTime(Uint64.valueOf(1)).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setPreCalculatedBackupPathNumber(Uint8.valueOf(1)).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setCoupledService(new CoupledServiceBuilder().build()).build())
            .hasPassed());
    }

    @Test
    void testCheckWhenUnprotectedResiliencyWithCorrectAttributes() {
        assertTrue(ServicehandlerServiceResiliencyCheck
            .check(new ServiceResiliencyBuilder().setResiliency(Unprotected.VALUE).build())
            .hasPassed());
    }

    @Test
    void testCheckWhenUnprotectedDiverselyRoutedResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(UnprotectedDiverselyRouted.VALUE);

        assertFalse(ServicehandlerServiceResiliencyCheck.check(input.setRevertive(true).build()).hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setWaitToRestore(Uint64.valueOf(1)).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setHoldoffTime(Uint64.valueOf(1)).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setPreCalculatedBackupPathNumber(Uint8.valueOf(1)).build())
            .hasPassed());
    }

    @Test
    void testCheckWhenUnprotectedDiverselyRoutedResiliencyWithCorrectAttributes() {
        assertTrue(ServicehandlerServiceResiliencyCheck
            .check(new ServiceResiliencyBuilder()
                .setResiliency(UnprotectedDiverselyRouted.VALUE)
                .setCoupledService(new CoupledServiceBuilder().build())
                .build())
            .hasPassed());
    }

    @Test
    void testCheckWhenProtectedResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(Protected.VALUE);

        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setWaitToRestore(Uint64.valueOf(1)).setRevertive(false).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setPreCalculatedBackupPathNumber(Uint8.valueOf(1)).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setCoupledService(new CoupledServiceBuilder().build()).build())
            .hasPassed());
    }

    @Test
    void testCheckWhenProtectedResiliencyWithCorrectAttributes() {
        assertTrue(ServicehandlerServiceResiliencyCheck
            .check(new ServiceResiliencyBuilder()
                .setResiliency(Protected.VALUE)
                .setRevertive(true)
                .setWaitToRestore(Uint64.valueOf(1))
                .setHoldoffTime(Uint64.valueOf(1))
                .build())
            .hasPassed());
    }

    @Test
    void testCheckWhenRestorableOrExternalTriggerRestorableResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(Restorable.VALUE);

        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setWaitToRestore(Uint64.valueOf(1)).setRevertive(false).build())
            .hasPassed());
        assertFalse(ServicehandlerServiceResiliencyCheck
            .check(input.setCoupledService(new CoupledServiceBuilder().build()).build())
            .hasPassed());
    }

    @Test
    void testCheckWhenRestorableOrExternalTriggerRestorableResiliencyWithCorrectAttributes() {
        assertTrue(ServicehandlerServiceResiliencyCheck
            .check(new ServiceResiliencyBuilder()
                .setResiliency(Restorable.VALUE)
                .setRevertive(true)
                .setWaitToRestore(Uint64.valueOf(1))
                .setHoldoffTime(Uint64.valueOf(1))
                .setPreCalculatedBackupPathNumber(Uint8.valueOf(1))
                .build())
            .hasPassed());
    }
}
