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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

@RunWith(RobolectricTestRunner::class)
class MultiColumnDeckOperationTest : MultiColumnDeckTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   @Test
   fun animateScroll() {
      val deckState = createDefaultDeckState<Int>()
      lateinit var coroutineScope: CoroutineScope
      var columnCount by mutableIntStateOf(2)
      var windowInsets by mutableStateOf(WindowInsets(0))
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         MultiColumnDeck(
            deck = remember { createDeck(cardCount = 4) },
            deckState,
            columnCount = columnCount,
            windowInsets = windowInsets
         )
      }

      class ScrollParameterType
      val byIndex = ScrollParameterType()
      val byKey = ScrollParameterType()

      suspend fun animateScroll(
         card: Int,
         targetPositionInDeck: PositionInDeck,
         parameterType: ScrollParameterType
      ) {
         when (parameterType) {
            byIndex -> {
               deckState.animateScroll(card, targetPositionInDeck)
            }
            byKey -> {
               deckState.animateScrollByKey(card, targetPositionInDeck)
            }
         }
      }

      fun assertScrollOffset(leftmostCardIndex: Int) {
         rule.onNodeWithText("$leftmostCardIndex")
            .assertLeftPositionInRootIsEqualTo(
               expectedCardLeftPosition(0,
                  columnCount = columnCount, windowInsets = windowInsets)
            )

         rule.runOnIdle {
            assertEquals(
               expectedScrollOffset(leftmostCardIndex,
                  columnCount = columnCount, windowInsets = windowInsets),
               deckState.scrollState.scrollOffset
            )
         }
      }

      for (insets in listOf(
         WindowInsets(left = 0, right = 0),
         WindowInsets(left = 32.dp, right = 32.dp)))
      {
         windowInsets = insets

         for (parameterType in listOf(byIndex, byKey)) {
            // -------- columnCount = 2 --------
            columnCount = 2
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.FirstVisible, byIndex)
            }
            assertScrollOffset(0)

            // ==== FirstVisible ====

            //  0]1 2]3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(1)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0[1 2]3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(1)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(0)

            coroutineScope.launch {
               assertFails {
                  animateScroll(4, PositionInDeck.FirstVisible, parameterType)
               }
               assertFails {
                  animateScroll(-1, PositionInDeck.FirstVisible, parameterType)
               }
            }

            // ==== LastVisible ====

            //  0[1 2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(1)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0[1 2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(1)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(0)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(2)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(0)

            coroutineScope.launch {
               assertFails {
                  animateScroll(4, PositionInDeck.LastVisible, parameterType)
               }
               assertFails {
                  animateScroll(-1, PositionInDeck.LastVisible, parameterType)
               }
            }

            // ==== NearestVisible ====

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0[1 2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(1)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0[1 2]3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(1)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0 1[2 3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(2)

            // [0 1]2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(0)

            coroutineScope.launch {
               assertFails {
                  animateScroll(4, PositionInDeck.NearestVisible, parameterType)
               }
               assertFails {
                  animateScroll(-1, PositionInDeck.NearestVisible, parameterType)
               }
            }

            // -------- columnCount = 1 --------
            columnCount = 1
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.FirstVisible, byIndex)
            }
            assertScrollOffset(0)

            // ==== FirstVisible ====

            //  0[1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(1)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0 1 2[3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(3)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0[1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(1)

            // [0]1 2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(2)

            // [0]1 2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.FirstVisible, parameterType)
            }
            assertScrollOffset(0)

            coroutineScope.launch {
               assertFails {
                  animateScroll(4, PositionInDeck.FirstVisible, parameterType)
               }
               assertFails {
                  animateScroll(-1, PositionInDeck.FirstVisible, parameterType)
               }
            }

            // ==== LastVisible ====

            //  0[1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(1)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0 1 2[3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(3)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0[1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(1)

            // [0]1 2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(2)

            // [0]1 2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.LastVisible, parameterType)
            }
            assertScrollOffset(0)

            coroutineScope.launch {
               assertFails {
                  animateScroll(4, PositionInDeck.LastVisible, parameterType)
               }
               assertFails {
                  animateScroll(-1, PositionInDeck.LastVisible, parameterType)
               }
            }

            // ==== NearestVisible ====

            //  0[1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(1)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0 1 2[3]
            coroutineScope.launch {
               animateScroll(3, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(3)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(2)

            //  0[1]2 3
            coroutineScope.launch {
               animateScroll(1, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(1)

            // [0]1 2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(0)

            //  0 1[2]3
            coroutineScope.launch {
               animateScroll(2, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(2)

            // [0]1 2 3
            coroutineScope.launch {
               animateScroll(0, PositionInDeck.NearestVisible, parameterType)
            }
            assertScrollOffset(0)

            coroutineScope.launch {
               assertFails {
                  animateScroll(4, PositionInDeck.NearestVisible, parameterType)
               }
               assertFails {
                  animateScroll(-1, PositionInDeck.NearestVisible, parameterType)
               }
            }
         }
      }
   }
}
