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
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeIdentifier;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification.Datastore;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotificationBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.EditBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.changed.by.parms.ChangedBy;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.changed.by.parms.ChangedByBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.changed.by.parms.changed.by.server.or.user.ServerBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CpSlots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.Port;
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
    private static final String INTERFACE_CLASS = "org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface";
    private static final String CIRCUITPACK_CLASS = "org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks";

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
        this.dataBroker.registerDataTreeChangeListener(DataTreeIdentifier.create(LogicalDatastoreType.OPERATIONAL, DEVICE_CONTAINER_ID),
                new DataTreeChangeListener<OrgOpenroadmDevice>() {
                    @Override
                    public void onDataTreeChanged(Collection<DataTreeModification<OrgOpenroadmDevice>> changes) {
                        LOG.info("onDataTreeChanged");
                        ChangeNotification changeNotification = null;
                        try {
                            changeNotification = transformToNotification(changes);
                            LOG.info("Emitting notification : {}", changeNotification);
                            collector.onNotification(changeNotification);
                            LOG.info("Succesfully emitted notification : {}", changeNotification);
                        } catch (NullPointerException e) {
                            LOG.warn("Failed to emit notification"); // Remember this means that it is not an interface or that it actually failed
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
            String nodeId = rootNode.getDataAfter().getInfo().getNodeId().toString();
            if (rootNode != null) {
                Collection<? extends DataObjectModification<? extends DataObject>> modifiedChildren = rootNode.getModifiedChildren();
                switch (rootNode.getModificationType()) {
                    case SUBTREE_MODIFIED: // OrgOpenroadmDevice
                        if (!modifiedChildren.isEmpty()) {
                            Iterator<? extends DataObjectModification<? extends DataObject>> iterator = modifiedChildren.iterator();
                            while (iterator.hasNext()) {
                                DataObjectModification<? extends DataObject> modified = iterator.next();
                                if (!modified.getDataAfter().equals(modified.getDataBefore())) {
                                    LOG.info(
                                            "modified = \ndataType : {}\nid : {}\nmodifiedType : {}\noldData : {}\nnewData : {} \n",
                                            modified.getDataType(), modified.getIdentifier(), modified.getModificationType(),
                                            modified.getDataBefore(), modified.getDataAfter());
                                    String dataType = modified.getDataType().getName();
                                    switch (dataType) {
                                        case INTERFACE_CLASS:
                                            Interface ifc_bef = null;
                                            Interface ifc_aft = null;
                                            LOG.info("DevNotProd Interface type update !");
                                            switch (modified.getModificationType()) {
                                                case SUBTREE_MODIFIED:
                                                    ifc_aft = (Interface) modified.getDataAfter();
                                                    ifc_bef = (Interface) modified.getDataBefore();
                                                    break;
                                                case WRITE:
                                                    ifc_aft = (Interface) modified.getDataAfter();
                                                    ifc_bef = (Interface) modified.getDataBefore();
                                                    break;
                                                case DELETE:
                                                    ifc_bef = (Interface) modified.getDataBefore();
                                                    ifc_aft = (Interface) modified.getDataAfter();
                                                    break;
                                                default:
                                                    break;
                                            }
                                            if (ifc_aft != null) {
                                                LOG.info("DevNotProd Interface data = {}", ifc_aft);
                                                JSONObject ifc_aft_obj = InterfaceToList(ifc_aft);
                                                LOG.info("DevNotProd Interface json data = {}", ifc_aft_obj.toString());
                                                String interfaceName = ifc_aft.getName();
                                                if (ifc_aft_obj != null && interfaceName != null) {
                                                    //Passing modified interface
                                                    InstanceIdentifier<Interface> interfaceIId = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                                                            Interface.class, new InterfaceKey(interfaceName));
                                                    Edit edit = new EditBuilder()
                                                            .setOperation(EditOperationType.Merge)
                                                            .setTarget(interfaceIId)
                                                            .setBefore(InterfaceToList(ifc_bef).toString())
                                                            .setAfter(ifc_aft_obj.toString())
                                                            .build();
                                                    LOG.info("Interface Edit generated: {}", edit.toString());
                                                    editList.add(edit);
                                                }
                                            } else {
                                                LOG.warn("Interface data is null !");
                                            }
                                            break;
                                        case CIRCUITPACK_CLASS:
                                            CircuitPacks cpck_bef = null;
                                            CircuitPacks cpck_aft = null;
                                            LOG.info("CircuitPack type update !");
                                            switch (modified.getModificationType()) {
                                                case SUBTREE_MODIFIED:
                                                    cpck_aft = (CircuitPacks) modified.getDataAfter();
                                                    cpck_bef = (CircuitPacks) modified.getDataBefore();
                                                    break;
                                                case WRITE:
                                                    cpck_aft = (CircuitPacks) modified.getDataAfter();
                                                    cpck_bef = (CircuitPacks) modified.getDataBefore();
                                                    break;
                                                case DELETE:
                                                    cpck_bef = (CircuitPacks) modified.getDataBefore();
                                                    cpck_aft = (CircuitPacks) modified.getDataAfter();
                                                    break;
                                                default:
                                                    break;
                                            }
                                            if (cpck_aft != null && cpck_bef != null) {
                                                LOG.info("CircuitPacks data = {}", cpck_aft);
                                                JSONObject cpack_aft_obj = CircuitPackToList(cpck_aft);
                                                LOG.info("DevNotProd circuit pack json data = {}", cpack_aft_obj.toString());
                                                String circuit_name = cpck_aft.getCircuitPackName();
                                                if (cpack_aft_obj != null && circuit_name != null) {
                                                    //Passing modified Circuit pack
                                                    InstanceIdentifier<CircuitPacks> circuitPackIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
                                                            CircuitPacks.class, new CircuitPacksKey(circuit_name));
                                                    Edit edit = new EditBuilder()
                                                            .setOperation(EditOperationType.Merge)
                                                            .setTarget(circuitPackIID)
                                                            .setBefore(CircuitPackToList(cpck_bef).toString())
                                                            .setAfter(cpack_aft_obj.toString())
                                                            .build();
                                                    LOG.info("Circuit Pack Edit generated: {}", edit.toString());
                                                    editList.add(edit);
                                                }
                                            } else {
                                                LOG.warn("Circuit Pack data after/before is null !");
                                            }
                                            break;
                                        default:
                                            LOG.info("Type of change = {}", dataType);
                                            LOG.info("Change not supported");
                                    }
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
    private JSONObject InterfaceToList(Interface iface) {
        LOG.info("DevNotProd hello from interface to list");
        JSONObject data = new JSONObject();
        data.put("AdministrativeState", iface.getAdministrativeState().toString());
        data.put("CircuitId", iface.getCircuitId());
        data.put("Name", iface.getName());
        data.put("SupportingCircuitPackName", iface.getSupportingCircuitPackName());
        data.put("SupportingInterface", iface.getSupportingInterface());
        data.put("SupportingPort", iface.getSupportingPort().toString());
        data.put("Type", iface.getType().getSimpleName());
        //Augmentations are not considered
        return data;

    }
    private JSONObject CircuitPackToList(CircuitPacks cpacks) {
        LOG.info("DevNotProd hello from CircuitPacks to list");
        JSONObject data = new JSONObject();
        data.put("AdministrativeState", cpacks.getAdministrativeState().toString());
        //CircuitCategory pack
        JSONObject categoryPack = new JSONObject();
        categoryPack.put("Type", cpacks.getCircuitPackCategory().getType());
        data.put("CircuitPackCategory", categoryPack);
        data.put("CircuitPackMode", cpacks.getCircuitPackMode());
        data.put("CircuitPackName", cpacks.getCircuitPackName());
        data.put("CircuitPackType", cpacks.getCircuitPackType());
        data.put("Clei", cpacks.getClei());
        //CpSlots
        List<CpSlots> cpSlotsList = cpacks.getCpSlots();
        if (cpSlotsList != null) {
            JSONArray slotArray = new JSONArray();
            for(CpSlots cpSlots:cpSlotsList){
                JSONObject slotJson = new JSONObject();
                slotJson.put("Label", cpSlots.getLabel());
                slotJson.put("ProvisionedCircuitPack", cpSlots.getProvisionedCircuitPack());
                slotJson.put("SlotName", cpSlots.getSlotName());
                slotArray.put(slotJson);
            }
            data.put("CpSlots", slotArray);
        }
        // Parent circuit pack
        if (cpacks.getParentCircuitPack() != null) {
            JSONObject parentCpack = new JSONObject();
            parentCpack.put("CircuitPackName", cpacks.getParentCircuitPack().getCircuitPackName());
            parentCpack.put("CpSlotName", cpacks.getParentCircuitPack().getCpSlotName());
            data.put("ParentCircuitPack", parentCpack);
        }
        data.put("EquipmentState", cpacks.getEquipmentState());
        data.put("ManufactureDate", cpacks.getManufactureDate().getValue());
        data.put("Model", cpacks.getModel());
        if (cpacks.getOperationalState() != null){
            data.put("OperationalState", cpacks.getOperationalState().toString());
        }
        //Ports
        List<Ports> portsList = cpacks.getPorts();
        JSONArray portsArray = new JSONArray();
        for(Ports ports:portsList){
            JSONObject portJson = new JSONObject();
            portJson.put("AdministrativeState", ports.getAdministrativeState().toString());
            portJson.put("CircuitId", ports.getCircuitId());
            portJson.put("LogicalConnectionPoint", ports.getLogicalConnectionPoint());
            portJson.put("OperationalState", ports.getOperationalState().toString());
            portJson.put("PortDirection", ports.getPortDirection());
            portJson.put("PortName", ports.getPortName());
            portJson.put("PortQual", ports.getPortQual());
            portJson.put("PortType", ports.getPortType());
            if (ports.getPortWavelengthType() != null) {
                portJson.put("PortWavelengthType", ports.getPortWavelengthType());
            }
            //These are the common parameters to all ports, some could have roadmport with tx/rx information. Not considered now...
            portsArray.put(portJson);
        }
        data.put("Ports", portsArray); //finish
        data.put("SerialId", cpacks.getSerialId());
        data.put("Shelf", cpacks.getShelf());
        data.put("Model", cpacks.getModel());
        data.put("Slot", cpacks.getSlot());
        data.put("SubSlot", cpacks.getSubSlot());
        data.put("Vendor", cpacks.getVendor());
        //Augmentations are not considered
        return data;
    }
}
