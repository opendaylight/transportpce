/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.ArrayList;
import java.util.List;
//import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opendaylight.mdsal.binding.api.DataBroker;
//import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.EquipmentTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.change.notification.Edit;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CircuitPackCategory;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CircuitPackCategoryBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CpSlots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.CpSlotsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.ParentCircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.ParentCircuitPackBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container
// .OrgOpenroadmDevice;
//import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container
// .org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.AdminStates;
import org.opendaylight.yang.gen.v1.http.org.openroadm.equipment.states.types.rev161014.States;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.EthernetCsmacd;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.Ip;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalChannel;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOdu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OtnOtu;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev170929.PortWavelengthTypes;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
//import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class DeviceListener implements OrgOpenroadmDeviceListener {

    private static final long GET_DATA_SUBMIT_TIMEOUT = 5000;
    private static final TimeUnit MAX_DURATION_TO_SUBMIT_TIMEUNIT = TimeUnit.MILLISECONDS;
    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener.class);
    private final DataBroker databroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final String deviceId;
    private final NetworkModelService networkModelService;

    public DeviceListener(DeviceTransactionManager deviceTransactionManager,
                          final DataBroker dataBroker, String deviceId, final NetworkModelService networkModelService) {
        this.databroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.deviceId = deviceId;
        this.networkModelService = networkModelService;
    }


    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */
    @Override
    public void onChangeNotification(ChangeNotification notification) {

        LOG.info("Notification {} received {}", ChangeNotification.QNAME, notification);
        String infoaft = null;
        String inforbef = null;
        String type = null;
        for (Edit edit:notification.getEdit()) {
            // type of resource changed
            // name of the resource
            type = edit.getTarget().getTargetType().getSimpleName();
            infoaft = edit.getAfter();
            inforbef = edit.getBefore();
        }
        // Aqui deberia hacerse el getDataFromDevice para obtener el objeto que se ha cambiado
        switch (type) {
            case "Interface":
                // do changes
                Interface ifcaft = convertStringtoInterface(infoaft);
                Interface ifcbef = convertStringtoInterface(inforbef);
                LOG.info("Inferface after = {}\nInterface before = {}", ifcaft, ifcbef);
                break;
            case "CircuitPacks":
                // do changes
                // Now we are considering AdminState = OperState
                CircuitPacks cpckaft = convertStringtoCircuitPack(infoaft);
                CircuitPacks cpckbef = convertStringtoCircuitPack(inforbef);
                LOG.info("CircuitPack after = {}\nCircuitPack before = {}", cpckaft, cpckbef);
                for (int i = 0; i < cpckaft.getPorts().size(); i++) {
                    if (!cpckaft.getPorts().get(i).getOperationalState().getName()
                            .equals(cpckbef.getPorts().get(i).getOperationalState().getName())) {
                        LOG.info("Detected port {} with new operational state",
                                cpckaft.getPorts().get(i).getPortName());
                        networkModelService
                                .updateOpenRoadmNode(deviceId,
                                        cpckaft.getPorts().get(i)
                                                .getLogicalConnectionPoint(),
                                        cpckaft.getPorts().get(i),
                                        cpckaft.getCircuitPackName());
                    } else {
                        LOG.info("Operational state of port {} hasnt changed",
                                cpckaft.getPorts().get(i).getPortName());
                    }
                }
                break;
            default:
                // to do
                LOG.info("Type {} change not recognized", type);
        }

        /*
        List<Edit> notificationEdit = notification.getEdit();
        List<String> keyNames = new ArrayList<String>();
        for (Edit edit : notificationEdit) {
            // Get type of change and from there, get the class
            keyNames.add(edit.getTarget().getTargetType().getName());

        }
        LOG.info("Keys stored: {}", keyNames.toString());
        if (this.deviceTransactionManager.isDeviceMounted(deviceId)) {
            InstanceIdentifier<Info> infoIID = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(Info.class);
            Optional<Info> infoOpt =
                    deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL, infoIID,
                                                               GET_DATA_SUBMIT_TIMEOUT,
                                                               MAX_DURATION_TO_SUBMIT_TIMEUNIT);
            if (infoOpt.isPresent()) {
                LOG.info("Info of device {} from datastore: {}", deviceId, infoOpt.get().toString());
            } else {
                LOG.warn("Could not get info from device");
            }
        } else {
            LOG.warn("Device {} not mounted", deviceId);
        }

        */
    }


    private CircuitPacks convertStringtoCircuitPack(String infoaft) {
        CircuitPacksBuilder circuitPacksBuilder = new CircuitPacksBuilder();
        JSONObject jsonObject = new JSONObject(infoaft);
        //Circuit Pack Category
        JSONObject packCategory = jsonObject.getJSONObject("CircuitPackCategory");
        CircuitPackCategory circuitPackCategory = new CircuitPackCategoryBuilder()
                .setType(EquipmentTypeEnum.valueOf(packCategory.getString("Type")))
                .build();
        circuitPacksBuilder.setCircuitPackCategory(circuitPackCategory);
        //CpSlots
        List<CpSlots> cpSlotsList = new ArrayList<CpSlots>();
        if (jsonObject.has("CpSlots")) {
            JSONArray slots = jsonObject.getJSONArray("CpSlots");
            for (int i = 0; i < slots.length(); i++) {
                JSONObject aux = slots.getJSONObject(i);
                CpSlots cpSlots = new CpSlotsBuilder()
                        .setLabel((String) aux.get("Label"))
                        .setProvisionedCircuitPack((String) aux.get("ProvisionedCircuitPack"))
                        .setSlotName((String) aux.get("SlotName"))
                        .build();
                cpSlotsList.add(cpSlots);
            }
            circuitPacksBuilder.setCpSlots(cpSlotsList);
        }
        if (jsonObject.has("ParentCircuitPack")) {
            JSONObject parentcPack = jsonObject.getJSONObject("ParentCircuitPack");
            ParentCircuitPack parentCircuitPack = new ParentCircuitPackBuilder()
                    .setCircuitPackName(parentcPack.getString("CircuitPackName"))
                    .setCpSlotName(parentcPack.getString("CpSlotName"))
                    .build();
            circuitPacksBuilder.setParentCircuitPack(parentCircuitPack);
        }
        if (jsonObject.has("OperationalState")) {
            circuitPacksBuilder.setOperationalState(
                    State.valueOf(jsonObject.getString("OperationalState")));
        }
        //Ports
        List<Ports> portList = new ArrayList<Ports>();
        JSONArray ports = jsonObject.getJSONArray("Ports");
        for (int i = 0; i < ports.length(); i++) {
            JSONObject aux = ports.getJSONObject(i);
            Ports ports1 = new PortsBuilder()
                    .setAdministrativeState(AdminStates.valueOf(aux.getString("AdministrativeState")))
                    .setCircuitId(!aux.has("CircuitId") ? null : aux.getString("CircuitId"))
                    .setLogicalConnectionPoint(!aux.has("LogicalConnectionPoint") ? null
                            : aux.getString("LogicalConnectionPoint"))
                    .setOperationalState(State.valueOf(aux.getString("OperationalState")))
                    .setPortDirection(Direction.valueOf(aux.getString("PortDirection")))
                    .setPortName(aux.getString("PortName"))
                    .setPortQual(Port.PortQual.valueOf(aux.getString("PortQual")))
                    .setPortType(aux.getString("PortType"))
                    .setPortWavelengthType(!aux.has("PortWavelengthType") ? null
                            : PortWavelengthTypes.valueOf(aux.getString(
                            "PortWavelengthType")))
                    .build();
            portList.add(ports1);
        }
        return circuitPacksBuilder
                .setAdministrativeState(AdminStates.valueOf(jsonObject.getString("AdministrativeState")))
                .setCircuitPackMode(jsonObject.getString("CircuitPackMode"))
                .setCircuitPackName(jsonObject.getString("CircuitPackName"))
                .setCircuitPackType(jsonObject.getString("CircuitPackType"))
                .setClei(jsonObject.getString("Clei"))
                .setEquipmentState(States.valueOf(jsonObject.getString("EquipmentState")))
                .setManufactureDate(DateAndTime.getDefaultInstance(jsonObject.getString("ManufactureDate")))
                .setModel(jsonObject.getString("Model"))
                .setPorts(portList)
                .setSerialId(jsonObject.getString("SerialId"))
                .setShelf(jsonObject.getString("Shelf"))
                .setSlot(jsonObject.getString("Slot"))
                .setSubSlot(jsonObject.getString("SubSlot"))
                .setVendor(jsonObject.getString("Vendor"))
                .build();


    }

    private Interface convertStringtoInterface(String infoaft) {
        JSONObject jsonObject = new JSONObject(infoaft);
        return new InterfaceBuilder()
                .setAdministrativeState(AdminStates.valueOf(jsonObject.getString("AdministrativeState")))
                .setCircuitId(jsonObject.getString("CircuitId"))
                .setName(jsonObject.getString("Name"))
                .setSupportingCircuitPackName(jsonObject.getString("SupportingCircuitPackName"))
                .setSupportingInterface(!jsonObject.has("SupportingInterface") ? null : jsonObject
                        .getString("SupportingInterface"))
                .setSupportingPort(jsonObject.get("SupportingPort"))
                .setType(getInterfaceType(jsonObject.getString("Type")))
                .build();
    }

    private Class<? extends InterfaceType> getInterfaceType(String type) {
        if (type.equals("openROADMOpticalMultiplex")) {
            return OpenROADMOpticalMultiplex.class;
        } else {
            if (type.equals("otnOtu")) {
                return OtnOtu.class;
            } else {
                if (type.equals("otnOdu")) {
                    return OtnOdu.class;
                } else {
                    if (type.equals("opticalTransport")) {
                        return OpticalTransport.class;
                    } else {
                        if (type.equals("opticalChannel")) {
                            return OpticalChannel.class;
                        } else {
                            if (type.equals("ip")) {
                                return Ip.class;
                            } else {
                                if (type.equals("ethernetCsmacd")) {
                                    return EthernetCsmacd.class;
                                } else {
                                    return null;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Callback for otdr-scan-result.
     *
     * @param notification OtdrScanResult object
     */
    @Override
    public void onOtdrScanResult(OtdrScanResult notification) {

        LOG.info("Notification {} received {}", OtdrScanResult.QNAME, notification);
    }
}