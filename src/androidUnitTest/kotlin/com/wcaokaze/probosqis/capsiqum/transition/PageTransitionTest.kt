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

package com.wcaokaze.probosqis.capsiqum.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
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

   private class PageTransitionStateImpl<S>(
      private val transitionSpec: PageTransitionSpec
   ) : PageTransitionState<S>()
      where S : Comparable<S>
   {
      override fun S.compareTransitionTo(other: S) = this.compareTo(other)
      override fun getKey(state: S) = state

      override fun getTransitionSpec(frontState: S, backState: S)
            = transitionSpec
   }

   private val emptyPageTransitionSpec = pageTransitionSpec(enter = {}, exit = {})

   // ==== Tests ===============================================================

   @Test
   fun pageLayoutInfo_getViaCompositionLocal() {
      var pageALayoutInfo: PageLayoutInfo? = null
      var pageBLayoutInfo: PageLayoutInfo? = null

      val transitionState = PageTransitionStateImpl<Boolean>(emptyPageTransitionSpec)

      var targetState by mutableStateOf(false)

      rule.setContent {
         PageTransition(transitionState, targetState) {
            if (it) {
               pageBLayoutInfo = LocalPageLayoutInfo.current
            } else {
               pageALayoutInfo = LocalPageLayoutInfo.current
            }
         }
      }

      rule.runOnIdle {
         assertNotNull(pageALayoutInfo)
         assertNull   (pageBLayoutInfo)
      }

      targetState = true

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

   @Stable
   private class RecompositionCounterState {
      var count by mutableStateOf(0)
   }

   @Composable
   private fun RecompositionCounter(state: RecompositionCounterState) {
      SideEffect {
         state.count++
      }
   }

   @Test
   fun pageTransition() {
      val page0RecompositionCounter = RecompositionCounterState()
      val page1RecompositionCounter = RecompositionCounterState()
      val page2RecompositionCounter = RecompositionCounterState()

      val transitionState = PageTransitionStateImpl<Int>(emptyPageTransitionSpec)

      var targetState by mutableIntStateOf(1)

      rule.setContent {
         PageTransition(transitionState, targetState) {
            // アニメーション等に影響されず「コンポーズされた回数」を確実に
            // 数えるため、スキップされるComposable内で数える
            when (it) {
               0 -> RecompositionCounter(page0RecompositionCounter)
               1 -> RecompositionCounter(page1RecompositionCounter)
               2 -> RecompositionCounter(page2RecompositionCounter)
            }
         }
      }

      rule.runOnIdle {
         assertEquals(0, page0RecompositionCounter.count)
         assertEquals(1, page1RecompositionCounter.count)
         assertEquals(0, page2RecompositionCounter.count)
      }

      targetState = 0

      rule.runOnIdle {
         assertEquals(1, page0RecompositionCounter.count)
         assertEquals(1, page1RecompositionCounter.count)
         assertEquals(0, page2RecompositionCounter.count)
      }

      targetState = 2

      rule.runOnIdle {
         assertEquals(1, page0RecompositionCounter.count)
         assertEquals(1, page1RecompositionCounter.count)
         assertEquals(1, page2RecompositionCounter.count)
      }
   }

   @Test
   fun onlyForefrontComposableIsCalled() {
      var pageAComposed = false
      var pageBComposed = false

      val transitionState = PageTransitionStateImpl<Boolean>(emptyPageTransitionSpec)

      var targetState by mutableStateOf(false)

      rule.setContent {
         PageTransition(transitionState, targetState) {
            if (it) {
               DisposableEffect(Unit) {
                  pageBComposed = true
                  onDispose {
                     pageBComposed = false
                  }
               }
            } else {
               DisposableEffect(Unit) {
                  pageAComposed = true
                  onDispose {
                     pageAComposed = false
                  }
               }
            }
         }
      }

      rule.runOnIdle {
         assertTrue (pageAComposed)
         assertFalse(pageBComposed)
      }

      targetState = true

      rule.runOnIdle {
         assertFalse(pageAComposed)
         assertTrue (pageBComposed)
      }

      targetState = false

      rule.runOnIdle {
         assertTrue (pageAComposed)
         assertFalse(pageBComposed)
      }
   }

   @Test
   fun composeBothDuringTransition() {
      var pageAComposed = false
      var pageBComposed = false

      val transitionState = PageTransitionStateImpl<Boolean>(emptyPageTransitionSpec)

      var targetState by mutableStateOf(false)

      rule.setContent {
         PageTransition(transitionState, targetState) {
            if (it) {
               DisposableEffect(Unit) {
                  pageBComposed = true
                  onDispose {
                     pageBComposed = false
                  }
               }
            } else {
               DisposableEffect(Unit) {
                  pageAComposed = true
                  onDispose {
                     pageAComposed = false
                  }
               }
            }
         }
      }

      rule.waitForIdle()

      assertTrue(pageAComposed)
      assertFalse(pageBComposed)

      rule.mainClock.autoAdvance = false
      targetState = true

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

      val transitionState = PageTransitionStateImpl<Boolean>(emptyPageTransitionSpec)

      var targetState by mutableStateOf(false)

      rule.setContent {
         PageTransition(transitionState, targetState) {
            if (it) {
               pageBPageLayoutInfo = LocalPageLayoutInfo.current
               DisposableEffect(Unit) {
                  pageBComposed = true
                  onDispose {
                     pageBComposed = false
                  }
               }
            } else {
               pageAPageLayoutInfo = LocalPageLayoutInfo.current
               DisposableEffect(Unit) {
                  pageAComposed = true
                  onDispose {
                     pageAComposed = false
                  }
               }
            }
         }
      }

      rule.waitForIdle()

      val prevALayoutInfo = assertNotNull(pageAPageLayoutInfo)

      rule.mainClock.autoAdvance = false
      targetState = true

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
      targetState = false

      rule.waitUntil {
         rule.waitForIdle()
         rule.mainClock.advanceTimeByFrame()

         pageAComposed && pageBComposed
      }

      assertSame(prevBLayoutInfo, pageBPageLayoutInfo)
      assertNotSame(prevALayoutInfo, pageAPageLayoutInfo)
   }
}
