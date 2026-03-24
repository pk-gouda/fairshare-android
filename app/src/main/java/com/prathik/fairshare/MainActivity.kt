package com.prathik.fairshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.prathik.fairshare.ui.theme.FairShareTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.foundation.background
import com.prathik.fairshare.ui.theme.Surface0

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FairShareTheme {
                Box(
                    modifier          = Modifier.fillMaxSize().background(Surface0),
                    contentAlignment  = Alignment.Center,
                ) {
                    Text(
                        text  = "FairShare",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color(0xFF22C97A),
                    )
                }
            }
        }
    }
}