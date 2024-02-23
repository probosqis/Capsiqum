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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
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
import com.wcaokaze.probosqis.capsiqum.Page
import com.wcaokaze.probosqis.capsiqum.PageComposableSwitcher
import com.wcaokaze.probosqis.capsiqum.PageContent
import com.wcaokaze.probosqis.capsiqum.PageFooter
import com.wcaokaze.probosqis.capsiqum.PageStack
import com.wcaokaze.probosqis.capsiqum.PageStackState
import com.wcaokaze.probosqis.capsiqum.PageStateStore
import com.wcaokaze.probosqis.capsiqum.pageFooterHeight
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
internal abstract class PageTransitionState {
   private val layoutInfoMap = mutableMapOf<Any, PageLayoutInfoImpl>()

   private var currentState: IndexedValue<PageStack.SavedPageState>? = null
   private var targetState:  IndexedValue<PageStack.SavedPageState>? = null
   private var isTargetFirstComposition = false

   var visiblePageStates by mutableStateOf(emptyList<PageComposableArguments>())

   private val emptyPageTransitionAnimSet: PageTransitionElementAnimSet
         = persistentMapOf()

   protected abstract fun getKey(state: PageStack.SavedPageState): Any

   /**
    * 2つの状態の画面上での階層の深さを比較する。
    *
    * レシーバーインスタンスがotherより前面に表示されるとき負の値、
    * レシーバーインスタンスがotherより奥に表示されるとき正の値、
    * レシーバーインスタンスがotherと同一の場合0を返却する。
    * これは画面遷移をStack構造と考えるとき、一般的に
    * `this.size.compareTo(other.size)` で実装できる。
    */
   protected abstract infix fun
         IndexedValue<PageStack.SavedPageState>.compareTransitionTo(
            other: IndexedValue<PageStack.SavedPageState>
         ): Int

   protected abstract fun getEnteringTransitionSpec(
      currentPage: Page,
      targetPage: Page
   ): PageTransitionSpec

   protected abstract fun getExitingTransitionSpec(
      currentPage: Page,
      targetPage: Page
   ): PageTransitionSpec

   @Composable
   fun updateTransition(pageStack: PageStack): Transition<PageLayoutInfo> {
      /*
       * pageStack.headが変化した際、直前に表示されていたPageを
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

      val targetState = pageStack.indexedHead
      val isTargetFirstComposition = getLayoutInfo(targetState.value).isEmpty()

      val transitionTargetState = if (isTargetFirstComposition) {
         this.targetState ?: targetState
      } else {
         targetState
      }

      val transition = updateTransition(
         transitionTargetState,
         label = "PageStackContentTransition"
      )

      val currentState = transition.currentState

      if (currentState             != this.currentState ||
          targetState              != this.targetState  ||
          isTargetFirstComposition != this.isTargetFirstComposition)
      {
         updateVisiblePageStates(currentState, targetState, isTargetFirstComposition)

         this.currentState = currentState
         this.targetState  = targetState
         this.isTargetFirstComposition = isTargetFirstComposition
      }

      @OptIn(ExperimentalTransitionApi::class)
      return transition.createChildTransition(label = "PageTransition") {
         getLayoutInfo(it.value)
      }
   }

   @Composable
   internal fun createChildTransitionForPreview(
      baseTransition: Transition<IndexedValue<PageStack.SavedPageState>>
   ): Transition<PageLayoutInfo> {
      val targetState = baseTransition.targetState
      val isTargetFirstComposition = getLayoutInfo(targetState.value).isEmpty()

      @OptIn(ExperimentalTransitionApi::class)
      val transition = baseTransition.createChildTransition(
         label = "PageStackContentTransition"
      ) {
         if (it != targetState) {
            it
         } else {
            if (isTargetFirstComposition) {
               this.targetState ?: targetState
            } else {
               targetState
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

      val currentState = transition.currentState

      if (currentState             != this.currentState ||
          targetState              != this.targetState  ||
          isTargetFirstComposition != this.isTargetFirstComposition)
      {
         updateVisiblePageStates(currentState, targetState, isTargetFirstComposition)

         this.currentState = currentState
         this.targetState  = targetState
         this.isTargetFirstComposition = isTargetFirstComposition
      }

      @OptIn(ExperimentalTransitionApi::class)
      return transition.createChildTransition(label = "PageTransition") {
         getLayoutInfo(it.value)
      }
   }

   private fun getLayoutInfo(state: PageStack.SavedPageState)
         = getLayoutInfo(getKey(state))

   private fun getLayoutInfo(key: Any): PageLayoutInfoImpl {
      return layoutInfoMap.getOrPut(key) {
         PageLayoutInfoImpl(key)
      }
   }

   private fun updateVisiblePageStates(
      currentState: IndexedValue<PageStack.SavedPageState>,
      targetState:  IndexedValue<PageStack.SavedPageState>,
      isTargetFirstComposition: Boolean
   ) {
      val (_, currentPage) = currentState
      val (_, targetPage ) = targetState

      val ord = currentState compareTransitionTo targetState

      visiblePageStates = when {
         ord < 0 -> {
            val transitionSpec = getEnteringTransitionSpec(currentPage.page, targetPage.page)

            if (isTargetFirstComposition) {
               listOf(
                  Triple(targetPage,  getLayoutInfo(targetPage),  emptyPageTransitionAnimSet),
                  Triple(currentPage, getLayoutInfo(currentPage), emptyPageTransitionAnimSet)
               )
            } else {
               listOf(
                  Triple(currentPage, getLayoutInfo(currentPage), transitionSpec.enteringCurrentPageElementAnimations),
                  Triple(targetPage,  getLayoutInfo(targetPage),  transitionSpec.enteringTargetPageElementAnimations)
               )
            }
         }
         ord > 0 -> {
            val transitionSpec = getExitingTransitionSpec(currentPage.page, targetPage.page)

            if (isTargetFirstComposition) {
               listOf(
                  Triple(targetPage,  getLayoutInfo(targetPage),  emptyPageTransitionAnimSet),
                  Triple(currentPage, getLayoutInfo(currentPage), emptyPageTransitionAnimSet)
               )
            } else {
               listOf(
                  Triple(targetPage,  getLayoutInfo(targetPage),  transitionSpec.exitingTargetPageElementAnimations),
                  Triple(currentPage, getLayoutInfo(currentPage), transitionSpec.exitingCurrentPageElementAnimations)
               )
            }
         }
         else -> {
            listOf(
               Triple(targetPage, getLayoutInfo(targetPage), emptyPageTransitionAnimSet)
            )
         }
      }

      val iter = layoutInfoMap.keys.iterator()
      while (iter.hasNext()) {
         val key = iter.next()

         if (visiblePageStates.none { getKey(it.first) == key }) {
            iter.remove()
         }
      }
   }
}

@Stable
internal class PageTransitionStateImpl(
   private val pageComposableSwitcher: PageComposableSwitcher
) : PageTransitionState() {
   override fun getKey(state: PageStack.SavedPageState) = state.id

   override fun IndexedValue<PageStack.SavedPageState>.compareTransitionTo(
      other: IndexedValue<PageStack.SavedPageState>
   ): Int {
      return this.index.compareTo(other.index)
   }

   override fun getEnteringTransitionSpec(
      currentPage: Page,
      targetPage: Page
   ): PageTransitionSpec {
      val currentPageComposable = pageComposableSwitcher[currentPage] ?: TODO()
      val targetPageComposable  = pageComposableSwitcher[targetPage ] ?: TODO()

      return currentPageComposable.pageTransitionSet.getTransitionTo  (targetPage ::class)
         ?:  targetPageComposable .pageTransitionSet.getTransitionFrom(currentPage::class)
         ?:  defaultPageTransitionSpec
   }

   override fun getExitingTransitionSpec(
      currentPage: Page,
      targetPage: Page
   ): PageTransitionSpec {
      val currentPageComposable = pageComposableSwitcher[currentPage] ?: TODO()
      val targetPageComposable  = pageComposableSwitcher[targetPage ] ?: TODO()

      return targetPageComposable .pageTransitionSet.getTransitionTo  (currentPage::class)
         ?:  currentPageComposable.pageTransitionSet.getTransitionFrom(targetPage ::class)
         ?:  defaultPageTransitionSpec
   }
}

@Composable
internal fun PageTransition(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
) {
   val transitionState = remember(pageComposableSwitcher) {
      PageTransitionStateImpl(pageComposableSwitcher)
   }

   PageTransition(
      transitionState,
      pageStackState.pageStack
   ) { savedPageState ->
      PageTransitionContent(savedPageState, pageStackState,
         pageComposableSwitcher, pageStateStore, windowInsets)
   }
}

@Composable
internal fun PageTransition(
   transitionState: PageTransitionState,
   targetState: PageStack,
   content: @Composable (PageStack.SavedPageState) -> Unit
) {
   PageTransition(
      transitionState,
      transition = transitionState.updateTransition(targetState),
      content
   )
}

@Composable
internal fun PageTransitionPreview(
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   baseTransition: Transition<IndexedValue<PageStack.SavedPageState>>,
   windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
) {
   val transitionState = remember(pageComposableSwitcher) {
      PageTransitionStateImpl(pageComposableSwitcher)
   }

   PageTransitionPreview(
      transitionState,
      baseTransition,
   ) { savedPageState ->
      PageTransitionContent(savedPageState, pageStackState,
         pageComposableSwitcher, pageStateStore, windowInsets)
   }
}

@Composable
internal fun PageTransitionPreview(
   transitionState: PageTransitionState,
   baseTransition: Transition<IndexedValue<PageStack.SavedPageState>>,
   content: @Composable (PageStack.SavedPageState) -> Unit
) {
   PageTransition(
      transitionState,
      transition = transitionState.createChildTransitionForPreview(baseTransition),
      content
   )
}

@Composable
private fun PageTransition(
   transitionState: PageTransitionState,
   transition: Transition<PageLayoutInfo>,
   content: @Composable (PageStack.SavedPageState) -> Unit
) {
   Box {
      val backgroundColor = MaterialTheme.colorScheme
         .surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)

      for ((savedPageState, layoutInfo, transitionAnimations)
         in transitionState.visiblePageStates)
      {
         key(layoutInfo.key) {
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

                  content(savedPageState)
               }
            }
         }
      }
   }
}

@Composable
private fun PageTransitionContent(
   savedPageState: PageStack.SavedPageState,
   pageStackState: PageStackState,
   pageComposableSwitcher: PageComposableSwitcher,
   pageStateStore: PageStateStore,
   windowInsets: WindowInsets
) {
   Box {
      val page = savedPageState.page
      val pageComposable = pageComposableSwitcher[page] ?: TODO()

      val pageState = remember(savedPageState.id) {
         pageStateStore.get(savedPageState)
      }

      val footerComposable = pageComposable.footerComposable

      Box(
         modifier = Modifier
            .transitionElement(PageLayoutIds.content)
      ) {
         val contentWindowInsets = if (footerComposable != null) {
            windowInsets
               .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
               .add(WindowInsets(bottom = pageFooterHeight))
         } else {
            windowInsets
         }

         PageContent(
            pageComposable.contentComposable,
            page,
            pageState,
            pageStackState,
            contentWindowInsets
         )
      }

      if (footerComposable != null) {
         Box(
            Modifier
               .align(Alignment.BottomCenter)
               .transitionElement(PageLayoutIds.footer)
         ) {
            PageFooter(
               footerComposable,
               page,
               pageState,
               pageStackState,
               windowInsets.only(
                  WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
            )
         }
      }
   }
}
