package com.bdf.saleor.data

/**
 * Build-time Saleor catalog settings. Injected once via Hilt; not mutable at runtime.
 *
 * [locale] is the storefront URL slug (e.g. `ko`). GraphQL uses the uppercase
 * Saleor [LanguageCodeEnum] value (e.g. `KO`).
 */
data class SaleorCatalogConfig(
    val apiUrl: String,
    val channel: String,
    val locale: String,
    val checkoutCountry: String,
    val featuredCollectionSlug: String,
    val storefrontUrl: String,
    /** Strapi CMS base URL used for extra product-detail blocks. */
    val cmsUrl: String,
) {
    val graphqlLanguageCode: String get() = locale.uppercase()

    val passwordResetRedirectUrl: String
        get() = "${storefrontUrl.trimEnd('/')}/$locale/$channel/reset-password"

    val accountConfirmRedirectUrl: String
        get() = "${storefrontUrl.trimEnd('/')}/$locale/$channel/account/confirm"

    val accountDeleteRedirectUrl: String
        get() = "${storefrontUrl.trimEnd('/')}/$locale/$channel/account"
}
