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

package com.wcaokaze.probosqis.capsiqum.typeswitcher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlin.reflect.KClass

inline fun <reified T : Any> typeSwitcherChild(
   noinline composable: @Composable (T) -> Unit
) = TypeSwitcherChildComposable(T::class, composable)

@Immutable
class TypeSwitcherChildComposable<T : Any>(
   val type: KClass<T>,
   val composable: @Composable (T) -> Unit
)

@Stable
class TypeSwitcherState<T : Any>(
   childComposables: List<TypeSwitcherChildComposable<out T>>
) {
   private val childComposables = buildMap {
      for (c in childComposables) {
         put(c.type, c)
      }
   }

   @Stable
   internal fun <C : T> getChildFor(value: C): TypeSwitcherChildComposable<C>? {
      @Suppress("UNCHECKED_CAST")
      return childComposables[value::class] as TypeSwitcherChildComposable<C>?
   }
}

/**
 * [value]の型によって表示するComposableを変えるComposable。
 *
 * ```kotlin
 * abstract class Page
 * class PageA : Page()
 * class PageB : Page()
 * class PageC : Page()
 *
 * val pageAComposable = typeSwitcherChild<PageA> {
 *    ....
 * }
 * val pageBComposable = typeSwitcherChild<PageB> {
 *    ....
 * }
 * val pageCComposable = typeSwitcherChild<PageC> {
 *    ....
 * }
 *
 * val typeSwitcherState = remember {
 *    TypeSwitcherState(listOf(pageAComposable, pageBComposable, pageCComposable))
 * }
 *
 * val page: Page = ...
 *
 * TypeSwitcher(
 *    typeSwitcherState,
 *    page
 * )
 * ```
 *
 * 普通はwhenでいいよね。
 * サブタイプがモジュール外にもあるときとかに使えるよ
 */
@Composable
fun <T : Any> TypeSwitcher(
   state: TypeSwitcherState<T>,
   value: T
) {
   val c = state.getChildFor(value)
   if (c != null) {
      TypeSwitcherChild(c.composable, value)
   }
}

@Composable
private inline fun <T : Any> TypeSwitcherChild(
   childComposable: @Composable (T) -> Unit,
   value: T
) {
   childComposable(value)
}
