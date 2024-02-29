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

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.test.assertPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.capsiqum.PageStack
import com.wcaokaze.probosqis.capsiqum.PageStackBoard
import kotlinx.collections.immutable.persistentMapOf
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class TransitionElementTest {
   @get:Rule
   val rule = createComposeRule()

   @Test
   fun collectCoordinates() {
      val box1 = PageLayoutInfo.LayoutId()
      val box2 = PageLayoutInfo.LayoutId()

      val layoutInfo = PageLayoutInfoImpl(PageStack.PageId(0L))

      rule.setContent {
         CompositionLocalProvider(
            LocalPageLayoutInfo provides layoutInfo,
            LocalPageTransitionAnimations provides persistentMapOf(),
            LocalPageTransition provides updateTransition(layoutInfo, label = null)
         ) {
            Column {
               Box(
                  Modifier
                     .transitionElement(box1)
                     .size(32.dp)
               )
               Box(
                  Modifier
                     .padding(horizontal = 16.dp)
                     .transitionElement(box2)
                     .size(16.dp, 24.dp)
               )
            }
         }
      }

      rule.runOnIdle {
         assertNotNull(layoutInfo[box1]).let { box1Coordinates ->
            assertEquals(
               with (rule.density) {
                  IntSize(32.dp.roundToPx(), 32.dp.roundToPx())
               },
               box1Coordinates.size
            )

            assertEquals(
               Offset.Zero,
               box1Coordinates.positionInRoot()
            )
         }

         assertNotNull(layoutInfo[box2]).let { box2Coordinates ->
            assertEquals(
               with (rule.density) {
                  IntSize(16.dp.roundToPx(), 24.dp.roundToPx())
               },
               box2Coordinates.size
            )

            assertEquals(
               with (rule.density) {
                  Offset(16.dp.toPx(), 32.dp.toPx())
               },
               box2Coordinates.positionInRoot()
            )
         }
      }
   }

   @Test
   fun updateAfterRecomposition() {
      val id = PageLayoutInfo.LayoutId()

      val layoutInfo = PageLayoutInfoImpl(PageStack.PageId(0L))

      var padding by mutableStateOf(PaddingValues(0.dp))
      var size by mutableStateOf(DpSize(32.dp, 32.dp))

      rule.setContent {
         CompositionLocalProvider(
            LocalPageLayoutInfo provides layoutInfo,
            LocalPageTransitionAnimations provides persistentMapOf(),
            LocalPageTransition provides updateTransition(layoutInfo, label = null)
         ) {
            Box(Modifier.size(100.dp)) {
               Box(
                  Modifier
                     .padding(padding)
                     .transitionElement(id)
                     .size(size)
               )
            }
         }
      }

      rule.runOnIdle {
         assertNotNull(layoutInfo[id]).let { coordinates ->
            assertEquals(
               with (rule.density) {
                  IntSize(32.dp.roundToPx(), 32.dp.roundToPx())
               },
               coordinates.size
            )

            assertEquals(
               Offset.Zero,
               coordinates.positionInRoot()
            )
         }
      }

      padding = PaddingValues(horizontal = 8.dp, vertical = 16.dp)

      rule.runOnIdle {
         assertNotNull(layoutInfo[id]).let { coordinates ->
            assertEquals(
               with (rule.density) {
                  IntSize(32.dp.roundToPx(), 32.dp.roundToPx())
               },
               coordinates.size
            )

            assertEquals(
               with (rule.density) {
                  Offset(8.dp.toPx(), 16.dp.toPx())
               },
               coordinates.positionInRoot()
            )
         }
      }

      size = DpSize(48.dp, 56.dp)

      rule.runOnIdle {
         assertNotNull(layoutInfo[id]).let { coordinates ->
            assertEquals(
               with (rule.density) {
                  IntSize(48.dp.roundToPx(), 56.dp.roundToPx())
               },
               coordinates.size
            )

            assertEquals(
               with (rule.density) {
                  Offset(8.dp.toPx(), 16.dp.toPx())
               },
               coordinates.positionInRoot()
            )
         }
      }
   }

   @Test
   fun applyAnimationModifier() {
      val id = PageLayoutInfo.LayoutId()

      val layoutInfo = PageLayoutInfoImpl(PageStack.PageId(0L))
      val transitionLayoutInfo = PageLayoutInfoImpl(PageStack.PageId(1L))

      val animations = persistentMapOf(
         id to CurrentPageTransitionElementAnim {
            assertSame(transitionLayoutInfo, transition.targetState)
            Modifier.padding(16.dp)
         }
      )

      rule.setContent {
         val transition = updateTransition<PageLayoutInfo>(
            transitionLayoutInfo, label = null)

         CompositionLocalProvider(
            LocalPageLayoutInfo provides layoutInfo,
            LocalPageTransitionAnimations provides animations,
            LocalPageTransition provides transition
         ) {
            Box(Modifier.transitionElement(id)) {
               Text("Content")
            }
         }
      }

      rule.onNodeWithText("Content")
         .assertPositionInRootIsEqualTo(16.dp, 16.dp)
   }
}
