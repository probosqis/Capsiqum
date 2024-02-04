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

package com.wcaokaze.probosqis.capsiqum

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SingleColumnPageStackBoardScrollTest : SingleColumnPageStackBoardComposeTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   override lateinit var pageStackRepository: PageStackRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = createPageStackRepositoryMock()
   }

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
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 3)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
      rule.runOnIdle {
         assertEquals(0.0f, pageStackBoardState.scrollState.scrollOffset)
      }

      val scrollAmount = 25.dp

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-scrollAmount.toPx(), 0.0f))
         // upするとsnapする
         // up()
      }

      rule.onNodeWithText("0").assertLeftPositionInRootIsEqualTo(
         expectedPageStackLeftPosition(0) - scrollAmount)
      rule.runOnIdle {
         assertEquals(
            with (rule.density) { scrollAmount.toPx() },
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scrollEdge() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 2)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(100.dp.toPx(), 0.0f))
      }
      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-defaultPageStackBoardWidth.toPx(), 0.0f))
      }
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         moveBy(Offset(-defaultPageStackBoardWidth.toPx(), 0.0f))
      }
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scrollEdge_afterSizeChanged() {
      var boardWidth by mutableStateOf(200.dp)

      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 2)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState, boardWidth)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, boardWidth),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      boardWidth = 90.dp
      rule.onNodeWithText("1").assertLeftPositionInRootIsEqualTo(
         expectedPageStackLeftPosition(0, boardWidth))
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, boardWidth),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-100.dp.toPx(), 0.0f))
      }
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, boardWidth),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         up()
      }
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, boardWidth),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      boardWidth = 100.dp
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, pageStackBoardWidth = 90.dp),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap_tooFast() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(96.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(96.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(96.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap_edges() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      repeat (3) {
         rule.onNodeWithTag(pageStackBoardTag).swipeLeft(40.dp)
      }
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(3),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(3),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap_afterImmobility() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(40.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      val halfWidth = (defaultPageStackBoardWidth + 16.dp) / 2

      rule.onNodeWithTag(pageStackBoardTag)
         .swipeLeft(halfWidth - 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag)
         .swipeRight(halfWidth - 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag)
         .swipeLeft(halfWidth + 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag)
         .swipeRight(halfWidth + 2.dp, duration = 1000L, decayInterpolator)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun overscroll() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      val scrollDistance = with (rule.density) { 32.dp.toPx() }

      coroutineScope.launch {
         pageStackBoardState.scrollState.scroll {
            scrollBy(-scrollDistance)

            assertEquals(
               0.0f,
               pageStackBoardState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            0.0f,
            pageStackBoardState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         pageStackBoardState.scrollState.scroll(enableOverscroll = true) {
            scrollBy(-scrollDistance)

            assertEquals(
               -scrollDistance,
               pageStackBoardState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            0.0f,
            pageStackBoardState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         pageStackBoardState.animateScroll(3)

         pageStackBoardState.scrollState.scroll {
            scrollBy(scrollDistance)

            assertEquals(
               expectedScrollOffset(3),
               pageStackBoardState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            expectedScrollOffset(3),
            pageStackBoardState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         pageStackBoardState.scrollState.scroll(enableOverscroll = true) {
            scrollBy(scrollDistance)

            assertEquals(
               expectedScrollOffset(3) + scrollDistance,
               pageStackBoardState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            expectedScrollOffset(3),
            pageStackBoardState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )
      }

      rule.waitForIdle()
   }
}
