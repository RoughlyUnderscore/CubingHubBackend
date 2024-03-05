package com.roughlyunderscore.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Subset(val id: Int, val name: String, val imageUrl: String, val algorithms: List<Algorithm>)
