/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.device;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.opendaylight.mdsal.binding.api.MountPoint;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.openroadminterfaces.message.Message;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

/**
 * <p>
 *     Device transaction manager manages access to netconf devices. Only one transaction can be opened per device so
 *     it IS IMPORTANT TO CLOSE TRANSACTION as soon as transactions is not needed.
 * </p>
 *
 * <p>
 *     Most important method is {@link DeviceTransactionManager#getDeviceTransaction(String)}. This method let's you
 *     obtain {@link DeviceTransaction} on the device. {@link DeviceTransaction} provices methods to read/write data
 *     from/to device.
 * </p>
 *
 * <p>
 *     Method
 *  {@link DeviceTransactionManager#getDataFromDevice(String, LogicalDatastoreType, InstanceIdentifier, long, TimeUnit)}
 *     is 'shortcut' to get data from device. It creates {@link DeviceTransaction}, gets data via it and then closes
 *     the transaction.
 * </p>
 *
 * <p>
 *     Two timeouts are built in process to prevent locking device forever:
 * </p>
 * <ul>
 *     <li>
 *     First is from creation of {@link DeviceTransaction} to calling method to close it (commit or cancel). When using
 *     {@link DeviceTransactionManager#getDeviceTransaction(String)} method then default timeout will be used. If there
 *     is need to specify this timeout manually use
 *     {@link DeviceTransactionManager#getDeviceTransaction(String, long, TimeUnit)} method. So if programmer will
 *     forgot to close transaction or it will take too much time transaction will be cancelled automatically and device
 *     will be unlocked.
 *     </li>
 *
 *     <li>
 *     Second timeout is from calling {@link DeviceTransaction#commit(long, TimeUnit)} until commit is completed on
 *     device. Timeout can be specified directly using commit method. So in case commit will freeze somewhere on device
 *     or it will take too much time device will be unlocked.
 *     </li>
 * </ul>
 *
 * <p>
 *     If there is only need to read from device
 *  {@link DeviceTransactionManager#getDataFromDevice(String, LogicalDatastoreType, InstanceIdentifier, long, TimeUnit)}
 *     method can be used. It will automatically take care of {@link DeviceTransaction} and it will return data.
 *     This method <b>SHOULD NOT BE USED TOGETHER WITH DEVICE TRANSACTION ON THE SAME DEVICE IN THE SAME TIME</b>.
 *     In case that {@link DeviceTransaction} is created on device and before committing it
 *  {@link DeviceTransactionManager#getDataFromDevice(String, LogicalDatastoreType, InstanceIdentifier, long, TimeUnit)}
 *     method is called then get method will wait (will be blocking current thread) until device will be unlocked.
 *     However device is locked by transaction previously created. So this will result in blocking current thread until
 *     timeout for commit transaction will run out and cancel transaction. This can lead to incorrect execution of code.
 * </p>
 *
 * <p>
 * Bellow is simple example how to get {@link DeviceTransaction}, put some data to it and then commit it.
 * </p>
 * <pre>
 * {@code
 *     // get device transaction future from device transaction manager
 *     Future<Optional<DeviceTransaction>> deviceTxFuture = deviceTransactionManager.getDeviceTransaction(deviceId);
 *     DeviceTransaction deviceTx;
 *     try {
 *         // wait until device transaction is available
 *         Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
 *
 *         // check if device transaction is present
 *         if (deviceTxOpt.isPresent()) {
 *             deviceTx = deviceTxOpt.get();
 *         } else {
 *             throw new IllegalStateException("Device transaction for device " + deviceId + " was not found!");
 *         }
 *     } catch (InterruptedException | ExecutionException e) {
 *         throw new IllegalStateException("Unable to obtain device transaction for device " + deviceId + "!", e);
 *     }
 *
 *     // do some operations with transaction
 *     deviceTx.put(LogicalDatastoreType.CONFIGURATION, someInstanceIdentifier, someData);
 *     deviceTx.delete(LogicalDatastoreType.CONFIGURATION, someOtherInstanceIdentifier, someOtherData);
 *
 *     // commit transaction with 5 seconds timeout
 *     FluentFuture<? extends @NonNull CommitInfo> commit = deviceTx.commit(5, TimeUnit.SECONDS);
 *     try {
 *         // wait until transaction is committed
 *         commit.get();
 *     } catch (InterruptedException | ExecutionException e) {
 *         throw new IllegalStateException("Failed to post data to device " + deviceId + "!", e);
 *     }
 * }
 * </pre>
 */
public interface DeviceTransactionManager {

    /**
     * Gets Future containing {@link DeviceTransaction}. Since only one transaction can be opened per device future will
     * return transaction when all previously committed transaction on device are closed. This method will use default
     * timeout for commit transaction.
     *
     * @param deviceId device identifier on which will be transaction created.
     * @return Future returning Optional of DeviceTransaction. Optional will be empty if device with specified ID
     *         does not exists or transaction will fail to obtain.
     */
    Future<Optional<DeviceTransaction>> getDeviceTransaction(String deviceId);

    /**
    * Works same as {@link DeviceTransactionManager#getDeviceTransaction(String)} but with option to set custom timeout.
     *
     * @param deviceId device id on which will be transaction created.
     * @param timeoutToSubmit timeout will start running when transaction is created. If transaction will not be
     *                        closed (committed or cancelled) when times runs out it will be canceled (so device will
     *                        be unlocked).
     * @param timeUnit time units for timeout.
     * @return Future returning Optional of DeviceTransaction. Optional will be empty if device with specified ID
     *         does not exists or transaction will fail to obtain.
     */
    Future<Optional<DeviceTransaction>> getDeviceTransaction(String deviceId, long timeoutToSubmit, TimeUnit timeUnit);

    // TODO make private in impl
    Optional<MountPoint> getDeviceMountPoint(String deviceId);

    /**
     * Returns data from device from specified path. Creates new device transaction, gets data via it and closes
     * transaction.
     *
     * <p>
     * This method is blocking - it's waiting until it receives {@link DeviceTransaction} and then the data from device.
     * </p>
     *
     * @param deviceId Device identifier from which will be data read.
     * @param logicalDatastoreType Datastore type.
     * @param path Path to data in device's datastore.
     * @param timeout Timeout to automatically close transaction AND to get data from device (sets both timeouts to
     *                same value).
     * @param timeUnit Time unit of timeout.
     * @param <T> Type of data to be returned.
     * @return Optional of data obtained from device. If device does not contain data or device does not exists then
     *         empty Optional will be returned.
     */
    <T extends DataObject> Optional<T> getDataFromDevice(String deviceId, LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<T> path, long timeout, TimeUnit timeUnit);

    /**
     * Returns data from device from specified path. Creates new device transaction, gets data via it and closes
     * transaction.
     * Adds a subscriber to
     * {@link #getDataFromDevice(String, LogicalDatastoreType, InstanceIdentifier, long, TimeUnit)}
     */
    <T extends DataObject> Optional<T> getDataFromDevice(
            String deviceId,
            LogicalDatastoreType logicalDatastoreType,
            InstanceIdentifier<T> path,
            long timeout,
            TimeUnit timeUnit,
            Subscriber subscriber,
            Message errorMessage
    );

    /**
     * Checks if device with specified ID is mounted.
     *
     * @param deviceId Identifier of device to check.
     * @return True if device is mounted.
     */
    boolean isDeviceMounted(String deviceId);
}
