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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class MultiColumnDeckActiveCardTest : MultiColumnDeckTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   @Test
   fun detectClick() {
      val deckState = createDeckState(cardCount = 4)

      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         MultiColumnDeck(deckState)
      }

      rule.onNodeWithText("1").performClick()

      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(2)
      }

      rule.onNodeWithText("2").performClick()

      rule.runOnIdle {
         assertEquals(2, deckState.activeCardIndex)
      }

      rule.onNodeWithText("1").performClick()

      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }
   }

   @Test
   fun scrolling() {
      val deckState = createDeckState(cardCount = 4)

      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         MultiColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(0, deckState.activeCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }

      rule.onNodeWithText("2").performClick()
      rule.runOnIdle {
         assertEquals(2, deckState.activeCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.LastVisible)
      }
      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }
   }

   @Test
   fun scrolling_windowInsets() {
      val windowInsets = WindowInsets(left = 32.dp, right = 32.dp)
      val deckState = createDeckState(cardCount = 4)

      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         MultiColumnDeck(deckState, windowInsets = windowInsets)
      }

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(2, PositionInDeck.FirstVisible)
      }
      rule.runOnIdle {
         assertEquals(2, deckState.activeCardIndex)
      }

      rule.onNodeWithText("3").performClick()
      rule.runOnIdle {
         assertEquals(3, deckState.activeCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(2, PositionInDeck.LastVisible)
      }
      rule.runOnIdle {
         assertEquals(2, deckState.activeCardIndex)
      }
   }
}
