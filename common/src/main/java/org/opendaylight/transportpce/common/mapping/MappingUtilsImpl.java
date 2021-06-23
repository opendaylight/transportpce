/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev210425.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GEODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If1GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.If400GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.IfOTUCnODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev200327.SupportedIfCapability;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MappingUtilsImpl implements MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImpl.class);

    private final DataBroker dataBroker;

    public MappingUtilsImpl(DataBroker dataBroker) {

        this.dataBroker = dataBroker;

    }

    public String getOpenRoadmVersion(String nodeId) {
        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey(nodeId)).child(NodeInfo.class).build();
        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            Optional<NodeInfo> nodeInfoObj =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, nodeInfoIID).get();
            if (nodeInfoObj.isPresent()) {
                NodeInfo nodInfo = nodeInfoObj.get();
                switch (nodInfo.getOpenroadmVersion()) {
                    case _71:
                        return StringConstants.OPENROADM_DEVICE_VERSION_7_1;
                    case _221:
                        return StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
                    case _121:
                        return StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
                    default:
                        LOG.warn("unknown openROADM device version");
                }
            } else {
                LOG.warn("Could not find mapping for nodeId {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for nodeId {}",nodeId, ex);
        }
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.common.mapping.MappingUtils#getMcCapabilitiesForNode(java.lang.String)
    */
    @Override
    public List<McCapabilities> getMcCapabilitiesForNode(String nodeId) {
        List<McCapabilities> mcCapabilities = new ArrayList<>();
        InstanceIdentifier<Nodes> nodePortMappingIID = InstanceIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId)).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodePortMapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodePortMappingIID)
                    .get();
            if (nodePortMapObject.isPresent()) {
                LOG.info("Found node {}", nodeId);
                Nodes node = nodePortMapObject.get();
                mcCapabilities.addAll(node.nonnullMcCapabilities().values());
            }
        } catch (ExecutionException e) {
            LOG.error("Something went wrong while getting node {}", nodeId, e);
        } catch (InterruptedException e) {
            LOG.error("Request interrupted for node {} interrupted", nodeId, e);
            Thread.currentThread().interrupt();
        }
        LOG.info("Capabilitities for node {}: {}", nodeId, mcCapabilities);
        return mcCapabilities;
    }

    public static Class<? extends SupportedIfCapability> convertSupIfCapa(String ifCapType) {
        switch (ifCapType) {
            case "IfOTUCnODUCn":
                return IfOTUCnODUCn.class;
            case "IfOCHOTU4ODU4":
                return IfOCHOTU4ODU4.class;
            case "IfOCH":
                return IfOCH.class;
            case "If100GEODU4":
                return If100GEODU4.class;
            case "If10GEODU2e":
                return If10GEODU2e.class;
            case "If10GEODU2":
                return If10GEODU2.class;
            case "If1GEODU0":
                return If1GEODU0.class;
            case "If400GE":
                return If400GE.class;
            case "If100GE":
                return If100GE.class;
            case "If10GE":
                return If10GE.class;
            case "If1GE":
                return If1GE.class;
            default:
                return null;
        }
    }
}
