/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.util.Map;
import java.util.Optional;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectReference;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.binding.data.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier.Absolute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts XML and {@link DataObject} vice versa.
 *
 */
public abstract class AbstractDataObjectConverter implements DataObjectConverter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataObjectConverter.class);

    private final EffectiveModelContext schemaContext;
    private final BindingNormalizedNodeSerializer codecRegistry;

    /**
     * This is the default constructor, which should be used.
     *
     * @param schemaContext schema context for converter
     * @param codecRegistry codec registry used for converting
     *
     */
    protected AbstractDataObjectConverter(EffectiveModelContext schemaContext,
            BindingNormalizedNodeSerializer codecRegistry) {
        this.schemaContext = schemaContext;
        this.codecRegistry = codecRegistry;
    }

    public EffectiveModelContext getSchemaContext() {
        return schemaContext;
    }

    public BindingNormalizedNodeSerializer getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Transforms the given input {@link NormalizedNode} into the given
     * {@link DataObject}.
     * The input object should be {@link ContainerNode}
     *
     * @param normalizedNode normalized node you want to convert
     * @param rootNode {@link QName} of converted normalized node root
     *
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends DataObject> Optional<T> getDataObject(
            @NonNull NormalizedNode normalizedNode,
            @NonNull QName rootNode) {
        //Preconditions.checkNotNull(normalizedNode);
        if (normalizedNode instanceof ContainerNode) {
            YangInstanceIdentifier.PathArgument directChildIdentifier =
                    YangInstanceIdentifier.of(rootNode).getLastPathArgument();
            Optional<NormalizedNode> directChild =
                    NormalizedNodes.getDirectChild(normalizedNode, directChildIdentifier);
            if (!directChild.isPresent()) {
                throw new IllegalStateException(String.format("Could not get the direct child of %s", rootNode));
            }
            normalizedNode = directChild.orElseThrow();
        }
        YangInstanceIdentifier rootNodeYangInstanceIdentifier = YangInstanceIdentifier.of(rootNode);

        Map.Entry<?, ?> bindingNodeEntry =
                codecRegistry.fromNormalizedNode(rootNodeYangInstanceIdentifier, normalizedNode);
        if (bindingNodeEntry == null) {
            return Optional.empty();
        }
        return Optional.ofNullable((T) bindingNodeEntry.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DataObject> Optional<T> getDataObjectFromRpc(@NonNull NormalizedNode normalizedNode) {

        if (! (normalizedNode instanceof ContainerNode)) {
            LOG.error("converting normalized node is not ContainerNode. It's actual type is {}",
                    normalizedNode.getClass().getSimpleName());
            return Optional.empty();
        }
        T rpcDataObject = (T) codecRegistry
            .fromNormalizedNodeRpcData(Absolute.of(EffectiveModelContext.NAME), (ContainerNode) normalizedNode);
        return Optional.ofNullable(rpcDataObject);
    }

    @Override
    @SuppressWarnings({"rawtypes","unchecked"})
    public <T extends DataObject> Optional<NormalizedNode> toNormalizedNodes(@NonNull T object,
            Class dataObjectClass) {
        return Optional.of(codecRegistry.toNormalizedDataObject(
                DataObjectReference.builder(dataObjectClass).build(),
                object)
            .node());
    }

    @Override
    public <T extends DataObject> ConvertType<T> dataContainer() {
//        return (object, objectClass) -> {
//            NormalizedNode value =
//                    getCodecRegistry().toNormalizedNode(InstanceIdentifier.create(objectClass), object).getValue();
//            return Optional.ofNullable(value);
//        };
        return null;
    }

    @Override
    public <T extends DataContainer> ConvertType<T> rpcData() {
        return (object, objectClass) -> {
            ContainerNode normalizedNodeRpcData = getCodecRegistry().toNormalizedNodeRpcData(object);
            return Optional.ofNullable(normalizedNodeRpcData);
        };
    }

    @Override
    @SuppressWarnings("rawtypes")
    public <T extends Notification> ConvertType<T> notification() {
        return (object, objectClass) -> {
            ContainerNode normalizedNodeNotification = getCodecRegistry().toNormalizedNodeNotification(object);
            return Optional.ofNullable(normalizedNodeNotification);
        };
    }
}
