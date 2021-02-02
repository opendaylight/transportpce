/*
 * Copyright © 2021 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.dmaap.client.resource.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CreatedEvent {

    @JsonProperty("serverTimeMs")
    private Integer serverTimeMs;

    @JsonProperty("count")
    private Integer count;

    public Integer getServerTimeMs() {
        return serverTimeMs;
    }

    public void setServerTimeMs(Integer serverTimeMs) {
        this.serverTimeMs = serverTimeMs;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("CreatedEvent [serverTimeMs=")
                .append(serverTimeMs).append(", count=")
                .append(count).append("]")
                .toString();
    }
}
