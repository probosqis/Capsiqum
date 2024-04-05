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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.toOffset
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Deck内の位置
 * @see DeckState.animateScroll
 */
enum class PositionInDeck {
   /** 表示されている領域の一番左（Ltr時） */
   FirstVisible,
   /** 表示されている領域の一番右（Rtl時） */
   LastVisible,
   /**
    * 現在の位置から最も近い表示される位置。つまり、目的のCardが現在の表示領域
    * より右にある場合、表示領域の一番右、目的のCardが現在の表示領域より左に
    * ある場合、表示領域の一番左、目的のCardがすでに現在表示されている場合、
    * 現在の位置。
    */
   NearestVisible,
}

@Stable
sealed class DeckState<T> {
   internal val scrollState = DeckScrollState()

   internal abstract val layoutLogic: DeckLayoutLogic<T>

   /**
    * Deck内(WindowInsets領域を含む)の最初に表示されているCardのindex。
    *
    * WindowInsetsを含まない領域内の最初に表示されるCardを使う場合は
    * [firstContentCardIndex]
    */
   abstract val firstVisibleCardIndex: Int

   /**
    * Deck内(WindowInsets領域を含む)の最後に表示されているCardのindex。
    *
    * WindowInsetsを含まない領域内の最後に表示されるCardを使う場合は
    * [lastContentCardIndex]
    */
   abstract val lastVisibleCardIndex: Int

   /**
    * Deck内のWindowInsets領域を除いた領域の最初に表示されているCardのindex。
    *
    * WindowInsetsを含む領域内の最初に表示されるCardを使う場合は
    * [firstVisibleCardIndex]
    */
   abstract val firstContentCardIndex: Int

   val layoutInfo: DeckLayoutInfo<T> get() = layoutLogic
   val scrollOffset: Float get() = scrollState.scrollOffset

   /**
    * Deck内のWindowInsets領域を除いた領域の最後に表示されているCardのindex。
    *
    * WindowInsetsを含む領域内の最後に表示されるCardを使う場合は
    * [lastVisibleCardIndex]
    */
   abstract val lastContentCardIndex: Int

   suspend fun animateScroll(
      targetIndex: Int,
      targetPositionInDeck: PositionInDeck = PositionInDeck.NearestVisible,
      animationSpec: AnimationSpec<Float> = spring()
   ) {
      val layoutState = layoutLogic.layoutState(targetIndex)
      val targetScrollOffset = layoutLogic.getScrollOffset(
         layoutState, targetPositionInDeck, scrollState.scrollOffset)

      scrollState.animateScrollBy(
         targetScrollOffset - scrollState.scrollOffset, animationSpec)
   }

   suspend fun animateScrollByKey(
      targetCardKey: Any,
      targetPositionInDeck: PositionInDeck = PositionInDeck.NearestVisible,
      animationSpec: AnimationSpec<Float> = spring()
   ) {
      val layoutState = layoutLogic.layoutState(targetCardKey)
         ?: throw IllegalArgumentException("Card key not found: $targetCardKey")

      val targetScrollOffset = layoutLogic.getScrollOffset(
         layoutState, targetPositionInDeck, scrollState.scrollOffset)

      scrollState.animateScrollBy(
         targetScrollOffset - scrollState.scrollOffset, animationSpec)
   }

   internal fun getScrollOffset(
      cardIndex: Int,
      targetPositionInDeck: PositionInDeck
   ): Int = layoutLogic.getScrollOffset(
      layoutState = layoutLogic.layoutState(cardIndex),
      targetPositionInDeck,
      scrollState.scrollOffset
   )
}

interface DeckLayoutInfo<out T> {
   val width: Int

   val cardsInfo: List<CardInfo<T>>

   interface CardInfo<out T> {
      val card: Deck.Card<T>
      val key: Any
      val position: IntOffset
      val width: Int
   }
}

@Stable
internal class DeckCardLayoutState<T>(
   initialCard: Deck.Card<T>,
   override val key: Any
) : DeckLayoutInfo.CardInfo<T> {
   override var card: Deck.Card<T> by mutableStateOf(initialCard)

   private lateinit var positionAnimatable: Animatable<IntOffset, *>
   override val position: IntOffset get() = positionAnimatable.value

   override var width by mutableIntStateOf(0)
      private set

   internal fun update(
      position: IntOffset,
      width: Int,
      animCoroutineScope: CoroutineScope,
      positionAnimationSpec: AnimationSpec<IntOffset>
   ) {
      if (::positionAnimatable.isInitialized.not()) {
         // 初回コンポジション。アニメーション不要
         positionAnimatable = Animatable(position, IntOffset.VectorConverter)
      } else {
         // リコンポジション。位置が変化してる場合アニメーションする

         val targetPosition = positionAnimatable.targetValue
         if (targetPosition != position) {
            animCoroutineScope.launch {
               positionAnimatable.animateTo(position, positionAnimationSpec)
            }
         }
      }

      this.width = width
   }
}

internal abstract class DeckLayoutLogic<T>(
   private val contentKeyChooser: (T) -> Any
) : DeckLayoutInfo<T> {
   /** 最後に実行された[layout]の結果。[layout]の引数のDeckと同じ順 */
   protected var list: ImmutableList<DeckCardLayoutState<T>>
         by mutableStateOf(persistentListOf())

   protected var map: ImmutableMap<Any, DeckCardLayoutState<T>>
         by mutableStateOf(persistentMapOf())

   private var layoutDeck by mutableStateOf(Deck<T>())
   private var layoutKeys: Array<out Any> by mutableStateOf(emptyArray())

   private var maxScrollOffsetAnimTarget by mutableStateOf(0.0f)
   private var maxScrollOffsetAnimJob: Job
         by mutableStateOf(Job().apply { complete() })

   override val cardsInfo: List<DeckLayoutInfo.CardInfo<T>> get() = list

   fun layoutState(key: Any): DeckCardLayoutState<T>? = map[key]
   fun layoutState(index: Int): DeckCardLayoutState<T> = list[index]

   /**
    * 指定したCardが指定した位置にあるときの
    * [scrollOffset][DeckScrollState.scrollOffset]。
    * ただし実際にその位置までスクロールできるとは限らない。
    */
   internal abstract fun getScrollOffset(
      layoutState: DeckCardLayoutState<T>,
      targetPositionInDeck: PositionInDeck,
      currentScrollOffset: Float
   ): Int

   /** 指定したスクロール位置において、Deck内で左端に表示されるCardのindex */
   internal abstract fun indexOfScrollOffset(scrollOffset: Float): Int

   protected inline fun layout(
      deck: Deck<T>,
      vararg keys: Any,
      layoutLogic: (
         ImmutableList<DeckCardLayoutState<T>>,
         ImmutableMap<Any, DeckCardLayoutState<T>>
      ) -> Unit
   ) {
      val keyEqualsPreviousLayout = keys contentEquals layoutKeys

      if (deck === layoutDeck && keyEqualsPreviousLayout) { return }

      val shouldLayout = recreateLayoutState(deck)
      if (!shouldLayout && keyEqualsPreviousLayout) { return }

      layoutLogic(list, map)
      layoutDeck = deck
      layoutKeys = keys
   }

   /**
    * @return
    * [list], [map]に変更があった場合true。この場合レイアウトを再実行する
    * 必要がある
    */
   private fun recreateLayoutState(deck: Deck<T>): Boolean {
      val oldLayoutList = list
      val oldLayoutMap = map

      // 再生成が不要な可能性もあるため、必要になるまでは生成しないまま進める
      lateinit var resultList: MutableList<DeckCardLayoutState<T>>
      lateinit var resultMap: MutableMap<Any, DeckCardLayoutState<T>>
      var i = 0

      for (card in deck.sequence()) {
         val key = contentKeyChooser(card.content)

         var layoutState = if (i >= 0) {
            if (oldLayoutList.getOrNull(i)?.key == key) {
               oldLayoutList[i++]
            } else {
               resultList = oldLayoutList.subList(0, i).toMutableList()
               resultMap = mutableMapOf()
               for (s in resultList) {
                  resultMap[s.key] = s
               }
               i = -1

               oldLayoutMap[key]
            }
         } else {
            oldLayoutMap[key]
         }

         if (layoutState != null) {
            layoutState.card = card
         }

         if (i < 0) {
            if (layoutState == null) {
               layoutState = DeckCardLayoutState(card, key)
            }

            resultList += layoutState
            if (resultMap.put(key, layoutState) != null) {
               throw IllegalStateException("The key $key was used twice.")
            }
         }
      }

      return when {
         i < 0 -> {
            list = resultList.toImmutableList()
            map = resultMap.toImmutableMap()
            true
         }
         i < oldLayoutList.size -> {
            list = oldLayoutList.subList(0, i)
            resultMap = mutableMapOf()
            for (s in list) {
               resultMap[s.key] = s
            }
            map = resultMap.toImmutableMap()
            true
         }
         else -> false
      }
   }

   protected fun updateMaxScrollOffset(
      scrollState: DeckScrollState,
      maxScrollOffset: Float,
      animCoroutineScope: CoroutineScope,
      positionAnimationSpec: AnimationSpec<IntOffset>
   ) {
      if (maxScrollOffsetAnimJob.isActive
         && maxScrollOffsetAnimTarget == maxScrollOffset) { return }

      if (scrollState.scrollOffset <= maxScrollOffset) {
         // スクロール位置の調整不要なためmaxScrollOffsetのセットだけ行う
         scrollState.setMaxScrollOffset(maxScrollOffset)
      } else {
         // maxScrollOffsetまでスクロールアニメーションする
         maxScrollOffsetAnimTarget = maxScrollOffset
         maxScrollOffsetAnimJob = animCoroutineScope.launch {
            scrollState.scroll(enableOverscroll = true) {
               scrollState.setMaxScrollOffset(maxScrollOffset)

               val initialValue = IntOffset(scrollState.scrollOffset.toInt(), 0)
               var prevValue = initialValue.toOffset()
               animate(
                  IntOffset.VectorConverter,
                  initialValue,
                  targetValue = IntOffset(maxScrollOffset.toInt(), 0),
                  animationSpec = positionAnimationSpec
               ) { value, _ ->
                  val consumed = scrollBy(value.x - prevValue.x)
                  prevValue += Offset(consumed, 0.0f)
               }
            }
         }
      }
   }
}
