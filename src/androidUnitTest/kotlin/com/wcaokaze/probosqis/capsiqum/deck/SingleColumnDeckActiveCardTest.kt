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

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SingleColumnDeckActiveCardTest : SingleColumnDeckTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   @Test
   fun activeCard() {
      val deckState = createDeckState(cardCount = 4)

      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         SingleColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(0, deckState.activeCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-expectedScrollOffset(1) / 2 + 1.0f, 0.0f))
      }
      rule.runOnIdle {
         assertEquals(0, deckState.activeCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         moveBy(Offset(-2.0f, 0.0f))
      }
      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         up()
      }
      rule.runOnIdle {
         assertEquals(1, deckState.activeCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(0)
      }
      rule.runOnIdle {
         assertEquals(0, deckState.activeCardIndex)
      }
   }
}
