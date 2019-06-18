/*
 * Copyright © 2018 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonReader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javassist.ClassPool;

import javax.annotation.Nonnull;

import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.mdsal.binding.dom.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.mdsal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.mdsal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.generator.util.JavassistUtils;
import org.opendaylight.mdsal.binding.spec.reflect.BindingReflections;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.Result;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.explicit.route.hop.type.num.unnum.hop.NumUnnumHop;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.generic.path.properties.path.properties.PathMetric;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.generic.path.properties.path.properties.PathRouteObjects;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.result.Response;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.result.response.response.type.NoPathCase;
import org.opendaylight.yang.gen.v1.gnpy.path.rev190502.result.response.response.type.PathCase;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.General;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.GeneralBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.Include;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.IncludeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.include_.OrderedHops;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.constraints.sp.co.routing.or.general.general.include_.OrderedHopsBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.ordered.constraints.sp.HopType;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.ordered.constraints.sp.HopTypeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.ordered.constraints.sp.hop.type.hop.type.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraints;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017.routing.constraints.sp.HardConstraintsBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for analyzing the result sent by GNPy.
 *
 * @author Ahmed Triki ( ahmed.triki@orange.com )
 *
 */

public class GnpyResult {

    private static final Logger LOG = LoggerFactory.getLogger(GnpyResult.class);
    private Response response = null;

    public GnpyResult(String gnpyResponseString) throws Exception {

        // Create the schema context
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        Iterable<? extends YangModuleInfo> moduleInfos;
        moduleInfos = Collections.singleton(BindingReflections.getModuleInfo(Result.class));
        moduleContext.addModuleInfos(moduleInfos);
        SchemaContext schemaContext = moduleContext.tryToCreateSchemaContext().get();

        // Create the binding binding normalized node codec registry
        BindingRuntimeContext bindingRuntimeContext = BindingRuntimeContext.create(moduleContext, schemaContext);
        final BindingNormalizedNodeCodecRegistry codecRegistry = new BindingNormalizedNodeCodecRegistry(
                StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
        codecRegistry.onBindingRuntimeContextUpdated(bindingRuntimeContext);

        // Create the data object
        QName pathQname = QName.create("gnpy:path", "2019-05-02", "result");
        LOG.debug("the Qname is {} / namesapce {} ; module {}; ", pathQname.toString(), pathQname.getNamespace(),
                pathQname.getModule());
        YangInstanceIdentifier yangId = YangInstanceIdentifier.of(pathQname);
        DataObject dataObject = null;
        //Create the object response
        //Create JsonReader from String
        InputStream streamGnpyRespnse = new ByteArrayInputStream(gnpyResponseString.getBytes(StandardCharsets.UTF_8));
        InputStreamReader gnpyResultReader = new InputStreamReader(streamGnpyRespnse);
        JsonReader jsonReader = new JsonReader(gnpyResultReader);
        Optional<NormalizedNode<? extends PathArgument, ?>> transformIntoNormalizedNode = parseInputJSON(jsonReader,
                Result.class);
        NormalizedNode<? extends PathArgument, ?> normalizedNode = transformIntoNormalizedNode.get();
        if (codecRegistry.fromNormalizedNode(yangId, normalizedNode) != null) {
            LOG.debug("The key of the generated object",
                    codecRegistry.fromNormalizedNode(yangId, normalizedNode).getKey());
            dataObject = codecRegistry.fromNormalizedNode(yangId, normalizedNode).getValue();
        } else {
            LOG.warn("The codec registry from the normalized node is null!");
        }
        List<Response> responses = null;
        responses = ((Result) dataObject).getResponse();
        if (responses != null) {
            LOG.info("The response id is {}; ", responses.get(0).getResponseId());
        } else {
            LOG.warn("The response is null!");
        }
        this.response = responses.get(0);
        analyzeResult();
    }

    public boolean getPathFeasibility() {
        boolean isFeasible = false;
        if (response != null) {
            if (response.getResponseType() instanceof NoPathCase) {
                isFeasible = false;
                LOG.info("The path is not feasible ");
            } else if (response.getResponseType() instanceof PathCase) {
                isFeasible = true;
                LOG.info("The path is feasible ");
            }
        }
        return isFeasible;
    }

    public void analyzeResult() {
        if (response != null) {
            Long responseId = response.getResponseId();
            LOG.info("Response-Id {}", responseId);
            if (response.getResponseType() instanceof NoPathCase) {
                NoPathCase noPathCase = (NoPathCase) response.getResponseType();
                String noPathType = noPathCase.getNoPath().getNoPath();
                LOG.info("GNPy: No path - {}",noPathType);
                if (((noPathType.equals("NO_FEASIBLE_BAUDRATE_WITH_SPACING"))
                        && (noPathType.equals("NO_FEASIBLE_MODE"))) && ((noPathType.equals("MODE_NOT_FEASIBLE"))
                        && (noPathType.equals("NO_SPECTRUM")))) {
                    List<PathMetric> pathMetricList = noPathCase.getNoPath().getPathProperties().getPathMetric();
                    LOG.info("GNPy : path is not feasible : {}", noPathType);
                    for (PathMetric pathMetric : pathMetricList) {
                        String metricType = pathMetric.getMetricType().getSimpleName();
                        BigDecimal accumulativeValue = pathMetric.getAccumulativeValue();
                        LOG.info("Metric type {} // AccumulatriveValue {}", metricType, accumulativeValue);
                    }
                }
            } else if (response.getResponseType() instanceof PathCase) {
                LOG.info("GNPy : path is feasible");
                PathCase pathCase = (PathCase) response.getResponseType();
                List<PathMetric> pathMetricList = pathCase.getPathProperties().getPathMetric();
                for (PathMetric pathMetric : pathMetricList) {
                    String metricType = pathMetric.getMetricType().getSimpleName();
                    BigDecimal accumulativeValue = pathMetric.getAccumulativeValue();
                    LOG.info("Metric type {} // AccumulatriveValue {}", metricType, accumulativeValue);
                }
            }
        }
    }

    public HardConstraints analyzeGnpyPath() {
        HardConstraints hardConstraints = null;
        if (response != null) {
            Long responseId = response.getResponseId();
            LOG.info("Response-Id {}", responseId);
            if (response.getResponseType() instanceof NoPathCase) {
                NoPathCase noPathCase = (NoPathCase) response.getResponseType();
                LOG.info("No path feasible {}", noPathCase.toString());
            } else if (response.getResponseType() instanceof PathCase) {
                PathCase pathCase = (PathCase) response.getResponseType();
                List<PathMetric> pathMetricList = pathCase.getPathProperties().getPathMetric();
                for (PathMetric pathMetric : pathMetricList) {
                    String metricType = pathMetric.getMetricType().getSimpleName();
                    BigDecimal accumulativeValue = pathMetric.getAccumulativeValue();
                    LOG.info("Metric type {} // AccumulatriveValue {}", metricType, accumulativeValue);
                }

                // Includes the list of nodes in the GNPy computed path as constraints for the PCE
                List<OrderedHops> orderedHopsList = null;
                List<PathRouteObjects> pathRouteObjectList = pathCase.getPathProperties().getPathRouteObjects();
                int counter = 0;
                for (PathRouteObjects pathRouteObjects : pathRouteObjectList) {
                    if (pathRouteObjects.getPathRouteObject().getType() instanceof NumUnnumHop) {
                        NumUnnumHop numUnnumHop = (NumUnnumHop) pathRouteObjects.getPathRouteObject().getType();
                        String nodeId = numUnnumHop.getNodeId();
                        org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.routing.constraints.rev171017
                                .ordered.constraints.sp.hop.type.hop.type.Node node = new NodeBuilder()
                                .setNodeId(nodeId).build();
                        HopType hopType = new HopTypeBuilder().setHopType(node).build();
                        OrderedHops orderedHops = new OrderedHopsBuilder().setHopNumber(counter).setHopType(hopType)
                                .build();
                        LOG.info("- gnpyResult class : Hard Constraint: {} // - Hop Node {}", counter, nodeId);
                        orderedHopsList.add(orderedHops);
                        counter++;
                    }
                }
                Include include = new IncludeBuilder().setOrderedHops(orderedHopsList).build();
                General general = new GeneralBuilder().setInclude(include).build();
                hardConstraints = new HardConstraintsBuilder().setCoRoutingOrGeneral(general).build();
            }
        }
        return hardConstraints;
    }

    /**
     * Parses the input json with concrete implementation of
     * {@link JsonParserStream}.
     *
     * @param reader
     *            of the given JSON
     * @throws Exception
     *
     */
    private Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> parseInputJSON(JsonReader reader,
            Class<? extends DataObject> objectClass) throws Exception {
        NormalizedNodeResult result = new NormalizedNodeResult();
        SchemaContext schemaContext = getSchemaContext(objectClass);
        try (NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
                JsonParserStream jsonParser = JsonParserStream.create(streamWriter, schemaContext, schemaContext);) {
            LOG.debug("GNPy: the path to the reader {}", reader.getPath());
            LOG.debug("GNPy: the reader {}", reader.toString());
            LOG.debug("GNPy: the jsonParser class {} // jsonParser to string {}", jsonParser.getClass(),
                    jsonParser.toString());
            jsonParser.parse(reader);
        } catch (IOException e) {
            LOG.warn("GNPy: exception {} occured during parsing Json input stream", e.getMessage());
            return Optional.empty();
        }
        return Optional.ofNullable(result.getResult());
    }

    private SchemaContext getSchemaContext(Class<? extends DataObject> objectClass) throws Exception {
        final ModuleInfoBackedContext moduleContext = ModuleInfoBackedContext.create();
        Iterable<? extends YangModuleInfo> moduleInfos;
        SchemaContext schemaContext = null;
        moduleInfos = Collections.singleton(BindingReflections.getModuleInfo(objectClass));
        moduleContext.addModuleInfos(moduleInfos);
        schemaContext = moduleContext.tryToCreateSchemaContext().get();
        return schemaContext;
    }

    /**
     * Transforms the given input {@link NormalizedNode} into the given
     * {@link DataObject}.
     *
     * @param normalizedNode
     *            normalized node you want to convert
     * @param rootNode
     *            {@link QName} of converted normalized node root
     *
     *            <p>
     *            The input object should be {@link ContainerNode}
     *            </p>
     */
    public <T extends DataObject> Optional<T> getDataObject(@Nonnull NormalizedNode<?, ?> normalizedNode,
            @Nonnull QName rootNode, BindingNormalizedNodeSerializer codecRegistry) {
        if (normalizedNode != null) {
            LOG.debug("GNPy: The codecRegistry is ", codecRegistry.toString());
        } else {
            LOG.warn("GNPy: The codecRegistry is null");
        }
        Preconditions.checkNotNull(normalizedNode);
        if (normalizedNode instanceof ContainerNode) {
            YangInstanceIdentifier.PathArgument directChildIdentifier = YangInstanceIdentifier.of(rootNode)
                    .getLastPathArgument();
            Optional<NormalizedNode<?, ?>> directChild = NormalizedNodes.getDirectChild(normalizedNode,
                    directChildIdentifier);
            if (!directChild.isPresent()) {
                throw new IllegalStateException(String.format("Could not get the direct child of %s", rootNode));
            }
            normalizedNode = directChild.get();
            LOG.debug("GNPy: the normalized node is ", normalizedNode.getNodeType());
        }
        YangInstanceIdentifier rootNodeYangInstanceIdentifier = YangInstanceIdentifier.of(rootNode);
        LOG.debug("GNPy: the root Node Yang Instance Identifier is ", rootNodeYangInstanceIdentifier.toString());
        Map.Entry<?, ?> bindingNodeEntry = codecRegistry.fromNormalizedNode(rootNodeYangInstanceIdentifier,
                normalizedNode);
        if (bindingNodeEntry == null) {
            LOG.debug("The binding Node Entry is null");
            return Optional.empty();
        }
        return Optional.ofNullable((T) bindingNodeEntry.getValue());
    }

    public Response getResponse() {
        return response;
    }
}
