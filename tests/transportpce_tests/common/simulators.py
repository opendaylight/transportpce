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
    ('mpdra', 'oc200'): {'port': '19840', 'configfile': 'sample-openconfig200-mpdr.xml', 'logfile': 'mpdra-200.log', 'restconf_baseurl': 'http://127.0.0.1:8159/restconf'},
    ('mpdrc', 'oc200'): {'port': '19844', 'configfile': 'sample-openconfig200-mpdr.xml', 'logfile': 'mpdrc-200.log', 'restconf_baseurl': 'http://127.0.0.1:8160/restconf'}
}
