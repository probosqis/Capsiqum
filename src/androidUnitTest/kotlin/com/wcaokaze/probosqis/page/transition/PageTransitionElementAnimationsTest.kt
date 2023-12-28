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

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import com.wcaokaze.probosqis.page.MultiColumnPageStackBoardState
import com.wcaokaze.probosqis.page.Page
import com.wcaokaze.probosqis.page.PageComposable
import com.wcaokaze.probosqis.page.PageComposableSwitcher
import com.wcaokaze.probosqis.page.PageStack
import com.wcaokaze.probosqis.page.PageStackBoard
import com.wcaokaze.probosqis.page.PageStackState
import com.wcaokaze.probosqis.page.PageState
import com.wcaokaze.probosqis.page.PageStateStore
import com.wcaokaze.probosqis.page.PageTransitionSet
import com.wcaokaze.probosqis.page.pageStateFactory
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PageTransitionElementAnimationsTest {
   @get:Rule
   val rule = createComposeRule()

   // ==== Test Utils ==========================================================

   private class PageA : Page()
   private class PageB : Page()
   private class PageAState : PageState()
   private class PageBState : PageState()

   private fun createPageStackState(
      initialPage: Page,
      vararg initialPages: Page
   ): PageStackState {
      val pageStackCache = WritableCache(
         PageStack(
            PageStack.Id(0L),
            PageStack.SavedPageState(
               PageStack.PageId(0L),
               initialPage
            )
         )
      )

      for ((i, p) in initialPages.withIndex()) {
         pageStackCache.value = pageStackCache.value.added(
            PageStack.SavedPageState(
               PageStack.PageId(i.toLong()),
               p
            )
         )
      }

      return PageStackState(
         PageStackBoard.PageStackId(0L),
         pageStackCache,
         pageStackBoardState = mockk<MultiColumnPageStackBoardState>()
      )
   }

   private fun PageTransitionSpec.Builder.hideBackground() {
      currentPageElement(PageLayoutIds.background) {
         val alpha by transition.animateFloat(
            transitionSpec = { snap(500) },
            label = "background"
         ) {
            if (it.isCurrentPage) { 1.0f } else { 0.0f }
         }

         Modifier.alpha(alpha)
      }

      targetPageElement(PageLayoutIds.background) {
         val alpha by transition.animateFloat(
            transitionSpec = { snap(500) },
            label = "background"
         ) {
            if (it.isTargetPage) { 1.0f } else { 0.0f }
         }

         Modifier.alpha(alpha)
      }
   }

   private fun verifyEnterAnim(
      fileNamePrefix: String,
      pageAComposable: @Composable () -> Unit,
      pageBComposable: @Composable () -> Unit,
      enterTransitions: PageTransitionSpec.Builder.() -> Unit = {},
      exitTransitions:  PageTransitionSpec.Builder.() -> Unit = {}
   ) {
      verifyTransitionAnim(fileNamePrefix, pageAComposable, pageBComposable,
         enterTransitions, exitTransitions, verifyEnterAnim = true)
   }

   private fun verifyExitAnim(
      fileNamePrefix: String,
      pageAComposable: @Composable () -> Unit,
      pageBComposable: @Composable () -> Unit,
      enterTransitions: PageTransitionSpec.Builder.() -> Unit = {},
      exitTransitions:  PageTransitionSpec.Builder.() -> Unit = {}
   ) {
      verifyTransitionAnim(fileNamePrefix, pageAComposable, pageBComposable,
         enterTransitions, exitTransitions, verifyEnterAnim = false)
   }

   private fun verifyTransitionAnim(
      fileNamePrefix: String,
      pageAComposable: @Composable () -> Unit,
      pageBComposable: @Composable () -> Unit,
      enterTransitions: PageTransitionSpec.Builder.() -> Unit = {},
      exitTransitions:  PageTransitionSpec.Builder.() -> Unit = {},
      verifyEnterAnim: Boolean
   ) {
      val transitions = PageTransitionSet.Builder()
         .apply {
            transitionTo<PageB>(
               enter = {
                  hideBackground()
                  enterTransitions()
               },
               exit = {
                  hideBackground()
                  exitTransitions()
               }
            )
         }
         .build()

      val pageAComposables = PageComposable(
         PageA::class,
         pageStateFactory { _, _ -> PageAState() },
         contentComposable = { _, _, _ ->
            pageAComposable()
         },
         headerComposable = { _, _, _ -> },
         footerComposable = null,
         pageTransitionSet = transitions
      )

      val pageBComposables = PageComposable(
         PageB::class,
         pageStateFactory { _, _ -> PageBState() },
         contentComposable = { _, _, _ ->
            pageBComposable()
         },
         headerComposable = { _, _, _ -> },
         footerComposable = null,
         pageTransitionSet = PageTransitionSet.Builder().build()
      )

      val pageStackState = if (verifyEnterAnim) {
         createPageStackState(PageA())
      } else {
         createPageStackState(PageA(), PageB())
      }

      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()

         val pageComposableSwitcher = remember {
            PageComposableSwitcher(
               listOf(
                  pageAComposables,
                  pageBComposables,
               )
            )
         }

         val pageStateStore = remember {
            PageStateStore(
               listOf(
                  pageAComposables.pageStateFactory,
                  pageBComposables.pageStateFactory,
               ),
               coroutineScope
            )
         }

         Box(Modifier.size(300.dp, 600.dp)) {
            PageTransition(pageStackState, pageComposableSwitcher, pageStateStore)
         }
      }

      rule.mainClock.autoAdvance = false

      if (verifyEnterAnim) {
         pageStackState.startPage(PageB())
      } else {
         pageStackState.finishPage()
      }

      rule.waitForIdle()

      repeat (20) { i ->
         rule.onRoot().captureRoboImage("test/$fileNamePrefix$i.png")
         rule.mainClock.advanceTimeBy(16L)
      }
   }

   // ==== Tests ===============================================================

   private val textAId = PageLayoutInfo.LayoutId()
   private val textBId = PageLayoutInfo.LayoutId()

   @Composable
   private fun Page(
      text: String,
      elementId: PageLayoutInfo.LayoutId,
      x: Dp = 0.dp,
      y: Dp = 0.dp,
      fontSize: TextUnit = 56.sp
   ) {
      Box(
         Modifier
            .fillMaxSize()
            .padding(start = x, top = y)
      ) {
         Text(
            text,
            fontSize = fontSize,
            modifier = Modifier
               .transitionElement(elementId)
         )
      }
   }

   @Test
   fun animateScale_currentElement() {
      verifyEnterAnim(
         "animateScale/currentElement",
         pageAComposable = { Page("A", textAId, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, fontSize = 96.sp) },
         enterTransitions = {
            currentPageElement(textAId) {
               val scale by animateScale(textAId, textBId, label = "text")
               Modifier.graphicsLayer {
                  transformOrigin = TransformOrigin(0.0f, 0.0f)
                  scaleX = scale.scaleX
                  scaleY = scale.scaleY
               }
            }
         }
      )
   }

   @Test
   fun animateScale_targetElement() {
      verifyEnterAnim(
         "animateScale/targetElement",
         pageAComposable = { Page("A", textAId, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, fontSize = 96.sp) },
         enterTransitions = {
            targetPageElement(textBId) {
               val scale by animateScale(textAId, textBId, label = "text")
               Modifier.graphicsLayer {
                  transformOrigin = TransformOrigin(0.0f, 0.0f)
                  scaleX = scale.scaleX
                  scaleY = scale.scaleY
               }
            }
         }
      )
   }

   @Test
   fun animatePosition_currentElement() {
      verifyEnterAnim(
         "animatePosition/currentElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp) },
         enterTransitions = {
            currentPageElement(textAId) {
               val pos by animatePosition(textAId, textBId, label = "text")
               Modifier.offset { pos.round() }
            }
         }
      )
   }

   @Test
   fun animatePosition_targetElement() {
      verifyEnterAnim(
         "animatePosition/targetElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp) },
         enterTransitions = {
            targetPageElement(textBId) {
               val pos by animatePosition(textAId, textBId, label = "text")
               Modifier.offset { pos.round() }
            }
         }
      )
   }

   @Test
   fun sharedElement_crossFade() {
      verifyEnterAnim(
         "sharedElement/crossFade",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.CrossFade)
         }
      )
   }

   @Test
   fun sharedElement_currentElement() {
      verifyEnterAnim(
         "sharedElement/currentElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.Current)
         }
      )
   }

   @Test
   fun sharedElement_targetElement() {
      verifyEnterAnim(
         "sharedElement/targetElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.Target)
         }
      )
   }

   @Test
   fun sharedElement_onlyOffset_crossFade() {
      verifyEnterAnim(
         "sharedElement_onlyOffset/crossFade",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.CrossFade,
               SharedElementAnimations.Offset)
         }
      )
   }

   @Test
   fun sharedElement_onlyOffset_currentElement() {
      verifyEnterAnim(
         "sharedElement_onlyOffset/currentElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.Current,
               SharedElementAnimations.Offset)
         }
      )
   }

   @Test
   fun sharedElement_onlyOffset_targetElement() {
      verifyEnterAnim(
         "sharedElement_onlyOffset/targetElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.Target,
               SharedElementAnimations.Offset)
         }
      )
   }

   @Test
   fun sharedElement_onlyScale_crossFade() {
      verifyEnterAnim(
         "sharedElement_onlyScale/crossFade",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.CrossFade,
               SharedElementAnimations.Scale)
         }
      )
   }

   @Test
   fun sharedElement_onlyScale_currentElement() {
      verifyEnterAnim(
         "sharedElement_onlyScale/currentElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.Current,
               SharedElementAnimations.Scale)
         }
      )
   }

   @Test
   fun sharedElement_onlyScale_targetElement() {
      verifyEnterAnim(
         "sharedElement_onlyScale/targetElement",
         pageAComposable = { Page("A", textAId, x =   8.dp, y =   8.dp, fontSize = 56.sp) },
         pageBComposable = { Page("B", textBId, x = 100.dp, y = 300.dp, fontSize = 96.sp) },
         enterTransitions = {
            sharedElement(textAId, textBId, label = "text",
               SharedElementAnimatorElement.Target,
               SharedElementAnimations.Scale)
         }
      )
   }
}
