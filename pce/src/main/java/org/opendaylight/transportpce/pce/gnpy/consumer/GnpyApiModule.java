/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.gnpy.consumer;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.PackageVersion;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Request;
import org.opendaylight.yang.gen.v1.gnpy.gnpy.api.rev201022.Result;


//This class is a temporary workaround while waiting jackson
//support in yang tools https://git.opendaylight.org/gerrit/c/yangtools/+/94852
public class GnpyApiModule extends SimpleModule {

    private static final long serialVersionUID = 1L;

    public GnpyApiModule(JsonStringConverter<Request> requestConverter, JsonStringConverter<Result> resultConverter) {
        super(PackageVersion.VERSION);
        addSerializer(Request.class, new RequestSerializer(requestConverter));
        addDeserializer(Result.class, new ResultDeserializer(resultConverter));
    }
}
