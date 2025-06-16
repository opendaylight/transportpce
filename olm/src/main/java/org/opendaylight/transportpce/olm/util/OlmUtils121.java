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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Measurements;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPmKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Connection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.InternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.PhysicalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Shelf;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev161014.resource.resource.resource.Srg;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Direction;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Location;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifier;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OlmUtils121 {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils121.class);

    private OlmUtils121() {
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
    public static List<org.opendaylight.yang.gen.v1.http
            .org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
            pmFetch(GetPmInput input,DeviceTransactionManager deviceTransactionManager) {
        LOG.info("Getting PM Data for 1.2.1 NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
                input.getResourceType(), input.getResourceIdentifier());
        DataObjectIdentifier<CurrentPmlist> currentPmsIID = DataObjectIdentifier.builder(CurrentPmlist.class).build();
        Optional<CurrentPmlist> currentPmList;
        currentPmList = deviceTransactionManager
                .getDataFromDevice(input.getNodeId(), LogicalDatastoreType.OPERATIONAL, currentPmsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        List<org.opendaylight.yang.gen.v1.http
                .org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
                measurements = new ArrayList<>();
        if (currentPmList.isPresent()) {
            String pmExtension = null;
            org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Location location = null;
            org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Direction direction = null;
            if (input.getPmExtension() != null) {
                pmExtension = input.getPmExtension();
            }
            if (input.getLocation() != null) {
                location = input.getLocation();
            }
            if (input.getDirection() != null) {
                direction = input.getDirection();
            }
            //PmNamesEnum pmName = null;


            measurements = extractWantedMeasurements(currentPmList.orElseThrow(),
                    ResourceTypeEnum.forValue(input.getResourceType().getIntValue()),
                    input,
                    PmGranularity.forValue(input.getGranularity().getIntValue()),
                    //pmName
                    null,
                    pmExtension,
                    location,
                    direction);

        } else {
            LOG.info("Device PM Data for node: {} is not available", input.getNodeId());
        }

        return measurements;
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
    public static Map<String, List<GetPmOutput>> pmFetchAll(GetPmInput input,
                                                            DeviceTransactionManager deviceTransactionManager) {
        LOG.info("Getting PM Data for 1.2.1 NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
                input.getResourceType(), input.getResourceIdentifier());
        DataObjectIdentifier<CurrentPmlist> currentPmsIID = DataObjectIdentifier.builder(CurrentPmlist.class).build();
        Optional<CurrentPmlist> currentPmList;
        currentPmList = deviceTransactionManager
                .getDataFromDevice(input.getNodeId(), LogicalDatastoreType.OPERATIONAL, currentPmsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (currentPmList.isPresent()) {
            return getPms(currentPmList.orElseThrow(), input); //Modernizer forces me to use orElseThrow even though
                                                               //the condition is already checked.
        } else {
            LOG.error("Unable to get CurrentPmList for node {}", input.getNodeId());
        }
        return Map.of();
    }

    /*
     * Retrieves a map on the form interfaceName -> List<GetPmOutput>
     */
    private static Map<String, List<GetPmOutput>> getPms(CurrentPmlist currentPmList, GetPmInput input) {
        Map<String, List<GetPmOutput>> returnMap = new HashMap<>();
        Map<CurrentPmKey, CurrentPm> currentPmEntryList = currentPmList.nonnullCurrentPm();

        for (Map.Entry<CurrentPmKey, CurrentPm> entry : currentPmEntryList.entrySet()) {
            List<GetPmOutput> returnList = new ArrayList<>();
            CurrentPm cpe = entry.getValue();
            String interfaceName = getInterfaceName(cpe);
            ResourceIdentifier rsId = new ResourceIdentifierBuilder()
                    .setResourceName(interfaceName)
                    .build();
            Collection<CurrentPm> currentPMList = currentPmList.nonnullCurrentPm().values();
            for (CurrentPm currentPm:currentPMList) {
                GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
                List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce
                        .olm.rev210618.get.pm.output.Measurements> measurements = getMeasurements(currentPm);
                if (!measurements.isEmpty()) {
                    pmOutputBuilder.setNodeId(input.getNodeId()).setResourceType(input.getResourceType())
                            .setResourceIdentifier(rsId).setGranularity(input
                                    .getGranularity())
                            .setMeasurements(measurements);
                    returnList.add(pmOutputBuilder.build());
                } else {
                    LOG.info("No measurements found for node {} on type {} for granularity {}",
                            input.getNodeId(),
                            currentPm.getResource().getResourceType().getType().getName(),
                            input.getGranularity());
                }
            }
            returnMap.put(interfaceName,returnList);
        }
        return returnMap;
    }

    //Uses input only for throwing the error. Could and should be removed.
    private static List<org.opendaylight.yang.gen.v1.http.org.opendaylight
            .transportpce.olm.rev210618.get.pm.output.Measurements>
        getMeasurements(CurrentPm currentPm) {
        List<Measurements> pmMeasurements = currentPm.getMeasurements();
        List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
                extractedMeasurements = new ArrayList<>();
        if (pmMeasurements != null) {
            for (Measurements measure : pmMeasurements) {
                MeasurementsBuilder measurement = new MeasurementsBuilder();
                measurement.setPmparameterName(measure.getMeasurement().getPmParameterName().getType().toString());
                PmDataType pmValue = measure.getMeasurement().getPmParameterValue();
                if (pmValue != null) {
                    if (pmValue.getDecimal64() != null) {
                        measurement.setPmparameterValue(pmValue.getDecimal64()
                                .toString());
                    } else if (pmValue.getUint64() != null) {
                        measurement.setPmparameterValue(pmValue.getUint64().toString());
                    }
                    extractedMeasurements.add(measurement.build());
                }
            }
        }
        return extractedMeasurements;
    }

    private static String getInterfaceName(CurrentPm pm) {
        if (pm.getResource().getResourceType().getType().compareTo(ResourceTypeEnum.Interface) == 0) {
            return ((Interface) pm.getResource().getResource().getResource()).getInterfaceName();
        }
        return null;
    }

    private static
        List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
            extractWantedMeasurements(CurrentPmlist currentPmList, ResourceTypeEnum resourceTypeEnum,
            GetPmInput input,PmGranularity pmGranularity, PmNamesEnum pmNamesEnum,
            String extension, org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Location
            location, org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.Direction direction) {
        List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
            measurements = new ArrayList<>();
        for (CurrentPm pm : currentPmList.nonnullCurrentPm().values()) {
            ResourceTypeEnum currentResourceType = pm.getResource().getResourceType().getType();
            if (currentResourceType.equals(resourceTypeEnum)) {
                Resource currentResource = pm.getResource().getResource().getResource();
                PmGranularity currentGranularity = pm.getGranularity();
                boolean isWantedPowerMeasure = isWantedPowerMeasure(currentResource, currentGranularity,
                        resourceTypeEnum, input.getResourceIdentifier(), pmGranularity);
                if (isWantedPowerMeasure) {
                    measurements.addAll(extractMeasurements(pm.getMeasurements(),pmNamesEnum,
                            extension,location,direction, input.getNodeId()));
                }
            }
        }
        return measurements;
    }

    private static
         List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
            extractMeasurements(List<Measurements> measurementsFromDevice,
                                PmNamesEnum pmNamesEnum,
                                String extension,
                                Location location,
                                Direction direction,
                                String nodeId) {
        List<Measurements> pmMeasurements = measurementsFromDevice;
        Stream<Measurements> measurementStream = pmMeasurements.stream();
        if (pmNamesEnum != null) {
            LOG.info("pm name is not null {} {} {}",pmNamesEnum,pmNamesEnum.getName(),pmMeasurements.get(0)
                .getMeasurement().getPmParameterName().getType());
            measurementStream = measurementStream.filter(measure -> measure.getMeasurement().getPmParameterName()
                .getType().getName().equals(pmNamesEnum.getName()));
        }
        if (extension != null) {
            LOG.info("extension is not null {}",extension);
            measurementStream = measurementStream.filter(measure -> measure.getMeasurement()
                    .getPmParameterName().getType().toString().equals("vendorExtension")
                    && measure.getMeasurement().getPmParameterName().getExtension().equals(extension));
        }
        if (location != null) {
            LOG.info("location is not null {}",location);
            measurementStream = measurementStream.filter(measure -> measure.getMeasurement().getLocation().getName()
                .equals(location.getName()));
        }
        if (direction != null) {
            LOG.info("direction is not null {}",direction);
            measurementStream = measurementStream.filter(measure -> measure.getMeasurement().getDirection().getName()
                .equals(direction.getName()));
        }
        List<Measurements> filteredMeasurements = measurementStream.collect(Collectors.toList());
        List<org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements>
            extractedMeasurements = new ArrayList<>();
        for (Measurements measure : filteredMeasurements) {
            MeasurementsBuilder measurement = new MeasurementsBuilder();
            measurement.setPmparameterName(measure.getMeasurement().getPmParameterName().getType().toString());
            org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmDataType
                    pmValue = measure.getMeasurement().getPmParameterValue();
            if (pmValue != null) {
                if (pmValue.getDecimal64() != null) {
                    measurement.setPmparameterValue(pmValue.getDecimal64()
                            .toString());
                } else if (pmValue.getUint64() != null) {
                    measurement.setPmparameterValue(pmValue.getUint64().toString());
                }
            } else {
                throw new RuntimeException("No ParameterValue found for node " + nodeId
                        + " on parameter: " + measurement.getPmparameterName());
            }
            extractedMeasurements.add(measurement.build());
        }
        return extractedMeasurements;
    }

    private static boolean isWantedPowerMeasure(Resource resource, PmGranularity granularity,
            ResourceTypeEnum resourceTypeEnum, ResourceIdentifier wantedResourceIdentifier,
            PmGranularity pmGranularity) {
        boolean identifiersAreEqual = compareResourceIdentifiers(resource, resourceTypeEnum,
                wantedResourceIdentifier);
        return identifiersAreEqual && granularity != null && granularity.equals(pmGranularity);
    }

    private static boolean compareResourceIdentifiers(Resource resource, ResourceTypeEnum resourceTypeEnum,
            ResourceIdentifier wantedResourceIdentifier) {
        switch (resourceTypeEnum) {
            case CircuitPack:
                Optional<CircuitPack> circuitPackOptional = tryCastToParticularResource(CircuitPack.class, resource);
                return circuitPackOptional.flatMap(
                    circuitPack -> Optional.ofNullable(circuitPack.getCircuitPackName()))
                        .map(circuitPackName -> circuitPackName.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case Connection:
                Optional<Connection> connectionOptional = tryCastToParticularResource(Connection.class, resource);
                return connectionOptional.flatMap(
                    connection -> Optional.ofNullable(connection.getConnectionNumber()))
                        .map(connectionNumber -> connectionNumber.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case Degree:
                Optional<Degree> degreeOptional = tryCastToParticularResource(Degree.class, resource);
                return degreeOptional.flatMap(
                    degree -> Optional.ofNullable(degree.getDegreeNumber()))
                        .flatMap(degreeInteger -> Optional.of(degreeInteger.toString()))
                        .map(degreeNumberAsString ->
                                degreeNumberAsString.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case Interface:
                Optional<Interface> interfaceOptional = tryCastToParticularResource(Interface.class, resource);
                return interfaceOptional.flatMap(
                    interfaceResource -> Optional.ofNullable(interfaceResource.getInterfaceName()))
                        .map(interfaceName -> interfaceName.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case InternalLink:
                Optional<InternalLink> internalLinkOptional = tryCastToParticularResource(InternalLink.class, resource);
                return internalLinkOptional.flatMap(
                    internalLink -> Optional.ofNullable(internalLink.getInternalLinkName()))
                        .map(internalLinkName -> internalLinkName.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case PhysicalLink:
                Optional<PhysicalLink> physicalLinkOptional = tryCastToParticularResource(PhysicalLink.class, resource);
                return physicalLinkOptional.flatMap(
                    physicalLink -> Optional.ofNullable(physicalLink.getPhysicalLinkName()))
                        .map(physicalLinkName -> physicalLinkName.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case Service:
                Optional<Service> serviceOptional = tryCastToParticularResource(Service.class, resource);
                return serviceOptional.flatMap(
                    service -> Optional.ofNullable(service.getServiceName()))
                        .map(serviceName -> serviceName.equals(wantedResourceIdentifier.getResourceName()))
                    .orElse(false);
            case Shelf:
                Optional<Shelf> shelfOptional = tryCastToParticularResource(Shelf.class, resource);
                return shelfOptional.flatMap(
                    shelf -> Optional.ofNullable(shelf.getShelfName()))
                        .map(shelfName -> shelfName.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case SharedRiskGroup:
                Optional<Srg> sharedRiskGroupOptional = tryCastToParticularResource(Srg.class, resource);
                return sharedRiskGroupOptional.flatMap(
                    sharedRiskGroup -> Optional.ofNullable(sharedRiskGroup.getSrgNumber()))
                        .flatMap(sharedRiskGroupNumberInteger -> Optional.of(sharedRiskGroupNumberInteger.toString()))
                        .map(srgNumberAsString -> srgNumberAsString.equals(wantedResourceIdentifier.getResourceName()))
                        .orElse(false);
            case Port:
                Optional<Port> portContainerOptional = tryCastToParticularResource(Port.class, resource);
                return portContainerOptional.flatMap(
                    portContainer -> Optional.ofNullable(portContainer.getPort()))
                        .map(port -> {
                            String portName = port.getPortName();
                            String circuitPackName = port.getCircuitPackName();
                            return portName != null && circuitPackName != null
                                    && portName.equals(wantedResourceIdentifier.getResourceName())
                                    && circuitPackName.equals(wantedResourceIdentifier.getCircuitPackName());
                        })
                        .orElse(false);
            default:
                LOG.warn("Unknown resource type {}", resourceTypeEnum);
                return false;
        }
    }

    public static boolean setSpanLoss(String realNodeId, String interfaceName, BigDecimal spanLoss,
                                      String direction, OpenRoadmInterfaces openRoadmInterfaces) {
        RatioDB spanLossRx;
        RatioDB spanLossTx;

        try {
            Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface>
                    interfaceObject;
            interfaceObject = openRoadmInterfaces.getInterface(realNodeId, interfaceName);
            if (interfaceObject.isPresent()) {
                InterfaceBuilder interfaceBuilder = new InterfaceBuilder(interfaceObject.orElseThrow());
                OtsBuilder otsBuilder = new OtsBuilder();
                org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface
                        intf = interfaceObject.orElseThrow();
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
        } catch (OpenRoadmInterfaceException e) {
            LOG.error("Failed to set spanloss on node {}, OpenRoadmInterfaceException occured: {}",
                    realNodeId,
                    e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Resource> Optional<T> tryCastToParticularResource(Class<T> resourceClass,
            Resource resource) {
        if (resource == null) {
            LOG.warn("Resource is null.");
        } else if (! resourceClass.isInstance(resource)) {
            LOG.warn("Resource implement different type than expected. Expected {}, actual {}.",
                    resourceClass.getSimpleName(), resource.getClass().getSimpleName());
        } else {
            return Optional.of((T) resource);
        }
        return Optional.empty();
    }
}

