/*
 * Copyright © 2017 AT&T, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.pce;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.transportpce.common.fixedflex.GridConstant;
import org.opendaylight.transportpce.common.fixedflex.GridUtils;
import org.opendaylight.transportpce.pce.networkanalyzer.PceLink;
import org.opendaylight.transportpce.pce.networkanalyzer.PceResult;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyGHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.optical.channel.types.rev230526.FrequencyTHz;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.state.types.rev191129.State;
import org.opendaylight.yang.gen.v1.http.org.openroadm.common.types.rev181019.ModulationFormat;
import org.opendaylight.yang.gen.v1.http.org.openroadm.otn.common.types.rev210924.OpucnTribSlotDef;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.AToZDirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ZToADirectionBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZ;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.atoz.direction.AToZKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToA;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToABuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.path.description.ztoa.direction.ZToAKey;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.Resource;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.ResourceBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce.resource.resource.resource.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.LinkId;
import org.opendaylight.yangtools.yang.common.Decimal64;
import org.opendaylight.yangtools.yang.common.Uint32;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PcePathDescription {
    /* Logging. */
    private static final Logger LOG = LoggerFactory.getLogger(PcePathDescription.class);

    private List<PceLink> pathAtoZ = null;
    private PceResult rc;
    private Map<LinkId, PceLink> allPceLinks = null;

    public PcePathDescription(List<PceLink> pathAtoZ, Map<LinkId, PceLink> allPceLinks, PceResult rc) {
        super();
        this.allPceLinks = allPceLinks;
        this.pathAtoZ = pathAtoZ;
        this.rc = rc;
    }

    public PceResult buildDescriptions() {
        LOG.info("In buildDescriptions: AtoZ =  {}", pathAtoZ);
        Map<AToZKey,AToZ> atozMap = new HashMap<>();
        if (pathAtoZ == null) {
            rc.error("The path AtoZ is empty.");
            LOG.error("In buildDescriptions: there is empty AtoZ path");
            return rc;
        }

        buildAtoZ(atozMap, pathAtoZ);
        rc.setAtoZDirection(buildAtoZDirection(atozMap).build());
        List<PceLink> pathZtoA = ImmutableList.copyOf(pathAtoZ).reverse();
        LOG.info("In buildDescriptions: ZtoA {}", pathZtoA);

        Map<ZToAKey,ZToA> ztoaMap = new HashMap<>();
        if (pathZtoA == null) {
            rc.error("The path ZtoA is empty.");
            LOG.error("In buildDescriptions: there is empty ZtoA path");
            return rc;
        }
        buildZtoA(ztoaMap, pathZtoA);
        rc.setZtoADirection(buildZtoADirection(ztoaMap).build());

        return rc;
    }

    /**
     * Create a builder for AtoZDirection object.
     * @param atozMap Map of AToZ object
     * @return a builder for AtoZDirection object
     */
    private AToZDirectionBuilder buildAtoZDirection(Map<AToZKey, AToZ> atozMap) {
        ModulationFormat modulationFormat = GridConstant.RATE_MODULATION_FORMAT_MAP
                .getOrDefault(Uint32.valueOf(rc.getRate()), ModulationFormat.DpQpsk);
        AToZDirectionBuilder atoZDirectionBldr = new AToZDirectionBuilder()
            .setRate(Uint32.valueOf(rc.getRate()))
            .setModulationFormat(modulationFormat.getName())
            .setAToZ(atozMap);
        switch (rc.getServiceType()) {
            case StringConstants.SERVICE_TYPE_400GE:
            case StringConstants.SERVICE_TYPE_OTUC2:
            case StringConstants.SERVICE_TYPE_OTUC3:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_100GE_T:
            case StringConstants.SERVICE_TYPE_OTU4:
                atoZDirectionBldr
                        .setAToZMaxFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMaxFreq())))
                        .setAToZMinFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMinFreq())))
                        .setAToZWavelengthNumber(Uint32.valueOf(rc.getResultWavelength()))
                        // Used precision 5 to get the exact decimal values of the frequency
                        .setCentralFrequency(new FrequencyTHz(GridUtils.getCentralFrequencyWithPrecision(
                                rc.getMinFreq(), rc.getMaxFreq(), 5).getValue()))
                        .setWidth(GridUtils.getWidthFromRateAndModulationFormat(
                                Uint32.valueOf(rc.getRate()), modulationFormat));
                break;
            case StringConstants.SERVICE_TYPE_OTHER:
                FrequencyGHz width = FrequencyGHz.getDefaultInstance(rc.getMaxFreq().subtract(rc.getMinFreq())
                        .multiply(BigDecimal.valueOf(1000)).subtract(BigDecimal.valueOf(8.0)).toPlainString());
                atoZDirectionBldr
                        .setAToZMaxFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMaxFreq())))
                        .setAToZMinFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMinFreq())))
                        .setAToZWavelengthNumber(Uint32.valueOf(rc.getResultWavelength()))
                        .setCentralFrequency(new FrequencyTHz(GridUtils.getCentralFrequencyWithPrecision(
                                rc.getMinFreq(), rc.getMaxFreq(), 5).getValue()))
                        .setWidth(width);
                break;
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_100GE_S:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC2:
            case StringConstants.SERVICE_TYPE_ODUC3:
            case StringConstants.SERVICE_TYPE_ODUC4:
                if (rc.getResultTribPortTribSlot() != null && rc.getResultTribPortTribSlot().get(0) != null
                    && rc.getResultTribPortTribSlot().get(1) != null) {
                    atoZDirectionBldr.setAToZWavelengthNumber(Uint32.ZERO)
                            .setMinTribSlot(rc.getResultTribPortTribSlot().get(0))
                            .setMaxTribSlot(rc.getResultTribPortTribSlot().get(1));
                } else {
                    LOG.warn("Trib port and trib slot number should be present");
                    atoZDirectionBldr.setMinTribSlot(new OpucnTribSlotDef("0.0"))
                        .setMaxTribSlot(new OpucnTribSlotDef("0.0"));
                }
                break;
            default:
                LOG.warn("unknown service type : unable to set Min/Max frequencies or trib-port/trib-slot");
                break;
        }
        return atoZDirectionBldr;
    }

    /**
     * Create a builder for ZtoADirection object.
     * @param ztoaMap Map of ZToA object
     * @return a builder for ZtoADirection object
     */
    private ZToADirectionBuilder buildZtoADirection(Map<ZToAKey, ZToA> ztoaMap) {
        ModulationFormat modulationFormat = GridConstant.RATE_MODULATION_FORMAT_MAP
                .getOrDefault(Uint32.valueOf(rc.getRate()), ModulationFormat.DpQpsk);
        ZToADirectionBuilder ztoADirectionBldr = new ZToADirectionBuilder().setRate(Uint32.valueOf(rc.getRate()))
                .setModulationFormat(modulationFormat.getName())
                .setZToA(ztoaMap);
        switch (rc.getServiceType()) {
            case StringConstants.SERVICE_TYPE_400GE:
            case StringConstants.SERVICE_TYPE_OTUC2:
            case StringConstants.SERVICE_TYPE_OTUC3:
            case StringConstants.SERVICE_TYPE_OTUC4:
            case StringConstants.SERVICE_TYPE_100GE_T:
            case StringConstants.SERVICE_TYPE_OTU4:
                ztoADirectionBldr
                        .setZToAMaxFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMaxFreq())))
                        .setZToAMinFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMinFreq())))
                        .setZToAWavelengthNumber(Uint32.valueOf(rc.getResultWavelength()))
                        .setCentralFrequency(new FrequencyTHz(GridUtils.getCentralFrequencyWithPrecision(
                                rc.getMinFreq(), rc.getMaxFreq(), 5).getValue()))
                        .setWidth(GridUtils.getWidthFromRateAndModulationFormat(
                                Uint32.valueOf(rc.getRate()), modulationFormat));
                break;
            case StringConstants.SERVICE_TYPE_OTHER:
                FrequencyGHz width = FrequencyGHz.getDefaultInstance(rc.getMaxFreq().subtract(rc.getMinFreq())
                        .multiply(BigDecimal.valueOf(1000)).subtract(BigDecimal.valueOf(8.0)).toPlainString());
                ztoADirectionBldr
                        .setZToAMaxFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMaxFreq())))
                        .setZToAMinFrequency(new FrequencyTHz(Decimal64.valueOf(rc.getMinFreq())))
                        .setZToAWavelengthNumber(Uint32.valueOf(rc.getResultWavelength()))
                        .setCentralFrequency(new FrequencyTHz(GridUtils.getCentralFrequencyWithPrecision(
                                rc.getMinFreq(), rc.getMaxFreq(), 5).getValue()))
                        .setWidth(width);
                break;
            case StringConstants.SERVICE_TYPE_100GE_M:
            case StringConstants.SERVICE_TYPE_100GE_S:
            case StringConstants.SERVICE_TYPE_10GE:
            case StringConstants.SERVICE_TYPE_1GE:
            case StringConstants.SERVICE_TYPE_ODU4:
            case StringConstants.SERVICE_TYPE_ODUC2:
            case StringConstants.SERVICE_TYPE_ODUC3:
            case StringConstants.SERVICE_TYPE_ODUC4:
                if (rc.getResultTribPortTribSlot() != null && rc.getResultTribPortTribSlot().get(0) != null
                    && rc.getResultTribPortTribSlot().get(1) != null) {
                    ztoADirectionBldr.setZToAWavelengthNumber(Uint32.ZERO)
                            .setMinTribSlot(rc.getResultTribPortTribSlot().get(0))
                            .setMaxTribSlot(rc.getResultTribPortTribSlot().get(1));
                } else {
                    LOG.warn("Trib port and trib slot number should be present");
                    ztoADirectionBldr.setMinTribSlot(new OpucnTribSlotDef("0.0"))
                        .setMaxTribSlot(new OpucnTribSlotDef("0.0"));
                }
                break;
            default:
                LOG.warn("unknown service type : unable to set Min/Max frequencies");
                break;
        }
        return ztoADirectionBldr;
    }

    //sonar issue This method has 77 lines, which is greater than the 75 lines authorized. Split it into smaller
    //ignore as it's not relevant to split it from functional point
    private void buildAtoZ(Map<AToZKey, AToZ> atozMap, List<PceLink> path) {
        Integer index = 0;
        PceLink lastLink = null;
        AToZ lastResource = null;

        // build A side Client TP
        String tpName = path.get(0).getClientA();
        String xname = path.get(0).getSourceId().getValue();
        TerminationPoint stp = new TerminationPointBuilder()
                .setTpId(tpName).setTpNodeId(xname)
                .build();

        AToZKey clientKey = new AToZKey(index.toString());
        Resource clientResource = new ResourceBuilder().setResource(stp).setState(State.InService).build();
        AToZ firstResource = new AToZBuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        atozMap.put(firstResource.key(),firstResource);
        index += 1;
        for (PceLink pcelink : path) {
            String srcName = pcelink.getSourceId().getValue();
            // Nodes
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501
                    .pce.resource.resource.resource.Node sourceNode = new NodeBuilder()
                    .setNodeId(srcName)
                    .build();

            // Source Resource
            AToZKey sourceKey = new AToZKey(index.toString());
            Resource nodeResource1 = new ResourceBuilder().setResource(sourceNode).setState(State.InService).build();
            AToZ srcResource = new AToZBuilder().setId(srcName).withKey(sourceKey).setResource(nodeResource1).build();
            index += 1;
            atozMap.put(srcResource.key(),srcResource);

            // source TP
            tpName = pcelink.getSourceTP().getValue();
            stp = new TerminationPointBuilder()
                    .setTpNodeId(srcName).setTpId(tpName)
                    .build();

            // Resource
            AToZKey srcTPKey = new AToZKey(index.toString());
            Resource tpResource1 = new ResourceBuilder().setResource(stp).setState(State.InService).build();
            AToZ stpResource = new AToZBuilder().setId(tpName).withKey(srcTPKey).setResource(tpResource1).build();
            index += 1;
            atozMap.put(stpResource.key(),stpResource);

            String linkName = pcelink.getLinkId().getValue();
            // Link
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501
                    .pce.resource.resource.resource.Link atozLink = new LinkBuilder()
                    .setLinkId(linkName)
                    .build();

            // Link Resource
            AToZKey linkKey = new AToZKey(index.toString());
            Resource nodeResource2 = new ResourceBuilder().setResource(atozLink).setState(pcelink.getState()).build();
            AToZ linkResource = new AToZBuilder().setId(linkName).withKey(linkKey).setResource(nodeResource2).build();
            index += 1;
            atozMap.put(linkResource.key(),linkResource);

            String destName = pcelink.getDestId().getValue();
            // target TP
            tpName = pcelink.getDestTP().getValue();
            TerminationPoint dtp = new TerminationPointBuilder()
                .setTpNodeId(destName).setTpId(tpName)
                .build();

            // Resource
            AToZKey destTPKey = new AToZKey(index.toString());
            Resource tpResource2 = new ResourceBuilder().setResource(dtp).setState(State.InService).build();
            AToZ ttpResource = new AToZBuilder().setId(tpName).withKey(destTPKey).setResource(tpResource2).build();
            index += 1;
            atozMap.put(ttpResource.key(),ttpResource);

            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce
                .resource.resource.resource.Node targetNode = new NodeBuilder()
                .setNodeId(destName)
                .build();

            // Target Resource
            AToZKey targetKey = new AToZKey(index.toString());
            Resource nodeResource3 = new ResourceBuilder().setResource(targetNode).setState(State.InService).build();
            lastResource = new AToZBuilder().setId(destName).withKey(targetKey).setResource(nodeResource3).build();

            lastLink = pcelink;
        }

        if (lastResource != null) {
            atozMap.put(lastResource.key(),lastResource);
        }

        // build Z side Client TP
        tpName = lastLink.getClientZ();
        xname = lastLink.getDestId().getValue();
        stp = new TerminationPointBuilder()
                .setTpNodeId(xname).setTpId(tpName)
                .build();

        index += 1;
        clientKey = new AToZKey(index.toString());
        clientResource = new ResourceBuilder().setResource(stp).setState(State.InService).build();
        lastResource = new AToZBuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        atozMap.put(lastResource.key(),lastResource);

    }

    private void buildZtoA(Map<ZToAKey, ZToA> ztoaList, List<PceLink> path) {
        Integer index = 0;
        PceLink lastLink = null;
        ZToA lastResource = null;

        // build Z size Client TP
        PceLink pcelink = this.allPceLinks.get(path.get(0).getOppositeLink());
        String tpName = pcelink.getClientA();
        String xname = pcelink.getSourceId().getValue();
        TerminationPoint stp = new TerminationPointBuilder()
                .setTpNodeId(xname).setTpId(tpName)
                .build();

        ZToAKey clientKey = new ZToAKey(index.toString());
        Resource clientResource = new ResourceBuilder().setResource(stp).setState(State.InService).build();
        ZToA firstResource = new ZToABuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        ztoaList.put(firstResource.key(),firstResource);
        index += 1;

        for (PceLink pcelinkAtoZ : path) {

            pcelink = this.allPceLinks.get(pcelinkAtoZ.getOppositeLink());
            LOG.debug("link to oppsite: {} to {}", pcelinkAtoZ, pcelink);

            String srcName = pcelink.getSourceId().getValue();


            // Nodes
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce
                .resource.resource.resource.Node sourceNode = new NodeBuilder()
                .setNodeId(srcName).build();


            // Source Resource
            ZToAKey sourceKey = new ZToAKey(index.toString());
            Resource nodeResource1 = new ResourceBuilder().setResource(sourceNode).setState(State.InService).build();
            ZToA srcResource = new ZToABuilder().setId(srcName).withKey(sourceKey).setResource(nodeResource1).build();
            index += 1;
            ztoaList.put(srcResource.key(),srcResource);

            // source TP
            tpName = pcelink.getSourceTP().getValue();
            stp = new TerminationPointBuilder()
                    .setTpNodeId(srcName).setTpId(tpName)
                    .build();

            // Resource
            ZToAKey srcTPKey = new ZToAKey(index.toString());
            Resource tpResource1 = new ResourceBuilder().setResource(stp).setState(State.InService).build();
            ZToA stpResource = new ZToABuilder().setId(tpName).withKey(srcTPKey).setResource(tpResource1).build();
            index += 1;
            ztoaList.put(stpResource.key(),stpResource);

            String linkName = pcelink.getLinkId().getValue();
            // Link
            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce
                .resource.resource.resource.Link ztoaLink = new LinkBuilder()
                .setLinkId(linkName).build();

            // Link Resource
            ZToAKey linkKey = new ZToAKey(index.toString());
            Resource nodeResource2 = new ResourceBuilder().setResource(ztoaLink).setState(State.InService).build();
            ZToA linkResource = new ZToABuilder().setId(linkName).withKey(linkKey).setResource(nodeResource2).build();
            index += 1;
            ztoaList.put(linkResource.key(),linkResource);

            String destName = pcelink.getDestId().getValue();
            // target TP
            tpName = pcelink.getDestTP().getValue();
            TerminationPoint ttp = new TerminationPointBuilder()
                    .setTpNodeId(destName).setTpId(tpName).build();

            // Resource
            ZToAKey destTPKey = new ZToAKey(index.toString());
            Resource tpResource2 = new ResourceBuilder().setResource(ttp).setState(State.InService).build();
            ZToA ttpResource = new ZToABuilder().setId(tpName).withKey(destTPKey).setResource(tpResource2).build();
            index += 1;
            ztoaList.put(ttpResource.key(),ttpResource);


            org.opendaylight.yang.gen.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501.pce
                .resource.resource.resource.Node targetNode = new NodeBuilder()
                .setNodeId(destName).build();
            // Target Resource
            ZToAKey targetKey = new ZToAKey(index.toString());
            Resource nodeResource3 = new ResourceBuilder().setResource(targetNode).setState(State.InService).build();
            lastResource = new ZToABuilder().setId(destName).withKey(targetKey).setResource(nodeResource3).build();

            lastLink = pcelink;
        }
        if (lastResource != null) {
            ztoaList.put(lastResource.key(),lastResource);
        }

        // build Z side Client TP
        tpName = lastLink.getClientZ();
        xname = lastLink.getDestId().getValue();
        stp = new TerminationPointBuilder()
                .setTpNodeId(xname).setTpId(tpName).build();

        index += 1;
        clientKey = new ZToAKey(index.toString());
        clientResource = new ResourceBuilder().setResource(stp).setState(State.InService).build();
        lastResource = new ZToABuilder().setId(tpName).withKey(clientKey).setResource(clientResource).build();
        ztoaList.put(lastResource.key(),lastResource);
    }

    public PceResult getReturnStructure() {
        return rc;
    }
}
