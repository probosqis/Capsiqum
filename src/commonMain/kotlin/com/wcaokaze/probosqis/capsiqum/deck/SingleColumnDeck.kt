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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.TestOnly

object SingleColumnDeckDefaults {
   val CardPadding = 8.dp
}

@Stable
class SingleColumnDeckState<T>(
   initialDeck: Deck<T>,
   key: (T) -> Any
) : DeckState<T>(initialDeck) {
   override var firstVisibleCardIndex by mutableIntStateOf(0)
      internal set
   override var lastVisibleCardIndex by mutableIntStateOf(0)
      internal set

   override val firstContentCardIndex get() = firstVisibleCardIndex
   override val lastContentCardIndex  get() = lastVisibleCardIndex

   override val layoutLogic = SingleColumnLayoutLogic(initialDeck, key)

   internal fun layout(
      density: Density,
      animCoroutineScope: CoroutineScope,
      deckWidth: Int,
      cardPadding: Int
   ) {
      super.layout(density)

      layoutLogic.layout(animCoroutineScope, deckWidth, cardPadding, scrollState)
   }
}

@Stable
internal class SingleColumnLayoutLogic<T>(
   initialDeck: Deck<T>,
   contentKeyChooser: (T) -> Any
) : DeckLayoutLogic<T>(initialDeck, contentKeyChooser) {
   private var deckWidth by mutableStateOf(0)

   override val width: Int get() = deckWidth

   internal val layoutStateList
      @TestOnly get() = list

   internal val layoutStateMap
      @TestOnly get() = map

   override fun getScrollOffset(
      layoutState: DeckCardLayoutState<T>,
      targetPositionInDeck: PositionInDeck,
      currentScrollOffset: Float
   ): Int = layoutState.position.x

   override fun indexOfScrollOffset(scrollOffset: Float): Int {
      return when (list.size) {
         0 -> -1
         1 -> 0
         else -> {
            for (i in 1..list.lastIndex) {
               if (list[i].position.x > scrollOffset) { return i - 1 }
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
      animCoroutineScope: CoroutineScope,
      deckWidth: Int,
      cardPadding: Int,
      scrollState: DeckScrollState
   ) {
      val cardWidth = deckWidth

      var x = -cardPadding

      for (layoutState in list) {
         x += cardPadding
         layoutState.update(
            position = IntOffset(x, 0),
            width = cardWidth,
            animCoroutineScope,
            cardPositionAnimSpec()
         )
         x += cardWidth + cardPadding
      }

      x -= cardPadding

      this.deckWidth = deckWidth

      val maxScrollOffset = (x - deckWidth).toFloat().coerceAtLeast(0.0f)
      updateMaxScrollOffset(scrollState, maxScrollOffset, animCoroutineScope)
   }
}

@Composable
fun <T> SingleColumnDeck(
   state: SingleColumnDeckState<T>,
   modifier: Modifier = Modifier,
   cardPadding: Dp = SingleColumnDeckDefaults.CardPadding,
   card: @Composable (index: Int, T) -> Unit
) {
   val coroutineScope = rememberCoroutineScope()

   LaunchedEffect(coroutineScope) {
      state.setCoroutineScope(coroutineScope)
   }

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
      measurePolicy = remember(state) {{ constraints ->
         require(constraints.hasFixedWidth) {
            "Deck must has a fixed width (e.g. Modifier.size) since its cards' " +
            "width are determined from Deck's width. The intrinsic width of the " +
            "cards cannot be used as wrapContentWidth for Deck."
         }

         val deckWidth = constraints.maxWidth
         val cardPaddingPx = cardPadding.roundToPx()

         state.layout(density = this, coroutineScope, deckWidth, cardPaddingPx)

         val scrollOffset = state.scrollState.scrollOffset.toInt()
         val visibleLeft = scrollOffset
         val visibleRight = visibleLeft + deckWidth

         var firstVisibleIndex = -1
         var lastVisibleIndex  = -1

         val placeables = state.layoutLogic.layoutStateList.mapIndexedNotNull { index, layoutState ->
            val cardPosition = layoutState.position
            val cardWidth = layoutState.width

            if (firstVisibleIndex < 0) {
               if (cardPosition.x + cardWidth > visibleLeft) {
                  firstVisibleIndex = index
               }
            }

            if (cardPosition.x < visibleRight) {
               lastVisibleIndex = index
            }

            // TODO: Cardに影がつくかつかないか未定のためギリギリ範囲外の
            //       Cardもコンポーズしている。影の件が決まり次第変更する
            if (cardPosition.x + cardWidth + cardPaddingPx < visibleLeft ||
                cardPosition.x - cardPaddingPx > visibleRight)
            {
               return@mapIndexedNotNull null
            }

            val measurable = subcompose(layoutState.key) {
               Box(Modifier.alpha(layoutState.alpha)) {
                  card(index, layoutState.card.content)
               }
            } .single()

            val cardConstraints = Constraints(
               minWidth = cardWidth,
               maxWidth = cardWidth,
               minHeight = constraints.minHeight,
               maxHeight = constraints.maxHeight
            )

            val placeable = measurable.measure(cardConstraints)
            Pair(layoutState, placeable)
         }

         state.firstVisibleCardIndex = firstVisibleIndex
         state.lastVisibleCardIndex  = lastVisibleIndex

         val deckHeight = when {
            constraints.hasFixedHeight -> constraints.minHeight
            placeables.isEmpty() -> constraints.minHeight
            else -> constraints.constrainHeight(
               placeables.maxOf { it.second.height }
            )
         }

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
