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

import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION), ('roadma', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    # Connect the ROADMA
    def test_01_connect_rdm(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    # Verify the termination points of the ROADMA
    def test_02_compareOpenroadmTopologyPortMapping_rdm(self):
        resTopo = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(resTopo['status_code'], requests.codes.ok)
        nbMapCumul = 0
        nbMappings = 0
        for node in resTopo['network'][0]['node']:
            nodeId = node['node-id']
            # pylint: disable=consider-using-f-string
            print("nodeId={}".format(nodeId))
            nodeMapId = nodeId.split("-")[0] + "-" + nodeId.split("-")[1]
            if nodeMapId == 'TAPI-SBI':
                continue
            print("nodeMapId={}".format(nodeMapId))
            response = test_utils.get_portmapping_node_attr(nodeMapId, "node-info", None)
            self.assertEqual(response['status_code'], requests.codes.ok)
            responseMapList = test_utils.get_portmapping_node_attr(nodeMapId, None, None)
            nbMappings = len(responseMapList['nodes'][0]['mapping']) - nbMapCumul
            nbMapCurrent = 0
            for tp in node['ietf-network-topology:termination-point']:
                tpId = tp['tp-id']
                if (not "CP" in tpId) and (not "CTP" in tpId):
                    responseMap = test_utils.get_portmapping_node_attr(nodeMapId, "mapping", tpId)
                    self.assertEqual(responseMap['status_code'], requests.codes.ok)
                    if responseMap['status_code'] == requests.codes.ok:
                        nbMapCurrent += 1
            nbMapCumul += nbMapCurrent
        nbMappings -= nbMapCurrent
        self.assertEqual(nbMappings, 0)

    # Disconnect the ROADMA
    def test_03_disconnect_rdm(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

#     #Connect the XPDRA
    def test_04_connect_xpdr(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

#     #Verify the termination points related to XPDR
    def test_05_compareOpenroadmTopologyPortMapping_xpdr(self):
        self.test_02_compareOpenroadmTopologyPortMapping_rdm()

    # Disconnect the XPDRA
    def test_06_disconnect_device(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    unittest.main(verbosity=2)
