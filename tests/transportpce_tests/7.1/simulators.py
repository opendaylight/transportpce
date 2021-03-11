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

SIMS = {}

HONEYNODE_EXECUTABLE = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "7.1", "honeynode-simulator", "honeycomb-tpce")
SAMPLES_DIRECTORY = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "7.1")
