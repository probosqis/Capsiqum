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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.capsiqum.transition.PageTransition
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.TestOnly
import kotlin.math.roundToInt

private const val PAGE_STACK_PADDING_DP = 8

object MultiColumnPageStackBoardDefaults {
   @ExperimentalMaterial3Api
   @Composable
   fun pageStackAppBarColors(): MultiColumnPageStackAppBarColors {
      val colorScheme = MaterialTheme.colorScheme

      return MultiColumnPageStackAppBarColors(
         active = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surfaceTint.copy(alpha = 0.13f)
               .compositeOver(MaterialTheme.colorScheme.primaryContainer),
            navigationIconContentColor = colorScheme.onPrimaryContainer,
            titleContentColor = colorScheme.onPrimaryContainer,
            actionIconContentColor = colorScheme.onPrimaryContainer,
         ),
         inactive = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.surfaceColorAtElevation(4.dp),
         )
      )
   }
}

@Stable
class MultiColumnPageStackBoardState(
   pageStackBoardCache: WritableCache<PageStackBoard>,
   pageStackRepository: PageStackRepository,
   animCoroutineScope: CoroutineScope
) : PageStackBoardState(
   pageStackBoardCache,
   pageStackRepository,
   animCoroutineScope
) {
   override var firstVisiblePageStackIndex by mutableStateOf(0)
      internal set
   override var lastVisiblePageStackIndex by mutableStateOf(0)
      internal set

   override var activePageStackIndex by mutableStateOf(0)
      internal set

   override val layout = MultiColumnLayoutLogic(
      pageStackBoard,
      pageStackStateConstructor = { pageStackId, pageStackCache ->
         PageStackState(pageStackId, pageStackCache, pageStackBoardState = this)
      }
   )

   override fun pageStackState(id: PageStackBoard.PageStackId): PageStackState?
         = layout.pageStackLayout(id)?.pageStackState

   override fun pageStackState(index: Int): PageStackState
         = layout.pageStackLayout(index).pageStackState
}

@Stable
internal class MultiColumnLayoutLogic(
   pageStackBoard: PageStackBoard,
   pageStackStateConstructor:
      (PageStackBoard.PageStackId, WritableCache<PageStack>) -> PageStackState
) : PageStackBoardLayoutLogic(pageStackBoard, pageStackStateConstructor) {
   private var pageStackBoardWidth by mutableStateOf(0)
   private var pageStackPadding by mutableStateOf(0)

   internal val layoutStateList
      @TestOnly get() = list

   internal val layoutStateMap
      @TestOnly get() = map

   override fun getScrollOffset(
      pageStackLayoutState: PageStackLayoutState,
      targetPositionInBoard: PositionInBoard,
      currentScrollOffset: Float
   ): Int {
      when (targetPositionInBoard) {
         PositionInBoard.FirstVisible -> {
            return pageStackLayoutState.position.x - pageStackPadding * 2
         }
         PositionInBoard.LastVisible -> {
            return pageStackLayoutState.position.x - (
                  pageStackBoardWidth - pageStackLayoutState.width
                  - pageStackPadding * 2
            )
         }
         PositionInBoard.NearestVisible -> {
            return currentScrollOffset.roundToInt()
               .coerceIn(
                  getScrollOffset(pageStackLayoutState,
                     PositionInBoard.LastVisible, currentScrollOffset),
                  getScrollOffset(pageStackLayoutState,
                     PositionInBoard.FirstVisible, currentScrollOffset)
               )
         }
      }
   }

   override fun layout(
      animCoroutineScope: CoroutineScope,
      pageStackBoardWidth: Int,
      pageStackCount: Int,
      pageStackPadding: Int,
      scrollState: PageStackBoardScrollState
   ) {
      val pageStackWidth = (
         (pageStackBoardWidth - pageStackPadding * 2) / pageStackCount
         - pageStackPadding * 2
      )

      var x = pageStackPadding

      for (layoutState in list) {
         x += pageStackPadding
         layoutState.update(
            position = IntOffset(x, 0),
            width = pageStackWidth,
            animCoroutineScope,
            pageStackPositionAnimSpec()
         )
         x += pageStackWidth + pageStackPadding
      }

      x += pageStackPadding

      this.pageStackBoardWidth = pageStackBoardWidth
      this.pageStackPadding = pageStackPadding

      val maxScrollOffset = (x - pageStackBoardWidth).toFloat().coerceAtLeast(0f)
      updateMaxScrollOffset(scrollState, maxScrollOffset, animCoroutineScope)
   }
}

@ExperimentalMaterial3Api
@Immutable
data class MultiColumnPageStackAppBarColors(
   val active: TopAppBarColors,
   val inactive: TopAppBarColors
)

@ExperimentalMaterial3Api
@Composable
fun MultiColumnPageStackBoard(
   state: MultiColumnPageStackBoardState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   pageStackCount: Int,
   windowInsets: WindowInsets,
   modifier: Modifier = Modifier,
   pageStackAppBarColors: MultiColumnPageStackAppBarColors
         = MultiColumnPageStackBoardDefaults.pageStackAppBarColors(),
   onTopAppBarHeightChanged: (Dp) -> Unit = {}
) {
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
               PageStackBoardFlingBehavior.Standard(state)
            }
         ),
      measurePolicy = remember(state, pageStackCount) {{ constraints ->
         val pageStackBoardWidth = constraints.maxWidth
         val pageStackBoardHeight = constraints.maxHeight
         val pageStackPadding = PAGE_STACK_PADDING_DP.dp.roundToPx()

         state.layout(density = this, pageStackBoardWidth, pageStackCount,
            pageStackPadding)

         val scrollOffset = state.scrollState.scrollOffset.toInt()

         var firstVisibleIndex = -1
         var lastVisibleIndex = -1

         val placeables = state.layout.mapIndexedNotNull { index, pageStackLayout ->
            val pageStackPosition = pageStackLayout.position
            val pageStackWidth = pageStackLayout.width

            if (firstVisibleIndex < 0) {
               if (pageStackPosition.x + pageStackWidth > scrollOffset) {
                  firstVisibleIndex = index
               }
            }

            if (pageStackPosition.x < scrollOffset + pageStackBoardWidth) {
               lastVisibleIndex = index
            }

            // TODO: PageStackに影がつくかつかないか未定のためギリギリ範囲外の
            //       PageStackもコンポーズしている。影の件が決まり次第変更する
            if (pageStackPosition.x + pageStackWidth + pageStackPadding < scrollOffset ||
                pageStackPosition.x - pageStackPadding > scrollOffset + pageStackBoardWidth)
            {
               return@mapIndexedNotNull null
            }

            val measurable = subcompose(pageStackLayout.pageStackId) {
               PageStack(
                  pageStackLayout.pageStackState,
                  isActive = index == state.activePageStackIndex,
                  windowInsets.only(WindowInsetsSides.Bottom),
                  pageStackAppBarColors,
                  pageComposableSwitcher,
                  pageStateStore,
                  onTopAppBarHeightChanged,
                  modifier = Modifier
                     .alpha(pageStackLayout.alpha)
                     .detectTouch(
                        onTouch = { state.activePageStackIndex = index }
                     )
               )
            } .single()

            val pageStackConstraints = Constraints.fixed(
               pageStackWidth, pageStackBoardHeight)

            val placeable = measurable.measure(pageStackConstraints)
            Pair(pageStackLayout, placeable)
         }

         state.firstVisiblePageStackIndex = firstVisibleIndex
         state.lastVisiblePageStackIndex = lastVisibleIndex
         state.activePageStackIndex = state.activePageStackIndex
            .coerceIn(firstVisibleIndex, lastVisibleIndex)

         layout(pageStackBoardWidth, pageStackBoardHeight) {
            for ((layout, placeable) in placeables) {
               // scrollOffsetが大きいほど右のPageStackが表示される
               // つまりscrollOffsetが大きいほどPageStackの位置は左となるため
               // 符号が逆となる
               placeable.placeRelative(
                  -scrollOffset + layout.position.x, layout.position.y)
            }
         }
      }}
   )
}

@ExperimentalMaterial3Api
@Composable
private fun PageStack(
   state: PageStackState,
   isActive: Boolean,
   windowInsets: WindowInsets,
   appBarColors: MultiColumnPageStackAppBarColors,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   onTopAppBarHeightChanged: (Dp) -> Unit,
   modifier: Modifier = Modifier
) {
   Surface(
      shape = MaterialTheme.shapes.large,
      tonalElevation = if (isActive) { 3.dp } else { 1.dp },
      shadowElevation = if (isActive) { 4.dp } else { 2.dp },
      modifier = modifier
   ) {
      Column {
         val density by rememberUpdatedState(LocalDensity.current)

         MultiColumnPageStackAppBar(
            state,
            pageComposableSwitcher,
            pageStateStore,
            isActive,
            appBarColors,
            modifier = Modifier
               .onSizeChanged {
                  val heightPx = it.height
                  val heightDp = with (density) { heightPx.toDp() }
                  onTopAppBarHeightChanged(heightDp)
               }
         )

         PageTransition(
            state,
            pageComposableSwitcher,
            pageStateStore
         )
      }
   }
}

@ExperimentalMaterial3Api
@Composable
private fun MultiColumnPageStackAppBar(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   isActive: Boolean,
   colors: MultiColumnPageStackAppBarColors,
   modifier: Modifier = Modifier
) {
   @OptIn(ExperimentalMaterial3Api::class)
   PageStackAppBar(
      pageStackState,
      pageComposableSwitcher,
      pageStateStore,
      windowInsets = WindowInsets(0, 0, 0, 0),
      colors = if (isActive) { colors.active } else { colors.inactive },
      modifier = modifier
   )
}

@Stable
private fun Modifier.detectTouch(onTouch: () -> Unit): Modifier {
   return pointerInput(onTouch) {
      awaitEachGesture {
         val event = awaitPointerEvent(PointerEventPass.Initial)

         val isDownEvent = event.changes.any {
            if (it.type == PointerType.Mouse) {
               event.buttons.isPrimaryPressed && it.changedToDownIgnoreConsumed()
            } else {
               it.changedToDownIgnoreConsumed()
            }
         }

         if (isDownEvent) {
            onTouch()
         }
      }
   }
}
