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

import time
import unittest
import requests
from common import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims(['xpdra', 'roadma'])

    @classmethod
    def tearDownClass(cls):
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(10)

    # Connect the ROADMA
    def test_01_connect_rdm(self):
        response = test_utils.mount_device("ROADMA01", 'roadma')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    # Verify the termination points of the ROADMA
    def test_02_compareOpenroadmTopologyPortMapping_rdm(self):
        responseTopo = test_utils.get_ordm_topo_request("")
        resTopo = responseTopo.json()
        nbNode = len(resTopo['network'][0]['node'])
        nbMapCumul = 0
        nbMappings = 0
        for i in range(0, nbNode):
            nodeId = resTopo['network'][0]['node'][i]['node-id']
            nodeMapId = nodeId.split("-")[0]
            responseMapList = test_utils.portmapping_request(nodeMapId)
            resMapList = responseMapList.json()
            nbMappings = len(resMapList['nodes'][0]['mapping']) - nbMapCumul
            nbTp = len(resTopo['network'][0]['node'][i]['ietf-network-topology:termination-point'])
            nbMapCurrent = 0
            for j in range(0, nbTp):
                tpId = resTopo['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                if((not "CP" in tpId) and (not "CTP" in tpId)):
                    responseMap = test_utils.portmapping_request(nodeMapId+"/mapping/"+tpId)

    # Disconnect the ROADMA
    def test_03_disconnect_rdm(self):
        response = test_utils.unmount_device("ROADMA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

#     #Connect the XPDRA
    def test_04_connect_xpdr(self):
        response = test_utils.mount_device("XPDRA01", 'xpdra')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

#     #Verify the termination points related to XPDR
    def test_05_compareOpenroadmTopologyPortMapping_xpdr(self):
        self.test_02_compareOpenroadmTopologyPortMapping_rdm()

    # Disconnect the XPDRA
    def test_06_disconnect_device(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    #logging.debug('I am there')
    unittest.main(verbosity=2)
