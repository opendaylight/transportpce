/*
 * Copyright (c) 2018 Pantheon Technologies s.r.o. All Rights Reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 */

package io.lighty.controllers.tpce.utils;

import com.google.common.collect.ImmutableSet;
import io.lighty.modules.northbound.restconf.community.impl.util.RestConfConfigUtils;
import io.lighty.modules.southbound.netconf.impl.util.NetconfConfigUtils;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opendaylight.yangtools.binding.meta.YangModuleInfo;

public final class TPCEUtils {

    private static final Set<YangModuleInfo> TPCE_MODELS = ImmutableSet.of(

            // common models 1.2.1 and 2.2.1
            org.opendaylight.yang.svc.v1.http.org.openroadm.alarm.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.alarm.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.types.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.types.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.equipment.states.types.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.equipment.states.types.rev171215.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.layerrate.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.types.rev171215.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pm.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pm.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pm.types.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pm.types.rev171215.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.port.types.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.probablecause.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.probablecause.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.types.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.types.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.switching.pool.types.rev171215.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.tca.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.tca.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.user.mgmt.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.user.mgmt.rev171215.YangModuleInfoImpl.getInstance(),

            // common models 7.1
            org.opendaylight.yang.svc.v1.http.org.openroadm.alarm.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.alarm.pm.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.amplifier.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.attributes.rev200327.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.equipment.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.link.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.node.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.optical.channel.types.rev200529.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.state.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.types.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.equipment.states.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.interfaces.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.layerrate.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.manifest.file.rev200327.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.network.resource.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.types.rev200327.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pm.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pm.types.rev200327.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.port.types.rev200327.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.probablecause.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.types.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.service.format.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.switching.pool.types.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.tca.rev200327.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.user.mgmt.rev191129.YangModuleInfoImpl.getInstance(),

            // common models 13.1
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.attributes.rev210924.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.node.types.rev210528.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.optical.channel.types.rev230526.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.phy.codes.rev220527.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.types.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.types.rev210924.YangModuleInfoImpl
            .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.types.rev220325.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.resource.rev230526.YangModuleInfoImpl.getInstance(),

            // device models 1.2.1 and 2.2.1
            org.opendaylight.yang.svc.v1.http.org.openroadm.database.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.database.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.device.resource.types.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.operations.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.operations.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.swdl.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.swdl.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.device.rev170206.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.device.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ethernet.interfaces.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ethernet.interfaces.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.file.transfer.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.file.transfer.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.fwdl.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.fwdl.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.interfaces.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.interfaces.rev170626.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.lldp.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.lldp.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.loopback.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.loopback.rev171215.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.testsignal.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.testsignal.rev171215.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.media.channel.interfaces.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.network.media.channel.interfaces.rev181019
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.channel.interfaces.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.channel.interfaces.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.multiplex.interfaces.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.transport.interfaces.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.transport.interfaces.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.rev170626.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.odu.interfaces.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.odu.interfaces.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.otu.interfaces.rev161014.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.otu.interfaces.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.physical.types.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.physical.types.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pluggable.optics.holder.capability.rev181019
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.port.capability.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.prot.otn.linear.aps.rev181019.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.rstp.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.rstp.rev181019.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.syslog.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.syslog.rev171215.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.wavelength.map.rev161014.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.wavelength.map.rev171215.YangModuleInfoImpl.getInstance(),

            // device models 7.1
            org.opendaylight.yang.svc.v1.http.org.openroadm.database.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.operations.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.device.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.device.types.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.dhcp.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ethernet.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.file.transfer.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.fwdl.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.gcc.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.gnmi.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ip.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ipv4.unicast.routing.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ipv6.unicast.routing.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.key.chain.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.lldp.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.loopback.rev191129.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.maintenance.testsignal.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.media.channel.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.network.media.channel.interfaces.rev200529
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.channel.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.operational.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.transport.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.optical.channel.tributary.signal.interfaces.rev200529
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.common.rev200327.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.odu.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.otu.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otsi.group.interfaces.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.physical.types.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.pluggable.optics.holder.capability.rev200529
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.port.capability.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.ppp.interfaces.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.prot.otn.linear.aps.rev200529.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.routing.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.rstp.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.security.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.de.swdl.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.syslog.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.telemetry.types.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.wavelength.map.rev191129.YangModuleInfoImpl.getInstance(),

            // network models
            org.opendaylight.yang.svc.v1.http.org.openroadm.amplifier.rev210924.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.clli.network.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.network.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.degree.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.external.pluggable.rev230526.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.link.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.network.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.network.topology.rev230526.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.network.types.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.otn.network.topology.rev230526.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.roadm.rev191129.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.srg.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.xponder.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev180226.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev180226.YangModuleInfoImpl
                    .getInstance(),

            // service models
            org.opendaylight.yang.svc.v1.http.org.openroadm.ber.test.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.ber.test.rev200529.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.common.service.types.rev230526.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.controller.customization.rev230526.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.operational.mode.catalog.rev230526.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.routing.constraints.rev221209.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.service.rev230526.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.openroadm.topology.rev230526.YangModuleInfoImpl.getInstance(),

            // tapi models
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.oam.rev221121.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.common.rev221121.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.digital.otn.rev221121.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.dsr.rev221121.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.photonic.media.rev221121.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.connectivity.rev221121.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.path.computation.rev221121.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.topology.rev221121.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.eth.rev221121.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.onf.otcc.yang.tapi.notification.rev221121.YangModuleInfoImpl
                    .getInstance(),

            // API models / opendaylight
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.alarmsuppression.rev171102
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.networkutils.rev220630.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.olm.rev210618.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.pce.rev240205.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.portmapping.rev231221.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.tapinetworkutils.rev230728
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.device.renderer.rev211004
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.networkmodel.rev201116
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.renderer.rev210915.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.servicehandler.rev201125.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.http.org.transportpce.common.types.rev220926.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.gnpy.gnpy.api.rev220221.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.gnpy.gnpy.eqpt.config.rev220221.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.gnpy.gnpy.network.topology.rev220615.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.gnpy.path.rev220615.YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.opendaylight.transportpce.portmapping.rev231221
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.transportpce.b.c._interface.pathdescription.rev230501
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.transportpce.b.c._interface.service.types.rev220118
                    .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.http.org.transportpce.b.c._interface.servicepath.rev171017.YangModuleInfoImpl
                    .getInstance(),

            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netmod.notification.rev080714.YangModuleInfoImpl
                .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.iana.afn.safi.rev130704.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.notification._1._0.rev080714.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.netconf.base._1._0.rev110601.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.monitoring.rev101004
                     .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.ietf.params.xml.ns.yang.ietf.netconf.notifications.rev120206
                     .YangModuleInfoImpl.getInstance(),
            org.opendaylight.yang.svc.v1.urn.opendaylight.params.xml.ns.yang.tapi.rev230728.YangModuleInfoImpl
                    .getInstance(),
            org.opendaylight.yang.svc.v1.nbi.notifications.rev230728.YangModuleInfoImpl
                    .getInstance());

    private static final Set<YangModuleInfo> TPCE_YANG_MODEL = Stream.concat(
            Stream.concat(
                    RestConfConfigUtils.YANG_MODELS.stream(),
                    NetconfConfigUtils.NETCONF_TOPOLOGY_MODELS.stream())
            .collect(Collectors.toSet()).stream(),
            TPCE_MODELS.stream()).collect(Collectors.toSet());

    public static Set<YangModuleInfo> getYangModels() {
        return TPCE_YANG_MODEL;
    }

    private TPCEUtils() {
        throw new UnsupportedOperationException("Please do not instantiate utility class.");
    }

}
