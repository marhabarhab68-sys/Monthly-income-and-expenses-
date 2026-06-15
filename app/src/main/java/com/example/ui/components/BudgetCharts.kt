package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MonthlyTrend

@Composable
fun CategoryDonutChart(
    categoryTotals: Map<String, Double>,
    categoryColors: Map<String, String>,
    modifier: Modifier = Modifier
) {
    if (categoryTotals.isEmpty() || categoryTotals.values.sum() == 0.0) {
        Box(
            modifier = modifier.height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No expensive activities tracked in this range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = categoryTotals.values.sum()
    val rawValues = categoryTotals.values.toList()
    val labels = categoryTotals.keys.toList()
    
    val angles = rawValues.map { (it / total * 360f).toFloat() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The Donut
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                angles.forEachIndexed { i, sweepAngle ->
                    val colorHex = categoryColors[labels[i]] ?: "#4CAF50"
                    val segmentColor = try {
                        Color(android.graphics.Color.parseColor(colorHex))
                    } catch (e: Exception) {
                        Color.Gray
                    }
                    
                    drawArc(
                        color = segmentColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30f)
                    )
                    startAngle += sweepAngle
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Total Spend", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    String.format(java.util.Locale.getDefault(), "$%.0f", total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Legend
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            labels.take(4).forEachIndexed { index, label ->
                val amt = categoryTotals[label] ?: 0.0
                val colorHex = categoryColors[label] ?: "#4CAF50"
                val segmentColor = try {
                    Color(android.graphics.Color.parseColor(colorHex))
                } catch (e: Exception) {
                    Color.Gray
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .padding(end = 4.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = segmentColor)
                        }
                    }
                    Text(
                        text = "$label ($/$$amt)",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (labels.size > 4) {
                Text(
                    "+ ${labels.size - 4} more categories",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun MultiMonthTrendChart(
    trends: List<MonthlyTrend>,
    modifier: Modifier = Modifier
) {
    if (trends.isEmpty()) return

    val maxVal = (trends.map { maxOf(it.income, it.expense) }.maxOrNull() ?: 1000.0).coerceAtLeast(100.0)

    val expenseColor = Color(0xFFC62828)
    val incomeColor = Color(0xFF2E7D32)
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(16.dp)
    ) {
        // Chart container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val paddingLeft = 60f
                val paddingBottom = 40f
                val activeWidth = width - paddingLeft
                val activeHeight = height - paddingBottom

                // Draw horizontal guide lines & values
                val levels = 4
                for (l in 0..levels) {
                    val y = activeHeight - (l * activeHeight / levels)
                    val textVal = (l * maxVal / levels).toInt()
                    
                    drawLine(
                        color = axisColor.copy(alpha = 0.5f),
                        start = Offset(paddingLeft, y),
                        end = Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                // Draw Axis
                drawLine(
                    color = axisColor,
                    start = Offset(paddingLeft, 0f),
                    end = Offset(paddingLeft, activeHeight),
                    strokeWidth = 2f
                )
                drawLine(
                    color = axisColor,
                    start = Offset(paddingLeft, activeHeight),
                    end = Offset(width, activeHeight),
                    strokeWidth = 2f
                )

                // Draw Bars for each month
                val monthCount = trends.size
                val spaceBetweenMonths = activeWidth / monthCount
                
                trends.forEachIndexed { idx, trend ->
                    val centerX = paddingLeft + (idx * spaceBetweenMonths) + (spaceBetweenMonths / 2f)
                    
                    // Base heights
                    val incomeBarHeight = (trend.income / maxVal * activeHeight).toFloat()
                    val expenseBarHeight = (trend.expense / maxVal * activeHeight).toFloat()

                    val barWidth = 20f

                    // Draw Income Bar (Green)
                    val incLeft = centerX - barWidth - 4f
                    val incTop = activeHeight - incomeBarHeight
                    drawRect(
                        color = incomeColor,
                        topLeft = Offset(incLeft, incTop),
                        size = Size(barWidth, incomeBarHeight)
                    )

                    // Draw Expense Bar (Red)
                    val expLeft = centerX + 4f
                    val expTop = activeHeight - expenseBarHeight
                    drawRect(
                        color = expenseColor,
                        topLeft = Offset(expLeft, expTop),
                        size = Size(barWidth, expenseBarHeight)
                    )
                }
            }
        }

        // Custom Labels (Month Titles) below canvas to ensure standard SP typographic accessibility
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            trends.forEach { trend ->
                Text(
                    text = trend.monthLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        
        // Legend indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(12.dp).padding(end = 4.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = incomeColor) }
            }
            Text("Income", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(end = 16.dp))
            Box(modifier = Modifier.size(12.dp).padding(end = 4.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) { drawRect(color = expenseColor) }
            }
            Text("Expenses", style = MaterialTheme.typography.labelMedium)
        }
    }
}
