/*
 * Copyright 2018 Google LLC
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

package com.google.cloud.tools.managedcloudsdk.install;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

public class UnixInstallScriptProviderTest {

  @Test
  public void testGetScriptCommandLine_nonAbsoluteSdkRoot() {
    try {
      new UnixInstallScriptProvider(Collections.emptyMap())
          .getScriptCommandLine(Paths.get("relative/path"));
      Assert.fail();
    } catch (IllegalArgumentException e) {
      Assert.assertEquals("non-absolute SDK path", e.getMessage());
    }
  }

  @Test
  public void testGetScriptCommandLine() {
    Assume.assumeFalse(System.getProperty("os.name").startsWith("Windows"));

    Path sdkRoot = Paths.get("/path/to/sdk");
    List<String> commandLine =
        new UnixInstallScriptProvider(Collections.emptyMap()).getScriptCommandLine(sdkRoot);

    Assert.assertEquals(1, commandLine.size());
    Path scriptPath = Paths.get(commandLine.get(0));
    Assert.assertTrue(scriptPath.isAbsolute());
    Assert.assertEquals(Paths.get("/path/to/sdk/install.sh"), scriptPath);
  }
}
