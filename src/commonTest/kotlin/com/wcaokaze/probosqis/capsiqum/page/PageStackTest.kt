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

package com.wcaokaze.probosqis.capsiqum.page

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

class PageStackTest {
   @Serializable
   @SerialName("com.wcaokaze.probosqis.capsiqum.IntPage")
   class IntPage(val i: Int) : Page()

   @Serializable
   @SerialName("com.wcaokaze.probosqis.capsiqum.StringPage")
   class StringPage(val s: String) : Page()

   @Test
   fun add_pop() {
      var pageStack = PageStack(
         PageStack.Id(0L),
         PageStack.SavedPageState(
            PageStack.PageId(0L),
            IntPage(0)
         )
      )
      pageStack = pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(1L),
            StringPage("1")
         )
      )
      pageStack = pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(2L),
            StringPage("2")
         )
      )
      pageStack = pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(3L),
            IntPage(3)
         )
      )

      var headId = pageStack.head.id
      var headPage = pageStack.head.page
      assertEquals(PageStack.PageId(3L), headId)
      assertIs<IntPage>(headPage)
      assertEquals(3, headPage.i)

      var tail = pageStack.tailOrNull()
      assertNotNull(tail)
      headId = tail.head.id
      headPage = tail.head.page
      assertEquals(PageStack.PageId(2L), headId)
      assertIs<StringPage>(headPage)
      assertEquals("2", headPage.s)

      tail = tail.tailOrNull() ?: fail()
      headId = tail.head.id
      headPage = tail.head.page
      assertEquals(PageStack.PageId(1L), headId)
      assertIs<StringPage>(headPage)
      assertEquals("1", headPage.s)

      tail = tail.tailOrNull() ?: fail()
      headId = tail.head.id
      headPage = tail.head.page
      assertEquals(PageStack.PageId(0L), headId)
      assertIs<IntPage>(headPage)
      assertEquals(0, headPage.i)

      assertNull(tail.tailOrNull())
   }

   @Test
   fun immutability() {
      var pageStack = PageStack(
         PageStack.Id(0L),
         PageStack.SavedPageState(
            PageStack.PageId(0L),
            IntPage(0)
         )
      )
      pageStack = pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(1L),
            StringPage("1")
         )
      )

      val tail = pageStack.tailOrNull()
      assertNotNull(tail)
      val tailHead = tail.head.page
      assertIs<IntPage>(tailHead)
      assertEquals(0, tailHead.i)

      val pageStackHead = pageStack.head.page
      assertIs<StringPage>(pageStackHead)
      assertEquals("1", pageStackHead.s)
   }
}
