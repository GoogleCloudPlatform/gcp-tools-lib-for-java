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

package com.google.cloud.tools.libraries;

import com.google.common.base.Preconditions;
import java.text.MessageFormat;

/**
 * Describes a Maven artifact.
 */
public class MavenCoordinates {

  public static final String LATEST_VERSION = "LATEST";
  private static final String JAR_TYPE = "jar";

  private final String groupId;
  private final String artifactId;
  private String version = LATEST_VERSION;
  private String packaging = JAR_TYPE;
  private String classifier;

  /**
   * Create a new MavenCoordinates object.
   * 
   * @param groupId the Maven group ID, cannot be <code>null</code>
   * @param artifactId the Maven artifact ID, cannot be <code>null</code>
   */
  public MavenCoordinates(String groupId, String artifactId) {
    Preconditions.checkNotNull(groupId, "groupId null");
    Preconditions.checkNotNull(artifactId, "artifactId null");
    Preconditions.checkArgument(!groupId.isEmpty(), "groupId empty");
    Preconditions.checkArgument(!artifactId.isEmpty(), "artifactId empty");

    this.groupId = groupId;
    this.artifactId = artifactId;
  }

  /**
   * Returns the Maven version of the artifact. Defaults to the special value
   *     {@link MavenCoordinates#LATEST_VERSION}, 
   * 
   * @return the Maven version of the artifact, never <code>null</code>
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns the Maven packaging type, defaults to <code>jar</code>, never <code>null</code>.
   * 
   * @return the Maven packaging type, defaults to <code>jar</code>, never <code>null</code>
   */
  public String getPackaging() {
    return packaging;
  }

  /**
   * Returns the Maven classifier.
   * 
   * @return the Maven classifier or <code>null</code> if it was not set
   */
  public String getClassifier() {
    return classifier;
  }

  /**
   * Returns the Maven group ID.
   * 
   * @return the Maven group ID, never <code>null</code>
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Returns the Maven artifact ID.
   * 
   * @return the Maven artifact ID, never <code>null</code>
   */
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String toString() {
    return MessageFormat.format("MavenCoordinates [repository={0}, {1}:{2}:{3}:{4}]",
        groupId, artifactId, packaging, classifier, version);
  }
  
  @Override 
  public int hashCode() {
    return getCoordinates().hashCode();
  }

  private String getCoordinates() {
    String coordinates = groupId + ":" + artifactId + ":" + version;
    if (classifier != null) {
      coordinates += ":" + classifier;
    }
    if (!packaging.equals(JAR_TYPE)) {
      coordinates += ":" + packaging;
    }
    return coordinates;
  }
  
  @Override 
  public boolean equals(Object other) {
    if (other == null || !(other instanceof MavenCoordinates)) {
      return false;
    }
    MavenCoordinates coordinates = (MavenCoordinates) other;
    return coordinates.artifactId.equals(artifactId) 
        && coordinates.groupId.equals(groupId) 
        && coordinates.version.equals(version)
        && coordinates.classifier.equals(classifier)
        && coordinates.packaging.equals(packaging);
  }
  
  /**
   * A builder initially configured to create a copy of this object.
   */
  public Builder toBuilder() {
    Builder builder = new Builder();
    builder.groupId = groupId;
    builder.artifactId = artifactId;
    builder.version = version;
    builder.packaging = packaging;
    builder.classifier = classifier;
    return builder;
  }
  
  /**
   * A builder for immutable MavenCoordinates objects.
   */
  public static class Builder {
    private String groupId;
    private String artifactId;
    private String version = LATEST_VERSION;
    private String packaging = JAR_TYPE;
    private String classifier;

    /**
     * Create a new MavenCoordinates object.
     */
    public MavenCoordinates build() {
      MavenCoordinates coordinates = new MavenCoordinates(groupId, artifactId); 
      coordinates.version = version;
      coordinates.packaging = packaging;
      coordinates.classifier = classifier;
      return coordinates;
    }
    
    /**
     * Specify the Maven group ID.
     * 
     * @param groupId the Maven group ID
     */
    public Builder setGroupId(String groupId) {
      Preconditions.checkNotNull(groupId, "groupId null");
      Preconditions.checkArgument(!groupId.isEmpty(), "groupId is empty");
      this.groupId = groupId;
      return this;
    }

    /**
     * Specify the Maven artifact ID.
     *
     * @param artifactId the Maven artifact ID
     */
    public Builder setArtifactId(String artifactId) {
      Preconditions.checkNotNull(artifactId, "artifactId null");
      Preconditions.checkArgument(!artifactId.isEmpty(), "artifactId is empty");
      this.artifactId = artifactId;
      return this;
    }    
    
    /**
     * @param type the Maven packaging type, defaults to <code>jar</code>.
     *     Cannot be <code>null</code> or empty string.
     */
    public Builder setPackaging(String type) {
      Preconditions.checkNotNull(type, "type is null");
      Preconditions.checkArgument(!type.isEmpty(), "type is empty");
      this.packaging = type;
      return this;
    }

    /**
     * @param version the Maven version of the artifact, defaults to special value
     *     {@link MavenCoordinates#LATEST_VERSION}, cannot be <code>null</code> or empty string.
     */
    public Builder setVersion(String version) {
      Preconditions.checkNotNull(version, "version is null");
      Preconditions.checkArgument(!version.isEmpty(), "version is empty");
      this.version = version;
      return this;
    }

    /**
     * @param classifier the Maven classifier, defaults to null.
     */
    public Builder setClassifier(String classifier) {
      this.classifier = classifier;
      return this;
    }
    
  }  

}