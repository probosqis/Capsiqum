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

package com.wcaokaze.probosqis.capsiqum.deck

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MultiColumnDeckScrollTest : MultiColumnDeckTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   private fun SemanticsNodeInteraction.swipeLeft(
      offset: Dp,
      duration: Long = 100L,
      interpolator: (Float) -> Float = { it }
   ) {
      performTouchInput {
         val start = Offset(viewConfiguration.touchSlop + offset.toPx(), 0.0f)
         val end = Offset.Zero
         swipe(
            curve = {
               val fraction = interpolator(it / duration.toFloat())
               lerp(start, end, fraction)
            },
            duration
         )
      }
   }

   private fun SemanticsNodeInteraction.swipeRight(
      offset: Dp,
      duration: Long = 100L,
      interpolator: (Float) -> Float = { it }
   ) {
      performTouchInput {
         val start = Offset.Zero
         val end = Offset(viewConfiguration.touchSlop + offset.toPx(), 0.0f)
         swipe(
            curve = {
               val fraction = interpolator(it / duration.toFloat())
               lerp(start, end, fraction)
            },
            duration
         )
      }
   }

   private val decayInterpolator = fun (f: Float): Float {
      return 1.0f - (f - 1.0f) * (f - 1.0f)
   }

   @Test
   fun scroll() {
      val deckState = createDeckState(cardCount = 3)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(0))
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(1))
      rule.runOnIdle {
         assertEquals(0.0f, deckState.scrollState.scrollOffset)
      }

      val scrollAmount = 25.dp

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-scrollAmount.toPx(), 0.0f))
         // upするとsnapする
         // up()
      }

      rule.onNodeWithText("0").assertLeftPositionInRootIsEqualTo(
         expectedCardLeftPosition(0) - scrollAmount)
      rule.onNodeWithText("1").assertLeftPositionInRootIsEqualTo(
         expectedCardLeftPosition(1) - scrollAmount)
      rule.runOnIdle {
         assertEquals(
            with (rule.density) { scrollAmount.toPx() },
            deckState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun edge() {
      val deckState = createDeckState(cardCount = 3)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(0))
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(100.dp.toPx(), 0.0f))
      }
      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(0))
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-defaultDeckWidth.toPx(), 0.0f))
      }
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(0))
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         moveBy(Offset(-defaultDeckWidth.toPx(), 0.0f))
      }
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(0))
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }
   }

   @Test
   fun edge_afterSizeChanged() {
      var deckWidth by mutableStateOf(600.dp)

      val deckState = createDeckState(cardCount = 3)
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         MultiColumnDeck(deckState, deckWidth)
      }

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1, deckWidth), deckState.scrollState.scrollOffset)
      }

      deckWidth = 500.dp
      rule.onNodeWithText("1").assertLeftPositionInRootIsEqualTo(
         expectedCardLeftPosition(0, deckWidth))
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1, deckWidth), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-100.dp.toPx(), 0.0f))
      }
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1, deckWidth), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         up()
      }
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1, deckWidth), deckState.scrollState.scrollOffset)
      }

      deckWidth = 600.dp
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, deckWidth = 500.dp),
            deckState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun snap() {
      val deckState = createDeckState(cardCount = 4)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(2), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }
   }

   @Test
   fun snap_windowInsets() {
      val windowInsets = WindowInsets(left = 32.dp, right = 32.dp)

      val deckState = createDeckState(cardCount = 4)
      rule.setContent {
         MultiColumnDeck(deckState, windowInsets = windowInsets)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0, windowInsets = windowInsets),
            deckState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, windowInsets = windowInsets),
            deckState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2, windowInsets = windowInsets),
            deckState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(deckTestTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, windowInsets = windowInsets),
            deckState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(deckTestTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0, windowInsets = windowInsets),
            deckState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun snap_tooFast() {
      val deckState = createDeckState(cardCount = 4)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(96.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(96.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(2), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeRight(96.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }
   }

   @Test
   fun snap_edges() {
      val deckState = createDeckState(cardCount = 4)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }

      repeat (2) {
         rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      }
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(2), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(2), deckState.scrollState.scrollOffset)
      }
   }

   @Test
   fun snap_afterImmobility() {
      val deckState = createDeckState(cardCount = 4)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.onNodeWithTag(deckTestTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      val halfWidth = (expectedCardWidth() + 16.dp) / 2

      rule.onNodeWithTag(deckTestTag)
         .swipeLeft(halfWidth - 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag)
         .swipeRight(halfWidth - 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag)
         .swipeLeft(halfWidth + 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(2), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag)
         .swipeRight(halfWidth + 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(1), deckState.scrollState.scrollOffset)
      }
   }

   @Test
   fun swipe_overMultiplePageStacks() {
      val deckState = createDeckState(cardCount = 4)
      rule.setContent {
         MultiColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(0.0f, deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag)
         .swipeLeft(expectedCardWidth() + 48.dp, duration = 200L)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(2), deckState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(deckTestTag)
         .swipeRight(expectedCardWidth() + 48.dp, duration = 200L)
      rule.runOnIdle {
         assertEquals(expectedScrollOffset(0), deckState.scrollState.scrollOffset)
      }
   }

   @Test
   fun overscroll() {
      val deckState = createDeckState(cardCount = 4)
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         MultiColumnDeck(deckState)
      }

      val scrollDistance = with (rule.density) { 32.dp.toPx() }

      coroutineScope.launch {
         deckState.scrollState.scroll {
            scrollBy(-scrollDistance)

            assertEquals(
               0.0f,
               deckState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            0.0f,
            deckState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         deckState.scrollState.scroll(enableOverscroll = true) {
            scrollBy(-scrollDistance)

            assertEquals(
               -scrollDistance,
               deckState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            0.0f,
            deckState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         deckState.animateScroll(3)

         deckState.scrollState.scroll {
            scrollBy(scrollDistance)

            assertEquals(
               expectedScrollOffset(2),
               deckState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            expectedScrollOffset(2),
            deckState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         deckState.scrollState.scroll(enableOverscroll = true) {
            scrollBy(scrollDistance)

            assertEquals(
               expectedScrollOffset(2) + scrollDistance,
               deckState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            expectedScrollOffset(2),
            deckState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )
      }

      rule.waitForIdle()
   }
}
