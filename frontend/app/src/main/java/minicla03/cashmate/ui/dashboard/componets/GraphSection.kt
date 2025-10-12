package minicla03.cashmate.ui.dashboard.componets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

@Composable
fun GraphSection(modifier: Modifier = Modifier)
{
    val pieEntries = listOf(
        PieEntry(40f, "Cibo"),
        PieEntry(30f, "Trasporti"),
        PieEntry(20f, "Intrattenimento"),
        PieEntry(10f, "Altro")
    )

    val colors = listOf(
        Color(0xFF6A5AE0), // viola
        Color(0xFF4DB6AC), // acqua
        Color(0xFFFFB74D), // arancio tenue
        Color(0xFF64B5F6)  // blu chiaro
    )

    Column(
        modifier = modifier
            .background(Color.White)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Spese per categoria",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1E1E2F)
        )

        Spacer(Modifier.height(20.dp))

        AndroidView(
            factory = { context ->
                PieChart(context).apply {
                    val dataSet = PieDataSet(pieEntries, "")
                    dataSet.colors = colors.map { it.toArgb() }
                    dataSet.valueTextColor = Color.White.toArgb()
                    dataSet.valueTextSize = 14f
                    data = PieData(dataSet)

                    description.isEnabled = false
                    setUsePercentValues(true)
                    setDrawEntryLabels(false)

                    legend.isEnabled = true
                    legend.textSize = 14f
                    legend.form = Legend.LegendForm.CIRCLE
                    legend.textColor = Color(0xFF1E1E2F).toArgb()
                    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                    legend.orientation = Legend.LegendOrientation.HORIZONTAL
                    legend.setDrawInside(false)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            update = { chart -> chart.invalidate() }
        )
    }
}