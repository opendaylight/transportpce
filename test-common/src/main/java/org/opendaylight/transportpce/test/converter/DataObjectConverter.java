/*
 * Copyright © 2016 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;

public interface DataObjectConverter {

    <T extends DataObject> Optional<T> getDataObject(
            @Nonnull NormalizedNode normalizedNode,
            @Nonnull QName rootNode);

    <T extends DataObject> Optional<T> getDataObjectFromRpc(
            @Nonnull NormalizedNode normalizedNode,
            @Nonnull SchemaPath rpcSchemaPath);

    Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull InputStream inputStream);

    Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull Reader inputReader, SchemaNode parentSchema);

    Optional<NormalizedNode> transformIntoNormalizedNode(
            @Nonnull Reader inputReader);

    <T extends DataObject> Writer writerFromDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType);

    <T extends DataObject> Writer writerFromRpcDataObject(@Nonnull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType, QName rpcOutputQName, String rpcName);

    <T extends DataObject> Optional<NormalizedNode> toNormalizedNodes(@Nonnull T object,
            Class<T> dataObjectClass);

    public interface ConvertType<T> {
        Optional<NormalizedNode> toNormalizedNodes(T object, Class<T> clazz);
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
     *      representing rpc data
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
