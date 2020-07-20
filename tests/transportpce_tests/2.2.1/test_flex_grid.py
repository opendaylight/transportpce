#!/usr/bin/env python

##############################################################################
# Copyright (c) 2019 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import unittest
import time
import requests
from common import test_utils


class TransportPCEPortMappingTesting(unittest.TestCase):

    processes = None

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims(['roadmd'])

    @classmethod
    def tearDownClass(cls):
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

    def test_01_rdm_device_connection(self):
        response = test_utils.mount_device("ROADM-D1", 'roadmd')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_rdm_device_connected(self):
        response = test_utils.get_netconf_oper_request("ROADM-D1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')
        time.sleep(10)

    def test_03_rdm_portmapping_info(self):
        url = "{}/config/transportpce-portmapping:network/nodes/ROADM-D1/node-info"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'rdm',
                            u'node-ip-address': u'127.0.0.14',
                            u'node-clli': u'NodeD',
                            u'openroadm-version': u'2.2.1', u'node-vendor': u'vendorD',
                            u'node-model': u'model2',
                            u'grid-implementation': u'flex-grid',
                            u'slot-width-granularity': 12.5}},
            res)
        time.sleep(3)

    # TODO: Create DEG-MC interface
    @unittest.skip
    def test_04_degree_mc_interface(self):
        url = ""
        data = {
          "org-openroadm-device:interface": [
            {
              "name": "DEG1-TTP-RX-mc-0",
              "supporting-interface": "OMS-1-5-8",
              "supporting-circuit-pack-name": "1-5",
              "circuit-id": "TBD",
              "description": "TBD",
              "supporting-port": "8",
              "type": "org-openroadm-interfaces:mediaChannelTrailTerminationPoint",
              "org-openroadm-media-channel-interfaces:mc-ttp": {
                "min-freq": 196.00625,
                "max-freq": 196.09375
              }
            }
          ]
        }
        req = test_utils.put_request(url, data)
    # TODO: Create DEG-NMC interface
    # TODO: Create SRG-NMC interface
    # TODO: Create ROADM nmc interface
    # TODO: Renderer interface creations


if __name__ == "__main__":
    unittest.main(verbosity=2)
