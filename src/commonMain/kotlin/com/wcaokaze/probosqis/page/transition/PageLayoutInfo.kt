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

import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.LayoutCoordinates
import com.wcaokaze.probosqis.page.PageComposable
import com.wcaokaze.probosqis.page.PageStack
import com.wcaokaze.probosqis.page.PageStackBoard

/**
 * [LayoutId][PageLayoutInfo.LayoutId]をまとめたもの。
 *
 * [PageのComposable][PageComposable.contentComposable]ひとつに対してこのclassを
 * 継承したobjectをひとつ用意する。
 *
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
 * @see PageTransitionSpec
 */
open class PageLayoutIds {
   private object GlobalIds {
      val root       = PageLayoutInfo.LayoutId()
      val background = PageLayoutInfo.LayoutId()
      val content    = PageLayoutInfo.LayoutId()
   }

   /**
    * Page内の最も親のComposable。
    * [background]と[content]を子に持つ。
    */
   val root = GlobalIds.root

   /**
    * Pageの背景。遷移前のPageより手前、[content]よりは奥にある。
    * 遷移アニメーション中でなければPage全体に広がっている。
    */
   val background = GlobalIds.background

   /**
    * Page本体。[PageComposable.contentComposable]の親。
    */
   val content = GlobalIds.content

   companion object : PageLayoutIds()
}

/**
 * Page内にあるComposableの位置やサイズをまとめたもの。
 * 各ComposableのModifierに[transitionElement]を付与することで
 * 自動的にこのインスタンスに位置とサイズが収集されており、
 * [get]で取得可能。
 *
 * @see PageTransitionSpec
 */
@Stable
interface PageLayoutInfo {
   @JvmInline
   value class LayoutId private constructor(
      @VisibleForTesting
      internal val id: Long
   ) {
      companion object {
         private var nextId = 0L

         operator fun invoke(): LayoutId = synchronized (this) {
            LayoutId(nextId++)
         }
      }
   }

   val pageStackId: PageStackBoard.PageStackId
   val pageId: PageStack.PageId

   operator fun get(id: LayoutId): LayoutCoordinates?
}

@Stable
interface MutablePageLayoutInfo : PageLayoutInfo {
   operator fun set(id: PageLayoutInfo.LayoutId, coordinates: LayoutCoordinates)
}

@Stable
internal class PageLayoutInfoImpl(
   override val pageStackId: PageStackBoard.PageStackId,
   override val pageId: PageStack.PageId
) : MutablePageLayoutInfo {
   private val map = mutableStateMapOf<PageLayoutInfo.LayoutId, LayoutCoordinates>()

   override fun get(id: PageLayoutInfo.LayoutId): LayoutCoordinates? = map[id]

   override fun set(id: PageLayoutInfo.LayoutId, coordinates: LayoutCoordinates) {
      map[id] = coordinates
   }

   internal fun isEmpty(): Boolean = map.isEmpty()
}
