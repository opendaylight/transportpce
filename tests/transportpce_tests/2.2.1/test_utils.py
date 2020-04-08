import os
import subprocess

honeynode_executable = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "2.2.1", "honeynode-distribution", "target", "honeynode-distribution-1.19.04-hc",
    "honeynode-distribution-1.19.04", "honeycomb-tpce")
samples_directory = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "2.2.1")


def start_xpdra_honeynode():
    if os.path.isfile(honeynode_executable):
        with open('honeynode1.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, "17840", os.path.join(samples_directory, "oper-XPDRA.xml")],
                stdout=outfile, stderr=outfile)


def start_roadma_honeynode():
    if os.path.isfile(honeynode_executable):
        with open('honeynode2.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, "17841", os.path.join(samples_directory, "oper-ROADMA.xml")],
                stdout=outfile, stderr=outfile)


def start_roadmb_honeynode():
    if os.path.isfile(honeynode_executable):
        with open('honeynode5.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, "17842", os.path.join(samples_directory, "oper-ROADMB.xml")],
                stdout=outfile, stderr=outfile)


def start_roadmc_honeynode():
    if os.path.isfile(honeynode_executable):
        with open('honeynode3.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, "17843", os.path.join(samples_directory, "oper-ROADMC.xml")],
                stdout=outfile, stderr=outfile)


def start_xpdrc_honeynode():
    if os.path.isfile(honeynode_executable):
        with open('honeynode4.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, "17844", os.path.join(samples_directory, "oper-XPDRC.xml")],
                stdout=outfile, stderr=outfile)


def start_spdra_honeynode():
    if os.path.isfile(honeynode_executable):
        with open('honeynode6.log', 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, "17845", os.path.join(samples_directory, "oper-SPDRAv2.xml")],
                stdout=outfile, stderr=outfile)


def start_tpce():
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        print("starting LIGHTY.IO TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "lighty", "target", "lighty-transportpce-12.0.1-SNAPSHOT",
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
