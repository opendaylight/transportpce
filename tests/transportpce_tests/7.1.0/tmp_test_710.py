#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################
import json
import os
import re
import signal
import subprocess
import time
import psutil
import requests

HONEYNODE_OK_START_MSG = re.escape("Netconf SSH endpoint started successfully at 0.0.0.0")

TYPE_APPLICATION_JSON = {'content-type': 'application/json'}
restconf_baseurl = "http://localhost:8181/restconf"
headers = {'content-type': 'application/json'}

honeynode_executable = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "7.1.0", "honeynode-simulator", "honeycomb-tpce")


samples_directory = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "7.1.0")

log_directory = os.path.dirname(os.path.realpath(__file__))


def start_xpdra_honeynode():
    log_file = os.path.join(log_directory, "oper-XPDRA.log")
    process = start_node(log_file, "17840", "oper-XPDRA2.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 5000)
    return process


def start_tpce():
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        print("starting LIGHTY.IO TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "lighty", "target", "tpce",
            "clean-start-controller.sh")
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable], stdout=outfile, stderr=outfile,
                stdin=open(os.devnull))
    else:
        print("starting KARAF TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "karaf", "target", "assembly", "bin", "karaf")
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable, "server"], stdout=outfile, stderr=outfile,
                stdin=open(os.devnull))


def install_karaf_feature(feature_name: str):
    print("installing feature " + feature_name)
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "karaf", "target", "assembly", "bin", "client")
    return subprocess.run([executable],
                          input='feature:install ' + feature_name +
                                '\n feature:list | grep tapi \n logout \n',
                          universal_newlines=True)


def post_request(url, data, username, password):
    return requests.request(
        "POST", url, data=json.dumps(data),
        headers=TYPE_APPLICATION_JSON, auth=(username, password))


def put_request(url, data, username, password):
    return requests.request(
        "PUT", url, data=json.dumps(data), headers=TYPE_APPLICATION_JSON,
        auth=(username, password))


def delete_request(url, username, password):
    return requests.request(
        "DELETE", url, headers=TYPE_APPLICATION_JSON,
        auth=(username, password))


def generate_connect_data(node_id: str, node_port: str):
    data = {"node": [{
      "node-id": node_id,
      "netconf-node-topology:username": "admin",
      "netconf-node-topology:password": "admin",
      "netconf-node-topology:host": "127.0.0.1",
      "netconf-node-topology:port": node_port,
      "netconf-node-topology:tcp-only": "false",
      "netconf-node-topology:pass-through": {}}]}
    return data


def generate_link_data(xpdr_node: str, xpdr_num: str, network_num: str, rdm_node: str, srg_num: str, termination_num: str):
    data = {
              "networkutils:input": {
              "networkutils:links-input": {
              "networkutils:xpdr-node": xpdr_node,
              "networkutils:xpdr-num": xpdr_num,
              "networkutils:network-num": network_num,
              "networkutils:rdm-node": rdm_node,
              "networkutils:srg-num": srg_num,
              "networkutils:termination-point-num": termination_num
              }
            }
          }
    return data


def shutdown_process(process):
    if process is not None:
        for child in psutil.Process(process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        process.send_signal(signal.SIGINT)


def start_node(log_file: str, node_port: str, node_config_file_name: str):
    # print(log_file)
    config_file_path = os.path.join(samples_directory, node_config_file_name)
    if os.path.isfile(honeynode_executable):
        with open(log_file, 'w') as outfile:
            if os.path.isfile(config_file_path):
                return subprocess.Popen(
                    [honeynode_executable, node_port, config_file_path],
                    stdout=outfile, stderr=outfile)


def wait_until_log_contains(log_file, searched_string, time_to_wait=20):
    found = False
    tail = None
    try:
        with timeout(seconds=time_to_wait):
            print("Waiting for " + searched_string)
            tail = subprocess.Popen(['tail', '-F', log_file],
                                    stdout=subprocess.PIPE,
                                    stderr=subprocess.PIPE)
            regexp = re.compile(searched_string)
            while True:
                line = tail.stdout.readline().decode('utf-8')
                if regexp.search(line):
                    print("Searched string found.")
                    found = True
                    break
    except TimeoutError:
        print("Cannot find string "+searched_string+" after waiting for "+str(time_to_wait))
    finally:
        if tail is not None:
            print("Stopping tail command")
            tail.stderr.close()
            tail.stdout.close()
            tail.kill()
            tail.wait()
    return found


class timeout:
    def __init__(self, seconds=1, error_message='Timeout'):
      self.seconds = seconds
      self.error_message = error_message

    def handle_timeout(self, signum, frame):
      raise TimeoutError(self.error_message)

    def __enter__(self):
      signal.signal(signal.SIGALRM, self.handle_timeout)
      signal.alarm(self.seconds)

    def __exit__(self, type, value, traceback):
      signal.alarm(0)


def mount_openroadm_ne(node_id: str, port: int) -> None:
    data = generate_connect_data(node_id, port)
    url = ("{}/config/network-topology:"
           "network-topology/topology/topology-netconf/node/{}"
           .format(restconf_baseurl, node_id))
    response = requests.request(
        "PUT", url, data=json.dumps(data), headers=headers,
        auth=('admin', 'admin'))
    print("Response Status code is {}".format(response.status_code))
    print("Requests code for PUT operation is {}".format(requests.codes.created))
    # assert(response.status_code == requests.codes.created)
    time.sleep(10)
    response = requests.request(
        "GET", url, headers=headers, auth=('admin', 'admin'))
    # assert(response.status_code == requests.codes.ok)
    res = response.json()
    print(res)
    # assert(res['node'][0]['netconf-node-topology:connection-status']=='connected')


def teardown(process_name_list):
    for process_name in process_name_list:
        try:
            print("Tearing down {}".format(process_name.pid))
            for child in psutil.Process(process_name.pid).children():
                child.send_signal(signal.SIGINT)
                child.wait()
                process_name.send_signal(signal.SIGINT)
            process_name.wait()
        except AttributeError:
            print("There is no process for {}".format(process_name))
            continue


class timeout:
    def __init__(self, seconds=1, error_message='Timeout'):
      self.seconds = seconds
      self.error_message = error_message

    def handle_timeout(self, signum, frame):
      raise TimeoutError(self.error_message)

    def __enter__(self):
      signal.signal(signal.SIGALRM, self.handle_timeout)
      signal.alarm(self.seconds)

    def __exit__(self, type, value, traceback):
      signal.alarm(0)


# Here we need to start first the honeynodes for ROADM-A, C and OTN-A and C
if __name__ == '__main__':

    print("Start the tpce controller")
    # TODO: Check if the controller is already running
    odl_process = start_tpce()
    time.sleep(60)
    print("opendaylight started")
    honeynode_process1 = None
    
    print("Starting XPDR-A1....")
    honeynode_process1 = start_xpdra_honeynode()
  
    print("Sleeping for 30 seconds")
    time.sleep(30)
  
    if honeynode_process1 is not None:
        print("Start mounting XPDR-A1")
        mount_openroadm_ne('XPDR-A1', 17840)
        time.sleep(5)
    else:
        print("Skip the mounting of XPDR-A1")

    try:
        while True:
            continue
    except KeyboardInterrupt:
        print("Tearing down all the process that was started")
        # Tear down the process
        # This will teardown all the signals
        teardown([odl_process, honeynode_process1])
