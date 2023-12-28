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

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntSize
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Base64

private const val TYPE_NULL                 =  0
private const val TYPE_BOOLEAN              =  1
private const val TYPE_BOOLEAN_ARRAY        =  2
private const val TYPE_CHAR                 =  3
private const val TYPE_CHAR_ARRAY           =  4
private const val TYPE_BYTE                 =  5
private const val TYPE_BYTE_ARRAY           =  6
private const val TYPE_UNSIGNED_BYTE        =  7
private const val TYPE_UNSIGNED_BYTE_ARRAY  =  8
private const val TYPE_SHORT                =  9
private const val TYPE_SHORT_ARRAY          = 10
private const val TYPE_UNSIGNED_SHORT       = 11
private const val TYPE_UNSIGNED_SHORT_ARRAY = 12
private const val TYPE_INT                  = 13
private const val TYPE_INT_ARRAY            = 14
private const val TYPE_UNSIGNED_INT         = 15
private const val TYPE_UNSIGNED_INT_ARRAY   = 16
private const val TYPE_LONG                 = 17
private const val TYPE_LONG_ARRAY           = 18
private const val TYPE_UNSIGNED_LONG        = 19
private const val TYPE_UNSIGNED_LONG_ARRAY  = 20
private const val TYPE_FLOAT                = 21
private const val TYPE_FLOAT_ARRAY          = 22
private const val TYPE_DOUBLE               = 23
private const val TYPE_DOUBLE_ARRAY         = 24
private const val TYPE_STRING               = 25
private const val TYPE_PAIR                 = 26
private const val TYPE_TRIPLE               = 27
private const val TYPE_INT_SIZE             = 28
private const val TYPE_SIZE                 = 29
private const val TYPE_LIST                 = 32
private const val TYPE_SET                  = 33
private const val TYPE_MAP                  = 34
private const val TYPE_SERIALIZABLE         = 36

internal actual class JsonElementSaver<T>
   actual constructor(
      private val saver: Saver<T, *>
   )
   : Saver<T, JsonElement>
{
   private val saverScope = object : SaverScope {
      override fun canBeSaved(value: Any): Boolean {
         return canBeSavedImpl(value)
      }

      private fun canBeSavedImpl(value: Any?): Boolean {
         @OptIn(ExperimentalUnsignedTypes::class)
         return when (value) {
            null,
            is Boolean, is BooleanArray, is Char, is CharArray,
            is Byte, is ByteArray, is UByte, is UByteArray,
            is Short, is ShortArray, is UShort, is UShortArray,
            is Int, is IntArray, is UInt, is UIntArray,
            is Long, is LongArray, is ULong, is ULongArray,
            is Float, is FloatArray, is Double, is DoubleArray -> true

            is String -> true

            is Pair<*, *>, is Triple<*, *, *> -> true

            is IntSize, is Size -> true

            is Array<*>  -> value.all(::canBeSavedImpl)
            is List<*>   -> value.all(::canBeSavedImpl)
            is Set<*>    -> value.all(::canBeSavedImpl)
            is Map<*, *> -> value.all { (key, value) ->
               canBeSavedImpl(key) && canBeSavedImpl(value)
            }

            is Serializable -> true

            else -> false
         }
      }
   }

   private fun typedElement(type: Int, element: JsonElement) = JsonArray(
      listOf(JsonPrimitive(type), element)
   )

   private fun pair(first: JsonElement, second: JsonElement) = JsonArray(
      listOf(first, second)
   )

   private fun toJsonElement(value: Any?): JsonElement {
      @OptIn(ExperimentalUnsignedTypes::class)
      return when (value) {
         null            -> typedElement(TYPE_NULL,                 JsonNull)
         is Boolean      -> typedElement(TYPE_BOOLEAN,              JsonPrimitive(value))
         is BooleanArray -> typedElement(TYPE_BOOLEAN_ARRAY,        JsonArray(value.map(::JsonPrimitive)))
         is Char         -> typedElement(TYPE_CHAR,                 JsonPrimitive(value.code))
         is CharArray    -> typedElement(TYPE_CHAR_ARRAY,           JsonArray(value.map { JsonPrimitive(it.code) }))
         is Byte         -> typedElement(TYPE_BYTE,                 JsonPrimitive(value))
         is ByteArray    -> typedElement(TYPE_BYTE_ARRAY,           JsonArray(value.map(::JsonPrimitive)))
         is UByte        -> typedElement(TYPE_UNSIGNED_BYTE,        JsonPrimitive(value.toByte()))
         is UByteArray   -> typedElement(TYPE_UNSIGNED_BYTE_ARRAY,  JsonArray(value.map { JsonPrimitive(it.toByte()) }))
         is Short        -> typedElement(TYPE_SHORT,                JsonPrimitive(value))
         is ShortArray   -> typedElement(TYPE_SHORT_ARRAY,          JsonArray(value.map(::JsonPrimitive)))
         is UShort       -> typedElement(TYPE_UNSIGNED_SHORT,       JsonPrimitive(value.toShort()))
         is UShortArray  -> typedElement(TYPE_UNSIGNED_SHORT_ARRAY, JsonArray(value.map { JsonPrimitive(it.toShort()) }))
         is Int          -> typedElement(TYPE_INT,                  JsonPrimitive(value))
         is IntArray     -> typedElement(TYPE_INT_ARRAY,            JsonArray(value.map(::JsonPrimitive)))
         is UInt         -> typedElement(TYPE_UNSIGNED_INT,         JsonPrimitive(value.toInt()))
         is UIntArray    -> typedElement(TYPE_UNSIGNED_INT_ARRAY,   JsonArray(value.map { JsonPrimitive(it.toInt()) }))
         is Long         -> typedElement(TYPE_LONG,                 JsonPrimitive(value))
         is LongArray    -> typedElement(TYPE_LONG_ARRAY,           JsonArray(value.map(::JsonPrimitive)))
         is ULong        -> typedElement(TYPE_UNSIGNED_LONG,        JsonPrimitive(value.toLong()))
         is ULongArray   -> typedElement(TYPE_UNSIGNED_LONG_ARRAY,  JsonArray(value.map { JsonPrimitive(it.toLong()) }))
         is Float        -> typedElement(TYPE_FLOAT,                JsonPrimitive(value))
         is FloatArray   -> typedElement(TYPE_FLOAT_ARRAY,          JsonArray(value.map(::JsonPrimitive)))
         is Double       -> typedElement(TYPE_DOUBLE,               JsonPrimitive(value))
         is DoubleArray  -> typedElement(TYPE_DOUBLE_ARRAY,         JsonArray(value.map(::JsonPrimitive)))

         is String -> typedElement(TYPE_STRING, JsonPrimitive(value))

         is Pair<*, *> -> typedElement(
            TYPE_PAIR,
            pair(toJsonElement(value.first), toJsonElement(value.second))
         )
         is Triple<*, *, *> -> typedElement(
            TYPE_TRIPLE,
            JsonArray(listOf(
               toJsonElement(value.first),
               toJsonElement(value.second),
               toJsonElement(value.third)
            ))
         )

         is IntSize            -> typedElement(TYPE_INT_SIZE,       pair(JsonPrimitive(value.width), JsonPrimitive(value.height)))
         is Size               -> typedElement(TYPE_SIZE,           pair(JsonPrimitive(value.width), JsonPrimitive(value.height)))

         is List<*>   -> typedElement(TYPE_LIST,  JsonArray(value.map(::toJsonElement)))
         is Set<*>    -> typedElement(TYPE_SET,   JsonArray(value.map(::toJsonElement)))
         is Map<*, *> -> typedElement(
            TYPE_MAP,
            JsonArray(
               value.map { (key, value) -> pair(toJsonElement(key), toJsonElement(value)) }
            )
         )

         is Serializable -> {
            val outputStream = ByteArrayOutputStream()
            ObjectOutputStream(outputStream).use {
               it.writeObject(value)
            }
            val serializedStr = Base64.getEncoder()
               .encodeToString(outputStream.toByteArray())

            typedElement(TYPE_SERIALIZABLE, JsonPrimitive(serializedStr))
         }

         else -> throw SerializationException()
      }
   }

   private fun fromJsonElement(element: JsonElement): Any? {
      if (element !is JsonArray) { throw SerializationException() }
      if (element.size != 2) { throw SerializationException() }

      val (typeElement, contentElement) = element
      if (typeElement !is JsonPrimitive) { throw SerializationException() }
      val type = typeElement.intOrNull ?: throw SerializationException()

      @OptIn(ExperimentalUnsignedTypes::class)
      return when (type) {
         TYPE_NULL -> {
            if (contentElement !is JsonNull) { throw SerializationException() }
            null
         }
         TYPE_BOOLEAN -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.booleanOrNull ?: throw SerializationException()
         }
         TYPE_BOOLEAN_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map {
                  if (it !is JsonPrimitive) { throw SerializationException() }
                  it.booleanOrNull ?: throw SerializationException()
               }
               .toBooleanArray()
         }
         TYPE_CHAR -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.intOrNull
               ?.takeIf { it in Char.MIN_VALUE.code .. Char.MAX_VALUE.code }
               ?.toChar()
               ?: throw SerializationException()
         }
         TYPE_CHAR_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.intOrNull
                     ?.takeIf { it in Char.MIN_VALUE.code .. Char.MAX_VALUE.code }
                     ?.toChar()
                     ?: throw SerializationException()
               }
               .toCharArray()
         }
         TYPE_BYTE -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toByteOrNull() ?: throw SerializationException()
         }
         TYPE_BYTE_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toByteOrNull() ?: throw SerializationException()
               }
               .toByteArray()
         }
         TYPE_UNSIGNED_BYTE -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toUByteOrNull() ?: throw SerializationException()
         }
         TYPE_UNSIGNED_BYTE_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toUByteOrNull() ?: throw SerializationException()
               }
               .toUByteArray()
         }
         TYPE_SHORT -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toShortOrNull() ?: throw SerializationException()
         }
         TYPE_SHORT_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toShortOrNull() ?: throw SerializationException()
               }
               .toShortArray()
         }
         TYPE_UNSIGNED_SHORT -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toUShortOrNull() ?: throw SerializationException()
         }
         TYPE_UNSIGNED_SHORT_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toUShortOrNull() ?: throw SerializationException()
               }
               .toUShortArray()
         }
         TYPE_INT -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toIntOrNull() ?: throw SerializationException()
         }
         TYPE_INT_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toIntOrNull() ?: throw SerializationException()
               }
               .toIntArray()
         }
         TYPE_UNSIGNED_INT -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toUIntOrNull() ?: throw SerializationException()
         }
         TYPE_UNSIGNED_INT_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toUIntOrNull() ?: throw SerializationException()
               }
               .toUIntArray()
         }
         TYPE_LONG -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toLongOrNull() ?: throw SerializationException()
         }
         TYPE_LONG_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toLongOrNull() ?: throw SerializationException()
               }
               .toLongArray()
         }
         TYPE_UNSIGNED_LONG -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toULongOrNull() ?: throw SerializationException()
         }
         TYPE_UNSIGNED_LONG_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toULongOrNull() ?: throw SerializationException()
               }
               .toULongArray()
         }
         TYPE_FLOAT -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toFloatOrNull() ?: throw SerializationException()
         }
         TYPE_FLOAT_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toFloatOrNull() ?: throw SerializationException()
               }
               .toFloatArray()
         }
         TYPE_DOUBLE -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            contentElement.content.toDoubleOrNull() ?: throw SerializationException()
         }
         TYPE_DOUBLE_ARRAY -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement
               .map { v ->
                  if (v !is JsonPrimitive) { throw SerializationException() }
                  v.content.toDoubleOrNull() ?: throw SerializationException()
               }
               .toDoubleArray()
         }
         TYPE_STRING -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            if (!contentElement.isString) { throw SerializationException() }
            contentElement.content
         }
         TYPE_PAIR -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            if (contentElement.size != 2) { throw SerializationException() }
            val (firstElement, secondElement) = contentElement
            Pair(fromJsonElement(firstElement), fromJsonElement(secondElement))
         }
         TYPE_TRIPLE -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            if (contentElement.size != 3) { throw SerializationException() }
            val (firstElement, secondElement, thirdElement) = contentElement
            Triple(
               fromJsonElement(firstElement),
               fromJsonElement(secondElement),
               fromJsonElement(thirdElement)
            )
         }
         TYPE_INT_SIZE -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            if (contentElement.size != 2) { throw SerializationException() }
            val (firstElement, secondElement) = contentElement
            if (firstElement  !is JsonPrimitive) { throw SerializationException() }
            if (secondElement !is JsonPrimitive) { throw SerializationException() }
            val width  = firstElement .content.toIntOrNull() ?: throw SerializationException()
            val height = secondElement.content.toIntOrNull() ?: throw SerializationException()
            IntSize(width, height)
         }
         TYPE_SIZE -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            if (contentElement.size != 2) { throw SerializationException() }
            val (firstElement, secondElement) = contentElement
            if (firstElement  !is JsonPrimitive) { throw SerializationException() }
            if (secondElement !is JsonPrimitive) { throw SerializationException() }
            val width  = firstElement .content.toFloatOrNull() ?: throw SerializationException()
            val height = secondElement.content.toFloatOrNull() ?: throw SerializationException()
            Size(width, height)
         }
         TYPE_LIST -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement.map(::fromJsonElement)
         }
         TYPE_SET -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement.map(::fromJsonElement).toSet()
         }
         TYPE_MAP -> {
            if (contentElement !is JsonArray) { throw SerializationException() }
            contentElement.associate { v ->
               if (v !is JsonArray) { throw SerializationException() }
               if (v.size != 2) { throw SerializationException() }
               val (firstElement, secondElement) = v
               Pair(fromJsonElement(firstElement), fromJsonElement(secondElement))
            }
         }
         TYPE_SERIALIZABLE -> {
            if (contentElement !is JsonPrimitive) { throw SerializationException() }
            if (!contentElement.isString) { throw SerializationException() }
            val serialized = Base64.getDecoder().decode(contentElement.content)
            ObjectInputStream(serialized.inputStream()).use {
               it.readObject()
            }
         }
         else -> throw SerializationException()
      }
   }

   override fun SaverScope.save(value: T): JsonElement? {
      return try {
         val saved = with (saver) {
            saverScope.save(value)
         }

         toJsonElement(saved)
      } catch (_: Exception) {
         null
      }
   }

   override fun restore(value: JsonElement): T? {
      return try {
         val saved = fromJsonElement(value) ?: return null

         @Suppress("UNCHECKED_CAST")
         (saver as Saver<T, Any>).restore(saved)
      } catch (_: Exception) {
         null
      }
   }
}
