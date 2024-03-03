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

package com.wcaokaze.probosqis.capsiqum.page

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class PageSwitcherTest {
   @get:Rule
   val rule = createComposeRule()

   private class PageA : Page()
   private class PageB : Page()
   private class PageC : Page()

   @Test
   fun state_getChild() {
      val switcherState = PageSwitcherState(
         listOf(
            pageComposable<PageA> {},
            pageComposable<PageB> {},
         )
      )

      val pageA = PageA()
      val composableA = switcherState.getComposableFor(pageA)
      assertNotNull(composableA)
      assertEquals(composableA.pageClass, PageA::class)

      val pageB = PageB()
      val composableB = switcherState.getComposableFor(pageB)
      assertNotNull(composableB)
      assertEquals(composableB.pageClass, PageB::class)

      val pageC = PageC()
      val composableC = switcherState.getComposableFor(pageC)
      assertNull(composableC)
   }

   @Test
   fun pageComposable_argument() {
      var pageAArgument: PageA? = null
      var pageBArgument: PageB? = null

      val switcherState = PageSwitcherState(
         listOf(
            pageComposable<PageA> { page ->
               DisposableEffect(Unit) {
                  pageAArgument = page
                  onDispose { pageAArgument = null }
               }
            },
            pageComposable<PageB> { page ->
               DisposableEffect(Unit) {
                  pageBArgument = page
                  onDispose { pageBArgument = null }
               }
            },
         )
      )

      var page: Page by mutableStateOf(PageA())

      rule.setContent {
         PageSwitcher(switcherState, page)
      }

      rule.runOnIdle {
         assertSame(page, assertNotNull(pageAArgument))
         assertNull(pageBArgument)
      }

      page = PageB()

      rule.runOnIdle {
         assertNull(pageAArgument)
         assertSame(page, assertNotNull(pageBArgument))
      }
   }
}
