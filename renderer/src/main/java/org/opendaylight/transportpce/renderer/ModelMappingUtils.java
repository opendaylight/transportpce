/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Future;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev161014.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev170426.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev170907.olm.renderer.input.NodesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.ServicePowerSetupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.ServicePathInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.renderer.rev170228.ServicePathInputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ModelMappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ModelMappingUtils.class);
    private static final String TERMINATION_POINT = "TerminationPoint";

    private ModelMappingUtils() {
    }

    public static ServicePowerSetupInput createServicePowerSetupInput(List<Nodes> olmList,
        ServiceImplementationRequestInput input) {
        ServicePowerSetupInputBuilder olmSetupBldr = new ServicePowerSetupInputBuilder();
        olmSetupBldr.setNodes(olmList);
        olmSetupBldr.setWaveNumber(input.getPathDescription().getAToZDirection().getAToZWavelengthNumber());
        return olmSetupBldr.build();
    }

    public static ServiceImplementationRequestOutput createServiceImplResponse(String responseCode, String message) {
        ServiceImplementationRequestOutputBuilder outputBldr = new ServiceImplementationRequestOutputBuilder();
        outputBldr.setConfigurationResponseCommon(createCommonResponse(responseCode, message));
        return outputBldr.build();
    }

    public static ServiceDeleteOutput createServiceDeleteResponse(String responseCode, String message) {
        ServiceDeleteOutputBuilder outputBldr = new ServiceDeleteOutputBuilder();
        outputBldr.setConfigurationResponseCommon(createCommonResponse(responseCode, message));
        return outputBldr.build();
    }

    public static ConfigurationResponseCommon createCommonResponse(String responseCode, String message) {
        ConfigurationResponseCommonBuilder cmBldr = new ConfigurationResponseCommonBuilder();
        cmBldr.setResponseMessage(message);
        cmBldr.setResponseCode(responseCode);
        return cmBldr.build();
    }

    public static Future<RpcResult<ServiceImplementationRequestOutput>>
        createRpcResponse(ServiceImplementationRequestOutput payload) {
        return RpcResultBuilder.success(payload).buildFuture();
    }

    public static ServicePathInputData rendererCreateServiceInputAToZ(String serviceName,
        PathDescription pathDescription) {
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder();
        servicePathInputBuilder.setServiceName(serviceName);
        NodeLists nodeLists = getNodesListAToZ(pathDescription.getAToZDirection().getAToZ().iterator());
        servicePathInputBuilder.setServiceName(serviceName);
        servicePathInputBuilder.setOperation(ServicePathInput.Operation.Create);
        servicePathInputBuilder.setWaveNumber(new Long(pathDescription.getAToZDirection().getAToZWavelengthNumber()));
        servicePathInputBuilder.setNodes(nodeLists.getList());
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    public static ServicePathInputData rendererCreateServiceInputZToA(String serviceName,
        PathDescription pathDescription) {
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder();
        NodeLists nodeLists = getNodesListZtoA(pathDescription.getZToADirection().getZToA().iterator());
        servicePathInputBuilder.setOperation(ServicePathInput.Operation.Create);
        servicePathInputBuilder.setServiceName(serviceName);
        servicePathInputBuilder.setWaveNumber(new Long(pathDescription.getZToADirection().getZToAWavelengthNumber()));
        servicePathInputBuilder.setNodes(nodeLists.getList());
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    public static ServicePathInput rendererDeleteServiceInput(String serviceName,
        ServiceDeleteInput serviceDeleteInput) {
        ServicePathInputBuilder servicePathInput = new ServicePathInputBuilder();
        servicePathInput.setServiceName(serviceName);
        //TODO: finish model-model mapping
        return servicePathInput.build();
    }

    public static NodeLists getNodesListZtoA(Iterator<ZToA> iterator) {
        Map<Integer, NodeIdPair> treeMap = new TreeMap<>();
        List<Nodes> olmList = new ArrayList<>();
        List<Nodes> list = new ArrayList<>();
        String resourceType;
        TerminationPoint tp;
        String tpID = "";
        String nodeID = "";
        String sortId = "";
        while (iterator.hasNext()) {
            ZToA pathDesObj = iterator.next();
            resourceType = pathDesObj.getResource().getResource().getImplementedInterface().getSimpleName();
            LOG.info("Inside AtoZ {}", resourceType);

            try {
                if (TERMINATION_POINT.equals(resourceType)) {
                    tp = (TerminationPoint) pathDesObj.getResource().getResource();
                    LOG.info(" TP is {} {}", tp.getTerminationPointIdentifier().getTpId(),
                            tp.getTerminationPointIdentifier().getNodeId());
                    tpID = tp.getTerminationPointIdentifier().getTpId();
                    nodeID = tp.getTerminationPointIdentifier().getNodeId();
                    sortId = pathDesObj.getId();

                    //TODO: do not rely on ID to be in certain format
                    if (tpID.contains("CTP") || tpID.contains("CP")) {
                        continue;
                    }
                    if (!tpID.contains("TTP") && !tpID.contains("PP") && !tpID.contains("NETWORK")
                            && !tpID.contains("CLIENT")) {
                        continue;
                    }

                    int[] pos = findTheLongestSubstring(nodeID, tpID);
                    //TODO: do not rely on nodeId to be integer
                    int id = Integer.parseInt(sortId);
                    treeMap.put(id, new NodeIdPair(nodeID.substring(0, pos[0] - 1), tpID));
                } else if (resourceType.equals("Link")) {
                    LOG.info("The type is link");
                } else {
                    LOG.info("The type is not indentified: {}", resourceType);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                // TODO Auto-generated catch block
                LOG.error("Dont find the getResource method", e);
            }
        }

        String desID = null;
        String srcID = null;
        for (NodeIdPair values : treeMap.values()) {
            if (srcID == null) {
                srcID = values.getTpID();
            } else if (desID == null) {
                desID = values.getTpID();
                NodesBuilder nb = new NodesBuilder();
                nb.setKey(new NodesKey(values.getNodeID()));
                nb.setDestTp(desID);
                nb.setSrcTp(srcID);
                list.add(nb.build());

                NodesBuilder olmNb = new NodesBuilder();
                olmNb.setNodeId(values.getNodeID());
                olmNb.setDestTp(desID);
                olmNb.setSrcTp(srcID);
                olmList.add(olmNb.build());
                srcID = null;
                desID = null;
            } else {
                LOG.warn("both, the source and destination id are null!");
            }
        }
        return new NodeLists(olmList, list);
    }

    public static NodeLists getNodesListAToZ(Iterator<AToZ> iterator) {
        Map<Integer, NodeIdPair> treeMap = new TreeMap<>();
        List<Nodes> list = new ArrayList<>();
        List<Nodes> olmList = new ArrayList<>();
        String resourceType;
        TerminationPoint tp;
        String tpID = "";
        String nodeID = "";
        String sortId = "";

        while (iterator.hasNext()) {
            AToZ pathDesObj = iterator.next();
            resourceType = pathDesObj.getResource().getResource().getImplementedInterface().getSimpleName();
            LOG.info("Inside AtoZ {}", resourceType);
            try {
                if (TERMINATION_POINT.equals(resourceType)) {
                    tp = (TerminationPoint) pathDesObj.getResource().getResource();
                    LOG.info(" TP is {} {}", tp.getTerminationPointIdentifier().getTpId(),
                            tp.getTerminationPointIdentifier().getNodeId());
                    tpID = tp.getTerminationPointIdentifier().getTpId();
                    nodeID = tp.getTerminationPointIdentifier().getNodeId();
                    sortId = pathDesObj.getId();

                    //TODO: do not rely on ID to be in certain format
                    if (tpID.contains("CTP") || tpID.contains("CP")) {
                        continue;
                    }
                    if (!tpID.contains("TTP") && !tpID.contains("PP") && !tpID.contains("NETWORK")
                            && !tpID.contains("CLIENT")) {
                        continue;
                    }

                    int[] pos = findTheLongestSubstring(nodeID, tpID);
                    //TODO: do not rely on nodeId to be integer
                    int id = Integer.parseInt(sortId);
                    treeMap.put(id, new NodeIdPair(nodeID.substring(0, pos[0] - 1), tpID));
                } else if (resourceType.equals("Link")) {
                    LOG.info("The type is link");
                } else {
                    LOG.info("The type is not indentified: {}", resourceType);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                //TODO: Auto-generated catch block
                LOG.error("Dont find the getResource method", e);
            }
        }

        String desID = null;
        String srcID = null;
        for (NodeIdPair values : treeMap.values()) {
            if (srcID == null) {
                srcID = values.getTpID();
            } else if (desID == null) {
                desID = values.getTpID();
                NodesBuilder nb = new NodesBuilder();
                nb.setKey(new NodesKey(values.getNodeID()));
                nb.setDestTp(desID);
                nb.setSrcTp(srcID);
                list.add(nb.build());

                NodesBuilder olmNb = new NodesBuilder();
                olmNb.setNodeId(values.getNodeID());
                olmNb.setDestTp(desID);
                olmNb.setSrcTp(srcID);
                olmList.add(olmNb.build());
                srcID = null;
                desID = null;
            } else {
                LOG.warn("both, the source and destination id are null!");
            }
        }
        return new NodeLists(olmList, list);
    }

    public static int[] findTheLongestSubstring(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return null;
        }
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        int maxLen = 0;
        int endPos = 0;
        for (int i = 1; i < dp.length; i++) {
            for (int j = 1; j < dp[0].length; j++) {
                char ch1 = s1.charAt(i - 1);
                char ch2 = s2.charAt(j - 1);
                if (ch1 == ch2) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                    if (dp[i][j] >= maxLen) {
                        maxLen = dp[i][j];
                        endPos = i;
                    }
                }
            }
        }
        return new int[] { endPos - maxLen, endPos };
    }

}
