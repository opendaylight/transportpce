/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubrenderer;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *Class for Sending
 * Renderer requests :
 * - Service-implementation-request
 * - Service-delete-request.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class SendingRendererRPCs {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(SendingRendererRPCs.class);
    /* define procedure success (or not ). */
    private Boolean success;
    /* define type of request<br>
     * <code>true</code> pathcomputation <br>
     * <code>false</code> cancelresourcereserve. */
    private TopologyBuilder topology;
    private List<AToZ> atoz;
    private List<ZToA> ztoa;

    public SendingRendererRPCs() {
        success = true;
        setTopology(null);
    }

    private void buildAtoZ() {
        atoz = new ArrayList<AToZ>();
    }

    private void buildZtoA() {
        ztoa = new ArrayList<ZToA>();
    }

    public void serviceDelete() {
        LOG.info("Wait for 10s til beginning the Renderer ServiceDelete request");
        try {
            Thread.sleep(10000); //sleep for 10s
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        LOG.info("ServiceDelete ...");
    }

    public void serviceImplementation() {
        LOG.info("Wait for 10s til beginning the Renderer serviceImplementation request");
        try {
            Thread.sleep(10000); //sleep for 10s
        } catch (InterruptedException e) {
            LOG.error(e.toString());
        }
        LOG.info("serviceImplementation ...");
        buildAtoZ();
        buildZtoA();

        setTopology(new TopologyBuilder()
            .setAToZ(atoz)
            .setZToA(ztoa));
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public TopologyBuilder getTopology() {
        return topology;
    }

    public void setTopology(TopologyBuilder topo) {
        this.topology = topo;
    }
}
