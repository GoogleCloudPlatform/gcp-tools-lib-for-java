/*
 * Copyright 2016 Google Inc.
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

package com.google.cloud.tools.appengine.cloudsdk;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.deploy.AppEngineFlexibleStaging;
import com.google.cloud.tools.appengine.api.deploy.StageFlexibleConfiguration;
import com.google.cloud.tools.io.FileUtil;
import com.google.cloud.tools.project.AppYaml;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Cloud SDK based implementation of {@link AppEngineFlexibleStaging}.
 */
public class CloudSdkAppEngineFlexibleStaging implements AppEngineFlexibleStaging {

  private static Logger log = Logger.getLogger(CloudSdkAppEngineFlexibleStaging.class.getName());

  protected static final Set<String> APP_ENGINE_CONFIG_FILES_WHITELIST = ImmutableSet.of("app.yaml",
      "cron.yaml", "queue.yaml", "dispatch.yaml", "index.yaml", "dos.yaml", "swagger.json",
      "swagger.yaml");

  /**
   * Stages a Java JAR/WAR App Engine Flexible Environment application to be deployed.
   *
   * <p>Copies app.yaml, Dockerfile and the application artifact to the staging area.
   *
   * <p>If app.yaml or Dockerfile do not exist, gcloud cloud will create them during deployment.
   */
  @Override
  public void stageFlexible(StageFlexibleConfiguration config) throws AppEngineException {
    Preconditions.checkNotNull(config);
    Preconditions.checkNotNull(config.getStagingDirectory());
    Preconditions.checkNotNull(config.getArtifact());

    if (!config.getStagingDirectory().exists()) {
      throw new AppEngineException("Staging directory does not exist. Location: "
          + config.getStagingDirectory().toPath());
    }
    if (!config.getStagingDirectory().isDirectory()) {
      throw new AppEngineException("Staging location is not a directory. Location: "
          + config.getStagingDirectory().toPath());
    }

    // verification for app.yaml that contains runtime:java
    Path appYaml = config.getAppEngineDirectory().toPath().resolve("app.yaml");
    String runtime = "non-determined";
    try {
      if (Files.isRegularFile(appYaml)) {
        runtime = new AppYaml(appYaml).getRuntime();
      }
    } catch (IOException e) {
      log.warning("Unable to determine runtime: error parsing app.yaml");
    }

    try {

      if (config.getDockerDirectory() != null && config.getDockerDirectory().exists()) {
        if (runtime.equals("java")) {
          log.warning("WARNING: Runtime 'java' detected, any docker configuration in "
              + config.getDockerDirectory() + " will be ignored.");
        } else {
          // Copy docker context to staging
          if (!Files.isRegularFile(config.getDockerDirectory().toPath().resolve("Dockerfile"))) {
            throw new AppEngineException("Docker directory " + config.getDockerDirectory().toPath()
                + " does not contain Dockerfile");
          } else {
            FileUtil.copyDirectory(config.getDockerDirectory().toPath(),
                config.getStagingDirectory().toPath());
          }
        }
      }

      // Copy app.yaml and other App Engine config files to staging
      String[] appEngineConfigFiles = config.getAppEngineDirectory().list();
      if (appEngineConfigFiles != null) {
        for (String configFile : appEngineConfigFiles) {
          if (APP_ENGINE_CONFIG_FILES_WHITELIST.contains(configFile)) {
            Files.copy(config.getAppEngineDirectory().toPath().resolve(configFile),
                config.getStagingDirectory().toPath().resolve(configFile),
                REPLACE_EXISTING);
          } else if (configFile.equals("Dockerfile")) {
            throw new AppEngineException("Found 'Dockerfile' in the App Engine directory."
                + " Please move it to the Docker directory.");
          } else {
            throw new AppEngineException("Found an unexpected '" + configFile
                + "' file in the App Engine directory.");
          }
        }
      }

      // Copy the JAR/WAR file to staging.
      if (config.getArtifact() != null && config.getArtifact().exists()) {
        Path destination = config.getStagingDirectory().toPath()
            .resolve(config.getArtifact().toPath().getFileName());
        Files.copy(config.getArtifact().toPath(), destination, REPLACE_EXISTING);
      } else {
        throw new AppEngineException("Artifact doesn't exist at '" + config.getArtifact().getPath()
            + "'");
      }
    } catch (IOException e) {
      throw new AppEngineException(e);
    }
  }
}
