package org.codingmatters.poom.pack;


import org.codingmatters.poom.ci.api.PackagesGetRequest;
import org.codingmatters.poom.ci.api.packagesgetresponse.Status200;
import org.codingmatters.poom.pack.handler.GetPackage;
import org.codingmatters.poom.pack.handler.pack.JsonPackageBuilder;
import org.codingmatters.value.objects.values.ObjectValue;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class GetPackageTest {

    @Test
    public void testGetComposerJson() {
        String repositoryPath = Thread.currentThread().getContextClassLoader().getResource( "repository" ).getPath();
        final File repository = new File( repositoryPath );
        GetPackage getPackage = new GetPackage( repository, "http://service:456" );
        Status200 response = getPackage.apply(
                PackagesGetRequest.builder()
                        .build()
        ).opt().status200().orElseThrow( ()->new AssertionError( "Cannot get packages" ) );

        ObjectValue expected = ObjectValue.builder()
                .property( "vendor-1/package-1", o->o.objectValue( v1->v1
                        .property( "1.0", v->v.objectValue(
                                c->c
                                        .property( "name", n->n.stringValue( "vendor-1/package-1" ) )
                                        .property( "version", version->version.stringValue( "1.0" ) )
                                        .property( "dist", dist->dist.objectValue(
                                                d->d
                                                        .property( "url", url->url.stringValue( "http://service:456/vendor-1/package-1/1.0/package-1-1.0.zip" ) )
                                                        .property( "type", type->type.stringValue( "zip" ) )
                                        ) )
                                        .property( "autoload", auto->auto.objectValue(
                                                a->a.property( "psr-4", psr->psr.objectValue(
                                                        p->p.property( "io\\", io->io.stringValue( "io/" ) )
                                                ) )
                                        ))
                        ) )
                        .property( "2.0-SNAPSHOT", v->v.objectValue(
                                c->c
                                        .property( "name", n->n.stringValue( "vendor-1/package-1" ) )
                                        .property( "version", version->version.stringValue( "2.0-SNAPSHOT" ) )
                                        .property( "dist", dist->dist.objectValue(
                                                d->d
                                                        .property( "url", url->url.stringValue( "http://service:456/vendor-1/package-1/2.0-SNAPSHOT/package-1-2.0-SNAPSHOT.zip" ) )
                                                        .property( "type", type->type.stringValue( "zip" ) )
                                        ) )
                                        .property( "autoload", auto->auto.objectValue(
                                                a->a.property( "psr-4", psr->psr.objectValue(
                                                        p->p.property( "io\\", io->io.stringValue( "io/" ) )
                                                ) )
                                        ))
                                )
                        )
                ) )
                .property( "vendor-1/package-2", o->o.objectValue( v1->v1
                        .property( "1.0.0-SNAPSHOT", v->v.objectValue(
                                c->c
                                        .property( "name", n->n.stringValue( "vendor-1/package-2" ) )
                                        .property( "version", version->version.stringValue( "1.0.0-SNAPSHOT" ) )
                                        .property( "dist", dist->dist.objectValue(
                                                d->d
                                                        .property( "url", url->url.stringValue( "http://service:456/vendor-1/package-2/1.0.0-SNAPSHOT/package-2-1.0.0-SNAPSHOT.zip" ) )
                                                        .property( "type", type->type.stringValue( "zip" ) )
                                        ) )
                                        .property( "autoload", auto->auto.objectValue(
                                                a->a.property( "psr-4", psr->psr.objectValue(
                                                        p->p.property( "io\\", io->io.stringValue( "io/" ) )
                                                ) )
                                        ))
                        ) )
                ) )
                .property( "vendor-2/package-1", o->o.objectValue( v1->v1
                        .property( "1.0", v->v.objectValue(
                                c->c
                                        .property( "name", n->n.stringValue( "vendor-2/package-1" ) )
                                        .property( "version", version->version.stringValue( "1.0" ) )
                                        .property( "dist", dist->dist.objectValue(
                                                d->d
                                                        .property( "url", url->url.stringValue( "http://service:456/vendor-2/package-1/1.0/package-1-1.0.zip" ) )
                                                        .property( "type", type->type.stringValue( "zip" ) )
                                        ) )
                                        .property( "autoload", auto->auto.objectValue(
                                                a->a.property( "psr-4", psr->psr.objectValue(
                                                        p->p.property( "io\\", io->io.stringValue( "io/" ) )
                                                ) )
                                        ))
                        ) )
                ) )
                .build();

        assertThat( response.payload().packages(), is( expected ) );

    }
}

