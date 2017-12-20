/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.servicehandler;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Enum class to identify ServiceAEnd / serviceZEnd.
 *
 */
public enum ServiceEndpointType {
    SERVICEAEND(1),
    SERVICEZEND(2);

    int value;
    private static final Map<Integer, ServiceEndpointType> VALUE_MAP;

    ServiceEndpointType(int value) {
        this.value = value;
    }

    static {
        final ImmutableMap.Builder<java.lang.Integer, ServiceEndpointType> builder =
                ImmutableMap.builder();
        for (ServiceEndpointType enumItem : ServiceEndpointType.values()) {
            builder.put(enumItem.value, enumItem);
        }
        VALUE_MAP = builder.build();
    }

    /**
     * Get integer value.
     *
     * @return integer value.
     */
    public int getIntValue() {
        return value;
    }

    /**
     * Get Endpoint value.
     *
     * @param valueArg
     *            Integer to identify Enum
     * @return corresponding ServiceFormat item
     */
    public static ServiceEndpointType forValue(int valueArg) {
        return VALUE_MAP.get(valueArg);
    }

}
