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

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import kotlinx.coroutines.sync.Mutex

/**
 * 複数のコルーチンから同時に実行されないことを保証する。
 *
 * [Mutex]と違い、同時に呼び出された場合、先に実行されていた方がキャンセルされて
 * 後の呼び出しが実行される。
 *
 * [MutatorMutex]を常に同じ[Priority][MutatePriority]で呼び出すのと似ているが、
 * こちらはsuspendなしでキャンセルする[cancel]関数がある。
 */
internal expect class CancellerMutex() {
   suspend fun runCancelling(block: suspend () -> Unit)

   fun cancel()
}
