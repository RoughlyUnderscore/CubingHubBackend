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
import com.roughlyunderscore.data.model.generateToken
import com.roughlyunderscore.ch.lib.FIND_ACCOUNT_BY_EMAIL
import com.roughlyunderscore.ch.lib.verifyHash
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.auth() {
  get("/auth") {
    coroutineScope {
      val email = call.parameters["email"] ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No email specified")
      val password = call.parameters["password"] ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No password specified")

      val res = accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(email)) {
        if (!it.next()) return@query (HttpStatusCode.Unauthorized to "Account not found or password incorrect")

        val hashInDatabase = it.getString("pwhash") ?: return@query (HttpStatusCode.InternalServerError to "Failed to fetch password")
        if (!hashInDatabase.startsWith("\$argon2")) return@query (HttpStatusCode.InternalServerError to "Password hash is of invalid format")

        val verified = verifyHash(hashInDatabase, password)
        if (!verified) return@query (HttpStatusCode.Unauthorized to "Account not found or password incorrect")

        val token = generateToken(email)
        return@query (HttpStatusCode.OK to token)
      }

      call.respond(res.first, res.second)
    }
  }
}