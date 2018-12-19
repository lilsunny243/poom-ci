package org.codingmatters.poom.pack;

import io.undertow.Handlers;
import io.undertow.Undertow;
import org.codingmatters.poom.services.logging.CategorizedLogger;
import org.codingmatters.poom.services.support.Env;
import org.codingmatters.rest.undertow.CdmHttpUndertowHandler;

public class PoomPackComposerServices {

    static private final CategorizedLogger log = CategorizedLogger.getLogger( PoomPackComposerServices.class );
    private static final String REPOSITORY_PATH = "repository_path";
    private final String host;
    private final int port;
    private final PoomPackComposerApi api;
    private Undertow server;

    public PoomPackComposerServices( PoomPackComposerApi api, int port, String host ) {
        this.api = api;
        this.port = port;
        this.host = host;
    }


    public static void main( String[] args ) {
        String host = Env.mandatory( Env.SERVICE_HOST ).asString();
        int port = Env.mandatory( Env.SERVICE_PORT ).asInteger();

        PoomPackComposerServices service = new PoomPackComposerServices( api(), port, host );
        service.start();

        log.info( "poom-ci pipeline api service running" );
        while( true ) {
            try {
                Thread.sleep( 1000 );
            } catch( InterruptedException e ) {
                break;
            }
        }
        log.info( "poom-ci pipeline api service stopping..." );
        service.stop();
        log.info( "poom-ci pipeline api service stopped." );
    }

    private static PoomPackComposerApi api() {
        return new PoomPackComposerApi(
                Env.mandatory( REPOSITORY_PATH ).asString(),
                Env.mandatory( Env.SERVICE_URL ).asString(),
                Env.mandatory( "API_KEY" ).asString()
        );
    }


    public void start() {
        this.server = Undertow.builder()
                .addHttpListener( this.port, this.host )
                .setHandler( Handlers.path().addPrefixPath( api.path(), new CdmHttpUndertowHandler( this.api.processor() ) ) )
                .build();
        this.server.start();
    }

    public void stop() {
        this.server.stop();
    }

}

