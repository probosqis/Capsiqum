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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
class MultiColumnPageStackBoardOperationTest : MultiColumnPageStackBoardComposeTestBase() {
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
      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      var pageStackCount by mutableStateOf(2)
      var windowInsets by mutableStateOf(WindowInsets(0, 0, 0, 0))
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(
            remembered.pageStackBoardState,
            pageStackCount = pageStackCount,
            windowInsets = windowInsets
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
               expectedPageStackLeftPosition(0,
                  pageStackCount = pageStackCount, windowInsets = windowInsets)
            )

         rule.runOnIdle {
            assertEquals(
               expectedScrollOffset(leftmostPageStackIndex,
                  pageStackCount = pageStackCount, windowInsets = windowInsets),
               pageStackBoardState.scrollState.scrollOffset
            )
         }
      }

      for (insets in listOf(
         WindowInsets(left = 0, right = 0),
         WindowInsets(left = 32.dp, right = 32.dp)))
      {
         windowInsets = insets

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
}
