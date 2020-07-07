package org.codingmatters.poom.pack;

import org.codingmatters.poom.ci.api.ArtifactsDeleteRequest;
import org.codingmatters.poom.ci.api.RepositoryPostRequest;
import org.codingmatters.poom.pack.handler.DeleteArtifact;
import org.codingmatters.poom.pack.handler.SavePackage;
import org.codingmatters.poom.pack.handler.pack.JsonPackager;
import org.codingmatters.rest.api.types.File;
import org.codingmatters.rest.io.Content;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeleteArtifactTest {

    private final String API_KEY = "9";

    private TemporaryFolder temp;
    private SavePackage savePackage;
    private DeleteArtifact deleteArtifact;
    private String vendor;
    private String artifactId;
    private String artifactVersion;

    @Before
    public void setUp() throws Exception {
        JsonPackager.deleteFile();
        temp = new TemporaryFolder();
        temp.create();
        deleteArtifact = new DeleteArtifact( temp.getRoot().getPath(), API_KEY );
        savePackage = new SavePackage( temp.getRoot().getPath(), API_KEY );

        java.io.File zipArchive = new java.io.File( Thread.currentThread().getContextClassLoader().getResource( "flexio-tabular-php-client-1.0.0-SNAPSHOT.zip" ).getPath() );


        vendor = "flexio-services";
        artifactId = "flexio-tabular-php-client";
        artifactVersion = "1.0.0-dev";
        savePackage.apply(
                RepositoryPostRequest.builder()
                        .xVendor( vendor )
                        .xArtifactId( artifactId )
                        .xVersion( artifactVersion )
                        .xApiKey( API_KEY )
                        .payload( File.builder()
                                .content( Content.from( zipArchive ) )
                                .contentType( "application/zip" )
                                .build() )
                        .build()
        ).opt().status201().orElseThrow( ()->new AssertionError( "Cannot save the artifact" ) );
    }

    @Test
    public void testRemoveWithBadApiKey_thenReturn403() {
        deleteArtifact.apply(
                ArtifactsDeleteRequest.builder()
                        .xApiKey( "BAD_API_KEY" )
                        .vendor( vendor )
                        .packageName( artifactId )
                        .version( artifactVersion )
                        .fileName( artifactId + "-" + artifactVersion + ".zip" )
                        .build()
        ).opt().status403().orElseThrow( ()->new AssertionError( "Should get 403 with bad api key" ) );
    }

    @Test
    public void testRemove_thenReturn204_thenFileDoNotExist() {
        String filePath = temp.getRoot().getPath() + "/" + vendor + "/" + artifactId + "/" + artifactVersion + "/" + artifactId + "-" + artifactVersion + ".zip";
        assertTrue( new java.io.File( filePath ).exists() );
        deleteArtifact.apply(
                ArtifactsDeleteRequest.builder()
                        .xApiKey( API_KEY )
                        .vendor( vendor )
                        .packageName( artifactId )
                        .version( artifactVersion )
                        .fileName( artifactId + "-" + artifactVersion + ".zip" )
                        .build()
        ).opt().status204().orElseThrow( ()->new AssertionError( "Should delete this artifact" ) );
        assertFalse( new java.io.File( filePath ).exists() );
    }

    @Test
    public void testRemoveNonexistentArtifact_thenReturn404() {
        deleteArtifact.apply(
                ArtifactsDeleteRequest.builder()
                        .xApiKey( API_KEY )
                        .vendor( vendor )
                        .packageName( artifactId )
                        .vendor( artifactVersion )
                        .fileName( artifactId + "-" + artifactVersion + ".zip" )
                        .build()
        ).opt().status404().orElseThrow( ()->new AssertionError( "Should get 404 with non existent artifact" ) );

    }
}
