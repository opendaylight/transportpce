/*
 * Copyright © 2019 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer.provisiondevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.opendaylight.transportpce.common.crossconnect.CrossConnect;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.renderer.openroadminterface.OpenRoadmInterfaceFactory;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.OtnServicePathOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.node.interfaces.NodeInterface;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.node.interfaces.NodeInterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.node.interfaces.NodeInterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.otn.renderer.input.Nodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtnDeviceRendererServiceImpl implements OtnDeviceRendererService {
    private static final Logger LOG = LoggerFactory.getLogger(OtnDeviceRendererServiceImpl.class);
    private final OpenRoadmInterfaceFactory openRoadmInterfaceFactory;
    private final CrossConnect crossConnect;

    public OtnDeviceRendererServiceImpl(OpenRoadmInterfaceFactory openRoadmInterfaceFactory,
        CrossConnect crossConnect) {
        this.openRoadmInterfaceFactory = openRoadmInterfaceFactory;
        this.crossConnect = crossConnect;
    }

    @Override
    public OtnServicePathOutput setupOtnServicePath(OtnServicePathInput input) {
        boolean success = true;
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        List<String> results = new ArrayList<>();
        if (input.getServiceType().equals("Ethernet")) {
            try {
                nodeInterfaces = createInterface(input);
            }
            catch (OpenRoadmInterfaceException e) {
                //handle exception
                LOG.warn("Set up service path failed");
                success = false;
            }
        }
        if (success) {
            for (NodeInterface nodeInterface: nodeInterfaces) {
                results.add("Otn Service path was set up successfully for node :" + nodeInterface.getNodeId());
            }
        }
        OtnServicePathOutputBuilder otnServicePathOutputBuilder = new OtnServicePathOutputBuilder()
            .setSuccess(success)
            .setNodeInterface(nodeInterfaces)
            .setResult(String.join("\n", results));
        return otnServicePathOutputBuilder.build();
    }

    @Override
    public OtnServicePathOutput deleteOtnServicePath(OtnServicePathInput input) {
        OtnServicePathOutputBuilder otnServicePathOutputBuilder = new OtnServicePathOutputBuilder()
            .setResult("deleteServicePath was called");
        return otnServicePathOutputBuilder.build();
    }

    private List<NodeInterface> createInterface(OtnServicePathInput input) throws OpenRoadmInterfaceException {
        List<NodeInterface> nodeInterfaces = new ArrayList<>();
        if (input.getServiceRate().equals("1G")) {
            for (Nodes node: input.getNodes()) {
                //check if the node is mounted or not?
                List<String> createdEthInterfaces = new ArrayList<>();
                List<String> createdOduInterfaces = new ArrayList<>();
                createdEthInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmEth1GInterface(node.getNodeId(),
                    node.getClientTp()));
                LOG.info("created ethernet interface");
                createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(),
                    node.getClientTp(), input.getServiceName(),"07", false, input.getTribPortNumber(),
                    input.getTribSlot())); //suppporting interface?, payload ?
                LOG.info("Created odu interface client side");
                createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu0Interface(node.getNodeId(),
                    node.getNetworkTp(), input.getServiceName(),"07", true, input.getTribPortNumber(),
                    input.getTribSlot()));
                LOG.info("created odu inteface network side");

                    //implement cross connect
                Optional<String> connectionNameOpt = postCrossConnect(createdOduInterfaces, node);
                List<String> createdConnections = new ArrayList<>();
                createdConnections.add(connectionNameOpt.get());
                LOG.info("Created cross connects");
                NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder()
                    .withKey(new NodeInterfaceKey(node.getNodeId()))
                    .setNodeId(node.getNodeId())
                    .setConnectionId(createdConnections)
                    .setEthInterfaceId(createdEthInterfaces)
                    .setOduInterfaceId(createdOduInterfaces);
                nodeInterfaces.add(nodeInterfaceBuilder.build());
            }
        }
        else if (input.getServiceRate().equals("10G")) {
            // implementing ODU2e for now

            for (Nodes node: input.getNodes()) {
                //check if the node is mounted or not?
                List<String> createdEthInterfaces = new ArrayList<>();
                List<String> createdOduInterfaces = new ArrayList<>();
                createdEthInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmEth10GInterface(node.getNodeId(),
                    node.getClientTp()));
                createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                    node.getClientTp(), input.getServiceName(),"03", false)); //suppporting interface?, payload ?
                createdOduInterfaces.add(openRoadmInterfaceFactory.createOpenRoadmOdu2eInterface(node.getNodeId(),
                    node.getNetworkTp(), input.getServiceName(),"03" , true));// supporting interface? payload ?

                //implement cross connect
                Optional<String> connectionNameOpt = postCrossConnect(createdOduInterfaces, node);
                List<String> createdConnections = new ArrayList<>();
                createdConnections.add(connectionNameOpt.get());
                LOG.info("Created cross connects");
                NodeInterfaceBuilder nodeInterfaceBuilder = new NodeInterfaceBuilder();
                nodeInterfaceBuilder.withKey(new NodeInterfaceKey(node.getNodeId()))
                    .setNodeId(node.getNodeId())
                    .setConnectionId(createdConnections)
                    .setEthInterfaceId(createdEthInterfaces)
                    .setOduInterfaceId(createdOduInterfaces);
                nodeInterfaces.add(nodeInterfaceBuilder.build());
            }
        }
        return nodeInterfaces;
    }

    private Optional<String> postCrossConnect(List<String> createdOduInterfaces, Nodes node)
        throws OpenRoadmInterfaceException {
        return this.crossConnect.postOtnCrossConnect(createdOduInterfaces,node);
    }
}
