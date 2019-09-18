#!/bin/bash

echo "Starting openroadm 2.2.1 end2end tests with lighty build..."
export USE_LIGHTY="True"
tox -e end2end221
