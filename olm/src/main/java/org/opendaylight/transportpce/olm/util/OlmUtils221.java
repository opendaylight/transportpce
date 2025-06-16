/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

import static org.opendaylight.transportpce.common.StringConstants.TX;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.OrgOpenroadmDeviceData;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.internal.links.InternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.internal.links.InternalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.DegreeKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.physical.links.PhysicalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.physical.links.PhysicalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.shelves.Shelves;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.shelves.ShelvesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev181019.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Direction;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Location;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifier;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OlmUtils221 {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils221.class);

    private OlmUtils221() {
    }

    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity. Currently vendorExtentions
     * are excluded but can be added back based on requirement.
     *
     * <p>This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     *
     * @return Result of the request list of PM readings
     */
    public static List<Measurements> pmFetch(GetPmInput input, DeviceTransactionManager deviceTransactionManager) {
        LOG.info("Getting PM Data for 2.2.1 NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
                input.getResourceType(), input.getResourceIdentifier());

        DataObjectIdentifier<?> resourceKeyIID =
                    findClassKeyIdentifiers(input.getResourceType(), input.getResourceIdentifier());
        if (resourceKeyIID == null) {
            return List.of();
        }
        CurrentPmEntryKey resourceKey = new CurrentPmEntryKey(resourceKeyIID,
                convertResourceTypeEnum(input.getResourceType()),"");
        DataObjectIdentifier<CurrentPmList> iidCurrentPmList = DataObjectIdentifier
                .builder(CurrentPmList.class)
                .build();

        Optional<CurrentPmList> currentPmListOpt = deviceTransactionManager.getDataFromDevice(input.getNodeId(),
                LogicalDatastoreType.OPERATIONAL, iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        List<Measurements> measurements = new ArrayList<>();
        if (currentPmListOpt.isPresent()) {
            CurrentPmList currentPmList = currentPmListOpt.orElseThrow();
            @NonNull
            Map<CurrentPmEntryKey,CurrentPmEntry> currentPmEntryList = currentPmList.nonnullCurrentPmEntry();
            LOG.info("Current PM list exists for node {} and contains {} entries.", input.getNodeId(),
                    currentPmEntryList.size());
            List<CurrentPm> filteredPMs = filterPmEntryList(currentPmEntryList, input, resourceKey);
            measurements = extractWantedMeasurements(filteredPMs,input);
        } else {
            LOG.error("Unable to get CurrentPmList for node {}", input.getNodeId());
        }
        return measurements;
    }

    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity. Currently vendorExtentions
     * are excluded but can be added back based on requirement.
     *
     * <p>This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name∫
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     *
     * @return A {@code Map<String, List<GetPmOutput>>} where the String key is the interface name
     **/
    public static Map<String, List<GetPmOutput>> pmFetchAll(GetPmInput input,
                                                            DeviceTransactionManager deviceTransactionManager) {
        LOG.info("Getting ALL PM Data for 2.2.1 NodeId: {} ", input.getNodeId());

        DataObjectIdentifier<CurrentPmList> iidCurrentPmList = DataObjectIdentifier
                .builder(CurrentPmList.class)
                .build();

        Optional<CurrentPmList> currentPmListOpt = deviceTransactionManager.getDataFromDevice(input.getNodeId(),
                LogicalDatastoreType.OPERATIONAL, iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (currentPmListOpt.isPresent()) {
            CurrentPmList currentPmList = currentPmListOpt.orElseThrow();
            @NonNull
            Map<CurrentPmEntryKey,CurrentPmEntry> currentPmEntryList = currentPmList.nonnullCurrentPmEntry();
            LOG.info("Current PM list exists for node {} and contains {} entries.", input.getNodeId(),
                    currentPmEntryList.size());
            return getPms(currentPmList, input);
        } else {
            LOG.error("Unable to get CurrentPmList for node {}", input.getNodeId());
        }
        return Map.of();
    }

    public static boolean setSpanLoss(String realNodeId, String interfaceName, BigDecimal spanLoss,
                                      String direction, OpenRoadmInterfaces openRoadmInterfaces) {
        RatioDB spanLossRx;
        RatioDB spanLossTx;
        try {
            Optional<Interface> interfaceObject =
                    openRoadmInterfaces.getInterface(realNodeId, interfaceName);

            if (interfaceObject.isPresent()) {
                InterfaceBuilder interfaceBuilder =
                        new InterfaceBuilder(interfaceObject.orElseThrow());
                OtsBuilder otsBuilder = new OtsBuilder();
                Interface intf = interfaceObject.orElseThrow();
                if (intf.augmentation(Interface1.class) != null
                        && intf.augmentation(Interface1.class).getOts() != null) {
                    Ots ots = intf.augmentation(Interface1.class).getOts();

                    otsBuilder.setFiberType(ots.getFiberType());
                    spanLossRx = ots.getSpanLossReceive();
                    spanLossTx = ots.getSpanLossTransmit();
                } else {
                    spanLossRx = new RatioDB(Decimal64.valueOf(spanLoss));
                    spanLossTx = new RatioDB(Decimal64.valueOf(spanLoss));
                }
                Interface1Builder intf1Builder = new Interface1Builder();
                if (direction.equals(TX)) {
                    otsBuilder.setSpanLossTransmit(new RatioDB(Decimal64.valueOf(spanLoss)));
                    otsBuilder.setSpanLossReceive(spanLossRx);
                } else {
                    otsBuilder
                            .setSpanLossTransmit(spanLossTx)
                            .setSpanLossReceive(new RatioDB(Decimal64.valueOf(spanLoss)));
                }
                interfaceBuilder.addAugmentation(intf1Builder.setOts(otsBuilder.build()).build());
                openRoadmInterfaces.postInterface(realNodeId, interfaceBuilder);
                LOG.info("Spanloss Value update completed successfully");
                return true;
            } else {
                LOG.error("Interface not found for realNodeId: {} and interfaceName: {}", realNodeId, interfaceName);
                return false;
            }
        }
        catch (OpenRoadmInterfaceException e) {
            LOG.error("Failed to set spanloss on node {}, OpenRoadmInterfaceException occured: {}",
                    realNodeId,
                    e.getMessage());
            return false;
        }
    }

    /*
     * Retrieves a map on the form Map<String interfaceName, List<GetPmOutput>>
     */
    private static Map<String, List<GetPmOutput>> getPms(CurrentPmList currentPmList, GetPmInput input) {
        Map<String, List<GetPmOutput>> returnMap = new HashMap<>();
        Map<CurrentPmEntryKey, CurrentPmEntry> currentPmEntryList = currentPmList.nonnullCurrentPmEntry();

        for (Map.Entry<CurrentPmEntryKey, CurrentPmEntry> entry : currentPmEntryList.entrySet()) {
            List<GetPmOutput> returnList = new ArrayList<>();
            CurrentPmEntry cpe = entry.getValue();
            String interfaceName = getInterfaceName(cpe);
            ResourceIdentifier rsId = new ResourceIdentifierBuilder()
                    .setResourceName(interfaceName)
                    .build();
            List<CurrentPm> currentPMList = new ArrayList<>(cpe.nonnullCurrentPm().values());
            for (CurrentPm currentPm:currentPMList) {
                List<Measurements> measurements = getMeasurements(currentPm);
                if (!measurements.isEmpty()) {
                    GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
                    pmOutputBuilder.setNodeId(input.getNodeId())
                            .setResourceType(input.getResourceType())
                            .setResourceIdentifier(rsId)
                            .setGranularity(input
                                    .getGranularity())
                            .setMeasurements(measurements);
                    returnList.add(pmOutputBuilder.build());
                }  else {
                    LOG.info("No Measurements found for node {} on type {} for granularity {}",
                            input.getNodeId(),
                            currentPm.getType().name(),
                            input.getGranularity());
                }
            }
            returnMap.put(interfaceName,returnList);
        }
        return returnMap;
    }

    private static String getInterfaceName(CurrentPmEntry cpe) {
        InterfaceKey key = ((DataObjectIdentifier<?>)cpe.getPmResourceInstance()).firstKeyOf(Interface.class);
        if (key != null) {
            return key.getName();
        }
        return null;
    }


    private static List<Measurements> getMeasurements(CurrentPm currentPm) {
        List<Measurements> olmMeasurements = new ArrayList<>();
        for (Measurement measurements: currentPm.nonnullMeasurement().values()) {
            if (measurements.getPmParameterValue() != null) {
                MeasurementsBuilder pmMeasureBuilder = new MeasurementsBuilder();
                pmMeasureBuilder.setPmparameterName(currentPm.getType().name());
                pmMeasureBuilder.setPmparameterValue(measurements.getPmParameterValue().stringValue());
                olmMeasurements.add(pmMeasureBuilder.build());
            }
        }
        return olmMeasurements;
    }

    private static List<CurrentPm> filterPmEntryList(Map<CurrentPmEntryKey, CurrentPmEntry> currentPmEntryList,
            GetPmInput input, CurrentPmEntryKey resourceKey) {

        for (Map.Entry<CurrentPmEntryKey, CurrentPmEntry> entry : currentPmEntryList.entrySet()) {
            CurrentPmEntry cpe = entry.getValue();
            CurrentPmEntryKey cpek = new CurrentPmEntryKey(cpe.getPmResourceInstance(), cpe.getPmResourceType(),
                    "");
            if (resourceKey.equals(cpek)) {
                List<CurrentPm> currentPMList = new ArrayList<>(cpe.nonnullCurrentPm().values());
                Stream<CurrentPm> currentPMStream = currentPMList.stream();
                if (input.getPmNameType() != null) {
                    currentPMStream = currentPMStream.filter(pm -> pm.getType().getIntValue()
                            == PmNamesEnum.forValue(input.getPmNameType().getIntValue()).getIntValue());
                }
                if (input.getPmExtension() != null) {
                    currentPMStream = currentPMStream.filter(pm -> pm.getExtension()
                            .equals(input.getPmExtension()));
                }
                if (input.getLocation() != null) {
                    currentPMStream = currentPMStream.filter(pm -> Location.forValue(pm.getLocation()
                                    .getIntValue())
                            .equals(Location.forValue(input.getLocation().getIntValue())));
                }
                if (input.getDirection() != null) {
                    currentPMStream = currentPMStream.filter(pm -> Direction.forValue(pm.getDirection()
                                    .getIntValue())
                            .equals(Direction.forValue((input.getDirection().getIntValue()))));
                }
                return currentPMStream.collect(Collectors.toList());
            }
        }
        return List.of();
    }

    private static List<Measurements> extractWantedMeasurements(List<CurrentPm> currentPmList,
        GetPmInput input) {
        List<Measurements> olmMeasurements = new ArrayList<>();
        for (CurrentPm pm : currentPmList) {
            for (Measurement measurements: pm.nonnullMeasurement().values()) {
                if (measurements.getGranularity().getIntValue()
                        == org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity.forValue(
                        input.getGranularity().getIntValue()).getIntValue()) {
                    MeasurementsBuilder pmMeasureBuilder = new MeasurementsBuilder();
                    pmMeasureBuilder.setPmparameterName(pm.getType().name());
                    if (measurements.getPmParameterValue() != null) {
                        pmMeasureBuilder.setPmparameterValue(measurements.getPmParameterValue().stringValue());
                    }
                    else {
                        throw new RuntimeException("No ParameterValue found for node " + input.getNodeId()
                                + " on type " + pm.getType().name() + " for granularity " + input.getGranularity());
                    }
                    olmMeasurements.add(pmMeasureBuilder.build());
                }
            }
        }
        return olmMeasurements;
    }

    private static DataObjectIdentifier<?> findClassKeyIdentifiers(ResourceTypeEnum wantedResourceType,
            ResourceIdentifier wantedResourceIdentifier) {
        if (wantedResourceIdentifier.getResourceName() == null) {
            LOG.debug("resource {} is null", wantedResourceType);
            return null;
        }
        switch (wantedResourceType) {
            case Device:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .build();
            case Degree:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(Degree.class, new DegreeKey(Uint16.valueOf(wantedResourceIdentifier.getResourceName())))
                    .build();
            case SharedRiskGroup:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(
                        SharedRiskGroup.class,
                        new SharedRiskGroupKey(Uint16.valueOf(wantedResourceIdentifier.getResourceName())))
                    .build();
            case Connection:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(RoadmConnections.class, new RoadmConnectionsKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            case CircuitPack:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            case Port:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(wantedResourceIdentifier.getCircuitPackName()))
                    .child(Ports.class, new PortsKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            case Interface:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(Interface.class, new InterfaceKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            case InternalLink:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(InternalLink.class, new InternalLinkKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            case PhysicalLink:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(PhysicalLink.class, new PhysicalLinkKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            case Shelf:
                return DataObjectIdentifier
                    .builderOfInherited(OrgOpenroadmDeviceData.class, OrgOpenroadmDevice.class)
                    .child(Shelves.class, new ShelvesKey(wantedResourceIdentifier.getResourceName()))
                    .build();
            default:
                LOG.error("Unknown resource type {}", wantedResourceType);
                return null;
        }
    }

    private static org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum
        convertResourceTypeEnum(ResourceTypeEnum wantedResourceType) {
        switch (wantedResourceType) {
            case Device:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum.Device;
            case Degree:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum.Degree;
            case SharedRiskGroup:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019
                        .ResourceTypeEnum.SharedRiskGroup;
            case Connection:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019
                        .ResourceTypeEnum.Connection;
            case CircuitPack:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019
                        .ResourceTypeEnum.CircuitPack;
            case Port:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum.Port;
            case Interface:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019
                        .ResourceTypeEnum.Interface;
            case InternalLink:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019
                        .ResourceTypeEnum.InternalLink;
            case PhysicalLink:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019
                        .ResourceTypeEnum.PhysicalLink;
            case Shelf:
                return org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum.Shelf;
            default:
                LOG.error("Unknown resource type {}", wantedResourceType);
                return null;
        }
    }
}

