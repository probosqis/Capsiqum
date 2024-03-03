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
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.capsiqum.page.PageStack
import com.wcaokaze.probosqis.capsiqum.page.PageStackAppBar
import com.wcaokaze.probosqis.capsiqum.page.PageStackRepository
import com.wcaokaze.probosqis.capsiqum.page.PageStackState
import com.wcaokaze.probosqis.capsiqum.page.PageStateStore
import com.wcaokaze.probosqis.capsiqum.transition.PageTransition
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.TestOnly

private const val PAGE_STACK_PADDING_DP = 8

object SingleColumnPageStackBoardDefaults {
   @ExperimentalMaterial3Api
   @Composable
   fun appBarColors(): TopAppBarColors {
      val colorScheme = MaterialTheme.colorScheme

      return TopAppBarDefaults.topAppBarColors(
         containerColor = colorScheme.primaryContainer,
         navigationIconContentColor = colorScheme.onPrimaryContainer,
         titleContentColor = colorScheme.onPrimaryContainer,
         actionIconContentColor = colorScheme.onPrimaryContainer,
      )
   }
}

@Stable
class SingleColumnPageStackBoardState(
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

   override val firstContentPageStackIndex: Int
      get() = firstVisiblePageStackIndex
   override val lastContentPageStackIndex: Int
      get() = lastVisiblePageStackIndex

   override val activePageStackIndex: Int get() {
      val firstVisibleIndex = firstVisiblePageStackIndex
      val lastVisibleIndex = lastVisiblePageStackIndex
      if (firstVisibleIndex == lastVisibleIndex) { return firstVisibleIndex }

      val firstVisibleLayout = layout.pageStackLayout(firstVisibleIndex)
      val lastVisibleLayout  = layout.pageStackLayout(lastVisibleIndex)
      val boardWidth = firstVisibleLayout.width

      return if (
         scrollState.scrollOffset + boardWidth / 2.0f
            < (firstVisibleLayout.position.x
               + lastVisibleLayout.position.x + lastVisibleLayout.width) / 2.0f
      ) {
         firstVisiblePageStackIndex
      } else {
         firstVisiblePageStackIndex + 1
      }
   }

   override val layout = SingleColumnLayoutLogic(
      pageStackBoard,
      pageStackStateConstructor = { pageStackId, pageStackCache ->
         PageStackState(pageStackId, pageStackCache, pageStackBoardState = this)
      }
   )

   override fun pageStackState(id: PageStackBoard.PageStackId): PageStackState?
         = layout.pageStackLayout(id)?.pageStackState

   override fun pageStackState(index: Int): PageStackState
         = layout.pageStackLayout(index).pageStackState

   internal fun layout(
      density: Density,
      pageStackBoardWidth: Int,
      pageStackPadding: Int
   ) {
      super.layout(density)

      layout.layout(animCoroutineScope, pageStackBoardWidth,
         pageStackPadding, scrollState)
   }
}

@Stable
internal class SingleColumnLayoutLogic(
   pageStackBoard: PageStackBoard,
   pageStackStateConstructor:
      (PageStackBoard.PageStackId, WritableCache<PageStack>) -> PageStackState
) : PageStackBoardLayoutLogic(pageStackBoard, pageStackStateConstructor) {
   private var pageStackBoardWidth by mutableStateOf(0)

   internal val layoutStateList
      @TestOnly get() = list

   internal val layoutStateMap
      @TestOnly get() = map

   override fun getScrollOffset(
      pageStackLayoutState: PageStackLayoutState,
      targetPositionInBoard: PositionInBoard,
      currentScrollOffset: Float
   ): Int = pageStackLayoutState.position.x

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
    *   PageStackの移動や幅変更があったときのアニメーションを再生するための
    *   CoroutineScope
    */
   fun layout(
      animCoroutineScope: CoroutineScope,
      pageStackBoardWidth: Int,
      pageStackPadding: Int,
      scrollState: PageStackBoardScrollState
   ) {
      val pageStackWidth = pageStackBoardWidth

      var x = -pageStackPadding

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

      x -= pageStackPadding

      this.pageStackBoardWidth = pageStackBoardWidth

      val maxScrollOffset = (x - pageStackBoardWidth).toFloat().coerceAtLeast(0f)
      updateMaxScrollOffset(scrollState, maxScrollOffset, animCoroutineScope)
   }
}

@ExperimentalMaterial3Api
@Composable
fun SingleColumnPageStackBoardAppBar(
   state: SingleColumnPageStackBoardState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   modifier: Modifier = Modifier,
   windowInsets: WindowInsets = WindowInsets
      .safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
   colors: TopAppBarColors = SingleColumnPageStackBoardDefaults.appBarColors()
) {
   SubcomposeLayout(
      modifier = modifier
         .scrollable(
            state.scrollState,
            Orientation.Horizontal,
            reverseDirection = true,
            flingBehavior = remember(state) {
               PageStackBoardFlingBehavior.Standard(state)
            }
         ),
      measurePolicy = remember(state) {
         // PageStackBoardState.removePageStack等によって一瞬PageStackが画面内に
         // ひとつもないことがある。その際に前回のコンポジションの値を使うため
         // ここで保持している
         var boardAppBarHeight = 0

         { boardAppBarConstraints ->
            val boardAppBarWidth = boardAppBarConstraints.maxWidth

            val scrollOffset = state.scrollState.scrollOffset.toInt()

            val placeables = state.layout.mapNotNull { pageStackLayout ->
               if (!pageStackLayout.isInitialized) { return@mapNotNull null }

               val pageStackPosition = pageStackLayout.position
               val pageStackWidth = pageStackLayout.width

               if (pageStackPosition.x + pageStackWidth < scrollOffset ||
                  pageStackPosition.x > scrollOffset + boardAppBarWidth)
               {
                  return@mapNotNull null
               }

               val measurable = subcompose(pageStackLayout.pageStackId) {
                  SingleColumnPageStackAppBar(
                     pageStackLayout.pageStackState,
                     pageComposableSwitcher,
                     pageStateStore,
                     windowInsets,
                     colors,
                     modifier = Modifier.alpha(pageStackLayout.alpha)
                  )
               } .single()

               val appBarConstraints = Constraints(
                  minWidth = pageStackWidth,
                  maxWidth = pageStackWidth,
                  minHeight = boardAppBarConstraints.minHeight,
                  maxHeight = boardAppBarConstraints.maxHeight
               )

               val placeable = measurable.measure(appBarConstraints)
               Pair(pageStackLayout, placeable)
            }

            if (placeables.isNotEmpty()) {
               boardAppBarHeight = placeables.maxOf { (_, p) -> p.height }
            }

            layout(boardAppBarWidth, boardAppBarHeight) {
               for ((layout, placeable) in placeables) {
                  placeable.placeRelative(-scrollOffset + layout.position.x, 0)
               }
            }
         }
      }
   )
}

@Composable
fun SingleColumnPageStackBoard(
   state: SingleColumnPageStackBoardState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   windowInsets: WindowInsets,
   modifier: Modifier = Modifier,
) {
   SubcomposeLayout(
      modifier = modifier
         .scrollable(rememberScrollState(), Orientation.Vertical)
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
      measurePolicy = remember(state) {{ constraints ->
         val pageStackBoardWidth = constraints.maxWidth
         val pageStackBoardHeight = constraints.maxHeight
         val pageStackPadding = PAGE_STACK_PADDING_DP.dp.roundToPx()

         state.layout(density = this, pageStackBoardWidth, pageStackPadding)

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
               PageStackContent(
                  pageStackLayout.pageStackState,
                  pageComposableSwitcher,
                  pageStateStore,
                  windowInsets = windowInsets,
                  modifier = Modifier.alpha(pageStackLayout.alpha)
               )
            } .single()

            val pageStackConstraints = Constraints.fixed(
               pageStackWidth, pageStackBoardHeight)

            val placeable = measurable.measure(pageStackConstraints)
            Pair(pageStackLayout, placeable)
         }

         state.firstVisiblePageStackIndex = firstVisibleIndex
         state.lastVisiblePageStackIndex = lastVisibleIndex

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
private fun SingleColumnPageStackAppBar(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   windowInsets: WindowInsets,
   colors: TopAppBarColors,
   modifier: Modifier = Modifier
) {
   PageStackAppBar(
      pageStackState,
      pageComposableSwitcher,
      pageStateStore,
      windowInsets,
      colors,
      modifier = modifier
   )
}

@Composable
private fun PageStackContent(
   state: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   windowInsets: WindowInsets,
   modifier: Modifier = Modifier
) {
   Surface(
      tonalElevation = 3.dp,
      shadowElevation = 4.dp,
      modifier = modifier
   ) {
      PageTransition(state, pageComposableSwitcher, pageStateStore, windowInsets)
   }
}
