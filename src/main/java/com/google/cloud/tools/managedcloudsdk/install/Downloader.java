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

package com.google.cloud.tools.managedcloudsdk.install;

import com.google.cloud.tools.managedcloudsdk.MessageListener;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;

/**
 * Downloader for downloading a single Cloud SDK archive. It is a callable we use in chained
 * asynchronous calls.
 */
final class Downloader implements Callable<Path> {

  static final int BUFFER_SIZE = 8 * 1024;
  private final URL address;
  private final Path destinationFile;
  private final String userAgentString;
  private final MessageListener messageListener;

  /** Use {@link DownloaderFactory} to instantiate. */
  Downloader(
      URL source, Path destinationFile, String userAgentString, MessageListener messageListener) {
    this.address = source;
    this.destinationFile = destinationFile;
    this.userAgentString = userAgentString;
    this.messageListener = messageListener;
  }

  /** Download and return a {@link Path} to downloaded archive. */
  @Override
  public Path call() throws IOException, InterruptedException {
    if (!Files.exists(destinationFile.getParent())) {
      Files.createDirectories(destinationFile.getParent());
    }

    URLConnection connection = address.openConnection();
    connection.setRequestProperty("User-Agent", userAgentString);

    try (BufferedOutputStream out =
        new BufferedOutputStream(
            Files.newOutputStream(destinationFile, StandardOpenOption.CREATE_NEW))) {
      try (InputStream in = connection.getInputStream()) {
        messageListener.message("Downloading " + address);

        long contentLength = connection.getContentLengthLong();

        int bytesRead;
        byte[] buffer = new byte[BUFFER_SIZE];

        // Progress is updated every 1%
        long updateThreshold = contentLength / 100;
        long lastUpdated = 0;
        long totalBytesRead = 0;

        messageListener.message("0/" + String.valueOf(contentLength));
        while ((bytesRead = in.read(buffer)) != -1) {
          if (Thread.currentThread().isInterrupted()) {
            messageListener.message("Downloader was interrupted");
            throw new InterruptedException("Downloader was interrupted");
          }
          out.write(buffer, 0, bytesRead);

          // update progress
          totalBytesRead += bytesRead;
          long bytesSinceLastUpdate = totalBytesRead - lastUpdated;
          if (totalBytesRead == contentLength || bytesSinceLastUpdate > updateThreshold) {
            messageListener.message(
                String.valueOf(totalBytesRead) + "/" + String.valueOf(contentLength));
          }
          lastUpdated = totalBytesRead;
        }
      }
    }
    messageListener.message("Download complete");
    return destinationFile;
  }
}
