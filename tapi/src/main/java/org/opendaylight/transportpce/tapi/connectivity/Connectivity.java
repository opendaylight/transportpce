/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import java.util.List;
import org.jspecify.annotations.NonNull;

public interface Connectivity {

    List<@NonNull String> xpdrClientTplist();

    List<@NonNull String> xpdrNetworkTplist();

    List<@NonNull String> rdmAddDropTplist();

    List<@NonNull String> rdmDegTplist();

    List<@NonNull String> rdmNodelist();

    List<@NonNull String> xpdrNodelist();

}
