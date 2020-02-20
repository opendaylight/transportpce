/*
 * Copyright (c) 2018 Orange and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.device.write;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.binding.api.WriteTransaction;
import org.opendaylight.mdsal.common.api.CommitInfo;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDeviceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.OduConnectionKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.InterfacesBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.PathArgument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FluentFuture;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
final class DeviceChangeListener implements DataTreeChangeListener<OrgOpenroadmDevice> {

    private static final Logger LOG = LoggerFactory.getLogger(DeviceChangeListener.class);
    private final DataBroker dataBroker;

    public DeviceChangeListener(DataBroker deviceDataBroker) {
        this.dataBroker = deviceDataBroker;
        Preconditions.checkArgument(this.dataBroker != null, "Device datastore is null");
    }

    @Override
    public void onDataTreeChanged(Collection<DataTreeModification<OrgOpenroadmDevice>> changes) {
        LOG.info("onDataTreeChanged");
        for (DataTreeModification<OrgOpenroadmDevice> change : changes) {
            final DataObjectModification<OrgOpenroadmDevice> rootNode = change.getRootNode();
            final DataTreeIdentifier<OrgOpenroadmDevice> rootPath = change.getRootPath();
            if (rootNode != null ) {
                final OrgOpenroadmDevice dataBefore = rootNode.getDataBefore();
                final OrgOpenroadmDevice dataAfter = rootNode.getDataAfter();
                LOG.info("Received Device change({}):\n before={} \n after={}", rootNode.getModificationType(), dataBefore,
                        dataAfter);
                Collection<? extends DataObjectModification<? extends DataObject>> modifiedChildren = rootNode.getModifiedChildren();
                switch (rootNode.getModificationType()) {
                    case SUBTREE_MODIFIED:
                        if (!modifiedChildren.isEmpty()) {
                            Iterator<? extends DataObjectModification<? extends DataObject>> iterator = modifiedChildren.iterator();
                            while (iterator.hasNext()) {
                                DataObjectModification<? extends DataObject> modified = iterator.next();
                                LOG.info("modified = \ndataType : {}\nid : {}\nmodifiedType : {}\noldData : {}\nnewData : {} \n",
                                        modified.getDataType(), modified.getIdentifier(),modified.getModificationType(),
                                        modified.getDataBefore(), modified.getDataAfter());
                                switch (modified.getModificationType()) {
                                  case SUBTREE_MODIFIED:
                                  case WRITE :
                                      processChange(rootPath.getRootIdentifier(), modified, dataAfter);
                                      updateCircuitPackInterface(rootPath.getRootIdentifier(), modified, false);
                                      break;
                                  case DELETE:
                                      updateCircuitPackInterface(rootPath.getRootIdentifier(), modified, true);
                                      deleteContainer(rootPath, modified);
                                      break;
                                  default:
                                      break;
                               }
                            }
                        }
                        //processChange(rootPath.getRootIdentifier(), dataAfter);
                        break;
                    case WRITE :
                        processChange(rootPath.getRootIdentifier(), null, dataAfter);
                        break;
                    case DELETE:
                        LOG.info("device config datastore is deleted !");
                        break;
                    default:
                        break;
                }
            } else {
                LOG.error("rootNode is null !");
            }
        }
    }

    /**
     * Delete change from device
     * oper datastore.
     *
     * @param id container identifier
     */
    private void deleteContainer(DataTreeIdentifier<OrgOpenroadmDevice> rootPath,
            DataObjectModification<? extends DataObject> modified) {
        final String ROADM_CONNECTIONS = "interface org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019"
                + ".org.openroadm.device.container.org.openroadm.device.RoadmConnections";
        final String INTERFACE_GRP = "interface org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019"
                + ".interfaces.grp.Interface";
        final String ODU_CONNECTIONS = "interface org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019"
                + ".org.openroadm.device.container.org.openroadm.device.OduConnection";
        Class<? extends DataObject> type = modified.getDataType();
        PathArgument path = modified.getIdentifier();
        LOG.info("deleting container type '{}' with id '{}' ...", type.toString(), path);
        String key = extractKey(path.toString());
        if ( key != null) {
            InstanceIdentifier<?> iid = null;
            switch (type.toString()) {
                case ROADM_CONNECTIONS:
                    LOG.info("roadm-connections ...");
                    iid = rootPath.getRootIdentifier().child(RoadmConnections.class,
                        new RoadmConnectionsKey(key));
                    break;
                case INTERFACE_GRP:
                    LOG.info("interface ....");
                    iid = rootPath.getRootIdentifier().child(Interface.class,
                            new InterfaceKey(key));
                    break;
                case ODU_CONNECTIONS:
                    LOG.info("Deleting odu connections");
                    iid = rootPath.getRootIdentifier().child(OduConnection.class,
                            new OduConnectionKey(key));
                    break;
                default:
                    break;
            }
            LOG.info("iid : {}", iid);
            WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
            if (writeTx != null) {
                LOG.info("WriteTransaction is ok, delete container device from device oper datastore");
                try {
                    LOG.info("deleting container element from device oper DS ...");
                    writeTx.delete(LogicalDatastoreType.OPERATIONAL, iid);
                    FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                    future.get();
                    LOG.info("container element '{}' deleted from device oper datastore", iid);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to process WriteTransactions",e);
                }
            } else {
                LOG.error("WriteTransaction object is null");
            }
        } else {
            LOG.error("extract key is null");
        }
    }



    private String extractKey(String path) {
        LOG.info("getting key from pathArgument ...");
        String result = null;
        if (path != null && path.length() > 2) {
            result = path.substring(path.lastIndexOf("=") + 1, path.length()-2);
            LOG.info("result : {}", result);
        } else {
            LOG.error("String pathArgument is not compliant !!");
        }
        return result;
    }


    /**
     * Merge change to Honeycomb
     * config datastore and device
     * config datastore.
     *
     * @param id OrgOpenroadmDevice identifier
     * @param dataAfter OrgOpenroadmDevice to be merged
     */
    private void processChange(final InstanceIdentifier<OrgOpenroadmDevice> id,
            DataObjectModification<? extends DataObject> modified, final OrgOpenroadmDevice dataAfter) {
        LOG.info("processing change ...");
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        if (writeTx != null) {
            LOG.info("WriteTransactions are ok, merge device info to datastores");
            if(dataAfter != null) {
                String deviceId = dataAfter.getInfo().getNodeId().getValue();
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, id, dataAfter);
                FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                try {
                    future.get();
                    LOG.info("device '{}' merged to device oper datastore", deviceId);
                } catch (InterruptedException | ExecutionException e) {
                    LOG.error("Failed to merge Element '{}' to datastores", deviceId);
                }
            } else {
                LOG.error("device is null");
            }
        } else {
            LOG.error("WriteTransaction object is null");
        }
    }

    /**
     *Update Interface info on
     *Ports list in CircuitPacks.
     *
     * @param id device InstanceIdentifier
     * @param modified DataObjectModification
     * @param deviceData OrgOpenroadmDevice
     * @param delete if yes delete interface from circuitpacks else adding interface
     */
    private void updateCircuitPackInterface(final InstanceIdentifier<OrgOpenroadmDevice> id,
            DataObjectModification<? extends DataObject> modified, boolean delete) {
        LOG.info("Updating circuit packs interface ...");
        final String INTERFACE_GRP = "interface org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019"
                + ".interfaces.grp.Interface";
        InstanceIdentifier<CircuitPacks> iid = null;
        if (modified != null) {
            Class<? extends DataObject> type = modified.getDataType();
            LOG.info("getting container type '{}' ...", type.toString());
            if (type != null) {
                if (type.toString().compareTo(INTERFACE_GRP) == 0) {
                    LOG.warn("interface update ! ");
                    Interface data = null;
                    if (delete) {
                        data = (Interface) modified.getDataBefore();
                    } else {
                        data = (Interface) modified.getDataAfter();
                    }
                    LOG.info("Interface data gets : {}", data.toString());
                    if (data!= null) {
                        String circuitPackName = data.getSupportingCircuitPackName();
                        String port = data.getSupportingPort().toString();
                        String interfaceName = data.getName();
                        if (circuitPackName != null && port != null && interfaceName != null) {
                            iid = id.child(CircuitPacks.class,
                                    new CircuitPacksKey(circuitPackName));
                            OrgOpenroadmDevice operDevice = readDeviceOperData(id);
                            if (operDevice != null) {
                                OrgOpenroadmDeviceBuilder builder = new OrgOpenroadmDeviceBuilder(operDevice);
                                List<CircuitPacks> list = builder.getCircuitPacks();
                                LOG.info("Getting circuit packs ...");
                                for (CircuitPacks circuitPacks : list) {
                                    if (circuitPacks.getCircuitPackName().compareTo(circuitPackName) == 0) {
                                        LOG.info("circuitpack found");
                                        List<Ports> portList = circuitPacks.getPorts();
                                        LOG.info("Getting ports for circuit pack '{}' ...", circuitPackName);
                                        for (Ports ports : portList) {
                                            if (ports.getPortName().compareTo(port) == 0) {
                                                LOG.info("port found");
                                                PortsBuilder newPorts = new PortsBuilder(ports);
                                                List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                                                .port.Interfaces> value = newPorts.getInterfaces();
                                                if (!delete) {
                                                    LOG.info("adding interface info to port '{}'", port);
                                                    if (value == null || value.isEmpty()) {
                                                        LOG.info("Interfaces List is empty !");
                                                        value = new ArrayList<org.opendaylight.yang.gen.v1.http.org
                                                                .openroadm.device.rev181019.port.Interfaces>();
                                                        value.add(new InterfacesBuilder().setInterfaceName(interfaceName)
                                                                .build());
                                                    } else { // value is not empty
                                                        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                                                        .port.Interfaces tmp = getInterfaces(value, interfaceName);
                                                        if( tmp != null) {
                                                            LOG.warn("Interfaces with name '{}' already exists !",interfaceName);
                                                            break;
                                                        } else { // no interface found
                                                            value.add(new InterfacesBuilder().setInterfaceName(interfaceName)
                                                                    .build());
                                                        }
                                                    }
                                                } else {
                                                    LOG.info("removing interface info from port '{}'", port);
                                                    org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                                                    .port.Interfaces tmp = getInterfaces(value, interfaceName);
                                                    if( tmp != null) {
                                                        LOG.info("Interfaces with name '{}' gets",interfaceName);
                                                        value.remove(tmp);
                                                    }else {
                                                        LOG.error("Interfaces list is null");
                                                        value = new ArrayList<org.opendaylight.yang.gen.v1.http.org.openroadm
                                                                .device.rev181019.port.Interfaces>();
                                                    }
                                                }
                                                newPorts.setInterfaces(value);
                                                portList.remove(ports);
                                                portList.add(newPorts.build());
                                                LOG.info("port list updated !");
                                                update(circuitPacks, iid);
                                                break;
                                            }
                                        }
                                        break;
                                    }
                                }
                            } else {
                                LOG.error("reading operational device data failed !");
                            }

                        } else {
                            LOG.error("SupportingCircuitPackName / SupportingPort / Interface Name are/is null !");
                        }
                    }
                } else {
                    LOG.warn("not an interface update ! ");
                }
            }
        } else {
            LOG.error("DataObjectModification is null");
        }
    }

    /**
     * Search in {@link Interfaces} list
     * if an interfaces already exists.
     *
     * @param value Interfaces List
     * @param interfaceName Interface Name
     * @return Interfaces
     */
    private org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.Interfaces getInterfaces(
            List<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.Interfaces> value,
            String interfaceName) {
        org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.port.Interfaces result = null;
        if (value != null && !value.isEmpty()) {
            LOG.info("Getting interfaces list ...");
            for (org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                    .port.Interfaces tmp : value) {
                if( tmp != null
                        && tmp.getInterfaceName().compareTo(interfaceName) == 0) {
                    LOG.info("Interfaces with name '{}' gets",interfaceName);
                    result = tmp;
                    break;
                }
            }
        } else {
            LOG.error("Interfaces list is empty");
        }
        return result;
    }

    /**
     * Update circuitPacks information
     * in device oper datastore.
     *
     * @param circuitPacks CircuitPacks data
     * @param iid CircuitPacks InstanceIdentifier
     * @param id OrgOpenroadmDevice InstanceIdentifier
     * @param deviceData OrgOpenroadmDevice data
     */
    private void update(CircuitPacks circuitPacks, final InstanceIdentifier<CircuitPacks> iid) {
        LOG.info("updating oper Datastore with iid : {}", iid);
        WriteTransaction writeTx = this.dataBroker.newWriteOnlyTransaction();
        if (writeTx != null) {
            LOG.info("WriteTransaction is ok, update container circuitpacks from device oper datastore");
            try {
                LOG.info("updating container circuitpacks from device oper DS ...");
                writeTx.merge(LogicalDatastoreType.OPERATIONAL, iid,circuitPacks);
                FluentFuture< ? extends @NonNull CommitInfo> future = writeTx.commit();
                future.get();
                LOG.info("container circuitpacks '{}' merged to device oper datastore", iid);
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to process WriteTransactions",e);
            }
        } else {
            LOG.error("WriteTransaction object is null");
        }
    }

    /**
     * Get {@link OrgOpenroadmDevice} data
     * from operational datastore
     *
     * @param id OrgOpenroadmDevice InstanceIdentifier
     * @return OrgOpenroadmDevice result
     */
    private OrgOpenroadmDevice readDeviceOperData(final InstanceIdentifier<OrgOpenroadmDevice> id) {
        OrgOpenroadmDevice result = null;
        LOG.info("reading device operational datastore ...");
        ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction();
        if (readTx != null) {
            LOG.info("ReadTransaction is ok");
            try {
                LOG.info("reading device from device oper DS ...");
                java.util.Optional<OrgOpenroadmDevice> device = readTx.read(LogicalDatastoreType.OPERATIONAL, id).get();
                if (device.isPresent()) {
                    result = device.get();
                    LOG.info("device gets from device oper datastore");
                }
            } catch (InterruptedException | ExecutionException e) {
                LOG.error("Failed to process ReadTransaction",e);
            }
        } else {
            LOG.error("ReadTransaction object is null");
        }
        return result;

    }

}
