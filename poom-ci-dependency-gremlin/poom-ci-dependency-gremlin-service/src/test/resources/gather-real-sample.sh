#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $0))

REPOS=$(curl https://dependencies.ci.flexio.io/poomci-dependency-api/repositories/ | jq '.[] | .id' | xargs)

curl https://dependencies.ci.flexio.io/poomci-dependency-api/repositories/ | json_pp > $SCRIPT_DIR/real-sample/repositories.json

for REPO in $REPOS ; do
    curl https://dependencies.ci.flexio.io/poomci-dependency-api/repositories/$REPO/produces | json_pp > $SCRIPT_DIR/real-sample/produces/$REPO.json
    curl https://dependencies.ci.flexio.io/poomci-dependency-api/repositories/$REPO/depends-on | json_pp > $SCRIPT_DIR/real-sample/depends-on/$REPO.json
done
