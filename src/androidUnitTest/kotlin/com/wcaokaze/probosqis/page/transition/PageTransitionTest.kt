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

package com.wcaokaze.probosqis.page.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import com.wcaokaze.probosqis.page.Page
import com.wcaokaze.probosqis.page.PageComposable
import com.wcaokaze.probosqis.page.PageComposableSwitcher
import com.wcaokaze.probosqis.page.PageStack
import com.wcaokaze.probosqis.page.PageStackBoard
import com.wcaokaze.probosqis.page.PageStackState
import com.wcaokaze.probosqis.page.PageState
import com.wcaokaze.probosqis.page.PageStateStore
import com.wcaokaze.probosqis.page.PageTransitionSet
import com.wcaokaze.probosqis.page.SingleColumnPageStackBoardState
import com.wcaokaze.probosqis.page.SpyPage
import com.wcaokaze.probosqis.page.pageStateFactory
import com.wcaokaze.probosqis.page.spyPageComposable
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PageTransitionTest {
   @get:Rule
   val rule = createComposeRule()

   // ==== Test Utils ==========================================================

   private class PageA : Page()
   private class PageB : Page()
   private class PageAState : PageState()
   private class PageBState : PageState()

   private fun createPageStackState(initialPage: Page): PageStackState {
      val pageStackCache = WritableCache(
         PageStack(
            PageStack.Id(0L),
            PageStack.SavedPageState(
               PageStack.PageId(0L),
               initialPage
            )
         )
      )

      return PageStackState(
         PageStackBoard.PageStackId(0L),
         pageStackCache,
         pageStackBoardState = mockk()
      )
   }

   private fun pageAComposable(
      pageTransitions: PageTransitionSet.Builder.() -> Unit = {},
      content: @Composable (PageA, PageAState, PageStackState) -> Unit
   ) = PageComposable(
      PageA::class,
      pageStateFactory { _, _ -> PageAState() },
      content,
      headerComposable = { _, _, _ -> },
      footerComposable = null,
      pageTransitionSet = PageTransitionSet.Builder().apply(pageTransitions).build()
   )

   private fun pageBComposable(
      pageTransitions: PageTransitionSet.Builder.() -> Unit = {},
      content: @Composable (PageB, PageBState, PageStackState) -> Unit
   ) = PageComposable(
      PageB::class,
      pageStateFactory { _, _ -> PageBState() },
      content,
      headerComposable = { _, _, _ -> },
      footerComposable = null,
      pageTransitionSet = PageTransitionSet.Builder().apply(pageTransitions).build()
   )

   private data class RememberedPageStateStore(
      val coroutineScope: CoroutineScope,
      val pageComposableSwitcher: PageComposableSwitcher,
      val pageStateStore: PageStateStore
   )

   @Composable
   private fun rememberPageStateStore(
      vararg pageComposables: PageComposable<*, *>
   ): RememberedPageStateStore {
      val coroutineScope = rememberCoroutineScope()
      return remember(pageComposables) {
         RememberedPageStateStore(
            coroutineScope,
            PageComposableSwitcher(pageComposables.toList()),
            PageStateStore(pageComposables.map { it.pageStateFactory }, coroutineScope)
         )
      }
   }

   // ==== Tests ===============================================================

   @Test
   fun pageLayoutInfo_getViaCompositionLocal() {
      var pageALayoutInfo: PageLayoutInfo? = null
      var pageBLayoutInfo: PageLayoutInfo? = null

      val pageAComposable = pageAComposable { _, _, _ ->
         pageALayoutInfo = LocalPageLayoutInfo.current
      }

      val pageBComposable = pageBComposable { _, _, _ ->
         pageBLayoutInfo = LocalPageLayoutInfo.current
      }

      val pageStackState = createPageStackState(PageA())

      lateinit var coroutineScope: CoroutineScope
      rule.setContent {
         val remembered = rememberPageStateStore(pageAComposable, pageBComposable)

         SideEffect {
            coroutineScope = remembered.coroutineScope
         }

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.runOnIdle {
         assertNotNull(pageALayoutInfo)
         assertNull   (pageBLayoutInfo)
      }

      coroutineScope.launch {
         pageStackState.startPage(PageB())
      }

      rule.runOnIdle {
         assertNotNull(pageALayoutInfo)
         assertNotNull(pageBLayoutInfo)
         assertNotSame(pageALayoutInfo, pageBLayoutInfo)
      }
   }

   @Test
   fun pageLayoutInfo_getFromOutsideOfPage() {
      assertFails {
         rule.setContent {
            LocalPageLayoutInfo.current
         }
         rule.waitForIdle()
      }
   }

   @Test
   fun pageTransition() {
      val page1 = SpyPage()
      val page2 = SpyPage()
      val page3 = SpyPage()

      val pageStackState by derivedStateOf {
         var pageStack = PageStack(
            PageStack.Id(0L),
            PageStack.SavedPageState(
               PageStack.PageId(0L),
               page1
            )
         )
         pageStack = pageStack.added(
            PageStack.SavedPageState(
               PageStack.PageId(1L),
               page2
            )
         )

         PageStackState(
            PageStackBoard.PageStackId(pageStack.id.value),
            WritableCache(pageStack),
            mockk<SingleColumnPageStackBoardState>()
         )
      }

      rule.setContent {
         val remembered = rememberPageStateStore(spyPageComposable)

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.runOnIdle {
         assertEquals(0, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(0, page3.recompositionCount)
      }

      pageStackState.pageStackCache.value =
         assertNotNull(pageStackState.pageStack.tailOrNull())

      rule.runOnIdle {
         assertEquals(1, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(0, page3.recompositionCount)
      }

      pageStackState.pageStackCache.value = pageStackState.pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(2L),
            page3
         )
      )

      rule.runOnIdle {
         assertEquals(1, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(1, page3.recompositionCount)
      }
   }

   @Test
   fun pageTransition_viaPageStackState() {
      val page1 = SpyPage()
      val page2 = SpyPage()
      val page3 = SpyPage()

      val pageStackId = PageStackBoard.PageStackId(0L)

      val initialPageStack = PageStack(
         PageStack.Id(pageStackId.value),
         PageStack.SavedPageState(
            PageStack.PageId(0L),
            page1
         )
      )

      val pageStackBoardState = mockk<SingleColumnPageStackBoardState> {
         every { removePageStack(any()) } returns Job().apply { complete() }
      }

      val pageStackState = PageStackState(
         pageStackId,
         WritableCache(initialPageStack),
         pageStackBoardState
      )

      rule.setContent {
         val remembered = rememberPageStateStore(spyPageComposable)

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.runOnIdle {
         assertEquals(1, page1.recompositionCount)
         assertEquals(0, page2.recompositionCount)
         assertEquals(0, page3.recompositionCount)
         verify(inverse = true) { pageStackBoardState.removePageStack(pageStackId) }
      }

      pageStackState.startPage(page2)

      rule.runOnIdle {
         assertEquals(1, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(0, page3.recompositionCount)
         verify(inverse = true) { pageStackBoardState.removePageStack(pageStackId) }
      }

      pageStackState.finishPage()

      rule.runOnIdle {
         assertEquals(2, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(0, page3.recompositionCount)
         verify(inverse = true) { pageStackBoardState.removePageStack(pageStackId) }
      }

      pageStackState.startPage(page3)

      rule.runOnIdle {
         assertEquals(2, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(1, page3.recompositionCount)
         verify(inverse = true) { pageStackBoardState.removePageStack(pageStackId) }
      }

      pageStackState.finishPage()

      rule.runOnIdle {
         assertEquals(3, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(1, page3.recompositionCount)
         verify(inverse = true) { pageStackBoardState.removePageStack(pageStackId) }
      }

      pageStackState.finishPage()

      rule.runOnIdle {
         assertEquals(3, page1.recompositionCount)
         assertEquals(1, page2.recompositionCount)
         assertEquals(1, page3.recompositionCount)
         verify { pageStackBoardState.removePageStack(pageStackId) }
      }
   }

   @Test
   fun onlyForefrontComposableIsCalled() {
      var pageAComposed = false
      var pageBComposed = false

      val pageAComposable = pageAComposable { _, _, _ ->
         DisposableEffect(Unit) {
            pageAComposed = true
            onDispose {
               pageAComposed = false
            }
         }
      }

      val pageBComposable = pageBComposable { _, _, _ ->
         DisposableEffect(Unit) {
            pageBComposed = true
            onDispose {
               pageBComposed = false
            }
         }
      }

      val pageStackState = createPageStackState(PageA())

      rule.setContent {
         val remembered = rememberPageStateStore(pageAComposable, pageBComposable)

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.runOnIdle {
         assertTrue (pageAComposed)
         assertFalse(pageBComposed)
      }

      pageStackState.startPage(PageB())

      rule.runOnIdle {
         assertFalse(pageAComposed)
         assertTrue (pageBComposed)
      }

      pageStackState.finishPage()

      rule.runOnIdle {
         assertTrue (pageAComposed)
         assertFalse(pageBComposed)
      }
   }

   @Test
   fun composeBothDuringTransition() {
      var pageAComposed = false
      var pageBComposed = false

      val pageAComposable = pageAComposable { _, _, _ ->
         DisposableEffect(Unit) {
            pageAComposed = true
            onDispose {
               pageAComposed = false
            }
         }
      }

      val pageBComposable = pageBComposable { _, _, _ ->
         DisposableEffect(Unit) {
            pageBComposed = true
            onDispose {
               pageBComposed = false
            }
         }
      }

      val pageStackState = createPageStackState(PageA())

      rule.setContent {
         val remembered = rememberPageStateStore(pageAComposable, pageBComposable)

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.waitForIdle()

      assertTrue(pageAComposed)
      assertFalse(pageBComposed)

      rule.mainClock.autoAdvance = false
      pageStackState.startPage(PageB())

      rule.waitUntil {
         rule.waitForIdle()
         rule.mainClock.advanceTimeByFrame()

         pageAComposed && pageBComposed
      }

      rule.mainClock.autoAdvance = true
      rule.waitForIdle()

      assertFalse(pageAComposed)
      assertTrue(pageBComposed)
   }

   @Test
   fun pageLayoutInfo_doesntChangeInstance() {
      var pageAComposed = false
      var pageBComposed = false
      var pageAPageLayoutInfo: PageLayoutInfo? = null
      var pageBPageLayoutInfo: PageLayoutInfo? = null

      val pageAComposable = pageAComposable { _, _, _ ->
         pageAPageLayoutInfo = LocalPageLayoutInfo.current
         DisposableEffect(Unit) {
            pageAComposed = true
            onDispose {
               pageAComposed = false
            }
         }
      }

      val pageBComposable = pageBComposable { _, _, _ ->
         pageBPageLayoutInfo = LocalPageLayoutInfo.current
         DisposableEffect(Unit) {
            pageBComposed = true
            onDispose {
               pageBComposed = false
            }
         }
      }

      val pageStackState = createPageStackState(PageA())

      rule.setContent {
         val remembered = rememberPageStateStore(pageAComposable, pageBComposable)

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.waitForIdle()

      val prevALayoutInfo = assertNotNull(pageAPageLayoutInfo)

      rule.mainClock.autoAdvance = false
      pageStackState.startPage(PageB())

      rule.waitUntil {
         rule.waitForIdle()
         rule.mainClock.advanceTimeByFrame()

         pageAComposed && pageBComposed
      }

      assertSame(prevALayoutInfo, pageAPageLayoutInfo)
      val prevBLayoutInfo = assertNotNull(pageBPageLayoutInfo)

      rule.mainClock.autoAdvance = true
      rule.waitForIdle()

      assertSame(prevBLayoutInfo, pageBPageLayoutInfo)

      rule.mainClock.autoAdvance = false
      pageStackState.finishPage()

      rule.waitUntil {
         rule.waitForIdle()
         rule.mainClock.advanceTimeByFrame()

         pageAComposed && pageBComposed
      }

      assertSame(prevBLayoutInfo, pageBPageLayoutInfo)
      assertNotSame(prevALayoutInfo, pageAPageLayoutInfo)
   }

   private fun testPageTransitionSpec(
      expectedTransitionSpec: PageTransitionSpec,
      pageATransitions: PageTransitionSet.Builder.() -> Unit,
      pageBTransitions: PageTransitionSet.Builder.() -> Unit
   ) {
      var pageATransitionAnimations: PageTransitionElementAnimSet? = null
      var pageBTransitionAnimations: PageTransitionElementAnimSet? = null

      val pageAComposable = pageAComposable(
         content = { _, _, _ ->
            pageATransitionAnimations = LocalPageTransitionAnimations.current
         },
         pageTransitions = pageATransitions
      )

      val pageBComposable = pageBComposable(
         content = { _, _, _ ->
            pageBTransitionAnimations = LocalPageTransitionAnimations.current
         },
         pageTransitions = pageBTransitions
      )

      val pageStackState = createPageStackState(PageA())

      rule.setContent {
         val remembered = rememberPageStateStore(pageAComposable, pageBComposable)

         PageTransition(
            pageStackState,
            remembered.pageComposableSwitcher,
            remembered.pageStateStore
         )
      }

      rule.waitForIdle()

      rule.mainClock.autoAdvance = false
      pageStackState.startPage(PageB())

      rule.waitUntil {
         rule.waitForIdle()
         rule.mainClock.advanceTimeByFrame()

         val expectedPageAAnimations
               = expectedTransitionSpec.enteringCurrentPageElementAnimations
         val expectedPageBAnimations
               = expectedTransitionSpec.enteringTargetPageElementAnimations

         pageATransitionAnimations === expectedPageAAnimations &&
         pageBTransitionAnimations === expectedPageBAnimations
      }

      rule.mainClock.autoAdvance = true
      rule.waitForIdle()
      rule.mainClock.autoAdvance = false
      pageStackState.finishPage()

      rule.waitUntil {
         rule.waitForIdle()
         rule.mainClock.advanceTimeByFrame()

         val expectedPageAAnimations
               = expectedTransitionSpec.exitingTargetPageElementAnimations
         val expectedPageBAnimations
               = expectedTransitionSpec.exitingCurrentPageElementAnimations

         pageATransitionAnimations === expectedPageAAnimations &&
         pageBTransitionAnimations === expectedPageBAnimations
      }
   }

   @Test
   fun selectPageTransitionSpec_default() {
      testPageTransitionSpec(
         expectedTransitionSpec = defaultPageTransitionSpec,
         pageATransitions = {},
         pageBTransitions = {}
      )
   }

   @Test
   fun selectPageTransitionSpec_currentSide() {
      val expectedTransitionSpec = pageTransitionSpec(
         enter = {
            currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
         },
         exit = {
            currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
         }
      )

      testPageTransitionSpec(
         expectedTransitionSpec,
         pageATransitions = {
            transitionTo(PageB::class, expectedTransitionSpec)
         },
         pageBTransitions = {}
      )
   }

   @Test
   fun selectPageTransitionSpec_targetSide() {
      val expectedTransitionSpec = pageTransitionSpec(
         enter = {
            currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
         },
         exit = {
            currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
         }
      )

      testPageTransitionSpec(
         expectedTransitionSpec,
         pageATransitions = {},
         pageBTransitions = {
            transitionFrom(PageA::class, expectedTransitionSpec)
         }
      )
   }

   @Test
   fun selectPageTransitionSpec_both() {
      val expectedTransitionSpec = pageTransitionSpec(
         enter = {
            currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
         },
         exit = {
            currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
         }
      )

      testPageTransitionSpec(
         expectedTransitionSpec,
         pageATransitions = {
            transitionTo(PageB::class, expectedTransitionSpec)
         },
         pageBTransitions = {
            val unexpectedTransitionSpec = pageTransitionSpec(
               enter = {
                  currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
               },
               exit = {
                  currentPageElement(PageLayoutInfo.LayoutId()) { Modifier }
               }
            )

            transitionFrom(PageA::class, unexpectedTransitionSpec)
         }
      )
   }
}
