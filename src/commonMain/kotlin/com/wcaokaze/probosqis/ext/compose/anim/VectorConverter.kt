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

package com.wcaokaze.probosqis.ext.compose.anim

import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.ui.layout.ScaleFactor

val ScaleFactor.Companion.VectorConverter: TwoWayConverter<ScaleFactor, AnimationVector2D>
   get() = TwoWayConverter(
      convertToVector = { AnimationVector2D(it.scaleX, it.scaleY) },
      convertFromVector = { ScaleFactor(it.v1, it.v2) }
   )
