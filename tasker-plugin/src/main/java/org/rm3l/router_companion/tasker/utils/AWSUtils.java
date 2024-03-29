/*
 * DD-WRT Companion is a mobile app that lets you connect to,
 * monitor and manage your DD-WRT routers on the go.
 *
 * Copyright (C) 2014-2022  Armel Soro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact Info: Armel Soro <armel+router_companion@rm3l.org>
 */
package org.rm3l.router_companion.tasker.utils;

import android.content.Context;
import androidx.annotation.NonNull;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import java.util.Objects;

/** Created by rm3l on 01/08/16. */
public final class AWSUtils {

  private static AWSCredentialsProvider credsProvider;

  private static AmazonS3 s3Client;

  private static TransferUtility s3TransferUtility;

  @NonNull
  public static AWSCredentialsProvider getAWSCredentialsProvider(final Context context) {
    if (credsProvider == null) {
      credsProvider =
          new CognitoCachingCredentialsProvider(
              context,
              ContextUtils.getConfigProperty(context, "AWS_COGNITO_IDENTITY_POOL_ID", null),
              Regions.fromName(
                  Objects.requireNonNull(
                      ContextUtils.getConfigProperty(
                          context, "AWS_COGNITO_IDENTITY_POOL_REGION", "us-east-1"))));
    }
    return credsProvider;
  }

  @NonNull
  public static AmazonS3 getAmazonS3Client(final Context context) {
    if (s3Client == null) {
      s3Client =
          new AmazonS3Client(
              getAWSCredentialsProvider(context), Region.getRegion(Regions.DEFAULT_REGION));
      s3Client.setRegion(
          Region.getRegion(
              Objects.requireNonNull(
                  ContextUtils.getConfigProperty(
                      context, "AWS_COGNITO_IDENTITY_POOL_REGION", "us-east-1"))));
    }
    return s3Client;
  }

  @NonNull
  public static TransferUtility getTransferUtility(final Context context) {
    if (s3TransferUtility == null) {
      TransferNetworkLossHandler.getInstance(context);
      s3TransferUtility =
          TransferUtility.builder().s3Client(getAmazonS3Client(context)).context(context).build();
    }
    return s3TransferUtility;
  }

  @NonNull
  public static String getS3BucketName(final Context context) {
    return Objects.requireNonNull(
        ContextUtils.getConfigProperty(context, "AWS_S3_BUCKET", "dd-wrt-companion"));
  }

  private AWSUtils() {}
}
