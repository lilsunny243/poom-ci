_____________________________________________
GENERATE PRIVATE KEYSTORE / KEY / CERTIFICATE

keytool -genkeypair -keysize 2048 -keyalg RSA -deststoretype pkcs12 \
    -alias poom-ci-secret-1 \
    -dname "CN=poom-ci-secret, OU=poom-ci, O=Flexio.fr, L=Besancon, S=Doubs, C=FR" \
    -keystore poomci-private-store.p12

__________________
EXPORT CERTIFICATE

keytool -export -alias poom-ci-secret-1 -keystore poomci-private-store.p12 -file poom-ci.pem

_____________
CIPHER A FILE

java -jar poom-ci-ciphering-0.0.1-SNAPSHOT-cipher-client.jar