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

package com.wcaokaze.probosqis.capsiqum.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

inline fun <reified P : Page, reified S : PageState> PageComposable(
   noinline composable: @Composable (P, S) -> Unit
): PageComposable<P, S> {
   return PageComposable(P::class, S::class, composable)
}

@Immutable
class PageComposable<P : Page, S : PageState> (
   val pageClass: KClass<P>,
   val pageStateClass: KClass<S>,
   val composable: @Composable (P, S) -> Unit
)

fun PageSwitcherState(
   pageComposables: List<PageComposableWithStateFactory<*, *>>,
   coroutineScope: CoroutineScope
): PageSwitcherState {
   val pageComposablesWithoutFactory = pageComposables.map { it.pageComposable }
   val pageStateStore = PageStateStore(
      pageComposables.map { it.stateFactory },
      coroutineScope
   )
   return PageSwitcherState(pageComposablesWithoutFactory, pageStateStore)
}

@Suppress("FunctionName")
inline fun <reified P : Page, reified S : PageState> PageComposable(
   noinline stateFactory: (P, PageState.StateSaver) -> S,
   noinline composable: @Composable (P, S) -> Unit
): PageComposableWithStateFactory<P, S> {
   return PageComposableWithStateFactory(P::class, S::class, composable, stateFactory)
}

@Immutable
class PageComposableWithStateFactory<P : Page, S : PageState> (
   pageClass: KClass<P>,
   pageStateClass: KClass<S>,
   composable: @Composable (P, S) -> Unit,
   stateFactory: (P, PageState.StateSaver) -> S
) {
   val pageComposable = PageComposable(pageClass, pageStateClass, composable)
   val stateFactory = PageStateFactory(pageClass, pageStateClass, stateFactory)
}

@Stable
class PageSwitcherState
   /**
    * @throws IllegalArgumentException
    * 指定した[PageStateStore]が[pageComposables]のいずれかに必要な[PageState]を
    * インスタンス化できないとき。もしくはインスタンス化できるが
    * [pageComposables]が要求する型と一致しない場合。
    */
   constructor(
      pageComposables: List<PageComposable<*, *>>,
      val pageStateStore: PageStateStore
   )
{
   init {
      for (c in pageComposables) {
         val pageStateFactory = pageStateStore.pageStateFactories[c.pageClass]

         require(pageStateFactory != null) {
            "The specified PageStateStore has no PageStateFactory " +
                  "for ${c.pageClass.simpleName}"
         }

         require(c.pageStateClass == pageStateFactory.pageStateClass) {
            "The specified PageStateStore has a PageStateFactory " +
                  "for ${c.pageClass.simpleName}, but it will instantiate " +
                  "${pageStateFactory.pageStateClass.simpleName}, " +
                  "which the composable does not accept. " +
                  "(The composable accepts ${c.pageStateClass.simpleName})"
         }
      }
   }

   private val pageComposables = buildMap {
      for (c in pageComposables) {
         put(c.pageClass, c)
      }
   }

   @Stable
   internal fun <P : Page> getComposableFor(page: P): PageComposable<P, *>? {
      @Suppress("UNCHECKED_CAST")
      return pageComposables[page::class] as PageComposable<P, *>?
   }
}

/**
 * [SavedPageState]から[PageState]を復元し、対応する[]PageComposable]を
 * コンポーズする。
 *
 * ```kotlin
 * class PageA : Page()
 * class PageB : Page()
 * class PageAState : PageState()
 * class PageBState : PageState()
 *
 * val pageAComposable = PageComposable<PageA, PageAState>(
 *    stateFactory = { _, _ -> PageAState() },
 *    composable = { page, pageState ->
 *       ....
 *    }
 * )
 * val pageBComposable = PageComposable<PageB, PageBState>(
 *    stateFactory = { _, _ -> PageBState() },
 *    composable = { page, pageState ->
 *       ....
 *    }
 * )
 *
 * val pageSwitcherState = remember {
 *    PageSwitcherState(
 *       listOf(pageAComposable, pageBComposable, pageCComposable),
 *       coroutineScope
 *    )
 * }
 *
 * PageSwitcher(
 *    pageSwitcherState,
 *    savedPageState
 * )
 * ```
 */
@Composable
fun PageSwitcher(state: PageSwitcherState, savedPageState: SavedPageState) {
   val page = savedPageState.page
   val pageState = remember(savedPageState.id) {
      state.pageStateStore.get(savedPageState)
   }
   val c = state.getComposableFor(page) ?: TODO()
   Page(c.composable, page, pageState)
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
