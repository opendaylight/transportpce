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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface;
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
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.CurrentPmList;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.group.CurrentPm;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntry;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.list.CurrentPmEntryKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev181019.current.pm.val.group.Measurement;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.Direction;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.Location;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.olm.get.pm.input.ResourceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OlmUtils221 {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils221.class);

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
        LOG.info("Getting PM Data for 2.2.1 NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
                input.getResourceType(), input.getResourceIdentifier());

        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();

        InstanceIdentifier<?> resourceKeyIID =
                findClassKeyIdentifiers(input.getResourceType(), input.getResourceIdentifier());
        CurrentPmEntryKey resourceKey = new CurrentPmEntryKey(resourceKeyIID,
                convertResourceTypeEnum(input.getResourceType()),"");
        InstanceIdentifier<CurrentPmList> iidCurrentPmList = InstanceIdentifier.create(CurrentPmList.class);

        Optional<CurrentPmList> currentPmListOpt = deviceTransactionManager.getDataFromDevice(input.getNodeId(),
                LogicalDatastoreType.OPERATIONAL, iidCurrentPmList, Timeouts.DEVICE_READ_TIMEOUT,
                Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (currentPmListOpt.isPresent()) {
            CurrentPmList currentPmList = currentPmListOpt.get();
            @NonNull
            Map<CurrentPmEntryKey,CurrentPmEntry> currentPmEntryList = currentPmList.nonnullCurrentPmEntry();
            LOG.info("Current PM list exists for node {} and contains {} entries.", input.getNodeId(),
                    currentPmEntryList.size());
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
                    List<CurrentPm> filteredPMs = currentPMStream.collect(Collectors.toList());
                    List<Measurements> measurements = extractWantedMeasurements(filteredPMs,input.getGranularity());
                    if (measurements.isEmpty()) {
                        LOG.error(
                                "No Matching PM data found for node: {}, resource type: {}, resource name: {}, "
                                        + "pm type: {}, extention: {}, location: {} and direction: {}",
                                input.getNodeId(), input.getResourceType(),
                                getResourceIdentifierAsString(input.getResourceIdentifier()),
                                input.getPmNameType(),input.getPmExtension(),input.getLocation(),
                                input.getDirection());
                    } else {
                        pmOutputBuilder.setNodeId(input.getNodeId()).setResourceType(input.getResourceType())
                                .setResourceIdentifier(input.getResourceIdentifier()).setGranularity(input
                                .getGranularity())
                                .setMeasurements(measurements);
                        LOG.info(
                                "PM data found successfully for node: {}, resource type: {}, resource name: {}, "
                                        + "pm type: {}, extention: {}, location: {} and direction: {}",
                                input.getNodeId(), input.getResourceType(),
                                getResourceIdentifierAsString(input.getResourceIdentifier()),
                                input.getPmNameType(),input.getPmExtension(),input.getLocation(),
                                input.getDirection());
                    }
                }
            }
        } else {
            LOG.error("Unable to get CurrentPmList for node {}", input.getNodeId());
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
            for (Measurement measurements: pm.nonnullMeasurement().values()) {
                if (measurements.getGranularity().getIntValue()
                        == org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev171215.PmGranularity.forValue(
                                wantedGranularity.getIntValue()).getIntValue()) {
                    MeasurementsBuilder pmMeasureBuilder = new MeasurementsBuilder();
                    pmMeasureBuilder.setPmparameterName(pm.getType().name());
                    pmMeasureBuilder.setPmparameterValue(measurements.getPmParameterValue().stringValue());
                    olmMeasurements.add(pmMeasureBuilder.build());
                }
            }
        }
        return olmMeasurements;
    }

    private static InstanceIdentifier<?> findClassKeyIdentifiers(ResourceTypeEnum wantedResourceType,
            ResourceIdentifier wantedResourceIdentifier) {
        switch (wantedResourceType) {
            case Device:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class);
            case Degree:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(Degree.class, new DegreeKey(Uint16.valueOf(wantedResourceIdentifier.getResourceName())));
            case SharedRiskGroup:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(SharedRiskGroup.class,
                                new SharedRiskGroupKey(Uint16.valueOf(wantedResourceIdentifier.getResourceName())));
            case Connection:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(RoadmConnections.class, new RoadmConnectionsKey(wantedResourceIdentifier
                                .getResourceName()));
            case CircuitPack:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(wantedResourceIdentifier.getResourceName()));
            case Port:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(CircuitPacks.class, new CircuitPacksKey(wantedResourceIdentifier.getCircuitPackName()))
                        .child(Ports.class, new PortsKey(wantedResourceIdentifier.getResourceName()));
            case Interface:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(Interface.class, new InterfaceKey(wantedResourceIdentifier.getResourceName()));
            case InternalLink:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(InternalLink.class, new InternalLinkKey(wantedResourceIdentifier.getResourceName()));
            case PhysicalLink:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(PhysicalLink.class, new PhysicalLinkKey(wantedResourceIdentifier.getResourceName()));
            case Shelf:
                return InstanceIdentifier.create(OrgOpenroadmDevice.class)
                        .child(Shelves.class, new ShelvesKey(wantedResourceIdentifier.getResourceName()));
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

    private OlmUtils221() {
    }

}
