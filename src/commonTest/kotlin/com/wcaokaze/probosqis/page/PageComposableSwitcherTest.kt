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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PageComposableSwitcherTest {
   private class PageA : Page()
   private class PageAState : PageState()
   private class PageB : Page()
   private class PageBState : PageState()
   private class PageC : Page()

   @Test
   fun getPageComposable() {
      val pageComposableSwitcher = PageComposableSwitcher(
         listOf(
            pageComposable<PageA, PageAState>(
               pageStateFactory { _, _ -> PageAState() },
               content = { _, _, _ -> },
               header = { _, _, _ -> },
               footer = null,
               pageTransitions = {}
            ),
            pageComposable<PageB, PageBState>(
               pageStateFactory { _, _ -> PageBState() },
               content = { _, _, _ -> },
               header = { _, _, _ -> },
               footer = null,
               pageTransitions = {}
            ),
         )
      )

      val pageA = PageA()
      val pageComposableA = pageComposableSwitcher[pageA]
      assertNotNull(pageComposableA)
      assertEquals(pageComposableA.pageClass, PageA::class)

      val pageB = PageB()
      val pageComposableB = pageComposableSwitcher[pageB]
      assertNotNull(pageComposableB)
      assertEquals(pageComposableB.pageClass, PageB::class)

      val pageC = PageC()
      val pageComposableC = pageComposableSwitcher[pageC]
      assertNull(pageComposableC)
   }
}
