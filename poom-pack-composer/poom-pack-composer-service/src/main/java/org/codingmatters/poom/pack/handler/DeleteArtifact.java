package org.codingmatters.poom.pack.handler;

import org.codingmatters.poom.ci.api.ArtifactsDeleteRequest;
import org.codingmatters.poom.ci.api.ArtifactsDeleteResponse;
import org.codingmatters.poom.ci.api.artifactsdeleteresponse.Status204;
import org.codingmatters.poom.ci.api.artifactsdeleteresponse.Status403;
import org.codingmatters.poom.ci.api.artifactsdeleteresponse.Status404;
import org.codingmatters.poom.ci.api.types.Error;
import org.codingmatters.poom.pack.handler.pack.JsonPackager;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.File;
import java.util.function.Function;

public class DeleteArtifact implements Function<ArtifactsDeleteRequest, ArtifactsDeleteResponse> {

    private CategorizedLogger log = CategorizedLogger.getLogger( DeleteArtifact.class );
    private final String repositoryPath;
    private final String apiKey;

    public DeleteArtifact( String repositoryPath, String apiKey ) {
        this.repositoryPath = repositoryPath;
        this.apiKey = apiKey;
    }

    @Override
    public ArtifactsDeleteResponse apply( ArtifactsDeleteRequest artifactsDeleteRequest ) {
        if( !apiKey.equals( artifactsDeleteRequest.xApiKey() ) ) {
            return ArtifactsDeleteResponse.builder()
                    .status403( Status403.Builder::build )
                    .build();
        }
        String filePath = String.join( File.separator,
                repositoryPath,
                artifactsDeleteRequest.vendor(),
                artifactsDeleteRequest.packageName(),
                artifactsDeleteRequest.version(),
                artifactsDeleteRequest.packageName() + "-" + artifactsDeleteRequest.version() + ".zip"
        );
        File artifactFile = new File( filePath );
        if( !artifactFile.exists() ) {
            return ArtifactsDeleteResponse.builder()
                    .status404( Status404.Builder::build )
                    .build();
        } else {
            boolean deleted = artifactFile.delete();
            if( deleted ) {
                JsonPackager.deleteFile();
                return ArtifactsDeleteResponse.builder().status204( Status204.Builder::build ).build();
            } else {
                log.error( "Error, could not delete artifact" );
                return ArtifactsDeleteResponse.builder().status500( status->status.payload( err->err.code( Error.Code.UNEXPECTED_ERROR ).description( "Could not delete artifact" ) ) ).build();
            }
        }
    }
}

