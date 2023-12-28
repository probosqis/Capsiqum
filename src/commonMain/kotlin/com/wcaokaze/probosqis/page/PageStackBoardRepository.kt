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
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

interface PageStackBoardRepository {
   fun savePageStackBoard(pageStackBoard: PageStackBoard): WritableCache<PageStackBoard>
   fun loadPageStackBoard(): WritableCache<PageStackBoard>
}

abstract class AbstractPageStackBoardRepository
   internal constructor(
      private val pageStackRepository: PageStackRepository
   )
   : PageStackBoardRepository
{
   protected val json = Json {
      serializersModule = SerializersModule {
         contextual(PageStackCacheSerializer(pageStackRepository))
      }
   }
}
