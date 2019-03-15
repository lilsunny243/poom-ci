#!/usr/bin/env bash

SCRIPT_DIR=$(dirname $(readlink -f $0))

REPOS=$(curl https://dependencies.ci.flexio.io/poomci-dependency-api/repositories/ | jq '.[] | .id' | xargs)

for REPO in $REPOS ; do
    echo "deleting repository $REPO"
    curl -XDELETE https://dependencies.ci.flexio.io/poomci-dependency-api/repositories/$REPO
done
