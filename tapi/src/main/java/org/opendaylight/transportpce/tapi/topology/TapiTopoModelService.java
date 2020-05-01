package org.opendaylight.transportpce.tapi.topology;

import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;

public interface TapiTopoModelService {

    /**
     * Create new OpenROADM node in all OpenROADM topologies.
     *
     * @param nodeId
     *   unique node ID of new OpenROADM node
     */
    void createTapiNode(String nodeId, String nodeVersion);

    /**
     * Delete OpenROADM node mapping and topologies.
     *
     * @param nodeId unique node ID of OpenROADM node
     *
     */
    void deleteTapinode(String nodeId);

    /**
     * Set/update connection status of tapi node.
     *
     * @param nodeId
     *   unique node ID of new OpenROADM node
     */
    void deleteSips(String nodeId);

    void deleteLinks(String nodeId);

    void setTapiNodeStatus(String nodeId, NetconfNodeConnectionStatus.ConnectionStatus connectionStatus);
}
