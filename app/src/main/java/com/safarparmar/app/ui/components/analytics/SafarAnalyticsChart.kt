package com.safarparmar.app.ui.components.analytics

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.*
import com.safarparmar.app.performance.LocalMotionPolicy

/** App-owned Vico adapter. Values and labels come from the feature's observable state. */
@Composable
fun SafarAnalyticsChart(
    values: List<Number>,
    labels: List<String>,
    color: Color,
    description: String,
    modifier: Modifier = Modifier,
    columns: Boolean = false,
    percent: Boolean = false,
    foreground: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showAxis: Boolean = true,
    xValues: List<Number> = values.indices.toList(),
) {
    require(values.size == labels.size && values.size == xValues.size)
    if (values.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { Text("No recorded activity yet", color = foreground) }
        return
    }
    val producer = remember { CartesianChartModelProducer() }
    LaunchedEffect(values, columns, xValues) {
        producer.runTransaction {
            if (columns) columnSeries { series(xValues, values) } else lineSeries { series(xValues, values) }
        }
    }
    val maximum = if (percent) 100.0 else (values.maxOf { it.toDouble() } * 1.15).coerceAtLeast(1.0)
    val range = remember(maximum) { CartesianLayerRangeProvider.fixed(minY = 0.0, maxY = maximum) }
    val label = rememberTextComponent(TextStyle(color = foreground, fontSize = 10.sp))
    val grid = rememberLineComponent(Fill(foreground.copy(alpha = 0.12f)), 1.dp)
    val formatter = remember(percent) { CartesianValueFormatter { _, value, _ -> "${value.toInt()}${if (percent) "%" else ""}" } }
    val xFormatter = remember(labels, xValues) { CartesianValueFormatter { _, value, _ ->
        val index = xValues.indexOfFirst { it.toDouble() == value }
        labels.getOrNull(index) ?: "—"
    } }
    val markerLabel = rememberTextComponent(
        style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp),
        padding = Insets(10.dp, 6.dp),
        background = rememberShapeComponent(Fill(MaterialTheme.colorScheme.surface), RoundedCornerShape(8.dp)),
    )
    val markerFormatter = remember(labels, xValues, percent) {
        val valueFormatter = DefaultCartesianMarker.ValueFormatter.default(
            decimalCount = 0, suffix = if (percent) "%" else " lectures", colorCode = false,
        )
        DefaultCartesianMarker.ValueFormatter { context, targets ->
            val index = targets.firstOrNull()?.x?.let { x -> xValues.indexOfFirst { it.toDouble() == x } } ?: -1
            "${labels.getOrNull(index).orEmpty()} · ${valueFormatter.format(context, targets)}"
        }
    }
    val marker = rememberDefaultCartesianMarker(
        label = markerLabel,
        valueFormatter = markerFormatter,
        guideline = grid,
    )
    val layer = if (columns) {
        rememberColumnCartesianLayer(
            columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                rememberLineComponent(Fill(color), 18.dp, RoundedCornerShape(6.dp)),
            ),
            rangeProvider = range,
            dataLabel = label,
            dataLabelValueFormatter = formatter,
        )
    } else {
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(
                LineCartesianLayer.rememberLine(
                    fill = LineCartesianLayer.LineFill.single(Fill(color)),
                    stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 3.dp),
                    areaFill = LineCartesianLayer.AreaFill.single(Fill(Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.01f))))),
                ),
            ),
            rangeProvider = range,
        )
    }
    CartesianChartHost(
        chart = rememberCartesianChart(
            layer,
            startAxis = if (showAxis) VerticalAxis.rememberStart(
                label = label, valueFormatter = formatter, line = null, tick = null,
                guideline = grid, itemPlacer = VerticalAxis.ItemPlacer.step({ if (percent) 50.0 else kotlin.math.ceil(maximum / 3).coerceAtLeast(1.0) }),
            ) else null,
            bottomAxis = HorizontalAxis.rememberBottom(
                label = label, valueFormatter = xFormatter, line = null, tick = null, guideline = null,
                itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { if (columns) 1 else ((values.size - 1) / 3).coerceAtLeast(1) }),
            ),
            marker = marker,
        ),
        modelProducer = producer,
        modifier = modifier.semantics { contentDescription = description },
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
        animationSpec = if (LocalMotionPolicy.current.decorationsEnabled) tween(600) else null,
        placeholder = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading chart…", color = foreground) } },
    )
}
