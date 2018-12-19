package org.codingmatters.poom.pack;

import org.codingmatters.poom.ci.api.ArtifactsGetRequest;
import org.codingmatters.poom.ci.api.artifactsgetresponse.Status200;
import org.codingmatters.poom.pack.handler.GetArtifact;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;

public class GetArtifactTest {

    private GetArtifact getArtifact;
    private String repositoryPath;

    @Before
    public void setUp() throws Exception {
        repositoryPath = Thread.currentThread().getContextClassLoader().getResource( "repository" ).getPath();
        getArtifact = new GetArtifact( repositoryPath );
    }

    @Test
    public void testGetNonexistentArtifact_thenReturn404() {
        getArtifact.apply(
                ArtifactsGetRequest.builder()
                        .vendor( "vendor" )
                        .packageName( "package" )
                        .version( "version" )
                        .fileName( "file" )
                        .build()
        ).opt().status404().orElseThrow( ()->new AssertionError( "Should get 404 if artifact not found" ) );
    }

    @Test
    public void testGetExistingArtifact_thenReturn200() throws IOException {
        Status200 response = getArtifact.apply(
                ArtifactsGetRequest.builder()
                        .vendor( "vendor-1" )
                        .packageName( "package-1" )
                        .version( "1.0" )
                        .fileName( "package-1-1.0.zip" )
                        .build()
        ).opt().status200().orElseThrow( ()->new AssertionError( "Should get 200 with existing artefact" ) );

        byte[] content = Files.readAllBytes( Paths.get( repositoryPath + "/vendor-1/package-1/1.0/package-1-1.0.zip" ) );
        assertThat( response.payload().content().asBytes(), CoreMatchers.is( content ) );
    }

}
