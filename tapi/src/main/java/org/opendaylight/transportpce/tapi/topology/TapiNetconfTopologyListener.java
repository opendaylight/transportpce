package org.opendaylight.transportpce.tapi.topology;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.DataObjectModification;
import org.opendaylight.mdsal.binding.api.DataTreeChangeListener;
import org.opendaylight.mdsal.binding.api.DataTreeModification;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.device.DeviceTransactionManager;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.NetconfNodeConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netconf.node.topology.rev150114.netconf.node.connection.status.available.capabilities.AvailableCapability;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TapiNetconfTopologyListener implements DataTreeChangeListener<Node> {

    private static final Logger LOG = LoggerFactory.getLogger(TapiNetconfTopologyListener.class);
    private TapiTopoModelService tapiTopoModelService;
    private final DataBroker dataBroker;
    private final DeviceTransactionManager deviceTransactionManager;

    public TapiNetconfTopologyListener(TapiTopoModelService tapiTopoModelService, DataBroker dataBroker,
                                       DeviceTransactionManager deviceTransactionManager) {
        this.tapiTopoModelService = tapiTopoModelService;
        this.dataBroker = dataBroker;
        this.deviceTransactionManager = deviceTransactionManager;
    }

    @Override
    public void onDataTreeChanged(@NonNull Collection<DataTreeModification<Node>> changes) {
        LOG.info("onDataTreeChanged");
        for (DataTreeModification<Node> change : changes) {
            DataObjectModification<Node> rootNode = change.getRootNode();
            if ((rootNode.getDataAfter() == null)
                    && (rootNode.getModificationType() != DataObjectModification.ModificationType.DELETE)) {
                LOG.error("rootNode.getDataAfter is null : Node not connected via Netconf protocol");
                continue;
            }
            if (rootNode.getModificationType() == DataObjectModification.ModificationType.DELETE) {
                if (rootNode.getDataBefore() != null) {
                    String nodeId = rootNode.getDataBefore().key().getNodeId().getValue();
                    LOG.info("Node {} deleted", nodeId);
                    this.tapiTopoModelService.deleteTapinode(nodeId);
                    // onDeviceDisConnected(nodeId);
                } else {
                    LOG.error("rootNode.getDataBefore is null !");
                }
                continue;
            }
            String nodeId = rootNode.getDataAfter().key().getNodeId().getValue();
            NetconfNode netconfNode = rootNode.getDataAfter().augmentation(NetconfNode.class);

            if ((netconfNode != null) && !StringConstants.DEFAULT_NETCONF_NODEID.equals(nodeId)) {
                switch (rootNode.getModificationType()) {
                    case WRITE:
                        LOG.info("Node added: {}", nodeId);
                        //fallthrough
                    case SUBTREE_MODIFIED:
                        NetconfNodeConnectionStatus.ConnectionStatus connectionStatus =
                                netconfNode.getConnectionStatus();
                        try {
                            List<AvailableCapability> deviceCapabilities = netconfNode.getAvailableCapabilities()
                                    .getAvailableCapability().stream().filter(cp -> cp.getCapability()
                                            .contains(StringConstants.OPENROADM_DEVICE_MODEL_NAME)).collect(
                                            Collectors.toList());
                            if (!deviceCapabilities.isEmpty()) {
                                Collections.sort(deviceCapabilities, (cp0, cp1) -> cp1.getCapability()
                                        .compareTo(cp0.getCapability()));
                                LOG.info("OpenROADM node detected: {} {}", nodeId, connectionStatus.name());
                                switch (connectionStatus) {
                                    case Connected:
                                        this.tapiTopoModelService.createTapiNode(nodeId, deviceCapabilities.get(0)
                                                .getCapability());
                                        // onDeviceConnected(nodeId,deviceCapabilities.get(0).getCapability());
                                        break;
                                    case Connecting:
                                    case UnableToConnect:
                                        this.tapiTopoModelService.setTapiNodeStatus(nodeId, connectionStatus);
                                        // onDeviceDisConnected(nodeId);
                                        break;
                                    default:
                                        LOG.warn("Unsupported device state {}", connectionStatus.getName());
                                        break;
                                }
                            }

                        } catch (NullPointerException e) {
                            LOG.error("Cannot get available Capabilities");
                        }
                        break;
                    default:
                        LOG.warn("Unexpected connection status : {}", rootNode.getModificationType());
                        break;
                }
            }
        }
    }
}
