/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.util;
/**
 * The Class RoadmLinks.
 */
public class RoadmLinks {

    /** The src node id. */
    private String srcNodeId;

    /** The src tp id. */
    private String srcTpId;

    /** The dest node id. */
    private String destNodeId;

    /** The dest tpid. */
    private String destTpid;

    /**
     * Gets the src node id.
     *
     * @return the src node id
     */
    public String getSrcNodeId() {
        return srcNodeId;
    }

    /**
     * Sets the src node id.
     *
     * @param srcNodeId the new src node id
     */
    public void setSrcNodeId(String srcNodeId) {
        this.srcNodeId = srcNodeId;
    }

    /**
     * Gets the src tp id.
     *
     * @return the src tp id
     */
    public String getSrcTpId() {
        return srcTpId;
    }

    /**
     * Sets the src tp id.
     *
     * @param srcTpId the new src tp id
     */
    public void setSrcTpId(String srcTpId) {
        this.srcTpId = srcTpId;
    }

    /**
     * Gets the dest node id.
     *
     * @return the dest node id
     */
    public String getDestNodeId() {
        return destNodeId;
    }

    /**
     * Sets the dest node id.
     *
     * @param destNodeId the new dest node id
     */
    public void setDestNodeId(String destNodeId) {
        this.destNodeId = destNodeId;
    }

    /**
     * Gets the dest tpid.
     *
     * @return the dest tpid
     */
    public String getDestTpid() {
        return destTpid;
    }

    /**
     * Sets the dest tpid.
     *
     * @param destTpid the new dest tpid
     */
    public void setDestTpid(String destTpid) {
        this.destTpid = destTpid;
    }

}
