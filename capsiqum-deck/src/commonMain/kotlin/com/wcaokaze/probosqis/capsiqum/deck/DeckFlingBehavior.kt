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

package com.wcaokaze.probosqis.capsiqum.deck

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import kotlin.math.sign

internal object DeckFlingBehavior {
   @Stable
   class Standard(private val state: DeckState<*>) : FlingBehavior {
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

         val currentIdx   = state.layoutLogic.indexOfScrollOffset(currentScrollOffset)
         val estimatedIdx = state.layoutLogic.indexOfScrollOffset(estimatedScrollOffset)

         val targetIdx = when {
            currentIdx < 0 || estimatedIdx < 0 -> {
               return FlingSpec(0.0f, 0.0f, 0.0f, 0.0f)
            }

            estimatedIdx > currentIdx ->
               (currentIdx + 1).coerceAtMost(state.layoutInfo.cardsInfo.size - 1)

            estimatedIdx < currentIdx ->
               currentIdx

            currentIdx >= state.layoutInfo.cardsInfo.size - 1 ->
               currentIdx

            else -> {
               val leftCardScrollOffset  = state.getScrollOffset(currentIdx,     PositionInDeck.FirstVisible)
               val rightCardScrollOffset = state.getScrollOffset(currentIdx + 1, PositionInDeck.FirstVisible)

               if (
                  (estimatedScrollOffset - leftCardScrollOffset)
                     / (rightCardScrollOffset - leftCardScrollOffset)
                     < 0.5f
               ) {
                  currentIdx
               } else {
                  (currentIdx + 1).coerceAtMost(state.layoutInfo.cardsInfo.size - 1)
               }
            }
         }

         val targetScrollOffset = state.getScrollOffset(
            targetIdx, PositionInDeck.FirstVisible)

         // ちょうどtargetScrollOffsetで止まる加速度を算出する
         val acceleration = initialVelocity * initialVelocity /
               (targetScrollOffset - currentScrollOffset) / -2.0f

         val duration = estimateFlingingDuration(initialVelocity, acceleration)
         return if (duration in 0.0f..0.25f) {
            FlingSpec(targetScrollOffset.toFloat(), duration, initialVelocity,
               acceleration)
         } else {
            // ほとんど速さが0に近いような状態で指を離された場合は
            // ある程度の初速をもたせる。
            // initialVelocityがtargetScrollOffsetと逆向きのとき
            // durationが負になる。この場合も同様にtargetScrollOffset向きの初速に
            // 修正する。
            val fixedVelocity = (targetScrollOffset - currentScrollOffset) / 0.25f
            val fixedAcceleration = fixedVelocity * fixedVelocity /
                  (targetScrollOffset - currentScrollOffset) / -2.0f
            val fixedDuration = estimateFlingingDuration(
               fixedVelocity, fixedAcceleration)

            FlingSpec(targetScrollOffset.toFloat(), fixedDuration, fixedVelocity,
               fixedAcceleration)
         }
      }

      /** s */
      private fun estimateFlingingDuration(
         /** px/s */
         velocity: Float,
         /** px/s² */
         acceleration: Float
      ): Float {
         if (velocity == 0.0f) { return Float.POSITIVE_INFINITY }
         return velocity / -acceleration
      }

      private fun estimateFlingingScrollOffset(
         /** px */
         currentScrollOffset: Float,
         /** px/s */
         velocity: Float,
         /** px/s² */
         acceleration: Float = sign(velocity) * -1000.0f
      ): Float {
         if (velocity == 0.0f) { return currentScrollOffset }

         val d = estimateFlingingDuration(velocity, acceleration)
         return currentScrollOffset + velocity * d + acceleration * d * d / 2.0f
      }
   }
}
