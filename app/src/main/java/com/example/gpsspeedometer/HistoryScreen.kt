package com.example.gpsspeedometer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    history: List<TripRecord>,
    onClearHistory: () -> Unit = {}
) {
    val tiffanyBlue = Color(0xFF0ABAB5)
    val mutedTiffany = Color(0x8A0ABAB5)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TRIP HISTORY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = tiffanyBlue
                )
                
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Clear All", color = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No trips recorded yet.",
                        color = mutedTiffany,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { record ->
                        HistoryItem(record, tiffanyBlue, mutedTiffany)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: TripRecord,
    accentColor: Color,
    mutedColor: Color
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(record.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = dateString,
                fontSize = 12.sp,
                color = mutedColor
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "AVG SPEED", fontSize = 10.sp, color = mutedColor)
                    Text(
                        text = "${record.avgSpeed} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "TOP SPEED", fontSize = 10.sp, color = mutedColor)
                    Text(
                        text = "${record.topSpeed} km/h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}
