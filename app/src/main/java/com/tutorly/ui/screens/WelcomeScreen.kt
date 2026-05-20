package com.tutorly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WelcomeScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF59B88B),
                        Color(0xFF2C8E74),
                        Color(0xFF146E64)
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 36.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TopEducationBlock()
            Spacer(modifier = Modifier.height(28.dp))
            MascotBlock()
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Tutorly",
                color = Color.White,
                fontSize = 68.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(0.86f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.35f))
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    text = "Learn. Grow. Succeed.",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.White.copy(alpha = 0.35f))
            }
        }
    }
}

@Composable
private fun TopEducationBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Color(0x33FFFFFF))
            .padding(vertical = 28.dp, horizontal = 22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF143D56), modifier = Modifier.size(46.dp))
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFFD24A), modifier = Modifier.size(86.dp))
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF13395B), modifier = Modifier.size(46.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(26.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color(0x884CA064))
            )
        }
    }
}

@Composable
private fun MascotBlock() {
    Box(
        modifier = Modifier
            .size(190.dp)
            .clip(CircleShape)
            .background(Color(0x22FFFFFF)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🐱", fontSize = 110.sp)
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = Color(0xFFFFD24A),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 18.dp)
                .size(20.dp)
        )
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = Color(0xFF1A3658),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
                .size(28.dp)
        )
    }
}
