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

import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PageStackBoardRepositoryTest {
   @Serializable
   @SerialName("com.wcaokaze.probosqis.page.IntPage")
   class IntPage(val i: Int) : Page()

   @Serializable
   @SerialName("com.wcaokaze.probosqis.page.StringPage")
   class StringPage(val s: String) : Page()

   private lateinit var pageStackRepository: PageStackRepository
   private lateinit var pageStackBoardRepository: PageStackBoardRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = createPageStackRepository(
         listOf(
            pageSerializer<IntPage>(),
            pageSerializer<StringPage>(),
         )
      )

      pageStackBoardRepository = createPageStackBoardRepository(
         pageStackRepository
      )
   }

   @AfterTest
   fun afterTest() {
      deleteRepositories()
   }

   @Test
   fun readWrite() {
      val intPage = IntPage(42)
      val stringPage = StringPage("wcaokaze")
      var pageStack = PageStack(
         PageStack.Id(0L),
         PageStack.SavedPageState(
            PageStack.PageId(0L),
            intPage
         )
      )
      pageStack = pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(1L),
            stringPage
         )
      )

      val pageStackCache = pageStackRepository.savePageStack(pageStack)
      val children = persistentListOf(
         PageStackBoard.PageStack(
            PageStackBoard.PageStackId(pageStackCache.value.id.value),
            pageStackCache
         ),
      )
      val rootRow = PageStackBoard.Row(children)
      val pageStackBoard = PageStackBoard(rootRow)

      pageStackBoardRepository.savePageStackBoard(pageStackBoard)

      val loadedCache = pageStackBoardRepository.loadPageStackBoard()

      assertEquals(loadedCache.value.rootRow.childCount, 1)

      val loadedPageStack
         = assertIs<PageStackBoard.PageStack>(loadedCache.value.rootRow[0])
         .pageStackCache.value

      val pageId1 = loadedPageStack.head.id
      val page1 = loadedPageStack.head.page
      assertEquals(PageStack.PageId(1L), pageId1)
      assertIs<StringPage>(page1)
      assertEquals(stringPage.s, page1.s)

      var tail = loadedPageStack.tailOrNull()
      assertNotNull(tail)
      val pageId2 = tail.head.id
      val page2 = tail.head.page
      assertEquals(PageStack.PageId(0L), pageId2)
      assertIs<IntPage>(page2)
      assertEquals(intPage.i, page2.i)

      tail = tail.tailOrNull()
      assertNull(tail)
   }
}
