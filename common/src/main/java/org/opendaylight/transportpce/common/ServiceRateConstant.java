/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import org.opendaylight.yangtools.yang.common.Uint32;

/**
 * Constant class for service rate.
 *
 */
public final class ServiceRateConstant {

    public static final Uint32 RATE_100 = Uint32.valueOf(100);
    public static final Uint32 RATE_200 = Uint32.valueOf(200);
    public static final Uint32 RATE_300 = Uint32.valueOf(300);
    public static final Uint32 RATE_400 = Uint32.valueOf(400);

    private ServiceRateConstant() {
    }
}
