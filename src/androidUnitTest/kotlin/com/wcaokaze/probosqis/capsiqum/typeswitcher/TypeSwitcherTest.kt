/*
 * Copyright 2024 wcaokaze
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

package com.wcaokaze.probosqis.capsiqum.typeswitcher

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class TypeSwitcherTest {
   @get:Rule
   val rule = createComposeRule()

   private abstract class State
   private class StateA : State()
   private class StateB : State()
   private class StateC : State()

   @Test
   fun state_getChild() {
      val switcherState = TypeSwitcherState(
         listOf(
            typeSwitcherChild<StateA> {},
            typeSwitcherChild<StateB> {},
         )
      )

      val stateA = StateA()
      val childA = switcherState.getChildFor(stateA)
      assertNotNull(childA)
      assertEquals(childA.type, StateA::class)

      val stateB = StateB()
      val childB = switcherState.getChildFor(stateB)
      assertNotNull(childB)
      assertEquals(childB.type, StateB::class)

      val stateC = StateC()
      val childC = switcherState.getChildFor(stateC)
      assertNull(childC)
   }

   @Test
   fun childComposable_argument() {
      var stateAArgument: StateA? = null
      var stateBArgument: StateB? = null

      val switcherState = TypeSwitcherState(
         listOf(
            typeSwitcherChild<StateA> { state ->
               DisposableEffect(Unit) {
                  stateAArgument = state
                  onDispose { stateAArgument = null }
               }
            },
            typeSwitcherChild<StateB> { state ->
               DisposableEffect(Unit) {
                  stateBArgument = state
                  onDispose { stateBArgument = null }
               }
            },
         )
      )

      var value: State by mutableStateOf(StateA())

      rule.setContent {
         TypeSwitcher(switcherState, value)
      }

      rule.runOnIdle {
         assertSame(value as State?, stateAArgument)
         assertNull(stateBArgument)
      }

      value = StateB()

      rule.runOnIdle {
         assertNull(stateAArgument)
         assertSame(value as State?, stateBArgument)
      }
   }
}
