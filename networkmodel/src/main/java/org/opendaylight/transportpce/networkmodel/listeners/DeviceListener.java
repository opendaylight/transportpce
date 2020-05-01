/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.Optional;
//import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.networkmodel.service.NetworkModelService;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.ChangeNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OrgOpenroadmDeviceListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.OtdrScanResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceListener implements OrgOpenroadmDeviceListener {

    private static final long GET_DATA_SUBMIT_TIMEOUT = 5000;
    private static final TimeUnit MAX_DURATION_TO_SUBMIT_TIMEUNIT = TimeUnit.MILLISECONDS;
    private static final Logger LOG = LoggerFactory.getLogger(DeviceListener.class);
    private final DeviceTransactionManager deviceTransactionManager;
    private final String deviceId;
    private final NetworkModelService networkModelService;

    // private final ListeningExecutorService executorService;

    public DeviceListener(DeviceTransactionManager deviceTransactionManager,
                          String deviceId, final NetworkModelService networkModelService) {
        this.deviceTransactionManager = deviceTransactionManager;
        this.deviceId = deviceId;
        this.networkModelService = networkModelService;
        // this.executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(4));
    }

    /**
     * Callback for change-notification.
     *
     * @param notification ChangeNotification object
     */
    @Override
    public void onChangeNotification(ChangeNotification notification) {

        LOG.debug("Notification {} received {}", ChangeNotification.QNAME, notification);
        // Create method to obtain the target of the notification
        String targetType = notification.getEdit().get(0).getTarget().getTargetType().getSimpleName();

        switch (targetType) {
            case "Interface":
                // do changes
                LOG.info("Inferface change on device");
                break;
            case "CircuitPacks":
                // do changes
                // Create method to obtain circuitPack name key
                String circuitPackName = null;
                for (InstanceIdentifier.PathArgument pathArgument : notification.getEdit().get(0)
                        .getTarget().getPathArguments()) {
                    if (pathArgument.toString().contains("CircuitPacks")) {
                        String circuitPacksKey = StringUtils.substringBetween(pathArgument.toString(), "{", "}");
                        circuitPackName = StringUtils.substringAfter(circuitPacksKey, "=");
                    }
                }

                if (this.deviceTransactionManager.isDeviceMounted(deviceId)) {
                    InstanceIdentifier<CircuitPacks> cpIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                            .child(CircuitPacks.class, new CircuitPacksKey(circuitPackName));

                    // TODO: runnable cannot return a result, whereas Callable can
                    // TODO: method 1
                    Runnable runnable = new Runnable() {
                        private CircuitPacks cpaks;

                        public CircuitPacks getCpacks() {
                            return cpaks;
                        }

                        public void setCpacks(CircuitPacks cpaks) {
                            this.cpaks = cpaks;
                        }

                        @Override
                        public void run() {
                            // Create method to obtain the cpack changed
                            Optional<CircuitPacks> cpacksOpt =
                                    deviceTransactionManager.getDataFromDevice(deviceId,
                                            LogicalDatastoreType.OPERATIONAL,
                                            cpIID,
                                            GET_DATA_SUBMIT_TIMEOUT,
                                            MAX_DURATION_TO_SUBMIT_TIMEUNIT);
                            if (cpacksOpt.isPresent()) {
                                setCpacks(cpacksOpt.get());
                                // TODO: update topology according to previous state of the openroadm topology and
                                //  the current state of the circuit pack
                                // Managing only change in Ports state of circuit pack
                                networkModelService.updateOpenRoadmNode(deviceId, getCpacks());
                            } else {
                                LOG.warn("Could not get info from device");
                            }
                        }
                    };
                    Thread readDataThread = new Thread(runnable);
                    readDataThread.start();

                    // TODO: method 2 -> throws same error when reading data from device
            /*
            try {
                Callable<Optional<CircuitPacks>> callable = () -> {
                    Optional<CircuitPacks> cpackOpt =
                            deviceTransactionManager.getDataFromDevice(deviceId, LogicalDatastoreType.OPERATIONAL,
                                    cpIID,
                                    GET_DATA_SUBMIT_TIMEOUT,
                                    MAX_DURATION_TO_SUBMIT_TIMEUNIT);
                    LOG.info("Info from device = {}", cpackOpt.get().toString());
                    return cpackOpt;
                };
                ListenableFuture<Optional<CircuitPacks>> future = executorService.submit(callable);
                Futures.addCallback(future, new FutureCallback<Optional<CircuitPacks>>() {
                    @Override
                    public void onSuccess(@Nullable Optional<CircuitPacks> circuitPacks) {
                        LOG.info("Success on getting data from device");
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        LOG.error("Couldnt get data from device");
                    }
                }, executorService);
                while (!future.isDone()) {
                    Thread.sleep(200);
                }
                Optional<CircuitPacks> info = null;
                info = future.get(1, TimeUnit.SECONDS);
                LOG.info("Data from device {}", info.toString());
                executorService.shutdown();
            } catch (InterruptedException e) {
                LOG.error("Error exception {}", e.toString());
            } catch (ExecutionException e) {
                LOG.error("Error exception {}", e.toString());
            } catch (TimeoutException e) {
                LOG.error("Error exception {}", e.toString());
            }

            */
                } else {
                    LOG.warn("Device {} not mounted", deviceId);
                }
                break;
            default:
                // to do
                LOG.info("Type {} change not recognized", targetType);
        }
    }

    /*
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

     */

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