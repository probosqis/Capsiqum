/*
 * Copyright 2024 wcaokaze
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

package com.wcaokaze.probosqis.capsiqum.deck

import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

internal actual class CancellerMutex {
   private val currentJob = AtomicReference<Job?>(null)

   actual suspend fun runCancelling(block: suspend () -> Unit) {
      val newJob = coroutineContext[Job]
      val oldJob = currentJob.getAndSet(newJob)
      oldJob?.cancel()

      try {
         block()
      } finally {
         currentJob.compareAndSet(newJob, null)
      }
   }

   actual fun cancel() {
      currentJob.getAndSet(null)?.cancel()
   }
}
