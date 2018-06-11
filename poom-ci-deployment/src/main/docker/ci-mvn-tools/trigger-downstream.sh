#!/usr/bin/env bash

UTILITIES=/usr/local/lib/poom-ci-utilities.jar
CLASS=org.codingmatters.poom.ci.utilities.pipeline.client.org.codingmatters.poom.ci.utilities.upstream.UpstreamBuildTriggerer

java -cp $UTILITIES $CLASS "$@"