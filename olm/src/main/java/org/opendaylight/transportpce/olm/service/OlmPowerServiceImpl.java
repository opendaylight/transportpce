/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;
import static org.opendaylight.transportpce.common.StringConstants.RX;
import static org.opendaylight.transportpce.common.StringConstants.TX;

import com.google.common.base.Strings;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.util.NodeInterfaceKey;
import org.opendaylight.transportpce.olm.util.OlmUtils;
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
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.OpenroadmNodeVersion;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.mapping.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250714.mapping.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.network.rev230526.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev230526.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.PmGranularity;
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
        OpenroadmNodeVersion nodeVersion = getNodeVersion(pmInput.getNodeId());
        if (nodeVersion == null) {
            return pmOutputBuilder.build();
        }
        LOG.info("Now calling get pm data");
        pmOutputBuilder = OlmUtils.pmFetch(pmInput, deviceTransactionManager,
            nodeVersion);
        return pmOutputBuilder.build();
    }

    @Override
    public Map<String, List<GetPmOutput>> getPmAll(GetPmInput input) {
        Map<String, List<GetPmOutput>> pmOutputMap = new HashMap<>();
        if (mappingUtils.getOpenRoadmVersion(input.getNodeId()) == null) {
            return pmOutputMap;
        }
        OpenroadmNodeVersion nodeVersion = getNodeVersion(input.getNodeId());
        if (nodeVersion == null) {
            return pmOutputMap;
        }
        LOG.info("Now calling get pm data");
        pmOutputMap = OlmUtils.pmFetchAll(input, deviceTransactionManager,
                nodeVersion);
        return pmOutputMap;
    }

    /**
     * This method retrieves all PM from a realId node.
     *
     * <p>Steps:
     *
     * <p>1. Get OTS interface name from port mapping by TPId
     *
     * <p>2. Call getPm RPC to get all OTS PMs
     *
     * @param realNodeId the real Node-id of the NE.
     * @return {@code Map<String, List<GetPmOutput>>} where the key is the nodeInterfaceName for the corresponding list.
     */
    private Map<NodeInterfaceKey, List<GetPmOutput>> getPmAll(String realNodeId) {
        LOG.info(" ------ Fetching data for realNode: {} --------", realNodeId);
        Map<MappingKey, Mapping> mappingMap = portMapping.getNode(realNodeId).getMapping();
        GetPmInput getPmInput = createPmInput(realNodeId);
        Map<String, List<GetPmOutput>> pmOutputMap = getPmAll(getPmInput);
        Map<NodeInterfaceKey, List<GetPmOutput>> outputMap = updateKeys(pmOutputMap, realNodeId, mappingMap);

        if (outputMap == null || outputMap.isEmpty()) {
            LOG.info("OTS PM not found for NodeId: {}", realNodeId);
            return null;
        }
        return outputMap;
    }

    private OpenroadmNodeVersion getNodeVersion(String nodeId) {
        if (mappingUtils.getOpenRoadmVersion(nodeId) == null) {
            return null;
        }
        String openRoadmVersion = mappingUtils.getOpenRoadmVersion(nodeId);
        switch (openRoadmVersion) {
            case StringConstants.OPENROADM_DEVICE_VERSION_1_2_1:
                return OpenroadmNodeVersion._121;
            case StringConstants.OPENROADM_DEVICE_VERSION_2_2_1:
                return OpenroadmNodeVersion._221;
            case StringConstants.OPENROADM_DEVICE_VERSION_7_1:
                return OpenroadmNodeVersion._71;
            default:
                LOG.error("Unknown device version: {}", openRoadmVersion);
                return null;
        }
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

    /*
     * Sorts and changes they keys for this map. Replaces the interface name (OMS or OTS) with the realNodeId
     * to create the correct string key. If there would exist OTS and OMS interfaces on the same node this method
     * discards the OMS entries.
     */
    private Map<NodeInterfaceKey, List<GetPmOutput>> updateKeys(Map<String, List<GetPmOutput>> outputMap,
                                                                String realNodeId,
                                                                Map<MappingKey, Mapping> mapping) {
        Map<NodeInterfaceKey, List<GetPmOutput>> transformed = outputMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .collect(Collectors.toMap(
                        entry -> replaceInterfaceWithNodeId(entry.getKey(), realNodeId, mapping),
                        Map.Entry::getValue,
                        (original, duplicate) -> original, // in case of a key clash, keep the original
                        LinkedHashMap::new  //keep the sortorder.
                ));
        return transformed;
    }

    private NodeInterfaceKey replaceInterfaceWithNodeId(String key,
                                                        String realNodeId, Map<MappingKey, Mapping> mapping) {
        for (Entry<MappingKey, Mapping> mapEntry:mapping.entrySet()) {
            if (hasValidInterface(mapEntry, key)) {
                return new NodeInterfaceKey(realNodeId, mapEntry.getValue().getLogicalConnectionPoint());
            }
        }
        return null;
    }

    private static boolean hasValidInterface(Entry<MappingKey, Mapping> mapEntry, String key) {
        return key.equals(mapEntry.getValue().getSupportingOts()) || key.equals(mapEntry.getValue().getSupportingOms());
    }

    private static GetPmInput createPmInput(String realNodeId) {
        GetPmInputBuilder builder = new GetPmInputBuilder();
        builder.setNodeId(realNodeId);
        builder.setGranularity(PmGranularity._15min);
        builder.setResourceType(ResourceTypeEnum.Interface);
        return builder.build();
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
     * @param realNodeId The real nodeId of NE on which spanloss need to be updated
     * @param interfaceName OTS interface for NE on which spanloss is cacluated
     * @param spanLoss calculated spanloss value
     * @param direction for which spanloss is calculated.It can be either Tx or Rx
     * @return true/false
     */
    private boolean setSpanLoss(String realNodeId, String interfaceName, BigDecimal spanLoss, String direction) {
        OpenroadmNodeVersion nodeVersion = getNodeVersion(realNodeId);
        if (nodeVersion == null) {
            LOG.error("Couldn't get node version for node {}", realNodeId);
            return false;
        } else {
            return OlmUtils.setSpanLoss(realNodeId,
                    interfaceName,
                    spanLoss,
                    direction,
                    nodeVersion,
                    openRoadmInterfaces);
        }
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
        LOG.info("Executing GetLinkSpanLoss");
        LOG.info("Updating {} RoadmLinks", roadmLinks.size());

        //Map nodeId -> RealNodeId
        Map<String, String> realNodeIdMap = populateRealIdMap(roadmLinks);
        //Map NodeInterfaceKey -> list of GetPmOutput
        Map<NodeInterfaceKey, List<GetPmOutput>> nodePms = fetchMeasurements(realNodeIdMap);

        LOG.info("Done, got {} nodes", nodePms.size());

        Map<NodeInterfaceKey, Map<String, BigDecimal>> spanLosses;
        spanLosses = calculateSpannLosses(roadmLinks, nodePms, realNodeIdMap);
        Map<LinkId, BigDecimal> map = getLinksResultMap(roadmLinks, spanLosses, realNodeIdMap);
        Map<NodeInterfaceKey, Boolean> resultMap = setSpanLosses(nodePms, spanLosses);
        logSpanLossResult(resultMap);
        return map;
    }

    private Map<NodeInterfaceKey, Map<String, BigDecimal>> calculateSpannLosses(
            List<RoadmLinks> roadmLinks,
            Map<NodeInterfaceKey, List<GetPmOutput>> nodePms,
            Map<String, String> realNodeIdMap) {

        Map<NodeInterfaceKey, Map<String, BigDecimal>> spanLosses = new HashMap<>();

        for (RoadmLinks link : roadmLinks) {
            String sourceNodeId = link.getSrcNodeId();
            String destNodeId = link.getDestNodeId();

            NodeInterfaceKey sourceNodeKey = createKey(realNodeIdMap.get(sourceNodeId), link.getSrcTpId());
            NodeInterfaceKey destNodeKey = createKey(realNodeIdMap.get(destNodeId), link.getDestTpid());
            List<String> outputMeasures = List.of("OpticalPowerOutput", "OpticalPowerOutputOCS");
            List<String> inputMeasures = List.of("OpticalPowerInput", "OpticalPowerInputOCS");

            if (getMeasurement(nodePms.get(sourceNodeKey), outputMeasures) == null) {
                LOG.warn("OTS configuration issue at {}", sourceNodeKey);
                continue;
            }

            if (getMeasurement(nodePms.get(destNodeKey), inputMeasures) == null) {
                LOG.warn("OTS configuration issue at {} ", destNodeKey);
                continue;
            }

            Double srcPowerOutput = getMeasurement(nodePms.get(sourceNodeKey), outputMeasures);
            Double destPowerInput = getMeasurement(nodePms.get(destNodeKey), inputMeasures);

            BigDecimal spanLoss = BigDecimal.valueOf(srcPowerOutput - destPowerInput)
                    .setScale(1, RoundingMode.HALF_UP);
            LOG.info("Spanloss Calculated as :{}={}-{}",
                    spanLoss, srcPowerOutput, destPowerInput);
            if (spanLoss.doubleValue() > 28) {
                LOG.warn("Span Loss is out of range of OpenROADM specifications");
            }
            if (spanLoss.intValue() <= 0) {
                spanLoss = BigDecimal.valueOf(0);
            }

            spanLosses = setSpanLossMap(spanLosses, sourceNodeKey, TX, spanLoss);
            spanLosses = setSpanLossMap(spanLosses, destNodeKey, RX, spanLoss);
        }
        return spanLosses;
    }

    private Map<LinkId, BigDecimal> getLinksResultMap(List<RoadmLinks> roadmLinks,
                                                      Map<NodeInterfaceKey, Map<String, BigDecimal>> spanLosses,
                                                      Map<String, String> realNodeIdMap) {
        Map<LinkId, BigDecimal> map = new HashMap<>();
        for (RoadmLinks link : roadmLinks) {
            String sourceNodeId = link.getSrcNodeId();
            NodeInterfaceKey sourceNodeKey = createKey(realNodeIdMap.get(sourceNodeId), link.getSrcTpId());
            BigDecimal spanLoss = spanLosses.get(sourceNodeKey).get(TX);
            map.put(link.getLinkId(), spanLoss);
        }
        return map;
    }

    private void logSpanLossResult(Map<NodeInterfaceKey, Boolean> resultMap) {
        LOG.info("Set spanloss result:");

        for (Entry<NodeInterfaceKey, Boolean> entry: resultMap.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString())).toList()) {
            String success = (entry.getValue() ? "Success!" : " -- Failed! --");
            LOG.info("Node id {} - {}: {}",
                    entry.getKey().nodeId(),
                    entry.getKey().logicalConnectionPoint(),
                    success);
        }
    }

    //Creates a Map where each NodeInterfaceKey has a status boolean whether or not the setSpanloss succeeded or not.
    private Map<NodeInterfaceKey, Boolean> setSpanLosses(Map<NodeInterfaceKey, List<GetPmOutput>> nodePms,
                                                         Map<NodeInterfaceKey, Map<String, BigDecimal>> lossMap) {
        Map<NodeInterfaceKey, Boolean> resultMap = new HashMap<>();

        Map<String, List<NodeInterfaceKey>> nodesWithKeys = lossMap.entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getKey().nodeId(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

        List<Callable<Map<NodeInterfaceKey, Boolean>>> tasks = new ArrayList<>();
        for (Entry<String, List<NodeInterfaceKey>> entry : nodesWithKeys.entrySet()) {
            List<NodeInterfaceKey> nodeKeys = entry.getValue();
            tasks.add(() -> setSpanLossForNode(nodeKeys, nodePms, lossMap));
        }

        LOG.info("Setting spanlosses on the nodes in parallell....");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map<NodeInterfaceKey, Boolean>>> futures = invokeAll(executor, tasks);
            for (Future<Map<NodeInterfaceKey, Boolean>> future : futures) {
                resultMap.putAll(getFutureMap(future));  // Will block until result is ready
            }
        }
        return resultMap;
    }

    /*
     * Substitute method for the executor.invokeAll method.
     * Using submit makes sure this do not throw InterruptedException, and hence all tasks will succeed to be scheduled.
     */
    private <T> List<Future<T>> invokeAll(ExecutorService executor, List<Callable<T>> tasks) {
        List<Future<T>> futures = new ArrayList<>();

        for (Callable<T> task:tasks) {
            futures.add(executor.submit(task));
        }
        return futures;
    }

    /*
     * Factored out for readability. Could be modified to keep waiting in case if an InterruoptedException is thrown
     * but for now we just return an empty map in this case.
     */
    private <K, V> Map<K, V> getFutureMap(Future<Map<K, V>> future) {
        try {
            return future.get();
        } catch (ExecutionException e) {
            LOG.error("ExecutionException while getting future map", e);
        } catch (InterruptedException e) {
            LOG.error("InterruptedException while getting future map", e);
        }
        return Map.of();
    }

    private Map<NodeInterfaceKey, Boolean> setSpanLossForNode(List<NodeInterfaceKey> nodeKeys,
                                       Map<NodeInterfaceKey, List<GetPmOutput>> nodePms,
                                       Map<NodeInterfaceKey, Map<String, BigDecimal>> lossMap) {
        boolean successTx;
        boolean successRx;
        Map<NodeInterfaceKey, Boolean> returnMap = new HashMap<>();
        for (NodeInterfaceKey key:nodeKeys) {
            LOG.info("Setting spanloss for node {}, connectionPoint {}", key.nodeId(), key.logicalConnectionPoint());
            String resourceName = nodePms.get(key).getFirst().getResourceIdentifier().getResourceName();
            if (lossMap.get(key).get(TX) != null) {
                successTx = setSpanLoss(key.nodeId(),
                        resourceName,
                        lossMap.get(key).get(TX),
                        TX);

                if (!successTx) {
                    logSetSpanLossFail(key.nodeId(), TX);
                }
            } else {
                successTx = true; //Nothing to set for Tx so operation is considered succeeded.
            }

            if (lossMap.get(key).get(RX) != null) {
                successRx = setSpanLoss(key.nodeId(),
                        resourceName,
                        lossMap.get(key).get(RX),
                        RX);

                if (!successTx) {
                    logSetSpanLossFail(key.nodeId(), RX);
                }
            } else {
                successRx = true; //nothing to set for RX so operation is considered succeeded.
            }
            returnMap.put(key, successTx && successRx);
        }
        return returnMap;
    }

    private void logSetSpanLossFail(String nodeId, String direction) {
        LOG.info("Setting spanLoss failed for realNodeId {} direction {}",nodeId, direction);
    }

    private Map<NodeInterfaceKey, Map<String, BigDecimal>> setSpanLossMap(
            Map<NodeInterfaceKey,Map<String, BigDecimal>> spanLoss,
            NodeInterfaceKey key, String direction, BigDecimal spanloss) {
        if (spanLoss == null) {
            spanLoss = new HashMap<>();
        }

        Map<String, BigDecimal> innerMap = spanLoss.get(key);

        if (innerMap == null) {
            innerMap = new HashMap<>();
        }

        innerMap.put(direction, spanloss);
        spanLoss.put(key, innerMap);

        return spanLoss;
    }

    private NodeInterfaceKey createKey(String realNodeId, String tpId) {
        return new NodeInterfaceKey(realNodeId, tpId);
    }

    /*
     * Creates a Map of type {@code <String, String>} where the key is the nodeId from the link (either src or dest
     * node) and the value is the RealNodeId. This is to quickly be able to look up the realNodeId.
     */
    private Map<String, String> populateRealIdMap(List<RoadmLinks> roadmLinks) {
        Map<String, String> realNodeIdMap = new HashMap<>();
        for (RoadmLinks link : roadmLinks) {
            String srcNodeId = link.getSrcNodeId();
            String destNodeId = link.getDestNodeId();
            realNodeIdMap.put(srcNodeId, getRealNodeId(srcNodeId));
            realNodeIdMap.put(destNodeId, getRealNodeId(destNodeId));
        }
        return realNodeIdMap;
    }

    private Map<NodeInterfaceKey, List<GetPmOutput>> fetchMeasurements(Map<String, String> realNodeIdMap) {

        Map<NodeInterfaceKey, List<GetPmOutput>> nodePms = new HashMap<>();
        Set<String> realNodeIds = new HashSet<>(realNodeIdMap.values());

        List<Callable<Map<NodeInterfaceKey, List<GetPmOutput>>>> tasks = new ArrayList<>();
        for (String realNodeId : realNodeIds) {
            tasks.add(() -> getPmAll(realNodeId));
        }

        LOG.info("Fetching measurements from nodes in parallell....");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Map<NodeInterfaceKey, List<GetPmOutput>>>> futures = invokeAll(executor, tasks);
            for (Future<Map<NodeInterfaceKey, List<GetPmOutput>>> future : futures) {
                nodePms.putAll(getFutureMap(future));
            }
        }
        return nodePms;
    }

    /*
     * Extracts a measurement from the list of outputs. The list of string given is a prioritized
     * list of measurements to fetch. The first one to contain a valid measurement is returned.
     */
    private Double getMeasurement(List<GetPmOutput> outputs, List<String> names) {
        if (outputs == null || outputs.isEmpty()) {
            return null;
        }
        for (GetPmOutput output : outputs) {
            if (output.getMeasurements() == null || output.getMeasurements().size() == 0) {
                return null;
            }
            for (String name:names) {
                for (Measurements measurement : output.getMeasurements()) {
                    if (isValidMeasurement(name, measurement)) {
                        return Double.parseDouble(measurement.getPmparameterValue());
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidMeasurement(String name, Measurements measurement) {
        return measurement.getPmparameterName().equalsIgnoreCase(name)
                && measurement.getPmparameterValue() != null
                && !measurement.getPmparameterValue().isEmpty();
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
