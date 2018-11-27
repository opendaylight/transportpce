/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.transportpce.common.mapping;

import com.google.common.util.concurrent.CheckedFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.common.Timeouts;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaceException;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfaces;
import org.opendaylight.transportpce.common.openroadminterfaces.OpenRoadmInterfacesImpl;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.NetworkBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.NodesBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.CpToDegree;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.MappingBuilder;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev170228.network.nodes.MappingKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.NodeTypes;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.Ports;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.pack.PortsKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacks;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.circuit.packs.CircuitPacksKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.org.openroadm.device.Info;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.port.Interfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.InterfaceType;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpenROADMOpticalMultiplex;
import org.opendaylight.yang.gen.v1.http.org.openroadm.interfaces.rev161014.OpticalTransport;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortMappingImpl implements PortMapping {

    private static final Logger LOG = LoggerFactory.getLogger(PortMappingImpl.class);

    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;
    private final OpenRoadmInterfaces openRoadmInterfaces;

    public PortMappingImpl(DataBroker dataBroker, DeviceTransactionManager deviceTransactionManager,
        OpenRoadmInterfaces openRoadmInterfaces) {
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
        this.openRoadmInterfaces = openRoadmInterfaces;
    }





    /**
     * This private method builds the mapping object to be pushed in MD-SAL in
     * order to save port mapping. In case of TTP ports, it also saves the
     * OTS,OMS interfaces provisioned on the port.
     *
     * @param port
     *            Reference to device's port subtree object.
     * @param circuitPackName
     *            Name of cp where port exists.
     * @param logicalConnectionPoint
     *            logical name of the port.
     *
     * @return true/false based on status of operation
     */

    public Mapping createMappingObject(String nodeId, Ports port, String circuitPackName,
        String logicalConnectionPoint) {
        MappingBuilder mpBldr = new MappingBuilder();
        mpBldr.withKey(new MappingKey(logicalConnectionPoint)).setLogicalConnectionPoint(logicalConnectionPoint)
            .setSupportingCircuitPackName(circuitPackName).setSupportingPort(port.getPortName());

        // Get OMS and OTS interface provisioned on the TTP's
        if (logicalConnectionPoint.contains(OpenRoadmInterfacesImpl.TTP_TOKEN) && (port.getInterfaces() != null)) {
            for (Interfaces interfaces : port.getInterfaces()) {
                try {
                    Optional<Interface> openRoadmInterface = this.openRoadmInterfaces.getInterface(nodeId,
                        interfaces.getInterfaceName());
                    if (openRoadmInterface.isPresent()) {
                        Class<? extends InterfaceType> interfaceType = openRoadmInterface.get().getType();
                        // Check if interface type is OMS or OTS
                        if (interfaceType.equals(OpenROADMOpticalMultiplex.class)) {
                            mpBldr.setSupportingOms(interfaces.getInterfaceName());
                        }
                        if (interfaceType.equals(OpticalTransport.class)) {
                            mpBldr.setSupportingOts(interfaces.getInterfaceName());
                        }
                    } else {
                        LOG.warn("Interface {} from node {} was null!", interfaces.getInterfaceName(), nodeId);
                    }
                } catch (OpenRoadmInterfaceException ex) {
                    LOG.warn("Error while getting interface {} from node {}!", interfaces.getInterfaceName(), nodeId,
                        ex);
                }
            }
        }
        return mpBldr.build();
    }


    /**
     * This method for ports the portMapping corresponding to the
     * portmapping.yang file to the MD-SAL datastore.
     *
     * <p>
     * 1. Supporting circuit pack 2. Supporting port 3. Supporting OMS interface
     * (if port on ROADM)
     *
     * @param deviceInfo
     *            Info subtree from the device.
     * @param portMapList
     *            Reference to the list containing the mapping to be pushed to
     *            MD-SAL.
     *
     * @return Result true/false based on status of operation.
     */
    private boolean postPortMapping(Info deviceInfo, List<Mapping> portMapList, Integer nodeType,
        List<CpToDegree> cp2DegreeList) {
        NodesBuilder nodesBldr = new NodesBuilder();
        nodesBldr.withKey(new NodesKey(deviceInfo.getNodeId())).setNodeId(deviceInfo.getNodeId());
        nodesBldr.setNodeType(NodeTypes.forValue(nodeType));

        if (portMapList != null) {
            nodesBldr.setMapping(portMapList);
        }
        if (cp2DegreeList != null) {
            nodesBldr.setCpToDegree(cp2DegreeList);
        }

        List<Nodes> nodesList = new ArrayList<>();
        nodesList.add(nodesBldr.build());

        NetworkBuilder nwBldr = new NetworkBuilder();
        nwBldr.setNodes(nodesList);

        final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Network> nodesIID = InstanceIdentifier.builder(Network.class).build();
        Network network = nwBldr.build();
        writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, nodesIID, network);
        CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
        try {
            submit.checkedGet();
            return true;

        } catch (TransactionCommitFailedException e) {
            LOG.warn("Failed to post {}", network, e);
            return false;
        }
    }

    @Override
    public Mapping getMapping(String nodeId, String logicalConnPoint) {

        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<Mapping> portMappingIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
            new NodesKey(nodeId)).child(Mapping.class, new MappingKey(logicalConnPoint)).build();
        try (ReadOnlyTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Mapping> mapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, portMappingIID).get()
                .toJavaUtil();
            if (mapObject.isPresent()) {
                Mapping mapping = mapObject.get();
                LOG.info("Found mapping for the logical port {}. Mapping: {}", logicalConnPoint, mapping.toString());
                return mapping;
            } else {
                LOG.warn("Could not find mapping for logical connection point {} for nodeId {}", logicalConnPoint,
                    nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for logical connection point : {} for nodeId {}", logicalConnPoint,
                nodeId, ex);
        }
        return null;
    }

    @Override
    public void deleteMappingData(String nodeId) {
        LOG.info("Deleting Mapping Data corresponding at node '{}'", nodeId);
        WriteTransaction rw = this.dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Nodes> nodesIID = InstanceIdentifier.create(Network.class)
            .child(Nodes.class, new NodesKey(nodeId));
        rw.delete(LogicalDatastoreType.CONFIGURATION, nodesIID);
        try {
            rw.submit().get(1, TimeUnit.SECONDS);
            LOG.info("Port mapping removal for node '{}'", nodeId);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error for removing port mapping infos for node '{}'", nodeId);
        }

    }

    @Override
    public boolean updateMapping(String nodeId, Mapping oldMapping) {
        LOG.info("Updating Mapping Data {} for node {}", oldMapping, nodeId);
        InstanceIdentifier<Ports> portIId = InstanceIdentifier.create(OrgOpenroadmDevice.class).child(
            CircuitPacks.class, new CircuitPacksKey(oldMapping.getSupportingCircuitPackName())).child(Ports.class,
                new PortsKey(oldMapping.getSupportingPort()));
        if ((oldMapping != null) && (nodeId != null)) {
            try {
                Optional<Ports> portObject = this.deviceTransactionManager.getDataFromDevice(nodeId,
                    LogicalDatastoreType.OPERATIONAL, portIId, Timeouts.DEVICE_READ_TIMEOUT,
                    Timeouts.DEVICE_READ_TIMEOUT_UNIT);
                if (portObject.isPresent()) {
                    Ports port = portObject.get();
                    Mapping newMapping = createMappingObject(nodeId, port, oldMapping.getSupportingCircuitPackName(),
                        oldMapping.getLogicalConnectionPoint());

                    final WriteTransaction writeTransaction = this.dataBroker.newWriteOnlyTransaction();
                    InstanceIdentifier<Mapping> mapIID = InstanceIdentifier.create(Network.class).child(Nodes.class,
                        new NodesKey(nodeId)).child(Mapping.class, new MappingKey(oldMapping
                            .getLogicalConnectionPoint()));
                    writeTransaction.merge(LogicalDatastoreType.CONFIGURATION, mapIID, newMapping);
                    CheckedFuture<Void, TransactionCommitFailedException> submit = writeTransaction.submit();
                    submit.checkedGet();
                    return true;
                }
                return false;
            } catch (TransactionCommitFailedException e) {
                LOG.error("Transaction Commit Error updating Mapping {} for node {}", oldMapping
                    .getLogicalConnectionPoint(), nodeId, e);
                return false;
            }
        } else {
            LOG.error("Impossible to update mapping");
            return false;
        }
    }

    @Override
    public boolean createMappingData(Info deviceInfo, List<Mapping> portMapList, List<CpToDegree> degreeCpList) {
        LOG.info("Create Mapping Data for node {}", deviceInfo.getNodeId());
        return postPortMapping(deviceInfo, portMapList, deviceInfo.getNodeType().getIntValue(), degreeCpList);
    }

}
