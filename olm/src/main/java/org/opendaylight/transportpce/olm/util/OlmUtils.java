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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.CurrentPmlist;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.current.pm.Measurements;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.rev161014.currentpmlist.CurrentPm;
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
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.get.pm.input.ResourceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.network.topology.topology.topology.types.TopologyNetconf;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm.output.MeasurementsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmUtils {

    private static final Logger LOG = LoggerFactory.getLogger(OlmUtils.class);
    private static long DATABROKER_READ_TIMEOUT_SECONDS = 120;
    private static final InstanceIdentifier<Topology> NETCONF_TOPO_IID =
            InstanceIdentifier
                    .create(NetworkTopology.class)
                    .child(Topology.class,
                            new TopologyKey(new TopologyId(TopologyNetconf.QNAME.getLocalName())));

    /**
     * This static method returns the port mapping {@link Nodes} for node.
     *
     * @param nodeId
     *            Unique identifier for the mounted netconf node
     * @param db
     *            Databroker used to read data from data store.
     * @return {@link Nodes } from portMapping for given nodeId
     */
    public static Optional<Nodes> getNode(String nodeId, DataBroker db) {
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
                .child(Nodes.class, new NodesKey(nodeId));
        try (ReadOnlyTransaction readTransaction = db.newReadOnlyTransaction()) {
            return readTransaction.read(LogicalDatastoreType.CONFIGURATION, nodesIID)
                    .get(DATABROKER_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS).toJavaUtil();
        } catch (InterruptedException | ExecutionException | TimeoutException ex) {
            LOG.info("Unable to read Portmapping for nodeId {}", nodeId, ex);
            return Optional.empty();
        }
    }

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
        LOG.info("Getting PM Data for NodeId: {} ResourceType: {} ResourceName: {}", input.getNodeId(),
                input.getResourceType(), input.getResourceIdentifier());
        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
        InstanceIdentifier<CurrentPmlist> currentPmsIID = InstanceIdentifier.create(CurrentPmlist.class);
        Optional<CurrentPmlist> currentPmList;

        currentPmList = deviceTransactionManager
                .getDataFromDevice(input.getNodeId(), LogicalDatastoreType.OPERATIONAL, currentPmsIID,
                        Timeouts.DEVICE_READ_TIMEOUT, Timeouts.DEVICE_READ_TIMEOUT_UNIT);
        if (currentPmList.isPresent()) {
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm.output
                    .Measurements> measurements = extractWantedMeasurements(currentPmList.get(),
                    input.getResourceType(), input.getResourceIdentifier(), input.getGranularity());
            if (measurements.isEmpty()) {
                LOG.error("No Matching PM data found for node: {}, " + "resource type: {}, resource name: {}",
                        input.getNodeId(), input.getResourceType(),
                        getResourceIdentifierAsString(input.getResourceIdentifier()));
            } else {
                pmOutputBuilder.setNodeId(input.getNodeId()).setResourceType(input.getResourceType())
                        .setResourceIdentifier(input.getResourceIdentifier()).setGranularity(input.getGranularity())
                        .setMeasurements(measurements);
                LOG.info("PM Data found successfully for node: {}, resource type: {}, " + "resource name {}",
                        input.getNodeId(), input.getResourceType(),
                        getResourceIdentifierAsString(input.getResourceIdentifier()));
            }

        } else {
            LOG.info("Device PM Data for node: {} is not available", input.getNodeId());
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

    private static List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm
            .output.Measurements> extractWantedMeasurements(CurrentPmlist currentPmList,
            ResourceTypeEnum wantedResourceType, ResourceIdentifier wantedResourceIdentifier,
            PmGranularity wantedGranularity) {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm
            .output.Measurements> measurements = new ArrayList<>();
        for (CurrentPm pm : currentPmList.getCurrentPm()) {
            ResourceTypeEnum currentResourceType = pm.getResource().getResourceType().getType();
            if (currentResourceType.equals(wantedResourceType)) {
                Resource currentResource = pm.getResource().getResource().getResource();
                PmGranularity currentGranularity = pm.getGranularity();
                boolean isWantedPowerMeasure = isWantedPowerMeasure(currentResource, currentGranularity,
                        wantedResourceType, wantedResourceIdentifier, wantedGranularity);
                if (isWantedPowerMeasure) {
                    measurements.addAll(extractMeasurements(pm.getMeasurements()));
                }
            }
        }
        return measurements;
    }

    private static List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm
            .output.Measurements> extractMeasurements(List<Measurements> measurementsFromDevice) {
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm.output.Measurements>
            extractedMeasurements = new ArrayList<>();
        for (Measurements measure : measurementsFromDevice) {
            MeasurementsBuilder measurement = new MeasurementsBuilder();
            if (!measure.getMeasurement().getPmParameterName().getType().equals(PmNamesEnum.VendorExtension)) {
                measurement.setPmparameterName(measure.getMeasurement().getPmParameterName().getType().toString());
            } else {
                measurement.setPmparameterName(measure.getMeasurement().getPmParameterName().getExtension());
            }

            if (measure.getMeasurement().getPmParameterValue().getDecimal64() != null) {
                measurement.setPmparameterValue(measure.getMeasurement().getPmParameterValue().getDecimal64()
                    .toPlainString());
            } else if (measure.getMeasurement().getPmParameterValue().getUint64() != null) {
                measurement.setPmparameterValue(measure.getMeasurement().getPmParameterValue().getUint64().toString());
            }
            extractedMeasurements.add(measurement.build());
        }
        return extractedMeasurements;
    }

    private static boolean isWantedPowerMeasure(Resource resource, PmGranularity granularity,
            ResourceTypeEnum wantedResourceType, ResourceIdentifier wantedResourceIdentifier,
            PmGranularity wantedGranularity) {
        boolean identifiersAreEqual = compareResourceIdentifiers(resource, wantedResourceType,
                wantedResourceIdentifier);
        return identifiersAreEqual && granularity != null && granularity.equals(wantedGranularity);
    }

    private static boolean compareResourceIdentifiers(Resource resource, ResourceTypeEnum wantedResourceType,
            ResourceIdentifier wantedResourceIdentifier) {
        switch (wantedResourceType) {
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
                            return portName != null
                                    && circuitPackName != null
                                    && portName.equals(wantedResourceIdentifier.getResourceName())
                                    && circuitPackName.equals(wantedResourceIdentifier.getCircuitPackName());
                        })
                        .orElse(false);
            default:
                LOG.warn("Unknown resource type {}", wantedResourceType);
                return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Resource> Optional<T> tryCastToParticularResource(Class<T> resourceClass,
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
