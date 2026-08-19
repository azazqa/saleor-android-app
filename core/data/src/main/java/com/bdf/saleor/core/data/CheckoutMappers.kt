package com.bdf.saleor.core.data

import com.apollographql.apollo.api.Optional
import com.bdf.saleor.core.model.Address
import com.bdf.saleor.core.model.AddressDraft
import com.bdf.saleor.core.model.CartLine
import com.bdf.saleor.core.model.CheckoutAuthorizeStatus
import com.bdf.saleor.core.model.CheckoutSession
import com.bdf.saleor.core.model.Money
import com.bdf.saleor.core.model.PaymentGatewayInfo
import com.bdf.saleor.core.model.PaymentResult
import com.bdf.saleor.graphql.fragment.CheckoutAddress
import com.bdf.saleor.graphql.fragment.CheckoutDetails
import com.bdf.saleor.graphql.fragment.CheckoutLineDetails
import com.bdf.saleor.graphql.fragment.CheckoutMoney
import com.bdf.saleor.graphql.type.AddressInput
import com.bdf.saleor.graphql.type.CheckoutAuthorizeStatusEnum
import com.bdf.saleor.graphql.type.CountryCode

internal fun CheckoutMoney.toMoney(): Money = Money(amount, currency)

internal fun CheckoutAddress.toAddress(): Address = Address(
    id = id,
    firstName = firstName,
    lastName = lastName,
    companyName = companyName,
    streetAddress1 = streetAddress1,
    streetAddress2 = streetAddress2,
    city = city,
    cityArea = cityArea,
    postalCode = postalCode,
    countryCode = country.code,
    countryName = country.country,
    countryArea = countryArea,
    phone = phone.orEmpty(),
    isDefaultShipping = isDefaultShippingAddress == true,
    isDefaultBilling = isDefaultBillingAddress == true,
)

internal fun CheckoutLineDetails.toCartLine(): CartLine {
    val variantName = variant.translation?.name?.takeIf { it.isNotBlank() }
        ?: variant.name.takeIf { it.isNotBlank() }
        ?: variant.selectionAttributes.flatMap { attr ->
            attr.values.map { it.translation?.name ?: it.name.orEmpty() }
        }.filter { it.isNotBlank() }.joinToString(" / ")
    return CartLine(
        id = id,
        variantId = variant.id,
        productName = variant.product.translation?.name ?: variant.product.name,
        variantName = variantName,
        thumbnailUrl = variant.product.thumbnail?.url,
        quantity = quantity,
        unitPrice = unitPrice.gross.checkoutMoney.toMoney(),
        totalPrice = totalPrice.gross.checkoutMoney.toMoney(),
    )
}

internal fun CheckoutDetails.toSession(): CheckoutSession = CheckoutSession(
    id = id,
    email = email,
    lines = lines.map { it.checkoutLineDetails.toCartLine() },
    quantity = quantity,
    subtotal = subtotalPrice.gross.checkoutMoney.toMoney(),
    shipping = shippingPrice.gross.checkoutMoney.toMoney(),
    discount = discount?.checkoutMoney?.toMoney(),
    voucherCode = voucherCode,
    discountName = discountName,
    total = totalPrice.gross.checkoutMoney.toMoney(),
    totalBalance = totalBalance.checkoutMoney.toMoney(),
    shippingAddress = shippingAddress?.checkoutAddress?.toAddress(),
    billingAddress = billingAddress?.checkoutAddress?.toAddress(),
    isShippingRequired = isShippingRequired,
    selectedDeliveryMethodId = delivery?.id,
    authorizeStatus = when (authorizeStatus) {
        CheckoutAuthorizeStatusEnum.NONE -> CheckoutAuthorizeStatus.NONE
        CheckoutAuthorizeStatusEnum.PARTIAL -> CheckoutAuthorizeStatus.PARTIAL
        CheckoutAuthorizeStatusEnum.FULL -> CheckoutAuthorizeStatus.FULL
        else -> CheckoutAuthorizeStatus.UNKNOWN
    },
    availablePaymentGateways = availablePaymentGateways.map { gateway ->
        PaymentGatewayInfo(
            id = gateway.id,
            name = gateway.name,
            currencies = gateway.currencies,
            config = gateway.config.associate { it.field to it.value.orEmpty() },
        )
    },
    channelSlug = channel.slug,
)

internal fun AddressDraft.toCheckoutInput(): AddressInput = AddressInput(
    firstName = Optional.present(firstName.trim()),
    lastName = Optional.present(lastName.trim().ifBlank { firstName.trim() }),
    companyName = Optional.present(companyName.trim()),
    streetAddress1 = Optional.present(streetAddress1.trim()),
    streetAddress2 = Optional.present(streetAddress2.trim()),
    city = Optional.present(city.trim().ifBlank { streetAddress1.trim() }),
    cityArea = Optional.present(cityArea.trim()),
    postalCode = Optional.present(postalCode.trim()),
    country = Optional.present(CountryCode.safeValueOf(countryCode.ifBlank { "KR" })),
    countryArea = Optional.present(countryArea.trim()),
    phone = Optional.presentIfNotNull(phone.trim().takeIf { it.isNotEmpty() }),
)

internal fun parseTossTransactionData(
    transactionId: String?,
    data: Any?,
    authorizedAmount: Double? = null,
): PaymentResult {
    val map = data as? Map<*, *>
    val amountValue = when (val amount = map?.get("amount")) {
        is Map<*, *> -> (amount["value"] as? Number)?.toDouble()
        is Number -> amount.toDouble()
        else -> null
    }
    val orderId = map?.get("orderId") as? String
    val orderName = map?.get("orderName") as? String
    if (transactionId.isNullOrBlank() || orderId.isNullOrBlank() || orderName.isNullOrBlank() || amountValue == null) {
        return PaymentResult(success = false, message = "결제 정보를 확인할 수 없습니다")
    }
    return PaymentResult(
        success = true,
        transactionId = transactionId,
        orderId = orderId,
        orderName = orderName,
        amount = amountValue,
        currency = ((map["amount"] as? Map<*, *>)?.get("currency") as? String) ?: "KRW",
        customerKey = map["customerKey"] as? String,
        authorizedAmount = authorizedAmount,
    )
}

internal fun parseClientKey(data: Any?): String? {
    val map = data as? Map<*, *> ?: return null
    return (map["clientKey"] as? String)?.takeIf { it.isNotBlank() }
}
