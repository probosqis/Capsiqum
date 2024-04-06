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

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DeckTest {
   private infix fun <T> Deck<T>.treeEquals(other: Deck<T>): Boolean {
      fun subtreeEquals(
         expected: Deck.LayoutParent<T>,
         actual:   Deck.LayoutParent<T>
      ): Boolean {
         if (expected is Deck.Column && actual !is Deck.Column) { return false }
         if (expected is Deck.Row    && actual !is Deck.Row   ) { return false }

         if (expected.childCount != actual.childCount) { return false }

         for ((expectedChild, actualChild) in expected.zip(actual)) {
            when (expectedChild) {
               is Deck.Card -> {
                  if (actualChild !is Deck.Card) { return false }

                  val expectedContent = expectedChild.content
                  val actualContent   = actualChild  .content
                  if (expectedContent != actualContent) { return false }
               }

               is Deck.LayoutParent -> {
                  if (actualChild !is Deck.LayoutParent) { return false }
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
         val a = Deck(Deck.Card(0))
         val b = Deck(Deck.Card(0))
         a treeEquals b
      }

      assertFalse {
         val a = Deck(Deck.Card(0))
         val b = Deck(Deck.Card(1))
         a treeEquals b
      }

      assertTrue {
         val a = Deck(
            Deck.Column(Deck.Card(0)),
         )
         val b = Deck(
            Deck.Column(Deck.Card(0)),
         )
         a treeEquals b
      }

      assertFalse {
         val a = Deck(
            Deck.Column(Deck.Card(0)),
         )
         val b = Deck(
            Deck.Row(Deck.Card(0)),
         )
         a treeEquals b
      }

      assertFalse {
         val a = Deck(
            Deck.Column(Deck.Card(0)),
         )
         val b = Deck(
            Deck.Card(0),
         )
         a treeEquals b
      }

      assertFalse {
         val a = Deck(
            Deck.Column(Deck.Card(0)),
         )
         val b = Deck(
            Deck.Column(Deck.Card(0)),
            Deck.Card(1),
         )
         a treeEquals b
      }

      assertFalse {
         val a = Deck(
            Deck.Column(Deck.Card(0)),
            Deck.Card(1),
         )
         val b = Deck(
            Deck.Column(Deck.Card(0)),
         )
         a treeEquals b
      }

      assertTrue {
         val a = Deck(
            Deck.Column(
               Deck.Card(0),
            ),
            Deck.Row(
               Deck.Column(
                  Deck.Card(1),
               ),
               Deck.Card(2),
            ),
         )
         val b = Deck(
            Deck.Column(
               Deck.Card(0),
            ),
            Deck.Row(
               Deck.Column(
                  Deck.Card(1),
               ),
               Deck.Card(2),
            ),
         )
         a treeEquals b
      }
   }

   @Test
   fun column_insert() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Column(Deck.Card(2))
      column = column.inserted(1, inserted)

      assertEquals(3, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertSame(inserted, column[1])
      assertIs<Deck.Card<*>>(column[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun column_insertFirst() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Column(Deck.Card(2))
      column = column.inserted(0, inserted)

      assertEquals(3, column.childCount)
      assertSame(inserted, column[0])
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun column_insertLast() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Column(Deck.Card(2))
      column = column.inserted(2, inserted)

      assertEquals(3, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertSame(inserted, column[2])
   }

   @Test
   fun column_insertOutOfBounds() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Column(Deck.Card(2))

      assertFails {
         column.inserted(3, inserted)
      }
   }

   @Test
   fun column_insertNegativeIndex() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Column(Deck.Card(2))

      assertFails {
         column.inserted(-1, inserted)
      }
   }

   @Test
   fun column_remove() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      column = column.removed(1)

      assertEquals(2, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun column_removeFirst() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      column = column.removed(0)

      assertEquals(2, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun column_removeLast() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      column = column.removed(2)

      assertEquals(2, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun column_removeOutOfBounds() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))

      assertFails {
         column.removed(3)
      }
   }

   @Test
   fun column_removeNegativeIndex() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))

      assertFails {
         column.removed(-1)
      }
   }

   @Test
   fun column_replace() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Column(Deck.Card(4))
      column = column.replaced(1, replaced)

      assertEquals(3, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertSame(replaced, column[1])
      assertIs<Deck.Card<*>>(column[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun column_replaceFirst() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Column(Deck.Card(4))
      column = column.replaced(0, replaced)

      assertEquals(3, column.childCount)
      assertSame(replaced, column[0])
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertIs<Deck.Card<*>>(column[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun column_replaceLast() {
      var column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Column(Deck.Card(4))
      column = column.replaced(2, replaced)

      assertEquals(3, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertSame(replaced, column[2])
   }

   @Test
   fun column_replaceOutOfBounds() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Column(Deck.Card(4))

      assertFails {
         column.replaced(3, replaced)
      }
   }

   @Test
   fun column_replaceNegativeIndex() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Column(Deck.Card(4))

      assertFails {
         column.replaced(-1, replaced)
      }
   }

   @Test
   fun column_insertImmutability() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val insertedColumn = column.inserted(0, Deck.Card(2))

      assertNotSame(column, insertedColumn)
      assertEquals(2, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun column_removeImmutability() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val insertedColumn = column.removed(0)

      assertNotSame(column, insertedColumn)
      assertEquals(2, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun column_replaceImmutability() {
      val column = Deck.Column(Deck.Card(0), Deck.Card(1))
      val replacedColumn = column.replaced(0, Deck.Card(2))

      assertNotSame(column, replacedColumn)
      assertEquals(2, column.childCount)
      assertIs<Deck.Card<*>>(column[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(column[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun row_insert() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Row(Deck.Card(2))
      row = row.inserted(1, inserted)

      assertEquals(3, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertSame(inserted, row[1])
      assertIs<Deck.Card<*>>(row[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun row_insertFirst() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Row(Deck.Card(2))
      row = row.inserted(0, inserted)

      assertEquals(3, row.childCount)
      assertSame(inserted, row[0])
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun row_insertLast() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Row(Deck.Card(2))
      row = row.inserted(2, inserted)

      assertEquals(3, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertSame(inserted, row[2])
   }

   @Test
   fun row_insertOutOfBounds() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Row(Deck.Card(2))

      assertFails {
         row.inserted(3, inserted)
      }
   }

   @Test
   fun row_insertNegativeIndex() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val inserted = Deck.Row(Deck.Card(2))

      assertFails {
         row.inserted(-1, inserted)
      }
   }

   @Test
   fun row_remove() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      row = row.removed(1)

      assertEquals(2, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun row_removeFirst() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      row = row.removed(0)

      assertEquals(2, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun row_removeLast() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      row = row.removed(2)

      assertEquals(2, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun row_removeOutOfBounds() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))

      assertFails {
         row.removed(3)
      }
   }

   @Test
   fun row_removeNegativeIndex() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))

      assertFails {
         row.removed(-1)
      }
   }

   @Test
   fun row_replace() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Row(Deck.Card(4))
      row = row.replaced(1, replaced)

      assertEquals(3, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertSame(replaced, row[1])
      assertIs<Deck.Card<*>>(row[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun row_replaceFirst() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Row(Deck.Card(4))
      row = row.replaced(0, replaced)

      assertEquals(3, row.childCount)
      assertSame(replaced, row[0])
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertIs<Deck.Card<*>>(row[2])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(2, it) }
   }

   @Test
   fun row_replaceLast() {
      var row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Row(Deck.Card(4))
      row = row.replaced(2, replaced)

      assertEquals(3, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
      assertSame(replaced, row[2])
   }

   @Test
   fun row_replaceOutOfBounds() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Row(Deck.Card(4))

      assertFails {
         row.replaced(3, replaced)
      }
   }

   @Test
   fun row_replaceNegativeIndex() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1), Deck.Card(2))
      val replaced = Deck.Row(Deck.Card(4))

      assertFails {
         row.replaced(-1, replaced)
      }
   }

   @Test
   fun row_insertImmutability() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val insertedRow = row.inserted(0, Deck.Card(2))

      assertNotSame(row, insertedRow)
      assertEquals(2, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun row_removeImmutability() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val insertedRow = row.removed(0)

      assertNotSame(row, insertedRow)
      assertEquals(2, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun row_replaceImmutability() {
      val row = Deck.Row(Deck.Card(0), Deck.Card(1))
      val replacedRow = row.replaced(0, Deck.Card(2))

      assertNotSame(row, replacedRow)
      assertEquals(2, row.childCount)
      assertIs<Deck.Card<*>>(row[0])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(0, it) }
      assertIs<Deck.Card<*>>(row[1])
         .let { assertIs<Int>(it.content) }
         .let { assertEquals(1, it) }
   }

   @Test
   fun getAsTreeIndex() {
      val deck = Deck(
         Deck.Card(0),
         Deck.Column(
            Deck.Card(2),
            Deck.Column(
               Deck.Card(3),
               Deck.Card(5),
            ),
            Deck.Card(6),
            Deck.Row(
               Deck.Card(8),
            ),
         ),
         Deck.Card(11),
         Deck.Row(
            Deck.Card(12),
            Deck.Column(
               Deck.Card(14),
            ),
            Deck.Card(17),
            Deck.Row(
               Deck.Card(18),
               Deck.Card(20),
            ),
         ),
      )

      assertEquals( 0, deck[ 0].content)
      assertEquals( 2, deck[ 1].content)
      assertEquals( 3, deck[ 2].content)
      assertEquals( 5, deck[ 3].content)
      assertEquals( 6, deck[ 4].content)
      assertEquals( 8, deck[ 5].content)
      assertEquals(11, deck[ 6].content)
      assertEquals(12, deck[ 7].content)
      assertEquals(14, deck[ 8].content)
      assertEquals(17, deck[ 9].content)
      assertEquals(18, deck[10].content)
      assertEquals(20, deck[11].content)
      assertFails { deck[12] }
      assertFails { deck[-1] }
   }

   @Test
   fun sequenceIndexed() {
      fun card  (i: Int) = DeckNodeIndex(null, i, DeckNodeIndex.NodeType.CARD)
      fun column(i: Int) = DeckNodeIndex(null, i, DeckNodeIndex.NodeType.COLUMN)
      fun row   (i: Int) = DeckNodeIndex(null, i, DeckNodeIndex.NodeType.ROW)
      operator fun DeckNodeIndex.minus(child: DeckNodeIndex)
            = DeckNodeIndex(this, child.indexInLayout, child.nodeType)
      infix fun DeckNodeIndex.to(cardContent: Int)
            = IndexedCard(this, Deck.Card(cardContent))

      val deck = Deck(
         Deck.Card(0),
         Deck.Column(
            Deck.Card(2),
            Deck.Column(
               Deck.Card(3),
               Deck.Card(5),
            ),
            Deck.Card(6),
            Deck.Row(
               Deck.Card(8),
            ),
         ),
         Deck.Card(11),
         Deck.Row(
            Deck.Card(12),
            Deck.Column(
               Deck.Card(14),
            ),
            Deck.Card(17),
            Deck.Row(
               Deck.Card(18),
               Deck.Card(20),
            ),
         ),
      )

      val expected = listOf(
         card(0)                      to  0,
         column(1)-card(0)            to  2,
         column(1)-column(1)-card(0)  to  3,
         column(1)-column(1)-card(1)  to  5,
         column(1)-card(2)            to  6,
         column(1)-row(3)-card(0)     to  8,
         card(2)                      to 11,
         row(3)-card(0)               to 12,
         row(3)-column(1)-card(0)     to 14,
         row(3)-card(2)               to 17,
         row(3)-row(3)-card(0)        to 18,
         row(3)-row(3)-card(1)        to 20,
      )
      val actual = deck.sequenceIndexed().toList()

      assertContentEquals(expected, actual)
   }

   @Test
   fun removeFromDeck() {
      val deck = Deck(
         Deck.Column(
            Deck.Card(0),
            Deck.Column(
               Deck.Card(1),
               Deck.Card(2),
            ),
            Deck.Card(3),
            Deck.Row(Deck.Card(4)),
         ),
         Deck.Row(
            Deck.Column(Deck.Card(5)),
            Deck.Card(6),
            Deck.Row(
               Deck.Card(7),
               Deck.Card(8),
            ),
            Deck.Card(9),
         ),
      )

      assertTrue(
         deck.removed(0) treeEquals Deck(
            Deck.Column(
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(1) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(2) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(3) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(4) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(5) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(6) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(7) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(8),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(8) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
               ),
               Deck.Card(9),
            ),
         )
      )

      assertTrue(
         deck.removed(9) treeEquals Deck(
            Deck.Column(
               Deck.Card(0),
               Deck.Column(
                  Deck.Card(1),
                  Deck.Card(2),
               ),
               Deck.Card(3),
               Deck.Row(Deck.Card(4)),
            ),
            Deck.Row(
               Deck.Column(Deck.Card(5)),
               Deck.Card(6),
               Deck.Row(
                  Deck.Card(7),
                  Deck.Card(8),
               ),
            ),
         )
      )

      assertFails {
         deck.removed(-1)
         deck.removed(10)
      }
   }
}
