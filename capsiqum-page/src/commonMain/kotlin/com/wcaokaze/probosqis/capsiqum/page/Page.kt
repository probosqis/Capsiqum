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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import com.wcaokaze.probosqis.panoptiqon.compose.asState
import com.wcaokaze.probosqis.panoptiqon.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.seconds

@Serializable
abstract class Page

fun PageId(clock: Clock = Clock.System) = PageId(
   clock.now().toEpochMilliseconds()
)

/**
 * その名の通り[Page]のIDだが、[PageStack]に挿入されるときに[SavedPageState]が
 * 生成されるのと同時にIDが発行されるため、[Page]自身はIDを知らない
 */
@Serializable
@JvmInline
value class PageId(val value: Long)

internal expect class JsonElementSaver<T>(saver: Saver<T, *>) : Saver<T, JsonElement>

internal expect object PageStateHiddenArguments {
   fun get(): PageState.Arguments
   fun set(args: PageState.Arguments)
}

@Stable
abstract class PageState<out P : Page> {
   val page: P
   val pageId: PageId
   val pageStateScope: CoroutineScope
   val stateSaver: StateSaver

   init {
      val args = PageStateHiddenArguments.get()
      pageStateScope = args.coroutineScope
      @Suppress("UNCHECKED_CAST")
      page = args.page as P
      pageId = args.pageId
      stateSaver = args.stateSaver
   }

   protected inline fun <T> save(
      key: String,
      serializer: KSerializer<T>,
      crossinline init: () -> T,
      crossinline recover: () -> T
   ): MutableState<T> = stateSaver.save(key, serializer, init, recover)

   protected fun <T> save(
      key: String,
      serializer: KSerializer<T>,
      init: () -> T
   ): MutableState<T> = stateSaver.save(key, serializer, init)

   protected inline fun <T> save(
      key: String,
      saver: Saver<T, *>,
      crossinline init: () -> T,
      crossinline recover: () -> T
   ): MutableState<T> = stateSaver.save(key, saver, init, recover)

   protected fun <T> save(
      key: String,
      saver: Saver<T, *>,
      init: () -> T
   ): MutableState<T> = stateSaver.save(key, saver, init)

   internal class Arguments(
      val page: Page,
      val pageId: PageId,
      val coroutineScope: CoroutineScope,
      val stateSaver: StateSaver,
   )

   @Stable
   class StateSaver(
      private val cache: WritableCache<JsonObject>,
      @PublishedApi
      internal val wasCacheDeleted: Boolean,
      private val pageStateScope: CoroutineScope
   ) {
      /**
       * @param init Pageが起動されるときの初期値
       * @param recover
       *   容量不足等の理由により保存されたデータが削除されたあと再初期化する際の値
       */
      inline fun <T> save(
         key: String,
         serializer: KSerializer<T>,
         crossinline init: () -> T,
         crossinline recover: () -> T
      ): MutableState<T> {
         return save(key, serializer) {
            if (wasCacheDeleted) { recover() } else { init() }
         }
      }

      fun <T> save(
         key: String,
         serializer: KSerializer<T>,
         init: () -> T
      ): MutableState<T> {
         return ElementState(cache, key, serializer, init)
      }

      inline fun <T> save(
         key: String,
         saver: Saver<T, *>,
         crossinline init: () -> T,
         crossinline recover: () -> T
      ): MutableState<T> {
         return save(key, saver) {
            if (wasCacheDeleted) { recover() } else { init() }
         }
      }

      fun <T> save(
         key: String,
         saver: Saver<T, *>,
         init: () -> T
      ): MutableState<T> {
         return AutoSaveableElementState(cache, key, saver, init, pageStateScope)
      }

      private class ElementState<T>(
         private val source: WritableCache<JsonObject>,
         private val key: String,
         private val serializer: KSerializer<T>,
         private val init: () -> T
      ) : MutableState<T> {
         init {
            if (!source.value.containsKey(key)) {
               initialize()
            }
         }

         override var value: T
            get() {
               val element = source.value[key]
               return if (element != null) {
                  try {
                     Json.decodeFromJsonElement(serializer, element)
                  } catch (_: Exception) {
                     initialize()
                  }
               } else {
                  initialize()
               }
            }
            set(value) {
               updateSource(value)
            }

         override fun component1(): T = value
         override fun component2(): (T) -> Unit = { value = it }

         private fun updateSource(value: T) {
            val jsonValue = Json.encodeToJsonElement(serializer, value)
            source.update { JsonObject(it + (key to jsonValue)) }
         }

         private fun initialize(): T {
            val initValue = init()
            updateSource(initValue)
            return initValue
         }
      }

      private class AutoSaveableElementState<T>(
         private val source: WritableCache<JsonObject>,
         private val key: String,
         saver: Saver<T, *>,
         private val init: () -> T,
         pageStateCoroutineScope: CoroutineScope
      ) : MutableState<T> {
         private val jsonSaver = JsonElementSaver(saver)

         private val mutex = Mutex()
         private var savedJsonElement: JsonElement? = source.value[key]

         override var value: T by mutableStateOf(
            restoreOrInstantiate(savedJsonElement)
         )

         init {
            pageStateCoroutineScope.launch {
               launch {
                  @OptIn(FlowPreview::class)
                  snapshotFlow { save(value) }
                     .debounce(2.seconds)
                     .collectLatest(::updateSource)
               }

               launch {
                  val sourceState = source.asState()
                  snapshotFlow { sourceState.value[key] }
                     .drop(1)
                     .collectLatest(::onSourceChanged)
               }
            }
         }

         override fun component1(): T = value
         override fun component2(): (T) -> Unit = { value = it }

         private fun save(value: T): JsonElement? {
            return with (jsonSaver) {
               val scope = SaverScope { it is JsonElement }
               scope.save(value)
            }
         }

         private fun restoreOrInstantiate(savedJsonElement: JsonElement?): T {
            return savedJsonElement?.let { jsonSaver.restore(it) } ?: init()
         }

         private suspend fun updateSource(value: JsonElement?) {
            mutex.withLock {
               savedJsonElement = value

               source.update { currentSourceContent ->
                  if (!isChanged(currentSourceContent[key], value)) { return }

                  val map = buildMap {
                     putAll(currentSourceContent)
                     if (value != null) {
                        put(key, value)
                     } else {
                        remove(key)
                     }
                     Unit
                  }

                  JsonObject(map)
               }
            }
         }

         private suspend fun onSourceChanged(newSavedElement: JsonElement?) {
            val oldValue = value
            val newValue = mutex.withLock {
               if (source.value[key] !== newSavedElement) { return }

               val prevSavedElement = savedJsonElement
               if (!isChanged(newSavedElement, prevSavedElement)) { return }

               savedJsonElement = newSavedElement
               val restoredValue = restoreOrInstantiate(newSavedElement)
               value = restoredValue
               restoredValue
            }

            // JsonElementが変化したのに復元後のインスタンスが同一になる場合がある
            // （JsonElementがnullでinit()呼び出しが行われた場合等）
            // その場合snapshotFlowは再実行されずvalueがsourceに反映されないため
            // ここで明示的に行う
            if (newValue == oldValue) {
               val savedElement = save(newValue)
               updateSource(savedElement)
            }
         }

         private fun isChanged(
            oldElement: JsonElement?,
            newElement: JsonElement?
         ): Boolean {
            return when (newElement) {
               null -> oldElement != null
               is JsonNull -> oldElement !is JsonNull
               is JsonPrimitive -> {
                  if (oldElement !is JsonPrimitive) { return true }
                  if (newElement.isString != oldElement.isString) { return true }
                  if (newElement.content != oldElement.content) { return true }
                  false
               }
               is JsonArray -> {
                  if (oldElement !is JsonArray) { return true }
                  if (newElement.size != oldElement.size) { return true }
                  for (i in newElement.indices) {
                     if (isChanged(newElement[i], oldElement[i])) { return true }
                  }
                  false
               }
               is JsonObject -> {
                  if (oldElement !is JsonObject) { return true }
                  if (newElement.size != oldElement.size) { return true }
                  for ((newKey, newValue) in newElement.entries) {
                     val oldValue = oldElement[newKey]
                     if (isChanged(newValue, oldValue)) { return true }
                  }
                  false
               }
            }
         }
      }
   }
}
