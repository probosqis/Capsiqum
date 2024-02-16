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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.robolectric.annotation.Config

abstract class DeckTestBase {
   protected val deckTestTag = "Deck"

   protected fun createDeck(cardCount: Int) = Deck(
      List(cardCount) { Deck.Card(it) }
   )
}

@Config(qualifiers = "w600dp")
abstract class MultiColumnDeckTestBase : DeckTestBase() {
   protected val defaultDeckWidth = 600.dp
   protected val defaultCardCount = 2
   protected val defaultWindowInsets = WindowInsets(0)

   protected abstract val density: Density

   protected fun createDeckState(cardCount: Int) = MultiColumnDeckState(
      createDeck(cardCount),
      key = { it }
   )

   @Composable
   protected fun MultiColumnDeck(
      state: MultiColumnDeckState<Int>,
      width: Dp = defaultDeckWidth,
      cardCount: Int = defaultCardCount,
      windowInsets: WindowInsets = defaultWindowInsets,
      card: @Composable (Int) -> Unit = { Text("$it", Modifier.fillMaxWidth()) }
   ) {
      MultiColumnDeck(
         state,
         cardCount,
         windowInsets = windowInsets,
         card = card,
         modifier = Modifier
            .width(width)
            .testTag(deckTestTag)
      )
   }

   protected fun expectedCardWidth(
      deckWidth: Dp = defaultDeckWidth,
      cardCount: Int = defaultCardCount,
      windowInsets: WindowInsets = defaultWindowInsets
   ): Dp {
      val leftWindowInset:  Dp
      val rightWindowInset: Dp

      with (density) {
         leftWindowInset  = windowInsets.getLeft (this, LayoutDirection.Ltr).toDp()
         rightWindowInset = windowInsets.getRight(this, LayoutDirection.Ltr).toDp()
      }

      return (deckWidth - leftWindowInset - rightWindowInset - 16.dp) / cardCount - 16.dp
   }

   protected fun expectedCardLeftPosition(
      index: Int,
      deckWidth: Dp = defaultDeckWidth,
      cardCount: Int = defaultCardCount,
      windowInsets: WindowInsets = defaultWindowInsets
   ): Dp {
      val leftWindowInset = with (density) {
         windowInsets.getLeft(this, LayoutDirection.Ltr).toDp()
      }

      val cardWidth = expectedCardWidth(deckWidth, cardCount, windowInsets)

      return leftWindowInset + 16.dp + (cardWidth + 16.dp) * index
   }

   protected fun expectedScrollOffset(
      index: Int,
      deckWidth: Dp = defaultDeckWidth,
      cardCount: Int = defaultCardCount,
      windowInsets: WindowInsets = defaultWindowInsets
   ): Float {
      val cardDistance = expectedCardWidth(
         deckWidth, cardCount, windowInsets) + 16.dp

      return with (density) { cardDistance.toPx() * index }
   }
}
