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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
   private class PageAState : PageState()
   private class PageBState : PageState()

   @Test
   fun pageComposable_argument() {
      var pageStateAArgumentPage: PageA? = null
      var pageStateBArgumentPage: PageB? = null
      var pageStateAArgumentPageId: PageId? = null
      var pageStateBArgumentPageId: PageId? = null
      var pageAArgumentPage: PageA? = null
      var pageBArgumentPage: PageB? = null

      val pageStack = PageStack(
         PageStack.Id(0L),
         SavedPageState(PageId(0L), PageA())
      )

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

      lateinit var pageStackState: PageStackState

      rule.setContent {
         val coroutineScope = rememberCoroutineScope()

         pageStackState = remember {
            PageStackState(pageStack, pageStateFactories, coroutineScope)
         }

         PageSwitcher(pageStackState, pageComposables)
      }

      rule.runOnIdle {
         val head = pageStackState.pageStack.head
         assertSame(head.page, assertNotNull(pageStateAArgumentPage))
         assertEquals(head.id, pageStateAArgumentPageId)
         assertSame(head.page, assertNotNull(pageAArgumentPage))

         assertNull(pageStateBArgumentPage)
         assertNull(pageStateBArgumentPageId)
         assertNull(pageBArgumentPage)
      }

      pageStackState.pageStack = pageStackState.pageStack.added(
         SavedPageState(PageId(1L), PageB())
      )

      rule.runOnIdle {
         // PageStateFactoryではDisposableEffectを使用しないため以前の値が残っている
         // assertNull(pageStateAArgumentPage)
         // assertNull(pageStateAArgumentPageId)
         assertNull(pageAArgumentPage)

         val head = pageStackState.pageStack.head
         assertSame(head.page, assertNotNull(pageStateBArgumentPage))
         assertEquals(head.id, pageStateBArgumentPageId)
         assertSame(head.page, assertNotNull(pageBArgumentPage))
      }
   }
}
