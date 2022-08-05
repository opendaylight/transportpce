/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler.validation.checks;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.Protected;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.Restorable;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.Unprotected;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.UnprotectedDiverselyRouted;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.resiliency.ServiceResiliencyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev211210.service.resiliency.service.resiliency.CoupledServiceBuilder;
import org.opendaylight.yangtools.yang.common.Uint64;
import org.opendaylight.yangtools.yang.common.Uint8;

@RunWith(MockitoJUnitRunner.class)
public class CheckCoherencyServiceResiliencyTest {

    @Test
    public void testCheckWhenResiliencyNull() {
        ComplianceCheckResult result = ServicehandlerServiceResiliencyCheck.check(
                new ServiceResiliencyBuilder().setRevertive(true).build());

        Assert.assertFalse(result.hasPassed());
        Assert.assertEquals(ServicehandlerServiceResiliencyCheck.LOG_RESILIENCY_NULL, result.getMessage());
    }

    @Test
    public void testCheckWhenUnprotectedResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(Unprotected.VALUE);

        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(input.setRevertive(true).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setWaitToRestore(Uint64.valueOf(1)).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setHoldoffTime(Uint64.valueOf(1)).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setPreCalculatedBackupPathNumber(Uint8.valueOf(1)).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setCoupledService(new CoupledServiceBuilder().build()).build()).hasPassed());
    }

    @Test
    public void testCheckWhenUnprotectedResiliencyWithCorrectAttributes() {
        Assert.assertTrue(ServicehandlerServiceResiliencyCheck.check(
                new ServiceResiliencyBuilder()
                        .setResiliency(Unprotected.VALUE)
                        .build())
                .hasPassed());
    }

    @Test
    public void testCheckWhenUnprotectedDiverselyRoutedResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(UnprotectedDiverselyRouted.VALUE);

        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(input.setRevertive(true).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setWaitToRestore(Uint64.valueOf(1)).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setHoldoffTime(Uint64.valueOf(1)).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setPreCalculatedBackupPathNumber(Uint8.valueOf(1)).build()).hasPassed());
    }

    @Test
    public void testCheckWhenUnprotectedDiverselyRoutedResiliencyWithCorrectAttributes() {
        Assert.assertTrue(ServicehandlerServiceResiliencyCheck.check(
                new ServiceResiliencyBuilder().setResiliency(UnprotectedDiverselyRouted.VALUE)
                        .setCoupledService(new CoupledServiceBuilder().build()).build()).hasPassed());
    }

    @Test
    public void testCheckWhenProtectedResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(Protected.VALUE);

        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setWaitToRestore(Uint64.valueOf(1)).setRevertive(false).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setPreCalculatedBackupPathNumber(Uint8.valueOf(1)).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setCoupledService(new CoupledServiceBuilder().build()).build()).hasPassed());
    }

    @Test
    public void testCheckWhenProtectedResiliencyWithCorrectAttributes() {
        Assert.assertTrue(ServicehandlerServiceResiliencyCheck.check(
                new ServiceResiliencyBuilder()
                        .setResiliency(Protected.VALUE)
                        .setRevertive(true)
                        .setWaitToRestore(Uint64.valueOf(1))
                        .setHoldoffTime(Uint64.valueOf(1))
                        .build())
                .hasPassed());
    }

    @Test
    public void testCheckWhenRestorableOrExternalTriggerRestorableResiliencyWithWrongAttributes() {
        ServiceResiliencyBuilder input = new ServiceResiliencyBuilder().setResiliency(Restorable.VALUE);

        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setWaitToRestore(Uint64.valueOf(1)).setRevertive(false).build()).hasPassed());
        Assert.assertFalse(ServicehandlerServiceResiliencyCheck.check(
                input.setCoupledService(new CoupledServiceBuilder().build()).build()).hasPassed());
    }

    @Test
    public void testCheckWhenRestorableOrExternalTriggerRestorableResiliencyWithCorrectAttributes() {
        Assert.assertTrue(ServicehandlerServiceResiliencyCheck.check(
                new ServiceResiliencyBuilder()
                        .setResiliency(Restorable.VALUE)
                        .setRevertive(true)
                        .setWaitToRestore(Uint64.valueOf(1))
                        .setHoldoffTime(Uint64.valueOf(1))
                        .setPreCalculatedBackupPathNumber(Uint8.valueOf(1))
                        .build())
                .hasPassed());
    }
}
