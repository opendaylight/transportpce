/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.servicehandler;

import java.util.Map;

/*
 * Enum class to identify ServiceAEnd / serviceZEnd.
 *
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
enum MyEndpoint {
    SERVICEAEND(1),
    SERVICEZEND(2);

    int value;
    private static final Map<Integer, MyEndpoint> VALUE_MAP;

    MyEndpoint(int value) {
        this.value = value;
    }

    static {
        final com.google.common.collect.ImmutableMap.Builder<java.lang.Integer, MyEndpoint> b =
                com.google.common.collect.ImmutableMap.builder();
        for (MyEndpoint enumItem : MyEndpoint.values()) {
            b.put(enumItem.value, enumItem);
        }

        VALUE_MAP = b.build();
    }

    /*
     * Get integer value.
     *
     * @return integer value.
     */
    public int getIntValue() {
        return value;
    }

    /*
     * Get Endpoint value.
     *
     * @param valueArg
     *            Integer to identify Enum
     * @return corresponding ServiceFormat item
     */
    public static MyEndpoint forValue(int valueArg) {
        return VALUE_MAP.get(valueArg);
    }
}
