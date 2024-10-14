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

package com.wcaokaze.probosqis.capsiqum.page.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.wcaokaze.probosqis.capsiqum.page.Page
import com.wcaokaze.probosqis.capsiqum.page.PageId
import com.wcaokaze.probosqis.capsiqum.page.PageState
import com.wcaokaze.probosqis.capsiqum.page.PageStateFactory
import kotlinx.coroutines.CoroutineScope

@Composable
fun <P : Page, S : PageState<P>> PageStateFactory<P, S>.rememberTestPageState(
   page: P,
   pageId: PageId = PageId(0L),
   pageStateScope: CoroutineScope = rememberCoroutineScope(),
   stateSaver: PageState.StateSaver = rememberTestStateSaver(),
): S {
   return remember(pageId) {
      createPageState(
         page,
         pageId,
         pageStateScope,
         stateSaver,
      )
   }
}
