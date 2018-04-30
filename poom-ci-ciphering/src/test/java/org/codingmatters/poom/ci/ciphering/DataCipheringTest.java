package org.codingmatters.poom.ci.ciphering;

import org.codingmatters.poom.ci.ciphering.descriptors.CipheredData;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DataCipheringTest {

    private KeyStore privateKeystore;
    private KeyStore publicKeystore;

    @Before
    public void setUp() throws Exception {
        this.privateKeystore = KeyStore.getInstance("pkcs12");
        this.privateKeystore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("private-store.p12"), "changeit".toCharArray());
        assertTrue(this.privateKeystore.containsAlias("test-secret"));

        this.publicKeystore = KeyStore.getInstance("pkcs12");
        this.publicKeystore.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("public-store.p12"), "changeit".toCharArray());
        assertTrue(this.publicKeystore.containsAlias("test-secret"));
    }

    @Test
    public void file() throws Exception {
        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) fact.generateCertificate(Thread.currentThread().getContextClassLoader().getResourceAsStream("test.pem"));

        CipheredData ciphered = new DataCipherer(certificate, readResource("sample.xml")).cipher();
        byte[] unciphered = new DataUncipherer(this.privateKeystore, "changeit".toCharArray()).uncipher(ciphered);

        assertThat(unciphered, is(this.readResource("sample.xml")));

    }

    private byte[] readResource(String resource) throws Exception {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream(); InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            byte [] buffer = new byte[1024];
            for(int read = in.read(buffer) ; read != -1 ; read = in.read(buffer)) {
                out.write(buffer, 0, read);
            }
            out.close();
            return out.toByteArray();
        }
    }


}