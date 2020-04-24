package org.codingmatters.poom.ci.gremlin.queries;

import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public interface Schema {

    class ModuleSpec {

        static ModuleSpec from(Map<String, List<VertexProperty>> map) {
            return new ModuleSpec(
                    (String) map.get("spec").get(0).value(),
                    (String) map.get("version").get(0).value()
            );
        }

        public final String spec;
        public final String version;

        public ModuleSpec(String spec, String version) {
            this.spec = spec;
            this.version = version;
        }

        @Override
        public String toString() {
            return "ModuleSpec{" +
                    "spec='" + spec + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ModuleSpec that = (ModuleSpec) o;
            return Objects.equals(spec, that.spec) &&
                    Objects.equals(version, that.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(spec, version);
        }
    }

}
