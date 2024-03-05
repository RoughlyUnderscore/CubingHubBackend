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
import com.roughlyunderscore.data.model.Subset
import com.roughlyunderscore.data.model.Variation
import com.roughlyunderscore.db
import com.roughlyunderscore.ch.lib.FETCH_SUBSETS
import com.roughlyunderscore.ch.lib.FETCH_VARIATION
import com.roughlyunderscore.ch.lib.GET_ALGORITHMS
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.subsets() {
  get("/subsets") {
    coroutineScope {
      val subsets = mutableListOf<Subset>()

      db.query(FETCH_SUBSETS, listOf()) { resultSet ->
        while (resultSet.next()) {
          val subsetId = resultSet.getInt("id")
          val name = resultSet.getString("name")
          val imageUrl = resultSet.getString("image_url")

          val algorithms = mutableListOf<Algorithm>()
          db.query(GET_ALGORITHMS, listOf(name)) { algsSet ->
            while (algsSet.next()) {
              val id = algsSet.getInt("id")
              val exhibit = algsSet.getString("exhibit")
              val exhibitImageUrl = algsSet.getString("exhibit_image_url")

              val variations = mutableListOf<Variation>()
              val variationsString = algsSet.getString("variations").split(";").map { it.toInt() }

              for (variationId in variationsString) {
                db.query(FETCH_VARIATION, listOf(variationId)) { varSet ->
                  while (varSet.next()) {
                    val value = varSet.getString("value")
                    val likes = varSet.getString("likes")?.split(";") ?: listOf()
                    val dislikes = varSet.getString("dislikes")?.split(";") ?: listOf()

                    variations.add(Variation(variationId, value, likes, dislikes))
                  }
                }
              }

              algorithms.add(Algorithm(id, name, exhibit, exhibitImageUrl, variations))
            }
          }

          subsets.add(Subset(subsetId, name, imageUrl, algorithms))
        }
      }

      if (subsets.isEmpty()) {
        return@coroutineScope call.respond(HttpStatusCode.NotFound, "No subsets found")
      }

      call.respond(HttpStatusCode.OK, subsets)
    }
  }
}