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

sims = {
    'xpdra': {'port': '17830', 'configfile': 'oper-XPDRA.xml', 'logfile': 'oper-XPDRA.log'},
    'roadma': {'port': '17831', 'configfile': 'oper-ROADMA.xml', 'logfile': 'oper-ROADMA.log'},
    'roadmb': {'port': '17832', 'configfile': 'oper-ROADMB.xml', 'logfile': 'oper-ROADMB.log'},
    'roadmc': {'port': '17833', 'configfile': 'oper-ROADMC.xml', 'logfile': 'oper-ROADMC.log'},
    'xpdrc': {'port': '17834', 'configfile': 'oper-XPDRC.xml', 'logfile': 'oper-XPDRC.log'},
    'roadma-full': {'port': '17821', 'configfile': 'oper-ROADMA-full.xml', 'logfile': 'oper-ROADMA.log'},
    'roadmc-full': {'port': '17823', 'configfile': 'oper-ROADMC-full.xml', 'logfile': 'oper-ROADMC.log'}
}

honeynode_executable = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "1.2.1", "honeynode-simulator", "honeycomb-tpce")
samples_directory = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "1.2.1")
