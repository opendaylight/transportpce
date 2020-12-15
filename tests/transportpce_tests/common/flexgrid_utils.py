#!/usr/bin/env python

##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member

import base64

def check_freq_map(freq_map):
    freq_map_array = [int(x) for x in freq_map]
    return freq_map_array[0] == 255 and freq_map_array[1] == 255


def set_used_index_for_freq_map(freq_map, index):
    freq_map[index] = 0
    return freq_map


INDEX_1_USED_FREQ_MAP = base64.b64encode(set_used_index_for_freq_map(bytearray(b'\xFF' * 96), 0)).decode('UTF-8')

INDEX_1_2_USED_FREQ_MAP = base64.b64encode(set_used_index_for_freq_map(
    set_used_index_for_freq_map(bytearray(b'\xFF' * 96), 0), 1)).decode('utf-8')

AVAILABLE_FREQ_MAP = base64.b64encode(bytearray(b'\xFF' * 96)).decode('UTF-8')
