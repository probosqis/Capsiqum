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
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import com.wcaokaze.probosqis.panoptiqon.compose.asState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

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
   private val savedPageStates: List<SavedPageState>
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
}

@Stable
abstract class PageStackState {
   abstract val pageStack: PageStack
}

fun PageStackState(initialPageStack: PageStack) = object : PageStackState() {
   override val pageStack: PageStack by mutableStateOf(initialPageStack)
}

fun PageStackState(cache: WritableCache<PageStack>) = object : PageStackState() {
   override val pageStack: PageStack by cache.asState()
}

@Composable
fun PageStack(
   state: PageStackState,
   pageComposables: ImmutableList<PageComposableWithStateFactory<*, *>>,
   fallback: @Composable (Page, PageState) -> Unit = { _, _ -> }
) {
   val coroutineScope = rememberCoroutineScope()
   val pageStateStore = remember {
      PageStateStore(
         pageComposables.map { it.stateFactory },
         coroutineScope // TODO
      )
   }
   val pageSwitcherState = remember(pageComposables) {
      PageSwitcherState(pageComposables.map { it.pageComposable }, pageStateStore)
   }

   val savedPageState = state.pageStack.head
   val page = savedPageState.page
   val pageState = remember(savedPageState.id) {
      pageSwitcherState.pageStateStore.get(savedPageState)
   }
   val composable = pageSwitcherState.getComposableFor(page)?.composable ?: fallback
   Page(composable, page, pageState)
}

@Composable
private inline fun <P : Page, S : PageState> Page(
   composable: @Composable (P, S) -> Unit,
   page: Page,
   pageState: PageState
) {
   @Suppress("UNCHECKED_CAST")
   composable(page as P, pageState as S)
}
