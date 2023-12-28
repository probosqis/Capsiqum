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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val testDir = File(".pageStackRepositoryTest")

internal fun createPageStackRepository(
   allPageSerializers: List<PageStackRepository.PageSerializer<*>>
): PageStackRepository {
   if (testDir.exists()) {
      if (!testDir.deleteRecursively()) { throw IOException() }
   }
   if (!testDir.mkdir()) { throw IOException() }

   return DesktopPageStackRepository(allPageSerializers, testDir)
}

internal fun createPageStackBoardRepository(
   pageStackRepository: PageStackRepository
): PageStackBoardRepository {
   return DesktopPageStackBoardRepository(pageStackRepository, testDir)
}

internal fun deletePageStackRepository() {
   testDir.deleteRecursively()
}

internal fun deleteRepositories() {
   testDir.deleteRecursively()
}

class PageStackRepositoryTest {
   @Serializable
   @SerialName("com.wcaokaze.probosqis.page.IntPage")
   class IntPage(val i: Int) : Page()

   @Serializable
   @SerialName("com.wcaokaze.probosqis.page.StringPage")
   class StringPage(val s: String) : Page()

   private lateinit var pageStackRepository: PageStackRepository

   @BeforeTest
   fun beforeTest() {
      pageStackRepository = createPageStackRepository(
         listOf(
            pageSerializer<IntPage>(),
            pageSerializer<StringPage>(),
         )
      )
   }

   @AfterTest
   fun afterTest() {
      deletePageStackRepository()
   }

   @Test
   fun readWrite() {
      val intPage = IntPage(42)
      val stringPage = StringPage("wcaokaze")
      var pageStack = PageStack(
         PageStack.Id(0L),
         PageStack.SavedPageState(
            PageStack.PageId(42L),
            intPage
         )
      )
      pageStack = pageStack.added(
         PageStack.SavedPageState(
            PageStack.PageId(43L),
            stringPage
         )
      )

      pageStackRepository.savePageStack(pageStack)

      val loadedCache = pageStackRepository.loadPageStack(pageStack.id)

      val pageId1 = loadedCache.value.head.id
      val page1 = loadedCache.value.head.page
      assertEquals(PageStack.PageId(43L), pageId1)
      assertIs<StringPage>(page1)
      assertEquals(stringPage.s, page1.s)

      var tail = loadedCache.value.tailOrNull()
      assertNotNull(tail)
      val pageId2 = tail.head.id
      val page2 = tail.head.page
      assertEquals(PageStack.PageId(42L), pageId2)
      assertIs<IntPage>(page2)
      assertEquals(intPage.i, page2.i)

      tail = tail.tailOrNull()
      assertNull(tail)
   }

   @Test
   fun identifyFiles() {
      val pageStack1 = PageStack(
         PageStack.Id(1L),
         PageStack.SavedPageState(
            PageStack.PageId(42L),
            IntPage(42)
         )
      )

      val pageStack2 = PageStack(
         PageStack.Id(2L),
         PageStack.SavedPageState(
            PageStack.PageId(43L),
            IntPage(43)
         )
      )

      pageStackRepository.savePageStack(pageStack1)
      pageStackRepository.savePageStack(pageStack2)

      val loadedCache1 = pageStackRepository.loadPageStack(pageStack1.id)
      val loadedCache2 = pageStackRepository.loadPageStack(pageStack2.id)

      val page1 = loadedCache1.value.head.page
      val page2 = loadedCache2.value.head.page

      assertIs<IntPage>(page1)
      assertEquals(42, page1.i)

      assertIs<IntPage>(page2)
      assertEquals(43, page2.i)
   }

   @Test
   fun throwIfFileNotFound() {
      assertFails {
         pageStackRepository.loadPageStack(PageStack.Id(0L))
      }
   }
}
