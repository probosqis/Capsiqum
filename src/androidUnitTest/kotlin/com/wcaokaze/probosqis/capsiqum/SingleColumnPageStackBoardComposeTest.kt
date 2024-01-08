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

import androidx.compose.foundation.layout.Column
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
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SingleColumnPageStackBoardComposeTest : PageStackBoardComposeTestBase() {
   @get:Rule
   val rule = createComposeRule()

   private val defaultPageStackBoardWidth = 100.dp

   override lateinit var pageStackRepository: PageStackRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = mockk {
         every { savePageStack(any()) } answers { WritableCache(firstArg()) }
      }
   }

   @Composable
   private fun SingleColumnPageStackBoard(
      state: SingleColumnPageStackBoardState,
      width: Dp = defaultPageStackBoardWidth
   ) {
      val (defaultPageComposableSwitcher, defaultPageStateStore)
            = rememberDefaultPageComposableSwitcher()

      SingleColumnPageStackBoard(
         state,
         width,
         defaultPageComposableSwitcher,
         defaultPageStateStore
      )
   }

   @Composable
   private fun SingleColumnPageStackBoard(
      state: SingleColumnPageStackBoardState,
      width: Dp = defaultPageStackBoardWidth,
      pageComposableSwitcher: PageComposableSwitcher,
      pageStateStore: PageStateStore
   ) {
      SingleColumnPageStackBoard(
         state,
         pageComposableSwitcher,
         pageStateStore,
         WindowInsets(0, 0, 0, 0),
         modifier = Modifier
            .width(width)
            .testTag(pageStackBoardTag)
      )
   }

   @Composable
   private fun rememberSingleColumnPageStackBoardState(
      pageStackCount: Int
   ): RememberedPageStackBoardState<SingleColumnPageStackBoardState> {
      val animCoroutineScope = rememberCoroutineScope()
      return remember(animCoroutineScope) {
         val pageStackBoardCache = createPageStackBoard(pageStackCount)
         val pageStackBoardState = SingleColumnPageStackBoardState(
            pageStackBoardCache, pageStackRepository, animCoroutineScope)
         RememberedPageStackBoardState(pageStackBoardState, animCoroutineScope)
      }
   }

   private fun expectedPageStackLeftPosition(
      indexInBoard: Int,
      pageStackBoardWidth: Dp = defaultPageStackBoardWidth
   ): Dp {
      return (pageStackBoardWidth + 16.dp) * indexInBoard
   }

   private fun expectedScrollOffset(
      index: Int,
      pageStackBoardWidth: Dp = defaultPageStackBoardWidth
   ): Float {
      return with (rule.density) {
         (pageStackBoardWidth + 16.dp).toPx() * index
      }
   }

   @Test
   fun layout() {
      rule.setContent {
         val (pageStackBoardState, _)
               = rememberSingleColumnPageStackBoardState(pageStackCount = 2)
         SingleColumnPageStackBoard(pageStackBoardState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))
         .assertWidthIsEqualTo(defaultPageStackBoardWidth)
   }

   @Test
   fun layout_omitComposingInvisibles() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 3)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
      }

      rule.onNodeWithText("0").assertExists()
      rule.onNodeWithText("1").assertDoesNotExist()
      rule.onNodeWithText("2").assertDoesNotExist()

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("0").assertDoesNotExist()
      rule.onNodeWithText("1").assertExists()
      rule.onNodeWithText("2").assertDoesNotExist()

      coroutineScope.launch {
         pageStackBoardState.animateScroll(2, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("0").assertDoesNotExist()
      rule.onNodeWithText("1").assertDoesNotExist()
      rule.onNodeWithText("2").assertExists()
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
         layoutLogic: SingleColumnLayoutLogic
      ) {
         val pageStackCount = pageStackBoard.pageStackCount
         val ids = (0 until pageStackCount).map { pageStackBoard[it].id }

         assertEquals(ids, layoutLogic.layoutStateList.map { it.pageStackId })

         assertEquals(pageStackCount, layoutLogic.layoutStateMap.size)
         for (id in ids) {
            assertContains(layoutLogic.layoutStateMap, id)
         }
      }

      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 2)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
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

      rule.onNodeWithTag(pageStackBoardTag).swipeRight(20.dp)
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      repeat (3) {
         rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
      }
      rule.runOnIdle {
         assertEquals(
            expectedScrollOffset(3),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
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

      rule.onNodeWithTag(pageStackBoardTag).swipeLeft(20.dp)
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

   @Test
   fun animateScroll() {
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
            .assertLeftPositionInRootIsEqualTo(expectedPageStackLeftPosition(0))

         rule.runOnIdle {
            assertEquals(
               expectedScrollOffset(leftmostPageStackIndex),
               pageStackBoardState.scrollState.scrollOffset
            )
         }
      }

      for (parameterType in listOf(byIndex, byId)) {
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

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.firstVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-defaultPageStackBoardWidth.toPx() + 1.0f, 0.0f))
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
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               with (rule.density) { 16.dp.toPx() } - 1.0f,
               it.left,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.firstVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         up()
      }
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(0.0f, it.left, absoluteTolerance = 0.05f) }
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

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.lastVisiblePageStackIndex)
      }

      val pageStackBoardWidth = with (rule.density) {
         defaultPageStackBoardWidth.toPx()
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-expectedScrollOffset(1) + 1.0f, 0.0f))
      }
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               pageStackBoardWidth + 1.0f,
               it.left + it.width,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.lastVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         moveBy(Offset(-16.dp.toPx() - 2.0f, 0.0f))
      }
      rule.onNodeWithText("2")
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
         assertEquals(2, pageStackBoardState.lastVisiblePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         up()
      }
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(0.0f, it.left, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.lastVisiblePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(0, PositionInBoard.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.lastVisiblePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.lastVisiblePageStackIndex)
      }
   }

   @Test
   fun activePageStack() {
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

      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.activePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-(defaultPageStackBoardWidth / 2 + 8.dp).toPx() + 1.0f, 0.0f))
      }
      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.activePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         moveBy(Offset(-2.0f, 0.0f))
      }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }

      rule.onNodeWithTag(pageStackBoardTag).performTouchInput {
         up()
      }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(0)
      }
      rule.runOnIdle {
         assertEquals(0, pageStackBoardState.activePageStackIndex)
      }
   }

   // TODO: Row内のPageStackとかでもテストする
   @Test
   fun addPageStack_viaPageStackState() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 2)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
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

         SingleColumnPageStackBoard(
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
      rule.onNodeWithText("Add PageStack 0").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(0, 100, 1),
            pageStackBoardState.pageStackBoard
         )

         // ボタン押下直後、まだBoardは動いていない
         assertEquals(
            expectedScrollOffset(0),
            pageStackBoardState.scrollState.scrollOffset
         )

         // 挿入されるPageStackは透明
         assertEquals(0.0f, pageStackBoardState.layout.pageStackLayout(1).alpha)
      }
      // PageStackひとつ分スクロールされるまで進める
      rule.mainClock.advanceTimeUntil {
         expectedScrollOffset(1) == pageStackBoardState.scrollState.scrollOffset
      }
      rule.runOnIdle {
         // PageStack挿入アニメーションが開始されているがまだ終わっていない
         assertNotEquals(1.0f, pageStackBoardState.layout.pageStackLayout(1).alpha)
      }
      // アニメーション終了まで進める
      rule.mainClock.autoAdvance = true
      rule.runOnIdle {
         // 挿入アニメーション終了後不透明度は100%
         assertEquals(1.0f, pageStackBoardState.layout.pageStackLayout(1).alpha)
      }
   }

   @Test
   fun removePageStack_viaPageStackState() {
      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberSingleColumnPageStackBoardState(pageStackCount = 6)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         SingleColumnPageStackBoard(remembered.pageStackBoardState)
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

      rule.onNodeWithText("Remove PageStack 0").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(1, 2, 3, 4, 5),
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

      rule.onNodeWithText("Remove PageStack 2").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(1, 3, 4, 5),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(1),
            pageStackBoardState.scrollState.scrollOffset
         )
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(3, PositionInBoard.FirstVisible)
      }

      rule.onNodeWithText("Remove PageStack 5").performClick()
      rule.runOnIdle {
         assertPageNumbers(
            listOf(1, 3, 4),
            pageStackBoardState.pageStackBoard
         )

         assertEquals(
            expectedScrollOffset(2),
            pageStackBoardState.scrollState.scrollOffset
         )
      }
   }

   @Test
   fun pageStackHeaderFooter() {
      class PageA : Page()
      class PageB : Page()

      class PageAState : PageState()
      class PageBState : PageState()

      var pageAHeaderComposed = false
      var pageBHeaderComposed = false

      val pageAComposable = pageComposable<PageA, PageAState>(
         pageStateFactory { _, _ -> PageAState() },
         content = { _, _, _ -> },
         header = { _, _, _ ->
            DisposableEffect(Unit) {
               pageAHeaderComposed = true
               onDispose {
                  pageAHeaderComposed = false
               }
            }
         },
         footer = null,
         pageTransitions = {}
      )

      val pageBComposable = pageComposable<PageB, PageBState>(
         pageStateFactory { _, _ -> PageBState() },
         content = { _, _, _ -> },
         header = { _, _, _ ->
            DisposableEffect(Unit) {
               pageBHeaderComposed = true
               onDispose {
                  pageBHeaderComposed = false
               }
            }
         },
         footer = null,
         pageTransitions = {}
      )

      lateinit var pageStackBoardState: SingleColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()

         pageStackBoardState = remember {
            SingleColumnPageStackBoardState(
               createPageStackBoard(PageA()),
               pageStackRepository,
               coroutineScope
            )
         }

         val pageComposableSwitcher = remember {
            PageComposableSwitcher(
               listOf(pageAComposable, pageBComposable)
            )
         }

         val pageStateStore = remember {
            PageStateStore(
               listOf(pageAComposable.pageStateFactory, pageBComposable.pageStateFactory),
               coroutineScope
            )
         }

         Column {
            @OptIn(ExperimentalMaterial3Api::class)
            SingleColumnPageStackBoardAppBar(
               pageStackBoardState,
               pageComposableSwitcher,
               pageStateStore
            )

            SingleColumnPageStackBoard(
               pageStackBoardState,
               pageComposableSwitcher = pageComposableSwitcher,
               pageStateStore = pageStateStore
            )
         }
      }

      rule.runOnIdle {
         assertTrue (pageAHeaderComposed)
         assertFalse(pageBHeaderComposed)
      }

      pageStackBoardState.pageStackState(0).startPage(PageB())

      rule.runOnIdle {
         assertFalse(pageAHeaderComposed)
         assertTrue (pageBHeaderComposed)
      }

      pageStackBoardState.pageStackState(0).finishPage()

      rule.runOnIdle {
         assertTrue (pageAHeaderComposed)
         assertFalse(pageBHeaderComposed)
      }
   }
}
