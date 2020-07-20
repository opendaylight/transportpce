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
        response = test_utils.portmapping_request("ROADM-D1/node-info")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'rdm',
                            u'node-ip-address': u'127.0.0.14',
                            u'node-clli': u'NodeD',
                            u'openroadm-version': u'2.2.1', u'node-vendor': u'vendorD',
                            u'node-model': u'model2',
                            }},
            res)
        time.sleep(3)

    def test_04_rdm_deg1_lcp(self):
        response = test_utils.portmapping_request("ROADM-D1/mc-capabilities/DEG1-TTP")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {
              "mc-capabilities": [
                {
                  "mc-node-name": "DEG1-TTP",
                  "center-freq-granularity": 6.25,
                  "slot-width-granularity": 12.5
                }
              ]
            }, res)
        time.sleep(3)

    def test_05_rdm_deg2_lcp(self):
        response = test_utils.portmapping_request("ROADM-D1/mc-capabilities/DEG2-TTP")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {
              "mc-capabilities": [
                {
                  "mc-node-name": "DEG2-TTP",
                  "center-freq-granularity": 6.25,
                  "slot-width-granularity": 12.5
                }
              ]
            }, res)
        time.sleep(3)

    def test_06_rdm_srg1_lcp(self):
        response = test_utils.portmapping_request("ROADM-D1/mc-capabilities/SRG1-PP")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {
              "mc-capabilities": [
                {
                  "mc-node-name": "SRG1-PP",
                  "center-freq-granularity": 6.25,
                  "slot-width-granularity": 12.5
                }
              ]
            }, res)
        time.sleep(3)

    # Create Degree MC interface
    def test_07_degree_mc_interface(self):
        data = {
          "org-openroadm-device:interface": [
            {
              "name": "DEG1-TTP-TXRX-mc-1",
              "supporting-interface": "OMS-DEG1-TTP-TXRX",
              "supporting-circuit-pack-name": "1/0",
              "circuit-id": "TBD",
              "description": "TBD",
              "supporting-port": "L1",
              "type": "org-openroadm-interfaces:mediaChannelTrailTerminationPoint",
              "org-openroadm-media-channel-interfaces:mc-ttp": {
                "min-freq": 196.00625,
                "max-freq": 196.09375
              }
            }
          ]
        }
        response = test_utils.put_request(test_utils.URL_CONFIG_NETCONF_TOPO +
                                     "node/ROADM-D1/yang-ext:mount/" +
                                     "org-openroadm-device:org-openroadm-device/" +
                                     "interface/DEG1-TTP-TXRX-mc-1", data)

        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(3)

    # Create DEG-NMC interface
    def test_08_degree_nmc_interface(self):
        data = {
          "org-openroadm-device:interface": [
            {
              "name": "DEG1-TTP-TXRX-nmc-1",
              "supporting-interface": "DEG1-TTP-TXRX-mc-1",
              "org-openroadm-network-media-channel-interfaces:nmc-ctp": {
                "frequency": 196.05,
                "width": 75
              },
              "supporting-circuit-pack-name": "1/0",
              "circuit-id": "TBD",
              "description": "TBD",
              "supporting-port": "L1",
              "type": "org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint"
            }
          ]
        }
        response = test_utils.put_request(test_utils.URL_CONFIG_NETCONF_TOPO +
                                     "node/ROADM-D1/yang-ext:mount/" +
                                     "org-openroadm-device:org-openroadm-device/" +
                                     "interface/DEG1-TTP-TXRX-nmc-1", data)
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(3)

    # Create SRG-NMC interface
    def test_09_degree_nmc_interface(self):
        data = {
          "org-openroadm-device:interface": [
            {
              "name": "SRG1-PP1-TXRX-nmc-1",
              "org-openroadm-network-media-channel-interfaces:nmc-ctp": {
                "frequency": 196.05,
                "width": 75
              },
              "supporting-circuit-pack-name": "3/0",
              "circuit-id": "TBD",
              "description": "TBD",
              "supporting-port": "C1",
              "type": "org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint"
            }
          ]
        }
        response = test_utils.put_request(test_utils.URL_CONFIG_NETCONF_TOPO +
                                     "node/ROADM-D1/yang-ext:mount/" +
                                     "org-openroadm-device:org-openroadm-device/" +
                                     "interface/SRG1-PP1-TXRX-nmc-1", data)
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(3)

    # Create ROADM-connection
    def test_10_roadm_connection(self):
        data = {
          "org-openroadm-device:roadm-connections": [
            {
              "connection-name": "DEG1-TTP-TXRX-SRG1-PP1-TXRX-nmc-1",
              "destination": {
                "dst-if": "SRG1-PP1-TXRX-nmc-1"
              },
              "opticalControlMode": "off",
              "source": {
                "src-if": "DEG1-TTP-TXRX-nmc-1"
              }
            }
          ]
        }
        response = test_utils.put_request(test_utils.URL_CONFIG_NETCONF_TOPO +
                                     "node/ROADM-D1/yang-ext:mount/" +
                                     "org-openroadm-device:org-openroadm-device/" +
                                     "roadm-connections/" +
                                     "DEG1-TTP-TXRX-SRG1-PP1-TXRX-nmc-1", data)
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(3)

    # TODO: Delete ROADM connection

    # Renderer interface creations
    def test_11_device_renderer(self):
        data = {
            "transportpce-device-renderer:input": {
            "transportpce-device-renderer:modulation-format": "dp-qpsk",
            "transportpce-device-renderer:operation": "create",
            "transportpce-device-renderer:service-name": "testNMC-MC",
            "transportpce-device-renderer:wave-number": "2",
            "transportpce-device-renderer:center-freq": "196.05",
            "transportpce-device-renderer:slot-width": "87.5",
            "transportpce-device-renderer:nodes": [
              {
                "transportpce-device-renderer:node-id": "ROADM-D1",
                "transportpce-device-renderer:src-tp": "SRG1-PP2-TXRX",
                "transportpce-device-renderer:dest-tp": "DEG2-TTP-TXRX"
              }
            ]
          }
        }
        url = test_utils.RESTCONF_BASE_URL + \
              "/operations/transportpce-device-renderer:service-path"
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes',
                      res['output']['result'])
        time.sleep(10)


if __name__ == "__main__":
    unittest.main(verbosity=2)
