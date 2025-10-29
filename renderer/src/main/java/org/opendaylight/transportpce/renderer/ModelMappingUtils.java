/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import com.google.common.util.concurrent.ListenableFuture;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.opendaylight.transportpce.common.NodeIdPair;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.common.mapping.PortMappingUtils;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.Action;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OperationalModeType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OtnServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.OtnServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.ServicePathInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.ServicePathInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.AEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.AEndApiInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.ZEndApiInfo;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.device.renderer.rev250325.az.api.info.ZEndApiInfoBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.olm.rev210618.ServicePowerSetupInputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceDeleteOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestInput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutput;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.renderer.rev210915.ServiceImplementationRequestOutputBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.configuration.response.common.ConfigurationResponseCommon;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev250110.configuration.response.common.ConfigurationResponseCommonBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev250110.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.openroadm.service.rev250110.ServiceDeleteInput;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.PathDescription;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirection;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.Link;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.optical.renderer.nodes.Nodes;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.optical.renderer.nodes.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.optical.renderer.nodes.NodesKey;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.opendaylight.yangtools.yang.common.Uint16;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ModelMappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(ModelMappingUtils.class);
    private static final String TERMINATION_POINT = "TerminationPoint";
    private static final String LINK = "Link";

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
            // Set the MC-width for the OLM
            if (atoZDirection.getAToZMaxFrequency() != null || atoZDirection.getAToZMinFrequency() != null) {
                olmSetupBldr.setMcWidth(new FrequencyGHz(Decimal64.valueOf(
                        atoZDirection.getAToZMaxFrequency().getValue().decimalValue()
                                .subtract(atoZDirection.getAToZMinFrequency().getValue().decimalValue())
                                .multiply(new BigDecimal(1000)))));
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
            PathDescription pathDescription, Action operation, boolean isOpenConfig) {
        int scale = GridConstant.FIXED_GRID_FREQUENCY_PRECISION;
        AToZDirection atoZDirection = pathDescription.getAToZDirection();
        LOG.info("Building ServicePathInputData for a to z direction {}", atoZDirection);
        NodeLists nodeLists = getNodesListAToZ(atoZDirection.nonnullAToZ().values().iterator());
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder()
            .setServiceName(serviceName)
            .setOperation(operation)
            .setTargetOutputPower(atoZDirection.getTargetOutputPower())
            .setNodes(nodeLists.getRendererNodeList());
        if (atoZDirection.getAToZMaxFrequency() != null || atoZDirection.getAToZMinFrequency() != null) {
            servicePathInputBuilder.setNmcWidth(new FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_40)));
            servicePathInputBuilder.setMcWidth(new FrequencyGHz(Decimal64.valueOf(
                    pathDescription.getAToZDirection().getAToZMaxFrequency().getValue().decimalValue()
                    .subtract(pathDescription.getAToZDirection().getAToZMinFrequency().getValue().decimalValue()))));
        }
        if (atoZDirection.getAToZWavelengthNumber() != null) {
            servicePathInputBuilder
                    .setWaveNumber(atoZDirection.getAToZWavelengthNumber());
            if (Uint32.valueOf(GridConstant.IRRELEVANT_WAVELENGTH_NUMBER)
                    .equals(atoZDirection.getAToZWavelengthNumber())) {
                scale = GridConstant.FLEX_GRID_FREQUENCY_PRECISION;
            }
        }
        if (!isOpenConfig) {
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

        }
        if (atoZDirection.getAToZMinFrequency() != null && atoZDirection.getAToZMaxFrequency() != null) {
            servicePathInputBuilder.setCenterFreq(GridUtils.getCentralFrequencyWithPrecision(
                    atoZDirection.getAToZMinFrequency().getValue().decimalValue(),
                    atoZDirection.getAToZMaxFrequency().getValue().decimalValue(),
                    scale));
        }
        if (atoZDirection.getWidth() != null) {
            servicePathInputBuilder.setNmcWidth(
                    FrequencyGHz.getDefaultInstance(atoZDirection.getWidth().getValue().toCanonicalString())
            );
        } else if (atoZDirection.getRate() != null && atoZDirection.getModulationFormat() != null) {
            ModulationFormat modulationFormat = ModulationFormat.forName(atoZDirection.getModulationFormat());
            if (modulationFormat != null
                    && GridConstant.FREQUENCY_WIDTH_TABLE
                    .contains(atoZDirection.getRate(), modulationFormat)) {
                servicePathInputBuilder
                    .setNmcWidth(FrequencyGHz
                        .getDefaultInstance(GridConstant.FREQUENCY_WIDTH_TABLE.get(atoZDirection.getRate(),
                        modulationFormat)));
            }
        }
        if (atoZDirection.getOpticalOperationalMode() != null) {
            servicePathInputBuilder.setOperationalMode(
                    new OperationalModeType(Uint16.valueOf(atoZDirection.getOpticalOperationalMode())));
        }
        servicePathInputBuilder.setModulationFormat(atoZDirection.getModulationFormat())
            .setAEndApiInfo(createAendApiInfo(pathDescription, false))
            .setZEndApiInfo(createZendApiInfo(pathDescription, false));
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    public static ServicePathInputData rendererCreateServiceInputZToA(String serviceName,
            PathDescription pathDescription, Action operation, boolean isOpenConfig) {
        int scale = GridConstant.FIXED_GRID_FREQUENCY_PRECISION;
        ZToADirection ztoADirection = pathDescription.getZToADirection();
        LOG.info("Building ServicePathInputData for z to a direction {}", ztoADirection);
        NodeLists nodeLists = getNodesListZtoA(pathDescription.getZToADirection().nonnullZToA().values().iterator());
        ServicePathInputBuilder servicePathInputBuilder = new ServicePathInputBuilder()
            .setOperation(operation)
            .setServiceName(serviceName)
            .setTargetOutputPower(ztoADirection.getTargetOutputPower())
            .setNodes(nodeLists.getRendererNodeList());
        if (ztoADirection.getZToAMaxFrequency() != null || ztoADirection.getZToAMinFrequency() != null) {
            servicePathInputBuilder.setNmcWidth(new FrequencyGHz(Decimal64.valueOf(GridConstant.WIDTH_40)));
            /*servicePathInputBuilder.setMcWidth(new FrequencyGHz(Decimal64.valueOf(
                    pathDescription.getAToZDirection().getAToZMaxFrequency().getValue().decimalValue()
                    .subtract(pathDescription.getAToZDirection().getAToZMinFrequency().getValue().decimalValue()))));*/
            servicePathInputBuilder.setMcWidth(new FrequencyGHz(Decimal64.valueOf(
                    ztoADirection.getZToAMaxFrequency().getValue().decimalValue()
                            .subtract(ztoADirection.getZToAMinFrequency().getValue().decimalValue()))));
        }
        if (ztoADirection.getZToAWavelengthNumber() != null) {
            servicePathInputBuilder
                    .setWaveNumber(ztoADirection.getZToAWavelengthNumber());

            if (Uint32.valueOf(GridConstant.IRRELEVANT_WAVELENGTH_NUMBER)
                    .equals(ztoADirection.getZToAWavelengthNumber())) {
                scale = GridConstant.FLEX_GRID_FREQUENCY_PRECISION;
            }
        }
        if (!isOpenConfig) {
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
                servicePathInputBuilder.setCenterFreq(GridUtils.getCentralFrequencyWithPrecision(
                        ztoADirection.getZToAMinFrequency().getValue().decimalValue(),
                        ztoADirection.getZToAMaxFrequency().getValue().decimalValue(),
                        scale));
            }
        } else if (ztoADirection.getZToAMinFrequency() != null && ztoADirection.getZToAMinFrequency() != null) {
            FrequencyTHz centralFrequencyWithPrecision = GridUtils.getCentralFrequencyWithPrecisionForOpenConfig(
                    ztoADirection.getZToAMinFrequency().getValue().decimalValue(),
                    ztoADirection.getZToAMinFrequency().getValue().decimalValue(),
                    GridConstant.FLEX_GRID_FREQUENCY_PRECISION);
            servicePathInputBuilder.setCenterFreq(centralFrequencyWithPrecision);
        }
        if (ztoADirection.getWidth() != null) {
            servicePathInputBuilder.setNmcWidth(
                    FrequencyGHz.getDefaultInstance(ztoADirection.getWidth().getValue().toCanonicalString())
            );
        } else if (ztoADirection.getRate() != null && ztoADirection.getModulationFormat() != null) {
            ModulationFormat modulationFormat = ModulationFormat.forName(ztoADirection.getModulationFormat());
            if (modulationFormat != null
                    && GridConstant.FREQUENCY_WIDTH_TABLE
                    .contains(ztoADirection.getRate(), modulationFormat)) {
                servicePathInputBuilder.setNmcWidth(FrequencyGHz
                        .getDefaultInstance(GridConstant.FREQUENCY_WIDTH_TABLE.get(ztoADirection.getRate(),
                                modulationFormat)));
            }
        }
        if (ztoADirection.getOpticalOperationalMode() != null) {
            servicePathInputBuilder.setOperationalMode(
                    new OperationalModeType(Uint16.valueOf(ztoADirection.getOpticalOperationalMode())));
        }
        servicePathInputBuilder.setModulationFormat(ztoADirection.getModulationFormat())
            .setAEndApiInfo(createAendApiInfo(pathDescription, false))
            .setZEndApiInfo(createZendApiInfo(pathDescription, false));
        return new ServicePathInputData(servicePathInputBuilder.build(), nodeLists);
    }

    // Adding createOtnServiceInputpath for A-Z and Z-A directions as one method
    public static OtnServicePathInput rendererCreateOtnServiceInput(String serviceName, Action operation,
        String serviceFormat, Uint32 serviceRate, PathDescription pathDescription, boolean asideToZside) {
        // If atoZ is set true use A-to-Z direction otherwise use Z-to-A
        List<org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.otn.renderer.nodes.Nodes> nodes =
            new ArrayList<>();
        NodeLists nodeLists =
            (asideToZside)
            ? getNodesListAToZ(pathDescription.getAToZDirection().nonnullAToZ().values().iterator())
            : getNodesListZtoA(pathDescription.getZToADirection().nonnullZToA().values().iterator());
        LOG.info("These are node-lists {}, {}", nodeLists.getRendererNodeList(), nodeLists.getOlmNodeList());
        for (Nodes node: nodeLists.getRendererNodeList()) {
            org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.otn.renderer.nodes.NodesBuilder nb
                = new org.opendaylight.yang.gen.v1.http.org.transportpce.common.types.rev250325.otn.renderer.nodes
                    .NodesBuilder().setNodeId(node.getNodeId()).setNetworkTp(node.getDestTp());
            if (node.getSrcTp() != null && node.getSrcTp().contains("NETWORK")) {
                nb.setNetwork2Tp(node.getSrcTp());
            } else {
                nb.setClientTp(node.getSrcTp());
            }
            nodes.add(nb.build());
        }
        OtnServicePathInputBuilder otnServicePathInputBuilder = new OtnServicePathInputBuilder()
            .setServiceName(serviceName)
            .setOperation(operation)
            .setServiceFormat(serviceFormat)
            .setServiceRate(serviceRate)
            .setNodes(nodes)
            .setAEndApiInfo(createAendApiInfo(pathDescription, true))
            .setZEndApiInfo(createZendApiInfo(pathDescription, true));

        // set the trib-slots and trib-ports for the lower oder odu
        if (serviceRate.intValue() == 1 || (serviceRate.intValue() == 10)
                && pathDescription.getAToZDirection().getMinTribSlot() != null) {
            Short tribPort = Short.valueOf(pathDescription.getAToZDirection().getMinTribSlot().getValue()
                .split("\\.")[0]);
            Short minTribSlot = Short.valueOf(pathDescription.getAToZDirection().getMinTribSlot().getValue()
                .split("\\.")[1]);
            otnServicePathInputBuilder
                .setTribPortNumber(tribPort)
                .setTribSlot(minTribSlot);
        }
        if (serviceRate.intValue() == 100 && pathDescription.getAToZDirection().getMinTribSlot() != null
                && pathDescription.getAToZDirection().getMaxTribSlot() != null) {
            Set<OpucnTribSlotDef> opucnTribSlotDefList = new HashSet<>();
            opucnTribSlotDefList.add(
                new OpucnTribSlotDef(pathDescription.getAToZDirection().getMinTribSlot().getValue()));
            opucnTribSlotDefList.add(
                new OpucnTribSlotDef(pathDescription.getAToZDirection().getMaxTribSlot().getValue()));
            otnServicePathInputBuilder.setOpucnTribSlots(opucnTribSlotDefList);
        }
        return otnServicePathInputBuilder.build();
    }

    public static ServicePathInput rendererDeleteServiceInput(String serviceName,
            ServiceDeleteInput serviceDeleteInput) {
        //TODO: finish model-model mapping
        return new ServicePathInputBuilder().setServiceName(serviceName).build();
    }

    public static Set<String> getLinksFromServicePathDescription(PathDescription pathDescription) {
        Set<String> linkidList = new HashSet<>();
        pathDescription.getAToZDirection().getAToZ().values().stream()
            .filter(lk -> "Link".equals(lk.getResource().getResource().implementedInterface().getSimpleName()))
            .forEach(rsc -> {
                Link link = (Link) rsc.getResource().getResource();
                linkidList.add(link.getLinkId());
            });
        pathDescription.getZToADirection().getZToA().values().stream()
            .filter(lk -> "Link".equals(lk.getResource().getResource().implementedInterface().getSimpleName()))
            .forEach(rsc -> {
                Link link = (Link) rsc.getResource().getResource();
                linkidList.add(link.getLinkId());
            });
        return linkidList;
    }

    private static NodeLists getNodesListZtoA(Iterator<ZToA> iterator) {
        Map<Integer, NodeIdPair> treeMap = new TreeMap<>();
        List<Nodes> olmList = new ArrayList<>();
        List<Nodes> list = new ArrayList<>();

        while (iterator.hasNext()) {
            ZToA pathDesObj = iterator.next();
            try {
                populateTreeMap(treeMap, pathDesObj.getResource().getResource(), pathDesObj.getId(), "ZtoA");
            } catch (IllegalArgumentException | SecurityException e) {
                //TODO: Auto-generated catch block
                LOG.error("Did not find the getResource method", e);
            }
        }
        populateNodeLists(treeMap, list, olmList, false);
        return new NodeLists(olmList, list);
    }

    private static NodeLists getNodesListAToZ(Iterator<AToZ> iterator) {
        Map<Integer, NodeIdPair> treeMap = new TreeMap<>();
        List<Nodes> list = new ArrayList<>();
        List<Nodes> olmList = new ArrayList<>();

        while (iterator.hasNext()) {
            AToZ pathDesObj = iterator.next();
            try {
                populateTreeMap(treeMap, pathDesObj.getResource().getResource(), pathDesObj.getId(), "AtoZ");
            } catch (IllegalArgumentException | SecurityException e) {
                //TODO: Auto-generated catch block
                LOG.error("Did not find the getResource method", e);
            }
        }
        populateNodeLists(treeMap, list, olmList, true);
        return new NodeLists(olmList, list);
    }

    private static void populateTreeMap(Map<Integer, NodeIdPair> treeMap, Resource rsrc, String sortId,
            String direction) {
        String resourceType = rsrc.implementedInterface().getSimpleName();
        LOG.info("Inside {} {}", direction, resourceType);
        switch (resourceType) {
            case TERMINATION_POINT:
                TerminationPoint tp = (TerminationPoint) rsrc;
                LOG.info(" TP is {} {}", tp.getTpId(), tp.getTpNodeId());
                String tpID = tp.getTpId();

                //TODO: do not rely on ID to be in certain format
                if (tpID.contains("CTP") || tpID.contains("CP")) {
                    return;
                }
                String nodeID = "";
                if (tpID.contains(StringConstants.TTP_TOKEN)) {
                    nodeID = tp.getTpNodeId().split("-DEG")[0];
                } else if (tpID.contains(StringConstants.PP_TOKEN)) {
                    nodeID = tp.getTpNodeId().split("-SRG")[0];
                } else if (tpID.contains(StringConstants.NETWORK_TOKEN)
                        || tpID.contains(StringConstants.CLIENT_TOKEN) || tpID.isEmpty()) {
                    nodeID = tp.getTpNodeId().split("-XPDR")[0];
                } else {
                    return;
                }
                int id = Integer.parseInt(sortId);
                treeMap.put(id, new NodeIdPair(nodeID, tpID));
                return;
            case LINK:
                LOG.info("The type is link");
                return;
            default:
                LOG.info("The type is not identified: {}", resourceType);
                return;
        }
    }

    @SuppressFBWarnings(
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
                LOG.warn("both, the source and destination id are not null!");
            }
        }
    }

    private static AEndApiInfo createAendApiInfo(PathDescription pathDescription, boolean isForOtn) {
        String anodeId = null;
        String sapi = null;
        String dapi = null;
        if (isForOtn) {
            anodeId = extractAendFromPathDescriptionForOtn(pathDescription).get("nodeId");
            sapi = PortMappingUtils.fnv1size64(
                String.join("-", anodeId, extractAendFromPathDescriptionForOtn(pathDescription).get("tpid")));
            dapi = PortMappingUtils.fnv1size64(
                String.join("-", extractZendFromPathDescriptionForOtn(pathDescription).get("nodeId"),
                    extractZendFromPathDescriptionForOtn(pathDescription).get("tpid")));
        } else {
            anodeId = extractAendFromPathDescriptionForOptical(pathDescription).get("nodeId");
            sapi = PortMappingUtils.fnv1size64(
                String.join("-", anodeId, extractAendFromPathDescriptionForOptical(pathDescription).get("tpid")));
            dapi = PortMappingUtils.fnv1size64(
                String.join("-", extractZendFromPathDescriptionForOptical(pathDescription).get("nodeId"),
                    extractZendFromPathDescriptionForOptical(pathDescription).get("tpid")));
        }
        return new AEndApiInfoBuilder()
            .setSapi(sapi)
            .setExpectedDapi(sapi)
            .setDapi(dapi)
            .setExpectedSapi(dapi)
            .setNodeId(anodeId)
            .build();
    }

    private static ZEndApiInfo createZendApiInfo(PathDescription pathDescription, boolean isForOtn) {
        String znodeId = null;
        String sapi = null;
        String dapi = null;
        if (isForOtn) {
            znodeId = extractZendFromPathDescriptionForOtn(pathDescription).get("nodeId");
            sapi = PortMappingUtils.fnv1size64(
                String.join("-", znodeId, extractZendFromPathDescriptionForOtn(pathDescription).get("tpid")));
            dapi = PortMappingUtils.fnv1size64(
                String.join("-", extractAendFromPathDescriptionForOtn(pathDescription).get("nodeId"),
                    extractAendFromPathDescriptionForOtn(pathDescription).get("tpid")));
        } else {
            znodeId = extractZendFromPathDescriptionForOptical(pathDescription).get("nodeId");
            sapi = PortMappingUtils.fnv1size64(
                String.join("-", znodeId, extractZendFromPathDescriptionForOptical(pathDescription).get("tpid")));
            dapi = PortMappingUtils.fnv1size64(
                String.join("-", extractAendFromPathDescriptionForOptical(pathDescription).get("nodeId"),
                    extractAendFromPathDescriptionForOptical(pathDescription).get("tpid")));
        }
        return new ZEndApiInfoBuilder()
            .setSapi(sapi)
            .setExpectedDapi(sapi)
            .setDapi(dapi)
            .setExpectedSapi(dapi)
            .setNodeId(znodeId)
            .build();
    }

    private static Map<String, String> extractAendFromPathDescriptionForOtn(PathDescription pathDescription) {
        List<AToZ> tpList = pathDescription.getAToZDirection().getAToZ().values().stream()
            .sorted((az1, az2) -> Integer.compare(Integer.parseInt(az1.getId()), Integer.parseInt(az2.getId())))
            .filter(az -> TERMINATION_POINT.equals(az.getResource().getResource().implementedInterface()
                .getSimpleName()))
            .collect(Collectors.toList());
        for (AToZ atoZ : tpList) {
            TerminationPoint tp = (TerminationPoint) atoZ.getResource().getResource();
            if (!tp.getTpId().isEmpty() && !tp.getTpNodeId().isEmpty()) {
                String nodeId = tp.getTpNodeId();
                String lcp = tp.getTpId();
                return Map.of("nodeId", nodeId, "tpid", lcp);
            }
        }
        return Map.of();
    }

    private static Map<String, String> extractZendFromPathDescriptionForOtn(PathDescription pathDescription) {
        List<ZToA> tpList = pathDescription.getZToADirection().getZToA().values().stream()
            .sorted((az1, az2) -> Integer.compare(Integer.parseInt(az1.getId()), Integer.parseInt(az2.getId())))
            .filter(az -> TERMINATION_POINT.equals(az.getResource().getResource().implementedInterface()
                .getSimpleName()))
            .collect(Collectors.toList());
        for (ZToA ztoA : tpList) {
            TerminationPoint tp = (TerminationPoint) ztoA.getResource().getResource();
            if (!tp.getTpId().isEmpty() && !tp.getTpNodeId().isEmpty()) {
                String nodeId = tp.getTpNodeId();
                String lcp = tp.getTpId();
                return Map.of("nodeId", nodeId, "tpid", lcp);
            }
        }
        return Map.of();
    }

    private static Map<String, String> extractAendFromPathDescriptionForOptical(PathDescription pathDescription) {
        List<AToZ> tpList = pathDescription.getAToZDirection().getAToZ().values().stream()
            .sorted((az1, az2) -> Integer.compare(Integer.parseInt(az1.getId()), Integer.parseInt(az2.getId())))
            .filter(az -> TERMINATION_POINT.equals(az.getResource().getResource().implementedInterface()
                .getSimpleName()))
            .collect(Collectors.toList());
        for (AToZ atoZ : tpList) {
            TerminationPoint tp = (TerminationPoint) atoZ.getResource().getResource();
            if (!tp.getTpId().contains("CLIENT") && !tp.getTpId().isEmpty() && !tp.getTpNodeId().isEmpty()) {
//                String nodeId = tp.getTpNodeId();
//                String lcp = tp.getTpId();
                return Map.of("nodeId", tp.getTpNodeId(), "tpid", tp.getTpId());
            }
        }
        return Map.of();
    }

    private static Map<String, String> extractZendFromPathDescriptionForOptical(PathDescription pathDescription) {
        List<ZToA> tpList = pathDescription.getZToADirection().getZToA().values().stream()
            .sorted((az1, az2) -> Integer.compare(Integer.parseInt(az1.getId()), Integer.parseInt(az2.getId())))
            .filter(az -> TERMINATION_POINT.equals(az.getResource().getResource().implementedInterface()
                .getSimpleName()))
            .collect(Collectors.toList());
        for (ZToA ztoA : tpList) {
            TerminationPoint tp = (TerminationPoint) ztoA.getResource().getResource();
            if (!tp.getTpId().contains("CLIENT")  && !tp.getTpId().isEmpty() && !tp.getTpNodeId().isEmpty()) {
//                String nodeId = tp.getTpNodeId();
//                String lcp = tp.getTpId();
                return Map.of("nodeId", tp.getTpNodeId(), "tpid", tp.getTpId());
            }
        }
        return Map.of();
    }
}
