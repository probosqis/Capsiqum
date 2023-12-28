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

import android.content.Context
import com.wcaokaze.probosqis.panoptiqon.TemporaryCacheApi
import com.wcaokaze.probosqis.panoptiqon.WritableCache
import com.wcaokaze.probosqis.panoptiqon.loadCache
import com.wcaokaze.probosqis.panoptiqon.saveCache
import java.io.File
import java.io.IOException

class AndroidPageStackRepository(
   context: Context,
   allPageSerializers: List<PageStackRepository.PageSerializer<*>>
) : AbstractPageStackRepository(allPageSerializers) {
   private val dir = File(context.filesDir, "L9h1Qx3xvfo0M0kX")
      .also { dir ->
         if (dir.exists()) {
            require(dir.isDirectory)
         } else {
            if (!dir.mkdirs()) { throw IOException() }
         }
      }

   /** @throws IOException */
   @TemporaryCacheApi
   override fun savePageStack(pageStack: PageStack): WritableCache<PageStack> {
      val fileName = pageStack.id.value.toString(16)
      val file = File(dir, fileName)
      return saveCache(pageStack, file, json)
   }

   /** @throws IOException */
   @TemporaryCacheApi
   override fun loadPageStack(id: PageStack.Id): WritableCache<PageStack> {
      val fileName = id.value.toString(16)
      val file = File(dir, fileName)
      return loadCache(file, json)
   }

   override fun deleteAllPageStacks() {
      for (file in dir.listFiles() ?: emptyArray()) {
         file.deleteRecursively()
      }
   }
}
