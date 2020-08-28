package org.codingmatters.poom.ci.apps.releaser.command;

import org.codingmatters.poom.ci.apps.releaser.command.exception.CommandFailed;
import org.codingmatters.poom.services.support.process.ProcessInvoker;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class CommandHelper {
    private final ProcessInvoker.OutputListener outputListener;
    private final ProcessInvoker.ErrorListener errorListener;

    public CommandHelper(ProcessInvoker.OutputListener outputListener, ProcessInvoker.ErrorListener errorListener) {
        this.outputListener = outputListener;
        this.errorListener = errorListener;
    }

    public void exec(ProcessBuilder processBuilder, String commandName) throws CommandFailed {
        executeWith(processBuilder, commandName, this.outputListener, this.errorListener);
    }

    public String[] execWithStdout(ProcessBuilder processBuilder, String commandName) throws CommandFailed {
        List<String> results = Collections.synchronizedList(new LinkedList<>());
        executeWith(processBuilder, commandName, line -> results.add(line), this.errorListener);
        return results.toArray(new String[0]);
    }

    static private void executeWith(ProcessBuilder processBuilder, String commandName, ProcessInvoker.OutputListener out, ProcessInvoker.ErrorListener err) throws CommandFailed {
        try {
            ProcessInvoker invoker = new ProcessInvoker();
            int status = invoker.exec(processBuilder, out, err);
            if (status != 0) {
                throw new CommandFailed(commandName + "failed with status " + status);
            }
        } catch (IOException e) {
            throw new CommandFailed(commandName + "failed", e);
        } catch (InterruptedException e) {
            throw new CommandFailed(commandName + "failed", e);
        }
    }
}
