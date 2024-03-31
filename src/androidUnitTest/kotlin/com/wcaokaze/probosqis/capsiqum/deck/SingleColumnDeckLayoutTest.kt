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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class SingleColumnDeckLayoutTest : SingleColumnDeckTestBase() {
   @get:Rule
   val rule = createComposeRule()

   override val density: Density
      get() = rule.density

   @Test
   fun basicLayout() {
      rule.setContent {
         val deckState = remember { createDeckState(cardCount = 2) }
         SingleColumnDeck(deckState)
      }

      rule.onNodeWithText("0")
         .assertLeftPositionInRootIsEqualTo(expectedCardLeftPosition(0))
         .assertWidthIsEqualTo(expectedCardWidth())
   }

   @Test
   fun width_sizeModifier() {
      var width by mutableStateOf(50.dp)
      val deckState = createDeckState(cardCount = 1)

      rule.setContent {
         SingleColumnDeck(
            deckState,
            sizeModifier = { Modifier.width(width).fillMaxHeight() }
         )
      }

      rule.onNodeWithTag(deckTestTag).assertWidthIsEqualTo(50.dp)
      width = 100.dp
      rule.onNodeWithTag(deckTestTag).assertWidthIsEqualTo(100.dp)
      width = 70.dp
      rule.onNodeWithTag(deckTestTag).assertWidthIsEqualTo(70.dp)
   }

   @Test
   fun width_wrapContent() {
      assertFails {
         rule.setContent {
            SingleColumnDeck(
               remember { createDeckState(cardCount = 1) },
               sizeModifier = { Modifier.wrapContentWidth().fillMaxHeight() }
            )
         }
      }
   }

   @Test
   fun height_sizeModifier() {
      var height by mutableStateOf(50.dp)
      val deckState = createDeckState(cardCount = 1)

      rule.setContent {
         SingleColumnDeck(
            deckState,
            sizeModifier = { Modifier.fillMaxWidth().height(height) }
         )
      }

      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(50.dp)
      height = 100.dp
      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(100.dp)
      height = 70.dp
      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(70.dp)
   }

   @Test
   fun height_wrapContent() {
      val deckState = SingleColumnDeckState(
         Deck(
            listOf(Deck.Card(20.dp), Deck.Card(40.dp))
         ),
         key = { it }
      )

      rule.setContent {
         SingleColumnDeck(
            deckState,
            sizeModifier = { Modifier.fillMaxWidth().wrapContentHeight() }
         ) { _, height ->
            Box(Modifier.height(height))
         }
      }

      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(20.dp)

      rule.onNodeWithTag(deckTestTag).performTouchInput { swipeLeft() }
      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(40.dp)

      rule.onNodeWithTag(deckTestTag).performTouchInput { swipeRight() }
      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(20.dp)

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop - 20.dp.toPx(), 0.0f))
         // up()
      }
      rule.onNodeWithTag(deckTestTag).assertHeightIsEqualTo(40.dp)
   }

   @Test
   fun duplicatedKeys() {
      rule.setContent {
         assertFails {
            SingleColumnDeckState(
               Deck(List(2) { Deck.Card(0) }),
               key = { it }
            )
         }

         val deckState = SingleColumnDeckState(
            Deck(listOf(Deck.Card(0))),
            key = { it }
         )

         assertFails {
            deckState.deck = Deck(List(2) { Deck.Card(0) })
         }

         assertFails {
            deckState.addColumn(1, 0)
         }
      }
   }

   @Test
   fun omitComposingInvisibles() {
      val deckState = createDeckState(cardCount = 5)
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         SingleColumnDeck(deckState)
      }

      rule.onNodeWithText("0").assertExists()
      rule.onNodeWithText("1").assertDoesNotExist()
      rule.onNodeWithText("2").assertDoesNotExist()

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.FirstVisible)
      }

      rule.onNodeWithText("0").assertDoesNotExist()
      rule.onNodeWithText("1").assertExists()
      rule.onNodeWithText("2").assertDoesNotExist()

      coroutineScope.launch {
         deckState.animateScroll(2, PositionInDeck.FirstVisible)
      }

      rule.onNodeWithText("0").assertDoesNotExist()
      rule.onNodeWithText("1").assertDoesNotExist()
      rule.onNodeWithText("2").assertExists()
   }

   @Test
   fun mutateDeck() {
      fun assertCardNumbers(expectedCardNumbers: List<Int>, deck: Deck<Int>) {
         val cardCount = deck.cardCount
         assertEquals(expectedCardNumbers.size, cardCount)

         for (i in 0 until cardCount) {
            val cardNumber = deck[i].content
            assertEquals(expectedCardNumbers[i], cardNumber)
         }
      }

      fun assertLayoutStatesExist(
         deck: Deck<Int>,
         layoutLogic: SingleColumnLayoutLogic<Int>
      ) {
         val cardCount = deck.cardCount
         val cardContents = (0 until cardCount).map { deck[it].content }

         assertEquals(cardContents, layoutLogic.layoutStateList.map { it.key })

         assertEquals(cardCount, layoutLogic.layoutStateMap.size)
         for (c in cardContents) {
            assertContains(layoutLogic.layoutStateMap, c)
         }
      }

      lateinit var coroutineScope: CoroutineScope
      val deckState = createDeckState(cardCount = 2)
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         SingleColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertCardNumbers(listOf(0, 1), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
      }

      // ---- insert first ----

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.inserted(0, Deck.Card(2))
      )

      assertFalse(deckState.layoutLogic.layoutState(0).isInitialized)

      rule.runOnIdle {
         assertCardNumbers(listOf(2, 0, 1), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertTrue(deckState.layoutLogic.layoutState(0).isInitialized)
         assertEquals(expectedScrollOffset(0), deckState.scrollOffset)
      }

      // ---- insert last ----

      coroutineScope.launch {
         deckState.animateScroll(2)
      }

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.inserted(3, Deck.Card(3))
      )

      assertFalse(deckState.layoutLogic.layoutState(3).isInitialized)

      rule.runOnIdle {
         assertCardNumbers(listOf(2, 0, 1, 3), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertTrue(deckState.layoutLogic.layoutState(3).isInitialized)
         assertEquals(expectedScrollOffset(2), deckState.scrollOffset)
      }

      // ---- insert middle ----

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.inserted(2, Deck.Card(4))
      )

      assertFalse(deckState.layoutLogic.layoutState(2).isInitialized)

      rule.runOnIdle {
         assertCardNumbers(listOf(2, 0, 4, 1, 3), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertTrue(deckState.layoutLogic.layoutState(2).isInitialized)
         assertEquals(expectedScrollOffset(2), deckState.scrollOffset)
      }

      // ---- replace ----

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.replaced(2, Deck.Card(5))
      )

      assertFalse(deckState.layoutLogic.layoutState(2).isInitialized)

      rule.runOnIdle {
         assertCardNumbers(listOf(2, 0, 5, 1, 3), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertTrue(deckState.layoutLogic.layoutState(2).isInitialized)
         assertEquals(expectedScrollOffset(2), deckState.scrollOffset)
      }

      // ---- remove first ----

      coroutineScope.launch {
         deckState.animateScroll(0)
      }

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.removed(0)
      )

      rule.runOnIdle {
         assertCardNumbers(listOf(0, 5, 1, 3), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertEquals(expectedScrollOffset(0), deckState.scrollOffset)
      }

      // ---- remove last ----

      coroutineScope.launch {
         deckState.animateScroll(3)
      }

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.removed(3)
      )

      rule.runOnIdle {
         assertCardNumbers(listOf(0, 5, 1), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertEquals(expectedScrollOffset(2), deckState.scrollOffset)
      }

      // ---- remove middle ----

      deckState.deck = Deck(
         rootRow = deckState.deck.rootRow.removed(1)
      )

      rule.runOnIdle {
         assertCardNumbers(listOf(0, 1), deckState.deck)
         assertLayoutStatesExist(deckState.deck, deckState.layoutLogic)
         assertEquals(expectedScrollOffset(1), deckState.scrollOffset)
      }
   }

   @Test
   fun firstVisibleIndex() {
      val deckState = createDeckState(cardCount = 4)
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         SingleColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(0, deckState.firstVisibleCardIndex)
         assertEquals(0, deckState.firstContentCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-expectedCardWidth().toPx() + 1.0f, 0.0f))
      }
      rule.onNodeWithText("0")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(1.0f, it.left + it.width, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(0, deckState.firstVisibleCardIndex)
         assertEquals(0, deckState.firstContentCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
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
         assertEquals(1, deckState.firstVisibleCardIndex)
         assertEquals(1, deckState.firstContentCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         moveBy(Offset(1.0f, 0.0f))
         up()
      }
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(0.0f, it.left, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(1, deckState.firstVisibleCardIndex)
         assertEquals(1, deckState.firstContentCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(0, PositionInDeck.FirstVisible)
      }

      rule.runOnIdle {
         assertEquals(0, deckState.firstVisibleCardIndex)
         assertEquals(0, deckState.firstContentCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.FirstVisible)
      }

      rule.runOnIdle {
         assertEquals(1, deckState.firstVisibleCardIndex)
         assertEquals(1, deckState.firstContentCardIndex)
      }
   }

   @Test
   fun lastVisibleIndex() {
      val deckState = createDeckState(cardCount = 4)
      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         SingleColumnDeck(deckState)
      }

      rule.runOnIdle {
         assertEquals(0, deckState.lastVisibleCardIndex)
         assertEquals(0, deckState.lastContentCardIndex)
      }

      val deckWidth = with (rule.density) { defaultDeckWidth.toPx() }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         down(Offset(0.0f, 0.0f))
         moveBy(Offset(-viewConfiguration.touchSlop, 0.0f))
         moveBy(Offset(-expectedScrollOffset(1) + 1.0f, 0.0f))
      }
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               deckWidth + 1.0f,
               it.left + it.width,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(1, deckState.lastVisibleCardIndex)
         assertEquals(1, deckState.lastContentCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         moveBy(Offset(-16.dp.toPx() - 2.0f, 0.0f))
      }
      rule.onNodeWithText("2")
         .fetchSemanticsNode()
         .boundsInRoot
         .let {
            assertEquals(
               deckWidth - 1.0f,
               it.left,
               absoluteTolerance = 0.05f
            )
         }
      rule.runOnIdle {
         assertEquals(2, deckState.lastVisibleCardIndex)
         assertEquals(2, deckState.lastContentCardIndex)
      }

      rule.onNodeWithTag(deckTestTag).performTouchInput {
         moveBy(Offset(1.0f, 0.0f))
         up()
      }
      rule.onNodeWithText("1")
         .fetchSemanticsNode()
         .boundsInRoot
         .let { assertEquals(0.0f, it.left, absoluteTolerance = 0.05f) }
      rule.runOnIdle {
         assertEquals(1, deckState.lastVisibleCardIndex)
         assertEquals(1, deckState.lastContentCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(0, PositionInDeck.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(0, deckState.lastVisibleCardIndex)
         assertEquals(0, deckState.lastContentCardIndex)
      }

      coroutineScope.launch {
         deckState.animateScroll(1, PositionInDeck.LastVisible)
      }

      rule.runOnIdle {
         assertEquals(1, deckState.lastVisibleCardIndex)
         assertEquals(1, deckState.lastContentCardIndex)
      }
   }
}
