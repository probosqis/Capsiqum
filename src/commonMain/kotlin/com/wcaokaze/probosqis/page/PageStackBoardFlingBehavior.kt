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

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import kotlin.math.sign

internal object PageStackBoardFlingBehavior {
   @Stable
   class Standard(private val state: PageStackBoardState) : FlingBehavior {
      override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
         val flingSpec = FlingSpec(initialVelocity)

         // ns
         var finishTime = Long.MIN_VALUE
         // ns
         var prevTime = Long.MIN_VALUE

         var velocity = flingSpec.initialVelocity
         val acceleration = flingSpec.acceleration

         withFrameNanos { time ->
            finishTime = time + (flingSpec.duration * 1_000_000_000.0f).toLong()
            prevTime = time
         }

         do {
            val isRunning = withFrameNanos { time ->
               if (time < finishTime) {
                  val d = (time - prevTime) / 1_000_000_000.0f
                  val diff = velocity * d + acceleration * d * d / 2.0f
                  scrollBy(diff)
                  velocity += acceleration * d
                  prevTime = time

                  true
               } else {
                  scrollBy(flingSpec.targetScrollOffset - state.scrollState.scrollOffset)

                  false
               }
            }
         } while (isRunning)

         return 0.0f
      }

      private class FlingSpec(
         /** px */
         val targetScrollOffset: Float,
         /** s */
         val duration: Float,
         /** px/s */
         val initialVelocity: Float,
         /** px/s² */
         val acceleration: Float
      )

      private fun FlingSpec(initialVelocity: Float): FlingSpec {
         val currentScrollOffset = state.scrollState.scrollOffset
         val estimatedScrollOffset = estimateFlingingScrollOffset(
            currentScrollOffset, initialVelocity)

         val currentIdx = state.firstVisiblePageStackIndex
         val estimatedIdx = state.layout
            .indexOfFirst { it.position.x + it.width > estimatedScrollOffset }

         val targetIdx = when {
            estimatedIdx < 0 || estimatedIdx > currentIdx -> {
               (currentIdx + 1)
                  .coerceAtMost(state.pageStackBoard.pageStackCount - 1)
            }
            estimatedIdx < currentIdx -> {
               currentIdx
            }

            currentIdx >= state.pageStackBoard.pageStackCount - 1 -> {
               currentIdx
            }

            else -> {
               val leftStackScrollOffset  = state.getScrollOffset(currentIdx,     PositionInBoard.FirstVisible)
               val rightStackScrollOffset = state.getScrollOffset(currentIdx + 1, PositionInBoard.FirstVisible)

               if (
                  (estimatedScrollOffset - leftStackScrollOffset)
                  / (rightStackScrollOffset - leftStackScrollOffset)
                  < 0.5f
               ) {
                  currentIdx
               } else {
                  (currentIdx + 1)
                     .coerceAtMost(state.pageStackBoard.pageStackCount - 1)
               }
            }
         }

         val targetScrollOffset = state.getScrollOffset(
            targetIdx, PositionInBoard.FirstVisible)

         // ちょうどtargetScrollOffsetで止まる加速度を算出する
         val acceleration = initialVelocity * initialVelocity /
               (targetScrollOffset - currentScrollOffset) / -2.0f

         val duration = estimateFlingingDuration(initialVelocity, acceleration)
         return if (duration <= 0.25f) {
            FlingSpec(targetScrollOffset.toFloat(), duration, initialVelocity,
               acceleration)
         } else {
            // ほとんど速さが0に近いような状態で指を離された場合は
            // ある程度の初速をもたせる
            val fixedVelocity = (targetScrollOffset - currentScrollOffset) / 0.25f
            val fixedAcceleration = fixedVelocity * fixedVelocity /
                  (targetScrollOffset - currentScrollOffset) / -2.0f
            val fixedDuration = estimateFlingingDuration(
               fixedVelocity, fixedAcceleration)

            FlingSpec(targetScrollOffset.toFloat(), fixedDuration, fixedVelocity,
               fixedAcceleration)
         }
      }

      private fun estimateFlingingDuration(
         velocity: Float,
         acceleration: Float
      ): Float {
         if (velocity == 0.0f) { return Float.POSITIVE_INFINITY }
         return velocity / -acceleration
      }

      private fun estimateFlingingScrollOffset(
         currentScrollOffset: Float,
         velocity: Float,
         acceleration: Float = sign(velocity) * -0.001f
      ): Float {
         if (velocity == 0.0f) { return currentScrollOffset }

         val d = estimateFlingingDuration(velocity, acceleration)
         return currentScrollOffset + velocity * d + acceleration * d * d / 2.0f
      }
   }
}
