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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PageStackStateTest {
   private class PageA : Page()
   private class PageAState : PageState<PageA>()
   private class PageB : Page()
   private class PageBState : PageState<PageB>()
   private class PageC : Page()

   @BeforeTest
   fun beforeTest() {
      PageStateHiddenArguments.clear()
   }

   @Test
   fun instantiatePageStack() {
      val pageA = SavedPageState(PageId(0L), PageA())
      val pageB = SavedPageState(PageId(1L), PageB())

      val pageStack = PageStack(PageStack.Id(0L), pageA).added(pageB)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      val pageAState = pageStackState.getPageState(pageA)
      assertIs<PageAState>(pageAState)

      val pageBState = pageStackState.getPageState(pageB)
      assertIs<PageBState>(pageBState)
   }

   @Test
   fun instantiatePageStack_noFactory() {
      val pageA = SavedPageState(PageId(0L), PageA())
      val pageStack = PageStack(PageStack.Id(0L), pageA)

      val pageStackState = PageStackState(
         pageStack,
         allPageStateFactories = emptyList(),
         CoroutineScope(EmptyCoroutineContext)
      )

      assertFails {
         pageStackState.getPageState(pageA)
      }
   }

   @Test
   fun instantiatePageStack_anotherThread() {
      val pageA = SavedPageState(PageId(0L), PageA())
      val pageStack = PageStack(PageStack.Id(0L), pageA)

      val pageStackState = PageStackState(
         pageStack,
         allPageStateFactories = listOf(
            PageStateFactory<PageA, PageAState> { _, _ ->
               runBlocking {
                  CoroutineScope(Dispatchers.Default)
                     .async { PageAState() }
                     .await()
               }
            },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      assertFails {
         pageStackState.getPageState(pageA)
      }
   }

   @Test
   fun instantiatePageStack_withoutFactory() {
      assertFails {
         PageAState()
      }
   }

   @Test
   fun cachePageStack() {
      val page1 = SavedPageState(PageId(0L), PageA())
      val page2 = SavedPageState(PageId(1L), PageA())

      val pageStack = PageStack(PageStack.Id(0L), page1).added(page2)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      val pageState1 = pageStackState.getPageState(page1)
      val pageState2 = pageStackState.getPageState(page1)
      assertSame(pageState1, pageState2)

      val pageState3 = pageStackState.getPageState(page2)
      assertNotSame(pageState1, pageState3)
   }

   @Test
   fun getPageState_whichIsNotInPageStack() {
      val page1 = SavedPageState(PageId(0L), PageA())
      val page2 = SavedPageState(PageId(1L), PageB())

      val pageStack = PageStack(PageStack.Id(0L), page1)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      assertFails {
         pageStackState.getPageState(page2)
      }
   }

   @Test
   fun pageStateScope_inactiveAfterPageRemoved() {
      val pageA = SavedPageState(PageId(0L), PageA())
      val pageB = SavedPageState(PageId(1L), PageB())

      val pageStack = PageStack(PageStack.Id(0L), pageA).added(pageB)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      val pageBState = pageStackState.getPageState(pageB)
      assertTrue(pageBState.pageStateScope.isActive)

      pageStackState.pageStack = assertNotNull(pageStackState.pageStack.tailOrNull())

      assertFalse(pageBState.pageStateScope.isActive)
   }

   @Test
   fun pageStateScope_activeAfterPageAdded() {
      val pageA = SavedPageState(PageId(0L), PageA())

      val pageStack = PageStack(PageStack.Id(0L), pageA)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      val pageAState = pageStackState.getPageState(pageA)
      assertTrue(pageAState.pageStateScope.isActive)

      val pageB = SavedPageState(PageId(1L), PageB())
      pageStackState.pageStack = pageStackState.pageStack.added(pageB)

      assertTrue(pageAState.pageStateScope.isActive)

      pageStackState.pageStack = assertNotNull(pageStackState.pageStack.tailOrNull())
      assertTrue(pageAState.pageStateScope.isActive)
   }

   @Test
   fun pageStateScope_parentScopeIsActiveAfterChildCancelled() {
      val pageA = SavedPageState(PageId(0L), PageA())
      val pageB = SavedPageState(PageId(1L), PageB())

      val pageStack = PageStack(PageStack.Id(0L), pageA).added(pageB)

      val parentScope = CoroutineScope(EmptyCoroutineContext)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         parentScope
      )

      assertTrue(parentScope.isActive)

      val pageBState = pageStackState.getPageState(pageB)
      pageStackState.pageStack = assertNotNull(pageStackState.pageStack.tailOrNull())
      assertFalse(pageBState.pageStateScope.isActive)

      assertTrue(parentScope.isActive)
   }

   @Test
   fun pageState_injection() {
      val page = SavedPageState(PageId(0L), PageA())

      val pageStack = PageStack(PageStack.Id(0L), page)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
         ),
         CoroutineScope(EmptyCoroutineContext)
      )

      val pageState = pageStackState.getPageState(page)
      assertSame(page.page, pageState.page)
      assertEquals(page.id, pageState.pageId)
   }
}
