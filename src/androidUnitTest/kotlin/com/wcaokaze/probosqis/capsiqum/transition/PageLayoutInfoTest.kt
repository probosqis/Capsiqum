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

import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.test.junit4.createComposeRule
import io.mockk.mockk
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class PageLayoutInfoTest {
   @get:Rule
   val rule = createComposeRule()

   @Test
   fun id_autoIncrement() {
      val a = PageLayoutInfo.LayoutId()
      val b = PageLayoutInfo.LayoutId()
      val c = PageLayoutInfo.LayoutId()
      assertEquals(1, b.id - a.id)
      assertEquals(2, c.id - a.id)
   }

   @Test
   fun globalIds_companionObjectEqualsSubclasses() {
      val someLayoutIds = object : PageLayoutIds() {}

      assertEquals(someLayoutIds.root,       PageLayoutIds.root)
      assertEquals(someLayoutIds.background, PageLayoutIds.background)
      assertEquals(someLayoutIds.content,    PageLayoutIds.content)
   }

   @Test
   fun getAndSet() {
      val pageLayoutInfo = PageLayoutInfoImpl(key = Unit)
      val layoutId1 = PageLayoutInfo.LayoutId()
      val layoutId2 = PageLayoutInfo.LayoutId()

      val coordinates = mockk<LayoutCoordinates>()
      pageLayoutInfo[layoutId1] = coordinates

      assertSame(coordinates, pageLayoutInfo[layoutId1])
      assertNull(pageLayoutInfo[layoutId2])
   }

   @Test
   fun isEmpty() {
      val pageLayoutInfo = PageLayoutInfoImpl(key = Unit)

      assertTrue(pageLayoutInfo.isEmpty())

      val layoutId = PageLayoutInfo.LayoutId()
      pageLayoutInfo[layoutId] = mockk()

      assertFalse(pageLayoutInfo.isEmpty())
   }
}
