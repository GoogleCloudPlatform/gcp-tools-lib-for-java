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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.api.devserver.DefaultRunConfiguration;
import com.google.cloud.tools.appengine.cloudsdk.internal.process.ProcessRunnerException;
import com.google.cloud.tools.test.utils.LogStoringHandler;
import com.google.cloud.tools.test.utils.SpyVerifier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Unit tests for {@link CloudSdkAppEngineDevServer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudSdkAppEngineDevServer1Test {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();
  private Path fakeJavaSdkHome;
  private File fakeStoragePath;
  private File fakeDatastorePath;

  private LogStoringHandler testHandler;
  @Mock
  private CloudSdk sdk;

  private CloudSdkAppEngineDevServer1 devServer;

  private final Path java8Service = Paths.get("src/test/resources/projects/EmptyStandard8Project");
  private final Path java7Service = Paths.get("src/test/resources/projects/EmptyStandard7Project");


  @Before
  public void setUp() throws IOException {
    devServer = Mockito.spy(new CloudSdkAppEngineDevServer1(sdk));
    fakeJavaSdkHome = temporaryFolder.newFolder("java-sdk").toPath();
    fakeStoragePath = new File("storage/path");
    fakeDatastorePath = temporaryFolder.newFile("datastore.db");

    Mockito.when(sdk.getJavaAppEngineSdkPath()).thenReturn(fakeJavaSdkHome);

    testHandler = LogStoringHandler.getForLogger(CloudSdkAppEngineDevServer1.class.getName());
  }

  @Test
  public void tesNullSdk() {
    try {
      new CloudSdkAppEngineDevServer1(null);
      Assert.fail("Allowed null SDK");
    } catch (NullPointerException expected) {
    }
  }

  @Test
  public void testPrepareCommand_allFlags() throws Exception {

    DefaultRunConfiguration configuration = Mockito.spy(new DefaultRunConfiguration());

    configuration.setServices(ImmutableList.of(java8Service));
    configuration.setHost("host");
    configuration.setPort(8090);
    configuration.setJvmFlags(ImmutableList.of("-Dflag1", "-Dflag2"));
    configuration.setDefaultGcsBucketName("buckets");
    configuration.setDatastorePath(fakeDatastorePath);
    configuration.setClearDatastore(true);

    // these params are not used by devappserver1 and will log warnings
    configuration.setAdminHost("adminHost");
    configuration.setAdminPort(8000);
    configuration.setAuthDomain("example.com");
    configuration.setStoragePath(fakeStoragePath);
    configuration.setLogLevel("debug");
    configuration.setMaxModuleInstances(3);
    configuration.setUseMtimeFileWatcher(true);
    configuration.setThreadsafeOverride("default:False,backend:True");
    configuration.setPythonStartupScript("script.py");
    configuration.setPythonStartupArgs("arguments");
    configuration.setRuntime("someRuntime");
    configuration.setCustomEntrypoint("entrypoint");
    configuration.setAllowSkippedFiles(true);
    configuration.setApiPort(8091);
    configuration.setAutomaticRestart(false);
    configuration.setDevAppserverLogLevel("info");
    configuration.setSkipSdkUpdateCheck(true);

    SpyVerifier.newVerifier(configuration).verifyDeclaredSetters();

    List<String> expectedFlags = ImmutableList.of("--address=host", "--port=8090",
        "--default_gcs_bucket=buckets", "--allow_remote_shutdown", "--disable_update_check",
        "--no_java_agent", java8Service.toString());

    List<String> expectedJvmArgs = ImmutableList.of("-Dflag1", "-Dflag2",
        "-Ddatastore.backing_store=" + fakeDatastorePath, "-Duse_jetty9_runtime=true",
        "-D--enable_all_permissions=true");

    devServer.run(configuration);

    verify(sdk, times(1)).runDevAppServer1Command(expectedJvmArgs, expectedFlags);

    SpyVerifier.newVerifier(configuration)
        .verifyDeclaredGetters(ImmutableMap.of("getServices", 4, "getJavaHomeDir", 2, "getJvmFlags", 2));

    // verify we are checking and ignoring these parameters
    verify(devServer, times(17)).checkAndWarnIgnored(Mockito.any(), Mockito.anyString());
    verify(devServer).checkAndWarnIgnored(configuration.getAdminHost(), "adminHost");
    verify(devServer).checkAndWarnIgnored(configuration.getAdminPort(), "adminPort");
    verify(devServer).checkAndWarnIgnored(configuration.getAuthDomain(), "authDomain");
    verify(devServer).checkAndWarnIgnored(configuration.getStoragePath(), "storagePath");
    verify(devServer).checkAndWarnIgnored(configuration.getLogLevel(), "logLevel");
    verify(devServer).checkAndWarnIgnored(configuration.getMaxModuleInstances(), "maxModuleInstances");
    verify(devServer).checkAndWarnIgnored(configuration.getUseMtimeFileWatcher(), "useMtimeFileWatcher");
    verify(devServer).checkAndWarnIgnored(configuration.getThreadsafeOverride(), "threadsafeOverride");
    verify(devServer).checkAndWarnIgnored(configuration.getPythonStartupScript(), "pythonStartupScript");
    verify(devServer).checkAndWarnIgnored(configuration.getPythonStartupArgs(), "pythonStartupArgs");
    verify(devServer).checkAndWarnIgnored(configuration.getRuntime(), "runtime");
    verify(devServer).checkAndWarnIgnored(configuration.getCustomEntrypoint(), "customEntrypoint");
    verify(devServer).checkAndWarnIgnored(configuration.getAllowSkippedFiles(), "allowSkippedFiles");
    verify(devServer).checkAndWarnIgnored(configuration.getApiPort(), "apiPort");
    verify(devServer).checkAndWarnIgnored(configuration.getAutomaticRestart(), "automaticRestart");
    verify(devServer).checkAndWarnIgnored(configuration.getDevAppserverLogLevel(), "devAppserverLogLevel");
    verify(devServer).checkAndWarnIgnored(configuration.getSkipSdkUpdateCheck(), "skipSdkUpdateCheck");
  }

  @Test
  public void testPrepareCommand_booleanFlags() throws AppEngineException, ProcessRunnerException {
    DefaultRunConfiguration configuration = new DefaultRunConfiguration();

    configuration.setServices(ImmutableList.of(java8Service));

    List<String> expectedFlags = ImmutableList.of("--allow_remote_shutdown",
        "--disable_update_check", "--no_java_agent", java8Service.toString());
    List<String> expectedJvmArgs = ImmutableList.of("-Duse_jetty9_runtime=true",
            "-D--enable_all_permissions=true");
    devServer.run(configuration);
    verify(sdk, times(1)).runDevAppServer1Command(expectedJvmArgs, expectedFlags);
  }

  @Test
  public void testPrepareCommand_noFlags() throws AppEngineException, ProcessRunnerException {

    DefaultRunConfiguration configuration = new DefaultRunConfiguration();
    configuration.setServices(ImmutableList.of(java8Service));

    List<String> expectedFlags = ImmutableList.of("--allow_remote_shutdown",
        "--disable_update_check", "--no_java_agent", java8Service.toString());

    List<String> expectedJvmArgs = ImmutableList.of("-Duse_jetty9_runtime=true",
            "-D--enable_all_permissions=true");

    devServer.run(configuration);

    verify(sdk, times(1)).runDevAppServer1Command(expectedJvmArgs, expectedFlags);
  }

  @Test
  public void testPrepareCommand_noFlagsJava7() throws AppEngineException, ProcessRunnerException {

    DefaultRunConfiguration configuration = new DefaultRunConfiguration();
    configuration.setServices(ImmutableList.of(java7Service));

    List<String> expectedFlags = ImmutableList.of("--allow_remote_shutdown",
        "--disable_update_check", java7Service.toString());
    List<String> expectedJvmArgs = ImmutableList
        .of("-javaagent:" + fakeJavaSdkHome.resolve("agent/appengine-agent.jar").toAbsolutePath()
            .toString());

    devServer.run(configuration);

    verify(sdk, times(1)).runDevAppServer1Command(expectedJvmArgs, expectedFlags);
  }

  @Test
  public void testPrepareCommand_noFlagsMultiModule() throws AppEngineException, ProcessRunnerException {

    DefaultRunConfiguration configuration = new DefaultRunConfiguration();
    configuration.setServices(ImmutableList.of(java7Service, java8Service));

    List<String> expectedFlags = ImmutableList.of("--allow_remote_shutdown",
        "--disable_update_check", "--no_java_agent", java7Service.toString(),
        java8Service.toString());

    List<String> expectedJvmArgs = ImmutableList.of("-Duse_jetty9_runtime=true",
        "-D--enable_all_permissions=true");

    devServer.run(configuration);

    verify(sdk, times(1)).runDevAppServer1Command(expectedJvmArgs, expectedFlags);
  }

  @Test
  public void testCheckAndWarnIgnored_withSetValue() {
    devServer.checkAndWarnIgnored(new Object(), "testName");

    Assert.assertEquals(1, testHandler.getLogs().size());

    LogRecord logRecord = testHandler.getLogs().get(0);
    Assert.assertEquals("testName only applies to Dev Appserver v2 and will be ignored by Dev Appserver v1", logRecord.getMessage());
    Assert.assertEquals(Level.WARNING, logRecord.getLevel());
  }

  @Test
  public void testCheckAndWarnIgnored_withUnsetValue() {
    devServer.checkAndWarnIgnored(null, "testName");

    Assert.assertEquals(0, testHandler.getLogs().size());
  }

  @Test
  public void testDetermineJavaRuntime_noWarningsJava7() {
    Assert.assertFalse(devServer.isJava8(ImmutableList.of(java7Service)));
    Assert.assertEquals(0, testHandler.getLogs().size());
  }

  @Test
  public void testDetermineJavaRuntime_noWarningsJava7Multiple() {
    Assert.assertFalse(devServer.isJava8(ImmutableList.of(java7Service, java7Service)));
    Assert.assertEquals(0, testHandler.getLogs().size());
  }

  @Test
  public void testDetermineJavaRuntime_noWarningsJava8() {
    Assert.assertTrue(devServer.isJava8(ImmutableList.of(java8Service)));
    Assert.assertEquals(0, testHandler.getLogs().size());
  }

  @Test
  public void testDetermineJavaRuntime_noWarningsJava8Multiple() {
    Assert.assertTrue(devServer.isJava8(ImmutableList.of(java8Service, java8Service)));
    Assert.assertEquals(0, testHandler.getLogs().size());
  }

  @Test
  public void testDetermineJavaRuntime_mixedModeWarning() {

    Assert.assertTrue(devServer.isJava8(ImmutableList.of(java8Service, java7Service)));
    Assert.assertEquals(1, testHandler.getLogs().size());

    LogRecord logRecord = testHandler.getLogs().get(0);
    Assert.assertEquals("Mixed runtimes java7/java8 detected, will use java8 settings", logRecord.getMessage());
    Assert.assertEquals(Level.WARNING, logRecord.getLevel());
  }

  @Test
  public void testHandleDatastoreFlags_setAndDoNotClear() {
    List<String> jvmArgs = new ArrayList<>();
    Assert.assertTrue(fakeDatastorePath.exists());

    devServer.handleDatastoreFlags(jvmArgs, fakeDatastorePath, false);

    Assert.assertEquals(jvmArgs, ImmutableList.of("-Ddatastore.backing_store=" + fakeDatastorePath));
    Assert.assertTrue(fakeDatastorePath.exists());
  }

  @Test
  public void testHandleDatastoreFlags_setAndNullClear() {
    List<String> jvmArgs = new ArrayList<>();
    Assert.assertTrue(fakeDatastorePath.exists());
    devServer.handleDatastoreFlags(jvmArgs, fakeDatastorePath, null);

    Assert.assertEquals(jvmArgs, ImmutableList.of("-Ddatastore.backing_store=" + fakeDatastorePath));
    Assert.assertTrue(fakeDatastorePath.exists());
  }

  @Test
  public void testHandleDatastoreFlags_setAndClear() {
    List<String> jvmArgs = new ArrayList<>();
    Assert.assertTrue(fakeDatastorePath.exists());

    devServer.handleDatastoreFlags(jvmArgs, fakeDatastorePath, true);

    Assert.assertEquals(jvmArgs, ImmutableList.of("-Ddatastore.backing_store=" + fakeDatastorePath));
    Assert.assertFalse(fakeDatastorePath.exists());
  }

  @Test
  public void testHandleDatastoreFlags_unSetandClear() {
    List<String> jvmArgs = new ArrayList<>();
    devServer.handleDatastoreFlags(jvmArgs, null, false);
    Assert.assertEquals(0, jvmArgs.size());

    Assert.assertEquals(1, testHandler.getLogs().size());
    LogRecord logRecord = testHandler.getLogs().get(0);
    Assert.assertEquals("'clearDatastore' flag does not apply unless 'datastorePath' is specified for Dev Appserver v1", logRecord.getMessage());
    Assert.assertEquals(Level.WARNING, logRecord.getLevel());
  }

  @Test
  public void testHandleDatastoreFlags_unSetandDoNotClear() {
    List<String> jvmArgs = new ArrayList<>();
    devServer.handleDatastoreFlags(jvmArgs, null, false);
    Assert.assertEquals(0, jvmArgs.size());

    Assert.assertEquals(1, testHandler.getLogs().size());
    LogRecord logRecord = testHandler.getLogs().get(0);
    Assert.assertEquals("'clearDatastore' flag does not apply unless 'datastorePath' is specified for Dev Appserver v1", logRecord.getMessage());
    Assert.assertEquals(Level.WARNING, logRecord.getLevel());
  }

  @Test
  public void testHandleDatastoreFlags_unSetandNullClear() {
    List<String> jvmArgs = new ArrayList<>();
    devServer.handleDatastoreFlags(jvmArgs, null, null);
    Assert.assertEquals(0, jvmArgs.size());

    Assert.assertEquals(0, testHandler.getLogs().size());
  }
}
