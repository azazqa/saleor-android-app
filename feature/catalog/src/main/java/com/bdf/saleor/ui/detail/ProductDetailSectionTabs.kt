package com.bdf.saleor.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bdf.saleor.feature.catalog.R

enum class ProductDetailSection {
    Summary,
    Detail,
    Qa,
}

private data class ProductDetailSectionSpec(
    val section: ProductDetailSection,
    val labelRes: Int,
    val testTag: String,
)

private val ProductDetailSectionSpecs = listOf(
    ProductDetailSectionSpec(ProductDetailSection.Summary, R.string.tab_summary, "product_detail_tab_summary"),
    ProductDetailSectionSpec(ProductDetailSection.Detail, R.string.tab_detail_description, "product_detail_tab_detail"),
    ProductDetailSectionSpec(ProductDetailSection.Qa, R.string.tab_qa, "product_detail_tab_qa"),
)

@Composable
fun ProductDetailSectionTabs(
    selected: ProductDetailSection,
    onSelect: (ProductDetailSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .testTag("product_detail_section_tabs"),
    ) {
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            ProductDetailSectionSpecs.forEach { spec ->
                ProductDetailSectionTab(
                    label = stringResource(spec.labelRes),
                    selected = spec.section == selected,
                    testTag = spec.testTag,
                    onClick = { onSelect(spec.section) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = colors.outlineVariant)
    }
}

@Composable
private fun ProductDetailSectionTab(
    label: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            color = if (selected) colors.onBackground else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(2.dp)
                .background(if (selected) colors.onBackground else colors.background),
        )
    }
}

internal object ProductDetailSectionIndices {
    const val GALLERY = 0
    const val BUY_BOX = 1
    const val STICKY_TABS = 2
    const val SUMMARY = 3
    const val DETAIL = 4
    const val QA = 5
}

internal fun ProductDetailSection.toLazyItemIndex(): Int = when (this) {
    ProductDetailSection.Summary -> ProductDetailSectionIndices.SUMMARY
    ProductDetailSection.Detail -> ProductDetailSectionIndices.DETAIL
    ProductDetailSection.Qa -> ProductDetailSectionIndices.QA
}
