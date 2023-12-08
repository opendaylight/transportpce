/*
 * Copyright © 2026 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.links;

import java.util.Objects;

/**
 * A {@link Link} identified solely by its source and destination port names.
 */
public class InterfaceLink implements Link {

    private final String source;

    private final String destination;

    public InterfaceLink(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public String destination() {
        return destination;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof InterfaceLink)) {
            return false;
        }
        InterfaceLink that = (InterfaceLink) obj;
        return Objects.equals(source, that.source) && Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }

    @Override
    public String toString() {
        return "InterfaceLink{"
                + "source='" + source + '\''
                + ", destination='" + destination + '\''
                + '}';
    }
}
