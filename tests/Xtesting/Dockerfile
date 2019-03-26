FROM alpine:3.18

ARG BRANCH=master

ADD . /src/
RUN apk  --update add \
        py3-tox py3-pip py3-wheel py3-psutil build-base linux-headers git python3-dev python3 libxml2-dev libxslt-dev gcc openjdk17-jdk maven && \
    git init /src/transportpce && \
    (cd /src/transportpce && \
        git fetch --tags https://git.opendaylight.org/gerrit/transportpce.git $BRANCH && \
        git checkout FETCH_HEAD) && \
    pip install -r /src/requirements.txt && \
    pip install -r /src/transportpce/tests/requirements.txt \
        -r /src/transportpce/tests/test-requirements.txt
COPY testcases.yaml /usr/lib/python3.11/site-packages/xtesting/ci/testcases.yaml
CMD ["run_tests", "-t", "all"]
