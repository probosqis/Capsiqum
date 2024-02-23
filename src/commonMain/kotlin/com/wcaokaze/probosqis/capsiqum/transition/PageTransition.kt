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

private typealias PageComposableArguments<S>
      = Triple<S, MutablePageLayoutInfo, PageTransitionElementAnimSet>

@Stable
internal abstract class PageTransitionState<S> {
   private val layoutInfoMap = mutableMapOf<Any, PageLayoutInfoImpl>()

   private val NULL = Any()
   private var currentState: Any? = NULL
   private var targetState:  Any? = NULL
   private var isTargetFirstComposition = false

   var visiblePageStates by mutableStateOf(emptyList<PageComposableArguments<S>>())

   private val emptyPageTransitionAnimSet: PageTransitionElementAnimSet
         = persistentMapOf()

   protected abstract fun getKey(state: S): Any

   /**
    * 2つの状態の画面上での階層の深さを比較する。
    *
    * レシーバーインスタンスがotherより前面に表示されるとき負の値、
    * レシーバーインスタンスがotherより奥に表示されるとき正の値、
    * レシーバーインスタンスがotherと同一の場合0を返却する。
    * これは画面遷移をStack構造と考えるとき、一般的に
    * `this.size.compareTo(other.size)` で実装できる。
    */
   protected abstract infix fun S.compareTransitionTo(other: S): Int

   protected abstract fun getEnteringTransitionSpec(
      currentState: S,
      targetState:  S
   ): PageTransitionSpec

   protected abstract fun getExitingTransitionSpec(
      currentState: S,
      targetState:  S
   ): PageTransitionSpec

   @Composable
   fun updateTransition(targetState: S): Transition<PageLayoutInfo> {
      /*
       * targetStateが変化した際、直前に表示されていたstateを
       * 表示したまま一度裏で遷移先のstateをコンポーズし、PageLayoutInfoが
       * 収集できてから遷移先のstateを表にして遷移アニメーションを開始する。
       * そのため、targetStateが変化した直後のリコンポジションではTransitionには
       * まだ遷移後のstateは渡さず、PageLayoutInfoが収集できてからTransitionに
       * 遷移後のstateを渡すことになる。
       *
       * | current | target | transitionTarget | isTargetFirstComposition | visiblePages |
       * |---------|--------|------------------|--------------------------|--------------|
       * |       0 |      0 |                0 | false                    | [0]          |
       * |       0 |      1 |                0 | true                     | [1, 0]       |
       * |       0 |      1 |                1 | false                    | [0, 1]       |
       * |       1 |      1 |                1 | false                    | [1]          |
       *
       */

      val isTargetFirstComposition = getLayoutInfo(targetState).isEmpty()

      val transitionTargetState = if (isTargetFirstComposition) {
         val prevTargetState = this.targetState
         if (prevTargetState !== NULL) {
            @Suppress("UNCHECKED_CAST")
            prevTargetState as S
         } else {
            targetState
         }
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
         getLayoutInfo(it)
      }
   }

   @Composable
   internal fun createChildTransitionForPreview(
      baseTransition: Transition<S>
   ): Transition<PageLayoutInfo> {
      val targetState = baseTransition.targetState
      val isTargetFirstComposition = getLayoutInfo(targetState).isEmpty()

      @OptIn(ExperimentalTransitionApi::class)
      val transition = baseTransition.createChildTransition(
         label = "PageStackContentTransition"
      ) {
         if (it != targetState) {
            it
         } else {
            if (isTargetFirstComposition) {
               val prevTargetState = this.targetState
               if (prevTargetState !== NULL) {
                  @Suppress("UNCHECKED_CAST")
                  prevTargetState as S
               } else {
                  targetState
               }
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
         getLayoutInfo(it)
      }
   }

   private fun getLayoutInfo(state: S) = getLayoutInfo(getKey(state))

   @JvmName("getLayoutInfoByKey")
   private fun getLayoutInfo(key: Any): PageLayoutInfoImpl {
      return layoutInfoMap.getOrPut(key) {
         PageLayoutInfoImpl(key)
      }
   }

   private fun updateVisiblePageStates(
      currentState: S,
      targetState:  S,
      isTargetFirstComposition: Boolean
   ) {
      val ord = currentState compareTransitionTo targetState

      visiblePageStates = when {
         ord < 0 -> {
            val transitionSpec = getEnteringTransitionSpec(currentState, targetState)

            if (isTargetFirstComposition) {
               listOf(
                  Triple(targetState,  getLayoutInfo(targetState),  emptyPageTransitionAnimSet),
                  Triple(currentState, getLayoutInfo(currentState), emptyPageTransitionAnimSet)
               )
            } else {
               listOf(
                  Triple(currentState, getLayoutInfo(currentState), transitionSpec.enteringCurrentPageElementAnimations),
                  Triple(targetState,  getLayoutInfo(targetState),  transitionSpec.enteringTargetPageElementAnimations)
               )
            }
         }
         ord > 0 -> {
            val transitionSpec = getExitingTransitionSpec(currentState, targetState)

            if (isTargetFirstComposition) {
               listOf(
                  Triple(targetState,  getLayoutInfo(targetState),  emptyPageTransitionAnimSet),
                  Triple(currentState, getLayoutInfo(currentState), emptyPageTransitionAnimSet)
               )
            } else {
               listOf(
                  Triple(targetState,  getLayoutInfo(targetState),  transitionSpec.exitingTargetPageElementAnimations),
                  Triple(currentState, getLayoutInfo(currentState), transitionSpec.exitingCurrentPageElementAnimations)
               )
            }
         }
         else -> {
            listOf(
               Triple(targetState, getLayoutInfo(targetState), emptyPageTransitionAnimSet)
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
) : PageTransitionState<PageStack>() {
   override fun getKey(state: PageStack) = state.head.id

   override fun PageStack.compareTransitionTo(other: PageStack): Int {
      return this.indexedHead.index.compareTo(other.indexedHead.index)
   }

   override fun getEnteringTransitionSpec(
      currentState: PageStack,
      targetState: PageStack
   ): PageTransitionSpec {
      val currentPage = currentState.head.page
      val targetPage  = targetState .head.page

      val currentPageComposable = pageComposableSwitcher[currentPage] ?: TODO()
      val targetPageComposable  = pageComposableSwitcher[targetPage ] ?: TODO()

      return currentPageComposable.pageTransitionSet.getTransitionTo  (targetPage ::class)
         ?:  targetPageComposable .pageTransitionSet.getTransitionFrom(currentPage::class)
         ?:  defaultPageTransitionSpec
   }

   override fun getExitingTransitionSpec(
      currentState: PageStack,
      targetState: PageStack
   ): PageTransitionSpec {
      val currentPage = currentState.head.page
      val targetPage  = targetState .head.page

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
   ) { pageStack ->
      PageTransitionContent(pageStack.head, pageStackState,
         pageComposableSwitcher, pageStateStore, windowInsets)
   }
}

@Composable
internal fun <S> PageTransition(
   transitionState: PageTransitionState<S>,
   targetState: S,
   content: @Composable (S) -> Unit
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
   baseTransition: Transition<PageStack>,
   windowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)
) {
   val transitionState = remember(pageComposableSwitcher) {
      PageTransitionStateImpl(pageComposableSwitcher)
   }

   PageTransitionPreview(
      transitionState,
      baseTransition,
   ) { pageStack ->
      PageTransitionContent(pageStack.head, pageStackState,
         pageComposableSwitcher, pageStateStore, windowInsets)
   }
}

@Composable
internal fun <S> PageTransitionPreview(
   transitionState: PageTransitionState<S>,
   baseTransition: Transition<S>,
   content: @Composable (S) -> Unit
) {
   PageTransition(
      transitionState,
      transition = transitionState.createChildTransitionForPreview(baseTransition),
      content
   )
}

@Composable
private fun <S> PageTransition(
   transitionState: PageTransitionState<S>,
   transition: Transition<PageLayoutInfo>,
   content: @Composable (S) -> Unit
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
