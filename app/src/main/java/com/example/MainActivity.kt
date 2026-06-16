package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CalculationHistory
import com.example.domain.BreakPeriod
import com.example.domain.CalculationResult
import com.example.domain.ProcessCalculator
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CalculatorViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CalculatorAppScreen(viewModel = viewModel)
            }
        }
    }
}

// Design Tokens (Toss-inspired Palette for Premium Look)
val BrandBlue = Color(0xFF3182F6)
val BrandBlueDark = Color(0xFF1B64DA)
val BrandBlueLight = Color(0xFFEBF3FE)
val BrandGreen = Color(0xFF1DBC8B)
val BrandGreenLight = Color(0xFFE8FAF4)
val BrandOrange = Color(0xFFFF6B35)
val BrandOrangeLight = Color(0xFFFFF0EB)
val BrandRed = Color(0xFFF04452)
val BrandRedLight = Color(0xFFFEF0F1)
val BrandYellow = Color(0xFFF5A623)
val BrandYellowLight = Color(0xFFFFF8EC)

val Gray50 = Color(0xFFF9FAFB)
val Gray100 = Color(0xFFF2F4F6)
val Gray200 = Color(0xFFE5E8EB)
val Gray300 = Color(0xFFD1D6DB)
val Gray400 = Color(0xFFB0B8C1)
val Gray500 = Color(0xFF8B95A1)
val Gray600 = Color(0xFF6B7684)
val Gray700 = Color(0xFF4E5968)
val Gray800 = Color(0xFF333D4B)
val Gray900 = Color(0xFF191F28)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorAppScreen(viewModel: CalculatorViewModel) {
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val hour by viewModel.inputHour.collectAsStateWithLifecycle()
    val minute by viewModel.inputMinute.collectAsStateWithLifecycle()
    val duration by viewModel.inputDuration.collectAsStateWithLifecycle()
    val result by viewModel.calculationResult.collectAsStateWithLifecycle()
    val history by viewModel.historyList.collectAsStateWithLifecycle()
    
    val historyOpen by viewModel.historyDrawerOpen.collectAsStateWithLifecycle()
    val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
    val dialogTitle by viewModel.dialogTitle.collectAsStateWithLifecycle()
    val dialogDesc by viewModel.dialogDesc.collectAsStateWithLifecycle()
    val showCancel by viewModel.showCancelButton.collectAsStateWithLifecycle()

    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Gray100,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.toggleHistoryDrawer(true) },
                containerColor = Gray900,
                contentColor = Color.White,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier
                    .padding(end = 4.dp, bottom = 4.dp)
                    .testTag("history_floating_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.List,
                    contentDescription = "이력함 보기",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "최근 이력함",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .clickable { focusManager.clearFocus() } // Tap outside fields to dismiss keyboard
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            AppHeader()

            // 1. Calculation Mode Card
            ModeSelectionCard(
                currentMode = mode,
                onModeChange = { viewModel.setMode(it) }
            )

            // 2. Main Inputs Card
            InputsCard(
                mode = mode,
                hour = hour,
                minute = minute,
                duration = duration,
                onHourChange = { viewModel.updateHour(it) },
                onMinChange = { viewModel.updateMinute(it) },
                onDurationChange = { viewModel.updateDuration(it) },
                onPresetClick = { viewModel.setDurationValue(it) },
                onCalculate = {
                    focusManager.clearFocus()
                    viewModel.calculate()
                }
            )

            // 3. Dynamic Calculation Results
            AnimatedVisibility(
                visible = result != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                result?.let { res ->
                    ResultCard(
                        result = res,
                        mode = mode,
                        inputHour = hour,
                        inputMin = minute,
                        inputDuration = duration,
                        onReset = { viewModel.reset() }
                    )
                }
            }

            // 4. Constant Work/Break Schedules Guides
            WorkScheduleGuideCard()
            
            // Bottom Safe padding
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Modal Calculation History Drawer (Model Sheet)
    if (historyOpen) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.toggleHistoryDrawer(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .width(40.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Gray300)
                )
            }
        ) {
            HistorySheetContent(
                history = history,
                onSelect = { viewModel.loadHistoryItem(it) },
                onDelete = { viewModel.deleteHistoryItem(it) },
                onClearAll = { viewModel.clearAllHistory() }
            )
        }
    }

    // Unified custom-dialog substitution for alerts/confirms
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog(false) },
            containerColor = Color.White,
            title = {
                Text(
                    text = dialogTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Gray900
                )
            },
            text = {
                Text(
                    text = dialogDesc,
                    fontSize = 14.sp,
                    color = Gray700,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissDialog(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("확인", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (showCancel) {
                    TextButton(
                        onClick = { viewModel.dismissDialog(false) },
                        colors = ButtonDefaults.textButtonColors(contentColor = Gray600)
                    ) {
                        Text("취소", fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

@Composable
fun AppHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .background(BrandBlueLight, RoundedCornerShape(999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "PROCESS ARRIVAL CALCULATOR",
                color = BrandBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "공정 도착시간 계산기",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Gray900,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "쉬는 시간 자동 반영 · 정확한 작업 도착 시각",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Gray600,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ModeSelectionCard(
    currentMode: String,
    onModeChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "계산 방향",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Gray600,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mode 1: Forward
                val isFwd = currentMode == "forward"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isFwd) BrandBlueLight else Color.White)
                        .border(
                            width = 2.dp,
                            color = if (isFwd) BrandBlue else Gray200,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onModeChange("forward") }
                        .padding(vertical = 14.dp, horizontal = 6.dp)
                        .testTag("forward_mode_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "▶",
                            fontSize = 22.sp,
                            color = if (isFwd) BrandBlue else Gray400
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "시작 → 도착",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFwd) BrandBlue else Gray700
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "시작 시간 입력",
                            fontSize = 12.sp,
                            color = if (isFwd) BrandBlue.copy(alpha = 0.7f) else Gray500
                        )
                    }
                }

                // Mode 2: Backward
                val isBwd = currentMode == "backward"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isBwd) BrandGreenLight else Color.White)
                        .border(
                            width = 2.dp,
                            color = if (isBwd) BrandGreen else Gray200,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onModeChange("backward") }
                        .padding(vertical = 14.dp, horizontal = 6.dp)
                        .testTag("backward_mode_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "◀",
                            fontSize = 22.sp,
                            color = if (isBwd) BrandGreen else Gray400
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "도착 → 시작 역산",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBwd) BrandGreen else Gray700
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "목표 도착 시간 입력",
                            fontSize = 12.sp,
                            color = if (isBwd) BrandGreen.copy(alpha = 0.7f) else Gray500
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InputsCard(
    mode: String,
    hour: String,
    minute: String,
    duration: String,
    onHourChange: (String) -> Unit,
    onMinChange: (String) -> Unit,
    onDurationChange: (String) -> Unit,
    onPresetClick: (Int) -> Unit,
    onCalculate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "기본 정보 입력",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Gray600,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 1. Time Input Group
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (mode == "forward") "🕐 시작 시간" else "🏁 목표 도착 시간",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gray750
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(BrandBlueLight, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "HH:MM",
                            color = BrandBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Beautiful Time Entry Picker Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray50, RoundedCornerShape(12.dp))
                        .border(1.5.dp, Gray200, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour Box
                    TimeChunkField(
                        value = hour,
                        placeholder = "09",
                        onValueChange = onHourChange,
                        modifier = Modifier.testTag("hour_input")
                    )

                    Text(
                        text = ":",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gray400,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    // Minute Box
                    TimeChunkField(
                        value = minute,
                        placeholder = "00",
                        onValueChange = onMinChange,
                        modifier = Modifier.testTag("minute_input")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Pure Work Duration In Minutes Group
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "⏱ 소요 시간",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray750,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray50, RoundedCornerShape(12.dp))
                        .border(1.5.dp, Gray200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = duration,
                        onValueChange = onDurationChange,
                        textStyle = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Gray900,
                            textAlign = TextAlign.End
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("duration_input"),
                        decorationBox = @Composable { innerTextField ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (duration.isEmpty()) {
                                    Text(
                                        text = "180",
                                        fontSize = 30.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Gray300,
                                        textAlign = TextAlign.End
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                    Text(
                        text = "분",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Gray500,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Prestiled easy chips row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(label = "30분", onClick = { onPresetClick(30) }, modifier = Modifier.weight(1f))
                        PresetChip(label = "1시간", onClick = { onPresetClick(60) }, modifier = Modifier.weight(1f))
                        PresetChip(label = "1.5h", onClick = { onPresetClick(90) }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PresetChip(label = "2시간", onClick = { onPresetClick(120) }, modifier = Modifier.weight(1f))
                        PresetChip(label = "3시간", onClick = { onPresetClick(180) }, modifier = Modifier.weight(1f))
                        PresetChip(label = "4시간", onClick = { onPresetClick(240) }, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Calculate Button
            Button(
                onClick = onCalculate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("calc_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandBlue, BrandBlueDark)
                            )
                        )
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🚀 ",
                            fontSize = 18.sp,
                        )
                        Text(
                            text = "분석 계산하기",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeChunkField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = TextStyle(
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Gray900,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next
        ),
        modifier = modifier.width(70.dp),
        decorationBox = @Composable { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Gray300,
                        textAlign = TextAlign.Center
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun PresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Gray100)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Gray700
        )
    }
}

@Composable
fun ResultCard(
    result: CalculationResult,
    mode: String,
    inputHour: String,
    inputMin: String,
    inputDuration: String,
    onReset: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Result Header with gradient
            val endColorHex = if (mode == "forward") BrandBlueDark else Color(0xFF0FA36E)
            val startColorHex = if (mode == "forward") BrandBlue else BrandGreen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(startColorHex, endColorHex)
                        )
                    )
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (mode == "forward") "예상 도착 시간" else "필요 시작 시간",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.resultTimeStr,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1.5).sp,
                        lineHeight = 54.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val hrPad = inputHour.padStart(2, '0')
                    val minPad = inputMin.padStart(2, '0')
                    Text(
                        text = if (mode == "forward") {
                            "$hrPad:$minPad 시작 · ${inputDuration}분 작업"
                        } else {
                            "$hrPad:$minPad 도착 목표 · ${inputDuration}분 작업"
                        },
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Calculations Stats and Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 3 Column Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBox(
                        value = "${result.pureWorkTime}",
                        label = "순수 가동",
                        color = if (mode == "forward") BrandBlue else BrandGreen,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "${result.totalBreakTime}",
                        label = "쉬는 시간",
                        color = BrandOrange,
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        value = "${result.totalElapsed}",
                        label = "총 경과",
                        color = Gray700,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Gray200, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Crossing Break schedules spanned
                Text(
                    text = "⛔ 경유 비가동 시간",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Gray500,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (result.passedBreaks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandGreenLight, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✅ 쉬는 시간 없이 연속 작업 가능!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreen,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        result.passedBreaks.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandOrangeLight, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⛔ ${b.label}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandOrange
                                )
                                Box(
                                    modifier = Modifier
                                        .background(BrandOrange, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+${b.duration}분",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Crossing Error/Warn banners
                if (result.warnings.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    result.warnings.forEach { w ->
                        val isErr = w.type == "error"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isErr) BrandRedLight else BrandYellowLight,
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isErr) BrandRed else BrandYellow,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = w.msg,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isErr) BrandRed else Gray800
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Gray200, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Restarter button
                TextButton(
                    onClick = onReset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_button")
                ) {
                    Text(
                        text = "↩ 다시 계산하기",
                        color = Gray600,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StatBox(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Gray50, RoundedCornerShape(12.dp))
            .border(1.dp, Gray200, RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = "분",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray400,
                    modifier = Modifier.padding(start = 1.dp, bottom = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Gray500
            )
        }
    }
}

@Composable
fun WorkScheduleGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "당일 고정 비가동 스케줄",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Gray600,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Grid Layout for breaks
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val chunks = ProcessCalculator.BREAKS.chunked(2)
                chunks.forEach { pairList ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pairList.forEach { b ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Gray50, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = b.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gray700
                                )
                                Text(
                                    text = "${b.duration}분",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gray500
                                )
                            }
                        }
                        if (pairList.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray100, RoundedCornerShape(8.dp))
                    .padding(vertical = 10.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "당일 운영: 06:45 ~ 24:20  |  총 비가동: 125분",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray700,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun HistorySheetContent(
    history: List<CalculationHistory>,
    onSelect: (CalculationHistory) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "📋 최근 계산 이력",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Gray900
            )
            if (history.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandRed),
                    modifier = Modifier.testTag("history_clear_btn")
                ) {
                    Text("전체 삭제", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "아직 계산 이력이 없습니다.\n계산 결과가 자동으로 저장됩니다.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Gray400,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(history, key = { it.id }) { item ->
                    HistoryItemView(
                        item = item,
                        onClick = { onSelect(item) },
                        onDelete = { onDelete(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemView(
    item: CalculationHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Gray50)
            .border(1.dp, Gray200, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isFwd = item.mode == "forward"
                Box(
                    modifier = Modifier
                        .background(
                            if (isFwd) BrandBlueLight else BrandGreenLight,
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isFwd) "시작→도착" else "도착→시작",
                        color = if (isFwd) BrandBlue else BrandGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            val hPad = item.baseHour.toString().padStart(2, '0')
            val mPad = item.baseMin.toString().padStart(2, '0')
            Text(
                text = "$hPad:$mPad → ${item.resultTimeStr}",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Gray900
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "순수 소요 ${item.duration}분  |  쉬는 시간 ${item.totalBreakTime}분 포함",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Gray500
            )
        }
        
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "삭제",
                tint = Gray400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Custom Gray750 definition to have perfect contrast
val Gray750 = Color(0xFF404A56)
