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

import android.os.Bundle
import android.util.Base64
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.long
import kotlinx.serialization.json.putJsonArray
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class PageStateSaverTest {
   @get:Rule
   val rule = createComposeRule()

   private class SerializableClass(val i: Int) : Serializable

   @Test
   fun serializer_getValue() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()

         // なにもsetContentしてないとwaitForIdleがめちゃくちゃ遅くなる
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {
         put("key", JsonPrimitive(42))
      })

      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
      val savedState = saver.save("key", Int.serializer()) { fail() }

      assertEquals(42, savedState.value)

      cache.value = buildJsonObject {
         put("key", JsonPrimitive(0))
      }

      assertEquals(0, savedState.value)
   }

   @Test
   fun serializer_initialize() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})

      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
      val savedState = saver.save("key", Int.serializer()) { 42 }

      assertEquals(1, cache.value.size)
      cache.value["key"].let {
         assertNotNull(it)
         assertIs<JsonPrimitive>(it)
         assertEquals(42, it.int)
      }

      assertEquals(42, savedState.value)

      cache.value = buildJsonObject {}
      assertFalse(cache.value.containsKey("key"))
      assertEquals(42, savedState.value)
      cache.value["key"].let {
         assertNotNull(it)
         assertIs<JsonPrimitive>(it)
         assertEquals(42, it.int)
      }

      cache.value = buildJsonObject {
         put("key", JsonPrimitive("Lorem ipsum"))
      }
      assertEquals(42, savedState.value)
      cache.value["key"].let {
         assertNotNull(it)
         assertIs<JsonPrimitive>(it)
         assertEquals(42, it.int)
      }
   }

   @Test
   fun serializer_recover() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})

      run {
         val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
         val savedState = saver.save("key", Int.serializer(), init = { 42 }, recover = { fail() })

         assertEquals(42, savedState.value)
      }

      run {
         val saver = PageState.StateSaver(cache, wasCacheDeleted = true, coroutineScope)
         val savedState = saver.save("key", Int.serializer(), init = { fail() }, recover = { 42 })

         assertEquals(42, savedState.value)
      }
   }

   @Test
   fun serializer_setValue() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {
         put("key", JsonPrimitive(3))
      })

      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
      val savedState = saver.save("key", Int.serializer()) { fail() }

      savedState.value = 42

      cache.value["key"].let {
         assertNotNull(it)
         assertIs<JsonPrimitive>(it)
         assertEquals(42, it.int)
      }
      assertEquals(42, savedState.value)
   }

   @Test
   fun serializer_nullable() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})
      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)

      val savedState = saver.save("key", Int.serializer().nullable) { 42 }
      assertEquals(42, savedState.value)
      cache.value["key"].let {
         assertNotNull(it)
         assertIs<JsonPrimitive>(it)
         assertEquals(42, it.int)
      }

      savedState.value = null
      assertEquals(null, savedState.value)
      cache.value["key"].let {
         assertNotNull(it)
         assertIs<JsonNull>(it)
      }
   }

   private fun getTypedValue(element: JsonElement?, type: Int): JsonElement? {
      if (element == null) { return null }
      if (element !is JsonArray) { return null }
      if (element.size != 2) { return null }

      val typeElement = element[0]
      if (typeElement !is JsonPrimitive) { return null }
      if (typeElement.isString) { return null }
      if (typeElement.int != type) { return null }

      return element[1]
   }

   private fun isTypedValue(element: JsonElement?, type: Int): Boolean
         = getTypedValue(element, type) != null

   private fun assertTypedElement(
      element: JsonElement?,
      type: Int,
      assertion: (JsonElement) -> Unit
   ) {
      val valueElement = getTypedValue(element, type)
      assertNotNull(valueElement)
      assertion(valueElement)
   }

   private fun isIntElement(element: JsonElement?, value: Int): Boolean {
      if (element !is JsonPrimitive) { return false }
      if (element.isString) { return false }
      return element.int == value
   }

   private fun ComposeContentTestRule.waitForSnapshotFlow(
      condition: (Duration) -> Boolean
   ) {
      val startTime = Clock.System.now()

      waitUntil(timeoutMillis = 3000) {
         waitForIdle()
         condition(Clock.System.now() - startTime)
      }
   }

   @OptIn(ExperimentalUnsignedTypes::class)
   @Test
   fun saver_restoredSaveableInstance() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {
         putJsonArray("key") {
            add(JsonPrimitive(32)) // LIST
            addJsonArray {
               addJsonArray {
                  add(JsonPrimitive(0)) // NULL
                  add(JsonNull)
               }
               addJsonArray {
                  add(JsonPrimitive(1)) // BOOLEAN
                  add(JsonPrimitive(true))
               }
               addJsonArray {
                  add(JsonPrimitive(2)) // BOOLEAN_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(true))
                     add(JsonPrimitive(true))
                     add(JsonPrimitive(false))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(3)) // CHAR
                  add(JsonPrimitive('A'.code))
               }
               addJsonArray {
                  add(JsonPrimitive(4)) // CHAR_ARRAY
                  addJsonArray {
                     add(JsonPrimitive('L'.code))
                     add(JsonPrimitive('i'.code))
                     add(JsonPrimitive('p'.code))
                     add(JsonPrimitive('s'.code))
                     add(JsonPrimitive('u'.code))
                     add(JsonPrimitive('m'.code))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(5)) // BYTE
                  add(JsonPrimitive(3))
               }
               addJsonArray {
                  add(JsonPrimitive(6)) // BYTE_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(3))
                     add(JsonPrimitive(3))
                     add(JsonPrimitive(4))
                     add(JsonPrimitive(2))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(7)) // UNSIGNED_BYTE
                  add(JsonPrimitive(5))
               }
               addJsonArray {
                  add(JsonPrimitive(8)) // UNSIGNED_BYTE_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(1))
                     add(JsonPrimitive(1))
                     add(JsonPrimitive(2))
                     add(JsonPrimitive(3))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(9)) // SHORT
                  add(JsonPrimitive(4))
               }
               addJsonArray {
                  add(JsonPrimitive(10)) // SHORT_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(5))
                     add(JsonPrimitive(8))
                     add(JsonPrimitive(13))
                     add(JsonPrimitive(21))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(11)) // UNSIGNED_SHORT
                  add(JsonPrimitive(11))
               }
               addJsonArray {
                  add(JsonPrimitive(12)) // UNSIGNED_SHORT_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(13))
                     add(JsonPrimitive(8))
                     add(JsonPrimitive(3))
                     add(JsonPrimitive(20))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(13)) // INT
                  add(JsonPrimitive(42))
               }
               addJsonArray {
                  add(JsonPrimitive(14)) // INT_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(3))
                     add(JsonPrimitive(13))
                     add(JsonPrimitive(210))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(15)) // UNSIGNED_INT
                  add(JsonPrimitive(64))
               }
               addJsonArray {
                  add(JsonPrimitive(16)) // UNSIGNED_INT_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(250))
                     add(JsonPrimitive(320))
                     add(JsonPrimitive(1020))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(17)) // LONG
                  add(JsonPrimitive(-35000))
               }
               addJsonArray {
                  add(JsonPrimitive(18)) // LONG_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(31))
                     add(JsonPrimitive(1695917981219))
                     add(JsonPrimitive(53))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(19)) // UNSIGNED_LONG
                  add(JsonPrimitive(9223371036891382938))
               }
               addJsonArray {
                  add(JsonPrimitive(20)) // UNSIGNED_LONG_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(310))
                     add(JsonPrimitive("9200222222222288888".toULong().toLong()))
                     add(JsonPrimitive(533))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(21)) // FLOAT
                  add(JsonPrimitive(42.2))
               }
               addJsonArray {
                  add(JsonPrimitive(22)) // FLOAT_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(61.7))
                     add(JsonPrimitive(3.325))
                     add(JsonPrimitive(12.13))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(23)) // DOUBLE
                  add(JsonPrimitive(32.5))
               }
               addJsonArray {
                  add(JsonPrimitive(24)) // DOUBLE_ARRAY
                  addJsonArray {
                     add(JsonPrimitive(-89.0))
                     add(JsonPrimitive(123456789.0987))
                     add(JsonPrimitive(0.1))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(25)) // STRING
                  add(JsonPrimitive("Lorem ipsum"))
               }
               addJsonArray {
                  add(JsonPrimitive(26)) // PAIR
                  addJsonArray {
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("Lorem"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("ipsum"))
                     }
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(27)) // TRIPLE
                  addJsonArray {
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("dolor"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("sit"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("amet"))
                     }
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(28)) // INT_SIZE
                  addJsonArray {
                     add(JsonPrimitive(3))
                     add(JsonPrimitive(4))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(29)) // SIZE
                  addJsonArray {
                     add(JsonPrimitive(3.3))
                     add(JsonPrimitive(4.5))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(30)) // ANDROID_SIZE
                  addJsonArray {
                     add(JsonPrimitive(1000))
                     add(JsonPrimitive(1902))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(31)) // ANDROID_SIZE_F
                  addJsonArray {
                     add(JsonPrimitive(1020.5))
                     add(JsonPrimitive(2003.1))
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(32)) // LIST
                  addJsonArray {
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("consectetur"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("adipiscing"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("elit"))
                     }
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(33)) // SET
                  addJsonArray {
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("sed"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("do"))
                     }
                     addJsonArray {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("eiusmod"))
                     }
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(34)) // MAP
                  addJsonArray {
                     addJsonArray {
                        addJsonArray {
                           add(JsonPrimitive(13)) // INT
                           add(JsonPrimitive(0))
                        }
                        addJsonArray {
                           add(JsonPrimitive(25)) // STRING
                           add(JsonPrimitive("tempor"))
                        }
                     }
                     addJsonArray {
                        addJsonArray {
                           add(JsonPrimitive(13)) // INT
                           add(JsonPrimitive(3))
                        }
                        addJsonArray {
                           add(JsonPrimitive(25)) // STRING
                           add(JsonPrimitive("incididunt"))
                        }
                     }
                     addJsonArray {
                        addJsonArray {
                           add(JsonPrimitive(13)) // INT
                           add(JsonPrimitive(5))
                        }
                        addJsonArray {
                           add(JsonPrimitive(25)) // STRING
                           add(JsonPrimitive("ut"))
                        }
                     }
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(35)) // BUNDLE
                  addJsonObject {
                     putJsonArray("labore") {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("et"))
                     }
                     putJsonArray("dolore") {
                        add(JsonPrimitive(25)) // STRING
                        add(JsonPrimitive("magna"))
                     }
                  }
               }
               addJsonArray {
                  add(JsonPrimitive(36)) // SERIALIZABLE

                  val outputStream = ByteArrayOutputStream()
                  ObjectOutputStream(outputStream).use {
                     it.writeObject(SerializableClass(88))
                  }
                  val serializedStr = Base64.encodeToString(
                     outputStream.toByteArray(), Base64.DEFAULT)
                  add(JsonPrimitive(serializedStr))
               }
            }
         }
      })

      val stateSaver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)

      stateSaver.save("key", init = { fail() }, saver = object : Saver<Unit, Any> {
         override fun SaverScope.save(value: Unit) = null

         override fun restore(value: Any) {
            assertIs<List<*>>(value)
            assertNull(value[0])
            assertIs<Boolean>(value[1]).also { assertTrue(it) }
            assertIs<BooleanArray>(value[2]).also {
               assertContentEquals(booleanArrayOf(true, true, false), it)
            }
            assertIs<Char>(value[3]).also { assertEquals('A', it) }
            assertIs<CharArray>(value[4]).also {
               assertContentEquals(charArrayOf('L', 'i', 'p', 's', 'u', 'm'), it)
            }
            assertIs<Byte>(value[5]).also { assertEquals(3, it) }
            assertIs<ByteArray>(value[6]).also {
               assertContentEquals(byteArrayOf(3, 3, 4, 2), it)
            }
            assertIs<UByte>(value[7]).also { assertEquals(5.toUByte(), it) }
            assertIs<UByteArray>(value[8]).also {
               assertContentEquals(
                  ubyteArrayOf(1.toUByte(), 1.toUByte(), 2.toUByte(), 3.toUByte()),
                  it
               )
            }
            assertIs<Short>(value[9]).also { assertEquals(4, it) }
            assertIs<ShortArray>(value[10]).also {
               assertContentEquals(shortArrayOf(5, 8, 13, 21), it)
            }
            assertIs<UShort>(value[11]).also { assertEquals(11.toUShort(), it) }
            assertIs<UShortArray>(value[12]).also {
               assertContentEquals(
                  ushortArrayOf(13.toUShort(), 8.toUShort(), 3.toUShort(), 20.toUShort()),
                  it
               )
            }
            assertIs<Int>(value[13]).also { assertEquals(42, it) }
            assertIs<IntArray>(value[14]).also {
               assertContentEquals(intArrayOf(3, 13, 210), it)
            }
            assertIs<UInt>(value[15]).also { assertEquals(64.toUInt(), it) }
            assertIs<UIntArray>(value[16]).also {
               assertContentEquals(
                  uintArrayOf(250.toUInt(), 320.toUInt(), 1020.toUInt()),
                  it
               )
            }
            assertIs<Long>(value[17]).also { assertEquals(-35000L, it) }
            assertIs<LongArray>(value[18]).also {
               assertContentEquals(longArrayOf(31L, 1695917981219L, 53L), it)
            }
            assertIs<ULong>(value[19]).also {
               assertEquals(9223371036891382938L.toULong(), it)
            }
            assertIs<ULongArray>(value[20]).also {
               assertContentEquals(
                  ulongArrayOf(
                     310L.toULong(),
                     "9200222222222288888".toULong(),
                     533L.toULong()
                  ),
                  it
               )
            }
            assertIs<Float>(value[21]).also {
               assertEquals(42.2f, it, absoluteTolerance = 0.01f)
            }
            assertIs<FloatArray>(value[22]).also {
               assertEquals(3, it.size)
               for ((expected, actual) in floatArrayOf(61.7f, 3.325f, 12.13f).zip(it)) {
                  assertEquals(expected, actual, absoluteTolerance = 0.01f)
               }
            }
            assertIs<Double>(value[23]).also {
               assertEquals(32.5, it, absoluteTolerance = 0.01)
            }
            assertIs<DoubleArray>(value[24]).also {
               assertEquals(3, it.size)
               for ((expected, actual) in doubleArrayOf(-89.0, 123456789.0987, 0.1).zip(it)) {
                  assertEquals(expected, actual, absoluteTolerance = 0.01)
               }
            }
            assertIs<String>(value[25]).also {
               assertEquals("Lorem ipsum", it)
            }
            assertIs<Pair<*, *>>(value[26]).also {
               assertEquals(Pair("Lorem", "ipsum"), it)
            }
            assertIs<Triple<*, *, *>>(value[27]).also {
               assertEquals(Triple("dolor", "sit", "amet"), it)
            }
            assertIs<IntSize>(value[28]).also {
               assertEquals(IntSize(3, 4), it)
            }
            assertIs<Size>(value[29]).also {
               assertEquals(3.3f, it.width,  absoluteTolerance = 0.01f)
               assertEquals(4.5f, it.height, absoluteTolerance = 0.01f)
            }
            assertIs<android.util.Size>(value[30]).also {
               assertEquals(android.util.Size(1000, 1902), it)
            }
            assertIs<android.util.SizeF>(value[31]).also {
               assertEquals(1020.5f, it.width,  absoluteTolerance = 0.01f)
               assertEquals(2003.1f, it.height, absoluteTolerance = 0.01f)
            }
            assertIs<List<*>>(value[32]).also {
               assertContentEquals(
                  listOf("consectetur", "adipiscing", "elit"),
                  it
               )
            }
            assertIs<Set<*>>(value[33]).also {
               assertEquals(3, it.size)
               for (a in listOf("sed", "do", "eiusmod")) {
                  assertTrue(it.contains(a))
               }
            }
            assertIs<Map<*, *>>(value[34]).also {
               assertEquals(3, it.size)
               for ((k, v) in listOf(0 to "tempor", 3 to "incididunt", 5 to "ut")) {
                  assertEquals(v, it[k])
               }
            }
            assertIs<Bundle>(value[35]).also {
               assertEquals(2, it.size())
               for ((k, v) in listOf("labore" to "et", "dolore" to "magna")) {
                  assertEquals(v, it.getString(k))
               }
            }
            assertIs<SerializableClass>(value[36]).also {
               assertEquals(88, it.i)
            }
         }
      })
   }

   @OptIn(ExperimentalUnsignedTypes::class)
   @Test
   fun saver_savedSaveableInstance() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})
      val stateSaver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)

      stateSaver.save(
         "key",
         object : Saver<List<Any?>, Any> {
            override fun SaverScope.save(value: List<Any?>) = value
            override fun restore(value: Any): List<Any?>? {
               fail()
            }
         }
      ) {
         listOf(
            null,
            true,
            booleanArrayOf(true, true, false),
            'A',
            charArrayOf('L', 'i', 'p', 's', 'u', 'm'),
            3.toByte(),
            byteArrayOf(3, 3, 4, 2),
            5.toUByte(),
            ubyteArrayOf(1.toUByte(), 1.toUByte(), 2.toUByte(), 3.toUByte()),
            4.toShort(),
            shortArrayOf(5, 8, 13, 21),
            11.toUShort(),
            ushortArrayOf(13.toUShort(), 8.toUShort(), 3.toUShort(), 20.toUShort()),
            42,
            intArrayOf(3, 13, 210),
            64.toUInt(),
            uintArrayOf(250.toUInt(), 320.toUInt(), 1020.toUInt()),
            -35000L,
            longArrayOf(31L, 1695917981219L, 53L),
            9223371036891382938L.toULong(),
            ulongArrayOf(310L.toULong(), "9200222222222288888".toULong(), 533L.toULong()),
            42.2f,
            floatArrayOf(61.7f, 3.325f, 12.13f),
            32.5,
            doubleArrayOf(-89.0, 123456789.0987, 0.1),
            "Lorem ipsum",
            Pair("Lorem", "ipsum"),
            Triple("dolor", "sit", "amet"),
            IntSize(3, 4),
            Size(3.3f, 4.5f),
            android.util.Size(1000, 1902),
            android.util.SizeF(1020.5f, 2003.1f),
            listOf("consectetur", "adipiscing", "elit"),
            setOf("sed", "do", "eiusmod"),
            mapOf(0 to "tempor", 3 to "incididunt", 5 to "ut"),
            bundleOf("labore" to "et", "dolore" to "magna"),
            SerializableClass(88),
         )
      }

      rule.waitForSnapshotFlow { cache.value.isNotEmpty() }

      assertTypedElement(cache.value["key"], 32) { rootValue ->
         val list = assertIs<JsonArray>(rootValue)
         assertEquals(37, list.size)

         assertTypedElement(list[0], 0) {
            assertIs<JsonNull>(it)
         }
         assertTypedElement(list[1], 1) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertTrue(it.boolean)
         }
         assertTypedElement(list[2], 2) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf(true, true, false).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.boolean)
            }
         }
         assertTypedElement(list[3], 3) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals('A'.code, it.int)
         }
         assertTypedElement(list[4], 4) {
            assertIs<JsonArray>(it)
            assertEquals(6, it.size)
            for ((expected, actual) in listOf('L', 'i', 'p', 's', 'u', 'm').zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected.code, actual.int)
            }
         }
         assertTypedElement(list[5], 5) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(3, it.int)
         }
         assertTypedElement(list[6], 6) {
            assertIs<JsonArray>(it)
            assertEquals(4, it.size)
            for ((expected, actual) in listOf(3, 3, 4, 2).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.int)
            }
         }
         assertTypedElement(list[7], 7) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(5, it.int)
         }
         assertTypedElement(list[8], 8) {
            assertIs<JsonArray>(it)
            assertEquals(4, it.size)
            for ((expected, actual) in listOf(1, 1, 2, 3).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.int)
            }
         }
         assertTypedElement(list[9], 9) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(4, it.int)
         }
         assertTypedElement(list[10], 10) {
            assertIs<JsonArray>(it)
            assertEquals(4, it.size)
            for ((expected, actual) in listOf(5, 8, 13, 21).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.int)
            }
         }
         assertTypedElement(list[11], 11) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(11, it.int)
         }
         assertTypedElement(list[12], 12) {
            assertIs<JsonArray>(it)
            assertEquals(4, it.size)
            for ((expected, actual) in listOf(13, 8, 3, 20).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.int)
            }
         }
         assertTypedElement(list[13], 13) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(42, it.int)
         }
         assertTypedElement(list[14], 14) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf(3, 13, 210).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.int)
            }
         }
         assertTypedElement(list[15], 15) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(64, it.int)
         }
         assertTypedElement(list[16], 16) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf(250, 320, 1020).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.int)
            }
         }
         assertTypedElement(list[17], 17) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(-35000, it.int)
         }
         assertTypedElement(list[18], 18) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf(31L, 1695917981219L, 53L).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.long)
            }
         }
         assertTypedElement(list[19], 19) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(9223371036891382938L, it.long)
         }
         assertTypedElement(list[20], 20) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf("310", "9200222222222288888", "533").zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected.toULong().toLong(), actual.long)
            }
         }
         assertTypedElement(list[21], 21) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(42.2f, it.content.toFloat(), absoluteTolerance = 0.01f)
         }
         assertTypedElement(list[22], 22) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf(61.7f, 3.325f, 12.13f).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.content.toFloat(), absoluteTolerance = 0.01f)
            }
         }
         assertTypedElement(list[23], 23) {
            assertIs<JsonPrimitive>(it)
            assertFalse(it.isString)
            assertEquals(32.5f, it.content.toFloat(), absoluteTolerance = 0.01f)
         }
         assertTypedElement(list[24], 24) {
            assertIs<JsonArray>(it)
            assertEquals(3, it.size)
            for ((expected, actual) in listOf(-89.0, 123456789.0987, 0.1).zip(it)) {
               assertIs<JsonPrimitive>(actual)
               assertFalse(actual.isString)
               assertEquals(expected, actual.content.toDouble(), absoluteTolerance = 0.01)
            }
         }
         assertTypedElement(list[25], 25) {
            assertIs<JsonPrimitive>(it)
            assertTrue(it.isString)
            assertEquals("Lorem ipsum", it.content)
         }
         assertTypedElement(list[26], 26) { pairElement ->
            assertIs<JsonArray>(pairElement)
            assertEquals(2, pairElement.size)
            assertTypedElement(pairElement[0], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("Lorem", it.content)
            }
            assertTypedElement(pairElement[1], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("ipsum", it.content)
            }
         }
         assertTypedElement(list[27], 27) { tripleElement ->
            assertIs<JsonArray>(tripleElement)
            assertEquals(3, tripleElement.size)
            assertTypedElement(tripleElement[0], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("dolor", it.content)
            }
            assertTypedElement(tripleElement[1], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("sit", it.content)
            }
            assertTypedElement(tripleElement[2], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("amet", it.content)
            }
         }
         assertTypedElement(list[28], 28) { intSizeElement ->
            assertIs<JsonArray>(intSizeElement)
            assertEquals(2, intSizeElement.size)

            val (widthElement, heightElement) = intSizeElement
            assertIs<JsonPrimitive>(widthElement)
            assertFalse(widthElement.isString)
            assertEquals(3, widthElement.int)
            assertIs<JsonPrimitive>(heightElement)
            assertFalse(heightElement.isString)
            assertEquals(4, heightElement.int)
         }
         assertTypedElement(list[29], 29) { sizeElement ->
            assertIs<JsonArray>(sizeElement)
            assertEquals(2, sizeElement.size)

            val (widthElement, heightElement) = sizeElement
            assertIs<JsonPrimitive>(widthElement)
            assertFalse(widthElement.isString)
            assertEquals(3.3f, widthElement.float, absoluteTolerance = 0.01f)
            assertIs<JsonPrimitive>(heightElement)
            assertFalse(heightElement.isString)
            assertEquals(4.5f, heightElement.float, absoluteTolerance = 0.01f)
         }
         assertTypedElement(list[30], 30) { intSizeElement ->
            assertIs<JsonArray>(intSizeElement)
            assertEquals(2, intSizeElement.size)

            val (widthElement, heightElement) = intSizeElement
            assertIs<JsonPrimitive>(widthElement)
            assertFalse(widthElement.isString)
            assertEquals(1000, widthElement.int)
            assertIs<JsonPrimitive>(heightElement)
            assertFalse(heightElement.isString)
            assertEquals(1902, heightElement.int)
         }
         assertTypedElement(list[31], 31) { sizeElement ->
            assertIs<JsonArray>(sizeElement)
            assertEquals(2, sizeElement.size)

            val (widthElement, heightElement) = sizeElement
            assertIs<JsonPrimitive>(widthElement)
            assertFalse(widthElement.isString)
            assertEquals(1020.5f, widthElement.float, absoluteTolerance = 0.01f)
            assertIs<JsonPrimitive>(heightElement)
            assertFalse(heightElement.isString)
            assertEquals(2003.1f, heightElement.float, absoluteTolerance = 0.01f)
         }
         assertTypedElement(list[32], 32) { listElement ->
            assertIs<JsonArray>(listElement)
            assertEquals(3, listElement.size)
            assertTypedElement(listElement[0], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("consectetur", it.content)
            }
            assertTypedElement(listElement[1], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("adipiscing", it.content)
            }
            assertTypedElement(listElement[2], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("elit", it.content)
            }
         }
         assertTypedElement(list[33], 33) { setElement ->
            assertIs<JsonArray>(setElement)
            assertEquals(3, setElement.size)
            assertTypedElement(setElement[0], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("sed", it.content)
            }
            assertTypedElement(setElement[1], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("do", it.content)
            }
            assertTypedElement(setElement[2], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("eiusmod", it.content)
            }
         }
         assertTypedElement(list[34], 34) { mapElement ->
            assertIs<JsonArray>(mapElement)
            assertEquals(3, mapElement.size)
            assertIs<JsonArray>(mapElement[0]).also { entryElement ->
               assertIs<JsonArray>(entryElement)
               assertEquals(2, entryElement.size)
               assertTypedElement(entryElement[0], 13) {
                  assertIs<JsonPrimitive>(it)
                  assertFalse(it.isString)
                  assertEquals(0, it.int)
               }
               assertTypedElement(entryElement[1], 25) {
                  assertIs<JsonPrimitive>(it)
                  assertTrue(it.isString)
                  assertEquals("tempor", it.content)
               }
            }
            assertIs<JsonArray>(mapElement[1]).also { entryElement ->
               assertIs<JsonArray>(entryElement)
               assertEquals(2, entryElement.size)
               assertTypedElement(entryElement[0], 13) {
                  assertIs<JsonPrimitive>(it)
                  assertFalse(it.isString)
                  assertEquals(3, it.int)
               }
               assertTypedElement(entryElement[1], 25) {
                  assertIs<JsonPrimitive>(it)
                  assertTrue(it.isString)
                  assertEquals("incididunt", it.content)
               }
            }
            assertIs<JsonArray>(mapElement[2]).also { entryElement ->
               assertIs<JsonArray>(entryElement)
               assertEquals(2, entryElement.size)
               assertTypedElement(entryElement[0], 13) {
                  assertIs<JsonPrimitive>(it)
                  assertFalse(it.isString)
                  assertEquals(5, it.int)
               }
               assertTypedElement(entryElement[1], 25) {
                  assertIs<JsonPrimitive>(it)
                  assertTrue(it.isString)
                  assertEquals("ut", it.content)
               }
            }
         }
         assertTypedElement(list[35], 35) { bundleElement ->
            assertIs<JsonObject>(bundleElement)
            assertEquals(2, bundleElement.size)
            assertTypedElement(bundleElement["labore"], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("et", it.content)
            }
            assertTypedElement(bundleElement["dolore"], 25) {
               assertIs<JsonPrimitive>(it)
               assertTrue(it.isString)
               assertEquals("magna", it.content)
            }
         }
         assertTypedElement(list[36], 36) { selializableElement ->
            assertIs<JsonPrimitive>(selializableElement)
            assertTrue(selializableElement.isString)

            val outputStream = ByteArrayOutputStream()
            ObjectOutputStream(outputStream).use {
               it.writeObject(SerializableClass(88))
            }
            val serializedStr = Base64.encodeToString(
               outputStream.toByteArray(), Base64.DEFAULT)
            assertEquals(serializedStr, selializableElement.content)
         }
      }
   }

   @Test
   fun saver_getValue() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {
         putJsonArray("key") {
            add(JsonPrimitive(13))
            add(JsonPrimitive(42))
         }
      })

      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
      val savedState = saver.save("key", autoSaver<Int>()) { fail() }
      assertEquals(42, savedState.value)

      // savedStateのインスタンス化直後、値をキャッシュに保存する処理が走っているため、
      // このタイミングでキャッシュを書き換えると後で終了するキャッシュ処理によって
      // 元の値が復元される。
      // そのためキャッシュ保存処理の終了を待つ必要があるが、処理の前後で
      // 観測可能な変化がなく終了したかどうかを判断できない。そのため処理に十分と
      // 思われる時間待機する
      rule.waitForSnapshotFlow { it > 50.milliseconds }

      cache.value = buildJsonObject {
         putJsonArray("key") {
            add(JsonPrimitive(13))
            add(JsonPrimitive(0))
         }
      }

      rule.waitForSnapshotFlow { savedState.value == 0 }
   }

   @Test
   fun saver_initialize() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})

      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
      val savedState = saver.save("key", autoSaver()) { 42 }

      rule.waitForSnapshotFlow { cache.value.containsKey("key") }
      cache.value["key"].let { typedElement ->
         assertNotNull(typedElement)
         assertTypedElement(typedElement, 13) {
            assertTrue(isIntElement(it, 42))
         }
      }

      assertEquals(42, savedState.value)

      rule.waitForSnapshotFlow { it > 2050.milliseconds }

      cache.value = buildJsonObject {}
      assertFalse(cache.value.containsKey("key"))
      rule.waitForSnapshotFlow { cache.value.containsKey("key") }

      assertEquals(42, savedState.value)
      cache.value["key"].let { typedElement ->
         assertNotNull(typedElement)
         assertTypedElement(typedElement, 13) {
            assertTrue(isIntElement(it, 42))
         }
      }
   }

   @Test
   fun saver_recover() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})

      run {
         val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
         val savedState = saver.save("key", autoSaver(), init = { 42 }, recover = { fail() })

         assertEquals(42, savedState.value)
      }

      run {
         val saver = PageState.StateSaver(cache, wasCacheDeleted = true, coroutineScope)
         val savedState = saver.save("key", autoSaver(), init = { fail() }, recover = { 42 })

         assertEquals(42, savedState.value)
      }
   }

   @Test
   fun saver_setValue() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {
         putJsonArray("key") {
            add(JsonPrimitive(13))
            add(JsonPrimitive(3))
         }
      })

      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
      val savedState = saver.save("key", autoSaver<Int>()) { fail() }

      savedState.value = 42
      rule.waitForSnapshotFlow {
         val valueElement = getTypedValue(cache.value["key"], 13)
         isIntElement(valueElement, 42)
      }
      assertEquals(42, savedState.value)
   }

   @Test
   fun saver_nullable() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})
      val saver = PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)

      val savedState = saver.save("key", autoSaver<Int?>()) { 42 }
      rule.waitForSnapshotFlow { cache.value.containsKey("key") }
      assertEquals(42, savedState.value)
      cache.value["key"].let { typedElement ->
         assertNotNull(typedElement)
         assertTypedElement(typedElement, 13) {
            assertTrue(isIntElement(it, 42))
         }
      }

      savedState.value = null
      rule.waitForSnapshotFlow { isTypedValue(cache.value["key"], 0) }
      assertNull(savedState.value)
      cache.value["key"].let { typedElement ->
         assertNotNull(typedElement)
         assertTypedElement(typedElement, 0) {
            assertIs<JsonNull>(it)
         }
      }
   }

   @Test
   fun saver_updateSourceWhenStateChanged() {
      val cache = WritableCache(buildJsonObject {})

      lateinit var savedScrollState: State<ScrollState>

      rule.setContent {
         val coroutineScope = rememberCoroutineScope()
         val saver = remember {
            PageState.StateSaver(cache, wasCacheDeleted = false, coroutineScope)
         }

         savedScrollState = remember {
            saver.save("scrollState", ScrollState.Saver) { ScrollState(0) }
         }

         Box(Modifier.height(100.dp).verticalScroll(savedScrollState.value)) {
            Box(Modifier.height(300.dp))
         }
      }

      rule.waitForSnapshotFlow {
         val valueElement = getTypedValue(cache.value["scrollState"], 13)
         isIntElement(valueElement, 0)
      }

      rule.onNode(hasScrollAction())
         .performTouchInput {
            down(Offset(0.0f, 50.0f + viewConfiguration.touchSlop))
            moveTo(Offset.Zero)
         }

      rule.runOnIdle {
         assertEquals(50, savedScrollState.value.value)
      }

      rule.waitForSnapshotFlow {
         val valueElement = getTypedValue(cache.value["scrollState"], 13)
         isIntElement(valueElement, 50)
      }
   }

   @Test
   fun saver_debounce() {
      lateinit var coroutineScope: CoroutineScope

      rule.setContent {
         coroutineScope = rememberCoroutineScope()
         Box(Modifier)
      }

      val cache = WritableCache(buildJsonObject {})
      val spyCache = spyk(cache)
      val saver = PageState.StateSaver(spyCache, wasCacheDeleted = false, coroutineScope)

      @Stable
      class State(initialI: Int) {
         var i by mutableStateOf(initialI)
      }
      val stateSaver = Saver<State, Int>(save = { it.i }, restore = { State(it) })
      val savedState by saver.save("state", stateSaver) { State(0) }

      savedState.i++
      rule.waitForSnapshotFlow { it > 500.milliseconds }
      savedState.i++
      rule.waitForSnapshotFlow { it > 1000.milliseconds }
      savedState.i++
      rule.waitForSnapshotFlow { it > 750.milliseconds }
      savedState.i++
      rule.waitForSnapshotFlow { it > 2100.milliseconds }
      savedState.i++
      rule.waitForSnapshotFlow { it > 1000.milliseconds }
      savedState.i++
      rule.waitForSnapshotFlow { it > 2500.milliseconds }
      savedState.i++
      rule.waitForSnapshotFlow { it > 2050.milliseconds }

      rule.runOnIdle {
         assertEquals(7, savedState.i)

         assertTypedElement(cache.value["state"], 13) {
            assertTrue(isIntElement(it, 7))
         }

         verify(exactly = 3) { spyCache.value = any() }
      }
   }
}
