//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package org.apache.cloudstack.utils.process;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.joda.time.Duration;

import com.cloud.utils.Ternary;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

public final class ProcessRunner {
    public static final Logger LOG = Logger.getLogger(ProcessRunner.class);

    // Default maximum timeout of 5 minutes for any command
    public static final Duration DEFAULT_MAX_TIMEOUT = new Duration(5 * 60 * 1000);
    private final ExecutorService executor;
    private final List<Ternary<String, String, String>> commandLogReplacements = new ArrayList<>();

    String removeCommandSensitiveInfoForLogging(String command) {
        String commandLog = command.trim();

        for (Ternary<String, String, String> replacement : commandLogReplacements) {
            if (commandLog.contains(replacement.first())) {
                Pattern pattern = Pattern.compile(replacement.second());
                Matcher matcher = pattern.matcher(commandLog);
                if (matcher.find()) {
                    commandLog = matcher.replaceAll(replacement.third());
                }
            }
        }
        return commandLog;
    }

    public ProcessRunner(ExecutorService executor) {
        this.executor = executor;
        commandLogReplacements.add(new Ternary<>("ipmitool", "-P\\s+\\S+", "-P *****"));
    }

    /**
     * Executes a process with provided list of commands with a max default timeout
     * of 5 minutes
     * @param commands list of string commands
     * @return returns process result
     */
    public ProcessResult executeCommands(final List<String> commands) {
        return executeCommands(commands, DEFAULT_MAX_TIMEOUT);
    }

    /**
     * Executes a process with provided list of commands with a given timeout that is less
     * than or equal to DEFAULT_MAX_TIMEOUT
     * @param commands list of string commands
     * @param timeOut timeout duration
     * @return returns process result
     */
    public ProcessResult executeCommands(final List<String> commands, final Duration timeOut) {
        Preconditions.checkArgument(commands != null && timeOut != null
                && timeOut.getStandardSeconds() > 0L
                && (timeOut.compareTo(DEFAULT_MAX_TIMEOUT) <= 0)
                && executor != null);

        int retVal = -2;
        String stdOutput = null;
        String stdError = null;
        String commandLog = removeCommandSensitiveInfoForLogging(StringUtils.join(commands, " "));

        try {
            LOG.debug(String.format("Preparing command [%s] to execute.", commandLog));
            final Process process = new ProcessBuilder().command(commands).start();

            LOG.debug(String.format("Submitting command [%s].", commandLog));
            final Future<Integer> processFuture = executor.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return process.waitFor();
                }
            });
            try {
                LOG.debug(String.format("Waiting for a response from command [%s]. Defined timeout: [%s].", commandLog, timeOut.getStandardSeconds()));
                retVal = processFuture.get(timeOut.getStandardSeconds(), TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                LOG.warn(String.format("Failed to complete the requested command [%s] due to execution error.", commands), e);
                retVal = -2;
                stdError = e.getMessage();
            } catch (TimeoutException e) {
                LOG.warn(String.format("Failed to complete the requested command [%s] within timeout. Defined timeout: [%s].", commandLog, timeOut.getStandardSeconds()), e);
                retVal = -1;
                stdError = "Operation timed out, aborted.";
            } finally {
                if (StringUtils.isEmpty(stdError)) {
                    stdOutput = CharStreams.toString(new InputStreamReader(process.getInputStream()));
                    stdError = CharStreams.toString(new InputStreamReader(process.getErrorStream()));
                }
                process.destroy();
            }

            LOG.debug(String.format("Process standard output for command [%s]: [%s].", commandLog, stdOutput));
            LOG.debug(String.format("Process standard error output command [%s]: [%s].", commandLog, stdError));
        } catch (IOException | InterruptedException e) {
            LOG.error(String.format("Exception caught error running command [%s].", commandLog), e);
            stdError = e.getMessage();
        }
        return new ProcessResult(stdOutput, stdError, retVal);
    }
}
