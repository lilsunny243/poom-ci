#!/usr/bin/env bash
set -e
if [ $# -lt 2 ]; then
    echo "Usage: $0 <dependency service base url> <repository dir>"
    echo "was : $@"
    exit 1
fi

BASE_URL=$1
REPO_DIR=$2

if [ -z $REPOSITORY_ID ]; then
    echo "REPOSITORY_ID must be setted"
    exit 1
fi
if [ -z $REPOSITORY ]; then
    echo "REPOSITORY must be setted"
    exit 1
fi
if [ -z $CHECKOUT_SPEC ]; then
    echo "CHECKOUT_SPEC must be setted"
    exit 1
fi

SCRIPT_DIR=$(dirname "$(readlink -f "$0")")

REPO="{\"name\":\"$REPOSITORY\",\"checkoutSpec\":\"$CHECKOUT_SPEC\"}"
echo "uploading repo metas : $REPO"
curl -XPUT $BASE_URL/repositories/$REPOSITORY_ID -d "$REPO"
echo

echo "uploading produced modules meta : "
MODULES=$($SCRIPT_DIR/repository-modules-maven.sh $REPO_DIR json)
echo $MODULES
curl -XPOST $BASE_URL/repositories/$REPOSITORY_ID/produces -d "$MODULES"
echo

echo "uploading depends-on modules meta :"
PRODUCES=$($SCRIPT_DIR/repository-dependencies-maven.sh $REPO_DIR json)
echo $PRODUCES
curl -XPOST $BASE_URL/repositories/$REPOSITORY_ID/depends-on -d "$PRODUCES"
echo
