/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.openroadminterfaces;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.loopback.rev161014.maint.loopback.MaintLoopbackBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.maintenance.testsignal.rev161014.maint.testsignal.MaintTestsignalBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.odu.interfaces.rev161014.odu.container.OduBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu.interfaces.rev161014.otu.container.OtuBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenRoadmInterfacesImpl implements OpenRoadmInterfaces {

    private static final Logger LOG = LoggerFactory.getLogger(OpenRoadmInterfacesImpl.class);

    // TODO move somewhere to constants
    public static final String NETWORK_TOKEN = "XPDR-LINE";
    public static final String TTP_TOKEN = "TTP";
    public static final String CLIENT_TOKEN = "XPDR-CLNT";
    public static final String PP_TOKEN = "PP";

    private final DeviceTransactionManager deviceTransactionManager;

    public OpenRoadmInterfacesImpl(DeviceTransactionManager deviceTransactionManager) {
        this.deviceTransactionManager = deviceTransactionManager;
    }

    @Override
    public void postInterface(String nodeId, InterfaceBuilder ifBuilder) throws OpenRoadmInterfaceException {
        Future<Optional<DeviceTransaction>> deviceTxFuture = this.deviceTransactionManager.getDeviceTransaction(nodeId);
        DeviceTransaction deviceTx;
        try {
            Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                        nodeId));
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                    nodeId), e);
        }

        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(ifBuilder.getName()));
        deviceTx.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, ifBuilder.build());
        ListenableFuture<Void> txSubmitFuture = deviceTx.submit(Timeouts.DEVICE_WRITE_TIMEOUT,
                Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
        try {
            txSubmitFuture.get();
            LOG.info("Successfully posted interface {} on node {}", ifBuilder.getName(), nodeId);
        } catch (InterruptedException | ExecutionException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to post interface %s on node %s!",
                    ifBuilder.getName(), nodeId), e);
        }
    }

    @Override
    public Optional<Interface> getInterface(String nodeId, String interfaceName) throws OpenRoadmInterfaceException {
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        return this.deviceTransactionManager.getDataFromDevice(nodeId, LogicalDatastoreType.CONFIGURATION,
                interfacesIID, Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
    }

    @Override
    public void deleteInterface(String nodeId, String interfaceName)
            throws OpenRoadmInterfaceException {
        Optional<Interface> intf2DeleteOpt;
        try {
            intf2DeleteOpt = getInterface(nodeId, interfaceName);
        } catch (OpenRoadmInterfaceException e) {
            throw new OpenRoadmInterfaceException(String.format("Failed to check if interface %s exists on node %s!",
                    interfaceName, nodeId), e);
        }
        if (intf2DeleteOpt.isPresent()) {
            Interface intf2Delete = intf2DeleteOpt.get();
            // State admin state to out of service
            InterfaceBuilder ifBuilder = new InterfaceBuilder(intf2Delete);
            if (ifBuilder.getType() == OtnOdu.class) {
                Interface1Builder oduBuilder = new Interface1Builder(intf2Delete.getAugmentation(Interface1.class));
                OduBuilder odu = new OduBuilder(oduBuilder.getOdu());
                if (odu.getMaintTestsignal() != null) {
                    MaintTestsignalBuilder maintSignalBuilder =
                            new MaintTestsignalBuilder();
                    maintSignalBuilder.setEnabled(false);
                    odu.setMaintTestsignal(maintSignalBuilder.build());
                }
                oduBuilder.setOdu(odu.build());
                ifBuilder.addAugmentation(Interface1.class, oduBuilder.build());
            } else if (ifBuilder.getType() == OtnOtu.class) {
                org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu
                    .interfaces.rev161014.Interface1Builder otuBuilder =
                    new org.opendaylight.yang.gen.v1.http.org.openroadm.otn.otu
                    .interfaces.rev161014.Interface1Builder(intf2Delete
                            .getAugmentation(org.opendaylight.yang.gen.v1
                                    .http.org.openroadm.otn.otu.interfaces.rev161014.Interface1.class));
                OtuBuilder otu = new OtuBuilder(otuBuilder.getOtu());
                if (otu.getMaintLoopback() != null) {
                    MaintLoopbackBuilder maintLoopBackBuilder =
                            new MaintLoopbackBuilder();
                    maintLoopBackBuilder.setEnabled(false);
                    otu.setMaintLoopback(maintLoopBackBuilder.build());
                }
                otuBuilder.setOtu(otu.build());
                ifBuilder.addAugmentation(org.opendaylight.yang.gen
                        .v1.http.org.openroadm.otn.otu.interfaces.rev161014.Interface1.class, otuBuilder.build());
            }
            ifBuilder.setAdministrativeState(AdminStates.OutOfService);
            // post interface with updated admin state
            try {
                postInterface(nodeId, ifBuilder);
            } catch (OpenRoadmInterfaceException ex) {
                throw new OpenRoadmInterfaceException(String.format("Failed to set state of interface %s to %s while"
                        + " deleting it!", interfaceName, AdminStates.OutOfService), ex);
            }

            InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                    .child(Interface.class, new InterfaceKey(interfaceName));
            Future<Optional<DeviceTransaction>> deviceTxFuture =
                this.deviceTransactionManager.getDeviceTransaction(nodeId);
            DeviceTransaction deviceTx;
            try {
                Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
                if (deviceTxOpt.isPresent()) {
                    deviceTx = deviceTxOpt.get();
                } else {
                    throw new OpenRoadmInterfaceException(String.format("Device transaction was not found for node %s!",
                            nodeId));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to obtain device transaction for node %s!",
                        nodeId), e);
            }

            deviceTx.delete(LogicalDatastoreType.CONFIGURATION, interfacesIID);
            ListenableFuture<Void> submit = deviceTx.submit(
                    Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);

            try {
                submit.get();
                LOG.info("Successfully deleted {} on node {}", interfaceName, nodeId);
            } catch (InterruptedException | ExecutionException e) {
                throw new OpenRoadmInterfaceException(String.format("Failed to delete interface %s on "
                        + "node %s", interfaceName, nodeId), e);
            }
        } else {
            LOG.info("Interface does not exist, cannot delete on node {}", nodeId);
        }
    }


}
