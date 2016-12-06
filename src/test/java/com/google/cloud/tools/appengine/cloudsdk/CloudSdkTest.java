package com.google.cloud.tools.appengine.cloudsdk;

import com.google.cloud.tools.appengine.api.AppEngineException;
import com.google.cloud.tools.appengine.cloudsdk.CloudSdk.Builder;
import com.google.cloud.tools.appengine.cloudsdk.process.ProcessOutputLineListener;
import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CloudSdk}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloudSdkTest {
  
  private Path root;
  private CloudSdk.Builder builder;

  @Mock
  private ProcessOutputLineListener outputListener;

  @Before
  public void setup() {
   root = Paths.get(Files.createTempDir().toString());
   builder = new CloudSdk.Builder().sdkPath(root);
  }

  private void writeVersionFile(String contents) throws IOException {
    Files.write(contents, root.resolve("VERSION").toFile(), Charset.defaultCharset());
  }

  @Test
  public void testGetSdkPath() {
    assertEquals(root, builder.build().getSdkPath());
  }
  
  @Test
  public void testValidateCloudSdk() {
    new CloudSdk.Builder().build().validateCloudSdk();
  }

  @Test
  public void testGetVersion_fileNotExists() throws IOException {
    try {
      builder.build().getVersion();
    } catch (CloudSdkOutOfDateException e) {
      assertEquals("Cloud SDK versions below " + CloudSdk.MINIMUM_VERSION
          + " are not supported by this library.", e.getMessage());
      return;
    }
    fail();
  }

  @Test
  public void testGetVersion_fileContentInvalid() throws IOException {
    writeVersionFile("invalid format");
    try {
      builder.build().getVersion();
    } catch (CloudSdkOutOfDateException e) {
      assertEquals("Cloud SDK versions below " + CloudSdk.MINIMUM_VERSION
          + " are not supported by this library.", e.getMessage());
      return;
    }
    fail();
  }

  @Test
  public void testGetVersion_fileContentValid() throws IOException {
    String version = "136.0.0";
    writeVersionFile(version);
    assertEquals(version, builder.build().getVersion().toString());
  }

  @Test
  public void testValidateAppEngineJavaComponents() {
    new CloudSdk.Builder().build().validateAppEngineJavaComponents();
  }
  
  @Test
  public void testGetWindowsPythonPath() {
    assertEquals("python", builder.build().getWindowsPythonPath().toString());
  }

  @Test
  public void testGetJavaAppEngineSdkPath() {
    assertEquals(root.resolve("platform/google_appengine/google/appengine/tools/java/lib"),
        builder.build().getJavaAppEngineSdkPath());
  }

  @Test
  public void testGetJarPathJavaTools() {
    assertEquals(root.resolve("platform/google_appengine/google/appengine"
        + "/tools/java/lib/appengine-tools-api.jar"),
        builder.build().getJarPath("appengine-tools-api.jar"));
  }

  @Test
  public void testNewCloudSdk_nullWaitingOutputListener() {
    CloudSdk sdk = builder
        .addStdOutLineListener(outputListener).runDevAppServerWait(10).async(false).build();

    assertNull(sdk.getRunDevAppServerWaitListener());

    sdk = builder.addStdOutLineListener(outputListener)
        .runDevAppServerWait(0).async(true).build();

    assertNull(sdk.getRunDevAppServerWaitListener());
  }

  @Test
  public void testNewCloudSdk_outListener() {
    builder.addStdOutLineListener(outputListener).runDevAppServerWait(10).async(true);

    CloudSdk sdk = builder.build();

    assertNotNull(sdk.getRunDevAppServerWaitListener());
    assertEquals(2, builder.getStdOutLineListeners().size());
    assertEquals(1, builder.getStdErrLineListeners().size());
    assertEquals(1, builder.getExitListeners().size());
  }

  @Test
  public void testNewCloudSdk_errListener() {
    builder.addStdErrLineListener(outputListener).runDevAppServerWait(10).async(true);
    CloudSdk sdk = builder.build();

    assertNotNull(sdk.getRunDevAppServerWaitListener());
    assertEquals(1, builder.getStdOutLineListeners().size());
    assertEquals(2, builder.getStdErrLineListeners().size());
    assertEquals(1, builder.getExitListeners().size());
  }

  @Test(expected = AppEngineException.class)
  public void testNewCloudSdk_inheritOutputAndOutListener() {
    builder.inheritProcessOutput(true).addStdOutLineListener(outputListener).build();
  }

  @Test(expected = AppEngineException.class)
  public void testNewCloudSdk_inheritOutputAndErrListener() {
    builder.inheritProcessOutput(true).addStdErrLineListener(outputListener).build();
  }

  @Test
  public void testResolversOrdering() {
    CloudSdkResolver r1 = Mockito.mock(CloudSdkResolver.class, "r1");
    when(r1.getRank()).thenReturn(0);
    when(r1.getCloudSdkPath()).thenReturn(Paths.get("/r1"));
    CloudSdkResolver r2 = Mockito.mock(CloudSdkResolver.class, "r2");
    when(r2.getRank()).thenReturn(10);
    when(r2.getCloudSdkPath()).thenReturn(Paths.get("/r2"));
    CloudSdkResolver r3 = Mockito.mock(CloudSdkResolver.class, "r3");
    when(r3.getRank()).thenReturn(100);
    when(r3.getCloudSdkPath()).thenReturn(Paths.get("/r3"));

    Builder builder = new CloudSdk.Builder().resolvers(Arrays.asList(r3, r2, r1));
    List<CloudSdkResolver> resolvers = builder.getResolvers();
    assertEquals(r1, resolvers.get(0));
    assertEquals(r2, resolvers.get(1));
    assertEquals(r3, resolvers.get(2));

    CloudSdk sdk = builder.build();
    assertEquals(r1.getCloudSdkPath(), sdk.getSdkPath());
  }

  @Test
  public void testResolverCascading() {
    CloudSdkResolver r1 = Mockito.mock(CloudSdkResolver.class, "r1");
    when(r1.getRank()).thenReturn(0);
    when(r1.getCloudSdkPath()).thenReturn(null);
    CloudSdkResolver r2 = Mockito.mock(CloudSdkResolver.class, "r2");
    when(r2.getRank()).thenReturn(10);
    when(r2.getCloudSdkPath()).thenReturn(Paths.get("/r2"));

    Builder builder = new CloudSdk.Builder().resolvers(Arrays.asList(r1, r2));
    List<CloudSdkResolver> resolvers = builder.getResolvers();
    assertEquals(r1, resolvers.get(0));
    assertEquals(r2, resolvers.get(1));

    CloudSdk sdk = builder.build();
    assertEquals("r1 should not resolve", r2.getCloudSdkPath(), sdk.getSdkPath());
  }
}
