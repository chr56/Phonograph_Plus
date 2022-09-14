
/*
 * Copyright © 2020-2022 Anggrayudi Hardiannico A.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib.phonograph.storage

import android.content.Context
import android.os.Environment

val externalStoragePath: String
    get() = Environment.getExternalStorageDirectory().absolutePath

/**
 * For files under [Environment.getExternalStorageDirectory]
 */
const val PRIMARY = "primary"

/**
 * For files under [Context.getFilesDir] or [Context.getDataDir].
 * It is not really a storage ID, and can't be used in file tree URI.
 */
const val DATA = "data"
