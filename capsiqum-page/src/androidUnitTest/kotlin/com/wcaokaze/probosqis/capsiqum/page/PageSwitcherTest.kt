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
import kotlinx.collections.immutable.persistentListOf
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
   private class PageAState : PageState()
   private class PageBState : PageState()
// private class PageCState : PageState()

   @Test
   fun state_getChild() {
      val switcherState = PageSwitcherState(
         listOf(
            PageComposable<PageA, PageAState> { _, _ -> },
            PageComposable<PageB, PageBState> { _, _ -> },
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
      var pageStateAArgumentPage: PageA? = null
      var pageStateBArgumentPage: PageB? = null
      var pageStateAArgumentPageId: PageId? = null
      var pageStateBArgumentPageId: PageId? = null
      var pageAArgumentPage: PageA? = null
      var pageBArgumentPage: PageB? = null

      var savedPageState by mutableStateOf(
         SavedPageState(PageId(0L), PageA()))

      val pageComposables = persistentListOf(
         PageComposable<PageA, PageAState> { page, _ ->
            DisposableEffect(Unit) {
               pageAArgumentPage = page
               onDispose { pageAArgumentPage = null }
            }
         },
         PageComposable<PageB, PageBState> { page, _ ->
            DisposableEffect(Unit) {
               pageBArgumentPage = page
               onDispose { pageBArgumentPage = null }
            }
         }
      )

      val pageStateFactories = listOf(
         PageStateFactory<PageA, PageAState> { page, pageId, _ ->
            pageStateAArgumentPage = page
            pageStateAArgumentPageId = pageId
            PageAState()
         },
         PageStateFactory<PageB, PageBState> { page, pageId, _ ->
            pageStateBArgumentPage = page
            pageStateBArgumentPageId = pageId
            PageBState()
         },
      )

      rule.setContent {
         val coroutineScope = rememberCoroutineScope()

         val pageStateStore = remember {
            PageStateStore(pageStateFactories, coroutineScope)
         }

         PageSwitcher(savedPageState, pageComposables, pageStateStore)
      }

      rule.runOnIdle {
         assertSame(savedPageState.page, assertNotNull(pageStateAArgumentPage))
         assertEquals(savedPageState.id, pageStateAArgumentPageId)
         assertSame(savedPageState.page, assertNotNull(pageAArgumentPage))

         assertNull(pageStateBArgumentPage)
         assertNull(pageStateBArgumentPageId)
         assertNull(pageBArgumentPage)
      }

      savedPageState = SavedPageState(PageId(1L), PageB())

      rule.runOnIdle {
         // PageStateFactoryではDisposableEffectを使用しないため以前の値が残っている
         // assertNull(pageStateAArgumentPage)
         // assertNull(pageStateAArgumentPageId)
         assertNull(pageAArgumentPage)

         assertSame(savedPageState.page, assertNotNull(pageStateBArgumentPage))
         assertEquals(savedPageState.id, pageStateBArgumentPageId)
         assertSame(savedPageState.page, assertNotNull(pageBArgumentPage))
      }
   }
}
