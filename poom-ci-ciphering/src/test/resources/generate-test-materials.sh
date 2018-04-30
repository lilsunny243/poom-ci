#!/usr/bin/env bash
set -x

rm -rf private-store.p12 public-store.p12 test.pem sample.xml.enc sample.xml.key sample.xml.base64

keytool -genkeypair -keysize 2048 -keyalg RSA -deststoretype pkcs12 \
    -alias old-test-secret \
    -dname "CN=test-secrets, OU=test, O=Flexio.fr, L=Besancon, S=Doubs, C=FR" \
    -keystore private-store.p12

keytool -genkeypair -keysize 2048 -keyalg RSA -deststoretype pkcs12 \
    -alias test-secret \
    -dname "CN=test-secrets, OU=test, O=Flexio.fr, L=Besancon, S=Doubs, C=FR" \
    -keystore private-store.p12



keytool -export -alias test-secret -keystore private-store.p12 -file test.pem

keytool -import -trustcacerts -file test.pem -alias test-secret -deststoretype pkcs12 -keystore public-store.p12



#openssl pkcs12 -in private-store.p12 -nokeys -out test.pem
#
#
#
#
#
##RAW_KEY=$(openssl rand 32)
##HEX_KEY=$(echo $RAW_KEY -n | hexdump -e '"%x"') ### HEX TOO LONG
#
#HEX_KEY=$(hexdump -n 16 -e '4/4 "%08X" 1 "\n"' /dev/random)
#RAW_KEY=$(echo $HEX_KEY | xxd -r -p)
#
#
#echo "-----BEGIN KEY-----" > sample.xml.enc
#echo $RAW_KEY | openssl rsautl -encrypt -certin -inkey test.pem | openssl enc -base64 >> sample.xml.enc
#echo "-----END KEY-----" >> sample.xml.enc
#
##hexdump -n 16 -e '4/4 "%08X" 1 "\n"' /dev/random
#
#echo "-----BEGIN DATA-----" >> sample.xml.enc
##openssl enc -in sample.xml -e -aes256 -k $PASS -iv "E48BD8C737D5BF62555115A7EF82AE34" -base64 >> sample.xml.enc
#openssl enc -in sample.xml -e -aes256 -K $HEX_KEY -iv "E48BD8C737D5BF62555115A7EF82AE34" -base64 >> sample.xml.enc
#echo "-----END DATA-----" >> sample.xml.enc
#
#
#cat sample.xml | openssl enc -base64 > sample.xml.base64
#
##openssl enc -in sample.xml.enc -out sample.xml.dec -d -aes256 -k $PASS -base64



