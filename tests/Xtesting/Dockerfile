FROM alpine:3.18

ARG BRANCH=master

ADD . /src/
RUN apk --no-cache add --virtual .build-deps --update \
        py3-tox py3-pip py3-wheel py3-psutil build-base linux-headers git && \
    git init /src/transportpce && \
    (cd /src/transportpce && \
        git fetch --tags https://git.opendaylight.org/gerrit/transportpce.git $BRANCH && \
        git checkout FETCH_HEAD) && \
    pip install -r /src/transportpce/tests/requirements.txt \
        -r /src/transportpce/tests/test-requirements.txt && \
    apk del .build-deps
COPY testcases.yaml /usr/lib/python3.11/site-packages/xtesting/ci/testcases.yaml
