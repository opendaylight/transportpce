#!/usr/bin/env python

##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import json
import os
import psutil
import requests
import signal
import shutil
import subprocess
import time
import unittest
import logging
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
        response = test_utils.mount_device("ROADM-A1", 'roadma')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    # Verify the termination points of the ROADMA
    def test_02_compareOpenroadmTopologyPortMapping_rdm(self):
        urlTopo = ("{}/config/ietf-network:networks/network/openroadm-topology"
                   .format(test_utils.RESTCONF_BASE_URL))
        responseTopo = requests.request(
            "GET", urlTopo, headers=test_utils.TYPE_APPLICATION_JSON, auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        resTopo = responseTopo.json()
        nbNode = len(resTopo['network'][0]['node'])
        nbMapCumul = 0
        nbMappings = 0
        for i in range(0, nbNode):
            nodeId = resTopo['network'][0]['node'][i]['node-id']
            print("nodeId={}".format(nodeId))
            nodeMapId = nodeId.split("-")[0] + "-" + nodeId.split("-")[1]
            print("nodeMapId={}".format(nodeMapId))
            urlMapList = "{}/config/transportpce-portmapping:network/nodes/" + nodeMapId
            urlMapListFull = urlMapList.format(test_utils.RESTCONF_BASE_URL)
            responseMapList = requests.request(
                "GET", urlMapListFull, headers=test_utils.TYPE_APPLICATION_JSON, auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
            resMapList = responseMapList.json()

            nbMappings = len(resMapList['nodes'][0]['mapping']) - nbMapCumul
            nbTp = len(resTopo['network'][0]['node'][i]['ietf-network-topology:termination-point'])
            nbMapCurrent = 0
            for j in range(0, nbTp):
                tpId = resTopo['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                if((not "CP" in tpId) and (not "CTP" in tpId)):
                    urlMap = "{}/config/transportpce-portmapping:network/nodes/" + nodeMapId + "/mapping/" + tpId
                    urlMapFull = urlMap.format(test_utils.RESTCONF_BASE_URL)
                    responseMap = requests.request(
                        "GET", urlMapFull, headers=test_utils.TYPE_APPLICATION_JSON, auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
                    self.assertEqual(responseMap.status_code, requests.codes.ok)
                    if(responseMap.status_code == requests.codes.ok):
                        nbMapCurrent += 1
            nbMapCumul += nbMapCurrent
        nbMappings -= nbMapCurrent
        self.assertEqual(nbMappings, 0)

    # Disconnect the ROADMA
    def test_03_disconnect_rdm(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(test_utils.RESTCONF_BASE_URL))
        data = {}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)

#     #Connect the XPDRA
    def test_04_connect_xpdr(self):
        response = test_utils.mount_device("XPDR-A1", 'xpdra')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

#     #Verify the termination points related to XPDR
    def test_05_compareOpenroadmTopologyPortMapping_xpdr(self):
        self.test_02_compareOpenroadmTopologyPortMapping_rdm()

    # Disconnect the XPDRA
    def test_06_disconnect_device(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(test_utils.RESTCONF_BASE_URL))
        data = {}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)


if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    #logging.debug('I am there')
    unittest.main(verbosity=2)
