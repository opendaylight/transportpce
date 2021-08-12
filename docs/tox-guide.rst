.. _transportpce-tox-guide:

TransportPCE Tox Guide
======================

What is tox?
------------

Tox is a tool written in Python to ease tests automation and dependencies management.
It provides a command line tool that run tests inside a Python virtual environment.
https://docs.python.org/3/glossary.html#term-virtual-environment
This means that it will not modify your local system settings and package or executable files
but uses a hidden folder (.tox) to install the required python dependencies via pip
(the package installer for Python https://pip.pypa.io/) before running tests.

You can fin more details about tox at https://tox.readthedocs.io/

This makes tox configuration and behavior very portable across GNU+Linux distributions
and environments.
Tox is often used as a front-end to Continuous Integration servers.

For instance, Linux Foundation globaljjb provides a gerrit-tox-verify profile that 
is used by TransportPCE in OpenDaylight Jenkins CI.
https://globaljjbdocstest.readthedocs.io/en/latest/jjb/lf-python-jobs.html#tox-verify
https://git.opendaylight.org/gerrit/c/releng/builder/+/78656

Once installed in your local environment, the same test suite can be run locally by
simply running tox inside your project git clone local folder::

    $ sudo apt-get install tox
    $ tox
