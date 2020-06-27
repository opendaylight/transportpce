#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import os

SIMS = {
    'xpdra': {'port': '17840', 'configfile': 'oper-XPDRA.xml', 'logfile': 'oper-XPDRA.log'},
    'roadma': {'port': '17841', 'configfile': 'oper-ROADMA.xml', 'logfile': 'oper-ROADMA.log'},
    'roadmb': {'port': '17842', 'configfile': 'oper-ROADMB.xml', 'logfile': 'oper-ROADMB.log'},
    'roadmc': {'port': '17843', 'configfile': 'oper-ROADMC.xml', 'logfile': 'oper-ROADMC.log'},
    'xpdrc': {'port': '17844', 'configfile': 'oper-XPDRC.xml', 'logfile': 'oper-XPDRC.log'},
    'spdra': {'port': '17845', 'configfile': 'oper-SPDRA.xml', 'logfile': 'oper-SPDRA.log'},
    'spdra_no_interface': {'port': '17825', 'configfile': 'oper-SPDRA_no_interface.xml', 'logfile': 'oper-SPDRA.log'}
}

HONEYNODE_EXECUTABLE = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "2.2.1", "honeynode-simulator", "honeycomb-tpce")
SAMPLES_DIRECTORY = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "2.2.1")
