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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
   protected fun <T : Any> createDefaultDeckState() = MultiColumnDeckState<T>(key = { it })
   protected val defaultDeckWidth = 600.dp
   protected val defaultColumnCount = 2
   protected val defaultWindowInsets = WindowInsets(0)
   protected val defaultCardComposable: @Composable (Int, Any?) -> Unit = { _, content ->
      Text("$content", Modifier.fillMaxWidth())
   }

   protected abstract val density: Density

   @Composable
   protected fun <T : Any> MultiColumnDeck(
      deck: Deck<T>,
      state: MultiColumnDeckState<T> = remember { createDefaultDeckState() },
      width: Dp = defaultDeckWidth,
      columnCount: Int = defaultColumnCount,
      windowInsets: WindowInsets = defaultWindowInsets,
      cardPadding: Dp = MultiColumnDeckDefaults.CardPadding,
      card: @Composable (index: Int, T) -> Unit = defaultCardComposable,
   ) {
      MultiColumnDeck(
         deck, state, sizeModifier = { Modifier.width(width).fillMaxHeight() },
         columnCount, windowInsets, cardPadding, card
      )
   }

   @Composable
   protected fun <T : Any> MultiColumnDeck(
      deck: Deck<T>,
      state: MultiColumnDeckState<T> = remember { createDefaultDeckState() },
      sizeModifier: () -> Modifier,
      columnCount: Int = defaultColumnCount,
      windowInsets: WindowInsets = defaultWindowInsets,
      cardPadding: Dp = MultiColumnDeckDefaults.CardPadding,
      card: @Composable (index: Int, T) -> Unit = defaultCardComposable,
   ) {
      MultiColumnDeck(
         deck,
         state,
         columnCount,
         windowInsets = windowInsets,
         cardPadding = cardPadding,
         card = card,
         modifier = Modifier
            .then(sizeModifier())
            .testTag(deckTestTag)
      )
   }

   protected fun expectedCardWidth(
      deckWidth: Dp = defaultDeckWidth,
      columnCount: Int = defaultColumnCount,
      windowInsets: WindowInsets = defaultWindowInsets
   ): Dp {
      val leftWindowInset:  Dp
      val rightWindowInset: Dp

      with (density) {
         leftWindowInset  = windowInsets.getLeft (this, LayoutDirection.Ltr).toDp()
         rightWindowInset = windowInsets.getRight(this, LayoutDirection.Ltr).toDp()
      }

      return (deckWidth - leftWindowInset - rightWindowInset - 16.dp) / columnCount - 16.dp
   }

   protected fun expectedCardLeftPosition(
      index: Int,
      deckWidth: Dp = defaultDeckWidth,
      columnCount: Int = defaultColumnCount,
      windowInsets: WindowInsets = defaultWindowInsets
   ): Dp {
      val leftWindowInset = with (density) {
         windowInsets.getLeft(this, LayoutDirection.Ltr).toDp()
      }

      val cardWidth = expectedCardWidth(deckWidth, columnCount, windowInsets)

      return leftWindowInset + 16.dp + (cardWidth + 16.dp) * index
   }

   protected fun expectedScrollOffset(
      index: Int,
      deckWidth: Dp = defaultDeckWidth,
      columnCount: Int = defaultColumnCount,
      windowInsets: WindowInsets = defaultWindowInsets
   ): Float {
      val cardDistance = expectedCardWidth(
         deckWidth, columnCount, windowInsets) + 16.dp

      return with (density) { cardDistance.toPx() * index }
   }
}

abstract class SingleColumnDeckTestBase : DeckTestBase() {
   protected fun <T : Any> createDefaultDeckState() = SingleColumnDeckState<T>(key = { it })
   protected val defaultDeckWidth = 300.dp
   protected val defaultCardComposable: @Composable (Int, Any?) -> Unit = { _, content ->
      Text("$content", Modifier.fillMaxWidth())
   }

   protected abstract val density: Density

   @Composable
   protected fun <T : Any> SingleColumnDeck(
      deck: Deck<T>,
      state: SingleColumnDeckState<T> = remember { createDefaultDeckState() },
      width: Dp = defaultDeckWidth,
      card: @Composable (index: Int, T) -> Unit = defaultCardComposable,
   ) {
      SingleColumnDeck(
         deck, state, sizeModifier = { Modifier.width(width).fillMaxHeight() },
         card
      )
   }

   @Composable
   protected fun <T : Any> SingleColumnDeck(
      deck: Deck<T>,
      state: SingleColumnDeckState<T> = remember { createDefaultDeckState() },
      sizeModifier: () -> Modifier,
      card: @Composable (index: Int, T) -> Unit = defaultCardComposable,
   ) {
      SingleColumnDeck(
         deck,
         state,
         card = card,
         modifier = Modifier
            .then(sizeModifier())
            .testTag(deckTestTag)
      )
   }

   protected fun expectedCardWidth(deckWidth: Dp = defaultDeckWidth): Dp = deckWidth

   protected fun expectedCardLeftPosition(
      index: Int,
      deckWidth: Dp = defaultDeckWidth
   ): Dp {
      return (deckWidth + 16.dp) * index
   }

   protected fun expectedScrollOffset(
      index: Int,
      deckWidth: Dp = defaultDeckWidth
   ): Float {
      return with (density) {
         (deckWidth + 16.dp).toPx() * index
      }
   }
}
