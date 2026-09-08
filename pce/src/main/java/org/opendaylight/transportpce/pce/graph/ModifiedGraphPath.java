/*
 * Copyright © 2026 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce.graph;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;

@SuppressWarnings("unchecked")
public class ModifiedGraphPath implements GraphPath<String, PceGraphEdge> {


    private Object gpath;

    public ModifiedGraphPath(Map<String, PceGraphEdge> pceGraphEdgeMap) {
        this.gpath = pceGraphEdgeMap;
    }

    public ModifiedGraphPath(GraphPath<String, PceGraphEdge> graphPath) {
        this.gpath = graphPath;
    }

    public boolean isGraphPath() {
        return gpath instanceof GraphPath;
    }

    public boolean isPceGraphEdgeMap() {
        return gpath instanceof Map;
    }

    public GraphPath<String, PceGraphEdge> getGraphPath() {
        if (isGraphPath()) {
            return ((GraphPath<String, PceGraphEdge>)gpath);
        }
        throw new IllegalStateException("ModifiedGraphPath is not a GraphPath");
    }

    public Map<String, PceGraphEdge> getPceGraphEdgeMap() {
        if (isPceGraphEdgeMap()) {
            return (Map<String, PceGraphEdge>) gpath;
        }
        throw new IllegalStateException("ModifiedGraphPath is not PceGraphEdgeMap");
    }

    public static ModifiedGraphPath fromGraphPath(GraphPath<String, PceGraphEdge> path) {
        return new ModifiedGraphPath(path);
    }

    public static ModifiedGraphPath fromPceGraphEdgeMap(Map<String, PceGraphEdge> path) {
        return new ModifiedGraphPath(path);
    }

    @Override
    public Graph<String, PceGraphEdge> getGraph() {
        if (isGraphPath()) {
            return ((GraphPath<String, PceGraphEdge>) gpath).getGraph();
        }
        return null;
    }

    @Override
    public String getStartVertex() {
        if (isGraphPath()) {
            return ((GraphPath<String, PceGraphEdge>) gpath).getStartVertex();
        }
        return null;
    }

    @Override
    public String getEndVertex() {
        if (isGraphPath()) {
            return ((GraphPath<String, PceGraphEdge>) gpath).getEndVertex();
        }
        return null;
    }

    @Override
    public List<PceGraphEdge> getEdgeList() {
        if (isPceGraphEdgeMap()) {
            return (List<PceGraphEdge>) ((Map<String, PceGraphEdge>) gpath)
                .values().stream().collect(Collectors.toList());
        }

        return getGraphPath().getEdgeList();

    }

    @Override
    public List<String> getVertexList() {
        if (isPceGraphEdgeMap()) {
            return (List<String>) ((Map<String, PceGraphEdge>) gpath).keySet().stream().collect(Collectors.toList());
        }
        return getGraphPath().getVertexList();

    }

    @Override
    public double getWeight() {
        if (isPceGraphEdgeMap()) {
            return ((Map<String, PceGraphEdge>) gpath).values().stream().map(PceGraphEdge::link)
                .mapToDouble(PceLink::getWeight).sum();
        }
        return ((GraphPath<String, PceGraphEdge>) gpath).getWeight();
    }

    @Override
    public int getLength() {
        return getEdgeList().size();
    }

}
