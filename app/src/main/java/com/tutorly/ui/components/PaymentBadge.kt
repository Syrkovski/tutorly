package com.tutorly.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PaymentBadge(paid: Boolean) {
    val (txt, bg, fg) = if (paid)
        Triple("Оплачено", Color(0xFFE6F4EA), Color(0xFF2E7D32))
    else
        Triple("Долг", Color(0xFFFFF3E0), Color(0xFFEF6C00))
    Surface(color = bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)) {
        Text(txt, color = fg, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}
