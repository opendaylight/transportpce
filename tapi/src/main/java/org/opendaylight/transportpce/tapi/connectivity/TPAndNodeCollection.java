/*
 * Copyright © 2025 Smartoptics and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.tapi.connectivity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TPAndNodeCollection keeps track of the various TP and Node IDs.
 * This implementation uses Sets to avoid duplicate values
 * and preserve insertion order.
 *
 * <p>Note:
 * This class is not thread-safe and is normally mutable.
 * However, one may create an immutable copy of this object by passing an instance
 * of IDCollection into one of the constructors.
 * Also, this class will not except any values being null or empty string.
 * @see TPAndNodeCollection#TPAndNodeCollection(IDCollection)
 */
public class TPAndNodeCollection implements IDCollection {

    private static final Logger LOG = LoggerFactory.getLogger(TPAndNodeCollection.class);

    private final Set<@NonNull String> xpdrClientTplist;

    private final Set<@NonNull String> xpdrNetworkTplist;

    private final Set<@NonNull String> rdmAddDropTplist;

    private final Set<@NonNull String> rdmDegTplist;

    private final Set<@NonNull String> rdmNodelist;

    private final Set<@NonNull String> xpdrNodelist;

    /**
     * Default constructor for an empty and mutable instance of TPAndNodeCollection.
     */
    public TPAndNodeCollection() {
        this.xpdrClientTplist = new LinkedHashSet<>();
        this.xpdrNetworkTplist = new LinkedHashSet<>();
        this.rdmAddDropTplist = new LinkedHashSet<>();
        this.rdmDegTplist = new LinkedHashSet<>();
        this.rdmNodelist = new LinkedHashSet<>();
        this.xpdrNodelist = new LinkedHashSet<>();
    }

    /**
     * Create an immutable instance of TPAndNodeCollection from an existing IDCollection instance.
     */
    public TPAndNodeCollection(IDCollection idCollection) {
        this.xpdrClientTplist   = immutableNonNullNonEmptySet(idCollection.xpdrClientTplist());
        this.xpdrNetworkTplist  = immutableNonNullNonEmptySet(idCollection.xpdrNetworkTplist());
        this.rdmAddDropTplist   = immutableNonNullNonEmptySet(idCollection.rdmAddDropTplist());
        this.rdmDegTplist       = immutableNonNullNonEmptySet(idCollection.rdmDegTplist());
        this.rdmNodelist        = immutableNonNullNonEmptySet(idCollection.rdmNodelist());
        this.xpdrNodelist       = immutableNonNullNonEmptySet(idCollection.xpdrNodelist());
    }

    /**
     * Create an immutable TPAndNodeCollection.
     */
    public TPAndNodeCollection(
            List<@NonNull String> xpdrClientTplist,
            List<@NonNull String> xpdrNetworkTplist,
            List<@NonNull String> rdmAddDropTplist,
            List<@NonNull String> rdmDegTplist,
            List<@NonNull String> rdmNodelist,
            List<@NonNull String> xpdrNodelist) {

        this.xpdrClientTplist  = immutableNonNullNonEmptySet(xpdrClientTplist);
        this.xpdrNetworkTplist = immutableNonNullNonEmptySet(xpdrNetworkTplist);
        this.rdmAddDropTplist  = immutableNonNullNonEmptySet(rdmAddDropTplist);
        this.rdmDegTplist      = immutableNonNullNonEmptySet(rdmDegTplist);
        this.rdmNodelist       = immutableNonNullNonEmptySet(rdmNodelist);
        this.xpdrNodelist      = immutableNonNullNonEmptySet(xpdrNodelist);
    }

    private static Set<@NonNull String> immutableNonNullNonEmptySet(Collection<String> source) {
        Objects.requireNonNull(source, "source collection must not be null");
        return Collections.unmodifiableSet(
                (Set<? extends String>) source.stream()
                        .filter(Objects::nonNull)                           // remove nulls
                        .map(String::trim)                                  // normalize whitespace
                        .filter(s -> !s.isEmpty())                    // remove empty strings
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    @Override
    public boolean addXpdrClientTp(@NonNull String tp) {
        if (tp == null || tp.isEmpty()) {
            return false;
        }

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
        if (tp == null || tp.isEmpty()) {
            return false;
        }

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
        if (tp == null || tp.isEmpty()) {
            return false;
        }

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
        if (tp == null || tp.isEmpty()) {
            return false;
        }

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
        if (tp == null || tp.isEmpty()) {
            return false;
        }

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
        if (tp == null || tp.isEmpty()) {
            return false;
        }

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
