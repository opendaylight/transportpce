/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev220316.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If10GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If10GEODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If1GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.If400GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCHOTU2EODU2E;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCHOTU2ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOTUCnODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.IfOtsiOtsigroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev201211.SupportedIfCapability;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MappingUtilsImpl implements MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImpl.class);

    private final DataBroker dataBroker;

    private static final ImmutableMap<String, SupportedIfCapability> CAP_TYPE_MAP =
        ImmutableMap.<String, SupportedIfCapability>builder()
            .put("IfOtsiOtsigroup", IfOtsiOtsigroup.VALUE)
            .put("IfOTUCnODUCn", IfOTUCnODUCn.VALUE)
            .put("IfOCHOTU4ODU4", IfOCHOTU4ODU4.VALUE)
            .put("IfOTU4ODU4", IfOTU4ODU4.VALUE)
            .put("IfOCH", IfOCH.VALUE)
            .put("If100GEODU4", If100GEODU4.VALUE)
            .put("If10GEODU2e", If10GEODU2e.VALUE)
            .put("If10GEODU2", If10GEODU2.VALUE)
            .put("If1GEODU0", If1GEODU0.VALUE)
            .put("If400GE", If400GE.VALUE)
            .put("If100GE", If100GE.VALUE)
            .put("If10GE", If10GE.VALUE)
            .put("If1GE", If1GE.VALUE)
            .put("IfOCHOTU2EODU2E", IfOCHOTU2EODU2E.VALUE)
            .put("IfOCHOTU2ODU2", IfOCHOTU2ODU2.VALUE)
            .put("If400GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-400GE}", If400GE.VALUE)
            .put("IfOtsiOtsigroup{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-otsi-otsigroup}",
                IfOtsiOtsigroup.VALUE)
            .put("IfOCH{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH}", IfOCH.VALUE)
            .put("IfOCHOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH-OTU4-ODU4}",
                IfOCHOTU4ODU4.VALUE)
            .put("IfOCHOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH-OTU4-ODU4}",
                IfOCHOTU4ODU4.VALUE)
            .put("If1GEODU0{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-1GE-ODU0}", If1GEODU0.VALUE)
            .put("If10GE{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-10GE}", If10GE.VALUE)
            .put("If10GEODU2{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-10GE-ODU2}",
                If10GEODU2.VALUE)
            .put("If10GEODU2e{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-10GE-ODU2e}",
                If10GEODU2e.VALUE)
            .put("If100GE{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-100GE}", If100GE.VALUE)
            .put("If100GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-100GE}", If100GE.VALUE)
            .put("If100GEODU4{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-100GE-ODU4}",
                If100GEODU4.VALUE)
            .put("If100GEODU4{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-100GE-ODU4}",
                If100GEODU4.VALUE)
            .build();

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
                LOG.debug("Found node {}", nodeId);
                Nodes node = nodePortMapObject.get();
                mcCapabilities.addAll(node.nonnullMcCapabilities().values());
            }
        } catch (ExecutionException e) {
            LOG.error("Something went wrong while getting node {}", nodeId, e);
        } catch (InterruptedException e) {
            LOG.error("Request interrupted for node {} interrupted", nodeId, e);
            Thread.currentThread().interrupt();
        }
        LOG.debug("Capabilitities for node {}: {}", nodeId, mcCapabilities);
        return mcCapabilities;
    }

    public static SupportedIfCapability convertSupIfCapa(String ifCapType) {
        if (!CAP_TYPE_MAP.containsKey(ifCapType)) {
            LOG.error("supported-if-capability {} not supported", ifCapType);
            return null;
        }
        return CAP_TYPE_MAP.get(ifCapType);
    }
}
