package org.codingmatters.poom.pack.handler.pack;

import org.codingmatters.poom.ci.api.types.JsonPackage;
import org.codingmatters.value.objects.values.ObjectValue;

import java.io.File;
import java.io.FileFilter;

public class JsonPackageBuilder {
    private final File repository;
    private final String serviceUrl;

    public JsonPackageBuilder( File repository, String serviceUrl ) {
        this.repository = repository;
        this.serviceUrl = serviceUrl;
    }

    public JsonPackage buildPackage() {
        FileFilter directories = File::isDirectory;
        File[] vendors = repository.listFiles( directories );
        ObjectValue.Builder packages = ObjectValue.builder();
        if( vendors != null ){
            for( File vendor : vendors ){
                File[] packageNames = vendor.listFiles( directories );
                if( packageNames != null ){
                    for( File packageName : packageNames ){
                        ObjectValue.Builder vendorPackage = ObjectValue.builder();
                        String packageFullName = vendor.getName() + "/" + packageName.getName();
                        File[] versions = packageName.listFiles( directories );
                        if( versions != null ){
                            ObjectValue.Builder composer = ObjectValue.builder();
                            for( File version : versions ){
                                String expectedName = packageName.getName() + "-" + version.getName() + ".zip";
                                String[] composerFile = version.list( ( dir, name ) -> name.equals( expectedName ) );
                                String versionName = version.getName();
                                if( composerFile != null && composerFile.length == 1 ){
                                    composer.property( "name", name -> name.stringValue( packageFullName ) )
                                            .property( "version", packVersion -> packVersion.stringValue( versionName ) )
                                            .property( "dist", name -> name.objectValue( ObjectValue.builder()
                                                    .property( "url", url -> url.stringValue( serviceUrl + "/" + String.join( "/", packageFullName, versionName, packageName.getName() + "-" + versionName + ".zip" ) ) )
                                                    .property( "type", type -> type.stringValue( "zip" ) )
                                                    .build() ) )
                                            .property( "autoload", autoload -> autoload.objectValue(
                                                    obj -> obj.property( "psr-4", psr -> psr.objectValue(
                                                            ps -> ps.property( "io\\", io -> io.stringValue( "io/" ) )
                                                    ) )
                                            ) )
                                    ;

                                }
                                vendorPackage.property( versionName, v -> v.objectValue( composer.build() ) );
                            }
                            packages.property( packageFullName, p -> p.objectValue( vendorPackage.build() ) );
                        }
                    }
                }
            }
        }
        JsonPackage.Builder jsonPack = JsonPackage.builder();
        jsonPack.packages( packages.build() );
        return jsonPack.build();
    }
}
