package com.bdf.saleor.feature.catalog.detail

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
}

private data class ProductDetailSectionSpec(
    val section: ProductDetailSection,
    val labelRes: Int,
    val testTag: String,
)

private val ProductDetailSectionSpecs = listOf(
    ProductDetailSectionSpec(ProductDetailSection.Summary, R.string.tab_summary, "product_detail_tab_summary"),
    ProductDetailSectionSpec(
        ProductDetailSection.Detail,
        R.string.tab_detail_description,
        "product_detail_tab_detail",
    ),
)

@Composable
fun ProductDetailSectionTabs(
    sections: List<ProductDetailSection>,
    selected: ProductDetailSection,
    onSelect: (ProductDetailSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (sections.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val specs = ProductDetailSectionSpecs.filter { it.section in sections }
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
            specs.forEach { spec ->
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
fun ProductDetailSectionTitle(
    section: ProductDetailSection,
    modifier: Modifier = Modifier,
) {
    val labelRes = when (section) {
        ProductDetailSection.Summary -> R.string.tab_summary
        ProductDetailSection.Detail -> R.string.tab_detail_description
    }
    Text(
        text = stringResource(labelRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("product_detail_section_title"),
    )
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
            color = if (selected) colors.primary else colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Box(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(3.dp)
                .background(if (selected) colors.primary else colors.background),
        )
    }
}
