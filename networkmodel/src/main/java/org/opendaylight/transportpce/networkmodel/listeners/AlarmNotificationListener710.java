/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.networkmodel.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.ServiceNodelist;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.Nodelist;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.nodelist.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102.service.nodelist.nodelist.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev200529.AlarmNotification;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev200529.OrgOpenroadmAlarmListener;
import org.opendaylight.yang.gen.v1.http.org.openroadm.alarm.rev200529.alarm.ProbableCause;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.ResourceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.CircuitPack;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Connection;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Degree;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.InternalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.PhysicalLink;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Port;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Service;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Shelf;
import org.opendaylight.yang.gen.v1.http.org.openroadm.resource.rev200529.resource.resource.resource.Srg;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmNotificationListener710 implements OrgOpenroadmAlarmListener {

    private static final Logger LOG = LoggerFactory.getLogger(AlarmNotificationListener710.class);
    private static final String PIPE = "|";
    private final DataBroker dataBroker;

    public AlarmNotificationListener710(DataBroker dataBroker) {
        this.dataBroker = dataBroker;
    }


    /**
     * Callback for alarm-notification.
     *
     * @param notification AlarmNotification object
     */
    @Override
    public void onAlarmNotification(AlarmNotification notification) {
        List<Nodes> allNodeList = new ArrayList<>();
        InstanceIdentifier<ServiceNodelist> serviceNodeListIID = InstanceIdentifier.create(ServiceNodelist.class);
        try {
            ReadTransaction rtx = dataBroker.newReadOnlyTransaction();
            Optional<ServiceNodelist> serviceListObject =
                    rtx.read(LogicalDatastoreType.OPERATIONAL, serviceNodeListIID).get();
            if (serviceListObject.isPresent()) {
                for (Nodelist nodelist : serviceListObject.get().nonnullNodelist().values()) {
                    allNodeList.addAll(nodelist.nonnullNodes().values());
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.warn("Exception thrown while reading Logical Connection Point value", ex);
        }
        StringBuilder sb = new StringBuilder(notification.getResource().getDevice().getNodeId().getValue())
            .append(PIPE);
        sb.append(buildCause(notification.getProbableCause()));
        sb.append(notification.getId() != null ? notification.getId() : "").append(PIPE)
                .append(notification.getRaiseTime() != null ? notification.getRaiseTime().toString() : "").append(PIPE)
                .append(notification.getSeverity() != null ? notification.getSeverity().getName() : "").append(PIPE)
                .append(notification.getCircuitId() != null ? notification.getCircuitId() : "").append(PIPE);

        sb.append(buildType(notification));

        String message = sb.toString();
        Nodes build = new NodesBuilder().setNodeId(notification.getResource().getDevice().getNodeId().getValue())
            .build();
        if (allNodeList.contains(build)) {
            LOG.info("onAlarmNotification: {}", message);
        } else {
            LOG.warn("onAlarmNotification: {}", message);
        }
    }

    private String buildCause(ProbableCause probableCause) {
        StringBuilder sb = new StringBuilder();
        if (probableCause == null) {
            return "||||";
        }
        sb.append((probableCause.getCause() != null) ? probableCause.getCause().getName() : "").append(PIPE)
                .append((probableCause.getDirection() != null) ? probableCause.getDirection().getName() : "")
                .append(PIPE).append((probableCause.getExtension() != null) ? probableCause.getExtension() : "")
                .append(PIPE).append((probableCause.getLocation() != null) ? probableCause.getLocation().getName() : "")
                .append(PIPE);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Resource> Optional<T> tryCastToParticularResource(Class<T> resourceClass,
            Resource resource) {
        if (resource == null) {
            LOG.error("Resource is null.");
        } else if (!resourceClass.isInstance(resource)) {
            LOG.error("Resource implement different type than expected. Expected {}, actual {}.",
                    resourceClass.getSimpleName(), resource.getClass().getSimpleName());
        } else {
            return Optional.of((T) resource);
        }
        return Optional.empty();
    }

    private static String buildType(AlarmNotification notification) {
        String circuitPack = "";
        String connection = "";
        String degree = "";
        String iface = "";
        String internalLink = "";
        String physicalLink = "";
        String service = "";
        String shelf = "";
        String sharedRiskGroup = "";
        String port = "";
        String portCircuitPack = "";

        Resource resource = notification.getResource().getResource().getResource();
        ResourceType wantedResourceType = notification.getResource().getResourceType();

        switch (wantedResourceType.getType()) {
            case CircuitPack:
                Optional<CircuitPack> circuitPackOptional = tryCastToParticularResource(CircuitPack.class, resource);
                if (circuitPackOptional.isPresent()) {
                    circuitPack = circuitPackOptional.get().getCircuitPackName();
                }
                break;

            case Connection:
                Optional<Connection> connectionOptional = tryCastToParticularResource(Connection.class, resource);
                if (connectionOptional.isPresent()) {
                    connection = connectionOptional.get().getConnectionName();
                }
                break;

            case Degree:
                Optional<Degree> degreeOptional = tryCastToParticularResource(Degree.class, resource);
                if (degreeOptional.isPresent()) {
                    degree = degreeOptional.get().getDegreeNumber().toString();
                }
                break;

            case Interface:
                Optional<Interface> interfaceOptional = tryCastToParticularResource(Interface.class, resource);
                if (interfaceOptional.isPresent()) {
                    iface = interfaceOptional.get().getInterfaceName();
                }
                break;

            case InternalLink:
                Optional<InternalLink> internalLinkOptional = tryCastToParticularResource(InternalLink.class, resource);
                if (internalLinkOptional.isPresent()) {
                    internalLink = internalLinkOptional.get().getInternalLinkName();
                }
                break;

            case PhysicalLink:
                Optional<PhysicalLink> physicalLinkOptional = tryCastToParticularResource(PhysicalLink.class, resource);
                if (physicalLinkOptional.isPresent()) {
                    physicalLink = physicalLinkOptional.get().getPhysicalLinkName();
                }
                break;

            case Service:
                Optional<Service> serviceOptional = tryCastToParticularResource(Service.class, resource);
                if (serviceOptional.isPresent()) {
                    service = serviceOptional.get().getServiceName();
                }
                break;

            case Shelf:
                Optional<Shelf> shelfOptional = tryCastToParticularResource(Shelf.class, resource);
                if (shelfOptional.isPresent()) {
                    shelf = shelfOptional.get().getShelfName();
                }
                break;

            case SharedRiskGroup:
                Optional<Srg> sharedRiskGroupOptional = tryCastToParticularResource(Srg.class, resource);
                if (sharedRiskGroupOptional.isPresent()) {
                    sharedRiskGroup = sharedRiskGroupOptional.get().getSrgNumber().toString();
                }
                break;

            case Port:
                Optional<Port> portContainerOptional = tryCastToParticularResource(Port.class, resource);
                if (portContainerOptional.isPresent()) {
                    port = portContainerOptional.get().getPort().getPortName();
                    portCircuitPack = portContainerOptional.get().getPort().getCircuitPackName();
                }
                break;

            default:
                LOG.warn("Unknown resource type {}", wantedResourceType);
        }
        StringBuilder sb = new StringBuilder(circuitPack);
        sb.append(PIPE).append(connection).append(PIPE).append(degree).append(PIPE).append(iface);
        sb.append(PIPE).append(internalLink).append(PIPE).append(physicalLink).append(PIPE).append(service);
        sb.append(PIPE).append(shelf).append(PIPE).append(sharedRiskGroup).append(PIPE).append(port);
        sb.append(PIPE).append(portCircuitPack);
        return sb.toString();
    }
}
