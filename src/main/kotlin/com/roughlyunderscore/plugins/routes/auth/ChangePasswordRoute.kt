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
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope

fun Route.changePassword() {
  get("/change_password") {
    coroutineScope {
      val tokenString = call.parameters["token"] ?: return@coroutineScope call.respond(HttpStatusCode.Unauthorized, "No token specified")
      val token = fullValidateFetchPayload(tokenString)

      if (token.second != null) return@coroutineScope call.respond(token.second!!.first, token.second!!.second)

      val payload = token.first!!
      val email = payload.subject ?: return@coroutineScope call.respond(HttpStatusCode.NotFound, "No email specified")

      val response = accountDb.query(FIND_ACCOUNT_BY_EMAIL, listOf(email)) {
        if (!it.next()) return@query (HttpStatusCode.NotFound to "Account not found")

        val oldPassword = call.parameters["oldpassword"] ?: return@query (HttpStatusCode.Unauthorized to "No password specified")
        val passwordInDatabase = it.getString("pwhash") ?: return@query (HttpStatusCode.InternalServerError to "Failed to fetch password")

        val verified = verifyHash(passwordInDatabase, oldPassword)
        if (!verified) return@query (HttpStatusCode.Unauthorized to "Account not found or password incorrect")

        val newPassword = call.parameters["newpassword"] ?: return@query (HttpStatusCode.NotFound to "No new password specified")
        val newHash = hash(newPassword)

        if (!isSecurePassword(newPassword)) return@query (HttpStatusCode.BadRequest to "Password is not secure enough")

        val lastChange = it.getLong("last_pw_change")
        val endSessions = call.parameters["endsessions"]?.toBoolean() ?: false

        val timestamp = if (endSessions) System.currentTimeMillis() else lastChange

        accountDb.update(UPDATE_ACCOUNT, newHash, timestamp, email)
        return@query (HttpStatusCode.OK to "")
      }

      call.respond(response.first, response.second)
    }
  }
}