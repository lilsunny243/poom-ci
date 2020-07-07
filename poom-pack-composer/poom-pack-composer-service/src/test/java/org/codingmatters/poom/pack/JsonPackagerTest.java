package org.codingmatters.poom.pack;

import org.codingmatters.poom.ci.api.types.JsonPackage;
import org.codingmatters.poom.pack.handler.pack.JsonPackager;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JsonPackagerTest {


    private File repository;
    private String serviceUrl;

    @Before
    public void setUp() throws Exception {
        JsonPackager.deleteFile();
        String repositoryPath = Thread.currentThread().getContextClassLoader().getResource( "repository" ).getPath();
        repository = new File( repositoryPath );
        serviceUrl = "http://service:456";
    }

    @Test
    public void testJsonPackagerMechanic() throws IOException {
        File file1 = JsonPackager.getFile( repository, serviceUrl );
        File file2 = JsonPackager.getFile( repository, serviceUrl );
        assertThat( file1.getPath(), is( file2.getPath() ) );

        assertThat( file1.exists(), is( true ) );
        JsonPackager.deleteFile();
        assertThat( file1.exists(), is( false ) );

        file1 = JsonPackager.getFile( repository, serviceUrl );
        assertThat( file1.getPath(), not( file2.getPath() ) );
        assertThat( file1.exists(), is( true ) );
    }

    @Test
    public void jsonPackagerManualDeleteRecovery() throws IOException {
        File file1 = JsonPackager.getFile( repository, serviceUrl );
        assertTrue( file1.delete() );
        File file2 = JsonPackager.getFile( repository, serviceUrl );
        assertThat( file1.getPath(), not( file2.getPath() ) );
    }

}
