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

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class PageStackStateTest {
   private class PageA : Page()
   private class PageAState : PageState()
   private class PageB : Page()
   private class PageBState : PageState()
   private class PageC : Page()

   @Test
   fun instantiatePageStack() {
      val pageA = SavedPageState(PageId(0L), PageA())
      val pageB = SavedPageState(PageId(1L), PageB())

      val pageStack = PageStack(PageStack.Id(0L), pageA).added(pageB)

      val pageStackState = PageStackState(
         pageStack,
         listOf(
            PageStateFactory<PageA, PageAState> { _, _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _, _ -> PageBState() },
         ),
         appCoroutineScope = mockk()
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
         initialPageStack = pageStack,
         allPageStateFactories = emptyList(),
         appCoroutineScope = mockk()
      )

      assertFails {
         pageStackState.getPageState(pageA)
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
            PageStateFactory<PageA, PageAState> { _, _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _, _ -> PageBState() },
         ),
         appCoroutineScope = mockk()
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
            PageStateFactory<PageA, PageAState> { _, _, _ -> PageAState() },
            PageStateFactory<PageB, PageBState> { _, _, _ -> PageBState() },
         ),
         appCoroutineScope = mockk()
      )

      assertFails {
         pageStackState.getPageState(page2)
      }
   }
}
