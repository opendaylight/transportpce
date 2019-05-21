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
package io.fd.honeycomb.transportpce.device.notifications;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.fd.honeycomb.notification.ManagedNotificationProducer;
import io.fd.honeycomb.notification.NotificationCollector;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.ChangeNotification.Datastore;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.ChangeNotificationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.change.notification.EditBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.changed.by.parms.ChangedBy;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.changed.by.parms.ChangedByBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.changed.by.parms.changed.by.server.or.user.ServerBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.EditOperationType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Martial COULIBALY ( mcoulibaly.ext@orange.com ) on behalf of Orange
 */
public class DeviceNotificationProducer implements ManagedNotificationProducer {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceNotificationProducer.class);
    private static final InstanceIdentifier<OrgOpenroadmDevice> DEVICE_CONTAINER_ID = InstanceIdentifier
            .create(OrgOpenroadmDevice.class);
    private static final String INTERFACE_CLASS = Interface.class.getName();

    @Inject
    @Named("device-databroker")
    private DataBroker dataBroker;

    @Override
    public Collection<Class<? extends Notification>> getNotificationTypes() {
        final ArrayList<Class<? extends Notification>> classes = Lists.newArrayList();
        classes.add(ChangeNotification.class);
        return classes;
    }

    @Override
    public void close() throws Exception {
        stop();
    }

    @Override
    public void start(NotificationCollector collector) {
        LOG.info("Starting notification stream for OrgOpenroadmDevice");
        Preconditions.checkArgument(this.dataBroker != null, "Device datastore is null");
        this.dataBroker.registerDataTreeChangeListener(
                new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL, DEVICE_CONTAINER_ID),
                new DataTreeChangeListener<OrgOpenroadmDevice>() {
                    @Override
                    public void onDataTreeChanged(Collection<DataTreeModification<OrgOpenroadmDevice>> changes) {
                        LOG.info("onDataTreeChanged");
                        ChangeNotification changeNotification = null;
                        try {
                            changeNotification = transformToNotification(changes);
                            LOG.info("Emitting notification : {}", changeNotification);
                            collector.onNotification(changeNotification);
                        } catch (NullPointerException e) {
                            LOG.warn("Failed to emit notification");
                        }
                    }
                });
    }

    @Override
    public void stop() {
        LOG.info("Stopping OrgOpenroadmDevice change notification");
    }

    /**
     * Transform {@link Collection} of {@link DataTreeModification } to {@link ChangeNotification}
     *
     * @return changeNotification {@link ChangeNotification}
     */
    private ChangeNotification transformToNotification(Collection<DataTreeModification<OrgOpenroadmDevice>> changes) {
        LOG.info("transforming changes to notification...");
        ChangeNotification result = null;
        List<Edit> editList = new ArrayList<Edit>();
        for (DataTreeModification<OrgOpenroadmDevice> change : changes) {
            LOG.info("Received Device change :\n{}", change.getRootNode().getModificationType());
            final DataObjectModification<OrgOpenroadmDevice> rootNode = change.getRootNode();
            final DataTreeIdentifier<OrgOpenroadmDevice> rootPath = change.getRootPath();
            if (rootNode != null) {
                Collection<DataObjectModification<? extends DataObject>> modifiedChildren = rootNode.getModifiedChildren();
                switch (rootNode.getModificationType()) {
                case SUBTREE_MODIFIED: // OrgOpenroadmDevice
                    if (!modifiedChildren.isEmpty()) {
                        Iterator<DataObjectModification<? extends DataObject>> iterator = modifiedChildren.iterator();
                        while (iterator.hasNext()) {
                            DataObjectModification<? extends DataObject> modified = iterator.next();
                            LOG.info(
                                    "modified = \ndataType : {}\nid : {}\nmodifiedType : {}\noldData : {}\nnewData : {} \n",
                                    modified.getDataType(), modified.getIdentifier(), modified.getModificationType(),
                                    modified.getDataBefore(), modified.getDataAfter());
                            String dataType = modified.getDataType().getName();
                            if (dataType.equals(INTERFACE_CLASS)) {
                                Interface data = null;
                                LOG.info("Interface type update !");
                                switch (modified.getModificationType()) {
                                case SUBTREE_MODIFIED:
                                    data = (Interface) modified.getDataAfter();
                                    break;
                                case WRITE:
                                    data = (Interface) modified.getDataAfter();
                                    break;
                                case DELETE:
                                    data = (Interface) modified.getDataBefore();
                                    break;
                                default:
                                    break;
                                }
                                if (data!= null) {
                                    String circuitPackName = data.getSupportingCircuitPackName();
                                    String port = data.getSupportingPort().toString();
                                    String interfaceName = data.getName();
                                    if (circuitPackName != null && port != null && interfaceName != null) {
                                        InstanceIdentifier<Ports> portIId = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                                                CircuitPacks.class, new CircuitPacksKey(circuitPackName)).child(Ports.class,
                                                    new PortsKey(port));
                                        Edit edit = new EditBuilder()
                                                .setOperation(EditOperationType.Merge)
                                                .setTarget(portIId)
                                                .build();
                                        editList.add(edit);
                                    }
                                } else {
                                    LOG.warn("Interface data is null !");
                                }
                            } else {
                                LOG.warn("modifiedChild is not an interface !");
                            }
                        }
                    }
                    break;
                case WRITE:
                    LOG.info("device operational datastore is created !");
                    break;
                case DELETE:
                    LOG.info("device operational datastore is deleted !");
                    break;
                default:
                    break;
                }
            } else {
                LOG.error("rootNode is null !");
            }
        }
        if (!editList.isEmpty()) {
            ChangedBy changedBy = new ChangedByBuilder().setServerOrUser(new ServerBuilder().setServer(true).build())
                    .build();
            String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX").format(new Date());
            result = new ChangeNotificationBuilder().setChangedBy(changedBy).setChangeTime(new DateAndTime(time))
                    .setDatastore(Datastore.Running).setEdit(editList).build();
        } else {
            LOG.warn("edit List is empty !");
        }
        return result;
    }
}
