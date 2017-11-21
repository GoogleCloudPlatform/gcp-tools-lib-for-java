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

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/** Executes a shell command. */
public class ProcessExecutor {

  private ProcessBuilderFactory processBuilderFactory = new ProcessBuilderFactory();

  @VisibleForTesting
  ProcessExecutor setProcessBuilderFactory(ProcessBuilderFactory processBuilderFactory) {
    this.processBuilderFactory = processBuilderFactory;
    return this;
  }

  @VisibleForTesting
  static class ProcessBuilderFactory {
    ProcessBuilder createProcessBuilder() {
      return new ProcessBuilder();
    }
  }

  /**
   * Runs the command.
   *
   * @param command the list of command line tokens
   * @return exitcode from the process
   */
  public int run(
      List<String> command,
      Path workingDirectory,
      Map<String, String> environment,
      AsyncStreamHandler stdout,
      AsyncStreamHandler stderr)
      throws IOException, ExecutionException {

    // Builds the command to execute.
    ProcessBuilder processBuilder = processBuilderFactory.createProcessBuilder();
    processBuilder.command(command);
    if (workingDirectory != null) {
      processBuilder.directory(workingDirectory.toFile());
    }
    if (environment != null) {
      processBuilder.environment().putAll(environment);
    }
    final Process process = processBuilder.start();

    stdout.handleStream(process.getInputStream());
    stderr.handleStream(process.getErrorStream());

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException ex) {
      process.destroy();
      throw new ExecutionException("Process cancelled.", ex);
    }

    return exitCode;
  }
}