package org.codingmatters.poom.pack;

import com.fasterxml.jackson.core.JsonFactory;
import org.codingmatters.poom.ci.api.PoomPackComposerHandlers;
import org.codingmatters.poom.pack.handler.DeleteArtifact;
import org.codingmatters.poom.pack.handler.GetArtifact;
import org.codingmatters.poom.pack.handler.GetPackage;
import org.codingmatters.poom.pack.handler.SavePackage;
import org.codingmatters.poom.ci.service.PoomPackComposerProcessor;
import org.codingmatters.rest.api.Processor;


public class PoomPackComposerApi {

    private final String name = "poom-pack-composer";
    private PoomPackComposerHandlers handlers;
    private PoomPackComposerProcessor processor;

    public PoomPackComposerApi( String repositoryPath, String serviceUrl, String api_key ) {
        this.handlers = new PoomPackComposerHandlers.Builder()
                .packagesGetHandler( new GetPackage( repositoryPath, serviceUrl ) )
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

