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

package com.wcaokaze.probosqis.page

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.wcaokaze.probosqis.page.transition.PageTransitionSpec
import com.wcaokaze.probosqis.page.transition.pageTransitionSpec
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlin.reflect.KClass

inline fun <reified P : Page, S : PageState> pageComposable(
   pageStateFactory: PageStateFactory<P, S>,
   noinline content: @Composable (P, S, PageStackState) -> Unit,
   noinline header: @Composable (P, S, PageStackState) -> Unit,
   noinline footer: (@Composable (P, S, PageStackState) -> Unit)?,
   pageTransitions: PageTransitionSet.Builder.() -> Unit
) = PageComposable(
   P::class,
   pageStateFactory,
   content,
   header,
   footer,
   pageTransitionSet = PageTransitionSet.Builder().apply(pageTransitions).build()
)

@Stable
data class PageComposable<P : Page, S : PageState>(
   val pageClass: KClass<P>,
   val pageStateFactory: PageStateFactory<P, S>,
   val contentComposable: @Composable (P, S, PageStackState) -> Unit,
   val headerComposable: @Composable (P, S, PageStackState) -> Unit,
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
