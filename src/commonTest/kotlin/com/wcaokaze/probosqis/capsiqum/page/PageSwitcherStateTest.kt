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
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PageSwitcherStateTest {
   private class PageA : Page()
   private class PageAState : PageState()
   private class PageB : Page()
   private class PageBState : PageState()
   private class PageC : Page()

   @Test
   fun getPageComposable() {
      val pageSwitcherState = PageSwitcherState(
         listOf(
            PageComposable<PageA, PageAState>(
               stateFactory = { _, _ -> PageAState() },
               composable = { _, _ -> }
            ),
            PageComposable<PageB, PageBState>(
               stateFactory = { _, _ -> PageBState() },
               composable = { _, _ -> }
            ),
         ),
         object : CoroutineScope {
            override val coroutineContext = EmptyCoroutineContext
         }
      )

      val pageA = PageA()
      val pageComposableA = pageSwitcherState.getComposableFor(pageA)
      assertNotNull(pageComposableA)
      assertEquals(pageComposableA.pageClass, PageA::class)

      val pageB = PageB()
      val pageComposableB = pageSwitcherState.getComposableFor(pageB)
      assertNotNull(pageComposableB)
      assertEquals(pageComposableB.pageClass, PageB::class)

      val pageC = PageC()
      val pageComposableC = pageSwitcherState.getComposableFor(pageC)
      assertNull(pageComposableC)
   }
}
