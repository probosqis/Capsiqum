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

package com.wcaokaze.probosqis.capsiqum.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.capsiqum.MultiColumnPageStackBoardState
import com.wcaokaze.probosqis.capsiqum.Page
import com.wcaokaze.probosqis.capsiqum.PageComposable
import com.wcaokaze.probosqis.capsiqum.PageComposableSwitcher
import com.wcaokaze.probosqis.capsiqum.PageContent
import com.wcaokaze.probosqis.capsiqum.PageStack
import com.wcaokaze.probosqis.capsiqum.PageStackAppBar
import com.wcaokaze.probosqis.capsiqum.PageStackBoard
import com.wcaokaze.probosqis.capsiqum.PageStackRepository
import com.wcaokaze.probosqis.capsiqum.PageStackState
import com.wcaokaze.probosqis.capsiqum.PageStateStore
import com.wcaokaze.probosqis.panoptiqon.WritableCache

@Composable
fun <P : Page> PagePreview(
   page: P,
   pageComposable: PageComposable<P, *>
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

   Surface(
      shape = MaterialTheme.shapes.large,
      tonalElevation = 3.dp,
      shadowElevation = 4.dp
   ) {
      Column {
         @OptIn(ExperimentalMaterial3Api::class)
         PageStackAppBar(
            pageStackState,
            pageComposableSwitcher,
            pageStateStore,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = TopAppBarDefaults.topAppBarColors(
               containerColor = MaterialTheme.colorScheme.primaryContainer
            )
         )

         PageContent(
            savedPageState,
            pageComposableSwitcher,
            pageStateStore,
            pageStackState
         )
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
