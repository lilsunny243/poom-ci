package org.codingmatters.poom.services.support;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


public class ArgumentsTest {

    @Test
    public void givenCounting__whenEmptyArgs__then0Argument_and0Options() throws Exception {
        Arguments args = Arguments.from();
        assertThat(args.argumentCount(), is(0));
        assertThat(args.optionCount(), is(0));
    }

    @Test
    public void givenGettingArguments__whenEmptyArgs__thenEmptyList() throws Exception {
        Arguments args = Arguments.from();
        assertThat(args.arguments(), is(empty()));
    }

    @Test
    public void givenCounting__whenTwoPlainArgs__then2Arguments_and0Option() throws Exception {
        Arguments args = Arguments.from("arg1", "arg2");
        assertThat(args.argumentCount(), is(2));
        assertThat(args.optionCount(), is(0));
    }

    @Test
    public void givenGettingArguments__whenTwoPlainArgs__thenArgsareListed() throws Exception {
        Arguments args = Arguments.from("arg1", "arg2");
        assertThat(args.arguments(), contains("arg1", "arg2"));
    }

    @Test
    public void givenCounting__whenOptWithoutValue__then0Argument_and1Option() throws Exception {
        Arguments args = Arguments.from("--opt");
        assertThat(args.argumentCount(), is(0));
        assertThat(args.optionCount(), is(1));
    }

    @Test
    public void givenGettingOption__whenOptWithoutValue__thenOptionIsPresentAndValueIsNull() throws Exception {
        Arguments args = Arguments.from("--opt");
        assertThat(args.option("opt").get(), is(nullValue()));
        assertThat(args.option("opt").isPresent(), is(true));
    }

    @Test
    public void givenCounting__whenOptWithValue__then0Argument_and1Option() throws Exception {
        Arguments args = Arguments.from("--opt", "opt value");
        assertThat(args.argumentCount(), is(0));
        assertThat(args.optionCount(), is(1));
    }

    @Test
    public void givenGettingOption__whenOptWithValue__thenOptionIsPresentAndValueIsSet() throws Exception {
        Arguments args = Arguments.from("--opt", "opt value");
        assertThat(args.option("opt").get(), is("opt value"));
        assertThat(args.option("opt").isPresent(), is(true));
    }

    @Test
    public void givenGettingOption__whenEmpty__thenOptionIsPresentAndValueIsNull() throws Exception {
        Arguments args = Arguments.from();
        assertThat(args.option("opt").get(), is(nullValue()));
        assertThat(args.option("opt").isPresent(), is(false));
    }


    @Test
    public void givenGettingAndCounting__whenMixed__thenAllOk() throws Exception {
        Arguments args = Arguments.from("arg1", "--opt1", "--opt2", "val2", "arg2");

        assertThat(args.argumentCount(), is(2));
        assertThat(args.arguments(), contains("arg1", "arg2"));
        assertThat(args.optionCount(), is(2));
        assertThat(args.option("opt1").isPresent(), is(true));
        assertThat(args.option("opt1").get(), is(nullValue()));
        assertThat(args.option("opt2").isPresent(), is(true));
        assertThat(args.option("opt2").get(), is("val2"));
    }
}