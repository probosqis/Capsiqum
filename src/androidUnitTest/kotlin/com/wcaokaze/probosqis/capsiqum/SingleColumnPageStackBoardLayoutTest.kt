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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SingleColumnPageStackBoardLayoutTest : SingleColumnPageStackBoardComposeTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   override lateinit var pageStackRepository: PageStackRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = mockk {
         every { savePageStack(any()) } answers { WritableCache(firstArg()) }
      }
   }

   @Test
   fun basicLayout() {
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
   fun omitComposingInvisibles() {
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
   fun mutatePageStackBoard() {
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
         content = { _, _, _, _ ->
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
         content = { _, _, _, _ ->
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
}
