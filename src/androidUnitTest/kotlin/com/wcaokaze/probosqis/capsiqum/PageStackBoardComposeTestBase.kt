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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import kotlinx.coroutines.CoroutineScope

abstract class PageStackBoardComposeTestBase {
   protected abstract val pageStackRepository: PageStackRepository

   protected val pageStackBoardTag = "PageStackBoard"

   protected class TestPage(val i: Int) : Page()
   protected class TestPageState : PageState()

   @Composable
   protected inline fun <reified P : Page> rememberPageComposableSwitcher(
      noinline pageStateFactory: (P, PageState.StateSaver) -> PageState,
      noinline pageComposable: @Composable (P, PageState, PageStackState) -> Unit,
   ): Pair<PageComposableSwitcher, PageStateStore> {
      val headerComposable: @Composable (Page, PageState, PageStackState) -> Unit
            = { _, _, _ -> }

      val coroutineScope = rememberCoroutineScope()

      return remember {
         val testPageComposable = pageComposable(
            pageStateFactory(pageStateFactory),
            pageComposable,
            headerComposable,
            footer = null,
            pageTransitions = {}
         )

         Pair(
            PageComposableSwitcher(
               allPageComposables = listOf(testPageComposable)
            ),
            PageStateStore(
               allPageStateFactories = listOf(testPageComposable.pageStateFactory),
               coroutineScope
            )
         )
      }
   }

   @Composable
   protected fun rememberDefaultPageComposableSwitcher()
      : Pair<PageComposableSwitcher, PageStateStore>
   {
      return rememberPageComposableSwitcher<TestPage>(
         { _, _ -> TestPageState() },
         { page, _, pageStackState ->
            Column {
               Text(
                  "${page.i}",
                  modifier = Modifier.fillMaxWidth()
               )

               Button(
                  onClick = {
                     val newPage = TestPage(page.i + 100)
                     val newPageStack = PageStack(
                        PageStack.Id(pageStackState.pageStack.id.value + 100L),
                        PageStack.SavedPageState(
                           PageStack.PageId(newPage.i.toLong()),
                           newPage
                        )
                     )
                     pageStackState.addColumn(newPageStack)
                  }
               ) {
                  Text("Add PageStack ${page.i}")
               }

               Button(
                  onClick = {
                     pageStackState.removeFromBoard()
                  }
               ) {
                  Text("Remove PageStack ${page.i}")
               }
            }
         }
      )
   }

   protected fun createPageStackBoard(
      pageStackCount: Int
   ): WritableCache<PageStackBoard> {
      return createPageStackBoard(
         List(pageStackCount) { createPageStack(it) }
      )
   }

   protected fun createPageStackBoard(page: Page): WritableCache<PageStackBoard> {
      return createPageStackBoard(
         listOf(
            createPageStack(0, page),
         )
      )
   }

   protected fun createPageStackBoard(
      pageStackList: List<PageStackBoard.PageStack>
   ): WritableCache<PageStackBoard> {
      val rootRow = PageStackBoard.Row(pageStackList)
      val pageStackBoard = PageStackBoard(rootRow)
      return WritableCache(pageStackBoard)
   }

   protected fun createPageStack(i: Int): PageStackBoard.PageStack {
      return createPageStack(i, TestPage(i))
   }

   protected fun createPageStack(id: Int, page: Page): PageStackBoard.PageStack {
      val pageStack = PageStack(
         PageStack.Id(id.toLong()),
         PageStack.SavedPageState(
            PageStack.PageId(id.toLong()),
            page
         )
      )
      val cache = pageStackRepository.savePageStack(pageStack)
      return PageStackBoard.PageStack(
         PageStackBoard.PageStackId(pageStack.id.value),
         cache
      )
   }
}

@Stable
internal class RememberedPageStackBoardState<S : PageStackBoardState>(
   val pageStackBoardState: S,
   val coroutineScope: CoroutineScope
) {
   operator fun component1() = pageStackBoardState
   operator fun component2() = coroutineScope
}
