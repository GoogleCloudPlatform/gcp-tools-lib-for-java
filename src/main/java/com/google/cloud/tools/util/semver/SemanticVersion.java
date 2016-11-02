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

package com.google.cloud.tools.util.semver;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticVersion implements Comparable<SemanticVersion> {

  private static final Pattern SEMVER_PATTERN = Pattern.compile(getSemVerRegex());

  private final String version;

  private final int majorVersion;
  private final int minorVerion;
  private final int patchVersion;
  private final SemanticVersionPreRelease preRelease;
  private final String build;

  public SemanticVersion(String version) throws IllegalArgumentException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(version));

    Matcher matcher = SEMVER_PATTERN.matcher(version);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          String.format("Pattern \"%s\" is not a valid SemanticVersion.",version));
    }

    // TODO catch and rethrow as a IllegalArgument exception? or at least give a better message?
    // TODO assertNotNull for any of these?
    majorVersion = Integer.parseInt(matcher.group("major"));
    minorVerion = Integer.parseInt(matcher.group("minor"));
    patchVersion = Integer.parseInt(matcher.group("patch"));
    preRelease = matcher.group("prerelease") != null ?
        new SemanticVersionPreRelease(matcher.group("prerelease")) : null;
    build = matcher.group("build");

    this.version = version;
  }

  private static String getSemVerRegex() {
    // Only digits, with no leading zeros.
    String digits = "(?:0|[1-9][0-9]*)";
    // Digits, letters and dashes
    String alphaNum = "[-0-9A-Za-z]+";
    // This is an alphanumeric string that must have at least one letter (or else it would be
    // considered digits).
    String strictAlphaNum = "[-0-9A-Za-z]*[-A-Za-z]+[-0-9A-Za-z]*";

    String preReleaseIdentifier = "(?:" + digits + "|" + strictAlphaNum + ")";
    String preRelease = "(?:" + preReleaseIdentifier + "(?:\\." + preReleaseIdentifier + ")*)";
    String build = "(?:" + alphaNum + "(?:\\." + alphaNum + ")*)";

    return "^(?<major>"+ digits +")\\.(?<minor>" + digits + ")\\.(?<patch>" + digits + ")"
        + "(?:\\-(?<prerelease>" + preRelease + "))?(?:\\+(?<build>" + build + "))?$";
  }

  @Override
  public String toString() {
    return version;
  }

  @Override
  public int compareTo(SemanticVersion other) {
    // First, compare required fields
    List<Integer> mine = ImmutableList.of(majorVersion, minorVerion, patchVersion);
    List<Integer> others = ImmutableList.of(other.getMajorVersion(), other.getMinorVerion(),
        other.getPatchVersion());

    for (int i = 0; i < mine.size(); i++) {
      int result = mine.get(i).compareTo(others.get(i));
      if (result != 0) {
        return result;
      }
    }

    // If required components are equal, compare prerelease strings. Note that build numbers are
    // never included in comparison.

    // A SemVer with a pre-release version has lower precedence than a normal version.
    if (preRelease == null) {
      if (other.getPreRelease() == null) {
        return 0;
      }
      return 1;
    }

    return preRelease.compareTo(other.getPreRelease());
  }

  @Override
  public int hashCode() {
    // Purposely exclude the build identifier as it is not used for determining equality.
    return Objects.hash(majorVersion, minorVerion, patchVersion, preRelease);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (this.getClass() != obj.getClass()) {
      return false;
    }
    SemanticVersion otherVersion = (SemanticVersion) obj;

    return this.compareTo(otherVersion) == 0;
  }

  public int getMajorVersion() {
    return majorVersion;
  }

  public int getMinorVerion() {
    return minorVerion;
  }

  public int getPatchVersion() {
    return patchVersion;
  }

  protected SemanticVersionPreRelease getPreRelease() {
    return preRelease;
  }

  public String getBuild() {
    return build;
  }

}
