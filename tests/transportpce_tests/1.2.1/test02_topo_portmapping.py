#!/usr/bin/env python

##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods
# a pylint false positive due to unittest
# pylint: disable=no-self-use

import time
import unittest
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils_rfc8040  # nopep8


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '1.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils_rfc8040.start_tpce()
        cls.processes = test_utils_rfc8040.start_sims([('xpdra', cls.NODE_VERSION), ('roadma', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(10)

    # Connect the ROADMA
    def test_01_connect_rdm(self):
        response = test_utils_rfc8040.mount_device("ROADMA01", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    # Verify the termination points of the ROADMA
    def test_02_compare_Openroadm_topology_portmapping_rdm(self):
        responseTopo = test_utils_rfc8040.get_request(test_utils_rfc8040.URL_CONFIG_ORDM_TOPO)
        resTopo = responseTopo.json()
        firstEntry = resTopo['ietf-network:network'][0]['node']
        for i in range(0, len(firstEntry)):
            nodeId = firstEntry[i]['node-id']
            nodeMapId = nodeId.split("-")[0]
            response = test_utils_rfc8040.get_portmapping_node_info(nodeMapId)
            self.assertEqual(response['status_code'], requests.codes.ok)
            nbTp = len(firstEntry[i]['ietf-network-topology:termination-point'])
            for j in range(0, nbTp):
                tpId = firstEntry[i]['ietf-network-topology:termination-point'][j]['tp-id']
                if((not "CP" in tpId) and (not "CTP" in tpId)):
                    response2 = test_utils_rfc8040.portmapping_request(nodeMapId, tpId)
                    self.assertEqual(response2['status_code'], requests.codes.ok)

    # Disconnect the ROADMA
    def test_03_disconnect_rdm(self):
        response = test_utils_rfc8040.unmount_device("ROADMA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

#     #Connect the XPDRA
    def test_04_connect_xpdr(self):
        response = test_utils_rfc8040.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

#     #Verify the termination points related to XPDR
    def test_05_compare_Openroadm_topology_portmapping_xpdr(self):
        self.test_02_compare_Openroadm_topology_portmapping_rdm()

    # Disconnect the XPDRA
    def test_06_disconnect_device(self):
        response = test_utils_rfc8040.unmount_device("XPDRA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    #logging.debug('I am there')
    unittest.main(verbosity=2)
