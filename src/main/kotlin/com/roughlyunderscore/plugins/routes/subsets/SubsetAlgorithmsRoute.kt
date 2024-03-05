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

package com.roughlyunderscore.plugins.routes.subsets

import com.roughlyunderscore.data.model.Algorithm
import com.roughlyunderscore.data.model.Variation
import com.roughlyunderscore.data.model.Version
import com.roughlyunderscore.db
import com.roughlyunderscore.ch.lib.FETCH_VARIATION
import com.roughlyunderscore.ch.lib.GET_ALGORITHMS
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.subsetAlgorithms() {
  get("/algorithms") {
    val subset = call.parameters["subset"] ?: return@get call.respond(HttpStatusCode.NotFound, "No subset specified")
    coroutineScope {
      val algorithms = mutableListOf<Algorithm>()

      db.query(GET_ALGORITHMS, listOf(subset)) { resultSet ->
        while (resultSet.next()) {
          val exhibit = resultSet.getString("exhibit")
          val exhibitImageUrl = resultSet.getString("exhibit_image_url")

          val variations = mutableListOf<Variation>()
          val variationsString = resultSet.getString("variations").split(";").map { it.toInt() }

          for (variationId in variationsString) {
            db.query(FETCH_VARIATION, listOf(variationId)) { variationSet ->
              while (variationSet.next()) {
                val id = variationSet.getInt("id")
                val value = variationSet.getString("value")
                val likes = variationSet.getString("likes")?.split(";") ?: listOf()
                val dislikes = variationSet.getString("dislikes")?.split(";") ?: listOf()
                variations.add(Variation(id, value, likes, dislikes))
              }
            }
          }

          val id = resultSet.getInt("id")
          algorithms.add(Algorithm(id, subset, exhibit, exhibitImageUrl, variations))
        }
      }

      if (algorithms.isEmpty()) {
        return@coroutineScope call.respond(HttpStatusCode.NotFound, "No algorithms found")
      }

      call.respond(HttpStatusCode.OK, algorithms)
    }
  }
}