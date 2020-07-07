package org.codingmatters.poom.pack.handler.pack;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.codingmatters.poom.ci.api.types.JsonPackage;
import org.codingmatters.poom.ci.api.types.json.JsonPackageWriter;
import org.codingmatters.poom.services.logging.CategorizedLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class JsonPackager {

    private static final CategorizedLogger log = CategorizedLogger.getLogger( JsonPackager.class );
    private static final AtomicReference<File> file = new AtomicReference<>( null );

    public static File getFile( File repository, String serviceUrl ) throws IOException {
        synchronized( file ) {
            if( file.get() == null || !file.get().exists() ){
                buildFile( repository, serviceUrl );
            }
            return file.get();
        }
    }

    public static void buildFile( File repository, String serviceUrl ) throws IOException {
        synchronized( file ) {
            long start = System.currentTimeMillis();
            JsonPackage jsonPackage = new JsonPackageBuilder( repository, serviceUrl ).buildPackage();
            long durationMs = System.currentTimeMillis() - start;
            log.info( "Building package file tooks " + durationMs + " ms" );
            File tempFile = File.createTempFile( "poom-pack-package-", ".json" );
            try( JsonGenerator generator = new JsonFactory().createGenerator( new FileOutputStream( tempFile ) ) ){
                new JsonPackageWriter().write( generator, jsonPackage );
            }
            deleteFile();
            file.set( tempFile );
        }
    }

    public static void deleteFile() {
        synchronized( file ) {
            if( file.get() != null ){
                file.get().delete();
                file.set( null );
            }
        }
    }

}
