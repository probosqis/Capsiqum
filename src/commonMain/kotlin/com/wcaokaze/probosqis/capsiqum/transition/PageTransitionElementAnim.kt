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

package com.wcaokaze.probosqis.capsiqum.transition

import androidx.compose.animation.core.Transition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableMap

/**
 * ページ遷移中、[PageTransition]のcontentに適用されるModifier。
 *
 * [PageTransitionElementAnimScope.transition]から現在の遷移状況や
 * 遷移元、遷移先の[PageLayoutInfo]を取得できるため、それを使ってアニメーションを
 * 生成し、Modifierに適用する。
 *
 * @see PageTransitionSpec
 */
sealed class PageTransitionElementAnim<S : PageTransitionElementAnimScope> {
   abstract val createAnimModifier: @Composable S.() -> Modifier
}

/**
 * ページ遷移中、[PageTransition]の遷移元のcontentに適用されるModifier。
 *
 * @see PageTransitionSpec
 */
class CurrentPageTransitionElementAnim(
   override val createAnimModifier:
         @Composable CurrentPageTransitionElementAnimScope.() -> Modifier
) : PageTransitionElementAnim<CurrentPageTransitionElementAnimScope>()

/**
 * ページ遷移中、[PageTransition]の遷移先のcontentに適用されるModifier。
 *
 * @see PageTransitionSpec
 */
class TargetPageTransitionElementAnim(
   override val createAnimModifier:
         @Composable TargetPageTransitionElementAnimScope.() -> Modifier
) : PageTransitionElementAnim<TargetPageTransitionElementAnimScope>()

sealed class PageTransitionElementAnimScope {
   abstract val transition: Transition<PageLayoutInfo>

   val PageLayoutInfo.isCurrentPage: Boolean
      get() = key != transition.targetState.key
   val PageLayoutInfo.isTargetPage: Boolean
      get() = key == transition.targetState.key
}

class CurrentPageTransitionElementAnimScope(
   override val transition: Transition<PageLayoutInfo>
) : PageTransitionElementAnimScope()

class TargetPageTransitionElementAnimScope(
   override val transition: Transition<PageLayoutInfo>
) : PageTransitionElementAnimScope()

internal typealias PageTransitionElementAnimSet
      = ImmutableMap<PageLayoutInfo.LayoutId, PageTransitionElementAnim<*>>
internal typealias CurrentPageTransitionElementAnimSet
      = ImmutableMap<PageLayoutInfo.LayoutId, CurrentPageTransitionElementAnim>
internal typealias TargetPageTransitionElementAnimSet
      = ImmutableMap<PageLayoutInfo.LayoutId, TargetPageTransitionElementAnim>
