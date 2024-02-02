/*
 * Copyright 2023-2024 wcaokaze
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

package com.wcaokaze.probosqis.capsiqum

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import com.wcaokaze.probosqis.panoptiqon.compose.asMutableState
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
import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration.Companion.milliseconds

internal class PageStackCacheSerializer(
   private val pageStackRepository: PageStackRepository
) : KSerializer<WritableCache<PageStack>> {
   override val descriptor: SerialDescriptor
      get() = PrimitiveSerialDescriptor("Cache", PrimitiveKind.LONG)

   override fun serialize(encoder: Encoder, value: WritableCache<PageStack>) {
      encoder.encodeLong(value.value.id.value)
   }

   override fun deserialize(decoder: Decoder): WritableCache<PageStack> {
      val id = PageStack.Id(decoder.decodeLong())
      return pageStackRepository.loadPageStack(id)
   }
}

@Serializable
class PageStackBoard(val rootRow: Row) {
   @Serializable
   @JvmInline
   value class PageStackId(val value: Long)

   @Serializable
   sealed class LayoutElement

   @Serializable
   class PageStack(
      val id: PageStackId,
      @Contextual
      val pageStackCache: WritableCache<com.wcaokaze.probosqis.capsiqum.PageStack>
   ) : LayoutElement()

   @Serializable
   sealed class LayoutElementParent : LayoutElement(), Iterable<LayoutElement> {
      abstract val children: List<LayoutElement>

      /** 葉ノードの数。すなわちこのサブツリーの子孫にある[PageStack]の数。 */
      internal abstract val leafCount: Int

      val childCount: Int get() = children.size

      protected fun countLeafs(children: List<LayoutElement>): Int {
         return children.sumOf {
            when (it) {
               is PageStack -> 1
               is LayoutElementParent -> it.leafCount
            }
         }
      }

      operator fun get(index: Int): LayoutElement = children[index]

      override operator fun iterator(): Iterator<LayoutElement>
            = children.iterator()
   }

   @Serializable
   class Column(
      override val children: List<LayoutElement>
   ) : LayoutElementParent() {
      override val leafCount: Int = countLeafs(children)

      fun inserted(index: Int, element: LayoutElement) = Column(
         buildList(capacity = children.size + 1) {
            addAll(children.subList(0, index))
            add(element)
            addAll(children.subList(index, children.size))
         }
      )

      fun removed(index: Int) = Column(
         buildList(capacity = children.size) {
            addAll(children.subList(0, index))
            addAll(children.subList(index + 1, children.size))
         }
      )

      fun replaced(index: Int, element: LayoutElement) = Column(
         buildList(capacity = children.size) {
            addAll(children)
            set(index, element)
         }
      )
   }

   @Serializable
   class Row(
      override val children: List<LayoutElement>
   ) : LayoutElementParent() {
      override val leafCount: Int = countLeafs(children)

      fun inserted(index: Int, element: LayoutElement) = Row(
         buildList(capacity = children.size + 1) {
            addAll(children.subList(0, index))
            add(element)
            addAll(children.subList(index, children.size))
         }
      )

      fun removed(index: Int) = Row(
         buildList(capacity = children.size) {
            addAll(children.subList(0, index))
            addAll(children.subList(index + 1, children.size))
         }
      )

      fun replaced(index: Int, element: LayoutElement) = Row(
         buildList(capacity = children.size) {
            addAll(children)
            set(index, element)
         }
      )
   }

   val pageStackCount = rootRow.leafCount
}

internal fun <P : PageStackBoard.LayoutElementParent> P.removed(index: Int): P {
   fun PageStackBoard.LayoutElementParent
         .impl(index: Int): PageStackBoard.LayoutElementParent
   {
      return when (this) {
         is PageStackBoard.Column -> removed(index)
         is PageStackBoard.Row    -> removed(index)
      }
   }
   @Suppress("UNCHECKED_CAST")
   return impl(index) as P
}

internal fun <P : PageStackBoard.LayoutElementParent>
      P.replaced(index: Int, element: PageStackBoard.LayoutElement): P
{
   fun PageStackBoard.LayoutElementParent
         .impl(index: Int): PageStackBoard.LayoutElementParent
   {
      return when (this) {
         is PageStackBoard.Column -> replaced(index, element)
         is PageStackBoard.Row    -> replaced(index, element)
      }
   }
   @Suppress("UNCHECKED_CAST")
   return impl(index) as P
}

operator fun PageStackBoard.get(index: Int): PageStackBoard.PageStack {
   fun PageStackBoard.LayoutElementParent
         .findSubtree(indexInSubtree: Int): PageStackBoard.PageStack
   {
      assert(indexInSubtree >= 0)

      var leafIndex = 0
      for (node in this) {
         when (node) {
            is PageStackBoard.PageStack -> {
               if (leafIndex == indexInSubtree) { return node }
               leafIndex++
            }
            is PageStackBoard.LayoutElementParent -> {
               if (leafIndex + node.leafCount > indexInSubtree) {
                  return node.findSubtree(indexInSubtree - leafIndex)
               }
               leafIndex += node.leafCount
            }
         }
      }

      throw IndexOutOfBoundsException(
         "pageStack count: $leafIndex, specified index: $index")
   }

   if (index < 0) { throw IndexOutOfBoundsException("specified index: $index") }

   return rootRow.findSubtree(index)
}

internal fun PageStackBoard.removed(
   id: PageStackBoard.PageStackId
): PageStackBoard {
   val index = sequence().indexOfFirst { it.id == id }

   if (index < 0) { return this }
   return removed(index)
}

internal fun PageStackBoard.removed(index: Int): PageStackBoard {
   fun <P : PageStackBoard.LayoutElementParent>
         P.removedSubtree(indexInSubtree: Int): P
   {
      assert(indexInSubtree >= 0)

      var leafIndex = 0
      for ((childIndex, node) in this.withIndex()) {
         when (node) {
            is PageStackBoard.PageStack -> {
               if (leafIndex == indexInSubtree) {
                  return this.removed(childIndex)
               }
               leafIndex++
            }
            is PageStackBoard.LayoutElementParent -> {
               if (leafIndex + node.leafCount > indexInSubtree) {
                  val removedSubtree
                        = node.removedSubtree(indexInSubtree - leafIndex)
                  return if (removedSubtree.childCount > 0) {
                     this.replaced(childIndex, removedSubtree)
                  } else {
                     this.removed(childIndex)
                  }
               }
               leafIndex += node.leafCount
            }
         }
      }

      throw IndexOutOfBoundsException(
         "pageStack count: $leafIndex, specified index: $index")
   }

   if (index < 0) { throw IndexOutOfBoundsException("specified index: $index") }

   return PageStackBoard(
      rootRow.removedSubtree(index)
   )
}

fun PageStackBoard.sequence(): Sequence<PageStackBoard.PageStack> {
   suspend fun SequenceScope<PageStackBoard.PageStack>
         .yieldChildren(parent: PageStackBoard.LayoutElementParent)
   {
      for (node in parent) {
         when (node) {
            is PageStackBoard.PageStack           -> yield(node)
            is PageStackBoard.LayoutElementParent -> yieldChildren(node)
         }
      }
   }

   return sequence {
      yieldChildren(rootRow)
   }
}

/**
 * PageStackBoard内の位置
 * @see PageStackBoardState.animateScroll
 */
enum class PositionInBoard {
   /** 表示されている領域の一番左（Ltr時） */
   FirstVisible,
   /** 表示されている領域の一番右（Rtl時） */
   LastVisible,
   /**
    * 現在の位置から最も近い表示される位置。つまり、目的のPageStackが現在の表示領域
    * より右にある場合、表示領域の一番右、目的のPageStackが現在の表示領域より左に
    * ある場合、表示領域の一番左、目的のPageStackがすでに現在表示されている場合、
    * 現在の位置。
    */
   NearestVisible,
}

@Stable
sealed class PageStackBoardState(
   pageStackBoardCache: WritableCache<PageStackBoard>,
   private val pageStackRepository: PageStackRepository,
   protected val animCoroutineScope: CoroutineScope
) {
   private val pageStackBoardState = pageStackBoardCache.asMutableState()

   internal var pageStackBoard: PageStackBoard
      get() = pageStackBoardState.value
      set(value) {
         pageStackBoardState.value = value
         layout.recreateLayoutState(value)
      }

   internal val scrollState = PageStackBoardScrollState()

   internal abstract val layout: PageStackBoardLayoutLogic

   abstract val firstVisiblePageStackIndex: Int
   abstract val lastVisiblePageStackIndex: Int

   abstract val activePageStackIndex: Int

   private var pageStackInsertionAnimOffset by mutableStateOf(0.0f)

   abstract fun pageStackState(
      id: PageStackBoard.PageStackId
   ): PageStackState?

   abstract fun pageStackState(index: Int): PageStackState

   suspend fun animateScroll(
      pageStackIndex: Int,
      targetPositionInBoard: PositionInBoard = PositionInBoard.NearestVisible,
      animationSpec: AnimationSpec<Float> = spring()
   ) {
      val pageStackLayout = layout.pageStackLayout(pageStackIndex)
      pageStackLayout.awaitInitialized()
      val targetScrollOffset = layout.getScrollOffset(
         pageStackLayout, targetPositionInBoard, scrollState.scrollOffset)

      scrollState.animateScrollBy(
         targetScrollOffset - scrollState.scrollOffset, animationSpec)
   }

   suspend fun animateScroll(
      pageStackId: PageStackBoard.PageStackId,
      targetPositionInBoard: PositionInBoard = PositionInBoard.NearestVisible,
      animationSpec: AnimationSpec<Float> = spring()
   ) {
      val pageStackLayout = layout.pageStackLayout(pageStackId)
         ?: throw IllegalArgumentException("PageStack not found: $pageStackId")
      pageStackLayout.awaitInitialized()
      val targetScrollOffset = layout.getScrollOffset(
         pageStackLayout, targetPositionInBoard, scrollState.scrollOffset)

      scrollState.animateScrollBy(
         targetScrollOffset - scrollState.scrollOffset, animationSpec)
   }

   internal fun getScrollOffset(
      pageStackIndex: Int,
      targetPositionInBoard: PositionInBoard
   ): Int = layout.getScrollOffset(
      pageStackLayoutState = layout.pageStackLayout(pageStackIndex),
      targetPositionInBoard,
      scrollState.scrollOffset
   )

   internal fun getScrollOffset(
      pageStackId: PageStackBoard.PageStackId,
      targetPositionInBoard: PositionInBoard
   ): Int = layout.getScrollOffset(
      pageStackLayoutState = layout.pageStackLayout(pageStackId)
         ?: throw IllegalArgumentException("PageStack not found: $pageStackId"),
      targetPositionInBoard,
      scrollState.scrollOffset
   )

   fun addColumn(index: Int, pageStack: PageStack): Job = animCoroutineScope.launch {
      val pageStackCache = pageStackRepository.savePageStack(pageStack)
      val pageStackId = PageStackBoard.PageStackId(pageStack.id.value)

      pageStackBoard = PageStackBoard(
         pageStackBoard.rootRow.inserted(
            index,
            PageStackBoard.PageStack(pageStackId, pageStackCache)
         )
      )

      launch {
         animateScroll(pageStackId, PositionInBoard.NearestVisible,
            layout.pageStackPositionAnimSpec())
      }
      launch {
         layout.pageStackLayout(pageStackId)
            ?.animateInsertion(pageStackInsertionAnimOffset)
      }
   }

   fun removePageStack(id: PageStackBoard.PageStackId): Job {
      return animCoroutineScope.launch {
         layout.pageStackLayout(id)
            ?.animateRemoving(pageStackInsertionAnimOffset)

         pageStackBoard = pageStackBoard.removed(id)
      }
   }

   protected fun layout(density: Density) {
      with (density) {
         pageStackInsertionAnimOffset = 64.dp.toPx()
      }
   }
}

internal abstract class PageStackBoardLayoutLogic(
   pageStackBoard: PageStackBoard,
   private val pageStackStateConstructor:
      (PageStackBoard.PageStackId, WritableCache<PageStack>) -> PageStackState
) : Iterable<PageStackBoardLayoutLogic.PageStackLayoutState> {
   @Stable
   internal class PageStackLayoutState(val pageStackState: PageStackState) {
      val pageStackId = pageStackState.pageStackId

      /**
       * [PageStackBoardState.pageStackBoard]をセットして生成された直後の
       * インスタンスの場合 `false`。[layout]が呼ばれて位置とサイズが決まったあと
       * `true` になる
       */
      var isInitialized by mutableStateOf(false)
         internal set

      private val yOffsetAnimatable = Animatable(0.0f)
      internal suspend fun animateInsertion(yOffset: Float) {
         coroutineScope {
            alphaAnimatable.snapTo(0.0f)
            yOffsetAnimatable.snapTo(yOffset)

            delay(200.milliseconds)

            launch {
               alphaAnimatable.animateTo(1.0f, tween(durationMillis = 200))
            }
            launch {
               yOffsetAnimatable.animateTo(0.0f, tween(durationMillis = 200))
            }
         }
      }
      internal suspend fun animateRemoving(yOffset: Float) {
         coroutineScope {
            launch {
               alphaAnimatable.animateTo(
                  0.0f, tween(durationMillis = 200, easing = LinearEasing))
            }
            launch {
               yOffsetAnimatable.animateTo(
                  yOffset, tween(durationMillis = 200, easing = LinearEasing))
            }
         }
      }

      private lateinit var positionAnimatable: Animatable<IntOffset, *>
      val position: IntOffset get() {
         require(isInitialized)
         val (x, y) = positionAnimatable.value
         return IntOffset(x, y + yOffsetAnimatable.value.toInt())
      }

      private lateinit var widthAnimatable: Animatable<Int, *>
      val width: Int get() {
         require(isInitialized)
         return widthAnimatable.value
      }

      private val alphaAnimatable = Animatable(1.0f)
      val alpha: Float get() = alphaAnimatable.value

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

   /** [MultiColumnPageStackBoardState.pageStackBoard]と同じ順 */
   protected var list: ImmutableList<PageStackLayoutState>
         by mutableStateOf(persistentListOf())

   protected var map: ImmutableMap<PageStackBoard.PageStackId, PageStackLayoutState>
         by mutableStateOf(persistentMapOf())

   private var maxScrollOffsetAnimJob: Job
         by mutableStateOf(Job().apply { complete() })
   private var maxScrollOffsetAnimTarget by mutableStateOf(0.0f)

   init {
      recreateLayoutState(pageStackBoard)
   }

   override operator fun iterator(): Iterator<PageStackLayoutState>
         = list.iterator()

   fun pageStackLayout(id: PageStackBoard.PageStackId): PageStackLayoutState?
         = map[id]

   fun pageStackLayout(index: Int): PageStackLayoutState
         = list[index]

   /**
    * 指定したPageStackが指定した位置にあるときの
    * [scrollOffset][PageStackBoardScrollState.scrollOffset]。
    * ただし実際にその位置までスクロールできるとは限らない。
    */
   internal abstract fun getScrollOffset(
      pageStackLayoutState: PageStackLayoutState,
      targetPositionInBoard: PositionInBoard,
      currentScrollOffset: Float
   ): Int

   internal fun <T> pageStackPositionAnimSpec() = spring<T>()

   internal fun recreateLayoutState(pageStackBoard: PageStackBoard) {
      val prevLayoutList = list
      val prevLayoutMap = map

      // 再生成が不要な可能性もあるため、必要になるまでは生成しないまま進める
      lateinit var resultList: MutableList<PageStackLayoutState>
      lateinit var resultMap: MutableMap<PageStackBoard.PageStackId, PageStackLayoutState>
      var i = 0

      fun prepareResults() {
         resultList = prevLayoutList.subList(0, i).toMutableList()
         resultMap = mutableMapOf()
         for (l in resultList) {
            resultMap[l.pageStackId] = l
         }
         i = -1
      }

      for (pageStackElement in pageStackBoard.sequence()) {
         val pageStackId = pageStackElement.id

         var layoutState = if (i >= 0) {
            if (prevLayoutList.getOrNull(i)?.pageStackId == pageStackId) {
               prevLayoutList[i++]
            } else {
               prepareResults()
               prevLayoutMap[pageStackId]
            }
         } else {
            prevLayoutMap[pageStackId]
         }

         if (i < 0) {
            if (layoutState == null) {
               val pageStackState = pageStackStateConstructor(
                  pageStackId,
                  pageStackElement.pageStackCache
               )
               layoutState = PageStackLayoutState(pageStackState)
            }

            resultList += layoutState
            resultMap[pageStackId] = layoutState
         }
      }

      when {
         i < 0 -> {
            list = resultList.toImmutableList()
            map = resultMap.toImmutableMap()
         }
         i < prevLayoutList.size -> {
            prepareResults()
            list = resultList.toImmutableList()
            map = resultMap.toImmutableMap()
         }
      }
   }

   protected fun updateMaxScrollOffset(
      scrollState: PageStackBoardScrollState,
      maxScrollOffset: Float,
      animCoroutineScope: CoroutineScope
   ) {
      if (!maxScrollOffsetAnimJob.isActive
         || maxScrollOffsetAnimTarget != maxScrollOffset)
      {
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
                     animationSpec = pageStackPositionAnimSpec()
                  ) { value, _ ->
                     prevValue += scrollBy(value - prevValue)
                  }
               }
            }
         }
      }
   }
}
