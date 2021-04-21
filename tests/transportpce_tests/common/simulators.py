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
    ('xpdra', '1.2.1'): {'port': '17830', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra121.log'},
    ('roadma', '1.2.1'): {'port': '17831', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma121.log'},
    ('roadmb', '1.2.1'): {'port': '17832', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb121.log'},
    ('roadmc', '1.2.1'): {'port': '17833', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc121.log'},
    ('xpdrc', '1.2.1'): {'port': '17834', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc121.log'},
    ('roadma-full', '1.2.1'): {'port': '17821', 'configfile': 'oper-ROADMA-full.xml', 'logfile': 'roadma121.log'},
    ('roadmc-full', '1.2.1'): {'port': '17823', 'configfile': 'oper-ROADMC-full.xml', 'logfile': 'roadmc121.log'},
    ('xpdra', '2.2.1'): {'port': '17840', 'configfile': 'oper-XPDRA.xml', 'logfile': 'xpdra221.log'},
    ('roadma', '2.2.1'): {'port': '17841', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma221.log'},
    ('roadmb', '2.2.1'): {'port': '17842', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb221.log'},
    ('roadmc', '2.2.1'): {'port': '17843', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc221.log'},
    ('roadmd', '2.2.1'): {'port': '17847', 'configfile': 'oper-ROADMD.xml', 'logfile': 'roadmd221.log'},
    ('xpdrc', '2.2.1'): {'port': '17844', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc221.log'},
    ('spdra', '2.2.1'): {'port': '17845', 'configfile': 'oper-SPDRA.xml', 'logfile': 'spdra221.log'},
    ('spdrc', '2.2.1'): {'port': '17846', 'configfile': 'oper-SPDRC.xml', 'logfile': 'spdrc221.log'},
    ('xpdra', '7.1.0'): {'port': '17850', 'configfile': 'oper-XPDRA2.xml', 'logfile': 'xpdra71.log'},
    ('roadma', '7.1.0'): {'port': '17851', 'configfile': 'oper-ROADMA.xml', 'logfile': 'roadma71.log'},
    ('roadmb', '7.1.0'): {'port': '17852', 'configfile': 'oper-ROADMB.xml', 'logfile': 'roadmb71.log'},
    ('roadmc', '7.1.0'): {'port': '17853', 'configfile': 'oper-ROADMC.xml', 'logfile': 'roadmc71.log'},
    ('xpdrc', '7.1.0'): {'port': '17854', 'configfile': 'oper-XPDRC.xml', 'logfile': 'xpdrc71.log'}
}