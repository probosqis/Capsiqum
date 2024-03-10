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
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import com.wcaokaze.probosqis.capsiqum.page.JsonElementSaver
import com.wcaokaze.probosqis.capsiqum.page.PageState
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class StateSaverBuilder {
   private var savedState = JsonObject(emptyMap())

   fun <T> save(key: String, value: T, serializer: KSerializer<T>) {
      val savedValue = Json.encodeToJsonElement(serializer, value)
      savedState = JsonObject(savedState + (key to savedValue))
   }

   fun <T> save(key: String, value: T, saver: Saver<T, *>) {
      val jsonSaver = JsonElementSaver(saver)
      val savedValue = with (jsonSaver) {
         val scope = SaverScope { it is JsonElement }
         scope.save(value)
      }

      savedState = JsonObject(
         if (savedValue != null) {
            savedState + (key to savedValue)
         } else {
            savedState - key
         }
      )
   }

   fun build(saverCoroutineScope: CoroutineScope) = PageState.StateSaver(
      WritableCache(savedState), saverCoroutineScope
   )
}

fun buildPreviewStateSaver(
   buildAction: StateSaverBuilder.() -> Unit,
   saverCoroutineScope: CoroutineScope
): PageState.StateSaver {
   val builder = StateSaverBuilder()
   builder.buildAction()
   return builder.build(saverCoroutineScope)
}

@Composable
fun rememberPreviewStateSaver(
   buildAction: StateSaverBuilder.() -> Unit
): PageState.StateSaver {
   val coroutineScope = rememberCoroutineScope()

   return remember {
      val builder = StateSaverBuilder()
      builder.buildAction()
      builder.build(coroutineScope)
   }
}
