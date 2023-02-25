/*
 * Copyright © 2018 Orange Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class NodeIdPairTest {

    private static Stream<Arguments> provideNodeSamples() {
        NodeIdPair same = new NodeIdPair("nodeS", "CLIENT");
        return Stream.of(
            Arguments.of(new NodeIdPair("",""), null, false),
            Arguments.of(new NodeIdPair("",""), "", false),
            Arguments.of(new NodeIdPair("node1","PP"), new NodeIdPair("node2","PP"), false),
            Arguments.of(new NodeIdPair("node1","PP"), new NodeIdPair("node1","TTP"), false),
            Arguments.of(new NodeIdPair(null,"PP"), new NodeIdPair(null,"TTP"), false),
            Arguments.of(new NodeIdPair(null,"PP"), new NodeIdPair("node2","TTP"), false),
            Arguments.of(new NodeIdPair("node1",null), new NodeIdPair("node1","NETWORK"), false),
            Arguments.of(new NodeIdPair("node1",null), new NodeIdPair("node1",null), true),
            Arguments.of(new NodeIdPair("node1","TTP"), new NodeIdPair("node1","TTP"), true),
            Arguments.of(new NodeIdPair(null,null), new NodeIdPair(null,null), true),
            Arguments.of(same, same, true));
    }

    @ParameterizedTest
    @MethodSource("provideNodeSamples")
    void equalityTest(NodeIdPair firstPair, Object secondPair, boolean equality) {
        assertEquals(equality, firstPair.equals(secondPair));
        if ((secondPair != null) && firstPair.getClass().equals(secondPair.getClass())) {
            assertEquals(equality, firstPair.hashCode() == secondPair.hashCode());
        }
    }
}
