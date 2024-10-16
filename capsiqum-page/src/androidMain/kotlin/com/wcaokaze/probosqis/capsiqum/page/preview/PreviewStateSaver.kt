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

package com.wcaokaze.probosqis.capsiqum.page.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wcaokaze.probosqis.capsiqum.page.PageState
import kotlinx.coroutines.CoroutineScope

fun buildPreviewStateSaver(
   buildAction: StateSaverBuilder.() -> Unit,
   saverCoroutineScope: CoroutineScope
): PageState.StateSaver {
   val builder = StateSaverBuilder()
   builder.buildAction()
   return builder.build(wasCacheDeleted = false, saverCoroutineScope)
}

@Composable
fun rememberPreviewStateSaver(
   buildAction: StateSaverBuilder.() -> Unit
): PageState.StateSaver {
   val coroutineScope = rememberCoroutineScope()

   return remember {
      val builder = StateSaverBuilder()
      builder.buildAction()
      builder.build(wasCacheDeleted = false, coroutineScope)
   }
}
