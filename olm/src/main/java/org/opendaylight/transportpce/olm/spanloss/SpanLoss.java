/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.olm.spanloss;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.MountPointService;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.transportpce.olm.OlmPowerSetupImpl;
import org.opendaylight.transportpce.renderer.mapping.PortMapping;
import org.opendaylight.transportpce.renderer.provisiondevice.OpenRoadmInterfaces;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev161014.RatioDB;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.Interface;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceBuilder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.interfaces.grp.InterfaceKey;
import org.opendaylight.yang.gen.v1.http.org.openroadm.device.rev170206.org.openroadm.device.container.OrgOpenroadmDevice;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.Interface1Builder;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.Ots;
import org.opendaylight.yang.gen.v1.http.org.openroadm.optical.transport.interfaces.rev161014.ots.container.OtsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.GetPmOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.olm.rev170418.get.pm.output.Measurements;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.portmapping.rev170228.network.nodes.Mapping;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpanLoss {
    private static final Logger LOG = LoggerFactory.getLogger(SpanLoss.class);
    private final DataBroker db;
    private final MountPointService mps;

    /**
     * Instantiates a new span loss.
     *
     * @param db
     *            the db
     * @param mps
     *            the mps
     */
    public SpanLoss(DataBroker db, MountPointService mps) {
        this.db = db;
        this.mps = mps;
    }

    /**
     * This method retrieves OTS PM from current PM list by nodeId and TPId:
     * Steps:
     *
     * <p>
     * 1. Get OTS interface name from port mapping by TPId 2. Call getPm RPC to
     * get OTS PM
     *
     * <p>
     *
     * @param nodeId
     *            Node-id of the NE.
     * @param tpID
     *            Termination point Name.
     * @return reference to OtsPmHolder
     */
    public OtsPmHolder getPmMeasurements(String nodeId, String tpID, String pmName) {
        GetPmInputBuilder otsPmInputBuilder = new GetPmInputBuilder();
        Mapping portMapping = new OpenRoadmInterfaces(db, mps, nodeId, tpID).getMapping(nodeId, tpID);
        if (portMapping != null) {
            otsPmInputBuilder.setNodeId(nodeId).setResourceType("Interface").setGranularity("15min")
            .setResourceName(portMapping.getSupportingOts());
            Future<RpcResult<GetPmOutput>> otsPmOutput = new OlmPowerSetupImpl(db, mps)
                    .getPm(otsPmInputBuilder.build());
            if (otsPmOutput != null) {
                try {
                    for (Measurements measurement : otsPmOutput.get().getResult().getMeasurements()) {
                        if (measurement.getPmparameterName().equals(pmName)) {
                            return new OtsPmHolder(pmName,Double.parseDouble(measurement.getPmparameterValue()),
                                   portMapping.getSupportingOts());
                        }
                    }
                } catch (NumberFormatException | InterruptedException | ExecutionException e) {
                    LOG.warn("Unable to get PM for NodeId: {} TP Id:{} PMName:{}", nodeId, tpID, pmName, e);
                }
            } else {
                LOG.info("OTS PM not found for NodeId: {} TP Id:{} PMName:{}", nodeId, tpID, pmName);
            }
        }
        return null;
    }

    /**
     * This method Sets Spanloss on A-End and Z-End OTS interface: Steps:
     *
     * <p>
     * 1. Read existing interface details
     *
     * <p>
     * 2. Set spanloss
     *
     * @param nodeId
     *            nodeId of NE on which spanloss need to be updated
     * @param interfaceName
     *            OTS interface for NE on which spanloss is cacluated
     * @param spanLoss
     *            calculated spanloss value
     * @param direction
     *            for which spanloss is calculated.It can be either Tx or Rx
     * @return true/false
     */
    public boolean setSpanLoss(String nodeId, String interfaceName, BigDecimal spanLoss, String direction) {
        LOG.info("Setting Spanloss in device for:" + nodeId + " InterfaceName:" + interfaceName);
        InstanceIdentifier<Interface> interfacesIID = InstanceIdentifier.create(OrgOpenroadmDevice.class)
                .child(Interface.class, new InterfaceKey(interfaceName));
        Optional<Interface> interfaceObject;
        try {
            DataBroker deviceDb = PortMapping.getDeviceDataBroker(nodeId, mps);
            ReadWriteTransaction rwtx = deviceDb.newReadWriteTransaction();
            interfaceObject = rwtx.read(LogicalDatastoreType.CONFIGURATION, interfacesIID).get();
            if (interfaceObject.isPresent()) {
                Interface intf = interfaceObject.get();
                InterfaceBuilder interfaceBuilder = new InterfaceBuilder(intf);
                Ots ots = intf.getAugmentation(Interface1.class).getOts();
                Interface1Builder intf1Builder = new Interface1Builder();
                OtsBuilder otsBuilder = new OtsBuilder();
                otsBuilder.setFiberType(ots.getFiberType());
                if (direction.equals("TX")) {
                    otsBuilder.setSpanLossTransmit(new RatioDB(spanLoss)).setSpanLossReceive(ots.getSpanLossReceive());
                } else {
                    otsBuilder.setSpanLossTransmit(ots.getSpanLossTransmit()).setSpanLossReceive(new RatioDB(spanLoss));
                }
                interfaceBuilder.addAugmentation(Interface1.class, intf1Builder.setOts(otsBuilder.build()).build());
                rwtx.put(LogicalDatastoreType.CONFIGURATION, interfacesIID, interfaceBuilder.build());
                CheckedFuture<Void, TransactionCommitFailedException> submit = rwtx.submit();
                submit.checkedGet();
                LOG.info("Spanloss Value update completed successfully");
                return true;
            }
        } catch (InterruptedException | ExecutionException | TransactionCommitFailedException e) {
            LOG.warn("Unable to set spanloss", e);
        }
        return false;
    }

    /**
     * This method calculates Spanloss by TranmistPower - Receive Power Steps:
     *
     * <p>
     * 1. Read PM measurement
     *
     * <p>
     * 2. Set Spanloss value for interface
     *
     * @param roadmLinks
     *            reference to list of RoadmLinks
     * @return true/false
     */
    public boolean getLinkSpanloss(List<RoadmLinks> roadmLinks) {
        LOG.info("Executing GetLinkSpanLoss");
        BigDecimal spanLoss = new BigDecimal(0);
        for (int i = 0; i < roadmLinks.size(); i++) {
            OtsPmHolder srcOtsPmHoler = getPmMeasurements(roadmLinks.get(i).getSrcNodeId(),
                    roadmLinks.get(i).getSrcTpId(), "OpticalPowerOutput");
            OtsPmHolder destOtsPmHoler = getPmMeasurements(roadmLinks.get(i).getDestNodeId(),
                    roadmLinks.get(i).getDestTpid(), "OpticalPowerInput");
            spanLoss = new BigDecimal(srcOtsPmHoler.getOtsParameterVal() - destOtsPmHoler.getOtsParameterVal())
                    .setScale(0, RoundingMode.HALF_UP);
            LOG.info("Spanloss Calculated as :" + spanLoss + "=" + srcOtsPmHoler.getOtsParameterVal() + "-"
                    + destOtsPmHoler.getOtsParameterVal());
            if (spanLoss.doubleValue() < 28 && spanLoss.doubleValue() > 0) {
                if (!setSpanLoss(roadmLinks.get(i).getSrcNodeId(), srcOtsPmHoler.getOtsInterfaceName(), spanLoss,
                        "TX")) {
                    LOG.info("Setting spanLoss failed for " + roadmLinks.get(i).getSrcNodeId());
                    return false;
                }
                if (!setSpanLoss(roadmLinks.get(i).getDestNodeId(), destOtsPmHoler.getOtsInterfaceName(), spanLoss,
                        "RX")) {
                    LOG.info("Setting spanLoss failed for " + roadmLinks.get(i).getDestNodeId());
                    return false;
                }
            }
        }
        return true;
    }
}