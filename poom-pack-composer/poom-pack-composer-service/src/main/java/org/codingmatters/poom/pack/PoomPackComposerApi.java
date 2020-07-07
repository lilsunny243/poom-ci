package org.codingmatters.poom.pack;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.api.PoomPackComposerHandlers;
import org.codingmatters.poom.pack.handler.*;
import org.codingmatters.poom.ci.service.PoomPackComposerProcessor;
import org.codingmatters.poom.pack.handler.pack.JsonPackageBuilder;
import org.codingmatters.rest.api.Processor;

import java.io.File;


public class PoomPackComposerApi {

    private final String name = "poom-pack-composer";
    private PoomPackComposerHandlers handlers;
    private PoomPackComposerProcessor processor;

    public PoomPackComposerApi( String repositoryPath, String serviceUrl, String api_key ) {
        final File repository = new File( repositoryPath );
        this.handlers = new PoomPackComposerHandlers.Builder()
                .packagesGetHandler( new GetPackage( repository, serviceUrl ) )
                .repositoryPostHandler( new SavePackage( repositoryPath, api_key ) )
                .artifactsDeleteHandler( new DeleteArtifact( repositoryPath, api_key ) )
                .artifactsGetHandler( new GetArtifact( repositoryPath ) )
                .build();
        this.processor = new PoomPackComposerProcessor( this.path(), new JsonFactory(), handlers );
    }

    public Processor processor() {
        return this.processor;
    }


    public String path() {
        return "/" + this.name;
    }

}

