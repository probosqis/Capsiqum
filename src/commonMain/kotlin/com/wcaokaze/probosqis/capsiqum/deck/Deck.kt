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

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

class Deck<out T>(val rootRow: Row<T>) {
   constructor(children: List<Layout<T>>) : this(Row(children))
   constructor(vararg children: Layout<T>) : this(Row(*children))

   sealed class Layout<out T>

   class Card<out T>(val content: T) : Layout<T>() {
      override fun hashCode() = content.hashCode()
      override fun equals(other: Any?) = other is Card<*> && other.content == content
   }

   sealed class LayoutParent<out T> : Layout<T>(), Iterable<Layout<T>> {
      abstract val children: List<Layout<T>>

      /** 葉ノードの数。すなわちこのサブツリーの子孫にある[Card]の数。 */
      internal abstract val leafCount: Int

      val childCount: Int get() = children.size

      protected fun countLeafs(children: List<Layout<*>>): Int {
         return children.sumOf {
            when (it) {
               is Card -> 1
               is LayoutParent -> it.leafCount
            }
         }
      }

      operator fun get(index: Int): Layout<T> = children[index]
      override operator fun iterator(): Iterator<Layout<T>> = children.iterator()
   }

   class Column<out T>(children: List<Layout<T>>) : LayoutParent<T>() {
      constructor(vararg children: Layout<T>) : this(persistentListOf(*children))

      override val children: List<Layout<T>> = children.toImmutableList()
      override val leafCount: Int = countLeafs(children)

      override fun hashCode() = children.hashCode()
      override fun equals(other: Any?) = other is Column<*> && other.children == children
   }

   class Row<out T>(children: List<Layout<T>>) : LayoutParent<T>() {
      constructor(vararg children: Layout<T>) : this(persistentListOf(*children))

      override val children: List<Layout<T>> = children.toImmutableList()
      override val leafCount: Int = countLeafs(children)

      override fun hashCode() = children.hashCode()
      override fun equals(other: Any?) = other is Row<*> && other.children == children
   }

   val cardCount = rootRow.leafCount
}

fun <T> Deck.Column<T>.inserted(index: Int, element: T): Deck.Column<T>
      = inserted(index, Deck.Card(element))

fun <T> Deck.Column<T>.inserted(index: Int, element: Deck.Layout<T>) = Deck.Column(
   buildList(capacity = children.size + 1) {
      addAll(children.subList(0, index))
      add(element)
      addAll(children.subList(index, children.size))
   }
)

fun <T> Deck.Column<T>.removed(index: Int) = Deck.Column(
   buildList(capacity = children.size) {
      addAll(children.subList(0, index))
      addAll(children.subList(index + 1, children.size))
   }
)

fun <T> Deck.Column<T>.replaced(index: Int, element: T): Deck.Column<T>
      = replaced(index, Deck.Card(element))

fun <T> Deck.Column<T>.replaced(index: Int, element: Deck.Layout<T>) = Deck.Column(
   buildList(capacity = children.size) {
      addAll(children)
      set(index, element)
   }
)

fun <T> Deck.Row<T>.inserted(index: Int, element: T): Deck.Row<T>
      = inserted(index, Deck.Card(element))

fun <T> Deck.Row<T>.inserted(index: Int, element: Deck.Layout<T>) = Deck.Row(
   buildList(capacity = children.size + 1) {
      addAll(children.subList(0, index))
      add(element)
      addAll(children.subList(index, children.size))
   }
)

fun <T> Deck.Row<T>.removed(index: Int) = Deck.Row(
   buildList(capacity = children.size) {
      addAll(children.subList(0, index))
      addAll(children.subList(index + 1, children.size))
   }
)

fun <T> Deck.Row<T>.replaced(index: Int, element: T): Deck.Row<T>
      = replaced(index, Deck.Card(element))

fun <T> Deck.Row<T>.replaced(index: Int, element: Deck.Layout<T>) = Deck.Row(
   buildList(capacity = children.size) {
      addAll(children)
      set(index, element)
   }
)

operator fun <T> Deck<T>.get(index: Int): Deck.Card<T> {
   fun Deck.LayoutParent<T>.findSubtree(indexInSubtree: Int): Deck.Card<T> {
      assert(indexInSubtree >= 0)

      var leafIndex = 0
      for (node in this) {
         when (node) {
            is Deck.Card -> {
               if (leafIndex == indexInSubtree) { return node }
               leafIndex++
            }
            is Deck.LayoutParent -> {
               if (leafIndex + node.leafCount > indexInSubtree) {
                  return node.findSubtree(indexInSubtree - leafIndex)
               }
               leafIndex += node.leafCount
            }
         }
      }

      throw IndexOutOfBoundsException(
         "card count: $leafIndex, specified index: $index")
   }

   if (index < 0) { throw IndexOutOfBoundsException("specified index: $index") }

   return rootRow.findSubtree(index)
}

private fun <T, P : Deck.LayoutParent<T>> P.removed(index: Int): P {
   fun Deck.LayoutParent<T>.impl(index: Int): Deck.LayoutParent<T> {
      return when (this) {
         is Deck.Column -> removed(index)
         is Deck.Row    -> removed(index)
      }
   }
   @Suppress("UNCHECKED_CAST")
   return impl(index) as P
}

private fun <T, P : Deck.LayoutParent<T>>
      P.replaced(index: Int, element: Deck.Layout<T>): P
{
   fun Deck.LayoutParent<T>.impl(index: Int): Deck.LayoutParent<T> {
      return when (this) {
         is Deck.Column -> replaced(index, element)
         is Deck.Row    -> replaced(index, element)
      }
   }
   @Suppress("UNCHECKED_CAST")
   return impl(index) as P
}

fun <T> Deck<T>.removed(index: Int): Deck<T> {
   fun <P : Deck.LayoutParent<T>> P.removedSubtree(indexInSubtree: Int): P {
      assert(indexInSubtree >= 0)

      var leafIndex = 0
      for ((childIndex, node) in this.withIndex()) {
         when (node) {
            is Deck.Card -> {
               if (leafIndex == indexInSubtree) { return this.removed(childIndex) }
               leafIndex++
            }
            is Deck.LayoutParent -> {
               if (leafIndex + node.leafCount > indexInSubtree) {
                  val removedSubtree
                        = node.removedSubtree(indexInSubtree - leafIndex)
                  return if (removedSubtree.childCount > 0) {
                     this.replaced(childIndex, removedSubtree)
                  } else {
                     this.removed(childIndex)
                  }
               }
               leafIndex += node.leafCount
            }
         }
      }

      throw IndexOutOfBoundsException(
         "card count: $leafIndex, specified index: $index")
   }

   if (index < 0) { throw IndexOutOfBoundsException("specified index: $index") }

   return Deck(rootRow.removedSubtree(index))
}

fun <T> Deck<T>.sequence(): Sequence<Deck.Card<T>> {
   suspend fun SequenceScope<Deck.Card<T>>
         .yieldChildren(parent: Deck.LayoutParent<T>)
   {
      for (node in parent) {
         when (node) {
            is Deck.Card         -> yield(node)
            is Deck.LayoutParent -> yieldChildren(node)
         }
      }
   }

   return sequence {
      yieldChildren(rootRow)
   }
}
