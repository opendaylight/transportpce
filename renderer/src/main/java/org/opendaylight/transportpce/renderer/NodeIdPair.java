/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

public class NodeIdPair {

    private String nodeID;
    private String tpID;

    public NodeIdPair(String nodeID, String tpID) {
        this.nodeID = nodeID;
        this.tpID = tpID;
    }

    public String getNodeID() {
        return this.nodeID;
    }

    public String getTpID() {
        return this.tpID;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((object == null) || (getClass() != object.getClass())) {
            return false;
        }

        NodeIdPair that = (NodeIdPair) object;

        if (this.nodeID != null ? !this.nodeID.equals(that.nodeID) : that.nodeID != null) {
            return false;
        }
        return this.tpID != null ? this.tpID.equals(that.tpID) : that.tpID == null;
    }

    @Override
    public int hashCode() {
        int result = this.nodeID != null ? this.nodeID.hashCode() : 0;
        result = (31 * result) + (this.tpID != null ? this.tpID.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "NodeIdPair{" + "nodeID='" + this.nodeID + '\'' + ", tpID='" + this.tpID + '\'' + '}';
    }
}
