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

package com.wcaokaze.probosqis.capsiqum.page.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.capsiqum.MultiColumnPageStackBoardState
import com.wcaokaze.probosqis.capsiqum.page.Page
import com.wcaokaze.probosqis.capsiqum.PageComposable
import com.wcaokaze.probosqis.capsiqum.PageComposableSwitcher
import com.wcaokaze.probosqis.capsiqum.page.PageContent
import com.wcaokaze.probosqis.capsiqum.page.PageFooter
import com.wcaokaze.probosqis.capsiqum.page.PageStack
import com.wcaokaze.probosqis.capsiqum.page.PageStackAppBar
import com.wcaokaze.probosqis.capsiqum.PageStackBoard
import com.wcaokaze.probosqis.capsiqum.page.PageStackRepository
import com.wcaokaze.probosqis.capsiqum.page.PageStackState
import com.wcaokaze.probosqis.capsiqum.page.PageStateStore
import com.wcaokaze.probosqis.panoptiqon.WritableCache

@Composable
fun <P : Page> PagePreview(
   page: P,
   pageComposable: PageComposable<P, *>,
   windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
) {
   val savedPageState = remember {
      PageStack.SavedPageState(PageStack.PageId(0L), page)
   }

   val pageStackCache = remember {
      val pageStack = PageStack(PageStack.Id(0L), savedPageState)
      WritableCache(pageStack)
   }

   val pageStackLayoutElement = remember {
      PageStackBoard.PageStack(
         PageStackBoard.PageStackId(pageStackCache.value.id.value),
         pageStackCache
      )
   }

   val coroutineScope = rememberCoroutineScope()

   val pageStackBoardState = remember {
      val pageStackBoard = PageStackBoard(
         PageStackBoard.Row(
            listOf(pageStackLayoutElement)
         )
      )

      MultiColumnPageStackBoardState(
         WritableCache(pageStackBoard),
         PreviewPageStackRepository(),
         coroutineScope
      )
   }

   val pageStackState = remember {
      PageStackState(
         pageStackId = pageStackLayoutElement.id,
         pageStackCache,
         pageStackBoardState
      )
   }

   val pageComposableSwitcher = remember {
      PageComposableSwitcher(
         listOf(pageComposable)
      )
   }

   val pageStateStore = remember {
      PageStateStore(
         listOf(pageComposable.pageStateFactory),
         coroutineScope
      )
   }

   val pageState = remember { pageStateStore.get(savedPageState) }

   Surface(
      shape = MaterialTheme.shapes.large,
      tonalElevation = 3.dp,
      shadowElevation = 4.dp
   ) {
      val footerComposable = pageComposable.footerComposable

      Box {
         Column {
            @OptIn(ExperimentalMaterial3Api::class)
            PageStackAppBar(
               pageStackState,
               pageComposableSwitcher,
               pageStateStore,
               windowInsets.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
               colors = TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer
               )
            )

            val contentWindowInsetsSides = if (footerComposable != null) {
               WindowInsetsSides.Horizontal
            } else {
               WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
            }

            PageContent(
               pageComposable.contentComposable,
               page,
               pageState,
               pageStackState,
               windowInsets.only(contentWindowInsetsSides)
            )
         }

         if (footerComposable != null) {
            Box(Modifier.align(Alignment.BottomCenter)) {
               PageFooter(
                  pageComposable.footerComposable,
                  page,
                  pageState,
                  pageStackState,
                  windowInsets
                     .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
               )
            }
         }
      }
   }
}

internal class PreviewPageStackRepository : PageStackRepository {
   override fun deleteAllPageStacks() {}
   override fun loadPageStack(id: PageStack.Id): WritableCache<PageStack>
         = throw NotImplementedError()
   override fun savePageStack(pageStack: PageStack): WritableCache<PageStack>
         = throw NotImplementedError()
}
