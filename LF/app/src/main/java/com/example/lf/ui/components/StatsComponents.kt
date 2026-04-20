package com.example.lf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lf.R
import com.example.lf.ui.theme.*

@Composable
fun PeriodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Primary else BackgroundCard,
            contentColor = if (isSelected) Color.White else TextSecondary
        )
    ) {
        Text(text, fontSize = 14.sp)
    }
}

@Composable
fun StatValueCard(
    value: String,
    label: String,
    icon: Int,
    color: Color
) {
    Column(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun TopExerciseItem(
    name: String,
    count: Int,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 14.sp, color = TextPrimary)
            Text("$count раз", fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = progress.coerceIn(0f, 1f),
            modifier = Modifier.fillMaxWidth(),
            color = Primary,
            trackColor = Primary.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun HistoryItem(
    date: String,
    exercises: String,
    duration: String,
    accuracy: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(date, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(exercises, fontSize = 10.sp, color = TextSecondary)
        }
        Text(duration, fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(horizontal = 8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Success.copy(alpha = 0.15f)
        ) {
            Text("$accuracy%", fontSize = 12.sp, color = Success, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
        }
    }
}