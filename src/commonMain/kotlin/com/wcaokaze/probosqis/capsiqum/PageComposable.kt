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

package com.wcaokaze.probosqis.capsiqum

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.wcaokaze.probosqis.capsiqum.page.Page
import com.wcaokaze.probosqis.capsiqum.page.PageStackState
import com.wcaokaze.probosqis.capsiqum.page.PageState
import com.wcaokaze.probosqis.capsiqum.page.PageStateFactory
import com.wcaokaze.probosqis.capsiqum.transition.PageTransitionSpec
import com.wcaokaze.probosqis.capsiqum.transition.pageTransitionSpec
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlin.reflect.KClass

inline fun <reified P : Page, reified S : PageState> pageComposable(
   pageStateFactory: PageStateFactory<P, S>,
   noinline content: @Composable (P, S, PageStackState, WindowInsets) -> Unit,
   noinline header: @Composable (P, S, PageStackState) -> Unit,
   // 本来nullableである必要はないがデフォルト引数でreified型引数のPとSを使えないため
   // headerActionsが空の状態をnullでも表せるようにする
   noinline headerActions: (@Composable RowScope.(P, S, PageStackState) -> Unit)? = null,
   noinline footer: (@Composable (P, S, PageStackState) -> Unit)?,
   pageTransitions: PageTransitionSet.Builder.() -> Unit
) = PageComposable(
   P::class,
   S::class,
   pageStateFactory,
   content,
   header,
   headerActions,
   footer,
   pageTransitionSet = PageTransitionSet.Builder().apply(pageTransitions).build()
)

@Stable
data class PageComposable<P : Page, S : PageState>(
   val pageClass: KClass<P>,
   val pageStateClass: KClass<S>,
   val pageStateFactory: PageStateFactory<P, S>,
   val contentComposable: @Composable (P, S, PageStackState, WindowInsets) -> Unit,
   val headerComposable: @Composable (P, S, PageStackState) -> Unit,
   val headerActionsComposable: (@Composable RowScope.(P, S, PageStackState) -> Unit)?,
   val footerComposable: (@Composable (P, S, PageStackState) -> Unit)?,
   val pageTransitionSet: PageTransitionSet
)

@Immutable
data class PageTransitionSet(
   val outgoingTransitions: ImmutableMap<KClass<out Page>, PageTransitionSpec>,
   val incomingTransitions: ImmutableMap<KClass<out Page>, PageTransitionSpec>
) {
   fun getTransitionTo(pageClass: KClass<out Page>): PageTransitionSpec?
         = outgoingTransitions[pageClass]

   fun getTransitionFrom(pageClass: KClass<out Page>): PageTransitionSpec?
         = incomingTransitions[pageClass]

   class Builder {
      private var outgoingTransitions = mutableMapOf<KClass<out Page>, PageTransitionSpec>()
      private var incomingTransitions = mutableMapOf<KClass<out Page>, PageTransitionSpec>()

      fun transitionTo(
         pageClass: KClass<out Page>,
         transitionSpec: PageTransitionSpec
      ) {
         outgoingTransitions[pageClass] = transitionSpec
      }

      inline fun <reified P : Page> transitionTo(
         enter: PageTransitionSpec.Builder.() -> Unit,
         exit:  PageTransitionSpec.Builder.() -> Unit
      ) {
         transitionTo(
            P::class,
            pageTransitionSpec(enter, exit)
         )
      }

      fun transitionFrom(
         pageClass: KClass<out Page>,
         transitionSpec: PageTransitionSpec
      ) {
         incomingTransitions[pageClass] = transitionSpec
      }

      inline fun <reified P : Page> transitionFrom(
         enter: PageTransitionSpec.Builder.() -> Unit,
         exit:  PageTransitionSpec.Builder.() -> Unit
      ) {
         transitionFrom(
            P::class,
            pageTransitionSpec(enter, exit)
         )
      }

      fun build() = PageTransitionSet(
         outgoingTransitions.toImmutableMap(),
         incomingTransitions.toImmutableMap()
      )
   }
}
