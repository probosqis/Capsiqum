/*
 * Copyright 2023-2024 wcaokaze
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

package com.wcaokaze.probosqis.capsiqum.page

import androidx.compose.runtime.Stable
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.JsonObject

@Stable
class PageStateStore(
   allPageStateFactories: List<PageStateFactory<*, *>>,
   private val appCoroutineScope: CoroutineScope
) {
   private val factories = buildMap {
      for (f in allPageStateFactories) {
         put(f.pageClass, f)
      }
   }

   private val pageState = mutableMapOf<PageStack.PageId, PageState>()

   @Stable
   fun get(savedPageState: PageStack.SavedPageState): PageState {
      return pageState.getOrPut(savedPageState.id) {
         val page = savedPageState.page
         val factory = getStateFactory(page) ?: throw IllegalArgumentException(
            "cannot instantiate PageState for ${page::class}")
         val cache = WritableCache(
            JsonObject(emptyMap())
         )
         val stateSaver = PageState.StateSaver(
            cache,
            pageStateCoroutineScope = appCoroutineScope // TODO
         )
         factory.pageStateFactory(page, stateSaver)
      }
   }

   private fun <P : Page> getStateFactory(page: P): PageStateFactory<P, *>? {
      @Suppress("UNCHECKED_CAST")
      return factories[page::class] as PageStateFactory<P, *>?
   }
}
