/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev170418.ServicePowerSetupInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.device.rev200128.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev171017.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev171017.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.olm.renderer.input.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.olm.renderer.input.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev200128.olm.renderer.input.NodesKey;
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
        ServicePowerSetupInputBuilder olmSetupBldr = new ServicePowerSetupInputBuilder()
            .setNodes(olmList);
        if (input != null && input.getPathDescription() != null
                && input.getPathDescription().getAToZDirection() != null) {
            olmSetupBldr.setWaveNumber(
                    input.getPathDescription().getAToZDirection().getAToZWavelengthNumber());
        }
        return olmSetupBldr.build();
    }

    public static ServiceImplementationRequestOutput createServiceImplResponse(String responseCode, String message) {
        ServiceImplementationRequestOutputBuilder outputBldr = new ServiceImplementationRequestOutputBuilder()
            .setConfigurationResponseCommon(createCommonResponse(responseCode, message));
        return outputBldr.build();
    }

    public static ServiceDeleteOutput createServiceDeleteResponse(String responseCode, String message) {
        ServiceDeleteOutputBuilder outputBldr = new ServiceDeleteOutputBuilder()
            .setConfigurationResponseCommon(createCommonResponse(responseCode, message));
        return outputBldr.build();
    }

    public static ConfigurationResponseCommon createCommonResponse(String responseCode, String message) {
        ConfigurationResponseCommonBuilder cmBldr = new ConfigurationResponseCommonBuilder()
            .setResponseMessage(message)
            .setResponseCode(responseCode);
        return cmBldr.build();
    }

    public static ListenableFuture<RpcResult<ServiceImplementationRequestOutput>>
        createServiceImplementationRpcResponse(ServiceImplementationRequestOutput payload) {
        return RpcResultBuilder.success(payload).buildFuture();
    }

    public static ListenableFuture<RpcResult<ServiceDeleteOutput>>
        createServiceDeleteRpcResponse(ServiceDeleteOutput payload) {
        return RpcResultBuilder.success(payload).buildFuture();
    }

    public static ServicePathInputData rendererCreateServiceInputAToZ(String serviceName,
            PathDescription pathDescription) {
        NodeLists nodeLists = getNodesListAToZ(pathDescription.getAToZDirection().getAToZ().iterator());
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder()
            .setServiceName(serviceName)
            .setOperation(ServicePathInput.Operation.Create)
            .setWaveNumber(Long.valueOf(pathDescription.getAToZDirection().getAToZWavelengthNumber().toJava()))
            .setNodes(nodeLists.getList());
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    public static ServicePathInputData rendererCreateServiceInputZToA(String serviceName,
            PathDescription pathDescription) {
        NodeLists nodeLists = getNodesListZtoA(pathDescription.getZToADirection().getZToA().iterator());
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder()
            .setOperation(ServicePathInput.Operation.Create)
            .setServiceName(serviceName)
            .setWaveNumber(Long.valueOf(pathDescription.getZToADirection().getZToAWavelengthNumber().toJava()))
            .setNodes(nodeLists.getList());
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    public static ServicePathInput rendererDeleteServiceInput(String serviceName,
            ServiceDeleteInput serviceDeleteInput) {
        ServicePathInputBuilder servicePathInput = new ServicePathInputBuilder()
            .setServiceName(serviceName);
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
            resourceType = pathDesObj.getResource().getResource().implementedInterface().getSimpleName();
            LOG.info("Inside ZtoA {}", resourceType);

            try {
                if (TERMINATION_POINT.equals(resourceType)) {
                    tp = (TerminationPoint) pathDesObj.getResource().getResource();
                    LOG.info(" TP is {} {}", tp.getTpId(),
                            tp.getTpNodeId());
                    tpID = tp.getTpId();
                    nodeID = tp.getTpNodeId();
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
                    if (pos != null) {
                        //TODO: do not rely on nodeId to be integer
                        int id = Integer.parseInt(sortId);
                        treeMap.put(id, new NodeIdPair(nodeID.substring(0, pos[0] - 1), tpID));
                    }
                } else if ("Link".equals(resourceType)) {
                    LOG.info("The type is link");
                } else {
                    LOG.info("The type is not indentified: {}", resourceType);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                LOG.error("Dont find the getResource method", e);
            }
        }

        populateNodeLists(treeMap, list, olmList);
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
            resourceType = pathDesObj.getResource().getResource().implementedInterface().getSimpleName();
            LOG.info("Inside AtoZ {}", resourceType);
            try {
                if (TERMINATION_POINT.equals(resourceType)) {
                    tp = (TerminationPoint) pathDesObj.getResource().getResource();
                    LOG.info(" TP is {} {}", tp.getTpId(),
                            tp.getTpNodeId());
                    tpID = tp.getTpId();
                    nodeID = tp.getTpNodeId();
                    sortId = pathDesObj.getId();

                    //TODO: do not rely on ID to be in certain format
                    if (tpID.contains("CTP") || tpID.contains("CP")) {
                        continue;
                    }
                    if (!tpID.contains(StringConstants.TTP_TOKEN)
                        && !tpID.contains(StringConstants.PP_TOKEN)
                        && !tpID.contains(StringConstants.NETWORK_TOKEN)
                        && !tpID.contains(StringConstants.CLIENT_TOKEN)) {
                        continue;
                    }

                    int[] pos = findTheLongestSubstring(nodeID, tpID);
                    if (pos != null) {
                        //TODO: do not rely on nodeId to be integer
                        int id = Integer.parseInt(sortId);
                        treeMap.put(id, new NodeIdPair(nodeID.substring(0, pos[0] - 1), tpID));
                    }
                } else if ("Link".equals(resourceType)) {
                    LOG.info("The type is link");
                } else {
                    LOG.info("The type is not indentified: {}", resourceType);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                //TODO: Auto-generated catch block
                LOG.error("Dont find the getResource method", e);
            }
        }

        populateNodeLists(treeMap, list, olmList);
        return new NodeLists(olmList, list);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = {"NP_LOAD_OF_KNOWN_NULL_VALUE","RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE"},
            justification = "loop when value is not always null - "
                    + "TODO: check if something exists in Java lib")
    private static void populateNodeLists(Map<Integer, NodeIdPair> treeMap,
            List<Nodes> list, List<Nodes> olmList) {
        String desID = null;
        String srcID = null;
        for (NodeIdPair values : treeMap.values()) {
            if (srcID == null) {
                srcID = values.getTpID();
            } else if (desID == null) {
                desID = values.getTpID();
                NodesBuilder nb = new NodesBuilder()
                    .withKey(new NodesKey(values.getNodeID()))
                    .setDestTp(desID)
                    .setSrcTp(srcID);
                list.add(nb.build());

                NodesBuilder olmNb = new NodesBuilder()
                    .setNodeId(values.getNodeID())
                    .setDestTp(desID)
                    .setSrcTp(srcID);
                olmList.add(olmNb.build());
                srcID = null;
                desID = null;
            } else {
                LOG.warn("both, the source and destination id are null!");
            }
        }
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "PZLA_PREFER_ZERO_LENGTH_ARRAYS",
            justification = "not relevant to return and zero length array"
                    + " as we need real pos")
    public static int[] findTheLongestSubstring(String s1, String s2) {
        if ((s1 == null) || (s2 == null)) {
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
