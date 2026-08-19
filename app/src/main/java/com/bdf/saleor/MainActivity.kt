package com.bdf.saleor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bdf.saleor.ui.navigation.SaleorApp
import com.bdf.saleor.core.designsystem.theme.SaleorAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SaleorAppTheme {
                SaleorApp()
            }
        }
    }
}
