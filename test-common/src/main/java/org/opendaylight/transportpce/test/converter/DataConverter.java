/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.io.Reader;
import org.opendaylight.yangtools.binding.BindingObject;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.DataObjectIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainer;

/**
 * This interface may be useful to serialize / deserialize {@link DataContainer} or a {@link BindingObject}s to / from
 * its XML or JSON representation. Currently there are two implementations XmlDataConverter and
 * {@link JsonDataConverter}.
 * @param <T> Type of the DataObject to serialize / deserialize
 */
public interface DataConverter<T extends DataObject> {

    String serialize(DataObjectIdentifier<T> id, T dataContainer) throws ProcessingException;

    T deserialize(String data, QName object) throws ProcessingException;

    T deserialize(Reader data, QName object) throws ProcessingException;
}
