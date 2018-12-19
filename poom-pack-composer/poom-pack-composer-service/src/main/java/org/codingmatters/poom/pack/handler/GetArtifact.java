package org.codingmatters.poom.pack.handler;

import org.codingmatters.poom.ci.api.ArtifactsGetResponse;
import org.codingmatters.poom.ci.api.ArtifactsGetRequest;
import org.codingmatters.poom.ci.api.types.Error;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.rest.io.Content;

import java.io.File;
import java.util.function.Function;

public class GetArtifact implements Function<ArtifactsGetRequest, ArtifactsGetResponse> {

    private CategorizedLogger log = CategorizedLogger.getLogger( GetArtifact.class );
    private final String repositoryPath;

    public GetArtifact( String repositoryPath ) {
        this.repositoryPath = repositoryPath;
    }

    @Override
    public ArtifactsGetResponse apply( ArtifactsGetRequest artifactsGetRequest ) {
        try {
            String filePath = String.join( File.separator, repositoryPath, artifactsGetRequest.vendor(), artifactsGetRequest.packageName(), artifactsGetRequest.version(), artifactsGetRequest.fileName() );
            log.info( "Getting artifact: " + filePath );
            File artifactFile = new File( filePath );
            if( !artifactFile.exists() ) {
                return ArtifactsGetResponse.builder()
                        .status404( status->status.build() )
                        .build();
            }
            Content content = Content.from( artifactFile );
            return ArtifactsGetResponse.builder()
                    .status200( status->status
                            .contentLength( content.length() + "" )
                            .payload( file->file
                                    .contentType( "application/zip" )
                                    .content( content )
                            ) )
                    .build();
        } catch( Exception e ) {
            log.error( "Error while sending artifact:", e );
            return ArtifactsGetResponse.builder()
                    .status500( status->status
                            .payload( err->err.code( Error.Code.ARTIFACT_NOT_FOUND )
                                    .description( "Artifact not found" ) ) )
                    .build();
        }
    }
}
