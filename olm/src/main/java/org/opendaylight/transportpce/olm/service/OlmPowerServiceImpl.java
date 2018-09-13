/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.service;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.transportpce.common.InstanceIdentifiers;
import org.opendaylight.transportpce.common.NetworkUtils;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransaction;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.mapping.PortMapping;
import org.opendaylight.transportpce.olm.power.PowerMgmt;
import org.opendaylight.transportpce.olm.util.OlmUtils;
import org.opendaylight.transportpce.olm.util.OtsPmHolder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossBaseOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.CalculateSpanlossCurrentOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.GetPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerResetOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerTurndownOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.calculate.spanloss.base.output.Spans;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.calculate.spanloss.base.output.SpansBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.topology.rev170929.Link1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.network.types.rev170929.OpenroadmLinkType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.pm.types.rev161014.PmGranularity;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.types.rev161014.ResourceTypeEnum;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.get.pm.input.ResourceIdentifierBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.Network;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NetworkKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.NodeKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev150608.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.Network1;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.network.Link;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OlmPowerServiceImpl implements OlmPowerService {
    private static final Logger LOG = LoggerFactory.getLogger(OlmPowerServiceImpl.class);
    private static final String SUCCESS = "Success";
    private static final String FAILED = "Failed";
    private final DataBroker dataBroker;
    private final PowerMgmt powerMgmt;
    private final DeviceTransactionManager deviceTransactionManager;
    private final PortMapping portMapping;

    public OlmPowerServiceImpl(DataBroker dataBroker, PowerMgmt powerMgmt,
            DeviceTransactionManager deviceTransactionManager, PortMapping portMapping) {
        this.dataBroker = dataBroker;
        this.powerMgmt = powerMgmt;
        this.portMapping = portMapping;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    public void init() {
        LOG.info("init ...");
    }

    public void close() {
        LOG.info("close ...");
    }


    @Override
    public GetPmOutput getPm(GetPmInput pmInput) {
        GetPmOutputBuilder pmOutputBuilder = OlmUtils.pmFetch(pmInput, this.deviceTransactionManager);
        return pmOutputBuilder.build();
    }

    @Override
    public ServicePowerSetupOutput servicePowerSetup(ServicePowerSetupInput powerSetupInput) {
        ServicePowerSetupOutputBuilder powerSetupOutput = new ServicePowerSetupOutputBuilder();
        boolean successValPowerCalculation = this.powerMgmt.setPower(powerSetupInput);

        if (successValPowerCalculation) {
            powerSetupOutput.setResult(SUCCESS);
        } else {
            powerSetupOutput.setResult(FAILED);
        }
        return powerSetupOutput.build();
    }

    @Override
    public ServicePowerTurndownOutput servicePowerTurndown(
            ServicePowerTurndownInput powerTurndownInput) {

        ServicePowerTurndownOutputBuilder powerTurnDownOutput = new ServicePowerTurndownOutputBuilder();
        // TODO add flag or return failure instead of string
        if (this.powerMgmt.powerTurnDown(powerTurndownInput)) {
            powerTurnDownOutput.setResult(SUCCESS);
        } else {
            powerTurnDownOutput.setResult(FAILED);
        }
        return powerTurnDownOutput.build();
    }

    @Override
    public CalculateSpanlossBaseOutput calculateSpanlossBase(CalculateSpanlossBaseInput spanlossBaseInput) {

        LOG.info("CalculateSpanlossBase Request received for source type {}", spanlossBaseInput.getSrcType());
        List<Link> networkLinks = getNetworkLinks();
        if (networkLinks.isEmpty()) {
            LOG.warn("Failed to get links form {} topology.", NetworkUtils.OVERLAY_NETWORK_ID);
            return new CalculateSpanlossBaseOutputBuilder().setResult(FAILED).build();
        }

        if (! CalculateSpanlossBaseInput.SrcType.All.equals(spanlossBaseInput.getSrcType())) {
            networkLinks = networkLinks.stream()
                    .filter(link -> link.getLinkId().equals(spanlossBaseInput.getLinkId()))
                    .collect(Collectors.toList());
        }

        List<Link> roadmLinks = new ArrayList<>();
        for (Link link : networkLinks) {
            Link1 roadmLinkAugmentation = link.augmentation(Link1.class);
            if (roadmLinkAugmentation == null) {
                LOG.debug("Missing OpenRoadm link augmentation in link {} from {} topology.",
                        link.getLinkId().getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }
            if (OpenroadmLinkType.ROADMTOROADM.equals(roadmLinkAugmentation.getLinkType())) {
                // Only calculate spanloss for Roadm-to-Roadm links
                roadmLinks.add(link);
            }
        }

        if (roadmLinks.isEmpty()) {
            LOG.warn("Topology {} does not have any Roadm-to-Roadm links.", NetworkUtils.OVERLAY_NETWORK_ID);
            return new CalculateSpanlossBaseOutputBuilder().setResult(FAILED).build();
        }

        Map<LinkId, BigDecimal> spanLossResult = getLinkSpanloss(roadmLinks);
        CalculateSpanlossBaseOutputBuilder spanLossBaseBuilder = new CalculateSpanlossBaseOutputBuilder();

        if (spanLossResult != null && !spanLossResult.isEmpty()) {
            spanLossBaseBuilder.setResult(SUCCESS);
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
            spanLossBaseBuilder.setResult(FAILED);
            return spanLossBaseBuilder.build();
        }

    }

    @Override
    public CalculateSpanlossCurrentOutput calculateSpanlossCurrent(CalculateSpanlossCurrentInput input) {
        LOG.info("calculateSpanlossCurrent Request received for all links in network model.");
        List<Link> networkLinks = getNetworkLinks();
        if (networkLinks.isEmpty()) {
            LOG.warn("Failed to get links form {} topology.", NetworkUtils.OVERLAY_NETWORK_ID);
            return null;
        }
        List<Link> roadmLinks = new ArrayList<>();
        for (Link link : networkLinks) {
            Link1 roadmLinkAugmentation = link.augmentation(Link1.class);
            if (roadmLinkAugmentation == null) {
                LOG.debug("Missing OpenRoadm link augmentation in link {} from {} topology.",
                        link.getLinkId().getValue(), NetworkUtils.OVERLAY_NETWORK_ID);
                continue;
            }
            if (OpenroadmLinkType.ROADMTOROADM.equals(roadmLinkAugmentation.getLinkType())) {
                // Only calculate spanloss for Roadm-to-Roadm links
                roadmLinks.add(link);
            }
        }

        if (roadmLinks.isEmpty()) {
            LOG.warn("Topology {} does not have any Roadm-to-Roadm links.", NetworkUtils.OVERLAY_NETWORK_ID);
            return null;
        }

        Map<LinkId, BigDecimal> spanLossResult = getLinkSpanloss(roadmLinks);
        CalculateSpanlossCurrentOutputBuilder spanLossCurrentBuilder = new CalculateSpanlossCurrentOutputBuilder();
        if (spanLossResult != null && !spanLossResult.isEmpty()) {
            spanLossCurrentBuilder.setResult(SUCCESS);
            return spanLossCurrentBuilder.build();
        } else {
            LOG.error("Spanloss Current calculation failed");
            spanLossCurrentBuilder.setResult(FAILED);
            return spanLossCurrentBuilder.build();
        }
    }

    @Override
    public ServicePowerResetOutput servicePowerReset(ServicePowerResetInput input) {
        // TODO
        return null;
    }

    private List<Link> getNetworkLinks() {
        NetworkKey overlayTopologyKey = new NetworkKey(new NetworkId(NetworkUtils.OVERLAY_NETWORK_ID));

        InstanceIdentifier<Network1> networkIID = InstanceIdentifier.builder(Network.class, overlayTopologyKey)
                .augmentation(Network1.class)
                .build();
        Optional<Network1> networkOptional;
        try (ReadOnlyTransaction rtx = this.dataBroker.newReadOnlyTransaction()) {
            //TODO change to constant from Timeouts class when it will be merged.
            networkOptional = rtx.read(LogicalDatastoreType.CONFIGURATION, networkIID).get(Timeouts.DATASTORE_READ,
                    TimeUnit.MILLISECONDS).toJavaUtil();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.warn("Read of {} topology failed", NetworkUtils.OVERLAY_NETWORK_ID);
            return Collections.emptyList();
        }

        if (! networkOptional.isPresent()) {
            LOG.warn("Network augmentation with links data is not present in {} topology.",
                    NetworkUtils.OVERLAY_NETWORK_ID);
            return Collections.emptyList();
        }

        List<Link> networkLinks = networkOptional.get().getLink();
        if ((networkLinks == null) || networkLinks.isEmpty()) {
            LOG.warn("Links are not present in {} topology.", NetworkUtils.OVERLAY_NETWORK_ID);
            return Collections.emptyList();
        }
        return networkLinks;
    }

    /**
     * This method retrieves OTS PM from current PM list by nodeId and TPId: Steps:
     *
     * <p>
     * 1. Get OTS interface name from port mapping by TPId 2. Call getPm RPC to get OTS PM
     *
     * <p>
     *
     * @param nodeId Node-id of the NE.
     * @param tpID Termination point Name.
     * @param pmName PM name which need to be retrieved
     * @return reference to OtsPmHolder
     */
    private OtsPmHolder getPmMeasurements(String nodeId, String tpID, String pmName) {
        String realNodeId = getRealNodeId(nodeId);
        Mapping mapping = this.portMapping.getMapping(realNodeId, tpID);
        if (mapping == null) {
            return null;
        }
        GetPmInput getPmInput = new GetPmInputBuilder().setNodeId(realNodeId)
                .setResourceType(ResourceTypeEnum.Interface).setGranularity(PmGranularity._15min)
                .setResourceIdentifier(
                        new ResourceIdentifierBuilder().setResourceName(mapping.getSupportingOts()).build())
                .build();
        GetPmOutput otsPmOutput = getPm(getPmInput);

        if (otsPmOutput == null) {
            LOG.info("OTS PM not found for NodeId: {} TP Id:{} PMName:{}", realNodeId, tpID, pmName);
            return null;
        }
        try {
            for (Measurements measurement : otsPmOutput.getMeasurements()) {
                if (pmName.equals(measurement.getPmparameterName())) {
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
     * This method Sets Spanloss on A-End and Z-End OTS interface: Steps:
     *
     * <p>
     * 1. Read existing interface details
     *
     * <p>
     * 2. Set spanloss
     *
     * @param nodeId nodeId of NE on which spanloss need to be updated
     * @param interfaceName OTS interface for NE on which spanloss is cacluated
     * @param spanLoss calculated spanloss value
     * @param direction for which spanloss is calculated.It can be either Tx or Rx
     * @return true/false
     */
    private boolean setSpanLoss(String nodeId, String interfaceName, BigDecimal spanLoss, String direction) {
        String realNodeId = getRealNodeId(nodeId);
        LOG.info("Setting Spanloss in device for {}, InterfaceName: {}", realNodeId, interfaceName);
        switch (direction) {
            case "TX":
                LOG.info("Setting 'span-loss-transmit' in device:  {}, Interface: {}", realNodeId, interfaceName);
                break;
            case "RX":
                LOG.info("Setting 'span-loss-receive' in device:  {}, Interface: {}", realNodeId, interfaceName);
                break;
            default:
                LOG.error("Impossible to set spanloss in device:  {}, Interface: {}", realNodeId, interfaceName);
                break;
        }
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        com.google.common.base.Optional<Interface> interfaceObject;
        try {
            Future<Optional<DeviceTransaction>> deviceTxFuture =
                    this.deviceTransactionManager.getDeviceTransaction(realNodeId);
            java.util.Optional<DeviceTransaction> deviceTxOpt = deviceTxFuture.get();
            DeviceTransaction deviceTx;
            if (deviceTxOpt.isPresent()) {
                deviceTx = deviceTxOpt.get();
            } else {
                LOG.error("Device transaction for device {} was not found!", nodeId);
                return false;
            }
            interfaceObject = deviceTx.read(LogicalDatastoreType.CONFIGURATION, interfacesIID).get();
            BigDecimal initialSpanloss = new BigDecimal(0);
            RatioDB spanLossRx = new RatioDB(initialSpanloss);
            RatioDB spanLossTx = new RatioDB(initialSpanloss);
            if (interfaceObject.isPresent()) {
                Interface intf = interfaceObject.get();
                InterfaceBuilder interfaceBuilder = new InterfaceBuilder(intf);
                OtsBuilder otsBuilder = new OtsBuilder();
                if ((intf.augmentation(Interface1.class) != null)
                    && (intf.augmentation(Interface1.class).getOts() != null)) {
                    Ots ots = intf.augmentation(Interface1.class).getOts();
                    otsBuilder.setFiberType(ots.getFiberType());
                    spanLossRx = ots.getSpanLossReceive();
                    spanLossTx = ots.getSpanLossTransmit();
                } else {
                    spanLossRx = new RatioDB(spanLoss);
                    spanLossTx = new RatioDB(spanLoss);
                }
                Interface1Builder intf1Builder = new Interface1Builder();
                if (direction.equals("TX")) {
                    otsBuilder.setSpanLossTransmit(new RatioDB(spanLoss));
                    otsBuilder.setSpanLossReceive(spanLossRx);
                } else {
                    otsBuilder.setSpanLossTransmit(spanLossTx).setSpanLossReceive(new RatioDB(spanLoss));
                }
                interfaceBuilder.addAugmentation(Interface1.class, intf1Builder.setOts(otsBuilder.build()).build());
                deviceTx.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, interfaceBuilder.build());
                ListenableFuture<Void> submit =
                        deviceTx.submit(Timeouts.DEVICE_WRITE_TIMEOUT, Timeouts.DEVICE_WRITE_TIMEOUT_UNIT);
                submit.get();
                LOG.info("Spanloss Value update completed successfully");
                return true;
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.warn("Unable to set spanloss", e);
        }
        return false;
    }

    /**
     * This method calculates Spanloss by TranmistPower - Receive Power Steps:
     *
     * <p>
     * 1. Read PM measurement
     *
     * <p>
     * 2. Set Spanloss value for interface
     *
     * @param roadmLinks
     *            reference to list of RoadmLinks
     * @return map with list of spans with their spanloss value
     */
    private Map<LinkId, BigDecimal> getLinkSpanloss(List<Link> roadmLinks) {
        Map<LinkId, BigDecimal> map = new HashMap<LinkId, BigDecimal>();
        LOG.info("Executing GetLinkSpanLoss");
        BigDecimal spanLoss = new BigDecimal(0);
        for (Link link : roadmLinks) {
            String sourceNodeId = link.getSource().getSourceNode().getValue();
            String sourceTpId = link.getSource().getSourceTp().toString();
            String destNodeId = link.getDestination().getDestNode().getValue();
            String destTpId = link.getDestination().getDestTp().toString();
            OtsPmHolder srcOtsPmHoler = getPmMeasurements(sourceNodeId, sourceTpId, "OpticalPowerOutput");
            OtsPmHolder destOtsPmHoler = getPmMeasurements(destNodeId, destTpId, "OpticalPowerInput");
            spanLoss = new BigDecimal(srcOtsPmHoler.getOtsParameterVal() - destOtsPmHoler.getOtsParameterVal())
                .setScale(0, RoundingMode.HALF_UP);
            LOG.info("Spanloss Calculated as :" + spanLoss + "=" + srcOtsPmHoler.getOtsParameterVal() + "-"
                + destOtsPmHoler.getOtsParameterVal());
            if ((spanLoss.doubleValue() < 28) && (spanLoss.doubleValue() > 0)) {
                if (!setSpanLoss(sourceNodeId, srcOtsPmHoler.getOtsInterfaceName(), spanLoss, "TX")) {
                    LOG.info("Setting spanLoss failed for " + sourceNodeId);
                    return null;
                }
                if (!setSpanLoss(destNodeId, destOtsPmHoler.getOtsInterfaceName(), spanLoss, "RX")) {
                    LOG.info("Setting spanLoss failed for " + destNodeId);
                    return null;
                }
                map.put(link.getLinkId(), spanLoss);
            }
        }
        return map;
    }

    private String getRealNodeId(String mappedNodeId) {
        KeyedInstanceIdentifier<Node, NodeKey> mappedNodeII =
                InstanceIdentifiers.OVERLAY_NETWORK_II.child(Node.class, new NodeKey(new NodeId(mappedNodeId)));
        com.google.common.base.Optional<Node> realNode;
        try (ReadOnlyTransaction readOnlyTransaction = this.dataBroker.newReadOnlyTransaction()) {
            realNode = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, mappedNodeII).get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.error(e.getMessage(), e);
            throw new IllegalStateException(e);
        }
        if (!realNode.isPresent() || (realNode.get().getSupportingNode() == null)) {
            throw new IllegalArgumentException(
                    String.format("Could not find node %s, or supporting node is not present", mappedNodeId));
        }
        List<SupportingNode> collect = realNode.get().getSupportingNode().stream()
                .filter(node -> (node.getNetworkRef() != null)
                        && NetworkUtils.UNDERLAY_NETWORK_ID.equals(node.getNetworkRef().getValue())
                        && (node.getNodeRef() != null) && !Strings.isNullOrEmpty(node.getNodeRef().getValue()))
                .collect(Collectors.toList());
        if (collect.isEmpty() || (collect.size() > 1)) {
            throw new IllegalArgumentException(String.format("Invalid support node count [%d] was found for node %s",
                    collect.size(), mappedNodeId));
        }
        return collect.iterator().next().getNodeRef().getValue();
    }

}
