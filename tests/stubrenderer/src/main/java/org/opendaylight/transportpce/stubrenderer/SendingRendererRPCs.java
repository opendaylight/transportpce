/*
 * Copyright © 2017 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.stubrenderer;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.opendaylight.yang.gen.v1.http.org.openroadm.common.service.types.rev161014.service.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.AToZ;
import org.opendaylight.yang.gen.v1.http.org.openroadm.topology.rev161014.topology.ZToA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *Class for Sending
 * Renderer requests :
 * - Service-implementation-request
 * - Service-delete-request.
 * @author Martial Coulibaly ( martial.coulibaly@gfi.com ) on behalf of Orange
 *
 */
public class SendingRendererRPCs {
    /** Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(SendingRendererRPCs.class);
    /** define procedure success (or not ). */
    private Boolean success;
    /** define type of request<br>
     * <code>true</code> pathcomputation <br>
     * <code>false</code> cancelresourcereserve. */
    private TopologyBuilder topology;
    private List<AToZ> atoz;
    private List<ZToA> ztoa;
    private String error;
    private final ListeningExecutorService executor;

    public SendingRendererRPCs(ListeningExecutorService executor) {
        success = true;
        setTopology(null);
        this.executor = executor;
        setError("");
    }

    private void buildAtoZ() {
        atoz = new ArrayList<AToZ>();
    }

    private void buildZtoA() {
        ztoa = new ArrayList<ZToA>();
    }

    public ListenableFuture<Boolean> serviceDelete() {
        LOG.info("ServiceDelete request ...");
        success = false;
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = true;
                LOG.info("Wait for 10s til beginning the Renderer serviceDelete request");
                try {
                    Thread.sleep(10000); //sleep for 10s
                } catch (InterruptedException e) {
                    output = false;
                    LOG.error("Thread.sleep failed : {}", e.toString());
                }
                buildAtoZ();
                buildZtoA();
                success = true;
                return output;
            }
        });
    }

    public ListenableFuture<Boolean> serviceImplementation() {
        LOG.info("serviceImplementation request ...");
        success = false;
        return executor.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                Boolean output = true;
                LOG.info("Wait for 10s til beginning the Renderer serviceDelete request");
                try {
                    Thread.sleep(10000); //sleep for 10s
                } catch (InterruptedException e) {
                    output = false;
                    LOG.error("Thread.sleep failed : {}", e.toString());
                }
                buildAtoZ();
                buildZtoA();
                setTopology(new TopologyBuilder()
                        .setAToZ(atoz)
                        .setZToA(ztoa));
                output = true;
                success = true;
                return output;
            }
        });
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

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
