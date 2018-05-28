/*
 * Copyright 2018 Spotify AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.spotify.scio

import com.google.cloud.spanner._
import com.spotify.scio.io.Tap
import com.spotify.scio.testing.{CustomIO, TestIO}
import com.spotify.scio.values.SCollection
import org.apache.beam.sdk.io.gcp.spanner.{MutationGroup, SpannerConfig, SpannerIO}

import scala.collection.JavaConverters._
import scala.concurrent.Future

/**
 * Main package for Spanner APIs. Import all.
 *
 * {{{
 * import com.spotify.scio.spanner._
 * }}}
 */
package object spanner {

  /** Enhanced version of [[ScioContext]] with Spanner methods. */
  implicit class SpannerScioContext(val self: ScioContext) extends AnyVal {

    /**
     * Read from Spanner table. Return [[com.spotify.scio.values.SCollection SCollection]]
     * of `Struct`s.
     */
    def spannerFromTable(projectId: String,
                         instanceId: String,
                         databaseId: String,
                         table: String,
                         columns: Iterable[String],
                         keySet: KeySet = null,
                         partitionOptions: PartitionOptions = null,
                         timestampBound: TimestampBound = null): SCollection[Struct] = {

      val config = SpannerConfig.create
        .withProjectId(projectId)
        .withInstanceId(instanceId)
        .withDatabaseId(databaseId)

      spannerFromTableWithConfig(config, table, columns, keySet, partitionOptions, timestampBound)
    }

    /**
     * Read from Spanner table. Return [[com.spotify.scio.values.SCollection SCollection]]
     * of `Struct`s.
     */
    def spannerFromTableWithConfig(spannerConfig: SpannerConfig,
                                   table: String,
                                   columns: Iterable[String],
                                   keySet: KeySet = null,
                                   partitionOptions: PartitionOptions = null,
                                   timestampBound: TimestampBound = null): SCollection[Struct] =
      self.requireNotClosed {
        if (self.isTest) {
          val input = CustomIO[Struct](spannerConfig.toString)
          self.getTestInput[Struct](input)

        } else {
          var read = SpannerIO.read.withSpannerConfig(spannerConfig)
            .withTable(table)
            .withColumns(columns.toSeq.asJava)

          if (keySet != null) { read = read.withKeySet(keySet) }
          if (partitionOptions != null) { read = read.withPartitionOptions(partitionOptions) }
          if (timestampBound != null) { read = read.withTimestampBound(timestampBound) }

          self.wrap(self.applyInternal(read))
        }
    }

    /**
     * Read from Spanner with query. Return [[com.spotify.scio.values.SCollection SCollection]]
     * of `Struct`s.
     */
    def spannerFromQuery(projectId: String,
                         instanceId: String,
                         databaseId: String,
                         query: String,
                         index: String = null,
                         partitionOptions: PartitionOptions = null,
                         timestampBound: TimestampBound = null): SCollection[Struct] = {

      val config = SpannerConfig.create
        .withProjectId(projectId)
        .withInstanceId(instanceId)
        .withDatabaseId(databaseId)

      spannerFromQueryWithConfig(config, query, index, partitionOptions, timestampBound)
    }

    /**
     * Read from Spanner with query. Return [[com.spotify.scio.values.SCollection SCollection]]
     * of `Struct`s.
     */
    def spannerFromQueryWithConfig(spannerConfig: SpannerConfig,
                                   query: String,
                                   index: String = null,
                                   partitionOptions: PartitionOptions = null,
                                   timestampBound: TimestampBound = null): SCollection[Struct] =
      self.requireNotClosed {
        if (self.isTest) {
          val input = CustomIO[Struct](spannerConfig.toString)
          self.getTestInput[Struct](input)

        } else {
          var read = SpannerIO.read.withSpannerConfig(spannerConfig).withQuery(query)

          if (index != null) { read = read.withIndex(index) }
          if (partitionOptions != null) { read = read.withPartitionOptions(partitionOptions) }
          if (timestampBound != null) { read = read.withTimestampBound(timestampBound) }

          self.wrap(self.applyInternal(read))
        }
    }
  }

  /**
   * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with
   * Spanner methods.
   */
  implicit class SpannerSCollection(val self: SCollection[Mutation]) extends AnyVal {

    /** Commit `Mutation`s to Spanner. */
    def saveAsSpanner(projectId: String,
                      instanceId: String,
                      databaseId: String,
                      batchSizeBytes: Long = 0): Future[Tap[Mutation]] = {

      val config = SpannerConfig.create
        .withProjectId(projectId)
        .withInstanceId(instanceId)
        .withDatabaseId(databaseId)

      saveAsSpannerWithConfig(config, batchSizeBytes)
    }

    /** Commit `Mutation`s to Spanner. */
    def saveAsSpannerWithConfig(spannerConfig: SpannerConfig,
                                batchSizeBytes: Long = 0): Future[Tap[Mutation]] = {

      if (self.context.isTest) {
        val output = CustomIO[Mutation](spannerConfig.toString)
        self.context.testOut(output.asInstanceOf[TestIO[Mutation]])(self)

      } else {
        var write = SpannerIO.write.withSpannerConfig(spannerConfig)
        if (batchSizeBytes > 0) { write = write.withBatchSizeBytes(batchSizeBytes) }

        self.applyInternal(write)
      }
      Future.failed(new NotImplementedError("Spanner future not implemented."))
    }
  }

  /**
   * Enhanced version of [[com.spotify.scio.values.SCollection SCollection]] with
   * Spanner methods for committing groups of `Mutation`s atomically
   * ([[org.apache.beam.sdk.io.gcp.spanner.MutationGroup MutationGroup]]).
   */
  implicit class SpannerMutationGroupSCollection(val self: SCollection[MutationGroup])
    extends AnyVal {

    /** Commit [[org.apache.beam.sdk.io.gcp.spanner.MutationGroup MutationGroup]]s to Spanner. */
    def saveAsSpanner(projectId: String,
                      instanceId: String,
                      databaseId: String,
                      batchSizeBytes: Long = 0): Future[Tap[MutationGroup]] = {

      val config = SpannerConfig.create
        .withProjectId(projectId)
        .withInstanceId(instanceId)
        .withDatabaseId(databaseId)

      saveAsSpannerWithConfig(config, batchSizeBytes)
    }

    /** Commit [[org.apache.beam.sdk.io.gcp.spanner.MutationGroup MutationGroup]]s to Spanner. */
    def saveAsSpannerWithConfig(spannerConfig: SpannerConfig,
                                batchSizeBytes: Long = 0): Future[Tap[MutationGroup]] = {

      if (self.context.isTest) {
        val output = CustomIO[MutationGroup](spannerConfig.toString)
        self.context.testOut(output.asInstanceOf[TestIO[MutationGroup]])(self)

      } else {
        var write = SpannerIO.write.withSpannerConfig(spannerConfig)
        if (batchSizeBytes > 0) { write = write.withBatchSizeBytes(batchSizeBytes) }

        self.applyInternal(write.grouped)
      }
      Future.failed(new NotImplementedError("Spanner future not implemented."))
    }
  }
}