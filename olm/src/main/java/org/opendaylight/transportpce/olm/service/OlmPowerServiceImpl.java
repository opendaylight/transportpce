/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.ResponseCodes;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.device.observer.EventSubscriber;
import org.opendaylight.transportpce.common.device.observer.Subscriber;
import org.opendaylight.transportpce.common.mapping.MappingUtils;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.util.OlmUtils;
import org.opendaylight.transportpce.olm.util.OtsPmHolder;
import org.opendaylight.transportpce.olm.util.RoadmLinks;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossBaseOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.CalculateSpanlossCurrentOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.calculate.spanloss.base.output.Spans;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.calculate.spanloss.base.output.SpansBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250325.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmNamesEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.Networks;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.Link;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.networks.network.LinkKey;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.binding.DataObjectIdentifier.WithKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

@Component
public class OlmPowerServiceImpl implements OlmPowerService {
    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerServiceImpl.class);
    private final MappingUtils mappingUtils;
    private final OpenRoadmInterfaces openRoadmInterfaces;
    private final DataBroker dataBroker;
    private final PowerMgmt powerMgmt;
    private final DeviceTransactionManager deviceTransactionManager;
    private final PortMapping portMapping;

    @Activate
    public OlmPowerServiceImpl(@Reference DataBroker dataBroker,
            @Reference PowerMgmt powerMgmt,
            @Reference DeviceTransactionManager deviceTransactionManager,
            @Reference PortMapping portMapping,
            @Reference MappingUtils mappingUtils,
            @Reference OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.powerMgmt = powerMgmt;
        this.portMapping = portMapping;
        this.deviceTransactionManager = deviceTransactionManager;
        this.mappingUtils = mappingUtils;
        this.openRoadmInterfaces = openRoadmInterfaces;
        LOG.debug("OlmPowerServiceImpl Instantiated");
    }

    @Override
    public GetPmOutput getPm(GetPmInput pmInput) {
        GetPmOutputBuilder pmOutputBuilder = new GetPmOutputBuilder();
        if (mappingUtils.getOpenRoadmVersion(pmInput.getNodeId()) == null) {
            return pmOutputBuilder.build();
        }
        OpenroadmNodeVersion nodeVersion;
        switch (mappingUtils.getOpenRoadmVersion(pmInput.getNodeId())) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                nodeVersion = OpenroadmNodeVersion._121;
                break;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                nodeVersion = OpenroadmNodeVersion._221;
                break;
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                nodeVersion = OpenroadmNodeVersion._71;
                break;
            default:
                LOG.error("Unknown device version");
                return pmOutputBuilder.build();
        }
        LOG.info("Now calling get pm data");
        pmOutputBuilder = OlmUtils.pmFetch(pmInput, deviceTransactionManager,
            nodeVersion);
        return pmOutputBuilder.build();
    }

    @Override
    public ServicePowerSetupOutput servicePowerSetup(ServicePowerSetupInput powerSetupInput) {
        Subscriber errorSubscriber = new EventSubscriber();
        ServicePowerSetupOutputBuilder powerSetupOutput = new ServicePowerSetupOutputBuilder();
        boolean successValPowerCalculation = powerMgmt.setPower(powerSetupInput, errorSubscriber);
        if (successValPowerCalculation) {
            powerSetupOutput.setResult(ResponseCodes.SUCCESS_RESULT);
        } else {
            powerSetupOutput.setResult(
                String.format("OLM power setup failed (%s)",
                    errorSubscriber.last(Level.ERROR, "OLM power setup failed due to an unknown error.")
                )
            );
        }
        return powerSetupOutput.build();
    }

    @Override
    public ServicePowerTurndownOutput servicePowerTurndown(
        ServicePowerTurndownInput powerTurndownInput) {

        ServicePowerTurndownOutputBuilder powerTurnDownOutput = new ServicePowerTurndownOutputBuilder();
        // TODO add flag or return failure instead of string
        if (powerMgmt.powerTurnDown(powerTurndownInput)) {
            powerTurnDownOutput.setResult(ResponseCodes.SUCCESS_RESULT);
        } else {
            powerTurnDownOutput.setResult(ResponseCodes.FAILED_RESULT);
        }
        return powerTurnDownOutput.build();
    }

    @Override
    public CalculateSpanlossBaseOutput calculateSpanlossBase(CalculateSpanlossBaseInput spanlossBaseInput) {
        LOG.info("CalculateSpanlossBase Request received for source type {}", spanlossBaseInput.getSrcType());
        List<RoadmLinks> roadmLinks = new ArrayList<>();
        CalculateSpanlossBaseOutputBuilder spanLossBaseBuilder = new CalculateSpanlossBaseOutputBuilder();
        Map<LinkId, BigDecimal> spanLossResult = null;
        //Depending on the source type do the calculation
        switch (spanlossBaseInput.getSrcType()) {
            case Link:
                LOG.debug("Calculate SpanLossBase for a linkId: {}",spanlossBaseInput.getLinkId());
                Link inputLink = getNetworkLinkById(spanlossBaseInput.getLinkId());
                if (inputLink != null) {
                    RoadmLinks roadmLink = new RoadmLinks();
                    roadmLink.setSrcNodeId(inputLink.getSource().getSourceNode().getValue());
                    roadmLink.setSrcTpId(inputLink.getSource().getSourceTp().getValue());
                    roadmLink.setDestNodeId(inputLink.getDestination().getDestNode().getValue());
                    roadmLink.setDestTpid(inputLink.getDestination().getDestTp().getValue());
                    roadmLink.setLinkId(inputLink.getLinkId());
                    roadmLinks.add(roadmLink);
                    spanLossResult = getLinkSpanloss(roadmLinks);
                }
                break;
            case All:
                LOG.info("Do something for all");
                List<Link> networkLinks = getNetworkLinks();
                if (networkLinks.isEmpty()) {
                    LOG.warn("Failed to get links form {} topology.", StringConstants.OPENROADM_TOPOLOGY);
                    return new CalculateSpanlossBaseOutputBuilder().setResult(ResponseCodes.FAILED_RESULT).build();
                }
                //else for all other links
                for (Link link : networkLinks) {
                    Link1 roadmLinkAugmentation = link.augmentation(Link1.class);
                    if (roadmLinkAugmentation == null) {
                        LOG.debug("Missing OpenRoadm link augmentation in link {} from {} topology.",
                            link.getLinkId().getValue(), StringConstants.OPENROADM_TOPOLOGY);
                        continue;
                    }
                    if (OpenroadmLinkType.ROADMTOROADM.equals(roadmLinkAugmentation.getLinkType())) {
                        // Only calculate spanloss for Roadm-to-Roadm links
                        RoadmLinks roadmLink = new RoadmLinks();
                        roadmLink.setSrcNodeId(link.getSource().getSourceNode().getValue());
                        roadmLink.setSrcTpId(link.getSource().getSourceTp().getValue());
                        roadmLink.setDestNodeId(link.getDestination().getDestNode().getValue());
                        roadmLink.setDestTpid(link.getDestination().getDestTp().getValue());
                        roadmLink.setLinkId(link.getLinkId());
                        roadmLinks.add(roadmLink);
                    }
                }
                if (roadmLinks.isEmpty()) {
                    LOG.warn("Topology {} does not have any Roadm-to-Roadm links.", StringConstants.OPENROADM_TOPOLOGY);
                    return new CalculateSpanlossBaseOutputBuilder().setResult(ResponseCodes.FAILED_RESULT).build();
                }
                spanLossResult = getLinkSpanloss(roadmLinks);
                break;
            default:
                LOG.info("Invalid input in request");
        }

        if (spanLossResult != null && !spanLossResult.isEmpty()) {
            spanLossBaseBuilder.setResult(ResponseCodes.SUCCESS_RESULT);
            List<Spans> listSpans = new ArrayList<>();
            Set<Entry<LinkId, BigDecimal>> spanLossResultSet = spanLossResult.entrySet();
            for (Entry<LinkId, BigDecimal> entry : spanLossResultSet) {
                Spans span = new SpansBuilder().setLinkId(entry.getKey()).setSpanloss(entry.getValue().toString())
                    .build();
                listSpans.add(span);
            }
            spanLossBaseBuilder.setSpans(listSpans);
            return spanLossBaseBuilder.build();
        } else {
            LOG.warn("Spanloss calculation failed");
            spanLossBaseBuilder.setResult(ResponseCodes.FAILED_RESULT);
            return spanLossBaseBuilder.build();
        }

    }


    @Override
    public CalculateSpanlossCurrentOutput calculateSpanlossCurrent(CalculateSpanlossCurrentInput input) {
        LOG.info("calculateSpanlossCurrent Request received for all links in network model.");
        List<Link> networkLinks = getNetworkLinks();
        if (networkLinks.isEmpty()) {
            LOG.warn("Failed to get links form {} topology.", StringConstants.OPENROADM_TOPOLOGY);
            return null;
        }
        List<RoadmLinks> roadmLinks = new ArrayList<>();
        for (Link link : networkLinks) {
            Link1 roadmLinkAugmentation = link.augmentation(Link1.class);
            if (roadmLinkAugmentation == null) {
                LOG.debug("Missing OpenRoadm link augmentation in link {} from {} topology.",
                    link.getLinkId().getValue(), StringConstants.OPENROADM_TOPOLOGY);
                continue;
            }
            if (OpenroadmLinkType.ROADMTOROADM.equals(roadmLinkAugmentation.getLinkType())) {
                // Only calculate spanloss for Roadm-to-Roadm links
                RoadmLinks roadmLink = new RoadmLinks();
                roadmLink.setSrcNodeId(link.getSource().getSourceNode().getValue());
                roadmLink.setSrcTpId(link.getSource().getSourceTp().getValue());
                roadmLink.setDestNodeId(link.getDestination().getDestNode().getValue());
                roadmLink.setDestTpid(link.getDestination().getDestTp().getValue());
                roadmLinks.add(roadmLink);
            }
        }

        if (roadmLinks.isEmpty()) {
            LOG.warn("Topology {} does not have any Roadm-to-Roadm links.", StringConstants.OPENROADM_TOPOLOGY);
            return null;
        }

        Map<LinkId, BigDecimal> spanLossResult = getLinkSpanloss(roadmLinks);
        CalculateSpanlossCurrentOutputBuilder spanLossCurrentBuilder = new CalculateSpanlossCurrentOutputBuilder();
        if (spanLossResult != null && !spanLossResult.isEmpty()) {
            spanLossCurrentBuilder.setResult(ResponseCodes.SUCCESS_RESULT);
            return spanLossCurrentBuilder.build();
        } else {
            LOG.error("Spanloss Current calculation failed");
            spanLossCurrentBuilder.setResult(ResponseCodes.FAILED_RESULT);
            return spanLossCurrentBuilder.build();
        }
    }

    @Override
    public ServicePowerResetOutput servicePowerReset(ServicePowerResetInput input) {
        // TODO
        return null;
    }

    private List<Link> getNetworkLinks() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY));

        DataObjectIdentifier<Network1> networkIID = DataObjectIdentifier.builder(Networks.class)
                .child(Network.class, overlayTopologyKey)
                .augmentation(Network1.class)
                .build();
        Optional<Network1> networkOptional;
        try (ReadTransaction rtx = this.dataBroker.newReadOnlyTransaction()) {
            //TODO change to constant from Timeouts class when it will be merged.
            networkOptional = rtx.read(LogicalDatastoreType.CONFIGURATION, networkIID).get(Timeouts.DATASTORE_READ,
                TimeUnit.MILLISECONDS);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Read of {} topology failed", StringConstants.OPENROADM_TOPOLOGY, e);
            return Collections.emptyList();
        }

        if (! networkOptional.isPresent()) {
            LOG.warn("Network augmentation with links data is not present in {} topology.",
                    StringConstants.OPENROADM_TOPOLOGY);

            return Collections.emptyList();
        }

        @Nullable Map<LinkKey, Link> networkLinks = networkOptional.orElseThrow().getLink();
        if ((networkLinks == null) || networkLinks.isEmpty()) {
            LOG.warn("Links are not present in {} topology.", StringConstants.OPENROADM_TOPOLOGY);
            return Collections.emptyList();
        }
        return new ArrayList<>(networkLinks.values());
    }

    /**
     * This method retrieves OTS PM from current PM list by nodeId and TPId:
     *
     * <p>Steps:
     *
     * <p>1. Get OTS interface name from port mapping by TPId
     *
     * <p>2. Call getPm RPC to get OTS PM
     *
     * @param nodeId Node-id of the NE.
     * @param tpID Termination point Name.
     * @param pmName PM name which need to be retrieved
     * @return reference to OtsPmHolder
     */
    private OtsPmHolder getPmMeasurements(String nodeId, String tpID, String pmName) {
        String realNodeId = getRealNodeId(nodeId);
        Mapping mapping = portMapping.getMapping(realNodeId, tpID);
        if (mapping == null) {
            return null;
        }
        GetPmInput getPmInput = new GetPmInputBuilder().setNodeId(realNodeId)
            .setResourceType(ResourceTypeEnum.Interface)
            .setResourceIdentifier(
                new ResourceIdentifierBuilder().setResourceName(mapping.getSupportingOts()).build())
            .setPmNameType(PmNamesEnum.valueOf(pmName))
            .setGranularity(PmGranularity._15min)
            .build();
        GetPmOutput otsPmOutput = getPm(getPmInput);

        if (otsPmOutput == null || otsPmOutput.getMeasurements() == null) {
            LOG.info("OTS PM not found for NodeId: {} TP Id:{} PMName:{}", realNodeId, tpID, pmName);
            return null;
        }
        try {
            for (Measurements measurement : otsPmOutput.getMeasurements()) {
                if (pmName.equalsIgnoreCase(measurement.getPmparameterName())) {
                    return new OtsPmHolder(pmName, Double.parseDouble(measurement.getPmparameterValue()),
                        mapping.getSupportingOts());
                }
            }
        } catch (NumberFormatException e) {
            LOG.warn("Unable to get PM for NodeId: {} TP Id:{} PMName:{}", realNodeId, tpID, pmName, e);
        }
        return null;
    }

    /**
     * This method Sets Spanloss on A-End and Z-End OTS interface:
     *
     * <p>Steps:
     *
     * <p>1. Read existing interface details
     *
     * <p>2. Set spanloss
     *
     * @param nodeId nodeId of NE on which spanloss need to be updated
     * @param interfaceName OTS interface for NE on which spanloss is cacluated
     * @param spanLoss calculated spanloss value
     * @param direction for which spanloss is calculated.It can be either Tx or Rx
     * @return true/false
     */
    private boolean setSpanLoss(String nodeId, String interfaceName, BigDecimal spanLoss, String direction) {
        String realNodeId = getRealNodeId(nodeId);
        try {
            LOG.info("Setting Spanloss in device for {}, InterfaceName: {}", realNodeId, interfaceName);
            if (mappingUtils.getOpenRoadmVersion(realNodeId)
                .equals(StringConstants.OPENROADM_DEVICE_VERSION_1_2_1)) {
                RatioDB spanLossRx;
                RatioDB spanLossTx;

                Optional<Interface> interfaceObject;
                interfaceObject = openRoadmInterfaces.getInterface(realNodeId, interfaceName);
                if (interfaceObject.isPresent()) {
                    InterfaceBuilder interfaceBuilder = new InterfaceBuilder(interfaceObject.orElseThrow());
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
                    if (direction.equals("TX")) {
                        otsBuilder.setSpanLossTransmit(new RatioDB(Decimal64.valueOf(spanLoss)));
                        otsBuilder.setSpanLossReceive(spanLossRx);
                    } else {
                        otsBuilder
                            .setSpanLossTransmit(spanLossTx)
                            .setSpanLossReceive(new RatioDB(Decimal64.valueOf(spanLoss)));
                    }
                    interfaceBuilder.addAugmentation(intf1Builder.setOts(otsBuilder.build()).build());
                    openRoadmInterfaces.postInterface(realNodeId,interfaceBuilder);
                    LOG.info("Spanloss Value update completed successfully");
                    return true;
                } else {
                    LOG.error("Interface not found for nodeId: {} and interfaceName: {}", nodeId, interfaceName);
                    return false;
                }
            } else if (mappingUtils.getOpenRoadmVersion(realNodeId)
                .equals(StringConstants.OPENROADM_DEVICE_VERSION_2_2_1)) {

                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.RatioDB spanLossRx;
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.types
                    .rev181019.RatioDB spanLossTx;
                Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019
                    .interfaces.grp.Interface> interfaceObject =
                        openRoadmInterfaces.getInterface(realNodeId, interfaceName);
                if (interfaceObject.isPresent()) {
                    org.opendaylight.yang.gen.v1.http.org.openroadm.device
                        .rev181019.interfaces.grp.InterfaceBuilder interfaceBuilder =
                        new org.opendaylight.yang.gen.v1.http.org.openroadm.device
                            .rev181019.interfaces.grp.InterfaceBuilder(interfaceObject.orElseThrow());
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                        .transport.interfaces.rev181019.ots.container.OtsBuilder otsBuilder =
                        new org.opendaylight.yang.gen.v1.http.org.openroadm
                            .optical.transport.interfaces.rev181019.ots.container.OtsBuilder();
                    org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev181019.interfaces.grp.Interface intf =
                        interfaceObject.orElseThrow();
                    if (intf.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                        .transport.interfaces.rev181019.Interface1.class) != null
                            && intf.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport
                                .interfaces.rev181019.Interface1.class).getOts() != null) {
                        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces
                            .rev181019.ots.container.Ots ots =
                                intf.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                                    .transport.interfaces.rev181019.Interface1.class).getOts();

                        otsBuilder.setFiberType(ots.getFiberType());
                        spanLossRx = ots.getSpanLossReceive();
                        spanLossTx = ots.getSpanLossTransmit();
                    } else {
                        spanLossRx = new org.opendaylight.yang.gen.v1.http.org
                            .openroadm.common.types.rev181019.RatioDB(Decimal64.valueOf(spanLoss));
                        spanLossTx = new org.opendaylight.yang.gen.v1.http.org
                            .openroadm.common.types.rev181019.RatioDB(Decimal64.valueOf(spanLoss));
                    }
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces
                        .rev181019.Interface1Builder intf1Builder =
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                            .transport.interfaces.rev181019.Interface1Builder();
                    if (direction.equals("TX")) {
                        otsBuilder.setSpanLossTransmit(new org.opendaylight.yang.gen.v1.http.org
                            .openroadm.common.types.rev181019.RatioDB(Decimal64.valueOf(spanLoss)));
                        otsBuilder.setSpanLossReceive(spanLossRx);
                    } else {
                        otsBuilder
                            .setSpanLossTransmit(spanLossTx)
                            .setSpanLossReceive(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.RatioDB(
                                    Decimal64.valueOf(spanLoss)));
                    }
                    interfaceBuilder.addAugmentation(intf1Builder.setOts(otsBuilder.build()).build());
                    openRoadmInterfaces.postInterface(realNodeId,interfaceBuilder);
                    LOG.info("Spanloss Value update completed successfully");
                    return true;
                } else {
                    LOG.error("Interface not found for nodeId: {} and interfaceName: {}", nodeId, interfaceName);
                    return false;
                }
            } else if (mappingUtils.getOpenRoadmVersion(realNodeId)
                    .equals(StringConstants.OPENROADM_DEVICE_VERSION_7_1)) {

                org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB spanLossRx;
                org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB spanLossTx;
                Optional<org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529
                        .interfaces.grp.Interface> interfaceObject =
                        openRoadmInterfaces.getInterface(realNodeId, interfaceName);
                if (interfaceObject.isPresent()) {
                    org.opendaylight.yang.gen.v1.http.org.openroadm.device
                            .rev200529.interfaces.grp.InterfaceBuilder interfaceBuilder =
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.device
                                    .rev200529.interfaces.grp.InterfaceBuilder(interfaceObject.orElseThrow());
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                            .transport.interfaces.rev200529.ots.container.OtsBuilder otsBuilder =
                            new org.opendaylight.yang.gen.v1.http.org.openroadm
                                    .optical.transport.interfaces.rev200529.ots.container.OtsBuilder();
                    org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev200529.interfaces.grp.Interface intf =
                            interfaceObject.orElseThrow();
                    if (intf.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                            .transport.interfaces.rev200529.Interface1.class) != null
                            && intf.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport
                            .interfaces.rev200529.Interface1.class).getOts() != null) {
                        org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces
                                .rev200529.ots.container.Ots ots =
                                intf.augmentation(org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                                        .transport.interfaces.rev200529.Interface1.class).getOts();

                        otsBuilder.setFiberType(ots.getFiberType());
                        spanLossRx = ots.getSpanLossReceive();
                        spanLossTx = ots.getSpanLossTransmit();
                    } else {
                        spanLossRx = new org.opendaylight.yang.gen.v1.http.org
                                .openroadm.common.link.types.rev191129.RatioDB(Decimal64.valueOf(spanLoss));
                        spanLossTx = new org.opendaylight.yang.gen.v1.http.org
                                .openroadm.common.link.types.rev191129.RatioDB(Decimal64.valueOf(spanLoss));
                    }
                    org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces
                            .rev200529.Interface1Builder intf1Builder =
                            new org.opendaylight.yang.gen.v1.http.org.openroadm.optical
                                    .transport.interfaces.rev200529.Interface1Builder();
                    if (direction.equals("TX")) {
                        otsBuilder.setSpanLossTransmit(new org.opendaylight.yang.gen.v1.http.org
                                .openroadm.common.link.types.rev191129.RatioDB(Decimal64.valueOf(spanLoss)));
                        otsBuilder.setSpanLossReceive(spanLossRx);
                    } else {
                        otsBuilder
                            .setSpanLossTransmit(spanLossTx)
                            .setSpanLossReceive(
                                new org.opendaylight.yang.gen.v1.http.org.openroadm.common.link.types.rev191129.RatioDB(
                                        Decimal64.valueOf(spanLoss)));
                    }
                    interfaceBuilder.addAugmentation(intf1Builder.setOts(otsBuilder.build()).build());
                    openRoadmInterfaces.postInterface(realNodeId,interfaceBuilder);
                    LOG.info("Spanloss Value update completed successfully");
                    return true;
                } else {
                    LOG.error("Interface not found for nodeId: {} and interfaceName: {}", nodeId,interfaceName);
                    return false;
                }
            }
        } catch (OpenRoadmInterfaceException e) {
            // TODO Auto-generated catch block
            LOG.error("OpenRoadmInterfaceException occured: ",e);
        } /**catch (InterruptedException e) {
         // TODO Auto-generated catch block
         } catch (ExecutionException e) {
         // TODO Auto-generated catch block
         }**/
        return false;
    }

    /**
     * This method calculates Spanloss by TranmistPower - Receive Power Steps:
     *
     * <p>1. Read PM measurement
     *
     * <p>2. Set Spanloss value for interface
     *
     * @param roadmLinks
     *            reference to list of RoadmLinks
     * @return map with list of spans with their spanloss value
     */
    private Map<LinkId, BigDecimal> getLinkSpanloss(List<RoadmLinks> roadmLinks) {
        Map<LinkId, BigDecimal> map = new HashMap<>();
        LOG.info("Executing GetLinkSpanLoss");
        BigDecimal spanLoss;
        for (RoadmLinks link : roadmLinks) {
            String sourceNodeId = link.getSrcNodeId();
            String sourceTpId = link.getSrcTpId();
            String destNodeId = link.getDestNodeId();
            String destTpId = link.getDestTpid();
            OtsPmHolder srcOtsPmHolder = getPmMeasurements(sourceNodeId, sourceTpId, "OpticalPowerOutput");
            if (srcOtsPmHolder == null) {
                srcOtsPmHolder = getPmMeasurements(sourceNodeId, sourceTpId, "OpticalPowerOutputOSC");
                if (srcOtsPmHolder == null) {
                    LOG.warn("OTS configuration issue at {} - {}", sourceNodeId, sourceTpId);
                    continue;
                }
            }
            OtsPmHolder destOtsPmHolder = getPmMeasurements(destNodeId, destTpId, "OpticalPowerInput");
            if (destOtsPmHolder == null) {
                destOtsPmHolder = getPmMeasurements(destNodeId, destTpId, "OpticalPowerInputOSC");
                if (destOtsPmHolder == null) {
                    LOG.warn("OTS configuration issue at {} - {}", destNodeId, destTpId);
                    continue;
                }
            }
            spanLoss = BigDecimal.valueOf(srcOtsPmHolder.getOtsParameterVal() - destOtsPmHolder.getOtsParameterVal())
                .setScale(1, RoundingMode.HALF_UP);
            LOG.info("Spanloss Calculated as :{}={}-{}",
                spanLoss, srcOtsPmHolder.getOtsParameterVal(), destOtsPmHolder.getOtsParameterVal());
            if (spanLoss.doubleValue() > 28) {
                LOG.warn("Span Loss is out of range of OpenROADM specifications");
            }
            if (spanLoss.intValue() <= 0) {
                spanLoss = BigDecimal.valueOf(0);
            }
            if (!setSpanLoss(sourceNodeId, srcOtsPmHolder.getOtsInterfaceName(), spanLoss, "TX")) {
                LOG.info("Setting spanLoss failed for {}", sourceNodeId);
                return null;
            }
            if (!setSpanLoss(destNodeId, destOtsPmHolder.getOtsInterfaceName(), spanLoss, "RX")) {
                LOG.info("Setting spanLoss failed for {}", destNodeId);
                return null;
            }
            map.put(link.getLinkId(), spanLoss);
        }
        return map;
    }

    private String getRealNodeId(String mappedNodeId) {
        WithKey<Node, NodeKey> mappedNodeII = InstanceIdentifiers.OPENROADM_TOPOLOGY_II
                .toBuilder()
                .child(Node.class, new NodeKey(new NodeId(mappedNodeId)))
                .build();
        Optional<Node> realNode;
        try (ReadTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction()) {
            realNode = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, mappedNodeII).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error on getRealNodeId {} :", mappedNodeId, e);
            throw new IllegalStateException(e);
        }
        if (!realNode.isPresent() || (realNode.orElseThrow().getSupportingNode() == null)) {
            LOG.error("supporting node is null");
            throw new IllegalArgumentException(
                String.format("Could not find node %s, or supporting node is not present", mappedNodeId));
        }
        List<SupportingNode> collect = realNode.orElseThrow().nonnullSupportingNode().values().stream()
            .filter(node -> (node.getNetworkRef() != null)
                && StringConstants.OPENROADM_NETWORK.equals(node.getNetworkRef().getValue())
                && (node.getNodeRef() != null) && !Strings.isNullOrEmpty(node.getNodeRef().getValue()))
            .collect(Collectors.toList());
        if (collect.isEmpty() || (collect.size() > 1)) {
            throw new IllegalArgumentException(String.format("Invalid support node count [%d] was found for node %s",
                collect.size(), mappedNodeId));

        }
        LOG.info("getRealNodeId - return {}", collect.iterator().next().getNodeRef().getValue());
        return collect.iterator().next().getNodeRef().getValue();
    }

    private Link getNetworkLinkById(LinkId linkId) {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(StringConstants.OPENROADM_TOPOLOGY));
        DataObjectIdentifier<Link> linkIID = DataObjectIdentifier.builder(Networks.class)
            .child(Network.class, overlayTopologyKey)
            .augmentation(Network1.class).child(Link.class, new LinkKey(linkId))
            .build();
        Optional<Link> linkOptional;
        try (ReadTransaction rtx = dataBroker.newReadOnlyTransaction()) {
            //TODO change to constant from Timeouts class when it will be merged.
            linkOptional = rtx.read(LogicalDatastoreType.CONFIGURATION, linkIID).get(Timeouts.DATASTORE_READ,
                TimeUnit.MILLISECONDS);
            return linkOptional.orElseThrow();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Read of {} topology failed", StringConstants.OPENROADM_TOPOLOGY, e);
            return null;
        }
    }

}
