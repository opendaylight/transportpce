/*
 * Copyright (c) 2018 AT&T and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fd.honeycomb.transportpce.binding.converter;

import com.google.common.base.Preconditions;
import io.fd.honeycomb.transportpce.binding.converter.api.DataObjectConverter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.mdsal.binding.dom.codec.api.BindingNormalizedNodeSerializer;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNodes;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts XML and {@link DataObject} vice versa.
 *
 */
public abstract class AbstractDataObjectConverter implements DataObjectConverter {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataObjectConverter.class);

    private final SchemaContext schemaContext;
    private final BindingNormalizedNodeSerializer codecRegistry;

    /**
     * This is the default constructor, which should be used.
     *
     * @param schemaContext schema context for converter
     * @param codecRegistry codec registry used for converting
     *
     */
    protected AbstractDataObjectConverter(SchemaContext schemaContext, BindingNormalizedNodeSerializer codecRegistry) {
        this.schemaContext = schemaContext;
        this.codecRegistry = codecRegistry;
    }

    public SchemaContext getSchemaContext() {
        return schemaContext;
    }

    public BindingNormalizedNodeSerializer getCodecRegistry() {
        return codecRegistry;
    }

    /**
     * Transforms the given input {@link NormalizedNode} into the given
     * {@link DataObject}.
     *
     * @param normalizedNode normalized node you want to convert
     * @param rootNode {@link QName} of converted normalized node root
     *
     * <p>
     * The input object should be {@link ContainerNode}
     * </p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends DataObject> Optional<T> getDataObject(
            @Nonnull NormalizedNode<?, ?> normalizedNode,
            @Nonnull QName rootNode) {
        Preconditions.checkNotNull(normalizedNode);
        if (normalizedNode instanceof ContainerNode) {
            YangInstanceIdentifier.PathArgument directChildIdentifier =
                    YangInstanceIdentifier.of(rootNode).getLastPathArgument();
            Optional<NormalizedNode<?, ?>> directChild =
                    NormalizedNodes.getDirectChild(normalizedNode, directChildIdentifier);
            if (!directChild.isPresent()) {
                throw new IllegalStateException(String.format("Could not get the direct child of %s", rootNode));
            }
            normalizedNode = directChild.get();
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
    public <T extends DataObject> Optional<T> getDataObjectFromRpc(
            @Nonnull NormalizedNode<?, ?> normalizedNode,
            @Nonnull SchemaPath rpcSchemaPath) {

        if (! (normalizedNode instanceof ContainerNode)) {
            LOG.error("converting normalized node is not ContainerNode. It's actual type is {}",
                    normalizedNode.getClass().getSimpleName());
            return Optional.empty();
        }
        T rpcDataObject = (T) codecRegistry.fromNormalizedNodeRpcData(rpcSchemaPath, (ContainerNode) normalizedNode);
        return Optional.ofNullable(rpcDataObject);
    }

    @Override
    public <T extends DataObject> Optional<NormalizedNode<?, ?>> toNormalizedNodes(@Nonnull T object,
            Class<T> dataObjectClass) {
        Entry<YangInstanceIdentifier, NormalizedNode<?, ?>> normalizedNode =
                codecRegistry.toNormalizedNode(InstanceIdentifier.create(dataObjectClass), object);
        return Optional.ofNullable(normalizedNode.getValue());
    }

    @Override
    public <T extends DataObject> ConvertType<T> dataContainer() {
        return (object, objectClass) -> {
            NormalizedNode<?, ?> value =
                    getCodecRegistry().toNormalizedNode(InstanceIdentifier.create(objectClass), object).getValue();
            return Optional.ofNullable(value);
        };
    }

    @Override
    public <T extends DataContainer> ConvertType<T> rpcData() {
        return (object, objectClass) -> {
            ContainerNode normalizedNodeRpcData = getCodecRegistry().toNormalizedNodeRpcData(object);
            return Optional.ofNullable(normalizedNodeRpcData);
        };
    }

    @Override
    public <T extends Notification> ConvertType<T> notification() {
        return (object, objectClass) -> {
            ContainerNode normalizedNodeNotification = getCodecRegistry().toNormalizedNodeNotification(object);
            return Optional.ofNullable(normalizedNodeNotification);
        };
    }
}
