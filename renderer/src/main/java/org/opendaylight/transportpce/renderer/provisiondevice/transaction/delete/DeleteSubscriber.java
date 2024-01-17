/*
 * Copyright © 2024 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.renderer.provisiondevice.transaction.delete;

public class DeleteSubscriber implements Subscriber {

    private final Result result;

    public DeleteSubscriber(Result result) {
        this.result = result;
    }

    @Override
    public void result(Boolean success, String nodeId, String interfaceId) {

        result.add(success, nodeId, interfaceId);

    }

}