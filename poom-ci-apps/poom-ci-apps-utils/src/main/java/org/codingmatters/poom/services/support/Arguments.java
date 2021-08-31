package org.codingmatters.poom.services.support;

import java.util.*;

public class Arguments {

    private final List<String> arguments;
    private final Map<String, OptionValue> options;

    private Arguments(List<String> arguments, Map<String, OptionValue> options) {
        this.arguments = arguments;
        this.options = options;
    }

    public int argumentCount() {
        return this.arguments.size();
    }

    public int optionCount() {
        return this.options.size();
    }

    static public Arguments from(String ... args) {
        return new Builder(args).build();
    }

    public List<String> arguments() {
        return this.arguments;
    }

    public OptionValue option(String name) {
        return this.options.getOrDefault(name, OptionValue.NONE);
    }

    @Override
    public String toString() {
        return "Arguments{" +
                "arguments=" + arguments +
                ", options=" + options +
                '}';
    }

    static private class Builder {
        private final List<String> arguments = new LinkedList<>();
        private final Map<String, OptionValue> options = new HashMap<>();

        public Builder(String[] args) {
            if(args != null) {
                boolean inOption = false;
                String currentOption = null;
                for (String arg : args) {
                    if(arg.startsWith("--")) {
                        inOption = true;
                        currentOption = arg.substring("--".length());
                        this.options.put(currentOption, OptionValue.EMPTY);
                    } else if(inOption) {
                        this.options.put(currentOption, new OptionValue(true, arg));
                        inOption = false;
                        currentOption = null;
                    } else {
                        this.arguments.add(arg);
                    }
                }
            }
        }

        public Arguments build() {
            return new Arguments(Collections.unmodifiableList(this.arguments), Collections.unmodifiableMap(this.options));
        }
    }

    static public class OptionValue {
        static public OptionValue NONE = new OptionValue(false, null);
        static public OptionValue EMPTY = new OptionValue(true, null);

        private final boolean present;
        private final String value;

        private OptionValue(boolean present, String value) {
            this.present = present;
            this.value = value;
        }

        public boolean isPresent() {
            return this.present;
        }

        public String get() {
            return this.value;
        }
    }
}
