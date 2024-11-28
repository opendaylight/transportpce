#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=line-too-long
SIMS = {
    ('xpdra', '1.2.1'): {'port': '17830', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra-121.log', 'restconf_baseurl': 'http://127.0.0.1:8130/restconf'},
    ('roadma', '1.2.1'): {'port': '17831', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma-121.log', 'restconf_baseurl': 'http://127.0.0.1:8131/restconf'},
    ('roadmb', '1.2.1'): {'port': '17832', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb-121.log', 'restconf_baseurl': 'http://127.0.0.1:8132/restconf'},
    ('roadmc', '1.2.1'): {'port': '17833', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc-121.log', 'restconf_baseurl': 'http://127.0.0.1:8133/restconf'},
    ('xpdrc', '1.2.1'): {'port': '17834', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc-121.log', 'restconf_baseurl': 'http://127.0.0.1:8134/restconf'},
    ('roadma-full', '1.2.1'): {'port': '17821', 'configfile': 'oper-ROADMA-full.xml', 'logfile': 'roadma-121.log', 'restconf_baseurl': 'http://127.0.0.1:8121/restconf'},
    ('roadmc-full', '1.2.1'): {'port': '17823', 'configfile': 'oper-ROADMC-full.xml', 'logfile': 'roadmc-121.log', 'restconf_baseurl': 'http://127.0.0.1:8123/restconf'},
    ('xpdra', '2.2.1'): {'port': '17840', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra-221.log', 'restconf_baseurl': 'http://127.0.0.1:8140/restconf'},
    ('roadma', '2.2.1'): {'port': '17841', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma-221.log', 'restconf_baseurl': 'http://127.0.0.1:8141/restconf'},
    ('roadmb', '2.2.1'): {'port': '17842', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb-221.log', 'restconf_baseurl': 'http://127.0.0.1:8142/restconf'},
    ('roadmc', '2.2.1'): {'port': '17843', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc-221.log', 'restconf_baseurl': 'http://127.0.0.1:8143/restconf'},
    ('roadmd', '2.2.1'): {'port': '17847', 'configfile': 'oper-ROADMD.xml', 'logfile': 'roadmd-221.log', 'restconf_baseurl': 'http://127.0.0.1:8147/restconf'},
    ('xpdrc', '2.2.1'): {'port': '17844', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc-221.log', 'restconf_baseurl': 'http://127.0.0.1:8144/restconf'},
    ('spdra', '2.2.1'): {'port': '17845', 'configfile': 'oper-SPDRA.xml', 'logfile': 'spdra-221.log', 'restconf_baseurl': 'http://127.0.0.1:8145/restconf'},
    ('spdrc', '2.2.1'): {'port': '17846', 'configfile': 'oper-SPDRC.xml', 'logfile': 'spdrc-221.log', 'restconf_baseurl': 'http://127.0.0.1:8146/restconf'},
    ('spdrb', '2.2.1'): {'port': '17848', 'configfile': 'oper-SPDRB.xml', 'logfile': 'spdrb-221.log', 'restconf_baseurl': 'http://127.0.0.1:8148/restconf'},
    ('xpdra', '7.1'): {'port': '17850', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra-71.log', 'restconf_baseurl': 'http://127.0.0.1:8150/restconf'},
    ('roadma', '7.1'): {'port': '17851', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma-71.log', 'restconf_baseurl': 'http://127.0.0.1:8151/restconf'},
    ('roadmb', '7.1'): {'port': '17852', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb-71.log', 'restconf_baseurl': 'http://127.0.0.1:8152/restconf'},
    ('roadmc', '7.1'): {'port': '17853', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc-71.log', 'restconf_baseurl': 'http://127.0.0.1:8153/restconf'},
    ('xpdrc', '7.1'): {'port': '17854', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc-71.log', 'restconf_baseurl': 'http://127.0.0.1:8154/restconf'},
    ('xpdra2', '7.1'): {'port': '17857', 'configfile': 'oper-XPDRA2.xml', 'logfile': 'xpdra2-71.log', 'restconf_baseurl': 'http://127.0.0.1:8157/restconf'},
    ('xpdrc2', '7.1'): {'port': '17858', 'configfile': 'oper-XPDRC2.xml', 'logfile': 'xpdrc2-71.log', 'restconf_baseurl': 'http://127.0.0.1:8158/restconf'},
    ('sample-openconfig-mpdr', 'oc'): {'port': '19840', 'configfile': 'sample-openconfig-mpdr.xml', 'logfile': 'sample-openconfig-mpdr.log', 'restconf_baseurl': 'http://127.0.0.1:8159/restconf'}
}
