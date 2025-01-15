/*
 * Copyright © 2017 AT&T and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.common.mapping;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.opendaylight.mdsal.binding.api.DataBroker;
import org.opendaylight.mdsal.binding.api.ReadTransaction;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.transportpce.common.StringConstants;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.Network;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.mc.capabilities.McCapabilities;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.Nodes;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.NodesKey;
import org.opendaylight.yang.gen.v1.http.org.opendaylight.transportpce.portmapping.rev250115.network.nodes.NodeInfo;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GEODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If100GEOduflexgfp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEODU2e;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If10GEOduflexgfp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If1GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If1GEODU0;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If200GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If200GEOduflexcbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If25GEOduflexcbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If400GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If400GEOduflexcbr;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If400GEOdufleximp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If40GE;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If40GEODU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.If40GEOduflexgfp;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCH;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU1ODU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU2EODU2E;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU2ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU3ODU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4Regen;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOCHOTU4ODU4Uniregen;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTU1ODU1;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTU2ODU2;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTU3ODU3;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTU4ODU4;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTUCnODUCn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTUCnODUCnRegen;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOTUCnODUCnUniregen;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOtsiOtsigroup;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.IfOtsiOtucnOducn;
import org.opendaylight.yang.gen.v1.http.org.openroadm.port.types.rev230526.SupportedIfCapability;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public final class MappingUtilsImpl implements MappingUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MappingUtilsImpl.class);

    private static final ImmutableMap<String, SupportedIfCapability> CAP_TYPE_MAP =
        ImmutableMap.<String, SupportedIfCapability>builder()
            .put("If1GE{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-1GE}", If1GE.VALUE)
            .put("If1GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-1GE}", If1GE.VALUE)
            .put("If1GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-1GE}", If1GE.VALUE)
            .put("If1GEODU0{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-1GE-ODU0}", If1GEODU0.VALUE)
            .put("If1GEODU0{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-1GE-ODU0}", If1GEODU0.VALUE)
            .put("If1GEODU0{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-1GE-ODU0}", If1GEODU0.VALUE)
            .put("If10GE{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-10GE}", If10GE.VALUE)
            .put("If10GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-10GE}", If10GE.VALUE)
            .put("If10GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE}", If10GE.VALUE)
            .put("If10GEODU2{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-10GE-ODU2}",
                If10GEODU2.VALUE)
            .put("If10GEODU2{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-10GE-ODU2}",
                If10GEODU2.VALUE)
            .put("If10GEODU2{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE-ODU2}",
                If10GEODU2.VALUE)
            .put("If10GEODU2e{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-10GE-ODU2e}",
                If10GEODU2e.VALUE)
            .put("If10GEODU2e{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-10GE-ODU2e}",
                If10GEODU2e.VALUE)
            .put("If10GEODU2e{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE-ODU2e}",
                If10GEODU2e.VALUE)
            .put("If10GEoduflexgfp{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-10GE-oduflexgfp}",
                If10GEOduflexgfp.VALUE)
            .put("If10GEoduflexgfp{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-10GE-oduflexgfp}",
                If10GEOduflexgfp.VALUE)
            .put("If25GEoduflexcbr{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-25GE-oduflexcbr}",
                If25GEOduflexcbr.VALUE)
            .put("If25GEoduflexcbr{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-25GE-oduflexcbr}",
                If25GEOduflexcbr.VALUE)
            .put("If40GE{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-40GE}", If40GE.VALUE)
            .put("If40GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-40GE}", If40GE.VALUE)
            .put("If40GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-40GE}", If40GE.VALUE)
            .put("If40GEODU3{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-40GE-ODU3}",
                If40GEODU3.VALUE)
            .put("If40GEODU3{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-40GE-ODU3}",
                If40GEODU3.VALUE)
            .put("If40GEODU3{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-40GE-ODU3}",
                If40GEODU3.VALUE)
            .put("If40GEoduflexgfp{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-40GE-oduflexgfp}",
                If40GEOduflexgfp.VALUE)
            .put("If40GEoduflexgfp{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-40GE-oduflexgfp}",
                If40GEOduflexgfp.VALUE)
            .put("If100GE{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-100GE}", If100GE.VALUE)
            .put("If100GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-100GE}", If100GE.VALUE)
            .put("If100GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-100GE}", If100GE.VALUE)
            .put("If100GEODU4{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-100GE-ODU4}",
                If100GEODU4.VALUE)
            .put("If100GEODU4{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-100GE-ODU4}",
                If100GEODU4.VALUE)
            .put("If100GEODU4{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-100GE-ODU4}",
                If100GEODU4.VALUE)
            .put("If100GEoduflexgfp{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-100GE-oduflexgfp}",
                If100GEOduflexgfp.VALUE)
            .put("If100GEoduflexgfp{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-100GE-oduflexgfp}",
                If100GEOduflexgfp.VALUE)
            .put("If200GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-200GE}", If200GE.VALUE)
            .put("If200GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-200GE}", If200GE.VALUE)
            .put("If200GEoduflexcbr{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-200GE-oduflexcbr}",
                If200GEOduflexcbr.VALUE)
            .put("If200GEoduflexcbr{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-200GE-oduflexcbr}",
                If200GEOduflexcbr.VALUE)
            .put("If400GE{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-400GE}", If400GE.VALUE)
            .put("If400GE{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-400GE}", If400GE.VALUE)
            .put("If400GEodufleximp{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-400GE-odufleximp}",
                If400GEOdufleximp.VALUE)
            .put("If400GEodufleximp{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-400GE-odufleximp}",
                If400GEOdufleximp.VALUE)
            .put("If400GEoduflexcbr{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-400GE-oduflexcbr}",
                If400GEOduflexcbr.VALUE)
            .put("If400GEoduflexcbr{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-400GE-oduflexcbr}",
                If400GEOduflexcbr.VALUE)
            .put("IfOTU1ODU1{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OTU1-ODU1}",
                IfOTU1ODU1.VALUE)
            .put("IfOTU1ODU1{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OTU1-ODU1}",
                IfOTU1ODU1.VALUE)
            .put("IfOTU1ODU1{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OTU1-ODU1}",
                IfOTU1ODU1.VALUE)
            .put("IfOTU2ODU2{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OTU2-ODU2}",
                IfOTU2ODU2.VALUE)
            .put("IfOTU2ODU2{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OTU2-ODU2}",
                IfOTU2ODU2.VALUE)
            .put("IfOTU2ODU2{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OTU2-ODU2}",
                IfOTU2ODU2.VALUE)
            .put("IfOTU3ODU3{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OTU3-ODU3}",
                IfOTU3ODU3.VALUE)
            .put("IfOTU3ODU3{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OTU3-ODU3}",
                IfOTU3ODU3.VALUE)
            .put("IfOTU3ODU3{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OTU3-ODU3}",
                IfOTU3ODU3.VALUE)
            .put("IfOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OTU4-ODU4}",
                IfOTU4ODU4.VALUE)
            .put("IfOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OTU4-ODU4}",
                IfOTU4ODU4.VALUE)
            .put("IfOtsiOtsigroup{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-otsi-otsigroup}",
                IfOtsiOtsigroup.VALUE)
            .put("IfOtsiOtsigroup{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-otsi-otsigroup}",
                IfOtsiOtsigroup.VALUE)
            .put("IfOCH{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH}", IfOCH.VALUE)
            .put("IfOCH{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH}", IfOCH.VALUE)
            .put("IfOCH{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH}", IfOCH.VALUE)
            .put("IfOchOTU1ODU1{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH-OTU1-ODU1}",
                IfOCHOTU1ODU1.VALUE)
            .put("IfOchOTU1ODU1{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH-OTU1-ODU1}",
                IfOCHOTU1ODU1.VALUE)
            .put("IfOchOTU1ODU1{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH-OTU1-ODU1}",
                IfOCHOTU1ODU1.VALUE)
            .put("IfOchOTU2ODU2{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH-OTU2-ODU2}",
                IfOCHOTU2ODU2.VALUE)
            .put("IfOchOTU2ODU2{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH-OTU2-ODU2}",
                IfOCHOTU2ODU2.VALUE)
            .put("IfOchOTU2ODU2{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH-OTU2-ODU2}",
                IfOCHOTU2ODU2.VALUE)
            .put("IfOchOTU2EODU2E{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH-OTU2E-ODU2E}",
                IfOCHOTU2EODU2E.VALUE)
            .put("IfOchOTU2EODU2E{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH-OTU2E-ODU2E}",
                IfOCHOTU2EODU2E.VALUE)
            .put("IfOchOTU2EODU2E{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH-OTU2E-ODU2E}",
                IfOCHOTU2EODU2E.VALUE)
            .put("IfOchOTU3ODU3{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH-OTU3-ODU3}",
                IfOCHOTU3ODU3.VALUE)
            .put("IfOchOTU3ODU3{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH-OTU3-ODU3}",
                IfOCHOTU3ODU3.VALUE)
            .put("IfOchOTU3ODU3{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH-OTU3-ODU3}",
                IfOCHOTU3ODU3.VALUE)
            .put("IfOCHOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2018-10-19)if-OCH-OTU4-ODU4}",
                IfOCHOTU4ODU4.VALUE)
            .put("IfOCHOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2020-03-27)if-OCH-OTU4-ODU4}",
                IfOCHOTU4ODU4.VALUE)
            .put("IfOCHOTU4ODU4{qname=(http://org/openroadm/port/types?revision=2023-05-26)if-OCH-OTU4-ODU4}",
                IfOCHOTU4ODU4.VALUE)
            .put("IfOCHOTU4ODU4regen{qname=(http://org/openroadm/port/types?revision=2018-10-19)"
                + "if-OCH-OTU4-ODU4-regen}", IfOCHOTU4ODU4Regen.VALUE)
            .put("IfOCHOTU4ODU4uniregen{qname=(http://org/openroadm/port/types?revision=2018-10-19)"
                + "if-OCH-OTU4-ODU4-uniregen}", IfOCHOTU4ODU4Uniregen.VALUE)
            .put("IfOTUCnODUCn{qname=(http://org/openroadm/port/types?revision=2020-03-27)"
                + "if-OTUCn-ODUCn}", IfOTUCnODUCn.VALUE)
            .put("IfOTUCnODUCn{qname=(http://org/openroadm/port/types?revision=2023-05-26)"
                + "if-OTUCn-ODUCn}", IfOTUCnODUCn.VALUE)
            .put("IfOTUCnODUCnregen{qname=(http://org/openroadm/port/types?revision=2020-03-27)"
                + "if-OTUCn-ODUCn-regen}", IfOTUCnODUCnRegen.VALUE)
            .put("IfOTUCnODUCnregen{qname=(http://org/openroadm/port/types?revision=2023-05-26)"
                + "if-OTUCn-ODUCn-regen}", IfOTUCnODUCnRegen.VALUE)
            .put("IfOTUCnODUCnuniregen{qname=(http://org/openroadm/port/types?revision=2023-05-26)"
                + "if-OTUCn-ODUCn-uniregen}", IfOTUCnODUCnUniregen.VALUE)
            .put("IfOtsiOtucnOducn{qname=(http://org/openroadm/port/types?revision=2020-03-27)"
                + "if-otsi-otucn-oducn}", IfOtsiOtucnOducn.VALUE)
            .put("IfOtsiOtucnOducn{qname=(http://org/openroadm/port/types?revision=2023-05-26)"
                + "if-otsi-otucn-oducn}", IfOtsiOtucnOducn.VALUE)
            .build();

   //This map will expanded for other interface capabilities
    private static final ImmutableMap<String, SupportedIfCapability> OC_CAP_TYPE_MAP =
            ImmutableMap.<String, SupportedIfCapability>builder()
                    .put("if-100GE-ODU4", If100GEODU4.VALUE)
                    .put("if-OTUCN-ODUCN", IfOTUCnODUCn.VALUE).build();

    private static final ImmutableMap<String, String> OC_CAP_MAP =
            ImmutableMap.<String, String>builder()
                    .put("PROT100GE{qname=(http://openconfig.net/yang/transport-types?revision=2021-07-29)PROT_100GE}",
                            "100GE")
                    .put("PROTODU4{qname=(http://openconfig.net/yang/transport-types?revision=2021-07-29)PROT_ODU4}",
                            "ODU4")
                    .put("PROTOTUCN{qname=(http://openconfig.net/yang/transport-types?revision=2021-07-29)PROT_OTUCN}",
                            "OTUCN")
                    .put("PROTODUCN{qname=(http://openconfig.net/yang/transport-types?revision=2021-07-29)PROT_ODUCN}",
                            "ODUCN").build();
    private final DataBroker dataBroker;

    @Activate
    public MappingUtilsImpl(@Reference DataBroker dataBroker) {
        this.dataBroker = requireNonNull(dataBroker);
    }

    public String getOpenRoadmVersion(String nodeId) {
        /*
         * Getting physical mapping corresponding to logical connection point
         */
        InstanceIdentifier<NodeInfo> nodeInfoIID = InstanceIdentifier.builder(Network.class).child(Nodes.class,
                new NodesKey(nodeId)).child(NodeInfo.class).build();
        try (ReadTransaction readTx = dataBroker.newReadOnlyTransaction()) {
            Optional<NodeInfo> nodeInfoObj =
                    readTx.read(LogicalDatastoreType.CONFIGURATION, nodeInfoIID).get();
            if (nodeInfoObj.isPresent()) {
                NodeInfo nodInfo = nodeInfoObj.orElseThrow();
                switch (nodInfo.getOpenroadmVersion()) {
                    case _71:
                        return StringConstants.OPENROADM_DEVICE_VERSION_7_1;
                    case _221:
                        return StringConstants.OPENROADM_DEVICE_VERSION_2_2_1;
                    case _121:
                        return StringConstants.OPENROADM_DEVICE_VERSION_1_2_1;
                    default:
                        LOG.warn("unknown openROADM device version");
                }
            } else {
                LOG.warn("Could not find mapping for nodeId {}", nodeId);
            }
        } catch (InterruptedException | ExecutionException ex) {
            LOG.error("Unable to read mapping for nodeId {}",nodeId, ex);
        }
        return null;
    }

    /*
    * (non-Javadoc)
    *
    * @see org.opendaylight.transportpce.common.mapping.MappingUtils#getMcCapabilitiesForNode(java.lang.String)
    */
    @Override
    public List<McCapabilities> getMcCapabilitiesForNode(String nodeId) {
        List<McCapabilities> mcCapabilities = new ArrayList<>();
        InstanceIdentifier<Nodes> nodePortMappingIID = InstanceIdentifier.builder(Network.class)
                .child(Nodes.class, new NodesKey(nodeId)).build();
        try (ReadTransaction readTx = this.dataBroker.newReadOnlyTransaction()) {
            Optional<Nodes> nodePortMapObject = readTx.read(LogicalDatastoreType.CONFIGURATION, nodePortMappingIID)
                    .get();
            if (nodePortMapObject.isPresent()) {
                LOG.debug("Found node {}", nodeId);
                Nodes node = nodePortMapObject.orElseThrow();
                mcCapabilities.addAll(node.nonnullMcCapabilities().values());
            }
        } catch (ExecutionException e) {
            LOG.error("Something went wrong while getting node {}", nodeId, e);
        } catch (InterruptedException e) {
            LOG.error("Request interrupted for node {} interrupted", nodeId, e);
            Thread.currentThread().interrupt();
        }
        LOG.debug("Capabilitities for node {}: {}", nodeId, mcCapabilities);
        return mcCapabilities;
    }

    public static SupportedIfCapability convertSupIfCapa(String ifCapType) {
        if (!CAP_TYPE_MAP.containsKey(ifCapType)) {
            LOG.error("supported-if-capability {} not supported", ifCapType);
            return null;
        }
        return CAP_TYPE_MAP.get(ifCapType);
    }

    /**
     * This Method is used to get SupportedIfCapability for openconfig port.
     * @param ifCapType interface name
     * @return supportedIf-capability
     */
    public static SupportedIfCapability ocConvertSupIfCapa(String ifCapType) {
        if (!OC_CAP_TYPE_MAP.containsKey(ifCapType)) {
            LOG.error("supported-if-capability {} not supported", ifCapType);
            return null;
        }
        return OC_CAP_TYPE_MAP.get(ifCapType);
    }

    /**
     * This Method is used to get interface type from metadata.
     * @param interfaceType interface type
     * @return interface name
     */
    public static String getInterfaceType(String interfaceType) {
        if (!OC_CAP_MAP.containsKey(interfaceType)) {
            LOG.error("Interface type {} not found", interfaceType);
            return null;
        }
        return OC_CAP_MAP.get(interfaceType);
    }
}
