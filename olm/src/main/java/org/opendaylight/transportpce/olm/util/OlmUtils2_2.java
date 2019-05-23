/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Direction;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.Location;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.internal.links.InternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.internal.links.InternalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnections;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.RoadmConnectionsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.org.openroadm.device.container.org.openroadm.device.SharedRiskGroupKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.physical.links.PhysicalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.physical.links.PhysicalLinkKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.shelves.Shelves;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.shelves.ShelvesKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev181019.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.get.pm.input.ResourceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmUtils2_2 {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils2_2.class);

    /**
     * This method retrieves list of current PMs for given nodeId,
     * resourceType, resourceName and Granularity.Currently vendorExtentions
     * are excluded but can be added back based on requirement
     *
     * <p>
     * 1. pmFetch This operation traverse through current PM list and gets PM for
     * given NodeId and Resource name
     *
     * @param input
     *            Input parameter from the olm yang model get-pm rpc
     * @param deviceTransactionManager
     *            Device tx manager
     *
     * @return Result of the request list of PM readings
     */
    public static GetPmOutputBuilder pmFetch(GetPmInput input, DeviceTransactionManager deviceTransactionManager) {
        LOG.debug("Getting PM Data for 2.2 NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
            input.getResourceType(), input.getResourceIdentifier());
        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
        InstanceIdentifier<?> resourceKeyIID =
            findClassKeyIdentifiers(input.getResourceType(), input.getResourceIdentifier());
        ResourceTypeEnum enumVal = ResourceTypeEnum.forValue(input.getResourceType().getIntValue());
        CurrentPmEntryKey entryKey = new CurrentPmEntryKey(resourceKeyIID, enumVal,"3");
        //LOG.info("Key is {}",entryKey);
        InstanceIdentifier<CurrentPmEntry> currentPmsEntryIID = InstanceIdentifier.create(CurrentPmList.class)
            .child(CurrentPmEntry.class, entryKey);
        //LOG.info("This is the iid {}", currentPmsEntryIID);
        Optional<CurrentPmEntry> currentPmEntry;
        currentPmEntry = deviceTransactionManager
            .getDataFromDevice(input.getNodeId(), LogicalDatastoreType.OPERATIONAL, currentPmsEntryIID,
                Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (currentPmEntry.isPresent()) {
            //Apply filters in the current PM list
            //LOG.info("Current PM list exists {}, {}",currentPmEntry.get(),currentPmEntry.get().getCurrentPm().size());
            List<CurrentPm> currentPMList = currentPmEntry.get().getCurrentPm();
            //LOG.info("Current pm list has many {} elements", currentPMList.size());
            Stream<CurrentPm> currentPMStream = currentPMList.stream();
            if (input.getPmNameType() != null) {
                currentPMStream = currentPMStream.filter(pm -> pm.getType()
                    .equals(PmNamesEnum.forValue(input.getPmNameType().getIntValue())));
            }
            if (input.getPmExtension() != null) {
                currentPMStream = currentPMStream.filter(pm -> pm.getExtension()
                    .equals(input.getPmExtension()));
            }
            if (input.getLocation() != null) {
                currentPMStream = currentPMStream.filter(pm -> pm.getLocation()
                    .equals(Location.forValue(input.getLocation().getIntValue())));
            }
            if (input.getDirection() != null) {
                currentPMStream = currentPMStream.filter(pm -> pm.getDirection()
                    .equals(Direction.forValue((input.getDirection().getIntValue()))));
            }
            List<CurrentPm> filteredPMs = currentPMStream.collect(Collectors.toList());
            List<Measurements> measurements = extractWantedMeasurements(filteredPMs,input.getGranularity());
            if (measurements.isEmpty()) {
                LOG.error("No Matching PM data found for node: {}, " + "resource type: {},"
                        + " resource name: {}, pm type: {}, extention: {}"
                        + ", location: {} and direction: {}",
                    input.getNodeId(), input.getResourceType(),
                    getResourceIdentifierAsString(input.getResourceIdentifier()),
                    input.getPmNameType(),input.getPmExtension(),input.getLocation(),
                    input.getDirection());
            } else {
                pmOutputBuilder.setNodeId(input.getNodeId()).setResourceType(input.getResourceType())
                    .setResourceIdentifier(input.getResourceIdentifier()).setGranularity(input.getGranularity())
                    .setMeasurements(measurements);
                LOG.error("PM data found successfully for node: {}, " + "resource type: {},"
                        + " resource name: {}, pm type: {}, extention: {}"
                        + ", location: {} and direction: {}",
                    input.getNodeId(), input.getResourceType(),
                    getResourceIdentifierAsString(input.getResourceIdentifier()),
                    input.getPmNameType(),input.getPmExtension(),input.getLocation(),
                    input.getDirection());
            }

        } else {
            LOG.info("Device PM Data for node: {}, resource type {} and resource name {}"
                + "is not available", input.getNodeId());
        }

        return pmOutputBuilder;
    }

    private static String getResourceIdentifierAsString(ResourceIdentifier resourceIdentifier) {
        if (Strings.isNullOrEmpty(resourceIdentifier.getCircuitPackName())) {
            return resourceIdentifier.getResourceName();
        } else {
            return resourceIdentifier.getResourceName() + ", circuit pack name: "
                + resourceIdentifier.getCircuitPackName();
        }
    }

    private static List<Measurements> extractWantedMeasurements(List<CurrentPm> currentPmList,
        org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity wantedGranularity) {
        List<Measurements> olmMeasurements = new ArrayList<>();
        for (CurrentPm pm : currentPmList) {
            for (Measurement measurements: pm.getMeasurement()) {
                if (measurements.getGranularity().equals(org.opendaylight.yang.gen.v1.http
                    .org.openroadm.pm.types.rev171215.PmGranularity.forValue(wantedGranularity.getIntValue()))) {
                    MeasurementsBuilder pmMeasureBuilder = new MeasurementsBuilder();
                    pmMeasureBuilder.setPmparameterName(pm.getType().getName());
                    //LOG.info("Parameter value is: {} ",measurements.getPmParameterValue().getUint64());
                    pmMeasureBuilder.setPmparameterValue(measurements.getPmParameterValue().getUint64().toString());
                    olmMeasurements.add(pmMeasureBuilder.build());
                }
            }
        }
        return olmMeasurements;
    }

    private static InstanceIdentifier<?> findClassKeyIdentifiers(org.opendaylight.yang.gen.v1.http
        .org.openroadm.resource.types.rev161014.ResourceTypeEnum wantedResourceType,
        ResourceIdentifier wantedResourceIdentifier) {
        switch (wantedResourceType) {
            case Device:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class);
            case Degree:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                        .org.openroadm.device.container.org.openroadm.device.Degree.class,
                        new org.opendaylight.yang.gen.v1.http
                            .org.openroadm.device.rev181019.org.openroadm.device.container
                            .org.openroadm.device.DegreeKey(
                                Integer.parseInt(wantedResourceIdentifier.getResourceName()))
                    );
            case SharedRiskGroup:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(SharedRiskGroup.class,
                        new SharedRiskGroupKey(Integer.parseInt(wantedResourceIdentifier.getResourceName())));
            case Connection:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(RoadmConnections.class, new RoadmConnectionsKey(wantedResourceIdentifier.getResourceName()));
            case CircuitPack:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(wantedResourceIdentifier.getResourceName()));
            case Port:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(CircuitPacks.class, new CircuitPacksKey(wantedResourceIdentifier.getCircuitPackName()))
                    .child(Ports.class, new PortsKey(wantedResourceIdentifier.getResourceName()));
            case Interface:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(Interface.class, new InterfaceKey(wantedResourceIdentifier.getResourceName()));
            case InternalLink:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(InternalLink.class, new InternalLinkKey(wantedResourceIdentifier.getResourceName()));
            case PhysicalLink:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(PhysicalLink.class, new PhysicalLinkKey(wantedResourceIdentifier.getResourceName()));
            case Shelf:
                return InstanceIdentifier.create(org.opendaylight.yang.gen.v1.http.org.openroadm
                    .device.rev181019.org.openroadm.device.container.OrgOpenroadmDevice.class)
                    .child(Shelves.class, new ShelvesKey(wantedResourceIdentifier.getResourceName()));
            default:
                LOG.error("Unknown resource type {}", wantedResourceType);
                return null;
        }
    }


}
