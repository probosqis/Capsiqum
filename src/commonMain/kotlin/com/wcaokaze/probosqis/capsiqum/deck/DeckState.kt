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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

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
sealed class DeckState<T>(initialDeck: Deck<T>) {
   private var coroutineScope: CoroutineScope? = null
   internal fun setCoroutineScope(coroutineScope: CoroutineScope) {
      this.coroutineScope = coroutineScope
   }

   private val _deck = mutableStateOf(initialDeck)
   var deck: Deck<T>
      get() = _deck.value
      set(value) {
         _deck.value = value
         layoutLogic.recreateLayoutState(value)
      }

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

   private var cardInsertionAnimOffset by mutableFloatStateOf(0.0f)

   suspend fun animateScroll(
      targetIndex: Int,
      targetPositionInDeck: PositionInDeck = PositionInDeck.NearestVisible,
      animationSpec: AnimationSpec<Float> = spring()
   ) {
      val layoutState = layoutLogic.layoutState(targetIndex)
      layoutState.awaitInitialized()
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

      layoutState.awaitInitialized()
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

   fun addColumn(index: Int, content: T): Job {
      val coroutineScope = coroutineScope
      if (coroutineScope != null) {
         return coroutineScope.launch {
            deck = Deck(
               rootRow = deck.rootRow.inserted(index, content)
            )

            val layoutState = layoutLogic.layoutState(index)

            layoutState.awaitInitialized()

            val currentScrollOffset = scrollState.scrollOffset
            val targetScrollOffset = layoutLogic.getScrollOffset(
               layoutState, PositionInDeck.NearestVisible, currentScrollOffset)

            scrollState.animateScrollBy(targetScrollOffset - currentScrollOffset)
         }
      } else {
         deck = Deck(
            rootRow = deck.rootRow.inserted(index, content)
         )
         return Job().apply { complete() }
      }
   }

   fun removeCard(index: Int) {
      deck = deck.removed(index)
   }

   fun removeCardByKey(key: Any) {
      val index = layoutLogic.cardsInfo.indexOfFirst { it.key == key }

      if (index < 0) {
         throw IllegalArgumentException("Card key not found: $key")
      }

      removeCard(index)
   }

   protected fun layout(density: Density) {
      with (density) {
         cardInsertionAnimOffset = 64.dp.toPx()
      }
   }
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
internal class DeckCardLayoutState<out T>(
   override val card: Deck.Card<T>,
   override val key: Any
) : DeckLayoutInfo.CardInfo<T> {
   /**
    * [DeckState.deck]が更新されて生成された直後のインスタンスの場合 `false`。
    * [DeckState.layout]が呼ばれて位置とサイズが決まったあと `true` になる
    */
   var isInitialized by mutableStateOf(false)
      internal set

   private lateinit var positionAnimatable: Animatable<IntOffset, *>
   override val position: IntOffset get() {
      require(isInitialized)
      return positionAnimatable.value
   }

   private lateinit var widthAnimatable: Animatable<Int, *>
   override val width: Int get() {
      require(isInitialized)
      return widthAnimatable.value
   }

   internal fun update(
      position: IntOffset,
      width: Int,
      animCoroutineScope: CoroutineScope,
      positionAnimationSpec: AnimationSpec<IntOffset>
   ) {
      if (!isInitialized) {
         // 初回コンポジション。アニメーション不要
         initialize(position, width)
      } else {
         // リコンポジション。位置か幅が変化してる場合アニメーションする

         val targetPosition = positionAnimatable.targetValue
         if (targetPosition != position) {
            animCoroutineScope.launch {
               positionAnimatable
                  .animateTo(position, positionAnimationSpec)
            }
         }

         val targetWidth = widthAnimatable.targetValue
         if (targetWidth != width) {
            animCoroutineScope.launch {
               widthAnimatable.animateTo(width)
            }
         }
      }
   }

   internal suspend fun awaitInitialized() {
      snapshotFlow { isInitialized }
         .filter { it }
         .first()
   }

   private fun initialize(position: IntOffset, width: Int) {
      assert(!isInitialized)
      positionAnimatable = Animatable(position, IntOffset.VectorConverter)
      widthAnimatable = Animatable(width, Int.VectorConverter)
      isInitialized = true
   }
}

internal abstract class DeckLayoutLogic<T>(
   initialDeck: Deck<T>,
   private val contentKeyChooser: (T) -> Any
) : DeckLayoutInfo<T> {
   /** [DeckState.deck]と同じ順 */
   protected var list: ImmutableList<DeckCardLayoutState<T>>
         by mutableStateOf(persistentListOf())

   protected var map: ImmutableMap<Any, DeckCardLayoutState<T>>
         by mutableStateOf(persistentMapOf())

   private var maxScrollOffsetAnimTarget by mutableStateOf(0.0f)
   private var maxScrollOffsetAnimJob: Job
         by mutableStateOf(Job().apply { complete() })

   init {
      recreateLayoutState(initialDeck)
   }

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

   internal fun <U> cardPositionAnimSpec() = spring<U>()

   internal fun recreateLayoutState(deck: Deck<T>) {
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

      when {
         i < 0 -> {
            list = resultList.toImmutableList()
            map = resultMap.toImmutableMap()
         }
         i < oldLayoutList.size -> {
            list = oldLayoutList.subList(0, i)
            resultMap = mutableMapOf()
            for (s in list) {
               resultMap[s.key] = s
            }
            map = resultMap.toImmutableMap()
         }
      }
   }

   protected fun updateMaxScrollOffset(
      scrollState: DeckScrollState,
      maxScrollOffset: Float,
      animCoroutineScope: CoroutineScope
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

               var prevValue = scrollState.scrollOffset
               animate(
                  initialValue = prevValue,
                  targetValue = maxScrollOffset,
                  animationSpec = cardPositionAnimSpec()
               ) { value, _ ->
                  prevValue += scrollBy(value - prevValue)
               }
            }
         }
      }
   }
}
