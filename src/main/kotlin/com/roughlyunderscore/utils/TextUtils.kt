// Copyright 2024 RoughlyUnderscore
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.roughlyunderscore.utils

fun Iterable<String>.joinAndWrap(prefix: String, suffix: String, separator: String): String {
  val mutable = this.toMutableList()
  for (i in mutable.indices) {
    mutable[i] = "$prefix${mutable[i]}$suffix"
  }

  // Add separators to everything but last
  for (i in 0 ..< mutable.size - 1) {
    mutable[i] = "${mutable[i]}$separator"
  }

  return mutable.joinToString("")
}