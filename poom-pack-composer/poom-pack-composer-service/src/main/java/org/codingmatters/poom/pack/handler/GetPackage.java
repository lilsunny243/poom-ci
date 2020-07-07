package org.codingmatters.poom.pack.handler;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.api.PackagesGetRequest;
import org.codingmatters.poom.ci.api.PackagesGetResponse;
import org.codingmatters.poom.ci.api.types.Error;
import org.codingmatters.poom.ci.api.types.JsonPackage;
import org.codingmatters.poom.ci.api.types.json.JsonPackageReader;
import org.codingmatters.poom.pack.handler.pack.JsonPackager;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

public class GetPackage implements Function<PackagesGetRequest, PackagesGetResponse> {

    private final CategorizedLogger log = CategorizedLogger.getLogger( GetPackage.class );
    private final File repository;
    private final String serviceUrl;

    public GetPackage( File repository, String serviceUrl ) {
        this.repository = repository;
        this.serviceUrl = serviceUrl;
    }

    @Override
    public PackagesGetResponse apply( PackagesGetRequest packagesGetRequest ) {
        try{
            File packageFile = JsonPackager.getFile( repository, serviceUrl );
            JsonPackage pack = new JsonPackageReader().read( new JsonFactory().createParser( packageFile ) );
            return PackagesGetResponse.builder().status200( status -> status.payload( pack ) ).build();
        } catch( IOException e ) {
            log.tokenized().error( "Error", e );
            return PackagesGetResponse.builder()
                    .status404( st -> st.payload( Error.builder().build() ) )
                    .build();
        }
    }

}
