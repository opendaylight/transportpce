/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.renderer;

import org.opendaylight.transportpce.renderer.provisiondevice.tasks.RollbackTask;

public class TestRollbackTask extends RollbackTask {

    private boolean rollbackNecessary;

    public TestRollbackTask(String id, boolean rollbackNecessary) {
        super(id);
        this.rollbackNecessary = rollbackNecessary;
    }

    @Override
    public boolean isRollbackNecessary() {
        return rollbackNecessary;
    }

    @Override
    public Void call() throws Exception {
        return null;
    }
}
