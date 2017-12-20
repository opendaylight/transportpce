/*
 * Copyright © 2016 AT&T and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.networkmodel.util;

import java.text.MessageFormat;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev150608.LinkId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.networkutils.rev170818.InitRoadmNodesInput;

public final class LinkIdUtil {

    private static final String NETWORK = "-NETWORK";
    private static final String TRANSMIT = "-TX";
    private static final String RECEIVE = "-RX";
    private static final String BIDIRECTIONAL = "-TXRX";
    private static final MessageFormat LINK_ID_FORMAT = new MessageFormat("{0}-{1}to{2}-{3}");

    private LinkIdUtil() {
        // utility class
    }

    /**
     * Builds the Link id in format {@link LinkIdUtil#LINK_ID_FORMAT}.
     *
     * @param srcNode source node id string
     * @param srcTp source termination point
     * @param destNode destination node id
     * @param destTp destination termination point
     * @return {@link LinkId}
     */
    public static LinkId buildLinkId(String srcNode, String srcTp, String destNode, String destTp) {
        return new LinkId(LINK_ID_FORMAT
                .format(new Object[] {srcNode, srcTp, destNode, destTp}, new StringBuffer(), null).toString());
    }

    /**
     * Builds the opposite {@link LinkId} from the {@link InitRoadmNodesInput}.
     *
     * @param input an init link for ROADM nodes
     * @return opposite {@link LinkId}
     */
    public static LinkId getRdm2RdmOppositeLinkId(InitRoadmNodesInput input) {
        String srcNode = new StringBuilder(input.getRdmANode()).append("-DEG").append(input.getDegANum()).toString();
        String srcTp = input.getTerminationPointA();
        String destNode = new StringBuilder(input.getRdmZNode()).append("-DEG").append(input.getDegZNum()).toString();
        String destTp = input.getTerminationPointZ();

        Object[] params = buildParams(srcNode, srcTp, destNode, destTp, false);

        return new LinkId(LINK_ID_FORMAT.format(params, new StringBuffer(), null).toString());
    }

    /**
     * Builds the opposite {@link LinkId} from string descriptors.
     *
     * @param srcNode a source node
     * @param srcTp a source termination point
     * @param destNode a destination node
     * @param destTp a destination termination point
     * @return LinkId a link identifier
     */
    public static LinkId getOppositeLinkId(String srcNode, String srcTp, String destNode, String destTp) {
        return getOppositeLinkId(srcNode, srcTp, destNode, destTp, true);
    }

    /**
     * Builds the opposite {@link LinkId} from string descriptors.
     *
     * @param srcNode a source node
     * @param srcTp a source termination point
     * @param destNode a destination node
     * @param destTp a destination termination point
     * @return LinkId a link identifier
     */
    public static LinkId getOppositeLinkId(String srcNode, String srcTp, String destNode, String destTp,
        boolean checkNode) {
        Object[] params = buildParams(srcNode, srcTp, destNode, destTp, checkNode);
        return new LinkId(LINK_ID_FORMAT.format(params, new StringBuffer(), null).toString());
    }

    private static Object[] buildParams(String srcNode, String srcTp, String destNode, String destTp,
            boolean checkForNetwork) {
        Object[] params = null;
        if (checkBidirectional(checkForNetwork, srcTp)) {
            if (checkBidirectional(checkForNetwork, destTp)) {
                params = new Object[] {destNode, destTp, srcNode, srcTp};
            } else if (destTp.contains(RECEIVE)) {
                params = new Object[] {destNode, destTp.replace("RX", "TX"), srcNode, srcTp};
            } else {
                throw new IllegalArgumentException("Dest Termination Point is either RX/TX_RX ! Check TP");
            }
        } else if (srcTp.contains(TRANSMIT)) {
            String replacedSrcTp = srcTp.replace("TX", "RX");
            if (checkBidirectional(checkForNetwork, destTp)) {
                params = new Object[] {destNode, destTp, srcNode, replacedSrcTp};
            } else if (destTp.contains(RECEIVE)) {
                params = new Object[] {destNode, destTp.replace("RX", "TX"), srcNode, replacedSrcTp};
            } else {
                throw new IllegalArgumentException("Dest Termination Point is either RX/TX_RX ! Check TP");
            }
        } else {
            throw new IllegalArgumentException("SRC Termination Point is either TX/TXRX ! Check TP");
        }
        return params;
    }

    private static boolean checkBidirectional(boolean check, String tp) {
        return tp.contains(BIDIRECTIONAL) || (check && tp.contains(NETWORK));
    }
}
