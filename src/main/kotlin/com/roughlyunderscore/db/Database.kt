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

package com.roughlyunderscore.db

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.coroutineScope
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class Database(
  private val url: String,
  private val port: String,
  private val database: String,
  private val username: String,
  private val password: String,
) {
  private var dataSource: HikariDataSource = HikariDataSource().apply {
    this.jdbcUrl = "jdbc:mariadb://$url:$port/$database"
    this.username = this@Database.username
    this.password = this@Database.password
  }

  suspend fun update(query: String, vararg params: Any?) = coroutineScope {
    dataSource.connection.use { conn ->
      conn.prepareStatement(query).use { statement ->
        params.forEachIndexed { index, param -> statement.setObject(index + 1, param) }

        statement.executeUpdate()
      }
    }

    Unit
  }

  suspend fun update(query: String, params: List<Any>?) = update(query, *params?.toTypedArray() ?: emptyArray())

  suspend fun <R> query(query: String, params: List<Any>, block: suspend (ResultSet) -> R): R = coroutineScope {
    dataSource.connection.use { conn ->
      conn.prepareStatement(query).use { statement ->
        params.forEachIndexed { index, param -> statement.setObject(index + 1, param) }

        return@coroutineScope statement.executeQuery().use { rs -> block(rs) }
      }
    }
  }
}

