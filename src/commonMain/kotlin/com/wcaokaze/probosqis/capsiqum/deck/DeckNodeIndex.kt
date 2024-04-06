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

class DeckNodeIndex(
   val parent: DeckNodeIndex?,
   val indexInLayout: Int,
   val nodeType: NodeType
) {
   enum class NodeType {
      CARD,
      COLUMN,
      ROW,
   }

   override fun hashCode(): Int {
      if (parent == null) { return indexInLayout }

      var h = parent.hashCode()
      h = h * 31 + indexInLayout
      h = h * 31 + nodeType.ordinal
      return h
   }

   override fun equals(other: Any?): Boolean {
      if (other !is DeckNodeIndex) { return false }
      if (indexInLayout != other.indexInLayout) { return false }
      if (nodeType != other.nodeType) { return false }
      return parent == other.parent
   }

   override fun toString() = "$parent-$nodeType($indexInLayout)"
}

class IndexedCard<out T>(
   val index: DeckNodeIndex,
   val card: Deck.Card<T>
) {
   operator fun component1() = index
   operator fun component2() = card

   override fun hashCode() = index.hashCode()

   override fun equals(other: Any?): Boolean {
      if (other !is IndexedCard<*>) { return false }
      if (card != other.card) { return false }
      return index == other.index
   }

   override fun toString() = "IndexedCard($index, $card)"
}

fun <T> Deck<T>.sequenceIndexed(): Sequence<IndexedCard<T>> {
   suspend fun SequenceScope<IndexedCard<T>>.yieldChildren(
      parent: Deck.LayoutParent<T>,
      parentIndex: DeckNodeIndex?
   ) {
      parent.forEachIndexed { index, node ->
         when (node) {
            is Deck.Card -> {
               val cardIndex = DeckNodeIndex(
                  parentIndex, index, DeckNodeIndex.NodeType.CARD)

               yield(
                  IndexedCard(cardIndex, node)
               )
            }
            is Deck.Column -> {
               val nodeIndex = DeckNodeIndex(
                  parentIndex, index, DeckNodeIndex.NodeType.COLUMN)

               yieldChildren(node, nodeIndex)
            }
            is Deck.Row -> {
               val nodeIndex = DeckNodeIndex(
                  parentIndex, index, DeckNodeIndex.NodeType.ROW)

               yieldChildren(node, nodeIndex)
            }
         }
      }
   }

   return sequence {
      yieldChildren(rootRow, parentIndex = null)
   }
}

fun <T> Deck<T>.sequence(): Sequence<Deck.Card<T>>
      = sequenceIndexed().map { it.card }
