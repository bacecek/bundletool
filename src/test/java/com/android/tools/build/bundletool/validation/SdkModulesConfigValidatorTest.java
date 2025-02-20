/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tools.build.bundletool.validation;

import static com.android.tools.build.bundletool.model.RuntimeEnabledSdkVersionEncoder.VERSION_MAJOR_MAX_VALUE;
import static com.android.tools.build.bundletool.model.RuntimeEnabledSdkVersionEncoder.VERSION_MINOR_MAX_VALUE;
import static com.android.tools.build.bundletool.model.utils.BundleParser.EXTRACTED_SDK_MODULES_FILE_NAME;
import static com.android.tools.build.bundletool.testing.SdkBundleBuilder.createSdkModulesConfig;
import static com.android.tools.build.bundletool.testing.TestUtils.createZipBuilderForModules;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.android.bundle.SdkModulesConfigOuterClass.SdkModulesConfig;
import com.android.tools.build.bundletool.io.ZipBuilder;
import com.android.tools.build.bundletool.model.ZipPath;
import com.android.tools.build.bundletool.model.exceptions.InvalidBundleException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SdkModulesConfigValidatorTest {

  @Rule public final TemporaryFolder tmp = new TemporaryFolder();

  private Path sdkModulesPath;

  @Before
  public void setUp() throws Exception {
    sdkModulesPath = tmp.getRoot().toPath().resolve(EXTRACTED_SDK_MODULES_FILE_NAME);
  }

  @Test
  public void majorVersionTooBig_throws() throws IOException {
    assertExceptionThrown(
        /* bundletoolVersion= */ "1.9.1",
        /* major= */ VERSION_MAJOR_MAX_VALUE + 1,
        /* minor= */ 2,
        /* patch= */ 3,
        "SDK major version must be an integer between 0 and " + VERSION_MAJOR_MAX_VALUE);
  }

  @Test
  public void majorVersionNegative_throws() throws IOException {
    assertExceptionThrown(
        /* bundletoolVersion= */ "1.9.1",
        /* major= */ -1,
        /* minor= */ 2,
        /* patch= */ 3,
        "SDK major version must be an integer between 0 and " + VERSION_MAJOR_MAX_VALUE);
  }

  @Test
  public void minorVersionTooBig_throws() throws IOException {
    assertExceptionThrown(
        /* bundletoolVersion= */ "1.9.1",
        /* major= */ 1,
        /* minor= */ VERSION_MINOR_MAX_VALUE + 2,
        /* patch= */ 3,
        "SDK minor version must be an integer between 0 and " + VERSION_MINOR_MAX_VALUE);
  }

  @Test
  public void minorVersionNegative_throws() throws IOException {
    assertExceptionThrown(
        /* bundletoolVersion= */ "1.9.1",
        /* major= */ 1,
        /* minor= */ -2,
        /* patch= */ 3,
        "SDK minor version must be an integer between 0 and " + VERSION_MINOR_MAX_VALUE);
  }

  @Test
  public void patchVersionNegative_throws() throws IOException {
    assertExceptionThrown(
        /* bundletoolVersion= */ "1.9.1",
        /* major= */ 1,
        /* minor= */ 2,
        /* patch= */ -3,
        "SDK patch version must be a non-negative integer");
  }

  @Test
  public void invalidBundletoolVersion_throws() throws IOException {
    assertExceptionThrown(
        /* bundletoolVersion= */ "invalidVersion",
        /* major= */ 1,
        /* minor= */ 2,
        /* patch= */ 3,
        "Invalid Bundletool version in the SdkModulesConfig.pb file: 'invalidVersion'");
  }

  @Test
  public void validConfig_ok() throws IOException {
    createZipBuilderForModules().writeTo(sdkModulesPath);

    try (ZipFile modulesZip = new ZipFile(sdkModulesPath.toFile())) {
      new SdkModulesConfigValidator().validateSdkModulesZipFile(modulesZip);
    }
  }

  private void assertExceptionThrown(
      String bundletoolVersion, int major, int minor, int patch, String errorMessage)
      throws IOException {
    writeModulesZip(bundletoolVersion, major, minor, patch);

    try (ZipFile modulesZip = new ZipFile(sdkModulesPath.toFile())) {
      InvalidBundleException exception =
          assertThrows(
              InvalidBundleException.class,
              () -> new SdkModulesConfigValidator().validateSdkModulesZipFile(modulesZip));

      assertThat(exception).hasMessageThat().isEqualTo(errorMessage);
    }
  }

  private void writeModulesZip(String bundletoolVersion, int major, int minor, int patch)
      throws IOException {
    SdkModulesConfig sdkModulesConfig =
        createSdkModulesConfig(bundletoolVersion, "packageName", major, minor, patch);
    new ZipBuilder()
        .addFileWithContent(ZipPath.create("SdkModulesConfig.pb"), sdkModulesConfig.toByteArray())
        .writeTo(sdkModulesPath);
  }
}
