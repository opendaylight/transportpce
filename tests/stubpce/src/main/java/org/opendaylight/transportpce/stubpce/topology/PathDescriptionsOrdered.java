/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubpce.topology;

import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.servicepath.rev170426.path.description.list.PathDescriptions;

/**
 * class to create structure
 * of <code>PathDescriptions</code>
 * ordered.
 *
 * @author <a href="mailto:martial.coulibaly@gfi.com">Martial Coulibaly</a> on
 *         behalf of Orange
 */
public class PathDescriptionsOrdered implements Comparable<PathDescriptionsOrdered> {
    private PathDescriptions pathDescriptions;
    private int ord;

    /**
     * PathDescriptionsOrdered constructor.
     *
     * @param paths PathDescriptions
     * @param order Integer
     */
    public PathDescriptionsOrdered(PathDescriptions paths, int order) {
        setPathDescriptions(paths);
        setOrd(order);
    }

    @Override
    public int compareTo(PathDescriptionsOrdered paths) {
        return this.ord - paths.getOrd();
    }

    public PathDescriptions getPathDescriptions() {
        return pathDescriptions;
    }

    public void setPathDescriptions(PathDescriptions pathDescriptions) {
        this.pathDescriptions = pathDescriptions;
    }

    public int getOrd() {
        return ord;
    }

    public void setOrd(int ord) {
        this.ord = ord;
    }
}
