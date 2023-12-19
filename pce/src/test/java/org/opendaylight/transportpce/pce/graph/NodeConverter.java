/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.graph;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NetworkId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.NodeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.node.SupportingNodeKey;



public class NodeConverter implements Converter {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(NodeConverter.class);

    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @Override
    public void marshal(
            Object object,
            HierarchicalStreamWriter writer,
            MarshallingContext context) {

        Node node = (Node) object;

        writer.startNode("id");
        writer.setValue(node.getNodeId().getValue());
        writer.endNode();
        Map<SupportingNodeKey, SupportingNode> supportingNode = node.getSupportingNode();
        for (Map.Entry<SupportingNodeKey, SupportingNode> entry : Objects.requireNonNull(supportingNode).entrySet()) {
            SupportingNode supportingNodeEntry = entry.getValue();
            if (supportingNodeEntry != null) {
                writer.startNode("supporting-node");
                if (supportingNodeEntry.getNodeRef() != null) {
                    writer.startNode("node-ref");
                    writer.setValue(supportingNodeEntry.getNodeRef().getValue());
                    writer.endNode();
                }

                if (supportingNodeEntry.getNetworkRef() != null) {
                    writer.startNode("network-ref");
                    writer.setValue(supportingNodeEntry.getNetworkRef().getValue());
                    writer.endNode();
                }
                writer.endNode();
            }
        }
    }

    @Override
    public Object unmarshal(
            HierarchicalStreamReader reader,
            UnmarshallingContext context) {


        NodeBuilder nodeBuilder = new NodeBuilder();

        Map<SupportingNodeKey, SupportingNode> supportingNodes = new HashMap<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if ("id".equals(reader.getNodeName())) {
                nodeBuilder.setNodeId(NodeId.getDefaultInstance(reader.getValue()));
            } else if ("supporting-node".equals(reader.getNodeName())) {
                SupportingNodeBuilder supportingNodeBuilder = new SupportingNodeBuilder();
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    if ("node-ref".equals(reader.getNodeName())) {
                        supportingNodeBuilder.setNodeRef(NodeId.getDefaultInstance(reader.getValue()));
                    } else if ("network-ref".equals(reader.getNodeName())) {
                        supportingNodeBuilder.setNetworkRef(NetworkId.getDefaultInstance(reader.getValue()));
                    }
                    reader.moveUp();
                }
                SupportingNode supportingNode = supportingNodeBuilder.build();
                supportingNodes.put(supportingNode.key(), supportingNode);
            }
            reader.moveUp();
        }
        nodeBuilder.setSupportingNode(supportingNodes);

        return nodeBuilder.build();
    }

    @SuppressWarnings("checkstyle:LineLength")
    @Override
    public boolean canConvert(Class aclass) {
        LOG.info("canConvert: {}", aclass);
        return aclass.getName().equals("org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node")
                || aclass.getName().equals("org.opendaylight.yang.rt.v1.obj.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.networks.network.Node");
    }
}
