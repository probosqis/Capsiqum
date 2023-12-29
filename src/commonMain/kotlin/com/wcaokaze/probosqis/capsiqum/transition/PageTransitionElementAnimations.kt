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

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffset
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.layout.positionInRoot
import com.wcaokaze.probosqis.capsiqum.PageStackBoard

enum class SharedElementAnimatorElement {
   Current,
   Target,
   CrossFade,
}

class SharedElementAnimations(
   val offsetTransitionSpec:
         (@Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<Offset>)?
         = null,
   val scaleTransitionSpec:
         (@Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<ScaleFactor>)?
         = null,
) {
   companion object {
      val Offset = Offset { spring() }

      @Suppress("FunctionName")
      fun Offset(
         transitionSpec:
               @Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<Offset>
      ) = SharedElementAnimations(
         transitionSpec,
         scaleTransitionSpec = null,
      )

      val Scale = Scale { spring() }

      @Suppress("FunctionName")
      fun Scale(
         transitionSpec:
               @Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<ScaleFactor>
      ) = SharedElementAnimations(
         offsetTransitionSpec = null,
         transitionSpec,
      )
   }

   operator fun plus(another: SharedElementAnimations) = SharedElementAnimations(
      another.offsetTransitionSpec ?: offsetTransitionSpec,
      another.scaleTransitionSpec  ?: scaleTransitionSpec,
   )
}

fun PageTransitionSpec.Builder.sharedElement(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   animatorElement: SharedElementAnimatorElement
         = SharedElementAnimatorElement.CrossFade,
   animations: SharedElementAnimations
         = SharedElementAnimations.Offset + SharedElementAnimations.Scale
) {
   when (animatorElement) {
      SharedElementAnimatorElement.Current -> sharedElementAnimateCurrent(
         currentPageLayoutElementId, targetPageLayoutElementId, label, animations)
      SharedElementAnimatorElement.Target -> sharedElementAnimateTarget(
         currentPageLayoutElementId, targetPageLayoutElementId, label, animations)
      SharedElementAnimatorElement.CrossFade -> sharedElementCrossFade(
         currentPageLayoutElementId, targetPageLayoutElementId, label, animations)
   }
}

private fun PageTransitionSpec.Builder.sharedElementAnimateCurrent(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   animations: SharedElementAnimations
) {
   val runningTransitions
         = mutableStateMapOf<PageStackBoard.PageStackId, Transition<PageLayoutInfo>>()

   currentPageElement(currentPageLayoutElementId) {
      val pageStackId = transition.currentState.pageStackId

      DisposableEffect(transition, pageStackId) {
         runningTransitions[pageStackId] = transition

         onDispose {
            runningTransitions.remove(pageStackId)
         }
      }

      val offsetState = if (animations.offsetTransitionSpec != null) {
         animatePosition(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Position",
            animations.offsetTransitionSpec
         )
      } else {
         null
      }

      val scaleState = if (animations.scaleTransitionSpec != null) {
         animateScale(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Scale",
            animations.scaleTransitionSpec
         )
      } else {
         null
      }

      Modifier.graphicsLayer {
         transformOrigin = TransformOrigin(0.0f, 0.0f)
         if (offsetState != null) {
            translationX = offsetState.value.x
            translationY = offsetState.value.y
         }
         if (scaleState != null) {
            scaleX = scaleState.value.scaleX
            scaleY = scaleState.value.scaleY
         }
      }
   }

   targetPageElement(targetPageLayoutElementId) {
      val pageStackId = transition.currentState.pageStackId
      val transition = runningTransitions[pageStackId]

      Modifier.graphicsLayer {
         alpha = if (transition == null || transition.isRunning) {
            0.0f
         } else {
            1.0f
         }
      }
   }
}

private fun PageTransitionSpec.Builder.sharedElementAnimateTarget(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   animations: SharedElementAnimations
) {
   val runningTransitions
         = mutableStateMapOf<PageStackBoard.PageStackId, Transition<PageLayoutInfo>>()

   targetPageElement(targetPageLayoutElementId) {
      val pageStackId = transition.currentState.pageStackId

      DisposableEffect(transition, pageStackId) {
         runningTransitions[pageStackId] = transition

         onDispose {
            runningTransitions.remove(pageStackId)
         }
      }

      val offsetState = if (animations.offsetTransitionSpec != null) {
         animatePosition(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Position",
            animations.offsetTransitionSpec
         )
      } else {
         null
      }

      val scaleState = if (animations.scaleTransitionSpec != null) {
         animateScale(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Scale",
            animations.scaleTransitionSpec
         )
      } else {
         null
      }

      Modifier.graphicsLayer {
         transformOrigin = TransformOrigin(0.0f, 0.0f)
         if (offsetState != null) {
            translationX = offsetState.value.x
            translationY = offsetState.value.y
         }
         if (scaleState != null) {
            scaleX = scaleState.value.scaleX
            scaleY = scaleState.value.scaleY
         }
      }
   }

   currentPageElement(currentPageLayoutElementId) {
      val pageStackId = transition.currentState.pageStackId
      val transition = runningTransitions[pageStackId]

      Modifier.graphicsLayer {
         alpha = if (transition == null || transition.isRunning) {
            0.0f
         } else {
            1.0f
         }
      }
   }
}

private fun PageTransitionSpec.Builder.sharedElementCrossFade(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   animations: SharedElementAnimations
) {
   currentPageElement(currentPageLayoutElementId) {
      val alphaState = transition.animateFloat(
         transitionSpec = { tween(easing = LinearEasing) },
         "$label-CrossFade"
      ) {
         if (it.isCurrentPage) { 1.0f } else { 0.0f }
      }

      val offsetState = if (animations.offsetTransitionSpec != null) {
         animatePosition(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Position",
            animations.offsetTransitionSpec
         )
      } else {
         null
      }

      val scaleState = if (animations.scaleTransitionSpec != null) {
         animateScale(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Scale",
            animations.scaleTransitionSpec
         )
      } else {
         null
      }

      Modifier
         .graphicsLayer {
            transformOrigin = TransformOrigin(0.0f, 0.0f)
            if (offsetState != null) {
               translationX = offsetState.value.x
               translationY = offsetState.value.y
            }
            if (scaleState != null) {
               scaleX = scaleState.value.scaleX
               scaleY = scaleState.value.scaleY
            }
         }
         .graphicsLayer {
            alpha = alphaState.value
         }
   }

   targetPageElement(targetPageLayoutElementId) {
      val alphaState = transition.animateFloat(
         transitionSpec = { tween(easing = LinearEasing) },
         "$label-CrossFade"
      ) {
         if (it.isTargetPage) { 1.0f } else { 0.0f }
      }

      val offsetState = if (animations.offsetTransitionSpec != null) {
         animatePosition(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Position",
            animations.offsetTransitionSpec
         )
      } else {
         null
      }

      val scaleState = if (animations.scaleTransitionSpec != null) {
         animateScale(
            currentPageLayoutElementId,
            targetPageLayoutElementId,
            "$label-Scale",
            animations.scaleTransitionSpec
         )
      } else {
         null
      }

      Modifier
         .graphicsLayer {
            transformOrigin = TransformOrigin(0.0f, 0.0f)
            if (offsetState != null) {
               translationX = offsetState.value.x
               translationY = offsetState.value.y
            }
            if (scaleState != null) {
               scaleX = scaleState.value.scaleX
               scaleY = scaleState.value.scaleY
            }
         }
         .graphicsLayer {
            alpha = alphaState.value
         }
   }
}

// =============================================================================

@Composable
fun CurrentPageTransitionElementAnimScope.animateScale(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   transitionSpec: @Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<ScaleFactor>
         = { spring() }
): State<ScaleFactor> {
   return transition.animateValue(ScaleFactor.VectorConverter, transitionSpec, label) {
      if (it.isCurrentPage) {
         ScaleFactor(1.0f, 1.0f)
      } else {
         val currentCoordinates = transition.currentState[currentPageLayoutElementId]
         val targetCoordinates  = transition.targetState [targetPageLayoutElementId]

         if (currentCoordinates == null || targetCoordinates == null) {
            ScaleFactor(1.0f, 1.0f)
         } else {
            val currentSize = currentCoordinates.size
            val targetSize  = targetCoordinates .size

            ScaleFactor(
               targetSize.width  / currentSize.width .toFloat(),
               targetSize.height / currentSize.height.toFloat()
            )
         }
      }
   }
}

@Composable
fun TargetPageTransitionElementAnimScope.animateScale(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   transitionSpec: @Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<ScaleFactor>
      = { spring() }
): State<ScaleFactor> {
   return transition.animateValue(ScaleFactor.VectorConverter, transitionSpec, label) {
      if (it.isTargetPage) {
         ScaleFactor(1.0f, 1.0f)
      } else {
         val currentCoordinates = transition.currentState[currentPageLayoutElementId]
         val targetCoordinates  = transition.targetState [targetPageLayoutElementId]

         if (currentCoordinates == null || targetCoordinates == null) {
            ScaleFactor(1.0f, 1.0f)
         } else {
            val currentSize = currentCoordinates.size
            val targetSize  = targetCoordinates .size

            ScaleFactor(
               currentSize.width  / targetSize.width .toFloat(),
               currentSize.height / targetSize.height.toFloat()
            )
         }
      }
   }
}

@Composable
fun CurrentPageTransitionElementAnimScope.animatePosition(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   transitionSpec: @Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<Offset>
         = { spring() }
): State<Offset> {
   return transition.animateOffset(transitionSpec, label) {
      if (it.isCurrentPage) {
         Offset.Zero
      } else {
         val currentCoordinates = transition.currentState[currentPageLayoutElementId]
         val targetCoordinates  = transition.targetState [targetPageLayoutElementId]

         if (currentCoordinates == null || targetCoordinates == null) {
            Offset.Zero
         } else {
            targetCoordinates.positionInRoot() -
                  currentCoordinates.positionInRoot()
         }
      }
   }
}

@Composable
fun TargetPageTransitionElementAnimScope.animatePosition(
   currentPageLayoutElementId: PageLayoutInfo.LayoutId,
   targetPageLayoutElementId:  PageLayoutInfo.LayoutId,
   label: String,
   transitionSpec: @Composable Transition.Segment<PageLayoutInfo>.() -> FiniteAnimationSpec<Offset>
      = { spring() }
): State<Offset> {
   return transition.animateOffset(transitionSpec, label) {
      if (it.isTargetPage) {
         Offset.Zero
      } else {
         val currentCoordinates = transition.currentState[currentPageLayoutElementId]
         val targetCoordinates  = transition.targetState [targetPageLayoutElementId]

         if (currentCoordinates == null || targetCoordinates == null) {
            Offset.Zero
         } else {
            currentCoordinates.positionInRoot() -
                  targetCoordinates.positionInRoot()
         }
      }
   }
}
