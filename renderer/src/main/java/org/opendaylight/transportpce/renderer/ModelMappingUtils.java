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
import java.util.Optional;
import java.util.TreeMap;
import org.opendaylight.transportpce.common.NodeIdPair;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.OtnServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev210618.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210618.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev190531.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev190531.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev210705.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.optical.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.optical.renderer.nodes.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.optical.renderer.nodes.NodesKey;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public final class ModelMappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ModelMappingUtils.class);
    private static final String TERMINATION_POINT = "TerminationPoint";

    private ModelMappingUtils() {
    }

    public static ServicePowerSetupInput createServicePowerSetupInput(List<Nodes> olmList,
            ServiceImplementationRequestInput input) {
        ServicePowerSetupInputBuilder olmSetupBldr = new ServicePowerSetupInputBuilder().setNodes(olmList);
        if (input != null && input.getPathDescription() != null
                && input.getPathDescription().getAToZDirection() != null) {
            AToZDirection atoZDirection = input.getPathDescription().getAToZDirection();
            olmSetupBldr.setWaveNumber(atoZDirection.getAToZWavelengthNumber());
            if (atoZDirection.getAToZMinFrequency() != null) {
                olmSetupBldr.setLowerSpectralSlotNumber(Uint32
                        .valueOf(GridUtils
                                .getLowerSpectralIndexFromFrequency(atoZDirection.getAToZMinFrequency().getValue())));
            }
            if (atoZDirection.getAToZMaxFrequency() != null) {
                olmSetupBldr.setHigherSpectralSlotNumber(Uint32
                        .valueOf(GridUtils
                                .getHigherSpectralIndexFromFrequency(atoZDirection.getAToZMaxFrequency().getValue())));
            }
        }
        return olmSetupBldr.build();
    }

    public static ServiceImplementationRequestOutput createServiceImplResponse(String responseCode, String message) {
        return new ServiceImplementationRequestOutputBuilder()
                .setConfigurationResponseCommon(createCommonResponse(responseCode, message))
                .build();
    }

    public static ServiceDeleteOutput createServiceDeleteResponse(String responseCode, String message) {
        return new ServiceDeleteOutputBuilder()
                .setConfigurationResponseCommon(createCommonResponse(responseCode, message))
                .build();
    }

    public static ConfigurationResponseCommon createCommonResponse(String responseCode, String message) {
        return new ConfigurationResponseCommonBuilder()
                .setResponseMessage(message)
                .setResponseCode(responseCode)
                .build();
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
        int scale = GridConstant.FIXED_GRID_FREQUENCY_PRECISION;
        AToZDirection atoZDirection = pathDescription.getAToZDirection();
        LOG.info("Building ServicePathInputData for a to z direction {}", atoZDirection);
        NodeLists nodeLists = getNodesListAToZ(atoZDirection.nonnullAToZ().values().iterator());
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder()
            .setServiceName(serviceName)
            .setOperation(Action.Create)
            .setNodes(nodeLists.getList())
            .setWidth(new FrequencyGHz(GridConstant.WIDTH_40));
        if (atoZDirection.getAToZWavelengthNumber() != null) {
            servicePathInputBuilder
                .setWaveNumber(atoZDirection.getAToZWavelengthNumber());
        }
        if (Uint32.valueOf(GridConstant.IRRELEVANT_WAVELENGTH_NUMBER)
                .equals(atoZDirection.getAToZWavelengthNumber())) {
            scale = GridConstant.FLEX_GRID_FREQUENCY_PRECISION;
        }
        if (atoZDirection.getAToZMinFrequency() != null) {
            servicePathInputBuilder.setMinFreq(new FrequencyTHz(atoZDirection.getAToZMinFrequency().getValue()));
            servicePathInputBuilder.setLowerSpectralSlotNumber(Uint32
                    .valueOf(GridUtils
                            .getLowerSpectralIndexFromFrequency(atoZDirection.getAToZMinFrequency().getValue())));
        }
        if (atoZDirection.getAToZMaxFrequency() != null) {
            servicePathInputBuilder.setMaxFreq(new FrequencyTHz(atoZDirection.getAToZMaxFrequency().getValue()));
            servicePathInputBuilder.setHigherSpectralSlotNumber(
                    Uint32.valueOf(GridUtils
                            .getHigherSpectralIndexFromFrequency(atoZDirection.getAToZMaxFrequency().getValue())));
        }
        if (atoZDirection.getAToZMinFrequency() != null && atoZDirection.getAToZMaxFrequency() != null) {
            servicePathInputBuilder.setCenterFreq(
                    GridUtils.getCentralFrequencyWithPrecision(atoZDirection.getAToZMinFrequency().getValue(),
                            atoZDirection.getAToZMaxFrequency().getValue(), scale));
        }
        if (atoZDirection.getRate() != null && atoZDirection.getModulationFormat() != null) {
            Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
                    .forName(atoZDirection.getModulationFormat());
            if (optionalModulationFormat.isPresent()
                    && GridConstant.FREQUENCY_WIDTH_TABLE
                    .contains(atoZDirection.getRate(), optionalModulationFormat.get())) {
                servicePathInputBuilder
                    .setWidth(FrequencyGHz
                        .getDefaultInstance(GridConstant.FREQUENCY_WIDTH_TABLE.get(atoZDirection.getRate(),
                        optionalModulationFormat.get())));
            }
        }
        servicePathInputBuilder.setModulationFormat(atoZDirection.getModulationFormat());
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    public static ServicePathInputData rendererCreateServiceInputZToA(String serviceName,
            PathDescription pathDescription) {
        int scale = GridConstant.FIXED_GRID_FREQUENCY_PRECISION;
        ZToADirection ztoADirection = pathDescription.getZToADirection();
        LOG.info("Building ServicePathInputData for z to a direction {}", ztoADirection);
        NodeLists nodeLists = getNodesListZtoA(pathDescription.getZToADirection().nonnullZToA().values().iterator());
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder()
            .setOperation(Action.Create)
            .setServiceName(serviceName)
            .setNodes(nodeLists.getList())
            .setWidth(new FrequencyGHz(GridConstant.WIDTH_40));
        if (ztoADirection.getZToAWavelengthNumber() != null) {
            servicePathInputBuilder
                .setWaveNumber(ztoADirection.getZToAWavelengthNumber());
        }
        if (Uint32.valueOf(GridConstant.IRRELEVANT_WAVELENGTH_NUMBER)
                .equals(ztoADirection.getZToAWavelengthNumber())) {
            scale = GridConstant.FLEX_GRID_FREQUENCY_PRECISION;
        }
        if (ztoADirection.getZToAMinFrequency() != null) {
            servicePathInputBuilder.setMinFreq(new FrequencyTHz(ztoADirection.getZToAMinFrequency().getValue()));
            servicePathInputBuilder.setLowerSpectralSlotNumber(Uint32
                    .valueOf(GridUtils
                            .getLowerSpectralIndexFromFrequency(ztoADirection.getZToAMinFrequency().getValue())));
        }
        if (ztoADirection.getZToAMaxFrequency() != null) {
            servicePathInputBuilder.setMaxFreq(new FrequencyTHz(ztoADirection.getZToAMaxFrequency().getValue()));
            servicePathInputBuilder.setHigherSpectralSlotNumber(
                    Uint32.valueOf(GridUtils
                            .getHigherSpectralIndexFromFrequency(ztoADirection.getZToAMaxFrequency().getValue())));
        }
        if (ztoADirection.getZToAMinFrequency() != null && ztoADirection.getZToAMaxFrequency() != null) {
            servicePathInputBuilder.setCenterFreq(
                    GridUtils.getCentralFrequencyWithPrecision(ztoADirection.getZToAMinFrequency().getValue(),
                            ztoADirection.getZToAMaxFrequency().getValue(), scale));
        }
        if (ztoADirection.getRate() != null && ztoADirection.getModulationFormat() != null) {
            Optional<ModulationFormat> optionalModulationFormat = ModulationFormat
                    .forName(ztoADirection.getModulationFormat());
            if (optionalModulationFormat.isPresent()
                    && GridConstant.FREQUENCY_WIDTH_TABLE
                    .contains(ztoADirection.getRate(), optionalModulationFormat.get())) {
                servicePathInputBuilder.setWidth(FrequencyGHz
                        .getDefaultInstance(GridConstant.FREQUENCY_WIDTH_TABLE.get(ztoADirection.getRate(),
                                optionalModulationFormat.get())));
            }
        }
        servicePathInputBuilder.setModulationFormat(ztoADirection.getModulationFormat());
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    // Adding createOtnServiceInputpath for A-Z and Z-A directions as one method
    public static OtnServicePathInput rendererCreateOtnServiceInput(String serviceName, String serviceFormat,
        Uint32 serviceRate, PathDescription pathDescription, boolean asideToZside) {
        // If atoZ is set true use A-to-Z direction otherwise use Z-to-A
        List<org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.otn.renderer.nodes.Nodes> nodes =
            new ArrayList<>();
        NodeLists nodeLists = getNodesListAToZ(pathDescription.getAToZDirection().nonnullAToZ().values().iterator());
        if (!asideToZside) {
            nodeLists = getNodesListZtoA(pathDescription.getZToADirection().nonnullZToA().values().iterator());
        }
        LOG.info("These are node-lists {}, {}", nodeLists.getList(), nodeLists.getOlmList());
        for (Nodes node: nodeLists.getList()) {
            nodes.add(new org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev210618.otn.renderer.nodes
                .NodesBuilder()
                            .setNodeId(node.getNodeId())
                            .setClientTp(node.getSrcTp())
                            .setNetworkTp(node.getDestTp())
                            .build());
        }
        OtnServicePathInputBuilder otnServicePathInputBuilder = new OtnServicePathInputBuilder()
            .setServiceName(serviceName)
            .setServiceFormat(serviceFormat)
            .setServiceRate(serviceRate)
            .setNodes(nodes);

        // set the trib-slots and trib-ports for the lower oder odu
        if (serviceRate.intValue() == 1 || (serviceRate.intValue() == 10)) {
            Short tribPort = Short.valueOf(pathDescription.getAToZDirection().getMinTribSlot().getValue()
                .split("\\.")[0]);
            Short minTribSlot = Short.valueOf(pathDescription.getAToZDirection().getMinTribSlot().getValue()
                .split("\\.")[1]);
            otnServicePathInputBuilder
                .setTribPortNumber(tribPort)
                .setTribSlot(minTribSlot);
        }
        return otnServicePathInputBuilder.build();
    }

    public static ServicePathInput rendererDeleteServiceInput(String serviceName,
            ServiceDeleteInput serviceDeleteInput) {
        //TODO: finish model-model mapping
        return new ServicePathInputBuilder().setServiceName(serviceName).build();
    }

    private static NodeLists getNodesListZtoA(Iterator<ZToA> iterator) {
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
                    sortId = pathDesObj.getId();

                    //TODO: do not rely on ID to be in certain format
                    if (tpID.contains("CTP") || tpID.contains("CP")) {
                        continue;
                    }
                    if (tpID.contains(StringConstants.TTP_TOKEN)) {
                        nodeID = tp.getTpNodeId().split("-DEG")[0];
                    } else if (tpID.contains(StringConstants.PP_TOKEN)) {
                        nodeID = tp.getTpNodeId().split("-SRG")[0];
                    } else if (tpID.contains(StringConstants.NETWORK_TOKEN)
                        || tpID.contains(StringConstants.CLIENT_TOKEN) || tpID.isEmpty()) {
                        nodeID = tp.getTpNodeId().split("-XPDR")[0];
                    } else {
                        continue;
                    }
                    int id = Integer.parseInt(sortId);
                    treeMap.put(id, new NodeIdPair(nodeID, tpID));
                } else if ("Link".equals(resourceType)) {
                    LOG.info("The type is link");
                } else {
                    LOG.info("The type is not identified: {}", resourceType);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                LOG.error("Dont find the getResource method", e);
            }
        }
        populateNodeLists(treeMap, list, olmList, false);
        return new NodeLists(olmList, list);
    }

    private static NodeLists getNodesListAToZ(Iterator<AToZ> iterator) {
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
                    LOG.info("TP is {} {}", tp.getTpId(),
                            tp.getTpNodeId());
                    tpID = tp.getTpId();
                    sortId = pathDesObj.getId();

                    //TODO: do not rely on ID to be in certain format
                    if (tpID.contains("CTP") || tpID.contains("CP")) {
                        continue;
                    }
                    if (tpID.contains(StringConstants.TTP_TOKEN)) {
                        nodeID = tp.getTpNodeId().split("-DEG")[0];
                    } else if (tpID.contains(StringConstants.PP_TOKEN)) {
                        nodeID = tp.getTpNodeId().split("-SRG")[0];
                    } else if (tpID.contains(StringConstants.NETWORK_TOKEN)
                        || tpID.contains(StringConstants.CLIENT_TOKEN) || tpID.isEmpty()) {
                        nodeID = tp.getTpNodeId().split("-XPDR")[0];
                    } else {
                        continue;
                    }
                    int id = Integer.parseInt(sortId);
                    treeMap.put(id, new NodeIdPair(nodeID, tpID));
                } else if ("Link".equals(resourceType)) {
                    LOG.info("The type is link");
                } else {
                    LOG.info("The type is not identified: {}", resourceType);
                }
            } catch (IllegalArgumentException | SecurityException e) {
                //TODO: Auto-generated catch block
                LOG.error("Did not find the getResource method", e);
            }
        }
        populateNodeLists(treeMap, list, olmList, true);
        return new NodeLists(olmList, list);
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
        value = {"NP_LOAD_OF_KNOWN_NULL_VALUE","RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE"},
        justification = "loop when value is not always null - "
                + "TODO: check if something exists in Java lib")
    private static void populateNodeLists(Map<Integer, NodeIdPair> treeMap, List<Nodes> list, List<Nodes> olmList,
        boolean isAToz) {
        String desID = null;
        String srcID = null;
        LOG.info("treeMap values = {}", treeMap.values());
        for (NodeIdPair values : treeMap.values()) {
            if (srcID == null) {
                srcID = values.getTpID();
            } else if (desID == null) {
                desID = values.getTpID();
                NodesBuilder olmNb = new NodesBuilder()
                    .setNodeId(values.getNodeID())
                    .setDestTp(desID)
                    .setSrcTp(srcID);
                olmList.add(olmNb.build());
                if (srcID.isEmpty()) {
                    srcID = null;
                }
                if (desID.isEmpty()) {
                    desID = new StringBuilder(srcID).toString();
                    srcID = null;
                }
                if (isAToz) {
                    NodesBuilder nb = new NodesBuilder()
                        .withKey(new NodesKey(values.getNodeID()))
                        .setDestTp(desID)
                        .setSrcTp(srcID);
                    if (srcID != null && desID != null && srcID.contains(StringConstants.NETWORK_TOKEN)) {
                        nb.setDestTp(srcID).setSrcTp(desID);
                    }
                    list.add(nb.build());
                } else {
                    if (srcID != null && desID != null && !srcID.contains(StringConstants.NETWORK_TOKEN)
                        && !desID.contains(StringConstants.NETWORK_TOKEN)) {
                        NodesBuilder nb = new NodesBuilder()
                            .withKey(new NodesKey(values.getNodeID()))
                            .setDestTp(desID)
                            .setSrcTp(srcID);
                        list.add(nb.build());
                    }
                }
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
