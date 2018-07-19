#!/usr/bin/env bash
set -e
if [ $# -lt 1 ]; then
        echo "Usage: $0 <lookup dir> {json}"
        exit 1
fi

LOOKUP_DIR=$1
FORMAT=raw
if [ $# -gt 1 ]; then
    FORMAT=$2
fi

MVN="mvn -f $LOOKUP_DIR/pom.xml"

LIST_FILE=$(mktemp /tmp/mvn-dep-list.XXXXXXXXX)

$MVN dependency:list -DoutputFile=$LIST_FILE -DappendOutput=true -Dsilent=true > /dev/null

#   com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:jar:2.8.5:compile
DEPS=$(cat $LIST_FILE | grep -v "   none" | grep -E '^   *:*:*:*:*' | sed -r 's/^   (.*:.*):.*:(.*):.*/\1:\2/' | sort -u)
rm $LIST_FILE

if [ "$FORMAT" = "json" ]; then
    #declare -a MODULES
    MODULES=""
    for DEP in $DEPS
    do
        SPEC=$(echo $DEP | sed -r 's/(.*:.*):.*/\1/')
        VERSION=$(echo $DEP | sed -r 's/.*:.*:(.*)/\1/')

        MODULE="{\"spec\":\"$SPEC\",\"version\":\"$VERSION\"}"
        MODULES="$MODULES $MODULE"
    done

    MS=($MODULES)

    echo -n "["
    for (( i=0; i<${#MS[@]}; i++ ));
    do
        if [ $i -ne 0 ]; then
            echo -n ","
        fi
        echo -n ${MS[$i]}
    done
    echo -n "]"

else
    echo "${DEPS}"
fi