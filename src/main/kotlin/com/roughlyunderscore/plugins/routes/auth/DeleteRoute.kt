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

package com.roughlyunderscore.plugins.routes.auth

import com.roughlyunderscore.accountDb
import com.roughlyunderscore.ch.lib.*
import com.roughlyunderscore.data.model.fullValidateFetchPayload
import com.roughlyunderscore.db
import com.roughlyunderscore.plugins.routes.rate.RateType
import com.roughlyunderscore.utils.joinAndWrap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.delete() {
  get("/delete") {
    coroutineScope {
      val tokenString = call.parameters["token"] ?: return@coroutineScope call.respond(HttpStatusCode.Unauthorized, "No token specified")
      val token = fullValidateFetchPayload(tokenString)

      if (token.second != null) {
        return@coroutineScope call.respond(token.second!!.first, token.second!!.second)
      }

      val email = token.first!!.subject

      val response = accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(email)) {
        if (!it.next()) return@query (HttpStatusCode.NotFound to "Account not found")

        val password = call.parameters["password"] ?: return@query (HttpStatusCode.Unauthorized to "No password specified")
        val passwordInDatabase = it.getString("pwhash") ?: return@query (HttpStatusCode.InternalServerError to "Failed to fetch password")

        val verified = verifyHash(passwordInDatabase, password)
        if (!verified) return@query (HttpStatusCode.Unauthorized to "Account not found or password incorrect")

        val id = it.getInt("id")
        val liked = it.getString("likes")?.split(";")?.mapNotNull {
          algId -> algId.toIntOrNull()
        } ?: mutableListOf()
        val disliked = it.getString("dislikes")?.split(";")?.mapNotNull {
          algId -> algId.toIntOrNull()
        } ?: mutableListOf()

        unrateFor(id, liked, RateType.LIKE)
        unrateFor(id, disliked, RateType.DISLIKE)

        accountDb.update(DELETE_ACCOUNT, id)
        accountDb.update(INVALIDATE_TOKEN, tokenString)

        return@query (HttpStatusCode.OK to "Deleted account")
      }

      call.respond(response.first, response.second)
    }
  }
}

private suspend fun unrateFor(id: Int, variations: List<Int>, type: RateType) = coroutineScope {
  for (variationId in variations) {
    db.query(FETCH_VARIATION, listOf(variationId)) { set ->
      if (!set.next()) return@query

      when (type) {
        RateType.LIKE -> {
          val liked = (set.getString("likes")?.split(";")?.toMutableList() ?: mutableListOf()).apply { removeIf(String::isBlank) }
          if (!liked.contains(id.toString())) return@query

          liked.apply { remove(id.toString()) }.joinAndWrap("", "", ";").let { db.update(UPDATE_VARIATION_LIKES, it, variationId) }
        }
        RateType.DISLIKE -> {
          val disliked = (set.getString("dislikes")?.split(";")?.toMutableList() ?: mutableListOf()).apply { removeIf(String::isBlank) }
          if (!disliked.contains(id.toString())) return@query

          disliked.apply { remove(id.toString()) }.joinAndWrap("", "", ";").let { db.update(UPDATE_VARIATION_DISLIKES, it, variationId) }
        }
      }
    }
  }
}