package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dsp.*
import com.example.model.FilterEntity
import com.example.ui.components.ImpulseResponsePlot
import com.example.ui.components.MagnitudeResponsePlot
import com.example.ui.components.PhaseResponsePlot
import com.example.ui.viewmodel.FilterViewModel
import kotlin.math.roundToInt

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DESIGN("Design", Icons.Default.Settings),
    ANALYSIS("Analysis", Icons.Default.Info),
    COEFFICIENTS("Coefficients", Icons.Default.List),
    SAVED("Saved", Icons.Default.Favorite)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterAppScreen(
    viewModel: FilterViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(AppTab.DESIGN) }

    // Screen-edge adaptive state: screen layouts toggle if wide enough (tablet or landscape)
    BoxWithConstraints(modifier = modifier.fillMaxSize().navigationBarsPadding()) {
        val isWide = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Styled menu button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFDDE2F9), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = Color(0xFF191C1E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Text(
                            text = "FilterDesigner",
                            color = Color(0xFF191C1E),
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // Settings Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { /* Settings Trigger */ }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF43474E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            bottomBar = {
                // Pill-shaped bottom navigation for compact screens
                if (!isWide) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .background(Color(0xFFDDE2F9), RoundedCornerShape(28.dp))
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AppTab.values().forEach { tab ->
                                val selected = activeTab == tab
                                Box(
                                    modifier = Modifier
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(if (selected) Color(0xFF1B2D5D) else Color.Transparent)
                                        .clickable { activeTab = tab }
                                        .padding(horizontal = 12.dp)
                                        .testTag("tab_item_${tab.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.title,
                                            tint = if (selected) Color.White else Color(0xFF1B2D5D),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (selected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = tab.title,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Adaptive Navigation Rail for Wide Screens
                if (isWide) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        AppTab.values().forEach { tab ->
                            NavigationRailItem(
                                selected = activeTab == tab,
                                onClick = { activeTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag("rail_item_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                // Main Content View
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val result by viewModel.activeResult.collectAsStateWithLifecycle()
                    val errorMsg by viewModel.errorMsg.collectAsStateWithLifecycle()
                    val savedFilters by viewModel.savedFilters.collectAsStateWithLifecycle()

                    AnimatedContent(
                        targetState = activeTab,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "TabTransition"
                    ) { tab ->
                        when (tab) {
                            AppTab.DESIGN -> DesignTab(
                                viewModel = viewModel,
                                result = result,
                                errorMsg = errorMsg
                            )
                            AppTab.ANALYSIS -> AnalysisTab(
                                result = result,
                                errorMsg = errorMsg
                            )
                            AppTab.COEFFICIENTS -> CoefficientsTab(
                                result = result
                            )
                            AppTab.SAVED -> SavedTab(
                                savedFilters = savedFilters,
                                onLoad = { entity ->
                                    viewModel.loadSavedFilter(entity)
                                    activeTab = AppTab.DESIGN
                                },
                                onDelete = { id ->
                                    viewModel.deleteSavedFilter(id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==================== TABS IMPLEMENTATION ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignTab(
    viewModel: FilterViewModel,
    result: FilterResult?,
    errorMsg: String?
) {
    val sampleRate by viewModel.sampleRate.collectAsStateWithLifecycle()
    val filterMethod by viewModel.filterMethod.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val fc1 by viewModel.fc1.collectAsStateWithLifecycle()
    val fc2 by viewModel.fc2.collectAsStateWithLifecycle()
    val firTaps by viewModel.firTaps.collectAsStateWithLifecycle()
    val windowType by viewModel.windowType.collectAsStateWithLifecycle()
    val iirOrder by viewModel.iirOrder.collectAsStateWithLifecycle()
    val chebyRippleDb by viewModel.chebyRippleDb.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Validation Error Banner
        if (errorMsg != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMsg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Section 1: Filter Method & Architecture
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Design Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Filter Topology", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Segmented Filter Method Choices
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterMethod.values().forEach { method ->
                            val selected = filterMethod == method
                            val label = when (method) {
                                FilterMethod.FIR_WINDOW -> "FIR Window"
                                FilterMethod.IIR_BUTTERWORTH -> "IIR Butter"
                                FilterMethod.IIR_CHEBYSHEV -> "IIR Chebyshev"
                            }
                            OutlinedButton(
                                onClick = { viewModel.setFilterMethod(method) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("method_${method.name.lowercase()}"),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Filter Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Segmented Filter Type Choices
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterType.values().forEach { type ->
                            val isSupported = true
                            val selected = filterType == type
                            val label = type.name.lowercase().replaceFirstChar { it.uppercase() }

                            OutlinedButton(
                                onClick = { if (isSupported) viewModel.setFilterType(type) },
                                enabled = isSupported,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                ),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .height(36.dp)
                                    .testTag("type_${type.name.lowercase()}")
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                }
            }
        }

        // Section 2: Frequencies, Rates, Taps, Windows
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Frequency Specifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Sampling Rate Input
                    Text("Sampling Rate ($sampleRate Hz)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))

                    var rateTextState by remember(sampleRate) { mutableStateOf(sampleRate.roundToInt().toString()) }
                    OutlinedTextField(
                        value = rateTextState,
                        onValueChange = { newVal ->
                            rateTextState = newVal
                            newVal.toDoubleOrNull()?.let { viewModel.setSampleRate(it) }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(fontSize = 14.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .testTag("sample_rate_input"),
                        trailingIcon = { Text("Hz", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp)) }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Pre-config frequency density chips
                    val ratePresets = listOf(8000.0, 16000.0, 44100.0, 48000.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ratePresets.forEach { preset ->
                            val label = when (preset) {
                                8000.0 -> "8 kHz"
                                16000.0 -> "16 kHz"
                                44100.0 -> "44.1 kHz"
                                48000.0 -> "48 kHz"
                                else -> "$preset"
                            }
                            SuggestionChip(
                                onClick = { viewModel.setSampleRate(preset) },
                                label = { Text(label, fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (sampleRate == preset) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Cutoff Frequency 1 Slider / Input
                    val maxVal = sampleRate / 2.0
                    val fc1Min = 1f
                    val fc1Max = maxOf(2f, (maxVal - 10f).toFloat())
                    val fc1Coerced = fc1.toFloat().coerceIn(fc1Min, fc1Max)
                    Text("Cutoff Frequency 1: ${fc1.roundToInt()} Hz", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Slider(
                        value = fc1Coerced,
                        onValueChange = { viewModel.setFc1(it.roundToInt().toDouble()) },
                        valueRange = fc1Min..fc1Max,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cutoff_slider_1")
                    )

                    // Cutoff Frequency 2 (Bandpass / Bandstop only)
                    if (filterType == FilterType.BANDPASS || filterType == FilterType.BANDSTOP) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val fc2Min = fc1Coerced + 10f
                        val fc2Max = maxOf(fc2Min + 1f, (maxVal - 1f).toFloat())
                        val fc2Coerced = fc2.toFloat().coerceIn(fc2Min, fc2Max)
                        Text("Cutoff Frequency 2: ${fc2.roundToInt()} Hz", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Slider(
                            value = fc2Coerced,
                            onValueChange = { viewModel.setFc2(it.roundToInt().toDouble()) },
                            valueRange = fc2Min..fc2Max,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cutoff_slider_2")
                        )
                    }
                }
            }
        }

        // Section 3: Algorithm Specific params
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Topology Parameters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (filterMethod == FilterMethod.FIR_WINDOW) {
                        // FIR Parameters
                        Text("Filter Length (Taps): $firTaps", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        val tapsMin = 3f
                        val tapsMax = 127f
                        val tapsCoerced = firTaps.toFloat().coerceIn(tapsMin, tapsMax)
                        Slider(
                            value = tapsCoerced,
                            onValueChange = { viewModel.setFirTaps(it.roundToInt()) },
                            valueRange = tapsMin..tapsMax,
                            steps = 62, // Only odds? Since (127-3)/2 = 62 steps maps exactly to odd taps!
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("taps_slider")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Window Type", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WindowType.values().forEach { win ->
                                val selected = windowType == win
                                val label = win.name.lowercase().replaceFirstChar { it.uppercase() }
                                OutlinedButton(
                                    onClick = { viewModel.setWindowType(win) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)
                                        .height(36.dp)
                                        .testTag("window_${win.name.lowercase()}")
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // IIR Parameters
                        Text("Linear Filter Order: $iirOrder", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 6, 8).forEach { order ->
                                val selected = iirOrder == order
                                OutlinedButton(
                                    onClick = { viewModel.setIirOrder(order) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("order_$order")
                                ) {
                                    Text("Order $order", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (filterMethod == FilterMethod.IIR_CHEBYSHEV) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("Passband Ripple: %.1f dB".format(chebyRippleDb), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            val rippleMin = 0.1f
                            val rippleMax = 5f
                            val rippleCoerced = chebyRippleDb.toFloat().coerceIn(rippleMin, rippleMax)
                            Slider(
                                value = rippleCoerced,
                                onValueChange = { viewModel.setChebyRippleDb(it.toDouble()) },
                                valueRange = rippleMin..rippleMax,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ripple_slider")
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Mini-Preview of Impulse Response & Save Action
        if (result != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Active Design Specs",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    result.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }

                            Button(
                                onClick = { showSaveDialog = true },
                                modifier = Modifier.testTag("save_design_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Divider()
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        // Render lightweight impulse preview
                        Text(
                            "Impulse Response Preview",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        ImpulseResponsePlot(
                            result = result,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        }
    }

    // Save configuration Dialog
    if (showSaveDialog) {
        var labelText by remember { mutableStateOf("") }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Filter Design") },
            text = {
                Column {
                    Text("Enter a custom friendly label to identify this design later:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        placeholder = { Text("e.g., Audio LP 4kHz") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_label_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.saveCurrentFilter(labelText)
                        showSaveDialog = false
                        Toast.makeText(context, "Design Saved Successfully!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("dialog_save_confirm")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AnalysisTab(
    result: FilterResult?,
    errorMsg: String?
) {
        if (result == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                    contentDescription = "No design",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No design calculated. Verify topology specs inside the Design panel first.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    var selectedPlotTab by remember { mutableStateOf(0) } // 0: Mag, 1: Phase, 2: Impulse

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Sampling Frequency: ${result.sampleRate.roundToInt()} Hz | Cutoff: ${result.fCutoff1.roundToInt()} Hz " + 
                               if (result.type == FilterType.BANDPASS || result.type == FilterType.BANDSTOP) "| Cutoff 2: ${result.fCutoff2.roundToInt()} Hz" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Plot Select Tab Row
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedPlotTab,
                edgePadding = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = selectedPlotTab == 0,
                    onClick = { selectedPlotTab = 0 },
                    text = { Text("Magnitude (dB)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("plot_tab_mag")
                )
                Tab(
                    selected = selectedPlotTab == 1,
                    onClick = { selectedPlotTab = 1 },
                    text = { Text("Phase (Degrees)", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("plot_tab_phase")
                )
                Tab(
                    selected = selectedPlotTab == 2,
                    onClick = { selectedPlotTab = 2 },
                    text = { Text("Impulse Response", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("plot_tab_impulse")
                )
            }
        }

        // Responsive graph card viewport
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("graphed_viewport"),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AnimatedContent(
                        targetState = selectedPlotTab,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "PlotTransition"
                    ) { index ->
                        when (index) {
                            0 -> Column {
                                Text(
                                    "Magnitude frequency response (H(e^jω))",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    "Shows filter gain in decibels. Inspect passband ripples and stopband attenuations by dragging along the curve.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                MagnitudeResponsePlot(result = result)
                            }
                            1 -> Column {
                                Text(
                                    "Phase frequency response (∠H(e^jω))",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    "Shows Phase shift vs frequency in degrees. Linear phase is maintained for FIR designs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                PhaseResponsePlot(result = result)
                            }
                            2 -> Column {
                                Text(
                                    "Impulse response (h[n])",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    "Discrete-time representation of filter coefficient weights. Shows causal decay characteristics of the design.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                ImpulseResponsePlot(result = result)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoefficientsTab(
    result: FilterResult?
) {
    if (result == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Specify a digital filter in the design tab to view results.")
        }
        return
    }

    var showExporter by remember { mutableStateOf(false) }

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
            Column {
                Text(
                    "Computed Coefficients",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (result.isFir) "${result.firCoefficients.size} Tap direct FIR coefficients" else "${result.iirBiquads.size} Cascaded Second-Order Sections (Biquads)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Button(
                onClick = { showExporter = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("export_biquads_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export Code")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid/List of coefficients
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            if (result.isFir) {
                // FIR list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(result.firCoefficients.size) { index ->
                        val hVal = result.firCoefficients[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (index % 2 == 0) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "h[$index]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "%.10f".format(hVal),
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                // IIR Biquad list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(result.iirBiquads.size) { index ->
                        val biquad = result.iirBiquads[index]
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Biquad Section ${index + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(6.dp))

                                val rowStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Numerator (b)", fontWeight = FontWeight.Bold, style = rowStyle)
                                        Text("b0: %.10f".format(biquad.b0), style = rowStyle)
                                        Text("b1: %.10f".format(biquad.b1), style = rowStyle)
                                        Text("b2: %.10f".format(biquad.b2), style = rowStyle)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Denominator (a)", fontWeight = FontWeight.Bold, style = rowStyle)
                                        Text("a0: 1.0000000000", style = rowStyle)
                                        Text("a1: %.10f".format(biquad.a1), style = rowStyle)
                                        Text("a2: %.10f".format(biquad.a2), style = rowStyle)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExporter) {
        CodeExporterDialog(result = result, onDismiss = { showExporter = false })
    }
}

@Composable
fun SavedTab(
    savedFilters: List<FilterEntity>,
    onLoad: (FilterEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (savedFilters.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = "No Saved Designs",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No saved designs found. Build a digital filter inside the Design tab and click Save!",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(savedFilters) { filter ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLoad(filter) }
                    .testTag("saved_filter_${filter.id}"),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDE2F9)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1
                            )
                            val friendlyMethod = when (filter.method) {
                                "FIR_WINDOW" -> "FIR (${filter.windowType})"
                                "IIR_BUTTERWORTH" -> "IIR Butterworth"
                                "IIR_CHEBYSHEV" -> "IIR Chebyshev"
                                else -> filter.method
                            }
                            Text(
                                text = "$friendlyMethod - ${filter.type}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        IconButton(
                            onClick = { onDelete(filter.id) },
                            modifier = Modifier.testTag("delete_saved_${filter.id}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Fs Rate", style = MaterialTheme.typography.labelSmall)
                            Text("${filter.sampleRate.roundToInt()} Hz", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Fc Cutoff", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = if (filter.type == "BANDPASS" || filter.type == "BANDSTOP") 
                                    "${filter.fc1.roundToInt()}-${filter.fc2.roundToInt()} Hz"
                                    else "${filter.fc1.roundToInt()} Hz",
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text("Order/Taps", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = if (filter.method == "FIR_WINDOW") "${filter.firTaps} taps" else "Order ${filter.iirOrder}",
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onLoad(filter) }) {
                            Text("Load Design")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==================== CODE EXPORTER DIAOLOG ====================

@Composable
fun CodeExporterDialog(
    result: FilterResult,
    onDismiss: () -> Unit
) {
    var formatIndex by remember { mutableStateOf(0) } // 0: JSON, 1: C/C++, 2: Kotlin, 3: Matlab, 4: Python, 5: CSV
    val formats = listOf("JSON", "C Array", "Kotlin", "Matlab", "Python", "CSV")

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val exportText = remember(result, formatIndex) {
        val titleLine = "// Optimized ${result.title} Coefficients\n// Sampling Frequency: ${result.sampleRate} Hz\n"
        when (formatIndex) {
            0 -> { // JSON
                if (result.isFir) {
                    "{\n  \"sampling_rate\": ${result.sampleRate},\n  \"type\": \"${result.type.name}\",\n  \"coefficients\": [\n" + 
                            result.firCoefficients.joinToString(", \n") { "    $it" } + "\n  ]\n}"
                } else {
                    "{\n  \"sampling_rate\": ${result.sampleRate},\n  \"type\": \"${result.type.name}\",\n  \"biquads\": [\n" + 
                            result.iirBiquads.joinToString(",\n") { "    { \"b0\": ${it.b0}, \"b1\": ${it.b1}, \"b2\": ${it.b2}, \"a1\": ${it.a1}, \"a2\": ${it.a2} }" } + "\n  ]\n}"
                }
            }
            1 -> { // C Array
                if (result.isFir) {
                    "${titleLine}const double FILTER_TAPS[${result.firCoefficients.size}] = {\n" + 
                            result.firCoefficients.joinToString(", \n") { "    $it" } + "\n};"
                } else {
                    "${titleLine}typedef struct {\n    double b0, b1, b2;\n    double a1, a2;\n} Biquad;\n\n" + 
                            "const Biquad BIQUADS[${result.iirBiquads.size}] = {\n" + 
                            result.iirBiquads.joinToString(",\n") { "    { ${it.b0}, ${it.b1}, ${it.b2}, ${it.a1}, ${it.a2} }" } + "\n};"
                }
            }
            2 -> { // Kotlin
                if (result.isFir) {
                    "${titleLine}val FILTER_COEFFICIENTS = doubleArrayOf(\n" + 
                            result.firCoefficients.joinToString(", \n") { "    $it" } + "\n)"
                } else {
                    "${titleLine}data class Biquad(val b0: Double, val b1: Double, val b2: Double, val a1: Double, val a2: Double)\n\n" + 
                            "val BIQUADS = listOf(\n" + 
                            result.iirBiquads.joinToString(",\n") { "    Biquad(${it.b0}, ${it.b1}, ${it.b2}, ${it.a1}, ${it.a2})" } + "\n)"
                }
            }
            3 -> { // Matlab
                if (result.isFir) {
                    "%% Matlab FIR\nh = [\n" + result.firCoefficients.joinToString("; \n") { "  $it" } + "\n];\nfreqz(h, 1, 1024, ${result.sampleRate});"
                } else {
                    "%% Matlab IIR SOS (b0, b1, b2, 1, a1, a2)\nsos = [\n" + 
                            result.iirBiquads.joinToString(";\n") { "  ${it.b0}, ${it.b1}, ${it.b2}, 1, ${it.a1}, ${it.a2}" } + "\n];\ng = 1.0;\nfreqz(sos, 1024, ${result.sampleRate});"
                }
            }
            4 -> { // Python
                if (result.isFir) {
                    "# Python List\ncoefs = [\n" + result.firCoefficients.joinToString(", \n") { "    $it" } + "\n]"
                } else {
                    "# Python Biquads (num, den)\nsos = [\n" + 
                            result.iirBiquads.joinToString(",\n") { "  [${it.b0}, ${it.b1}, ${it.b2}, 1.0, ${it.a1}, ${it.a2}]" } + "\n]"
                }
            }
            else -> { // CSV
                if (result.isFir) {
                    "index,coefficient\n" + result.firCoefficients.indices.joinToString("\n") { "$it,${result.firCoefficients[it]}" }
                } else {
                    "section,b0,b1,b2,a0,a1,a2\n" + result.iirBiquads.indices.joinToString("\n") { i -> 
                        val b = result.iirBiquads[i]
                        "$i,${b.b0},${b.b1},${b.b2},1.0,${b.a1},${b.a2}"
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "Export Source Code",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Navigation format row
                ScrollableTabRow(
                    selectedTabIndex = formatIndex,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                ) {
                    formats.forEachIndexed { idx, name ->
                        Tab(
                            selected = formatIndex == idx,
                            onClick = { formatIndex = idx },
                            text = { Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable code container
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = exportText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(exportText))
                            Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("dialog_copy_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Code")
                    }
                }
            }
        }
    }
}
