package com.bdf.saleor.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Home : NavKey

@Serializable
data object Categories : NavKey

@Serializable
data object Search : NavKey

@Serializable
data object Account : NavKey

@Serializable
data class ProductList(
    val source: String,
    val slug: String = "",
    val title: String = "",
) : NavKey

@Serializable
data class ProductDetail(val slug: String) : NavKey

@Serializable
data object Register : NavKey

@Serializable
data object ForgotPassword : NavKey

@Serializable
data object Orders : NavKey

@Serializable
data class OrderDetail(val id: String) : NavKey
