/*
 * Copyright 2023 wcaokaze
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

package com.wcaokaze.probosqis.page.transition

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class TransitionSpecBuilderTest {
   @get:Rule
   val rule = createComposeRule()

   @Test
   fun build() {
      val enterCurrentId = PageLayoutInfo.LayoutId()
      val enterCurrent = CurrentPageTransitionElementAnim { Modifier }

      val enterTargetId = PageLayoutInfo.LayoutId()
      val enterTarget = TargetPageTransitionElementAnim { Modifier }

      val exitCurrentId = PageLayoutInfo.LayoutId()
      val exitCurrent = CurrentPageTransitionElementAnim { Modifier }

      val exitTargetId = PageLayoutInfo.LayoutId()
      val exitTarget = TargetPageTransitionElementAnim { Modifier }

      val transitionSpec = pageTransitionSpec(
         enter = {
            currentPageElement(enterCurrentId, enterCurrent)
            targetPageElement (enterTargetId,  enterTarget)
         },
         exit = {
            currentPageElement(exitCurrentId, exitCurrent)
            targetPageElement (exitTargetId,  exitTarget)
         }
      )

      assertEquals(
         persistentMapOf(enterCurrentId to enterCurrent),
         transitionSpec.enteringCurrentPageElementAnimations
      )
      assertEquals(
         persistentMapOf(enterTargetId to enterTarget),
         transitionSpec.enteringTargetPageElementAnimations
      )
      assertEquals(
         persistentMapOf(exitCurrentId to exitCurrent),
         transitionSpec.exitingCurrentPageElementAnimations
      )
      assertEquals(
         persistentMapOf(exitTargetId to exitTarget),
         transitionSpec.exitingTargetPageElementAnimations
      )
   }
}
