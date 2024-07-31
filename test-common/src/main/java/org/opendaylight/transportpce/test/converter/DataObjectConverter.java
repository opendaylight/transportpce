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
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.yangtools.binding.DataContainer;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.Notification;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;

public interface DataObjectConverter {

    <T extends DataObject> Optional<T> getDataObject(
            @NonNull NormalizedNode normalizedNode,
            @NonNull QName rootNode);

    <T extends DataObject> Optional<T> getDataObjectFromRpc(
            @NonNull NormalizedNode normalizedNode);

    Optional<NormalizedNode> transformIntoNormalizedNode(
            @NonNull InputStream inputStream);

    Optional<NormalizedNode> transformIntoNormalizedNode(
            @NonNull Reader inputReader, SchemaNode parentSchema);

    Optional<NormalizedNode> transformIntoNormalizedNode(
            @NonNull Reader inputReader);

    <T extends DataObject> Writer writerFromDataObject(@NonNull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType);

    <T extends DataObject> Writer writerFromRpcDataObject(@NonNull DataObject object, Class<T> dataObjectClass,
            ConvertType<T> convertType, QName rpcOutputQName, String rpcName);

    @SuppressWarnings("rawtypes")
    <T extends DataObject> Optional<NormalizedNode> toNormalizedNodes(@NonNull T object, Class dataObjectClass);

    interface ConvertType<T> {
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
    @SuppressWarnings("rawtypes")
    <T extends Notification> ConvertType<T> notification();

}
