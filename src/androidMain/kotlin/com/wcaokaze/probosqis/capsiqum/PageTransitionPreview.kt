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

package com.wcaokaze.probosqis.capsiqum

import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wcaokaze.probosqis.capsiqum.transition.PageTransitionPreview
import com.wcaokaze.probosqis.panoptiqon.WritableCache

enum class PageTransitionPreviewValue {
   Parent,
   Child
}

@Composable
fun <P : Page, C : Page> PageTransitionPreview(
   parentPage: P,
   childPage:  C,
   parentPageComposable: PageComposable<P, *>,
   childPageComposable:  PageComposable<C, *>
) {
   val parentPageState = PageStack.SavedPageState(PageStack.PageId(0L), parentPage)
   val childPageState  = PageStack.SavedPageState(PageStack.PageId(1L), childPage)

   val indexedParentPageState = IndexedValue(0, parentPageState)
   val indexedChildPageState  = IndexedValue(1, childPageState)

   val pageStackCache = remember {
      val pageStack = PageStack(PageStack.Id(0L), parentPageState)
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
         listOf(parentPageComposable, childPageComposable)
      )
   }

   val pageStateStore = remember {
      PageStateStore(
         listOf(
            parentPageComposable.pageStateFactory,
            childPageComposable .pageStateFactory,
         ),
         coroutineScope
      )
   }

   val transition = updateTransition(
      PageTransitionPreviewValue.Parent, label = "PageTransition")

   @OptIn(ExperimentalTransitionApi::class)
   val pageStateTransition = transition.createChildTransition {
      when (it) {
         PageTransitionPreviewValue.Parent -> indexedParentPageState
         PageTransitionPreviewValue.Child  -> indexedChildPageState
      }
   }

   PageTransitionPreview(
      pageStackState,
      pageComposableSwitcher,
      pageStateStore,
      pageStateTransition
   )
}

private class PreviewPageStackRepository : PageStackRepository {
   override fun deleteAllPageStacks() {}
   override fun loadPageStack(id: PageStack.Id): WritableCache<PageStack>
         = throw NotImplementedError()
   override fun savePageStack(pageStack: PageStack): WritableCache<PageStack>
         = throw NotImplementedError()
}
