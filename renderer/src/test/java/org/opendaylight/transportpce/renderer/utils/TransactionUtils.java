/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.utils;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public final class TransactionUtils {

    private TransactionUtils() {

    }

    @SuppressWarnings({ "unchecked", "deprecation", "rawtypes" })
    // deviceTx.put needs the "true" boolean parameter at the end in order to not compromise the Junit test suite
    // FIXME check if the InstanceIdentifier raw type can be avoided
    // Raw types use are discouraged since they lack type safety.
    // Resulting Problems are observed at run time and not at compile time
    public static boolean writeTransaction(DeviceTransactionManager deviceTransactionManager,
                                    String nodeId,
                                    LogicalDatastoreType logicalDatastoreType,
                                    InstanceIdentifier instanceIdentifier,
                                    DataObject object)
            throws ExecutionException, InterruptedException {
        Future<Optional<DeviceTransaction>> deviceTxFuture =
                deviceTransactionManager.getDeviceTransaction(nodeId);
        if (!deviceTxFuture.get().isPresent()) {
            return false;
        }
        DeviceTransaction deviceTx = deviceTxFuture.get().get();
        deviceTx.merge(logicalDatastoreType, instanceIdentifier, object);
        deviceTx.commit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT).get();
        return true;
    }

    public static DataObject readTransaction(DeviceTransactionManager deviceTransactionManager,
                                  String nodeId,
                                  LogicalDatastoreType logicalDatastoreType,
                                  InstanceIdentifier<? extends DataObject> instanceIdentifier)
            throws ExecutionException, InterruptedException {
        Future<Optional<DeviceTransaction>> deviceTxFuture =
                deviceTransactionManager.getDeviceTransaction(nodeId);
        if (!deviceTxFuture.get().isPresent()) {
            return null;
        }
        DeviceTransaction deviceTx = deviceTxFuture.get().get();
        Optional<? extends DataObject> readOpt
                = deviceTx.read(logicalDatastoreType, instanceIdentifier).get();
        if (!readOpt.isPresent()) {
            return null;
        }
        return readOpt.get();
    }

}
