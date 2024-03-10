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

package com.wcaokaze.probosqis.capsiqum.page

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.wcaokaze.probosqis.capsiqum.PageComposableSwitcher
import com.wcaokaze.probosqis.capsiqum.PageStackBoard
import com.wcaokaze.probosqis.capsiqum.PageStackBoardState
import com.wcaokaze.probosqis.capsiqum.sequence
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import com.wcaokaze.probosqis.panoptiqon.compose.asState
import com.wcaokaze.probosqis.panoptiqon.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

fun PageStack(
   savedPageState: SavedPageState,
   clock: Clock = Clock.System
) = PageStack(
   PageStack.Id(clock.now()),
   savedPageState
)

/**
 * 画面遷移した[Page]をStack状に積んだもの。
 *
 * 必ず1つ以上の[Page]を持つ（空のPageStackという概念はない）。
 *
 * UI上はカラムと呼ばれるが列ではない（列は[PageStackBoard.Column]）ので
 * 型としての名称はPageStack
 */
@Serializable
class PageStack private constructor(
   val id: Id, // TODO: キャッシュシステムの完成後Cache自体が識別子となるため削除する
   private val savedPageStates: List<SavedPageState>
) {
   @Serializable
   @JvmInline
   value class Id(val value: Long) {
      constructor(createdTime: Instant) : this(createdTime.toEpochMilliseconds())
   }

   constructor(id: Id, savedPageState: SavedPageState) : this(
      id, listOf(savedPageState)
   )

   /** このPageStackの一番上の[SavedPageState] */
   val head: SavedPageState get() = savedPageStates.last()

   val pageCount: Int get() = savedPageStates.size

   /**
    * @return
    * このPageStackの一番上の[Page]を取り除いたPageStack。
    * このPageStackにPageがひとつしかない場合はnull
    */
   fun tailOrNull(): PageStack? {
      val tailPages = savedPageStates.dropLast(1)
      return if (tailPages.isEmpty()) {
         null
      } else {
         PageStack(id, tailPages)
      }
   }

   fun added(savedPageState: SavedPageState) = PageStack(
      id, savedPageStates + savedPageState
   )
}

@Stable
class PageStackState internal constructor(
   val pageStackId: PageStackBoard.PageStackId,
   internal val pageStackCache: WritableCache<PageStack>,
   val pageStackBoardState: PageStackBoardState
) {
   internal val pageStack: PageStack by pageStackCache.asState()

   fun startPage(page: Page) {
      pageStackCache.update {
         it.added(
            SavedPageState(
               PageId(),
               page
            )
         )
      }
   }

   fun finishPage() {
      val tail = pageStackCache.value.tailOrNull()

      if (tail == null) {
         removeFromBoard()
         return
      }

      pageStackCache.value = tail
   }

   fun addColumn(pageStack: PageStack) {
      val board = pageStackBoardState.pageStackBoard

      val index = board.sequence().indexOfFirst { it.id == pageStackId }

      val insertionIndex = if (index < 0) {
         board.pageStackCount
      } else {
         board.rootRow.asSequence()
            .map { node ->
               when (node) {
                  is PageStackBoard.PageStack           -> 1
                  is PageStackBoard.LayoutElementParent -> node.leafCount
               }
            }
            .runningReduce { acc, leafCount -> acc + leafCount }
            .indexOfFirst { it > index }
            .plus(1)
      }

      pageStackBoardState.addColumn(insertionIndex, pageStack)
   }

   fun removeFromBoard() {
      pageStackBoardState.removePageStack(pageStackId)
   }
}

@ExperimentalMaterial3Api
private fun <P : Page, S : PageState> extractPageComposable(
   combined: com.wcaokaze.probosqis.capsiqum.PageComposable<P, S>,
   pageStackState: State<PageStackState>,
   colors: State<TopAppBarColors>,
   windowInsets: State<WindowInsets>
): PageComposable<P, S> {
   return PageComposable(
      combined.pageClass,
      combined.pageStateClass,
      composable = { page, pageState ->
         PageStackAppBar(combined, page, pageState,
            pageStackState.value, colors.value, windowInsets.value)
      }
   )
}

@ExperimentalMaterial3Api
@Composable
internal fun PageStackAppBar(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   windowInsets: WindowInsets,
   colors: TopAppBarColors,
   modifier: Modifier = Modifier
) {
   val updatedPageStackState = rememberUpdatedState(pageStackState)
   val updatedWindowInsets = rememberUpdatedState(windowInsets)
   val updatedColors = rememberUpdatedState(colors)

   val switcherState = remember(pageComposableSwitcher, pageStateStore) {
      PageSwitcherState(
         pageComposableSwitcher.allPageComposables.map {
            extractPageComposable(
               it, updatedPageStackState, updatedColors, updatedWindowInsets)
         },
         pageStateStore
      )
   }

   Box(modifier) {
      val savedPageState = pageStackState.pageStack.head
      PageSwitcher(switcherState, savedPageState)
   }
}

@ExperimentalMaterial3Api
@Composable
private fun <P : Page, S : PageState> PageStackAppBar(
   combined: com.wcaokaze.probosqis.capsiqum.PageComposable<P, S>,
   page: P,
   pageState: S,
   pageStackState: PageStackState,
   colors: TopAppBarColors,
   windowInsets: WindowInsets,
   modifier: Modifier = Modifier
) {
   TopAppBar(
      title = {
         combined.headerComposable(
            page,
            pageState,
            pageStackState
         )
      },
      navigationIcon = {
         IconButton(
            onClick = { pageStackState.finishPage() }
         ) {
            val icon = if (pageStackState.pageStack.tailOrNull() != null) {
               Icons.Default.ArrowBack
            } else {
               Icons.Default.Close
            }

            Icon(icon, contentDescription = "Close")
         }
      },
      actions = {
         val headerActionsComposable = combined.headerActionsComposable
         if (headerActionsComposable != null) {
            headerActionsComposable(
               page,
               pageState,
               pageStackState
            )
         }
      },
      windowInsets = windowInsets,
      colors = colors,
      modifier = modifier
   )
}
