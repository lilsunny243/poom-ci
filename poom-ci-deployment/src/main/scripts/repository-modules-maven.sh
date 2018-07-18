#!/usr/bin/env bash
set -e
set -x
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

DEPS=$($MVN -q -Dexec.executable='echo' -Dexec.args='\${project.groupId}:\${project.artifactId}:\${project.version}' exec:exec)

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