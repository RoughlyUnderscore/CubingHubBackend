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

package com.roughlyunderscore.plugins.routes.rate

import com.roughlyunderscore.accountDb
import com.roughlyunderscore.ch.lib.*
import com.roughlyunderscore.data.model.fullValidateFetchId
import com.roughlyunderscore.db
import com.roughlyunderscore.utils.joinAndWrap
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.rate() {
  get("/like") {
    coroutineScope {
      rate(call, RateType.LIKE)
    }
  }

  get("/dislike") {
    coroutineScope {
      rate(call, RateType.DISLIKE)
    }
  }
}

private suspend fun rate(call: ApplicationCall, type: RateType) = coroutineScope {
  val variationId = call.parameters["variationId"] ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No algorithmId specified")
  val token = call.parameters["token"] ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No token specified")

  val validationPair = fullValidateFetchId(token)
  if (validationPair.second != null) {
    return@coroutineScope call.respond(validationPair.second!!.first, validationPair.second!!.second)
  }

  val id = validationPair.first ?: return@coroutineScope call.respond(HttpStatusCode.InternalServerError, "Failed to validate token")

  val variation = db.query(FETCH_VARIATION, listOf(variationId)) {
    if (!it.next()) return@query null

    object {
      val likes = it.getString("likes")?.split(";")?.toMutableList() ?: mutableListOf()
      val dislikes = it.getString("dislikes")?.split(";")?.toMutableList() ?: mutableListOf()
    }
  } ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "Algorithm not found")

  val account = accountDb.query(FIND_ACCOUNT_BY_ID, listOf(id)) {
    if (!it.next()) return@query null

    object {
      val likes = it.getString("likes")?.split(";")?.toMutableList() ?: mutableListOf()
      val dislikes = it.getString("dislikes")?.split(";")?.toMutableList() ?: mutableListOf()
    }
  } ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "Account not found")

  when (type) {
    RateType.LIKE -> {
      if (variation.likes.contains(id.toString())) return@coroutineScope call.respond(HttpStatusCode.Conflict, "Already liked")
      if (account.likes.contains(variationId)) return@coroutineScope call.respond(HttpStatusCode.Conflict, "Already liked")

      if (variation.dislikes.contains(id.toString())) {
        variation.dislikes.remove(id.toString())
        account.dislikes.remove(variationId)
        db.update(UPDATE_VARIATION_DISLIKES, variation.dislikes.joinToString(";"), variationId)
        accountDb.update(UPDATE_ACCOUNT_DISLIKES, account.dislikes.joinToString(";"), id)
      }

      variation.likes.add(id.toString())
      account.likes.add(variationId)
      db.update(UPDATE_VARIATION_LIKES, variation.likes.joinToString(";"), variationId)
      accountDb.update(UPDATE_ACCOUNT_LIKES, account.likes.joinToString(";"), id)
    }

    RateType.DISLIKE -> {
      if (variation.dislikes.contains(id.toString())) return@coroutineScope call.respond(HttpStatusCode.Conflict, "Already disliked")
      if (account.dislikes.contains(variationId)) return@coroutineScope call.respond(HttpStatusCode.Conflict, "Already disliked")

      if (variation.likes.contains(id.toString())) {
        variation.likes.remove(id.toString())
        account.likes.remove(variationId)
        db.update(UPDATE_VARIATION_LIKES, variation.likes.joinAndWrap("", "", ";"), variationId)
        accountDb.update(UPDATE_ACCOUNT_LIKES, account.likes.joinAndWrap("", "", ";"), id)
      }

      variation.dislikes.add(id.toString())
      account.dislikes.add(variationId)
      db.update(UPDATE_VARIATION_DISLIKES, variation.dislikes.joinAndWrap("", "", ";"), variationId)
      accountDb.update(UPDATE_ACCOUNT_DISLIKES, account.dislikes.joinAndWrap("", "", ";"), id)
    }
  }

  call.respond(HttpStatusCode.OK, object {
    val rated = true
  })
}