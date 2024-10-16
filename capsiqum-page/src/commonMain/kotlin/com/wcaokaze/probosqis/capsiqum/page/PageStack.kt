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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import com.wcaokaze.probosqis.panoptiqon.compose.asMutableState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.KClass

fun PageStack(
   savedPageState: SavedPageState,
   clock: Clock = Clock.System
) = PageStack(
   PageStack.Id(clock.now()),
   savedPageState
)

/**
 * 画面遷移した[Page]をStack状に積んだもの。
 *
 * 必ず1つ以上の[Page]を持つ（空のPageStackという概念はない）。
 */
@Serializable
class PageStack private constructor(
   val id: Id, // TODO: キャッシュシステムの完成後Cache自体が識別子となるため削除する
   internal val savedPageStates: List<SavedPageState>
) {
   @Serializable
   @JvmInline
   value class Id(val value: Long) {
      constructor(createdTime: Instant) : this(createdTime.toEpochMilliseconds())
   }

   constructor(id: Id, savedPageState: SavedPageState) : this(
      id, listOf(savedPageState)
   )

   /** このPageStackの一番上の[SavedPageState] */
   val head: SavedPageState get() = savedPageStates.last()

   val pageCount: Int get() = savedPageStates.size

   /**
    * @return
    * このPageStackの一番上の[Page]を取り除いたPageStack。
    * このPageStackにPageがひとつしかない場合はnull
    */
   fun tailOrNull(): PageStack? {
      val tailPages = savedPageStates.dropLast(1)
      return if (tailPages.isEmpty()) {
         null
      } else {
         PageStack(id, tailPages)
      }
   }

   fun added(savedPageState: SavedPageState) = PageStack(
      id, savedPageStates + savedPageState
   )

   internal fun findPage(id: PageId): SavedPageState?
       = savedPageStates.findLast { it.id == id }
}

@Stable
abstract class PageStackState
   internal constructor(
      allPageStateFactories: List<PageStateFactory<*, *>>,
      private val coroutineScope: CoroutineScope
   )
{
   protected abstract var pageStackState: PageStack

   var pageStack: PageStack
      get() = pageStackState
      set(value) {
         pageStackState = value

         val ids = pageState.keys.toHashSet()
         for (p in value.savedPageStates) {
            ids -= p.id
         }
         for (id in ids) {
            val pageState = pageState.remove(id) ?: continue
            pageState.pageStateScope.cancel()
         }
      }

   private val pageStateFactories: Map<KClass<out Page>, PageStateFactory<*, *>>
       = buildMap {
         for (f in allPageStateFactories) {
            put(f.pageClass, f)
         }
      }

   private val pageState = mutableMapOf<PageId, PageState<*>>()

   @Stable
   fun getPageState(savedPageState: SavedPageState): PageState<*> {
      check(pageStack.findPage(savedPageState.id) != null) {
         "The specified page (id = ${savedPageState.id.value}) is not in pageStack."
      }

      return pageState.getOrPut(savedPageState.id) {
         instantiatePageState(savedPageState)
      }
   }

   private fun instantiatePageState(savedPageState: SavedPageState): PageState<*> {
      val page = savedPageState.page

      val factory = getStateFactory(page) ?: throw IllegalArgumentException(
         "cannot instantiate PageState for ${page::class}"
      )

      val pageStateScope = CoroutineScope(
         coroutineScope.coroutineContext + SupervisorJob()
      )

      val cache = WritableCache(
         JsonObject(emptyMap())
      )

      val stateSaver = PageState.StateSaver(
         cache,
         wasCacheDeleted = false,
         pageStateScope
      )

      return factory.createPageState(
         page,
         savedPageState.id,
         pageStateScope,
         stateSaver,
      )
   }

   private fun <P : Page> getStateFactory(page: P): PageStateFactory<P, *>? {
      @Suppress("UNCHECKED_CAST")
      return pageStateFactories[page::class] as PageStateFactory<P, *>?
   }
}

fun PageStackState(
   initialPageStack: PageStack,
   allPageStateFactories: List<PageStateFactory<*, *>>,
   coroutineScope: CoroutineScope
) = object : PageStackState(allPageStateFactories, coroutineScope) {
   override var pageStackState: PageStack by mutableStateOf(initialPageStack)
}

@Composable
fun rememberPageStackState(
   initialPageStack: PageStack,
   allPageStateFactories: List<PageStateFactory<*, *>>
): PageStackState {
   val coroutineScope = rememberCoroutineScope()
   return remember {
      PageStackState(initialPageStack, allPageStateFactories, coroutineScope)
   }
}

fun PageStackState(
   cache: WritableCache<PageStack>,
   allPageStateFactories: List<PageStateFactory<*, *>>,
   coroutineScope: CoroutineScope
) = object : PageStackState(allPageStateFactories, coroutineScope) {
   override var pageStackState: PageStack by cache.asMutableState()
}

@Composable
fun rememberPageStackState(
   cache: WritableCache<PageStack>,
   allPageStateFactories: List<PageStateFactory<*, *>>
): PageStackState {
   val coroutineScope = rememberCoroutineScope()
   return remember {
      PageStackState(cache, allPageStateFactories, coroutineScope)
   }
}
