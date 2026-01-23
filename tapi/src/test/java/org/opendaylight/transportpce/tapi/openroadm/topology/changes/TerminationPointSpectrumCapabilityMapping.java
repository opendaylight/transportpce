/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.changes;

import org.opendaylight.transportpce.tapi.openroadm.topology.terminationpoint.mapping.TerminationPointMapping;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.photonic.media.node.edge.point.spec.SpectrumCapabilityPac;

public record TerminationPointSpectrumCapabilityMapping(
        TerminationPointMapping terminationPointMapping,
        SpectrumCapabilityPac spectrumCapabilityPac) {
}
