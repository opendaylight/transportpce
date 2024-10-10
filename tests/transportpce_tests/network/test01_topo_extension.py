#!/usr/bin/env python

##############################################################################
# Copyright (c) 2024 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import os
import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
from keyring.backends import null
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


# pylint: disable=too-few-public-methods
class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class TransportPCEtesting(unittest.TestCase):

    processes = []
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'
    uuid_services = UuidServices()
    CHECK_DICT_TAPINODES = {
        'node-id': 'TAPI-SBI-ABS-NODE',
        'supporting-node': [
            {
                'network-ref': 'openroadm-network',
                'node-ref': 'TAPI-SBI-ABS-NODE'
            }
        ],
        'transportpce-or-network-augmentation:yang-data-model': 'tapi-ext',
        'transportpce-or-network-augmentation:topology-uuid': 'Uuid{value=a21e4756-4d70-3d40-95b6-f7f630b4a13b}',
        'ietf-network-topology:termination-point': [
            {
                'tp-id': 'ROADM-TA1-SRG1-PP1-TXRX',
                'transportpce-or-network-augmentation:supporting-node-uuid': 'f929e2dc-3c08-32c3-985f-c126023efc43',
                'org-openroadm-common-network:operational-state': 'inService',
                'transportpce-or-network-augmentation:tp-uuid': '3c3c3679-ccd7-3343-9f36-bdb7bea11a84',
                'transportpce-or-network-augmentation:supporting-node-topology-uuid':
                    'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                'org-openroadm-common-network:administrative-state': 'inService'
            },
            {
                'tp-id': 'ROADM-TA1-DEG2-TTP-TXRX',
                'transportpce-or-network-augmentation:supporting-node-uuid': 'f929e2dc-3c08-32c3-985f-c126023efc43',
                'org-openroadm-common-network:operational-state': 'inService',
                'transportpce-or-network-augmentation:tp-uuid': 'd42ed13c-d81f-3136-a7d8-b283681031d4',
                'transportpce-or-network-augmentation:supporting-node-topology-uuid':
                    'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                'org-openroadm-common-network:administrative-state': 'inService'
            },
            {
                'tp-id': 'ROADM-TC1-SRG1-PP1-TXRX',
                'transportpce-or-network-augmentation:supporting-node-uuid': '7a44ea23-90d1-357d-8754-6e88d404b670',
                'org-openroadm-common-network:operational-state': 'inService',
                'transportpce-or-network-augmentation:tp-uuid': 'e5a9d17d-40cd-3733-b736-cc787a876195',
                'transportpce-or-network-augmentation:supporting-node-topology-uuid':
                    'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                'org-openroadm-common-network:administrative-state': 'inService'
            },
            {
                'tp-id': 'ROADM-TC1-DEG1-TTP-TXRX',
                'transportpce-or-network-augmentation:supporting-node-uuid': '7a44ea23-90d1-357d-8754-6e88d404b670',
                'org-openroadm-common-network:operational-state': 'inService',
                'transportpce-or-network-augmentation:tp-uuid': 'fb3a00c1-342f-3cdc-b83d-2c257de298c1',
                'transportpce-or-network-augmentation:supporting-node-topology-uuid':
                    'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                'org-openroadm-common-network:administrative-state': 'inService'
            }
        ]
    }
    CHECK_DICT_INTERDOMAIN_LINK_8 = {
        'link-id': 'TAPI-SBI-ABS-NODE-ROADM-TC1-DEG1-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX',
        'source': {
            'source-node': 'TAPI-SBI-ABS-NODE',
            'source-tp': 'ROADM-TC1-DEG1-TTP-TXRX'
        },
        'org-openroadm-common-network:operational-state': 'inService',
        'org-openroadm-common-network:link-type': 'ROADM-TO-ROADM',
        'org-openroadm-common-network:administrative-state': 'inService',
        'destination': {
            'dest-tp': 'DEG2-TTP-TXRX',
            'dest-node': 'ROADM-C1-DEG2'
        },
        'org-openroadm-common-network:opposite-link':
            'ROADM-C1-DEG2-DEG2-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TC1-DEG1-TTP-TXRX',
        'transportpce-or-network-augmentation:link-class': 'inter-domain'
    }

    CHECK_DICT_ALIEN_TO_TAPI_LINK_14 = {
        'link-id': 'SPDR-SC1-XPDR1-XPDR1-NETWORK1toTAPI-SBI-ABS-NODE-ROADM-TC1-SRG1-PP1-TXRX',
        'source': {
            'source-node': 'SPDR-SC1-XPDR1',
            'source-tp': 'XPDR1-NETWORK1'
        },
        'org-openroadm-common-network:operational-state': 'inService',
        'org-openroadm-common-network:link-type': 'XPONDER-OUTPUT',
        'org-openroadm-common-network:administrative-state': 'inService',
        'destination': {
            'dest-tp': 'ROADM-TC1-SRG1-PP1-TXRX',
            'dest-node': 'TAPI-SBI-ABS-NODE'
        },
        'org-openroadm-common-network:opposite-link':
            'ROADM-TC1-ROADM-TC1-SRG1-PP1-TXRXtoSPDR-SC1-XPDR1-XPDR1-NETWORK1',
        'transportpce-or-network-augmentation:link-class': 'alien-to-tapi'
    }

    del_serv_input_data = {'uuid': 'TBD'}

    tapi_topo = {'topology-id': 'TBD'}

    @classmethod
    def setUpClass(cls):
        # pylint: disable=unsubscriptable-object
        cls.init_failed = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4096M'
        cls.processes = test_utils.start_tpce()
        # TAPI feature is not installed by default in Karaf
        if 'NO_ODL_STARTUP' not in os.environ or 'USE_LIGHTY' not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print('installing tapi feature...')
            result = test_utils.install_karaf_feature('odl-transportpce-tapi')
            if result.returncode != 0:
                cls.init_failed = True
            print('Restarting OpenDaylight...')
            test_utils.shutdown_process(cls.processes[0])
            cls.processes[0] = test_utils.start_karaf()
            test_utils.process_list[0] = cls.processes[0]
            cls.init_failed = not test_utils.wait_until_log_contains(
                test_utils.KARAF_LOG, test_utils.KARAF_OK_START_MSG, time_to_wait=60)
        if cls.init_failed:
            print('tapi installation feature failed...')
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print('all processes killed')

    def setUp(self):
        time.sleep(2)

    def test_01_connect_spdrA(self):
        print('Connecting SPDRA')
        response = test_utils.mount_device('SPDR-SA1', ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_02_connect_spdrC(self):
        print('Connecting SPDRC')
        response = test_utils.mount_device('SPDR-SC1', ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_03_connect_rdmA(self):
        print('Connecting ROADMA')
        response = test_utils.mount_device('ROADM-A1', ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_04_connect_rdmC(self):
        print('Connecting ROADMC')
        response = test_utils.mount_device('ROADM-C1', ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_05_connect_sprdA_1_N1_to_TAPI_EXT_roadmTA1_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-TA1', 'termination-point-num' : 'SRG1-PP1-TXRX',
                             'rdm-topology-uuid': 'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                             'rdm-nep-uuid': '3c3c3679-ccd7-3343-9f36-bdb7bea11a84',
                             'rdm-node-uuid': 'f929e2dc-3c08-32c3-985f-c126023efc43'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response['output']['result'])
        time.sleep(2)

    def test_06_connect_TAPI_EXT_roadmTA1_PP1_to_spdrA_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-TA1', 'termination-point-num' : 'SRG1-PP1-TXRX',
                             'rdm-topology-uuid': 'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                             'rdm-nep-uuid': '3c3c3679-ccd7-3343-9f36-bdb7bea11a84',
                             'rdm-node-uuid': 'f929e2dc-3c08-32c3-985f-c126023efc43'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response['output']['result'])
        time.sleep(2)

    def test_07_connect_sprdC_1_N1_to_TAPI_EXT_roadmTC1_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-TC1', 'termination-point-num' : 'SRG1-PP1-TXRX',
                             'rdm-topology-uuid': 'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                             'rdm-nep-uuid': 'e5a9d17d-40cd-3733-b736-cc787a876195',
                             'rdm-node-uuid': '7a44ea23-90d1-357d-8754-6e88d404b670'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response['output']['result'])
        time.sleep(2)

    def test_08_connect_TAPI_EXT_roadmTC1_PP1_to_spdrC_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-TC1', 'termination-point-num' : 'SRG1-PP1-TXRX',
                             'rdm-topology-uuid': 'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                             'rdm-nep-uuid': 'e5a9d17d-40cd-3733-b736-cc787a876195',
                             'rdm-node-uuid': '7a44ea23-90d1-357d-8754-6e88d404b670'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response['output']['result'])
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertIn(self.CHECK_DICT_ALIEN_TO_TAPI_LINK_14, response['network'][0]['ietf-network-topology:link'])
        time.sleep(2)

    def test_09_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 7, 'There should be 7 otn nodes including TAPI-EXT')
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_10_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 14, 'There should be 14 OR nodes including TAPI-EXT')
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 26,
                         'There should be 26 openroadm links')

    def test_11_connect_RDMA1_to_TAPI_EXT_roadmTA1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-inter-domain-links',
            {'a-end': {'rdm-node': 'ROADM-A1', 'deg-num': '1', 'termination-point': 'DEG1-TTP-TXRX'},
             'z-end': {'rdm-node': 'ROADM-TA1', 'deg-num': '2', 'termination-point': 'DEG2-TTP-TXRX',
                             'rdm-topology-uuid': 'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                             'rdm-nep-uuid': 'd42ed13c-d81f-3136-a7d8-b283681031d4',
                             'rdm-node-uuid': 'f929e2dc-3c08-32c3-985f-c126023efc43'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        print(response['output']['result'])
        time.sleep(2)

    def test_12_connect_RDMC1_to_TAPI_EXT_roadmTC1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-inter-domain-links',
            {'a-end': {'rdm-node': 'ROADM-C1', 'deg-num': '2', 'termination-point': 'DEG2-TTP-TXRX'},
             'z-end': {'rdm-node': 'ROADM-TC1', 'deg-num': '1', 'termination-point': 'DEG1-TTP-TXRX',
                             'rdm-topology-uuid': 'a21e4756-4d70-3d40-95b6-f7f630b4a13b',
                             'rdm-nep-uuid': 'fb3a00c1-342f-3cdc-b83d-2c257de298c1',
                             'rdm-node-uuid': '7a44ea23-90d1-357d-8754-6e88d404b670'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        print(response['output']['result'])
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertIn(self.CHECK_DICT_INTERDOMAIN_LINK_8, response['network'][0]['ietf-network-topology:link'])
        time.sleep(2)

    def test_13_getLinks_OpenroadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        # Tests related to links
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 26,
                         'There should be 26 openroadm links')
        check_list = {'TAPI-SBI-ABS-NODE-ROADM-TC1-DEG1-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX',
                      'TAPI-SBI-ABS-NODE-ROADM-TA1-DEG2-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX',
                      'TAPI-SBI-ABS-NODE-ROADM-TC1-SRG1-PP1-TXRXtoSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                      'TAPI-SBI-ABS-NODE-ROADM-TA1-SRG1-PP1-TXRXtoSPDR-SA1-XPDR1-XPDR1-NETWORK1',
                      'SPDR-SC1-XPDR1-XPDR1-NETWORK1toTAPI-SBI-ABS-NODE-ROADM-TC1-SRG1-PP1-TXRX',
                      'SPDR-SA1-XPDR1-XPDR1-NETWORK1toTAPI-SBI-ABS-NODE-ROADM-TA1-SRG1-PP1-TXRX',
                      'ROADM-C1-DEG2-DEG2-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TC1-DEG1-TTP-TXRX',
                      'ROADM-A1-DEG1-DEG1-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TA1-DEG2-TTP-TXRX'}
        interDomainLinkNber = 0
        alienToTapiLinkNber = 0
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            linkType = link['org-openroadm-common-network:link-type']
            if 'transportpce-or-network-augmentation:link-class' in link.keys():
                linkClass = link['transportpce-or-network-augmentation:link-class']
                if (linkType == 'ROADM-TO-ROADM' and linkClass == 'inter-domain') :
                    find = linkId in check_list
                    self.assertEqual(find, True)
                    interDomainLinkNber += 1
                if (linkType == 'XPONDER-OUTPUT' and linkClass == 'alien-to-tapi') :
                    find = linkId in check_list
                    self.assertEqual(find, True)
                    alienToTapiLinkNber += 1
                if (linkType == 'XPONDER-INPUT' and linkClass == 'alien-to-tapi') :
                    find = linkId in check_list
                    self.assertEqual(find, True)
                    alienToTapiLinkNber += 1
        self.assertEqual(interDomainLinkNber, 4)
        self.assertEqual(alienToTapiLinkNber, 4)

    def test_14_getNodes_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][3]['node-id'], 'TAPI-SBI-ABS-NODE')
        self.assertIn(self.CHECK_DICT_TAPINODES, response['network'][0]['node'])

    def test_15_disconnect_spdrA(self):
        time.sleep(2)
        response = test_utils.unmount_device('SPDR-SA1')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_16_disconnect_spdrC(self):
        response = test_utils.unmount_device('SPDR-SC1')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_17_disconnect_roadmA(self):
        response = test_utils.unmount_device('ROADM-A1')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_18_disconnect_roadmC(self):
        response = test_utils.unmount_device('ROADM-C1')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == '__main__':
    unittest.main(verbosity=2)
