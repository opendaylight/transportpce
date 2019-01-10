/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.olm.util;

import org.junit.Assert;
import org.junit.Test;

public class RoadmLinksTest {

    /*
     * test RoadmLinks initialization
     */
    @Test
    public void test() {
        RoadmLinks roadmlinks0 = new RoadmLinks();
        roadmlinks0.setDestNodeId("dest node");
        roadmlinks0.setDestTpid("tp");
        roadmlinks0.setSrcNodeId("src node");
        roadmlinks0.setSrcTpId("tp");

        Assert.assertEquals("dest node", roadmlinks0.getDestNodeId());
        Assert.assertEquals("tp", roadmlinks0.getDestTpid());
        Assert.assertEquals("src node", roadmlinks0.getSrcNodeId());
        Assert.assertEquals("tp", roadmlinks0.getSrcTpId());
    }
}
