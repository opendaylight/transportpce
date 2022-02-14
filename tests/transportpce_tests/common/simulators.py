#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

SIMS = {
    ('xpdra', '1.2.1'): {'port': '17830', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra-121.log'},
    ('roadma', '1.2.1'): {'port': '17831', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma-121.log'},
    ('roadmb', '1.2.1'): {'port': '17832', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb-121.log'},
    ('roadmc', '1.2.1'): {'port': '17833', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc-121.log'},
    ('xpdrc', '1.2.1'): {'port': '17834', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc-121.log'},
    ('roadma-full', '1.2.1'): {'port': '17821', 'configfile': 'oper-ROADMA-full.xml', 'logfile': 'roadma-121.log'},
    ('roadmc-full', '1.2.1'): {'port': '17823', 'configfile': 'oper-ROADMC-full.xml', 'logfile': 'roadmc-121.log'},
    ('xpdra', '2.2.1'): {'port': '17840', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra-221.log'},
    ('roadma', '2.2.1'): {'port': '17841', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma-221.log'},
    ('roadmb', '2.2.1'): {'port': '17842', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb-221.log'},
    ('roadmc', '2.2.1'): {'port': '17843', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc-221.log'},
    ('roadmd', '2.2.1'): {'port': '17847', 'configfile': 'oper-ROADMD.xml', 'logfile': 'roadmd-221.log'},
    ('xpdrc', '2.2.1'): {'port': '17844', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc-221.log'},
    ('spdra', '2.2.1'): {'port': '17845', 'configfile': 'oper-SPDRA.xml', 'logfile': 'spdra-221.log'},
    ('spdrc', '2.2.1'): {'port': '17846', 'configfile': 'oper-SPDRC.xml', 'logfile': 'spdrc-221.log'},
    ('spdrb', '2.2.1'): {'port': '17848', 'configfile': 'oper-SPDRB.xml', 'logfile': 'spdrb-221.log'},
    ('xpdra', '7.1'): {'port': '17850', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra-71.log'},
    ('roadma', '7.1'): {'port': '17851', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma-71.log'},
    ('roadmb', '7.1'): {'port': '17852', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb-71.log'},
    ('roadmc', '7.1'): {'port': '17853', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc-71.log'},
    ('xpdrc', '7.1'): {'port': '17854', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc-71.log'},
    ('xpdra2', '7.1'): {'port': '17857', 'configfile': 'oper-XPDRA2.xml', 'logfile': 'xpdra2-71.log'},
    ('xpdrc2', '7.1'): {'port': '17858', 'configfile': 'oper-XPDRC2.xml', 'logfile': 'xpdrc2-71.log'},
    ('CISCO-TPDR1-400G', '7.1'): {'port': '17859', 'configfile': 'CISCO-TPDR1-400G.xml', 'logfile': 'cisco-tpdr1-400G-71.log'},
    ('CISCO-TPDR2-400G', '7.1'): {'port': '17860', 'configfile': 'CISCO-TPDR2-400G.xml', 'logfile': 'cisco-tpdr2-400G-71.log'},
    ('FUJITSU-TPDR-400G', '7.1'): {'port': '17861', 'configfile': 'FUJITSU-TPDR-400G.xml', 'logfile': 'fuji-tpdr-400G-71.log'},
    ('INFINERA-TPDR-400G', '7.1'): {'port': '17862', 'configfile': 'INFINERA-TPDR-400G.xml', 'logfile': 'infinera-tpdr-400G-71.log'},
    ('CIENA-ROADMA', '2.2.1'): {'port': '17863', 'configfile': 'CIENA-ROADMA.xml', 'logfile': 'ciena-roadma-221.log'},
    ('FUJITSU-ROADMA', '2.2.1'): {'port': '17864', 'configfile': 'FUJITSU-ROADMA.xml', 'logfile': 'fuji-roadma-221.log'},
    ('FUJITSU-ROADMB', '2.2.1'): {'port': '17865', 'configfile': 'FUJITSU-ROADMB.xml', 'logfile': 'fuji-roadmb-221.log'},
    ('INFINERA-ROADMA', '2.2.1'): {'port': '17866', 'configfile': 'INFINERA-ROADMA.xml', 'logfile': 'fuji-roadma-221.log'},
    ('CIENA-TPDR', '2.2.1'): {'port': '17867', 'configfile': 'CIENA-TPDR.xml', 'logfile': 'ciena-tpdr-221.log'},
    ('ECI-SWPDR', '2.2.1'): {'port': '17868', 'configfile': 'ECI-SWPDR.xml', 'logfile': 'eci-swpdr-221.log'},
    ('NOKIA-SWPDR', '2.2.1'): {'port': '17869', 'configfile': 'NOKIA-SWPDR.xml', 'logfile': 'nokia-swpdr-221.log'},
    ('FUJITSU-TPDR', '2.2.1'): {'port': '17870', 'configfile': 'FUJITSU-TPDR.xml', 'logfile': 'fuji-tpdr-221.log'}




}
