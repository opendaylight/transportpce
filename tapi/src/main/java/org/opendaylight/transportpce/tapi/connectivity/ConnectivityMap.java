/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConnectivityMap keeps track of the various TPs and nodes involved in a connectivity.
 * This implementation uses Sets to avoid duplicates and preserve insertion order.
 * Note: This class is not thread-safe and is mutable.
 */
public class ConnectivityMap implements Connectivity {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectivityMap.class);

    private final Set<@NonNull String> xpdrClientTplist;

    private final Set<@NonNull String> xpdrNetworkTplist;

    private final Set<@NonNull String> rdmAddDropTplist;

    private final Set<@NonNull String> rdmDegTplist;

    private final Set<@NonNull String> rdmNodelist;

    private final Set<@NonNull String> xpdrNodelist;

    /**
     * Default constructor for an empty and mutable ConnectivityMap.
     */
    public ConnectivityMap() {
        this.xpdrClientTplist = new LinkedHashSet<>();
        this.xpdrNetworkTplist = new LinkedHashSet<>();
        this.rdmAddDropTplist = new LinkedHashSet<>();
        this.rdmDegTplist = new LinkedHashSet<>();
        this.rdmNodelist = new LinkedHashSet<>();
        this.xpdrNodelist = new LinkedHashSet<>();
    }

    /**
     * Create an immutable ConnectivityMap from an existing Connectivity instance.
     */
    public ConnectivityMap(Connectivity connectivity) {
        this.xpdrClientTplist = Collections.unmodifiableSet(new LinkedHashSet<>(connectivity.xpdrClientTplist()));
        this.xpdrNetworkTplist = Collections.unmodifiableSet(new LinkedHashSet<>(connectivity.xpdrNetworkTplist()));
        this.rdmAddDropTplist = Collections.unmodifiableSet(new LinkedHashSet<>(connectivity.rdmAddDropTplist()));
        this.rdmDegTplist = Collections.unmodifiableSet(new LinkedHashSet<>(connectivity.rdmDegTplist()));
        this.rdmNodelist = Collections.unmodifiableSet(new LinkedHashSet<>(connectivity.rdmNodelist()));
        this.xpdrNodelist = Collections.unmodifiableSet(new LinkedHashSet<>(connectivity.xpdrNodelist()));
    }

    @Override
    public boolean addXpdrClientTp(@NonNull String tp) {
        if (xpdrClientTplist.add(tp)) {
            LOG.debug("Added XPDR client TP: {}", tp);
            return true;
        }

        return false;
    }

    @Override
    public List<@NonNull String> xpdrClientTplist() {
        return new ArrayList<>(xpdrClientTplist);
    }

    @Override
    public boolean addXpdrNetworkTp(@NonNull String tp) {
        if (xpdrNetworkTplist.add(tp)) {
            LOG.debug("Added XPDR network TP: {}", tp);
            return true;
        }

        return false;
    }

    @Override
    public List<@NonNull String> xpdrNetworkTplist() {
        return new ArrayList<>(xpdrNetworkTplist);
    }

    @Override
    public boolean addRdmAddDropTp(@NonNull String tp) {
        if (rdmAddDropTplist.add(tp)) {
            LOG.debug("Added ROADM add/drop TP: {}", tp);
            return true;
        }

        return false;
    }

    @Override
    public List<@NonNull String> rdmAddDropTplist() {
        return new ArrayList<>(rdmAddDropTplist);
    }

    @Override
    public boolean addRdmDegTp(@NonNull String tp) {
        if (rdmDegTplist.add(tp)) {
            LOG.debug("Added ROADM degree TP: {}", tp);
            return true;
        }

        return false;
    }

    @Override
    public List<@NonNull String> rdmDegTplist() {
        return new ArrayList<>(rdmDegTplist);
    }

    @Override
    public boolean addRdmNode(@NonNull String tp) {
        if (rdmNodelist.add(tp)) {
            LOG.debug("Added ROADM node: {}", tp);
            return true;
        }

        return false;
    }

    @Override
    public List<@NonNull String> rdmNodelist() {
        return new ArrayList<>(rdmNodelist);
    }

    @Override
    public boolean addXpdrNode(@NonNull String tp) {
        if (xpdrNodelist.add(tp)) {
            LOG.debug("Added XPDR node: {}", tp);
            return true;
        }

        return false;
    }

    @Override
    public List<@NonNull String> xpdrNodelist() {
        return new ArrayList<>(xpdrNodelist);
    }

    @Override
    public void log() {
        LOG.info("ROADM node list = {}", rdmNodelist);
        LOG.info("ROADM degree list = {}", rdmDegTplist);
        LOG.info("ROADM addrop list = {}", rdmAddDropTplist);
        LOG.info("XPDR node list = {}", xpdrNodelist);
        LOG.info("XPDR network list = {}", xpdrNetworkTplist);
        LOG.info("XPDR client list = {}", xpdrClientTplist);
    }
}
