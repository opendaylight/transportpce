/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class NodeIdPairTest {

    private NodeIdPair firstPair;
    private Object secondPair;
    private boolean equality;

    public NodeIdPairTest(NodeIdPair firstPair, Object secondPair, boolean equality) {
        this.firstPair = firstPair;
        this.secondPair = secondPair;
        this.equality = equality;
    }

    @Parameterized.Parameters
    public static Collection<?> nodes() {
        NodeIdPair same = new NodeIdPair("nodeS", "CLIENT");
        return Arrays.asList(new Object[][] {
                { new NodeIdPair("",""), null, false },
                { new NodeIdPair("",""), "", false },
                { new NodeIdPair("node1","PP"), new NodeIdPair("node2","PP"), false },
                { new NodeIdPair("node1","PP"), new NodeIdPair("node1","TTP"), false },
                { new NodeIdPair(null,"PP"), new NodeIdPair(null,"TTP"), false },
                { new NodeIdPair(null,"PP"), new NodeIdPair("node2","TTP"), false },
                { new NodeIdPair("node1",null), new NodeIdPair("node1","NETWORK"), false },
                { new NodeIdPair("node1",null), new NodeIdPair("node1",null), true },
                { new NodeIdPair("node1","TTP"), new NodeIdPair("node1","TTP"), true },
                { new NodeIdPair(null,null), new NodeIdPair(null,null), true },
                {same, same, true}
        });
    }

    @Test
    public void equalityTest() {
        Assert.assertEquals(this.equality, firstPair.equals(this.secondPair));
        if ((this.secondPair != null) && this.firstPair.getClass().equals(this.secondPair.getClass())) {
            Assert.assertEquals(this.equality, this.firstPair.hashCode() == this.secondPair.hashCode());
        }
    }

}
