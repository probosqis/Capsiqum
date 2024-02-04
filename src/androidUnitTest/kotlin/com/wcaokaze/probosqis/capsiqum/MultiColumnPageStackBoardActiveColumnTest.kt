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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
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
class MultiColumnPageStackBoardActiveColumnTest : MultiColumnPageStackBoardComposeTestBase() {
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
   fun detectClick() {
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
   fun scrolling() {
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

   @Test
   fun scrolling_windowInsets() {
      val windowInsets = WindowInsets(left = 32.dp, right = 32.dp)

      lateinit var pageStackBoardState: MultiColumnPageStackBoardState
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberMultiColumnPageStackBoardState(pageStackCount = 4)
         SideEffect {
            pageStackBoardState = remembered.pageStackBoardState
            coroutineScope = remembered.coroutineScope
         }
         MultiColumnPageStackBoard(remembered.pageStackBoardState, windowInsets = windowInsets)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(1, PositionInBoard.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(1, pageStackBoardState.activePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(2, PositionInBoard.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.activePageStackIndex)
      }

      rule.onNodeWithText("3").performClick()
      rule.runOnIdle {
         assertEquals(3, pageStackBoardState.activePageStackIndex)
      }

      coroutineScope.launch {
         pageStackBoardState.animateScroll(2, PositionInBoard.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(2, pageStackBoardState.activePageStackIndex)
      }
   }
}
