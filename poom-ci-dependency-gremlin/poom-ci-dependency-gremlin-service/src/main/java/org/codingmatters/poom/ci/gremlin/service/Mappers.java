package org.codingmatters.poom.ci.gremlin.service;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.codingmatters.poom.ci.dependency.api.types.Module;
import org.codingmatters.poom.ci.dependency.api.types.Repository;

import java.util.List;
import java.util.Map;

public class Mappers {
    public static Repository repository(Map<String, List<VertexProperty>> map) {
        return Repository.builder()
                .id(singlePropertyValue(map, "repository-id"))
                .checkoutSpec(singlePropertyValue(map, "checkout-spec"))
                .name(singlePropertyValue(map, "name"))
                .build();
    }

    public static Module module(Map<String, List<VertexProperty>> map) {
        return Module.builder()
                .spec(singlePropertyValue(map, "spec"))
                .version(singlePropertyValue(map, "version"))
                .build();
    }

    public static String singlePropertyValue(Map<String, List<VertexProperty>> map, String prop) {
        return map.get(prop) != null && ! map.get(prop).isEmpty() ? (String) map.get(prop).get(0).value() : null;
    }
}
