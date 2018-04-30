#!/usr/bin/env bash
#set -x

PARAMS=$(openssl enc -aes-256-cbc -pass pass:$(hexdump -n 16 -e '4/4 "%08X" 1 "\n"' /dev/random) -P)

for PARAM in $PARAMS
do
    PNAME="$(echo $PARAM | cut -d'=' -f1)"
    PVALUE="$(echo $PARAM | cut -d'=' -f2)"
    echo "$PARAM : $PNAME --> $PVALUE"
done

