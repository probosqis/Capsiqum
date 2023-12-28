/*
 * Copyright 2023 wcaokaze
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

package com.wcaokaze.probosqis.page

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class PageStateStoreTest {
   private class PageA : Page()
   private class PageAState : PageState()
   private class PageB : Page()
   private class PageBState : PageState()
   private class PageC : Page()

   @Test
   fun instantiate() {
      val pageStateStore = PageStateStore(
         listOf(
            pageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            pageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         appCoroutineScope = mockk()
      )

      val pageAState = pageStateStore.get(
         PageStack.SavedPageState(
            PageStack.PageId(0L), PageA()
         )
      )

      assertIs<PageAState>(pageAState)

      val pageBState = pageStateStore.get(
         PageStack.SavedPageState(
            PageStack.PageId(1L), PageB()
         )
      )

      assertIs<PageBState>(pageBState)

      assertFails {
         pageStateStore.get(
            PageStack.SavedPageState(
               PageStack.PageId(2L), PageC()
            )
         )
      }
   }

   @Test
   fun cache() {
      val pageStateStore = PageStateStore(
         listOf(
            pageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
            pageStateFactory<PageB, PageBState> { _, _ -> PageBState() },
         ),
         appCoroutineScope = mockk()
      )

      val pageA = PageA()

      val pageState1 = pageStateStore.get(
         PageStack.SavedPageState(
            PageStack.PageId(0L), pageA
         )
      )

      val pageState2 = pageStateStore.get(
         PageStack.SavedPageState(
            PageStack.PageId(0L), pageA
         )
      )

      assertSame(pageState1, pageState2)

      val pageState3 = pageStateStore.get(
         PageStack.SavedPageState(
            PageStack.PageId(1L), pageA
         )
      )

      assertNotSame(pageState1, pageState3)
   }
}
