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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned

/**
 * Page遷移時アニメーションを可能にする。
 *
 * 遷移前/先のPageを元に適切な[PageTransitionElementAnim.createAnimModifier]が
 * 実行され、返却されたModifierがこのModifierに統合される。
 *
 * このModifierが付与されたComposableの位置は[PageLayoutInfo]に収集され、
 * Page遷移時に[createAnimModifier][PageTransitionElementAnim.createAnimModifier]に
 * 渡されるため、[createAnimModifier][PageTransitionElementAnim.createAnimModifier]は
 * 遷移前/先のPageの各Composableの位置を元にアニメーションを生成できる。
 *
 * @see PageTransitionSpec
 */
@Composable
fun Modifier.transitionElement(
   layoutId: PageLayoutInfo.LayoutId,
   enabled: Boolean = true
): Modifier {
   if (!enabled) { return this }

   val pageLayoutInfo = LocalPageLayoutInfo.current
   val pageTransitionAnimations = LocalPageTransitionAnimations.current
   val transition = LocalPageTransition.current

   val transitionAnimationModifier = when (
      val elementAnim = pageTransitionAnimations[layoutId]
   ) {
      null -> Modifier
      is CurrentPageTransitionElementAnim -> with (elementAnim) {
         CurrentPageTransitionElementAnimScope(transition).createAnimModifier()
      }
      is TargetPageTransitionElementAnim -> with (elementAnim) {
         TargetPageTransitionElementAnimScope(transition).createAnimModifier()
      }
   }

   return onGloballyPositioned { pageLayoutInfo[layoutId] = it }
      .then(transitionAnimationModifier)
}
