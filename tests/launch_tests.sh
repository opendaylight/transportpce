#!/bin/sh

if [ -n "$USE_ODL_ALT_KARAF_ENV" ]; then
    echo "using environment variables from $USE_ODL_ALT_KARAF_ENV"
    . $USE_ODL_ALT_KARAF_ENV
fi

if [ -n "$USE_OPENLAB_DEVICE_ENV" ]; then
    echo "using device environment variables from $USE_OPENLAB_DEVICE_ENV"
    . $USE_OPENLAB_DEVICE_ENV
fi

for arg in $@; do
    if [ -z "$test_suite" ]; then
        test_suite=$1
    else
# Tox reinterprets "olm" and "pce" by "../olm" "../pce" because of the
# changedir directive and the presence of modules folders "olm" and pce"
# at project root.
        if [ "$arg" = "../olm" ]; then
            arglist=$arglist" olm"
        elif [ "$arg" = "../pce" ]; then
            arglist=$arglist" pce"
        else
            arglist=$arglist" "$arg
        fi
    fi
done

scriptlist=""
if [ -z "$arglist" ]; then
    scriptlist="transportpce_tests/$test_suite/test[0-9][0-9]_*.py"
else
    for test in $arglist; do
        scriptlist=$scriptlist" transportpce_tests/$test_suite/test[0-9][0-9]_$test.py"
    done
fi
if [ -z "$LAUNCHER" ]; then
    if [ `which pytest-3` ]; then
            LAUNCHER="pytest-3"
    else
        if [ `which pytest` ]; then
            LAUNCHER="pytest"
        else
            LAUNCHER="python3 -m pytest"
        fi
    fi
    if [ -d "allure-report" ]; then
        LAUNCHER="$LAUNCHER --alluredir=allure-report/ -s"
    fi
    LAUNCHER="$LAUNCHER -q"
fi

for script in $scriptlist; do
    echo $LAUNCHER $(ls $script)
    $LAUNCHER $script || exit 1
done
