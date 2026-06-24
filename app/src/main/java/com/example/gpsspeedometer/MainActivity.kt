package com.example.gpsspeedometer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val viewModel: SpeedometerViewModel by viewModels()

    // Timer handling variables
    private val idleHandler = Handler(Looper.getMainLooper())
    private var isTimerRunning = false
    private val idleTimeout: Long = 300000 // 5 minutes in milliseconds

    // Permission request dialog pop-up
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF000000)
            ) {
                MainScreen(viewModel)
            }
        }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500)
            .setMinUpdateIntervalMillis(250)
            .setMinUpdateDistanceMeters(0f)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    val rawSpeedKmH = (location.speed * 3.6).toInt()
                    val speed = if (rawSpeedKmH < 3) 0 else rawSpeedKmH
                    viewModel.updateSpeed(speed)

                    if (speed > 0) {
                        resetIdleTimer()
                    } else {
                        startIdleTimer()
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun startIdleTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true
            idleHandler.postDelayed({
                // Calculates the summary in the background, but the UI elements stay visible
                viewModel.calculateSummary()
            }, idleTimeout)
        }
    }

    private fun resetIdleTimer() {
        if (isTimerRunning) {
            idleHandler.removeCallbacksAndMessages(null)
            isTimerRunning = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        idleHandler.removeCallbacksAndMessages(null)
    }
}

@Composable
fun MainScreen(viewModel: SpeedometerViewModel) {
    val navController = rememberNavController()
    val items = listOf("speedometer", "history")
    val icons = listOf(Icons.Default.Speed, Icons.Default.History)
    val tiffanyBlue = Color(0xFF0ABAB5)

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0A0A0A),
                contentColor = tiffanyBlue
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                items.forEachIndexed { index, screen ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = null) },
                        label = { Text(screen.replaceFirstChar { it.uppercase() }) },
                        selected = currentRoute == screen,
                        onClick = {
                            navController.navigate(screen)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = tiffanyBlue,
                            unselectedIconColor = Color.Gray,
                            selectedTextColor = tiffanyBlue,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color(0xFF1A1A1A)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "speedometer",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("speedometer") {
                SpeedometerScreen(
                    currentSpeed = viewModel.currentSpeed,
                    avgSpeed = viewModel.avgSpeed,
                    topSpeed = viewModel.topSpeed,
                    onReset = { viewModel.resetStats() },
                    onSimulate = {
                        viewModel.isSimulating = !viewModel.isSimulating
                        if (viewModel.isSimulating) {
                            viewModel.updateSpeed(150)
                        } else {
                            viewModel.updateSpeed(0)
                        }
                    }
                )
            }
            composable("history") {
                HistoryScreen(
                    history = viewModel.tripHistory,
                    onClearHistory = { viewModel.clearHistory() }
                )
            }
        }
    }
}

@Composable
fun SpeedometerScreen(
    currentSpeed: Int,
    avgSpeed: Int,
    topSpeed: Int,
    onReset: () -> Unit = {},
    onSimulate: () -> Unit = {}
) {
    val tiffanyBlue = Color(0xFF0ABAB5)
    val mutedTiffany = Color(0x8A0ABAB5)
    val darkTiffany = Color(0xFF032B2A)
    val darkBg = Color(0xFF000000)

    val startAngle = 135f
    val sweepAngle = 270f
    val maxDisplaySpeed = 240f

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val gaugeSize = if (isLandscape) maxHeight * 0.8f else 340.dp

        // SIMULATE BUTTON
        TextButton(
            onClick = onSimulate,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = if (isLandscape) 0.dp else 25.dp)
        ) {
            Text(
                text = "Simulate",
                color = tiffanyBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // RESET BUTTON
        TextButton(
            onClick = onReset,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (isLandscape) 0.dp else 25.dp)
        ) {
            Text(
                text = "Reset",
                color = tiffanyBlue,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isLandscape) {
            // LANDSCAPE LAYOUT: Stats on sides
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Stat
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "AVG. SPEED", fontSize = 11.sp, color = mutedTiffany, fontWeight = FontWeight.Black)
                    Text(text = "$avgSpeed km/h", fontSize = 18.sp, color = tiffanyBlue, fontWeight = FontWeight.Bold)
                }

                // Center Gauge
                SpeedGauge(
                    currentSpeed = currentSpeed,
                    gaugeSize = gaugeSize,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    maxDisplaySpeed = maxDisplaySpeed,
                    tiffanyBlue = tiffanyBlue,
                    darkTiffany = darkTiffany,
                    mutedTiffany = mutedTiffany,
                    darkBg = darkBg
                )

                // Right Stat
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MAX. SPEED", fontSize = 11.sp, color = mutedTiffany, fontWeight = FontWeight.Black)
                    Text(text = "$topSpeed km/h", fontSize = 18.sp, color = tiffanyBlue, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // PORTRAIT LAYOUT: Original center gauge + bottom stats
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpeedGauge(
                    currentSpeed = currentSpeed,
                    gaugeSize = gaugeSize,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    maxDisplaySpeed = maxDisplaySpeed,
                    tiffanyBlue = tiffanyBlue,
                    darkTiffany = darkTiffany,
                    mutedTiffany = mutedTiffany,
                    darkBg = darkBg
                )
            }

            // Bottom stats for portrait
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "AVG. SPEED", fontSize = 13.sp, color = mutedTiffany, fontWeight = FontWeight.Black)
                    Text(text = "$avgSpeed km/h", fontSize = 20.sp, color = tiffanyBlue, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "MAX. SPEED", fontSize = 13.sp, color = mutedTiffany, fontWeight = FontWeight.Black)
                    Text(text = "$topSpeed km/h", fontSize = 20.sp, color = tiffanyBlue, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SpeedGauge(
    currentSpeed: Int,
    gaugeSize: androidx.compose.ui.unit.Dp,
    startAngle: Float,
    sweepAngle: Float,
    maxDisplaySpeed: Float,
    tiffanyBlue: Color,
    darkTiffany: Color,
    mutedTiffany: Color,
    darkBg: Color
) {
    Box(
        modifier = Modifier.size(gaugeSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size
            val strokeWidth = (canvasSize.width * 0.1f).coerceIn(12.dp.toPx(), 36.dp.toPx())
            val diameter = canvasSize.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val arcSize = Size(diameter, diameter)

            // Background Track Ring
            drawArc(
                color = darkTiffany,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Active Progress Sweep
            val speedRatio = (currentSpeed.coerceAtMost(maxDisplaySpeed.toInt()) / maxDisplaySpeed)
            val progressSweep = sweepAngle * speedRatio

            if (progressSweep > 0f) {
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(tiffanyBlue, tiffanyBlue.copy(alpha = 0.6f)),
                        center = Offset(canvasSize.width / 2, canvasSize.height / 2)
                    ),
                    startAngle = startAngle,
                    sweepAngle = progressSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Dial Notch Markers
            val totalTicks = 11
            val radius = diameter / 2
            val center = Offset(canvasSize.width / 2, canvasSize.height / 2)

            for (i in 0 until totalTicks) {
                val tickAngleDegrees = startAngle + (sweepAngle / (totalTicks - 1)) * i
                val tickAngleRad = Math.toRadians(tickAngleDegrees.toDouble())

                val startX = center.x + (radius - strokeWidth / 2) * cos(tickAngleRad).toFloat()
                val startY = center.y + (radius - strokeWidth / 2) * sin(tickAngleRad).toFloat()
                val endX = center.x + (radius + strokeWidth / 2) * cos(tickAngleRad).toFloat()
                val endY = center.y + (radius + strokeWidth / 2) * sin(tickAngleRad).toFloat()

                drawLine(
                    color = darkBg,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }

        // Central Readout Labels
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val labelSize = (gaugeSize.value * 0.06f).coerceAtLeast(14f).sp
            val speedSize = (gaugeSize.value * 0.25f).coerceAtLeast(40f).sp
            
            Text(text = "km/h", fontSize = labelSize, color = mutedTiffany, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$currentSpeed",
                fontSize = speedSize,
                fontWeight = FontWeight.Bold,
                color = tiffanyBlue
            )
        }

        // Min/Max Gauge Limit Labels
        val labelPadding = gaugeSize * 0.15f
        val markerSize = (gaugeSize.value * 0.06f).coerceAtLeast(14f).sp
        
        Text(
            text = "0",
            color = tiffanyBlue,
            fontSize = markerSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = labelPadding, bottom = labelPadding * 1.1f)
        )

        Text(
            text = "240",
            color = tiffanyBlue,
            fontSize = markerSize,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = labelPadding, bottom = labelPadding * 1.1f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun SpeedometerPreview() {
    SpeedometerScreen(currentSpeed = 0, avgSpeed = 0, topSpeed = 0)
}