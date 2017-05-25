/*
 * Copyright 2017 Spotify AB.
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

package com.spotify.scio.extra

import com.spotify.scio.ScioContext
import com.spotify.scio.io.FileStorage
import com.spotify.scio.util.ScioUtil
import com.spotify.scio.values.SCollection

import scala.reflect.ClassTag

/**
 * Main package for checkpoint API. Import all.
 *
 * {{{
 * import com.spotify.scio.extra.checkpoint._
 * }}}
 */
package object checkpoint {

  implicit class CheckpointScioContext(val self: ScioContext) extends AnyVal {

    /**
     * Checkpoints are useful for debugging one part of a long flow, when you would otherwise have
     * to run many steps to get to the one you care about.  To enable checkpoints, sprinkle calls to
     * [[checkpoint[T]()]] throughout your flow, ideally after expensive steps.
     *
     * @param fileOrPath filename or fully qualified path to the checkpoint
     * @param fn result of this arbitrary => [[SCollection[T]] flow is what is checkpointed
     */
    def checkpoint[T: ClassTag](fileOrPath: String)
                               (fn: => SCollection[T]): SCollection[T] = {
      val path = ScioUtil.getTempFile(self, fileOrPath)
      if (FileStorage(ScioUtil.addPartSuffix(path)).isDone) {
        self.objectFile[T](ScioUtil.addPartSuffix(path))
      } else {
        val r = fn
        require(r.context == self, "Result SCollection has to share the same ScioContext")
        r.materialize(path)
        r
      }
    }
  }

}