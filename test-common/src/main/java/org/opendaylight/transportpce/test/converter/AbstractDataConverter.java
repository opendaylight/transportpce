/*
 * Copyright © 2025 Orange Innovation, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.test.converter;

import java.util.Set;
import org.opendaylight.yangtools.binding.DataObject;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingCodecContext;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;
import org.opendaylight.yangtools.binding.runtime.api.AbstractBindingRuntimeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class which may be helpful while manipulating JSON and XML converters.
 * @param <T> Type of the DataObject to serialize / deserialize
 */
public abstract class AbstractDataConverter<T extends DataObject> extends AbstractConverter
        implements DataConverter<T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDataConverter.class);
    protected final AbstractBindingRuntimeContext runtimeContext;
    protected final BindingCodecContext bindingCodecContext;


    protected AbstractDataConverter(Set<YangModuleInfo> models) {
        this.runtimeContext = createBindingRuntimeContext(models);
        this.bindingCodecContext = new BindingCodecContext(runtimeContext);
    }

}
