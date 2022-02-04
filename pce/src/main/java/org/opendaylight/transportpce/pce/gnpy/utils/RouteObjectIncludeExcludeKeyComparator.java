/*
 * Copyright © 2022 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.pce.gnpy.utils;

import java.io.Serializable;
import java.util.Comparator;
import org.opendaylight.yang.gen.v1.gnpy.path.rev201022.path.route.objects.explicit.route.objects.RouteObjectIncludeExcludeKey;

@SuppressWarnings("serial")
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "SE_NO_SERIALVERSIONID",
    justification = "https://github.com/rzwitserloot/lombok/wiki/WHY-NOT:-serialVersionUID")
public class RouteObjectIncludeExcludeKeyComparator implements Comparator<RouteObjectIncludeExcludeKey>, Serializable {

    @Override
    public int compare(RouteObjectIncludeExcludeKey value1, RouteObjectIncludeExcludeKey value2) {
        if (value1 == null && value2 == null) {
            return 0;
        } else if (value1 == null) {
            return 1;
        } else if (value2 == null) {
            return -1;
        } else {
            return Integer.compare(value1.getIndex().intValue(), value2.getIndex().intValue());
        }
    }

}
