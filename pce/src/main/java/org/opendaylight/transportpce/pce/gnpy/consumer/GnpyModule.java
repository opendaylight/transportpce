/*
 * Copyright © 2024 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev220221.Request;
import org.opendaylight.yang.gen.v1.gnpy.path.rev220615.Result;

public class GnpyModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public GnpyModule(JsonStringConverter<Request> requestConverter, JsonStringConverter<Result> resultConverter) {
        addSerializer(Request.class, new RequestSerializer(requestConverter));
        addDeserializer(Result.class, new ResultDeserializer(resultConverter));
    }

}
