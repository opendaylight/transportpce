/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.catalog;

import java.io.Serializable;
import java.util.Comparator;
import org.opendaylight.yang.gen.v1.http.org.openroadm.operational.mode.catalog.rev211210.operational.mode.transponder.parameters.Penalties;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class PenaltiesComparator implements Comparator<Penalties>, Serializable {
    @Override
    public int compare(Penalties o1, Penalties o2) {
        return Double.compare(o1.getUpToBoundary().doubleValue(), o2.getUpToBoundary().doubleValue());
    }
}
