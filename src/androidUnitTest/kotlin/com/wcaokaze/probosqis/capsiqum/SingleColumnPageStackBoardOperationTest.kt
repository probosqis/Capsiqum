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

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.wcaokaze.probosqis.capsiqum.page.PageStack
import com.wcaokaze.probosqis.capsiqum.page.PageStackRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

@RunWith(RobolectricTestRunner::class)
class SingleColumnPageStackBoardOperationTest : SingleColumnPageStackBoardComposeTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   override lateinit var pageStackRepository: PageStackRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = createPageStackRepositoryMock()
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
}
