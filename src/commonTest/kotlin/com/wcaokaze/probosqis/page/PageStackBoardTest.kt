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

import com.wcaokaze.probosqis.panoptiqon.WritableCache
import kotlinx.collections.immutable.toImmutableList
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PageStackBoardTest {
   @Serializable
   private class PageImpl(val i: Int) : Page()

   private fun createPageStackElement(i: Int): PageStackBoard.PageStack {
      val page = PageImpl(i)
      val pageStack = PageStack(
         PageStack.Id(i.toLong()),
         PageStack.SavedPageState(
            PageStack.PageId(i.toLong()),
            page
         )
      )
      return PageStackBoard.PageStack(
         PageStackBoard.PageStackId(pageStack.id.value),
         WritableCache(pageStack)
      )
   }

   private fun createColumn(pageStackCount: Int): PageStackBoard.Column {
      val children = List(pageStackCount) { createPageStackElement(it) }
         .toImmutableList()

      return PageStackBoard.Column(children)
   }

   private fun createRow(pageStackCount: Int): PageStackBoard.Row {
      val children = List(pageStackCount) { createPageStackElement(it) }
         .toImmutableList()

      return PageStackBoard.Row(children)
   }

   private fun assertPage(expected: Int, element: PageStackBoard.LayoutElement) {
      assertIs<PageStackBoard.PageStack>(element)
      val page = assertIs<PageImpl>(element.pageStackCache.value.head.page)
      assertEquals(expected, page.i)
   }

   private infix fun PageStackBoard.treeEquals(other: PageStackBoard): Boolean {
      fun subtreeEquals(
         expected: PageStackBoard.LayoutElementParent,
         actual:   PageStackBoard.LayoutElementParent
      ): Boolean {
         if (expected is PageStackBoard.Column
            && actual !is PageStackBoard.Column)
         {
            return false
         }
         if (expected is PageStackBoard.Row
            && actual !is PageStackBoard.Row)
         {
            return false
         }

         if (expected.childCount != actual.childCount) { return false }

         for ((expectedChild, actualChild) in expected.zip(actual)) {
            when (expectedChild) {
               is PageStackBoard.PageStack -> {
                  if (actualChild !is PageStackBoard.PageStack) { return false }

                  val expectedPageStack = expectedChild.pageStackCache.value
                  val actualPageStack   = actualChild  .pageStackCache.value
                  if (expectedPageStack.id != actualPageStack.id) { return false }
               }

               is PageStackBoard.LayoutElementParent -> {
                  if (actualChild !is PageStackBoard.LayoutElementParent) {
                     return false
                  }

                  if (!subtreeEquals(expectedChild, actualChild)) { return false }
               }
            }
         }

         return true
      }

      return subtreeEquals(rootRow, other.rootRow)
   }

   @Test
   fun treeEquals() {
      assertTrue {
         val a = PageStackBoard(
            Row(PageStack(0L))
         )
         val b = PageStackBoard(
            Row(PageStack(0L))
         )
         a treeEquals b
      }

      assertFalse {
         val a = PageStackBoard(
            Row(PageStack(0L))
         )
         val b = PageStackBoard(
            Row(PageStack(1L))
         )
         a treeEquals b
      }

      assertTrue {
         val a = PageStackBoard(
            Row(
               Column(PageStack(0L)),
            )
         )
         val b = PageStackBoard(
            Row(
               Column(PageStack(0L)),
            )
         )
         a treeEquals b
      }

      assertFalse {
         val a = PageStackBoard(
            Row(
               Column(PageStack(0L)),
            )
         )
         val b = PageStackBoard(
            Row(
               Row(PageStack(0L)),
            )
         )
         a treeEquals b
      }

      assertFalse {
         val a = PageStackBoard(
            Row(
               Column(PageStack(0L)),
            )
         )
         val b = PageStackBoard(
            Row(
               PageStack(0L),
            )
         )
         a treeEquals b
      }

      assertFalse {
         val a = PageStackBoard(
            Row(
               Column(PageStack(0L)),
            )
         )
         val b = PageStackBoard(
            Row(
               Column(PageStack(0L)),
               PageStack(1L),
            )
         )
         a treeEquals b
      }

      assertFalse {
         val a = PageStackBoard(
            Row(
               Column(PageStack(0L)),
               PageStack(1L),
            )
         )
         val b = PageStackBoard(
            Row(
               Column(PageStack(0L)),
            )
         )
         a treeEquals b
      }

      assertTrue {
         val a = PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
               ),
               Row(
                  Column(
                     PageStack(1L),
                  ),
                  PageStack(2L),
               ),
            )
         )
         val b = PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
               ),
               Row(
                  Column(
                     PageStack(1L),
                  ),
                  PageStack(2L),
               ),
            )
         )
         a treeEquals b
      }
   }

   @Test
   fun column_insert() {
      var column = createColumn(2)
      val inserted = createColumn(0)
      column = column.inserted(1, inserted)

      assertEquals(3, column.childCount)
      assertPage(0, column[0])
      assertSame(inserted, column[1])
      assertPage(1, column[2])
   }

   @Test
   fun column_insertFirst() {
      var column = createColumn(2)
      val inserted = createColumn(0)
      column = column.inserted(0, inserted)

      assertEquals(3, column.childCount)
      assertSame(inserted, column[0])
      assertPage(0, column[1])
      assertPage(1, column[2])
   }

   @Test
   fun column_insertLast() {
      var column = createColumn(2)
      val inserted = createColumn(0)
      column = column.inserted(2, inserted)

      assertEquals(3, column.childCount)
      assertPage(0, column[0])
      assertPage(1, column[1])
      assertSame(inserted, column[2])
   }

   @Test
   fun column_insertOutOfBounds() {
      val column = createColumn(2)
      val inserted = createColumn(0)

      assertFails {
         column.inserted(3, inserted)
      }
   }

   @Test
   fun column_insertNegativeIndex() {
      val column = createColumn(2)
      val inserted = createColumn(0)

      assertFails {
         column.inserted(-1, inserted)
      }
   }

   @Test
   fun column_remove() {
      var column = createColumn(3)
      column = column.removed(1)

      assertEquals(2, column.childCount)
      assertPage(0, column[0])
      assertPage(2, column[1])
   }

   @Test
   fun column_removeFirst() {
      var column = createColumn(3)
      column = column.removed(0)

      assertEquals(2, column.childCount)
      assertPage(1, column[0])
      assertPage(2, column[1])
   }

   @Test
   fun column_removeLast() {
      var column = createColumn(3)
      column = column.removed(2)

      assertEquals(2, column.childCount)
      assertPage(0, column[0])
      assertPage(1, column[1])
   }

   @Test
   fun column_removeOutOfBounds() {
      val column = createColumn(3)

      assertFails {
         column.removed(3)
      }
   }

   @Test
   fun column_removeNegativeIndex() {
      val column = createColumn(3)

      assertFails {
         column.removed(-1)
      }
   }

   @Test
   fun column_replace() {
      var column = createColumn(3)
      val replaced = createColumn(0)
      column = column.replaced(1, replaced)

      assertEquals(3, column.childCount)
      assertPage(0, column[0])
      assertSame(replaced, column[1])
      assertPage(2, column[2])
   }

   @Test
   fun column_replaceFirst() {
      var column = createColumn(3)
      val replaced = createColumn(0)
      column = column.replaced(0, replaced)

      assertEquals(3, column.childCount)
      assertSame(replaced, column[0])
      assertPage(1, column[1])
      assertPage(2, column[2])
   }

   @Test
   fun column_replaceLast() {
      var column = createColumn(3)
      val replaced = createColumn(0)
      column = column.replaced(2, replaced)

      assertEquals(3, column.childCount)
      assertPage(0, column[0])
      assertPage(1, column[1])
      assertSame(replaced, column[2])
   }

   @Test
   fun column_replaceOutOfBounds() {
      val column = createColumn(3)
      val replaced = createColumn(0)

      assertFails {
         column.replaced(3, replaced)
      }
   }

   @Test
   fun column_replaceNegativeIndex() {
      val column = createColumn(3)
      val replaced = createColumn(0)

      assertFails {
         column.replaced(-1, replaced)
      }
   }

   @Test
   fun column_insertImmutability() {
      val column = createColumn(2)

      val insertedColumn = column.inserted(0, createPageStackElement(2))
      assertNotSame(column, insertedColumn)
      assertEquals(2, column.childCount)
      assertPage(0, column[0])
      assertPage(1, column[1])
      assertEquals(3, insertedColumn.childCount)
   }

   @Test
   fun column_removeImmutability() {
      val column = createColumn(2)

      val removedColumn = column.removed(0)
      assertNotSame(column, removedColumn)
      assertEquals(2, column.childCount)
      assertPage(0, column[0])
      assertPage(1, column[1])
      assertEquals(1, removedColumn.childCount)
   }

   @Test
   fun column_replaceImmutability() {
      val column = createColumn(2)

      val replacedColumn = column.replaced(0, createPageStackElement(2))
      assertNotSame(column, replacedColumn)
      assertEquals(2, column.childCount)
      assertPage(0, column[0])
      assertPage(1, column[1])
      assertEquals(2, replacedColumn.childCount)
   }

   @Test
   fun row_insert() {
      var row = createRow(2)
      val inserted = createColumn(0)
      row = row.inserted(1, inserted)

      assertEquals(3, row.childCount)
      assertPage(0, row[0])
      assertSame(inserted, row[1])
      assertPage(1, row[2])
   }

   @Test
   fun row_insertFirst() {
      var row = createRow(2)
      val inserted = createColumn(0)
      row = row.inserted(0, inserted)

      assertEquals(3, row.childCount)
      assertSame(inserted, row[0])
      assertPage(0, row[1])
      assertPage(1, row[2])
   }

   @Test
   fun row_insertLast() {
      var row = createRow(2)
      val inserted = createColumn(0)
      row = row.inserted(2, inserted)

      assertEquals(3, row.childCount)
      assertPage(0, row[0])
      assertPage(1, row[1])
      assertSame(inserted, row[2])
   }

   @Test
   fun row_insertOutOfBounds() {
      val row = createRow(2)
      val inserted = createColumn(0)

      assertFails {
         row.inserted(3, inserted)
      }
   }

   @Test
   fun row_insertNegativeIndex() {
      val row = createRow(2)
      val inserted = createColumn(0)

      assertFails {
         row.inserted(-1, inserted)
      }
   }

   @Test
   fun row_remove() {
      var row = createRow(3)
      row = row.removed(1)

      assertEquals(2, row.childCount)
      assertPage(0, row[0])
      assertPage(2, row[1])
   }

   @Test
   fun row_removeFirst() {
      var row = createRow(3)
      row = row.removed(0)

      assertEquals(2, row.childCount)
      assertPage(1, row[0])
      assertPage(2, row[1])
   }

   @Test
   fun row_removeLast() {
      var row = createRow(3)
      row = row.removed(2)

      assertEquals(2, row.childCount)
      assertPage(0, row[0])
      assertPage(1, row[1])
   }

   @Test
   fun row_removeOutOfBounds() {
      val row = createRow(3)

      assertFails {
         row.removed(3)
      }
   }

   @Test
   fun row_removeNegativeIndex() {
      val row = createRow(3)

      assertFails {
         row.removed(-1)
      }
   }

   @Test
   fun row_replace() {
      var row = createRow(3)
      val replaced = createColumn(0)
      row = row.replaced(1, replaced)

      assertEquals(3, row.childCount)
      assertPage(0, row[0])
      assertSame(replaced, row[1])
      assertPage(2, row[2])
   }

   @Test
   fun row_replaceFirst() {
      var row = createRow(3)
      val replaced = createColumn(0)
      row = row.replaced(0, replaced)

      assertEquals(3, row.childCount)
      assertSame(replaced, row[0])
      assertPage(1, row[1])
      assertPage(2, row[2])
   }

   @Test
   fun row_replaceLast() {
      var row = createRow(3)
      val replaced = createColumn(0)
      row = row.replaced(2, replaced)

      assertEquals(3, row.childCount)
      assertPage(0, row[0])
      assertPage(1, row[1])
      assertSame(replaced, row[2])
   }

   @Test
   fun row_replaceOutOfBounds() {
      val row = createRow(3)
      val replaced = createColumn(0)

      assertFails {
         row.replaced(3, replaced)
      }
   }

   @Test
   fun row_replaceNegativeIndex() {
      val row = createRow(3)
      val replaced = createColumn(0)

      assertFails {
         row.replaced(-1, replaced)
      }
   }

   @Test
   fun row_insertImmutability() {
      val row = createRow(2)

      val insertedRow = row.inserted(0, createPageStackElement(2))
      assertNotSame(row, insertedRow)
      assertEquals(2, row.childCount)
      assertPage(0, row[0])
      assertPage(1, row[1])
      assertEquals(3, insertedRow.childCount)
   }

   @Test
   fun row_removeImmutability() {
      val row = createRow(2)

      val removedRow = row.removed(0)
      assertNotSame(row, removedRow)
      assertEquals(2, row.childCount)
      assertPage(0, row[0])
      assertPage(1, row[1])
      assertEquals(1, removedRow.childCount)
   }

   @Test
   fun row_replaceImmutability() {
      val row = createRow(2)

      val replacedRow = row.replaced(0, createPageStackElement(2))
      assertNotSame(row, replacedRow)
      assertEquals(2, row.childCount)
      assertPage(0, row[0])
      assertPage(1, row[1])
      assertEquals(2, replacedRow.childCount)
   }

   private fun Row(vararg children: PageStackBoard.LayoutElement)
         = PageStackBoard.Row(children.toList())
   private fun Column(vararg children: PageStackBoard.LayoutElement)
         = PageStackBoard.Column(children.toList())
   private fun PageStack(id: Long): PageStackBoard.PageStack {
      val pageStack = PageStack(
         PageStack.Id(id),
         PageStack.SavedPageState(
            PageStack.PageId(id),
            PageImpl(0)
         )
      )
      return PageStackBoard.PageStack(
         PageStackBoard.PageStackId(pageStack.id.value),
         WritableCache(pageStack)
      )
   }

   @Test
   fun getAsTreeIndex() {
      val pageStackBoard = PageStackBoard(
         Row(
            PageStack(0L),
            Column(
               PageStack(2L),
               Column(
                  PageStack(3L),
                  PageStack(5L),
               ),
               PageStack(6L),
               Row(
                  PageStack(8L),
               ),
            ),
            PageStack(11L),
            Row(
               PageStack(12L),
               Column(
                  PageStack(14L),
               ),
               PageStack(17L),
               Row(
                  PageStack(18L),
                  PageStack(20L),
               ),
            ),
         )
      )

      assertEquals( 0L, pageStackBoard[ 0].pageStackCache.value.id.value)
      assertEquals( 2L, pageStackBoard[ 1].pageStackCache.value.id.value)
      assertEquals( 3L, pageStackBoard[ 2].pageStackCache.value.id.value)
      assertEquals( 5L, pageStackBoard[ 3].pageStackCache.value.id.value)
      assertEquals( 6L, pageStackBoard[ 4].pageStackCache.value.id.value)
      assertEquals( 8L, pageStackBoard[ 5].pageStackCache.value.id.value)
      assertEquals(11L, pageStackBoard[ 6].pageStackCache.value.id.value)
      assertEquals(12L, pageStackBoard[ 7].pageStackCache.value.id.value)
      assertEquals(14L, pageStackBoard[ 8].pageStackCache.value.id.value)
      assertEquals(17L, pageStackBoard[ 9].pageStackCache.value.id.value)
      assertEquals(18L, pageStackBoard[10].pageStackCache.value.id.value)
      assertEquals(20L, pageStackBoard[11].pageStackCache.value.id.value)
      assertFails { pageStackBoard[12] }
      assertFails { pageStackBoard[-1] }
   }

   @Test
   fun sequence() {
      val pageStackBoard = PageStackBoard(
         Row(
            PageStack(0L),
            Column(
               PageStack(2L),
               Column(
                  PageStack(3L),
                  PageStack(5L),
               ),
               PageStack(6L),
               Row(
                  PageStack(8L),
               ),
            ),
            PageStack(11L),
            Row(
               PageStack(12L),
               Column(
                  PageStack(14L),
               ),
               PageStack(17L),
               Row(
                  PageStack(18L),
                  PageStack(20L),
               ),
            ),
         )
      )

      val expected = listOf(0L, 2L, 3L, 5L, 6L, 8L, 11L, 12L, 14L, 17L, 18L, 20L)

      val actual = pageStackBoard.sequence()
         .map { it.pageStackCache.value.id.value }
         .toList()

      assertContentEquals(expected, actual)
   }

   @Test
   fun removeFromBoard() {
      val pageStackBoard = PageStackBoard(
         Row(
            Column(
               PageStack(0L),
               Column(
                  PageStack(1L),
                  PageStack(2L),
               ),
               PageStack(3L),
               Row(PageStack(4L)),
            ),
            Row(
               Column(PageStack(5L)),
               PageStack(6L),
               Row(
                  PageStack(7L),
                  PageStack(8L),
               ),
               PageStack(9L),
            ),
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(0L)) treeEquals PageStackBoard(
            Row(
               Column(
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(1L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(2L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(3L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(4L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(5L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(6L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(7L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(8L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(8L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                  ),
                  PageStack(9L),
               ),
            )
         )
      )

      assertTrue(
         pageStackBoard.removed(PageStackBoard.PageStackId(9L)) treeEquals PageStackBoard(
            Row(
               Column(
                  PageStack(0L),
                  Column(
                     PageStack(1L),
                     PageStack(2L),
                  ),
                  PageStack(3L),
                  Row(PageStack(4L)),
               ),
               Row(
                  Column(PageStack(5L)),
                  PageStack(6L),
                  Row(
                     PageStack(7L),
                     PageStack(8L),
                  ),
               ),
            )
         )
      )

      assertFails {
         pageStackBoard.removed(-1)
         pageStackBoard.removed(10)
      }
   }
}
