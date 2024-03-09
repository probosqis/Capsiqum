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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
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
   private class PageAState : PageState()
   private class PageBState : PageState()
// private class PageCState : PageState()

   @Test
   fun state_getChild() {
      val switcherState = PageSwitcherState(
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
   fun illegalArgument_noPageStateFactory() {
      val pageStateStore = PageStateStore(
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
         ),
         object : CoroutineScope {
            override val coroutineContext = EmptyCoroutineContext
         }
      )

      assertFails { 
         PageSwitcherState(
            listOf(
               PageComposable<PageA, PageAState> { _, _ -> },
               PageComposable<PageB, PageBState> { _, _ -> },
            ),
            pageStateStore
         )
      }
   }

   @Test
   fun illegalArgument_pageStateTypeUnmatched() {
      val pageStateStore = PageStateStore(
         listOf(
            PageStateFactory<PageA, PageAState> { _, _ -> PageAState() },
         ),
         object : CoroutineScope {
            override val coroutineContext = EmptyCoroutineContext
         }
      )

      assertFails {
         PageSwitcherState(
            listOf(
               PageComposable<PageA, PageBState> { _, _ -> },
            ),
            pageStateStore
         )
      }
   }

   @Test
   fun pageComposable_argument() {
      var pageAArgument: PageA? = null
      var pageBArgument: PageB? = null

      lateinit var switcherState: PageSwitcherState

      var savedPageState by mutableStateOf(
         PageStack.SavedPageState(PageStack.PageId(0L), PageA()))

      rule.setContent {
         val coroutineScope = rememberCoroutineScope()

         switcherState = remember {
            PageSwitcherState(
               listOf(
                  PageComposable<PageA, PageAState>(
                     stateFactory = { _, _ -> PageAState() },
                     composable = { page, _ ->
                        DisposableEffect(Unit) {
                           pageAArgument = page
                           onDispose { pageAArgument = null }
                        }
                     }
                  ),
                  PageComposable<PageB, PageBState>(
                     stateFactory = { _, _ -> PageBState() },
                     composable = { page, _ ->
                        DisposableEffect(Unit) {
                           pageBArgument = page
                           onDispose { pageBArgument = null }
                        }
                     }
                  ),
               ),
               coroutineScope
            )
         }

         PageSwitcher(switcherState, savedPageState)
      }

      rule.runOnIdle {
         assertSame(savedPageState.page, assertNotNull(pageAArgument))
         assertNull(pageBArgument)
      }

      savedPageState = PageStack.SavedPageState(PageStack.PageId(1L), PageB())

      rule.runOnIdle {
         assertNull(pageAArgument)
         assertSame(savedPageState.page, assertNotNull(pageBArgument))
      }
   }
}
