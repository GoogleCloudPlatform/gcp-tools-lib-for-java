/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.tools.managedcloudsdk.process;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/** Execute a command and save and return stdout. */
public class CommandCaller {
  private final List<String> command;
  private final Path workingDirectory;
  private final Map<String, String> environment;
  private final CommandExecutorFactory commandExecutorFactory;
  private final AsyncStreamSaver stdOutListener;
  private final AsyncStreamSaver stdErrListener;

  private final Logger logger = Logger.getLogger(CommandCaller.class.getName());

  /**
   * Create a new Command Caller.
   *
   * @param command a command to run
   * @param workingDirectory the working directory to run in, can be {@code null}
   * @param environment map of environment variables, can be {@code null}
   */
  public CommandCaller(
      List<String> command,
      Path workingDirectory,
      Map<String, String> environment,
      CommandExecutorFactory commandExecutorFactory,
      AsyncStreamSaver stdOutListener,
      AsyncStreamSaver stdErrListener) {
    this.command = ImmutableList.copyOf(command);
    this.workingDirectory = workingDirectory;
    this.environment = environment;
    this.commandExecutorFactory = commandExecutorFactory;
    this.stdOutListener = stdOutListener;
    this.stdErrListener = stdErrListener;
  }

  /** Runs the command and returns stdout as a result. */
  public String call() throws IOException, ExecutionException, CommandExitException {
    CommandExecutor commandExecutor = commandExecutorFactory.newCommandExecutor();

    int exitCode =
        commandExecutor.run(command, workingDirectory, environment, stdOutListener, stdErrListener);
    if (exitCode != 0) {
      try {
        // only log stdErr if we encounter an error.
        logger.severe(stdErrListener.getResult().get());
      } catch (InterruptedException ignored) {
        // ignored
      }
      throw new CommandExitException("Process exited with non-zero exit code: " + exitCode);
    }
    try {
      return stdOutListener.getResult().get();
    } catch (InterruptedException ex) {
      throw new ExecutionException("Interrupted obtaining result.", ex);
    }
  }
}
