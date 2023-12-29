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

package com.wcaokaze.probosqis.capsiqum.transition

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.wcaokaze.probosqis.capsiqum.PageComposable
import com.wcaokaze.probosqis.capsiqum.pageComposable
import kotlinx.collections.immutable.toImmutableMap

/**
 * @see PageTransitionSpec
 */
inline fun pageTransitionSpec(
   enter: PageTransitionSpec.Builder.() -> Unit,
   exit:  PageTransitionSpec.Builder.() -> Unit
): PageTransitionSpec {
   val enterTransitionBuilder = PageTransitionSpec.Builder()
   val exitTransitionBuilder  = PageTransitionSpec.Builder()
   enterTransitionBuilder.enter()
   exitTransitionBuilder.exit()
   return PageTransitionSpec(enterTransitionBuilder, exitTransitionBuilder)
}

/**
 * ページ遷移アニメーション定義。
 * Probosqisのページ遷移アニメーションは大きく4つの要素で構成される。
 *
 * ## [Modifier.transitionElement]
 * [PageComposable.contentComposable]内のComposableにIDを付与する。
 * ```kotlin
 * object AccountPageLayoutIds : PageLayoutIds() {
 *    val accountIcon = PageLayoutInfo.LayoutId()
 *    val accountName = PageLayoutInfo.LayoutId()
 * }
 *
 * @Composable
 * fun AccountPage(state: AccountPageState) {
 *    Row {
 *       Image(
 *          state.account.icon,
 *          Modifier
 *             .transitionElement(AccountPageLayoutIds.accountIcon)
 *       )
 *
 *       Text(
 *          state.account.name,
 *          Modifier
 *             .transitionElement(AccountPageLayoutIds.accountName)
 *       )
 *    }
 * }
 * ```
 *
 * ## [PageLayoutInfo]
 * [PageComposable.contentComposable]内のComposableの位置やサイズをまとめたもの。
 * [Modifier.transitionElement]が付与されたComposableはその位置とサイズが
 * 自動的にこのインスタンスに収集される。
 *
 * ## [PageTransitionElementAnim]
 * アニメーションModifierを生成する関数。
 *
 * ラムダ式内では[transition][PageTransitionElementAnimScope.transition]に
 * アクセスでき、ページ遷移の進捗状況や遷移元/遷移先のページの[PageLayoutInfo]を
 * 取得可能。
 * ```kotlin
 * CurrentPageTransitionElementAnim {
 *    val animatedAlpha by transition.animateAlpha {
 *       if (it.isTargetPage) { 1.0f } else { 0.0f }
 *    }
 *    Modifier.alpha(animatedAlpha)
 * }
 * ```
 *
 * ## PageTransitionSpec
 * 特定のPageからPageへの遷移中の各Composableの[PageTransitionElementAnim]を
 * まとめたもの。
 *
 * PageAの上にPageBが起動された場合、PageAの各Composableに
 * [enteringCurrentPageElementAnimations]が適応され、PageBの各Composableに
 * [enteringTargetPageElementAnimations]が適応される。
 * PageBが閉じられ、PageAに戻るとき、PageBの各Composableに
 * [exitingCurrentPageElementAnimations]が適応され、PageAの各Composableに
 * [exitingTargetPageElementAnimations]が適応される。
 *
 * PageTransitionSpec自体は[pageComposable]でPageのComposableと同時に
 * 定義することができる。
 * ```kotlin
 * pageComposable<PostPage, PostPageState>(
 *    pageStateFactory { page, stateSaver -> PostPageState(page, stateSaver) },
 *    content = { page, pageState, pageStackState ->
 *       PostPage(page, pageState, pageStackState)
 *    },
 *    header = { page, pageState, pageStackState ->
 *       PostPageHeader(page, pageState, pageStackState)
 *    },
 *    footer = { page, pageState, pageStackState ->
 *       PostPageFooter(page, pageState, pageStackState)
 *    },
 *    pageTransitions = {
 *       transitionTo<AccountPage>(
 *          enter = {
 *             targetPageElement(AccountPageIds.background) {
 *                val animatedAlpha by transition.animateFloat(label = "background") {
 *                   if (it.isTargetPage) { 1.0f } else { 0.0f }
 *                }
 *                Modifier.alpha(animatedAlpha)
 *             }
 *             sharedElement(
 *                PostPageLayoutIds.accountIcon,
 *                AccountPageIds.accountIcon,
 *                label = "accountIcon"
 *             )
 *             sharedElement(
 *                PostPageLayoutIds.accountName,
 *                AccountPageIds.accountName,
 *                label = "accountIcon"
 *             )
 *          },
 *          exit = {
 *             targetPageElement(AccountPageIds.background) {
 *                val animatedAlpha by transition.animateFloat(label = "background") {
 *                   if (it.isCurrentPage) { 1.0f } else { 0.0f }
 *                }
 *                Modifier.alpha(animatedAlpha)
 *             }
 *             sharedElement(
 *                AccountPageIds.accountIcon,
 *                PostPageLayoutIds.accountIcon,
 *                label = "accountIcon"
 *             )
 *             sharedElement(
 *                AccountPageIds.accountName,
 *                PostPageLayoutIds.accountName,
 *                label = "accountIcon"
 *             )
 *          }
 *       )
 *    }
 * )
 * ```
 */
@Stable
class PageTransitionSpec(
   val enteringCurrentPageElementAnimations: CurrentPageTransitionElementAnimSet,
   val enteringTargetPageElementAnimations: TargetPageTransitionElementAnimSet,
   val exitingCurrentPageElementAnimations: CurrentPageTransitionElementAnimSet,
   val exitingTargetPageElementAnimations: TargetPageTransitionElementAnimSet
) {
   override fun equals(other: Any?): Boolean {
      if (other !is PageTransitionSpec) { return false }
      if (enteringCurrentPageElementAnimations != other.enteringCurrentPageElementAnimations) { return false }
      if (enteringTargetPageElementAnimations  != other.enteringTargetPageElementAnimations ) { return false }
      if (exitingCurrentPageElementAnimations  != other.exitingCurrentPageElementAnimations ) { return false }
      if (exitingTargetPageElementAnimations   != other.exitingTargetPageElementAnimations  ) { return false }
      return true
   }

   override fun hashCode(): Int {
      var h = 1
      h = h * 31 + enteringCurrentPageElementAnimations.hashCode()
      h = h * 31 + enteringTargetPageElementAnimations .hashCode()
      h = h * 31 + exitingCurrentPageElementAnimations .hashCode()
      h = h * 31 + exitingTargetPageElementAnimations  .hashCode()
      return h
   }

   constructor(enter: Builder, exit: Builder) : this(
      enter.currentPageAnimations.toImmutableMap(),
      enter.targetPageAnimations .toImmutableMap(),
      exit .currentPageAnimations.toImmutableMap(),
      exit .targetPageAnimations .toImmutableMap()
   )

   class Builder {
      internal val currentPageAnimations
            = mutableMapOf<PageLayoutInfo.LayoutId, CurrentPageTransitionElementAnim>()
      internal val targetPageAnimations
            = mutableMapOf<PageLayoutInfo.LayoutId, TargetPageTransitionElementAnim>()

      fun currentPageElement(
         id: PageLayoutInfo.LayoutId,
         animation: CurrentPageTransitionElementAnim
      ) {
         currentPageAnimations[id] = animation
      }

      fun targetPageElement(
         id: PageLayoutInfo.LayoutId,
         animation: TargetPageTransitionElementAnim
      ) {
         targetPageAnimations[id] = animation
      }

      fun currentPageElement(
         id: PageLayoutInfo.LayoutId,
         animationModifier:
               @Composable CurrentPageTransitionElementAnimScope.() -> Modifier
      ) {
         val anim = CurrentPageTransitionElementAnim(animationModifier)
         currentPageElement(id, anim)
      }

      fun targetPageElement(
         id: PageLayoutInfo.LayoutId,
         animationModifier:
               @Composable TargetPageTransitionElementAnimScope.() -> Modifier
      ) {
         val anim = TargetPageTransitionElementAnim(animationModifier)
         targetPageElement(id, anim)
      }
   }
}
