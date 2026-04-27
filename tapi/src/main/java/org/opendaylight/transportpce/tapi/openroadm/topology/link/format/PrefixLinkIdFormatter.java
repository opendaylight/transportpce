/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.openroadm.topology.link.format;

import java.util.Objects;

/**
 * {@link LinkIdFormatter} decorator that prepends a fixed prefix to the identifier
 * produced by a delegate {@link LinkIdFormatter}.
 */
public final class PrefixLinkIdFormatter implements LinkIdFormatter {

    private final String prefix;

    private final LinkIdFormatter linkIdFormatter;

    /**
     * Creates a formatter that prepends {@code prefix} to the identifier produced by
     * {@code linkIdFormatter}.
     *
     * @param prefix the prefix to prepend to the delegate-formatted link identifier
     * @param linkIdFormatter the delegate formatter used to build the unprefixed link identifier
     */
    public PrefixLinkIdFormatter(String prefix, LinkIdFormatter linkIdFormatter) {
        this.prefix = Objects.requireNonNull(prefix);
        this.linkIdFormatter = Objects.requireNonNull(linkIdFormatter);
    }

    /**
     * Creates a formatter that prepends {@code prefix} to the identifier produced by a
     * {@link DefaultLinkIdFormatter}.
     *
     * @param prefix the prefix to prepend to the delegate-formatted link identifier
     */
    public PrefixLinkIdFormatter(String prefix) {
        this(prefix, new DefaultLinkIdFormatter());
    }

    @Override
    public String linkId(
            String srcOpenRoadmTopologyNodeId,
            String srcOpenRoadmTopologyTerminationPointId,
            String destOpenRoadmTopologyNodeId,
            String destOpenRoadmTopologyTerminationPointId) {

        return prefix + linkIdFormatter.linkId(
                srcOpenRoadmTopologyNodeId,
                srcOpenRoadmTopologyTerminationPointId,
                destOpenRoadmTopologyNodeId,
                destOpenRoadmTopologyTerminationPointId
        );
    }
}
