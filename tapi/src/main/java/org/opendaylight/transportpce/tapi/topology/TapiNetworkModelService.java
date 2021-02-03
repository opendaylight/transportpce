package org.opendaylight.transportpce.tapi.topology;

public interface TapiNetworkModelService {

    /**
     * Create new TAPI node in tapi topologies.
     *
     * @param orNodeId
     *     unique node ID of new OpenROADM node
     * @param orNodeVersion
     *     OpenROADM node version
     */
    void createTapiNode(String orNodeId, String orNodeVersion);

    /**
     * Delete TAPI node in topologies and update corresponding TAPI context objects.
     *
     * @param nodeId
     *     unique node ID of OpenROADM node.
     *
     */
    void deleteTapinode(String nodeId);
}
