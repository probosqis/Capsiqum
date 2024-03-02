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
import com.wcaokaze.probosqis.capsiqum.Page
import kotlin.reflect.KClass

inline fun <reified P : Page> pageComposable(
   noinline composable: @Composable (P) -> Unit
) = PageComposable(P::class, composable)

@Immutable
class PageComposable<P : Page>(
   val pageClass: KClass<P>,
   val composable: @Composable (P) -> Unit
)

@Stable
class PageSwitcherState(
   pageComposables: List<PageComposable<*>>
) {
   private val pageComposables = buildMap {
      for (c in pageComposables) {
         put(c.pageClass, c)
      }
   }

   @Stable
   internal fun <P : Page> getComposableFor(page: P): PageComposable<P>? {
      @Suppress("UNCHECKED_CAST")
      return pageComposables[page::class] as PageComposable<P>?
   }
}

/**
 * ```kotlin
 * class PageA : Page()
 * class PageB : Page()
 * class PageC : Page()
 *
 * val pageAComposable = pageComposable<PageA> { page ->
 *    ....
 * }
 * val pageBComposable = pageComposable<PageB> { page ->
 *    ....
 * }
 * val pageCComposable = pageComposable<PageC> { page ->
 *    ....
 * }
 *
 * val pageSwitcherState = remember {
 *    PageSwitcherState(listOf(pageAComposable, pageBComposable, pageCComposable))
 * }
 *
 * val page: Page = ...
 *
 * PageSwitcher(
 *    pageSwitcherState,
 *    page
 * )
 * ```
 *
 * 普通はwhenでいいよね。
 * サブタイプがモジュール外にもあるときとかに使えるよ
 */
@Composable
fun PageSwitcher(state: PageSwitcherState, page: Page) {
   val c = state.getComposableFor(page) ?: TODO()
   Page(c.composable, page)
}

@Composable
private inline fun <P : Page> Page(
   composable: @Composable (P) -> Unit,
   page: P
) {
   composable(page)
}
