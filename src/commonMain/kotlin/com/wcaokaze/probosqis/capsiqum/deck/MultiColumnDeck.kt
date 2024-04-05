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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.TestOnly
import kotlin.math.ceil
import kotlin.math.roundToInt

object MultiColumnDeckDefaults {
   val CardPadding = 8.dp
   val cardPositionAnimSpec: AnimationSpec<IntOffset> = spring()
}

@Stable
class MultiColumnDeckState<T>(
   key: (T) -> Any,
   private val cardPositionAnimSpec: AnimationSpec<IntOffset>
         = MultiColumnDeckDefaults.cardPositionAnimSpec
) : DeckState<T>() {
   override var firstVisibleCardIndex by mutableIntStateOf(0)
      internal set
   override var lastVisibleCardIndex by mutableIntStateOf(0)
      internal set
   override var firstContentCardIndex by mutableIntStateOf(0)
      internal set
   override var lastContentCardIndex by mutableIntStateOf(0)
      internal set

   override val layoutLogic = MultiColumnLayoutLogic(key)

   internal fun layout(
      deck: Deck<T>,
      density: Density,
      animCoroutineScope: CoroutineScope,
      deckWidth: Int,
      columnCount: Int,
      cardPadding: Int,
      windowInsets: WindowInsets,
      layoutDirection: LayoutDirection
   ) {
      layoutLogic.layout(deck, density, deckWidth, columnCount, cardPadding,
         windowInsets, layoutDirection, scrollState, animCoroutineScope,
         cardPositionAnimSpec)
   }
}

@Stable
internal class MultiColumnLayoutLogic<T>(
   contentKeyChooser: (T) -> Any
) : DeckLayoutLogic<T>(contentKeyChooser) {
   private var deckWidth        by mutableStateOf(0)
   private var cardPadding      by mutableStateOf(0)
   private var leftWindowInset  by mutableStateOf(0)
   private var rightWindowInset by mutableStateOf(0)

   override val width: Int get() = deckWidth

   internal val layoutStateList
      @TestOnly get() = list

   internal val layoutStateMap
      @TestOnly get() = map

   override fun getScrollOffset(
      layoutState: DeckCardLayoutState<T>,
      targetPositionInDeck: PositionInDeck,
      currentScrollOffset: Float
   ): Int {
      when (targetPositionInDeck) {
         PositionInDeck.FirstVisible -> {
            return layoutState.position.x - (leftWindowInset + cardPadding * 2)
         }
         PositionInDeck.LastVisible -> {
            return layoutState.position.x - (
                  deckWidth - layoutState.width
                  - cardPadding * 2 - rightWindowInset
            )
         }
         PositionInDeck.NearestVisible -> {
            return currentScrollOffset.roundToInt().coerceIn(
               getScrollOffset(layoutState, PositionInDeck.LastVisible,  currentScrollOffset),
               getScrollOffset(layoutState, PositionInDeck.FirstVisible, currentScrollOffset)
            )
         }
      }
   }

   override fun indexOfScrollOffset(scrollOffset: Float): Int {
      return when (list.size) {
         0 -> -1
         1 -> 0
         else -> {
            val o = scrollOffset + leftWindowInset + cardPadding * 2
            for (i in 1..list.lastIndex) {
               if (list[i].position.x > o) { return i - 1 }
            }
            list.lastIndex
         }
      }
   }

   /**
    * @param animCoroutineScope
    *   Cardの移動や幅変更があったときのアニメーションを再生するための
    *   CoroutineScope
    */
   fun layout(
      deck: Deck<T>,
      density: Density,
      deckWidth: Int,
      columnCount: Int,
      cardPadding: Int,
      windowInsets: WindowInsets,
      layoutDirection: LayoutDirection,
      scrollState: DeckScrollState,
      animCoroutineScope: CoroutineScope,
      cardPositionAnimSpec: AnimationSpec<IntOffset>
   ) {
      val leftWindowInset  = windowInsets.getLeft (density, layoutDirection)
      val rightWindowInset = windowInsets.getRight(density, layoutDirection)

      val cardWidth = ceil(
         (deckWidth
               - leftWindowInset - rightWindowInset
               - cardPadding * 2) / columnCount.toDouble()
         - cardPadding * 2
      ).toInt()

      layout(deck, leftWindowInset, rightWindowInset, cardPadding, cardWidth) { list, _ ->
         var x = leftWindowInset + cardPadding

         for (layoutState in list) {
            x += cardPadding
            layoutState.update(
               position = IntOffset(x, 0),
               width = cardWidth,
               animCoroutineScope,
               cardPositionAnimSpec
            )
            x += cardWidth + cardPadding
         }

         x += cardPadding + rightWindowInset

         this.deckWidth        = deckWidth
         this.cardPadding      = cardPadding
         this.leftWindowInset  = leftWindowInset
         this.rightWindowInset = rightWindowInset

         val maxScrollOffset = (x - deckWidth).toFloat().coerceAtLeast(0.0f)
         updateMaxScrollOffset(
            scrollState, maxScrollOffset, animCoroutineScope, cardPositionAnimSpec)
      }
   }
}

@Composable
fun <T> MultiColumnDeck(
   deck: Deck<T>,
   state: MultiColumnDeckState<T>,
   columnCount: Int,
   modifier: Modifier = Modifier,
   windowInsets: WindowInsets = WindowInsets(0),
   cardPadding: Dp = MultiColumnDeckDefaults.CardPadding,
   card: @Composable (index: Int, T) -> Unit
) {
   val updatedDeck by rememberUpdatedState(deck)
   val coroutineScope = rememberCoroutineScope()

   SubcomposeLayout(
      modifier = modifier
         .scrollable(
            state.scrollState,
            Orientation.Horizontal,
            // scrollableで検知する指の動きは右に動いたとき正の値となる。
            // ScrollScope.scrollByは正のとき「右が見える」方向へスクロールする。
            // よってこの2つは符号が逆であるため、ここで反転する
            reverseDirection = true,
            flingBehavior = remember(state) {
               DeckFlingBehavior.Standard(state)
            }
         ),
      measurePolicy = remember(state, columnCount) {{ constraints ->
         require(constraints.hasFixedWidth) {
            "Deck must has a fixed width (e.g. Modifier.size) since its cards' " +
            "width are determined from Deck's width. The intrinsic width of " +
            "the cards cannot be used as wrapContentWidth for Deck."
         }
         require(constraints.hasFixedHeight) {
            "Deck must has a fixed height (e.g. Modifier.size) since its cards' " +
            "height are determined from Deck's height. The intrinsic height of " +
            "the cards cannot be used as wrapContentWidth for Deck."
         }

         val deckWidth  = constraints.maxWidth
         val deckHeight = constraints.maxHeight
         val cardPaddingPx = cardPadding.roundToPx()

         state.layout(updatedDeck, density = this, coroutineScope, deckWidth,
            columnCount, cardPaddingPx, windowInsets, layoutDirection)

         val scrollOffset = state.scrollState.scrollOffset.toInt()
         val visibleLeft = scrollOffset
         val visibleRight = visibleLeft + deckWidth
         val contentLeft  = visibleLeft  + windowInsets.getLeft (this, LayoutDirection.Ltr)
         val contentRight = visibleRight - windowInsets.getRight(this, LayoutDirection.Ltr)

         var firstVisibleIndex = -1
         var lastVisibleIndex  = -1
         var firstContentIndex = -1
         var lastContentIndex  = -1

         val placeables = state.layoutLogic.layoutStateList.mapIndexedNotNull { index, layoutState ->
            val cardPosition = layoutState.position
            val cardWidth = layoutState.width

            if (firstVisibleIndex < 0) {
               if (cardPosition.x + cardWidth > visibleLeft) {
                  firstVisibleIndex = index
               }
            }

            if (firstContentIndex < 0) {
               if (cardPosition.x + cardWidth > contentLeft) {
                  firstContentIndex = index
               }
            }

            if (cardPosition.x < visibleRight) {
               lastVisibleIndex = index
            }

            if (cardPosition.x < contentRight) {
               lastContentIndex = index
            }

            // TODO: Cardに影がつくかつかないか未定のためギリギリ範囲外の
            //       Cardもコンポーズしている。影の件が決まり次第変更する
            if (cardPosition.x + cardWidth + cardPaddingPx < visibleLeft ||
               cardPosition.x - cardPaddingPx > visibleRight)
            {
               return@mapIndexedNotNull null
            }

            val measurable = subcompose(layoutState.key) {
               Box {
                  card(index, layoutState.card.content)
               }
            } .single()

            val pageStackConstraints = Constraints.fixed(cardWidth, deckHeight)

            val placeable = measurable.measure(pageStackConstraints)
            Pair(layoutState, placeable)
         }

         state.firstVisibleCardIndex = firstVisibleIndex
         state.lastVisibleCardIndex  = lastVisibleIndex
         state.firstContentCardIndex = firstContentIndex
         state.lastContentCardIndex  = lastContentIndex

         layout(deckWidth, deckHeight) {
            for ((layoutState, placeable) in placeables) {
               // scrollOffsetが大きいほど右のCardが表示される
               // つまりscrollOffsetが大きいほどCardの位置は左となるため
               // 符号が逆となる
               placeable.placeRelative(
                  -scrollOffset + layoutState.position.x, layoutState.position.y)
            }
         }
      }}
   )
}
