/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.impl;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev201130.GetNotificationsServiceInputBuilder;

public class NbiNotificationsImplTest extends AbstractTest {

    private ListeningExecutorService executorService;
    private CountDownLatch endSignal;
    private static final int NUM_THREADS = 5;
    private boolean callbackRan;

    @Before
    public void setUp() {
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUM_THREADS));
        endSignal = new CountDownLatch(1);
        callbackRan = false;
        MockitoAnnotations.openMocks(this);
    }

    @Test(expected = NullPointerException.class)
    public void getNotificationsServiceShouldBeFailedWithEmptyInput() {
        NbiNotificationsImpl nbiNotificationsImpl = new NbiNotificationsImpl();
        nbiNotificationsImpl.getNotificationsService(new GetNotificationsServiceInputBuilder().build());
    }

    //TODO : unit test getNotificationsServiceShouldBeSuccessful
    public void getNotificationsServiceShouldBeSuccessful() {
    }
}
