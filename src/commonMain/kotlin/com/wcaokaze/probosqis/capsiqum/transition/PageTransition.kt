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

import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.createChildTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.wcaokaze.probosqis.capsiqum.PageComposableSwitcher
import com.wcaokaze.probosqis.capsiqum.PageContent
import com.wcaokaze.probosqis.capsiqum.PageFooter
import com.wcaokaze.probosqis.capsiqum.PageStack
import com.wcaokaze.probosqis.capsiqum.PageStackState
import com.wcaokaze.probosqis.capsiqum.PageStateStore
import kotlinx.collections.immutable.persistentMapOf

internal val LocalPageTransitionAnimations
   = compositionLocalOf<PageTransitionElementAnimSet> {
      throw IllegalStateException(
         "Attempt to get PageTransitionAnimations from outside a Page")
   }

val LocalPageLayoutInfo
   = compositionLocalOf<MutablePageLayoutInfo> {
      throw IllegalStateException(
         "Attempt to get a PageLayoutInfo from outside a Page")
   }

internal val LocalPageTransition
   = compositionLocalOf<Transition<PageLayoutInfo>> {
      throw IllegalStateException(
         "Attempt to get a PageTransition from outside a Page")
   }

internal val defaultPageTransitionSpec = pageTransitionSpec(
   enter = {
      targetPageElement(PageLayoutIds.background) {
         val alpha by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isTargetPage) { 1.0f } else { 0.0f }
         }

         val translation by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isTargetPage) {
               0.0f
            } else {
               with (LocalDensity.current) { 32.dp.toPx() }
            }
         }

         Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translation
         }
      }

      targetPageElement(PageLayoutIds.content) {
         val alpha by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isTargetPage) { 1.0f } else { 0.0f }
         }

         val translation by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isTargetPage) {
               0.0f
            } else {
               with (LocalDensity.current) { 32.dp.toPx() }
            }
         }

         Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translation
         }
      }

      targetPageElement(PageLayoutIds.footer) {
         val alpha by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isTargetPage) { 1.0f } else { 0.0f }
         }

         Modifier.graphicsLayer {
            this.alpha = alpha
         }
      }
   },
   exit = {
      currentPageElement(PageLayoutIds.background) {
         val alpha by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isCurrentPage) { 1.0f } else { 0.0f }
         }

         val translation by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isCurrentPage) {
               0.0f
            } else {
               with (LocalDensity.current) { 32.dp.toPx() }
            }
         }

         Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translation
         }
      }

      currentPageElement(PageLayoutIds.content) {
         val alpha by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isCurrentPage) { 1.0f } else { 0.0f }
         }

         val translation by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isCurrentPage) {
               0.0f
            } else {
               with (LocalDensity.current) { 32.dp.toPx() }
            }
         }

         Modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translation
         }
      }

      currentPageElement(PageLayoutIds.footer) {
         val alpha by transition.animateFloat(
            transitionSpec = { tween() }
         ) {
            if (it.isCurrentPage) { 1.0f } else { 0.0f }
         }

         Modifier.graphicsLayer {
            this.alpha = alpha
         }
      }
   }
)

private typealias PageComposableArguments
      = Triple<PageStack.SavedPageState, MutablePageLayoutInfo, PageTransitionElementAnimSet>

@Stable
internal class PageTransitionState(
   private val pageStackState: PageStackState,
   private val pageComposableSwitcher: PageComposableSwitcher
) {
   private val layoutInfoMap = mutableMapOf<PageStack.PageId, PageLayoutInfoImpl>()

   private var currentPageIndex = -1
   private var targetPageIndex  = -1
   private var isTargetFirstComposition = false
   private var targetPageState: IndexedValue<PageStack.SavedPageState>? = null

   var visiblePageStates by mutableStateOf(emptyList<PageComposableArguments>())

   private val emptyPageTransitionAnimSet: PageTransitionElementAnimSet
         = persistentMapOf()

   @Composable
   fun updateTransition(): Transition<PageLayoutInfo> {
      /*
       * pageStackState.pageStack.headが変化した際、直前に表示されていたPageを
       * 表示したまま一度裏で遷移先のPageをコンポーズし、PageLayoutInfoが
       * 収集できてから遷移先のPageを表にして遷移アニメーションを開始する。
       * そのため、pageが変化した直後のリコンポジションではTransitionには
       * まだ遷移後のPageは渡さず、PageLayoutInfoが収集できてからTransitionに
       * 遷移後のPageを渡すことになる。
       *
       * | current | target | transitionTarget | isTargetFirstComposition | visiblePages |
       * |---------|--------|------------------|--------------------------|--------------|
       * |       0 |      0 |                0 | false                    | [0]          |
       * |       0 |      1 |                0 | true                     | [1, 0]       |
       * |       0 |      1 |                1 | false                    | [0, 1]       |
       * |       1 |      1 |                1 | false                    | [1]          |
       *
       */

      val pageStack = pageStackState.pageStack
      val targetPageState = pageStack.indexedHead
      val isTargetFirstComposition = getLayoutInfo(targetPageState.value.id).isEmpty()

      val transitionTargetPageState = if (isTargetFirstComposition) {
         this.targetPageState ?: targetPageState
      } else {
         targetPageState
      }

      val transition = updateTransition(
         transitionTargetPageState,
         label = "PageStackContentTransition"
      )

      val currentPageState = transition.currentState

      this.targetPageState = transitionTargetPageState

      if (currentPageState.index   != currentPageIndex ||
          targetPageState .index   != targetPageIndex  ||
          isTargetFirstComposition != this.isTargetFirstComposition)
      {
         updateVisiblePageStates(
            currentPageState, targetPageState, isTargetFirstComposition)

         currentPageIndex = currentPageState.index
         targetPageIndex  = targetPageState .index
         this.isTargetFirstComposition = isTargetFirstComposition
      }

      @OptIn(ExperimentalTransitionApi::class)
      return transition.createChildTransition(label = "PageTransition") {
         getLayoutInfo(it.value.id)
      }
   }

   @Composable
   internal fun createChildTransitionForPreview(
      baseTransition: Transition<IndexedValue<PageStack.SavedPageState>>
   ): Transition<PageLayoutInfo> {
      val targetPageState = baseTransition.targetState
      val isTargetFirstComposition = getLayoutInfo(targetPageState.value.id).isEmpty()

      @OptIn(ExperimentalTransitionApi::class)
      val transition = baseTransition.createChildTransition(
         label = "PageStackContentTransition"
      ) {
         if (it != targetPageState) {
            it
         } else {
            if (isTargetFirstComposition) {
               this.targetPageState ?: targetPageState
            } else {
               targetPageState
            }
         }
      }

      // XXX
      // isTargetFirstCompositionのとき、まだ遷移アニメーションは再生されていない。
      // その次のリコンポジションでtransition.targetStateが変更されるものの、
      // baseTransition.targetStateはこのリコンポジションで変更されているため、
      // このリコンポジションで何もアニメーションが再生されないと
      // そのままbaseTransitionはこのリコンポジションで遷移完了となってしまう。
      transition.animateFloat(
         transitionSpec = { snap(delayMillis = 200) },
         label = "dummyAnim"
      ) {
         if (!this.isTargetFirstComposition && isTargetFirstComposition) {
            1.0f
         } else {
            0.0f
         }
      }

      val currentPageState = transition.currentState
      val transitionTargetPageState = transition.targetState

      this.targetPageState = transitionTargetPageState

      if (currentPageState.index   != currentPageIndex ||
          targetPageState .index   != targetPageIndex  ||
          isTargetFirstComposition != this.isTargetFirstComposition)
      {
         updateVisiblePageStates(
            currentPageState, targetPageState, isTargetFirstComposition)

         currentPageIndex = currentPageState.index
         targetPageIndex  = targetPageState .index
         this.isTargetFirstComposition = isTargetFirstComposition
      }

      @OptIn(ExperimentalTransitionApi::class)
      return transition.createChildTransition(label = "PageTransition") {
         getLayoutInfo(it.value.id)
      }
   }

   private fun getLayoutInfo(pageId: PageStack.PageId): PageLayoutInfoImpl {
      return layoutInfoMap.getOrPut(pageId) {
         PageLayoutInfoImpl(pageStackState.pageStackId, pageId)
      }
   }

   private fun updateVisiblePageStates(
      currentState: IndexedValue<PageStack.SavedPageState>,
      targetState:  IndexedValue<PageStack.SavedPageState>,
      isTargetFirstComposition: Boolean
   ) {
      val (currentIndex, currentPage) = currentState
      val (targetIndex,  targetPage ) = targetState

      visiblePageStates = when {
         currentIndex < targetIndex -> {
            val currentPageComposable = pageComposableSwitcher[currentPage.page] ?: TODO()
            val targetPageComposable  = pageComposableSwitcher[targetPage .page] ?: TODO()

            val transitionSpec
                  =  currentPageComposable.pageTransitionSet.getTransitionTo  (targetPage .page::class)
                  ?: targetPageComposable .pageTransitionSet.getTransitionFrom(currentPage.page::class)
                  ?: defaultPageTransitionSpec

            if (isTargetFirstComposition) {
               listOf(
                  Triple(targetPage,  getLayoutInfo(targetPage .id), emptyPageTransitionAnimSet),
                  Triple(currentPage, getLayoutInfo(currentPage.id), emptyPageTransitionAnimSet)
               )
            } else {
               listOf(
                  Triple(currentPage, getLayoutInfo(currentPage.id), transitionSpec.enteringCurrentPageElementAnimations),
                  Triple(targetPage,  getLayoutInfo(targetPage .id), transitionSpec.enteringTargetPageElementAnimations)
               )
            }
         }
         currentIndex > targetIndex -> {
            val currentPageComposable = pageComposableSwitcher[currentPage.page] ?: TODO()
            val targetPageComposable  = pageComposableSwitcher[targetPage .page] ?: TODO()

            val transitionSpec
                  =  targetPageComposable .pageTransitionSet.getTransitionTo  (currentPage.page::class)
                  ?: currentPageComposable.pageTransitionSet.getTransitionFrom(targetPage .page::class)
                  ?: defaultPageTransitionSpec

            if (isTargetFirstComposition) {
               listOf(
                  Triple(targetPage,  getLayoutInfo(targetPage .id), emptyPageTransitionAnimSet),
                  Triple(currentPage, getLayoutInfo(currentPage.id), emptyPageTransitionAnimSet)
               )
            } else {
               listOf(
                  Triple(targetPage,  getLayoutInfo(targetPage .id), transitionSpec.exitingTargetPageElementAnimations),
                  Triple(currentPage, getLayoutInfo(currentPage.id), transitionSpec.exitingCurrentPageElementAnimations)
               )
            }
         }
         else -> {
            listOf(
               Triple(targetPage, getLayoutInfo(targetPage.id), emptyPageTransitionAnimSet)
            )
         }
      }

      val iter = layoutInfoMap.keys.iterator()
      while (iter.hasNext()) {
         val pageId = iter.next()

         if (visiblePageStates.none { it.first.id == pageId }) {
            iter.remove()
         }
      }
   }
}

@Composable
internal fun PageTransition(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore
) {
   val transitionState = remember(pageStackState, pageComposableSwitcher) {
      PageTransitionState(pageStackState, pageComposableSwitcher)
   }

   PageTransition(
      transitionState,
      pageStackState,
      pageComposableSwitcher,
      pageStateStore,
      transition = transitionState.updateTransition()
   )
}

@Composable
internal fun PageTransitionPreview(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   baseTransition: Transition<IndexedValue<PageStack.SavedPageState>>
) {
   val transitionState = remember(pageStackState, pageComposableSwitcher) {
      PageTransitionState(pageStackState, pageComposableSwitcher)
   }

   PageTransition(
      transitionState,
      pageStackState,
      pageComposableSwitcher,
      pageStateStore,
      transition = transitionState.createChildTransitionForPreview(baseTransition)
   )
}

@Composable
private fun PageTransition(
   transitionState: PageTransitionState,
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   transition: Transition<PageLayoutInfo>
) {
   Box {
      val backgroundColor = MaterialTheme.colorScheme
         .surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)

      for ((savedPageState, layoutInfo, transitionAnimations)
         in transitionState.visiblePageStates)
      {
         key(savedPageState.id) {
            CompositionLocalProvider(
               LocalPageLayoutInfo provides layoutInfo,
               LocalPageTransitionAnimations provides transitionAnimations,
               LocalPageTransition provides transition,
            ) {
               Box(Modifier.transitionElement(PageLayoutIds.root)) {
                  Box(
                     Modifier
                        .transitionElement(PageLayoutIds.background)
                        .fillMaxSize()
                        .background(backgroundColor)
                  )

                  Box(Modifier.transitionElement(PageLayoutIds.content)) {
                     PageContent(
                        savedPageState,
                        pageComposableSwitcher,
                        pageStateStore,
                        pageStackState
                     )
                  }

                  Box(
                     Modifier
                        .align(Alignment.BottomCenter)
                        .transitionElement(PageLayoutIds.footer)
                  ) {
                     PageFooter(
                        savedPageState,
                        pageComposableSwitcher,
                        pageStateStore,
                        pageStackState,
                        WindowInsets(0, 0, 0, 0)
                     )
                  }
               }
            }
         }
      }
   }
}
