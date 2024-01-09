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

package com.wcaokaze.probosqis.capsiqum

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MultiColumnPageStackBoardComposeTest : PageStackBoardComposeTestBase() {
   @get:Rule
   val rule = createComposeRule()

   private val defaultPageStackBoardWidth = 200.dp
   private val defaultPageStackCount = 2

   override lateinit var pageStackRepository: PageStackRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = mockk {
         every { savePageStack(any()) } answers { WritableCache(firstArg()) }
      }
   }

   @Composable
   private fun MultiColumnPageStackBoard(
      state: MultiColumnPageStackBoardState,
      width: Dp = defaultPageStackBoardWidth,
      pageStackCount: Int = defaultPageStackCount
   ) {
      val (defaultPageComposableSwitcher, defaultPageStateStore)
            = rememberDefaultPageComposableSwitcher()

      MultiColumnPageStackBoard(
         state,
         width,
         pageStackCount,
         defaultPageComposableSwitcher,
         defaultPageStateStore
      )
   }

   @OptIn(ExperimentalMaterial3Api::class)
   @Composable
   private fun MultiColumnPageStackBoard(
      state: MultiColumnPageStackBoardState,
      width: Dp = defaultPageStackBoardWidth,
      pageStackCount: Int = defaultPageStackCount,
      pageComposableSwitcher: PageComposableSwitcher,
      pageStateStore: PageStateStore
   ) {
      MultiColumnPageStackBoard(
         state,
         pageComposableSwitcher,
         pageStateStore,
         pageStackCount,
         WindowInsets(0, 0, 0, 0),
         modifier = Modifier
            .width(width)
            .testTag(pageStackBoardTag)
      )
   }

   @Composable
   private fun rememberMultiColumnPageStackBoardState(
      pageStackCount: Int
   ): RememberedPageStackBoardState<MultiColumnPageStackBoardState> {
      val animCoroutineScope = rememberCoroutineScope()
      return remember(animCoroutineScope) {
         val pageStackBoardCache = createPageStackBoard(pageStackCount)
         val pageStackBoardState = MultiColumnPageStackBoardState(
            pageStackBoardCache, pageStackRepository, animCoroutineScope)
         RememberedPageStackBoardState(pageStackBoardState, animCoroutineScope)
      }
   }

   private fun expectedPageStackWidth(
      pageStackBoardWidth: Dp = defaultPageStackBoardWidth,
      pageStackCount: Int = defaultPageStackCount
   ): Dp {
      return (pageStackBoardWidth - 16.dp) / pageStackCount - 16.dp
   }

   private fun expectedPageStackLeftPosition(
      indexInBoard: Int,
      pageStackBoardWidth: Dp = defaultPageStackBoardWidth,
      pageStackCount: Int = defaultPageStackCount
   ): Dp {
      val pageStackWidth = expectedPageStackWidth(
         pageStackBoardWidth, pageStackCount)

      return 16.dp + (pageStackWidth + 16.dp) * indexInBoard
   }

   private fun expectedScrollOffset(
      index: Int,
      pageStackBoardWidth: Dp = defaultPageStackBoardWidth,
      pageStackCount: Int = defaultPageStackCount
   ): Float {
      return with (rule.density) {
         (pageStackBoardWidth - 16.dp).toPx() / pageStackCount * index
      }
   }

   @Test
   fun layout() {
      rule.setContent {
         val (pageStackBoardState, _)
               = rememberMultiColumnPageStackBoardState(pageStackCount = 2)
         MultiColumnPageStackBoard(pageStackBoardState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
         .assertWidthIsEqualTo(expectedPageStackWidth())
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(1))
         .assertWidthIsEqualTo(expectedPageStackWidth())
   }

   @Test
   fun layout_notEnoughPageStacks() {
      rule.setContent {
         val (pageStackBoardState, _)
               = rememberMultiColumnPageStackBoardState(pageStackCount = 1)
         MultiColumnPageStackBoard(pageStackBoardState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
         .assertWidthIsEqualTo(expectedPageStackWidth())
   }

   @Test
   fun layout_omitComposingInvisibles() {
      val boardWidth = 100.dp

      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 5)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState, boardWidth)
      }

      rule.onNodeWithText("0").assertExists()
      rule.onNodeWithText("1").assertExists()
      rule.onNodeWithText("2").assertExists() // 2はギリギリ存在する（影が見えるため）
      rule.onNodeWithText("3").assertDoesNotExist()
      rule.onNodeWithText("4").assertDoesNotExist()

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("0").assertExists()
      rule.onNodeWithText("1").assertExists()
      rule.onNodeWithText("2").assertExists()
      rule.onNodeWithText("3").assertExists()
      rule.onNodeWithText("4").assertDoesNotExist()

      coroutineScope.launch {
         pageStackBoardState.animateScroll(2, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("0").assertDoesNotExist()
      rule.onNodeWithText("1").assertExists() // 1はギリギリ存在する
      rule.onNodeWithText("2").assertExists()
      rule.onNodeWithText("3").assertExists()
      rule.onNodeWithText("4").assertExists()

      coroutineScope.launch {
         pageStackBoardState.animateScroll(3, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("0").assertDoesNotExist()
      rule.onNodeWithText("1").assertDoesNotExist()
      rule.onNodeWithText("2").assertExists()
      rule.onNodeWithText("3").assertExists()
      rule.onNodeWithText("4").assertExists()
   }

   @Test
   fun layout_mutatePageStackBoard() {
      fun assertPageNumbers(
         expectedPageNumbers: List<Int>,
         pageStackBoard: PageStackBoard
      ) {
         val pageStackCount = pageStackBoard.pageStackCount
         assertEquals(expectedPageNumbers.size, pageStackCount)

         for (i in 0 until pageStackCount) {
            val page = pageStackBoard[i].pageStackCache.value.head.page
            assertIs<TestPage>(page)
            assertEquals(expectedPageNumbers[i], page.i)
         }
      }

      fun assertPageStackLayoutStatesExist(
         pageStackBoard: PageStackBoard,
         layoutLogic: MultiColumnLayoutLogic
      ) {
         val pageStackCount = pageStackBoard.pageStackCount
         val ids = (0 until pageStackCount).map { pageStackBoard[it].id }

         assertEquals(ids, layoutLogic.layoutStateList.map { it.pageStackId })

         assertEquals(pageStackCount, layoutLogic.layoutStateMap.size)
         for (id in ids) {
            assertContains(layoutLogic.layoutStateMap, id)
         }
      }

      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 2)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertPageNumbers(listOf(0, 1), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)
      }

      // ---- insert first ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.inserted(0, createPageStack(2))
      )

      assertFalse(
         pageStackBoardState.layout.pageStackLayout(0).isInitialized)

      rule.runOnIdle {
         assertPageNumbers(listOf(2, 0, 1), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)

         assertTrue(
            pageStackBoardState.layout.pageStackLayout(0).isInitialized)
      }

      // ---- insert last ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.inserted(3, createPageStack(3))
      )

      assertFalse(
         pageStackBoardState.layout.pageStackLayout(3).isInitialized)

      rule.runOnIdle {
         assertPageNumbers(listOf(2, 0, 1, 3), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)

         assertTrue(
            pageStackBoardState.layout.pageStackLayout(3).isInitialized)
      }

      // ---- insert middle ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.inserted(2, createPageStack(4))
      )

      assertFalse(
         pageStackBoardState.layout.pageStackLayout(2).isInitialized)

      rule.runOnIdle {
         assertPageNumbers(listOf(2, 0, 4, 1, 3), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)

         assertTrue(
            pageStackBoardState.layout.pageStackLayout(2).isInitialized)
      }

      // ---- replace ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.replaced(2, createPageStack(5))
      )

      assertFalse(
         pageStackBoardState.layout.pageStackLayout(2).isInitialized)

      rule.runOnIdle {
         assertPageNumbers(listOf(2, 0, 5, 1, 3), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)

         assertTrue(
            pageStackBoardState.layout.pageStackLayout(2).isInitialized)
      }

      // ---- remove first ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.removed(0)
      )

      rule.runOnIdle {
         assertPageNumbers(listOf(0, 5, 1, 3), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)
      }

      // ---- remove last ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.removed(3)
      )

      rule.runOnIdle {
         assertPageNumbers(listOf(0, 5, 1), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)
      }

      // ---- remove middle ----

      pageStackBoardState.pageStackBoard = PageStackBoard(
         pageStackBoardState.pageStackBoard.rootRow.removed(1)
      )

      rule.runOnIdle {
         assertPageNumbers(listOf(0, 1), pageStackBoardState.pageStackBoard)

         assertPageStackLayoutStatesExist(
            pageStackBoardState.pageStackBoard, pageStackBoardState.layout)
      }
   }

   @Test
   fun scroll() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 3)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
      rule.onNodeWithText("1")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(1))
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
      rule.onNodeWithText("1").assertLeftPositionInRootIsEqualTo(
         expectedPageStackLeftPosition(1) - scrollAmount)
      rule.runOnIdle {
         assertEquals(
            with (rule.density) { scrollAmount.toPx() },
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scrollEdge() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 3)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
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

      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 3)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState, boardWidth)
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

      boardWidth = 190.dp
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

      boardWidth = 200.dp
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1, pageStackBoardWidth = 190.dp),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
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
   fun scroll_snap() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(20.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap_tooFast() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(64.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(64.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(64.dp, duration = 50L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap_edges() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(20.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      repeat (2) {
         rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
      }
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun scroll_snap_afterImmobility() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      val halfWidth = (expectedPageStackWidth() + 16.dp) / 2

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
   fun scroll_swipe_overMultiplePageStacks() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(0.0f, pageStackBoardState.scrollState.scrollOffset)
      }

      rule.onNodeWithTag(pageStackBoardTag)
         .swipeLeft(expectedPageStackWidth() + 40.dp, duration = 200L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag)
         .swipeRight(expectedPageStackWidth() + 40.dp, duration = 200L)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun overscroll() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
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
               expectedScrollOffset(2),
               pageStackBoardState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )

         pageStackBoardState.scrollState.scroll(enableOverscroll = true) {
            scrollBy(scrollDistance)

            assertEquals(
               expectedScrollOffset(2) + scrollDistance,
               pageStackBoardState.scrollState.scrollOffset,
               absoluteTolerance = 0.05f
            )
         }

         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset,
            absoluteTolerance = 0.05f
         )
      }

      rule.waitForIdle()
   }

   @Test
   fun animateScroll() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      var pageStackCount by mutableStateOf(2)
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(
            remembered.pageStackBoardState,
            pageStackCount = pageStackCount
         )
      }

      class ScrollParameterType
      val byIndex = ScrollParameterType()
      val byId = ScrollParameterType()

      suspend fun animateScroll(
         pageStack: Int,
         targetPositionInBoard: PositionInBoard,
         parameterType: ScrollParameterType
      ) {
         when (parameterType) {
            byIndex -> {
               pageStackBoardState
                  .animateScroll(pageStack, targetPositionInBoard)
            }
            byId -> {
               pageStackBoardState.animateScroll(
                  PageStackBoard.PageStackId(pageStack.toLong()),
                  targetPositionInBoard
               )
            }
         }
      }

      fun assertScrollOffset(leftmostPageStackIndex: Int) {
         rule.onNodeWithText("$leftmostPageStackIndex")
            .assertLeftPositionInRootIsEqualTo(
               expectedPageStackLeftPosition(0, pageStackCount = pageStackCount)
            )

         rule.runOnIdle {
            assertEquals(
               expectedScrollOffset(
                  leftmostPageStackIndex, pageStackCount = pageStackCount
               ),
               pageStackBoardState.scrollState.scrollOffset
            )
         }
      }

      for (parameterType in listOf(byIndex, byId)) {
         // -------- pageStackCount = 2 --------
         pageStackCount = 2
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.FirstVisible, byIndex)
         }
         assertScrollOffset(0)

         // ==== FirstVisible ====

         //  0]1 2]3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(1)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0[1 2]3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(1)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(0)

         coroutineScope.launch {
            assertFails {
               animateScroll(4, PositionInBoard.FirstVisible, parameterType)
            }
            assertFails {
               animateScroll(-1, PositionInBoard.FirstVisible, parameterType)
            }
         }

         // ==== LastVisible ====

         //  0[1 2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(1)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0[1 2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(1)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(0)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(2)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(0)

         coroutineScope.launch {
            assertFails {
               pageStackBoardState.animateScroll(4, PositionInBoard.LastVisible)
            }
            assertFails {
               pageStackBoardState.animateScroll(-1, PositionInBoard.LastVisible)
            }
         }

         // ==== NearestVisible ====

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0[1 2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(1)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0[1 2]3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(1)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0 1[2 3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(2)

         // [0 1]2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(0)

         coroutineScope.launch {
            assertFails {
               animateScroll(4, PositionInBoard.NearestVisible, parameterType)
            }
            assertFails {
               animateScroll(-1, PositionInBoard.NearestVisible, parameterType)
            }
         }

         // -------- pageStackCount = 1 --------
         pageStackCount = 1
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.FirstVisible, byIndex)
         }
         assertScrollOffset(0)

         // ==== FirstVisible ====

         //  0[1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(1)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0 1 2[3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(3)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0[1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(1)

         // [0]1 2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(2)

         // [0]1 2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.FirstVisible, parameterType)
         }
         assertScrollOffset(0)

         coroutineScope.launch {
            assertFails {
               animateScroll(4, PositionInBoard.FirstVisible, parameterType)
            }
            assertFails {
               animateScroll(-1, PositionInBoard.FirstVisible, parameterType)
            }
         }

         // ==== LastVisible ====

         //  0[1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(1)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0 1 2[3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(3)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0[1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(1)

         // [0]1 2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(2)

         // [0]1 2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.LastVisible, parameterType)
         }
         assertScrollOffset(0)

         coroutineScope.launch {
            assertFails {
               animateScroll(4, PositionInBoard.LastVisible, parameterType)
            }
            assertFails {
               animateScroll(-1, PositionInBoard.LastVisible, parameterType)
            }
         }

         // ==== NearestVisible ====

         //  0[1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(1)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0 1 2[3]
         coroutineScope.launch {
            animateScroll(3, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(3)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(2)

         //  0[1]2 3
         coroutineScope.launch {
            animateScroll(1, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(1)

         // [0]1 2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(0)

         //  0 1[2]3
         coroutineScope.launch {
            animateScroll(2, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(2)

         // [0]1 2 3
         coroutineScope.launch {
            animateScroll(0, PositionInBoard.NearestVisible, parameterType)
         }
         assertScrollOffset(0)

         coroutineScope.launch {
            assertFails {
               animateScroll(4, PositionInBoard.NearestVisible, parameterType)
            }
            assertFails {
               animateScroll(-1, PositionInBoard.NearestVisible, parameterType)
            }
         }
      }
   }

   @Test
   fun firstVisibleIndex() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.firstVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-expectedScrollOffset(1) + 1.0f, 0.0f))
      }
      rule.onNodeWithText("0")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(1.0f, it.left + it.width, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.firstVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         moveBy(Offset(-2.0f, 0.0f))
      }
      rule.onNodeWithText("0")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(-1.0f, it.left + it.width, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.firstVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         up()
      }
      rule.onNodeWithText("0")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(0.0f, it.left + it.width, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.firstVisiblePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(0, PositionInBoard.FirstVisible)
      }

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.firstVisiblePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.firstVisiblePageStackIndex)
      }
   }

   @Test
   fun lastVisibleIndex() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.lastVisiblePageStackIndex)
      }

      val pageStackBoardWidth = with (rule.density) {
         defaultPageStackBoardWidth.toPx()
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-expectedScrollOffset(1) + 1.0f, 0.0f))
      }
      rule.onNodeWithText("2")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               pageStackBoardWidth - with (rule.density) { 16.dp.toPx() } + 1.0f,
               it.left + it.width,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.lastVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         moveBy(Offset(-2.0f, 0.0f))
      }
      rule.onNodeWithText("3")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               pageStackBoardWidth - 1.0f,
               it.left,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(3, pageStackBoardState.lastVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         up()
      }
      rule.onNodeWithText("3")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               pageStackBoardWidth,
               it.left,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.lastVisiblePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.lastVisiblePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(2, PositionInBoard.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.lastVisiblePageStackIndex)
      }
   }

   @Test
   fun activePageStack_detectClick() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithText("1").performClick()

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(2)
      }

      rule.onNodeWithText("2").performClick()

      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.activePageStackIndex)
      }

      rule.onNodeWithText("1").performClick()

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }
   }

   @Test
   fun activePageStack_scrolling() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.activePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }

      rule.onNodeWithText("2").performClick()

      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.activePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }
   }

   // TODO: Row内のPageStackとかでもテストする
   @Test
   fun addPageStack_viaPageStackState() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 2)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         val (pageComposableSwitcher, pageStateStore)
               = rememberPageComposableSwitcher<TestPage>(
                  { _, _ -> TestPageState() },
                  { page, _, pageStackState ->
                     Button(
                        onClick = {
                           val newPage = TestPage(page.i + 100)
                           val newPageStack = PageStack(
                              PageStack.Id(pageStackState.pageStack.id.value + 100L),
                              PageStack.SavedPageState(
                                 PageStack.PageId(newPage.i.toLong()),
                                 newPage
                              )
                           )
                           pageStackState.addColumn(newPageStack)
                        }
                     ) {
                        Text("Add PageStack ${page.i}")
                     }
                  }
               )

         MultiColumnPageStackBoard(
            remembered.pageStackBoardState,
            pageComposableSwitcher = pageComposableSwitcher,
            pageStateStore = pageStateStore
         )
      }

      fun assertPageNumbers(expected: List<Int>, actual: PageStackBoard) {
         val pages = actual.sequence()
            .map { assertIs<PageStackBoard.PageStack>(it) }
            .map { it.pageStackCache.value }
            .map { assertIs<TestPage>(it.head.page) }
            .toList()

         assertContentEquals(expected, pages.map { it.i })
      }

      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 1),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.mainClock.autoAdvance = false
      rule.onNodeWithText("Add PageStack 1").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 1, 101),
            pageStackBoardState.pageStackBoard
         )

         // ボタン押下直後、まだBoardは動いていない
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )

         // 挿入されるPageStackは透明
         assertEquals(0.0f, pageStackBoardState.layout.pageStackLayout(2).alpha)
      }
      // PageStackひとつ分スクロールされるまで進める
      rule.mainClock.advanceTimeUntil {
         expectedScrollOffset(1) == pageStackBoardState.scrollState.scrollOffset
      }
      rule.runOnIdle {
         // PageStack挿入アニメーションが開始されているがまだ終わっていない
         assertNotEquals(1.0f, pageStackBoardState.layout.pageStackLayout(2).alpha)
      }
      // アニメーション終了まで進める
      rule.mainClock.autoAdvance = true
      rule.runOnIdle {
         // 挿入アニメーション終了後不透明度は100%
         assertEquals(1.0f, pageStackBoardState.layout.pageStackLayout(2).alpha)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(PageStackBoard.PageStackId(0L))
      }

      rule.onNodeWithText("Add PageStack 0").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 100, 1, 101),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithText("Add PageStack 100").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 100, 200, 1, 101),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun removePageStack_viaPageStackState() {
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 6)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState)
      }

      fun assertPageNumbers(expected: List<Int>, actual: PageStackBoard) {
         val pages = actual.sequence()
            .map { assertIs<PageStackBoard.PageStack>(it) }
            .map { it.pageStackCache.value }
            .map { assertIs<TestPage>(it.head.page) }
            .toList()

         assertContentEquals(expected, pages.map { it.i })
      }

      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 1, 2, 3, 4, 5),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithText("Remove PageStack 1").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 2, 3, 4, 5),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithText("Remove PageStack 0").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(2, 3, 4, 5),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("Remove PageStack 3").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(2, 4, 5),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithText("Remove PageStack 5").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(2, 4),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithText("Remove PageStack 4").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(2),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun pageComposableCalled() {
      class PageA : Page()
      class PageB : Page()

      class PageAState : PageState()
      class PageBState : PageState()

      var pageAHeaderComposed = false
      var pageBHeaderComposed = false
      var pageAHeaderActionsComposed = false
      var pageBHeaderActionsComposed = false
      var pageAContentComposed = false
      var pageBContentComposed = false
      var pageAFooterComposed = false
      var pageBFooterComposed = false

      val pageAComposable = pageComposable<PageA, PageAState>(
         pageStateFactory { _, _ -> PageAState() },
         content = { _, _, _ ->
            DisposableEffect(Unit) {
               pageAContentComposed = true
               onDispose {
                  pageAContentComposed = false
               }
            }
         },
         header = { _, _, _ ->
            DisposableEffect(Unit) {
               pageAHeaderComposed = true
               onDispose {
                  pageAHeaderComposed = false
               }
            }
         },
         headerActions = { _, _, _ ->
            DisposableEffect(Unit) {
               pageAHeaderActionsComposed = true
               onDispose {
                  pageAHeaderActionsComposed = false
               }
            }
         },
         footer = { _, _, _ ->
            DisposableEffect(Unit) {
               pageAFooterComposed = true
               onDispose {
                  pageAFooterComposed = false
               }
            }
         },
         pageTransitions = {}
      )

      val pageBComposable = pageComposable<PageB, PageBState>(
         pageStateFactory { _, _ -> PageBState() },
         content = { _, _, _ ->
            DisposableEffect(Unit) {
               pageBContentComposed = true
               onDispose {
                  pageBContentComposed = false
               }
            }
         },
         header = { _, _, _ ->
            DisposableEffect(Unit) {
               pageBHeaderComposed = true
               onDispose {
                  pageBHeaderComposed = false
               }
            }
         },
         headerActions = { _, _, _ ->
            DisposableEffect(Unit) {
               pageBHeaderActionsComposed = true
               onDispose {
                  pageBHeaderActionsComposed = false
               }
            }
         },
         footer = { _, _, _ ->
            DisposableEffect(Unit) {
               pageBFooterComposed = true
               onDispose {
                  pageBFooterComposed = false
               }
            }
         },
         pageTransitions = {}
      )

      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()

         pageStackBoardState = remember {
            MultiColumnPageStackBoardState(
               createPageStackBoard(PageA()),
               pageStackRepository,
               coroutineScope
            )
         }

         MultiColumnPageStackBoard(
            pageStackBoardState,
            pageComposableSwitcher = remember {
               PageComposableSwitcher(
                  listOf(pageAComposable, pageBComposable)
               )
            },
            pageStateStore = remember {
               PageStateStore(
                  listOf(pageAComposable.pageStateFactory, pageBComposable.pageStateFactory),
                  coroutineScope
               )
            }
         )
      }

      rule.runOnIdle {
         assertTrue(pageAHeaderComposed)
         assertTrue(pageAHeaderActionsComposed)
         assertTrue(pageAContentComposed)
         assertTrue(pageAFooterComposed)
         assertFalse(pageBHeaderComposed)
         assertFalse(pageBHeaderActionsComposed)
         assertFalse(pageBContentComposed)
         assertFalse(pageBFooterComposed)
      }

      pageStackBoardState.pageStackState(0).startPage(PageB())

      rule.runOnIdle {
         assertFalse(pageAHeaderComposed)
         assertFalse(pageAHeaderActionsComposed)
         assertFalse(pageAContentComposed)
         assertFalse(pageAFooterComposed)
         assertTrue(pageBHeaderComposed)
         assertTrue(pageBHeaderActionsComposed)
         assertTrue(pageBContentComposed)
         assertTrue(pageBFooterComposed)
      }

      pageStackBoardState.pageStackState(0).finishPage()

      rule.runOnIdle {
         assertTrue(pageAHeaderComposed)
         assertTrue(pageAHeaderActionsComposed)
         assertTrue(pageAContentComposed)
         assertTrue(pageAFooterComposed)
         assertFalse(pageBHeaderComposed)
         assertFalse(pageBHeaderActionsComposed)
         assertFalse(pageBContentComposed)
         assertFalse(pageBFooterComposed)
      }
   }

   @Test
   fun pageComposableArguments() {
      var contentArgumentPage:       TestPage? = null
      var headerArgumentPage:        TestPage? = null
      var headerActionsArgumentPage: TestPage? = null
      var footerArgumentPage:        TestPage? = null
      var contentArgumentPageState:       TestPageState? = null
      var headerArgumentPageState:        TestPageState? = null
      var headerActionsArgumentPageState: TestPageState? = null
      var footerArgumentPageState:        TestPageState? = null
      var contentArgumentPageStackState:       PageStackState? = null
      var headerArgumentPageStackState:        PageStackState? = null
      var headerActionsArgumentPageStackState: PageStackState? = null
      var footerArgumentPageStackState:        PageStackState? = null

      val pageComposable = pageComposable<TestPage, TestPageState>(
         pageStateFactory { _, _ -> TestPageState() },
         content = { page, pageState, pageStackState ->
            contentArgumentPage = page
            contentArgumentPageState = pageState
            contentArgumentPageStackState = pageStackState
         },
         header = { page, pageState, pageStackState ->
            headerArgumentPage = page
            headerArgumentPageState = pageState
            headerArgumentPageStackState = pageStackState
         },
         headerActions = { page, pageState, pageStackState ->
            headerActionsArgumentPage = page
            headerActionsArgumentPageState = pageState
            headerActionsArgumentPageStackState = pageStackState
         },
         footer = { page, pageState, pageStackState ->
            footerArgumentPage = page
            footerArgumentPageState = pageState
            footerArgumentPageStackState = pageStackState
         },
         pageTransitions = {}
      )

      lateinit var pageStackBoardState: MultiColumnPageStackBoardState

      val page = TestPage(0)

      rule.setContent {
         val coroutineScope = rememberCoroutineScope()

         pageStackBoardState = remember {
            MultiColumnPageStackBoardState(
               createPageStackBoard(page),
               pageStackRepository,
               coroutineScope
            )
         }

         MultiColumnPageStackBoard(
            pageStackBoardState,
            pageComposableSwitcher = remember {
               PageComposableSwitcher(
                  listOf(pageComposable)
               )
            },
            pageStateStore = remember {
               PageStateStore(
                  listOf(pageComposable.pageStateFactory),
                  coroutineScope
               )
            }
         )
      }

      rule.runOnIdle {
         assertSame(page, contentArgumentPage)
         assertSame(page, headerArgumentPage)
         assertSame(page, headerActionsArgumentPage)
         assertSame(page, footerArgumentPage)

         assertSame(contentArgumentPageState, headerArgumentPageState)
         assertSame(contentArgumentPageState, headerActionsArgumentPageState)
         assertSame(contentArgumentPageState, footerArgumentPageState)

         val pageStackState = pageStackBoardState.pageStackState(0)
         assertSame(pageStackState, contentArgumentPageStackState)
         assertSame(pageStackState, headerArgumentPageStackState)
         assertSame(pageStackState, headerActionsArgumentPageStackState)
         assertSame(pageStackState, footerArgumentPageStackState)
      }
   }
}
