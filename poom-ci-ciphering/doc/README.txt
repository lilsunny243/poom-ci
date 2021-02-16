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

java -jar poom-ci-ciphering-2.12.0-SNAPSHOT-cipher-client.jar

_______________
UNCIPHER A FILE

java -cp poom-ci-ciphering-2.12.0-SNAPSHOT-cipher-client.jar org.codingmatters.poom.ci.ciphering.app.FileUncipherer

_______________________________________
CREATE AND DISTRIBUTE A PGP SIGNING KEY

*GENERATE AND PUBLISH*

gpg --gen-key
gpg --keyserver hkp://pool.sks-keyservers.net --send-keys D1A7703E505EC1628689AC84273E5BFDDF72DDD4

*EXPORT KEY PAIR*

gpg --export --armor dev@flexio.fr > codesigning.asc
gpg --export-secret-keys --armor dev@flexio.fr >> codesigning.asc

*ENCRYPT FOR CI*

java -jar poom-ci-ciphering-0.0.1-SNAPSHOT-cipher-client.jar poom-ci.pem codesigning.asc codesigning.asc.enc

*IMPORT*
gpg --homedir /gnupg --import /gpgtransport/flexio-ci-gpg.key