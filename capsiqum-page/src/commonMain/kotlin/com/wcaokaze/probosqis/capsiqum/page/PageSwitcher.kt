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
import androidx.compose.runtime.remember
import kotlinx.collections.immutable.ImmutableList
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
fun PageSwitcher(
   pageStackState: PageStackState,
   pageComposables: ImmutableList<PageComposable<*, *>>,
   savedPageState: SavedPageState = pageStackState.pageStack.head,
   fallback: @Composable (Page, PageState) -> Unit = { _, _ -> }
) {
   val pageComposableMap = remember(pageComposables) {
      buildMap {
         for (c in pageComposables) {
            put(c.pageClass, c)
         }
      }
   }
   val page = savedPageState.page
   val pageState = remember(savedPageState.id) {
      pageStackState.getPageState(savedPageState)
   }
   val composable = remember(page) {
      pageComposableMap[page::class]?.composable ?: fallback
   }
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
