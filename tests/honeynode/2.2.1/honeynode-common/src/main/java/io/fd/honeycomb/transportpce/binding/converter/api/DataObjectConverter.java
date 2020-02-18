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
package io.fd.honeycomb.transportpce.binding.converter.api;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public interface DataObjectConverter {

    <T extends DataObject> Optional<T> getDataObject(
            @Nonnull NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> normalizedNode,
            @Nonnull QName rootNode);

    <T extends DataObject> Optional<T> getDataObjectFromRpc(
            @Nonnull NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?> normalizedNode,
            @Nonnull SchemaPath rpcSchemaPath);

    Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> transformIntoNormalizedNode(
            @Nonnull InputStream inputStream);

    Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> transformIntoNormalizedNode(
            @Nonnull Reader inputReader, SchemaNode parentSchema);

    Optional<NormalizedNode<? extends YangInstanceIdentifier.PathArgument, ?>> transformIntoNormalizedNode(
            @Nonnull Reader inputReader);

    <T extends DataObject> Writer writerFromDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType);

    <T extends DataObject> Writer writerFromRpcDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType, QName rpcOutputQName, String rpcName);

    <T extends DataObject> Optional<NormalizedNode<?, ?>> toNormalizedNodes(@Nonnull T object,
            Class<T> dataObjectClass);

    public interface ConvertType<T> {
        Optional<NormalizedNode<?, ?>> toNormalizedNodes(T object, Class<T> clazz);
    }

    /**
     * Returns a converter for {@link DataObject} container type.
     * @param <T> T extends DataObject
     *
     * @return {@link ConvertType} converter for {@link DataContainer}
     */
    <T extends DataObject> ConvertType<T> dataContainer();

    /**
     * Returns converter for {@link DataContainer} rpc type.
     * @param <T> T extends DataContainer
     *
     * @return {@link ConvertType} converter for {@link DataContainer}
     * representing rpc data
     */
    <T extends DataContainer> ConvertType<T> rpcData();

    /**
     * Return converter for {@link Notification}.
     * @param <T> T extends Notification
     *
     * @return {@link ConvertType} converter for {@link Notification}
     */
    <T extends Notification> ConvertType<T> notification();

}
