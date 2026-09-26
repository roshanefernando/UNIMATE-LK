package com.unimatelk.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen


private val BackgroundColor = Color(0xFF071525)
private val HeaderColor = Color(0xFF0B1F35)
private val CardColor = Color(0xFF102B4C)
private val PrimaryBlue = Color(0xFF1976D2)
private val HighlightBlue = Color(0xFF42A5F5)
private val TextSecondary = Color(0xFFB8D8F5)
private val BorderBlue = Color(0xFF5B7FA3)
private val SuccessGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFE57373)
private val WarningOrange = Color(0xFFFFB74D)

data class Module(
    val id: Long,
    val name: String,
    val credits: Double,
    val grade: String
)

data class ManagedModule(
    val id: Long,
    val code: String,
    val name: String,
    val credits: Double,
    val lecturer: String,
    val semester: String
)

data class Assignment(
    val id: Long,
    val title: String,
    val module: String,
    val dueDate: String,
    val completed: Boolean
)

data class Expense(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val date: String
)

data class TimetableClass(
    val id: Long,
    val day: String,
    val subject: String,
    val time: String,
    val room: String,
    val lecturer: String,
    val moduleCode: String = ""
)

private const val PREFS_NAME = "unimate_data"
private const val KEY_MODULES = "modules"
private const val KEY_MANAGED_MODULES = "managed_modules"
private const val KEY_ASSIGNMENTS = "assignments"
private const val KEY_EXPENSES = "expenses"
private const val KEY_TIMETABLE = "timetable"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (
                checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.POST_NOTIFICATIONS
                    ),
                    100
                )
            }
        }

        // Create notification channel
        ReminderManager.createNotificationChannel(this)

        setContent {
            MaterialTheme {
                UniMateApp()
            }
        }
    }
}

@Composable
fun UniMateApp() {

    val context = androidx.compose.ui.platform.LocalContext.current

    // =========================================================
    // DATA
    // =========================================================

    var modules by remember {
        mutableStateOf(loadModules(context))
    }

    var managedModules by remember {
        mutableStateOf(loadManagedModules(context))
    }

    var assignments by remember {
        mutableStateOf(loadAssignments(context))
    }

    var expenses by remember {
        mutableStateOf(loadExpenses(context))
    }

    var timetable by remember {
        mutableStateOf(loadTimetable(context))
    }

    // =========================================================
    // DERIVED DATA
    // =========================================================

    val gpa = remember(modules) {
        calculateGPA(modules)
    }

    val pendingAssignmentCount = remember(assignments) {
        assignments.count {
            !it.completed
        }
    }

    val todayClassCount = remember(timetable) {
        timetable.count {
            it.day == getTodayName()
        }
    }

    val moduleCount = remember(managedModules) {
        managedModules.size
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    var currentScreen by remember {
        mutableStateOf("home")
    }

    val navigationStack = remember {
        mutableStateListOf<String>()
    }

    fun navigateTo(screen: String) {

        if (currentScreen == screen) {
            return
        }

        navigationStack.add(currentScreen)

        currentScreen = screen
    }

    fun navigateBack() {

        if (navigationStack.isNotEmpty()) {

            currentScreen =
                navigationStack.removeAt(
                    navigationStack.lastIndex
                )

        } else {

            currentScreen = "home"
        }
    }

    // =========================================================
    // ANDROID BACK BUTTON
    // =========================================================

    BackHandler(
        enabled = currentScreen != "home"
    ) {
        navigateBack()
    }

    // =========================================================
    // SAVE DATA
    // =========================================================

    LaunchedEffect(modules) {

        saveModules(
            context,
            modules
        )
    }

    LaunchedEffect(managedModules) {

        saveManagedModules(
            context,
            managedModules
        )
    }

    LaunchedEffect(assignments) {

        saveAssignments(
            context,
            assignments
        )
    }

    LaunchedEffect(expenses) {

        saveExpenses(
            context,
            expenses
        )
    }

    LaunchedEffect(timetable) {

        saveTimetable(
            context,
            timetable
        )
    }

    // =========================================================
    // SCHEDULE EXISTING ASSIGNMENT REMINDERS
    // =========================================================

    LaunchedEffect(Unit) {

        assignments.forEach { assignment ->

            scheduleAssignmentReminder(
                context = context,
                assignment = assignment
            )
        }
    }

    // =========================================================
    // SCREEN DISPLAY
    // =========================================================

    when (currentScreen) {

        // =====================================================
        // HOME
        // =====================================================

        "home" -> HomeScreen(

            gpa = gpa,

            assignmentCount =
                pendingAssignmentCount,

            classCount =
                todayClassCount,

            moduleCount =
                moduleCount,

            onGpa = {
                navigateTo("gpa")
            },

            onModules = {
                navigateTo("modules")
            },

            onAssignments = {
                navigateTo("assignments")
            },

            onExpense = {
                navigateTo("expense")
            },

            onTimetable = {
                navigateTo("timetable")
            },

            onStatistics = {
                navigateTo("statistics")
            }
        )

        // =====================================================
        // GPA
        // =====================================================

        "gpa" -> GpaScreen(

            modules = modules,

            managedModules = managedModules,

            onAdd = { module ->

                modules =
                    modules + module
            },

            onDelete = { id ->

                modules =
                    modules.filterNot {
                        it.id == id
                    }
            },

            onClear = {

                modules = emptyList()
            },

            onBack = {

                navigateBack()
            }
        )

        // =====================================================
        // MODULE MANAGER
        // =====================================================

        "modules" -> ModuleManagerScreen(

            modules = managedModules,

            onAdd = { module ->

                managedModules =
                    managedModules + module
            },

            onDelete = { id ->

                managedModules =
                    managedModules.filterNot {
                        it.id == id
                    }
            },

            onBack = {

                navigateBack()
            }
        )

        // =====================================================
        // ASSIGNMENTS
        // =====================================================

        "assignments" -> AssignmentScreen(

            assignments = assignments,

            managedModules = managedModules,

            onAdd = { assignment ->

                assignments =
                    assignments + assignment

                scheduleAssignmentReminder(
                    context = context,
                    assignment = assignment
                )
            },

            onDelete = { id ->

                ReminderScheduler.cancelReminder(
                    context = context,
                    notificationId = id.hashCode()
                )

                assignments =
                    assignments.filterNot {
                        it.id == id
                    }
            },

            onToggle = { id ->

                val assignment =
                    assignments.find {
                        it.id == id
                    }

                if (assignment != null) {

                    val updatedAssignment =
                        assignment.copy(
                            completed =
                                !assignment.completed
                        )

                    assignments =
                        assignments.map {

                            if (it.id == id) {
                                updatedAssignment
                            } else {
                                it
                            }
                        }

                    if (updatedAssignment.completed) {

                        ReminderScheduler.cancelReminder(
                            context = context,
                            notificationId = id.hashCode()
                        )

                    } else {

                        scheduleAssignmentReminder(
                            context = context,
                            assignment =
                                updatedAssignment
                        )
                    }
                }
            },

            onBack = {

                navigateBack()
            }
        )

        // =====================================================
        // TIMETABLE
        // =====================================================

        "timetable" -> TimetableScreen(

            timetable = timetable,

            managedModules = managedModules,

            onAdd = { item ->

                timetable =
                    timetable + item
            },

            onDelete = { id ->

                timetable =
                    timetable.filterNot {
                        it.id == id
                    }
            },

            onBack = {

                navigateBack()
            }
        )

        // =====================================================
        // EXPENSE
        // =====================================================

        "expense" -> ExpenseScreen(

            expenses = expenses,

            onAdd = { expense ->

                expenses =
                    expenses + expense
            },

            onDelete = { id ->

                expenses =
                    expenses.filterNot {
                        it.id == id
                    }
            },

            onBack = {

                navigateBack()
            }
        )

        // =====================================================
        // STATISTICS
        // =====================================================

        "statistics" -> StatisticsScreen(

            modules = modules,

            managedModules = managedModules,

            assignments = assignments,

            expenses = expenses,

            timetable = timetable,

            onBack = {

                navigateBack()
            }
        )
    }
}
@Composable
fun HomeScreen(
    gpa: Double,
    assignmentCount: Int,
    classCount: Int,
    moduleCount: Int,
    onGpa: () -> Unit,
    onModules: () -> Unit,
    onAssignments: () -> Unit,
    onExpense: () -> Unit,
    onTimetable: () -> Unit,
    onStatistics: () -> Unit
) {

    val today = remember {
        getTodayName()
    }

    val greeting = remember {
        getGreeting()
    }

    val formattedGpa = remember(gpa) {
        if (gpa == 0.0) {
            "-"
        } else {
            String.format(
                "%.2f",
                gpa
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        // =========================================================
        // BACKGROUND IMAGE
        // =========================================================

        Image(
            painter = painterResource(
                id = R.drawable.gpa_background
            ),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // =========================================================
        // DARK OVERLAY
        // =========================================================

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Color.Black.copy(
                        alpha = 0.60f
                    )
                )
        )

        // =========================================================
        // HOME CONTENT
        // =========================================================

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 125.dp,
                bottom = 35.dp
            )
        ) {

            // =====================================================
            // HEADER
            // =====================================================

            item {

                Text(
                    text = greeting,
                    color = Color.White,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Welcome back to UniMate",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(
                        top = 5.dp
                    )
                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )
            }

            // =====================================================
            // OVERVIEW
            // =====================================================

            item {

                Text(
                    text = "Overview",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    OverviewItem(
                        title = "GPA",
                        value = formattedGpa,
                        modifier = Modifier.weight(1f)
                    )

                    OverviewItem(
                        title = "Tasks",
                        value = assignmentCount.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    OverviewItem(
                        title = "Classes",
                        value = classCount.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    OverviewItem(
                        title = "Modules",
                        value = moduleCount.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }

            // =====================================================
            // QUICK ACTIONS
            // =====================================================

            item {

                Text(
                    text = "Quick Actions",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    ActionCard(
                        title = "GPA",
                        onClick = onGpa,
                        modifier = Modifier.weight(1f)
                    )

                    ActionCard(
                        title = "Modules",
                        onClick = onModules,
                        modifier = Modifier.weight(1f)
                    )

                    ActionCard(
                        title = "Tasks",
                        onClick = onAssignments,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    ActionCard(
                        title = "Schedule",
                        onClick = onTimetable,
                        modifier = Modifier.weight(1f)
                    )

                    ActionCard(
                        title = "Finance",
                        onClick = onExpense,
                        modifier = Modifier.weight(1f)
                    )

                    ActionCard(
                        title = "Stats",
                        onClick = onStatistics,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }

            // =====================================================
            // TODAY
            // =====================================================

            item {

                Text(
                    text = "Today",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            CardColor.copy(
                                alpha = 0.94f
                            )
                    ),
                    shape = RoundedCornerShape(
                        20.dp
                    ),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 6.dp
                        )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = today,
                                color = HighlightBlue,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    if (classCount == 0) {
                                        "No classes scheduled today"
                                    } else {
                                        "$classCount class(es) scheduled today"
                                    },
                                color = Color.White,
                                fontSize = 15.sp
                            )
                        }

                        Text(
                            text = "›",
                            color = HighlightBlue,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )
            }

            // =====================================================
            // STATISTICS PROMO
            // =====================================================

            item {

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onStatistics()
                        },
                    colors = CardDefaults.cardColors(
                        containerColor =
                            PrimaryBlue.copy(
                                alpha = 0.92f
                            )
                    ),
                    shape = RoundedCornerShape(
                        20.dp
                    ),
                    elevation =
                        CardDefaults.cardElevation(
                            defaultElevation = 7.dp
                        )
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text =
                                    "View Your Statistics",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Track your academic and personal progress",
                                color =
                                    Color.White.copy(
                                        alpha = 0.82f
                                    ),
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "›",
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OverviewItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =
                CardColor.copy(
                    alpha = 0.90f
                )
        ),
        shape = RoundedCornerShape(
            16.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                color = HighlightBlue,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier
            .height(65.dp)
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor =
                CardColor.copy(
                    alpha = 0.92f
                )
        ),
        shape = RoundedCornerShape(
            16.dp
        )
    ) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.SemiBold,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

@Composable
fun StatisticsScreen(
    modules: List<Module>,
    managedModules: List<ManagedModule>,
    assignments: List<Assignment>,
    expenses: List<Expense>,
    timetable: List<TimetableClass>,
    onBack: () -> Unit
) {

    // -----------------------------
    // CALCULATIONS
    // -----------------------------

    val totalAssignments = remember(assignments) {
        assignments.size
    }

    val completedAssignments = remember(assignments) {
        assignments.count { it.completed }
    }

    val pendingAssignments = remember(assignments) {
        assignments.count { !it.completed }
    }

    val assignmentPercentage = remember(
        totalAssignments,
        completedAssignments
    ) {
        if (totalAssignments > 0) {
            (
                    completedAssignments.toDouble() /
                            totalAssignments.toDouble()
                    ) * 100.0
        } else {
            0.0
        }
    }

    val totalExpenses = remember(expenses) {
        expenses.sumOf { it.amount }
    }

    val totalCredits = remember(modules) {
        modules.sumOf { it.credits }
    }

    val todayClasses = remember(timetable) {
        timetable.count {
            it.day == getTodayName()
        }
    }

    val highestModule = remember(modules) {
        modules.maxByOrNull {
            gradePoint(it.grade)
        }
    }

    val lowestModule = remember(modules) {
        modules.minByOrNull {
            gradePoint(it.grade)
        }
    }

    val categoryTotals = remember(expenses) {
        expenses
            .groupBy {
                if (it.category.isBlank()) {
                    "Other"
                } else {
                    it.category
                }
            }
            .mapValues { entry ->
                entry.value.sumOf { it.amount }
            }
            .toList()
            .sortedByDescending {
                it.second
            }
    }

    // Pre-calculate category percentages
    val categoryStatistics = remember(
        categoryTotals,
        totalExpenses
    ) {
        categoryTotals.map { item ->

            val category = item.first
            val amount = item.second

            val percentage =
                if (totalExpenses > 0) {
                    amount / totalExpenses
                } else {
                    0.0
                }

            Triple(
                category,
                amount,
                percentage
            )
        }
    }

    val currentGpa = remember(modules) {
        if (modules.isEmpty()) {
            0.0
        } else {
            calculateGPA(modules)
        }
    }

    // -----------------------------
    // SCREEN
    // -----------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        ScreenHeader(
            title = "Statistics",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // =========================================================
            // OVERVIEW
            // =========================================================

            item {

                Text(
                    text = "Your Overview",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "A quick look at your academic and university activity.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            // =========================================================
            // GPA CARD
            // =========================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Current GPA",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                if (modules.isEmpty()) {
                                    "-"
                                } else {
                                    String.format(
                                        "%.2f",
                                        currentGpa
                                    )
                                },
                            color = HighlightBlue,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                if (modules.isEmpty()) {
                                    "Add GPA modules to calculate your GPA"
                                } else {
                                    "Based on ${modules.size} GPA module${
                                        if (modules.size == 1) "" else "s"
                                    }"
                                },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {

                            SmallStatistic(
                                title = "Modules",
                                value = modules.size.toString(),
                                modifier = Modifier.weight(1f)
                            )

                            SmallStatistic(
                                title = "Credits",
                                value = formatNumber(totalCredits),
                                modifier = Modifier.weight(1f)
                            )

                            SmallStatistic(
                                title = "Saved",
                                value = managedModules.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // =========================================================
            // ACADEMIC INSIGHTS
            // =========================================================

            if (modules.isNotEmpty()) {

                item {

                    Spacer(
                        modifier = Modifier.height(22.dp)
                    )

                    StatisticsSectionTitle(
                        "Academic Insights"
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    if (highestModule != null) {

                        StatisticsInfoCard(
                            title = "Best Performing Module",
                            mainText = highestModule.name,
                            subText =
                                "${highestModule.grade} • ${highestModule.credits} credits",
                            valueColor = SuccessGreen
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )

                    if (lowestModule != null) {

                        StatisticsInfoCard(
                            title = "Module to Focus On",
                            mainText = lowestModule.name,
                            subText =
                                "${lowestModule.grade} • ${lowestModule.credits} credits",
                            valueColor = WarningOrange
                        )
                    }
                }
            }

            // =========================================================
            // ASSIGNMENT PROGRESS
            // =========================================================

            item {

                Spacer(
                    modifier = Modifier.height(25.dp)
                )

                StatisticsSectionTitle(
                    "Assignment Progress"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = "Completion",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "${assignmentPercentage.roundToInt()}%",
                                    color = HighlightBlue,
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                horizontalAlignment =
                                    Alignment.End
                            ) {

                                Text(
                                    text =
                                        "$completedAssignments completed",
                                    color = SuccessGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "$pendingAssignments pending",
                                    color = WarningOrange,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        LinearProgressIndicator(
                            progress = {
                                (
                                        assignmentPercentage / 100.0
                                        )
                                    .toFloat()
                                    .coerceIn(0f, 1f)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = HighlightBlue,
                            trackColor =
                                BorderBlue.copy(alpha = 0.45f)
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                if (totalAssignments == 0) {
                                    "No assignments recorded yet."
                                } else {
                                    "$totalAssignments total assignment${
                                        if (totalAssignments == 1) "" else "s"
                                    }"
                                },
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // =========================================================
            // SCHEDULE
            // =========================================================

            item {

                Spacer(
                    modifier = Modifier.height(25.dp)
                )

                StatisticsSectionTitle(
                    "Schedule"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    SmallStatisticCard(
                        title = "Today's Classes",
                        value = todayClasses.toString(),
                        modifier = Modifier.weight(1f)
                    )

                    SmallStatisticCard(
                        title = "Weekly Classes",
                        value = timetable.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // =========================================================
            // FINANCE
            // =========================================================

            item {

                Spacer(
                    modifier = Modifier.height(25.dp)
                )

                StatisticsSectionTitle(
                    "Finance"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        Text(
                            text = "Total Recorded Expenses",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Rs. ${
                                    String.format(
                                        "%.2f",
                                        totalExpenses
                                    )
                                }",
                            color = HighlightBlue,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(18.dp)
                        )

                        if (categoryStatistics.isEmpty()) {

                            Text(
                                text =
                                    "No expense data available.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                        } else {

                            Text(
                                text = "Expense Categories",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            categoryStatistics.forEach { item ->

                                val category = item.first
                                val amount = item.second
                                val percentage = item.third

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 6.dp
                                        )
                                ) {

                                    Row(
                                        modifier =
                                            Modifier.fillMaxWidth(),
                                        horizontalArrangement =
                                            Arrangement.SpaceBetween
                                    ) {

                                        Text(
                                            text = category,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight =
                                                FontWeight.Medium
                                        )

                                        Text(
                                            text =
                                                "Rs. ${
                                                    String.format(
                                                        "%.2f",
                                                        amount
                                                    )
                                                }",
                                            color = TextSecondary,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Spacer(
                                        modifier =
                                            Modifier.height(5.dp)
                                    )

                                    LinearProgressIndicator(
                                        progress = {
                                            percentage
                                                .toFloat()
                                                .coerceIn(
                                                    0f,
                                                    1f
                                                )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = HighlightBlue,
                                        trackColor =
                                            BorderBlue.copy(
                                                alpha = 0.45f
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================
            // OVERALL SUMMARY
            // =========================================================

            item {

                Spacer(
                    modifier = Modifier.height(25.dp)
                )

                StatisticsSectionTitle(
                    "Overall Summary"
                )

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {

                        SummaryRow(
                            "Saved Modules",
                            managedModules.size.toString()
                        )

                        SummaryRow(
                            "GPA Modules",
                            modules.size.toString()
                        )

                        SummaryRow(
                            "Total Credits",
                            formatNumber(totalCredits)
                        )

                        SummaryRow(
                            "Assignments",
                            assignments.size.toString()
                        )

                        SummaryRow(
                            "Completed Assignments",
                            completedAssignments.toString()
                        )

                        SummaryRow(
                            "Pending Assignments",
                            pendingAssignments.toString()
                        )

                        SummaryRow(
                            "Timetable Classes",
                            timetable.size.toString()
                        )

                        SummaryRow(
                            "Recorded Expenses",
                            expenses.size.toString()
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}
@Composable
fun StatisticsSectionTitle(
    title: String
) {

    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun SmallStatistic(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =
                BackgroundColor.copy(
                    alpha = 0.7f
                )
        ),
        shape = RoundedCornerShape(
            14.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                color = HighlightBlue,
                fontSize = 20.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

@Composable
fun SmallStatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor =
                CardColor
        ),
        shape = RoundedCornerShape(
            18.dp
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(
                text = value,
                color = HighlightBlue,
                fontSize = 30.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign =
                    TextAlign.Center
            )
        }
    }
}

@Composable
fun StatisticsInfoCard(
    title: String,
    mainText: String,
    subText: String,
    valueColor: Color
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                CardColor
        ),
        shape = RoundedCornerShape(
            18.dp
        )
    ) {

        Column(
            modifier = Modifier.padding(
                18.dp
            )
        ) {

            Text(
                text = title,
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = mainText,
                color = valueColor,
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(3.dp)
            )

            Text(
                text = subText,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SummaryRow(
    title: String,
    value: String
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 8.dp
            ),
        horizontalArrangement =
            Arrangement.SpaceBetween
    ) {

        Text(
            text = title,
            color = TextSecondary,
            fontSize = 14.sp
        )

        Text(
            text = value,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpaScreen(
    modules: List<Module>,
    managedModules: List<ManagedModule>,
    onAdd: (Module) -> Unit,
    onDelete: (Long) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {

    // =========================================================
    // FORM STATE
    // =========================================================

    var moduleName by remember {
        mutableStateOf("")
    }

    var credits by remember {
        mutableStateOf("")
    }

    var grade by remember {
        mutableStateOf("")
    }

    var selectedModule by remember {
        mutableStateOf<ManagedModule?>(null)
    }

    var moduleDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var gradeDropdownExpanded by remember {
        mutableStateOf(false)
    }

    var showValidationError by remember {
        mutableStateOf(false)
    }

    // =========================================================
    // STATIC DATA
    // =========================================================

    val grades = remember {
        listOf(
            "A+",
            "A",
            "A-",
            "B+",
            "B",
            "B-",
            "C+",
            "C",
            "C-",
            "D",
            "F"
        )
    }

    // =========================================================
    // GPA
    // =========================================================

    val currentGpa by remember(modules) {
        mutableStateOf(
            calculateGPA(modules)
        )
    }

    // =========================================================
    // SCREEN
    // =========================================================

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        // =====================================================
        // HEADER
        // =====================================================

        ScreenHeader(
            "GPA Calculator",
            onBack
        )

        // =====================================================
        // MAIN CONTENT
        // =====================================================

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(
                0.dp
            )
        ) {

            // =================================================
            // GPA RESULT
            // =================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 6.dp
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Current GPA",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = String.format(
                                "%.2f",
                                currentGpa
                            ),
                            color = HighlightBlue,
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                if (modules.isEmpty()) {
                                    "Add modules to calculate your GPA"
                                } else {
                                    "${modules.size} module(s) included"
                                },
                            color =
                                Color.White.copy(
                                    alpha = 0.70f
                                ),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(28.dp)
                )
            }

            // =================================================
            // ADD MODULE TITLE
            // =================================================

            item {

                Text(
                    text = "Add Module",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =================================================
            // SAVED MODULE DROPDOWN
            // =================================================

            if (managedModules.isNotEmpty()) {

                item {

                    Text(
                        text = "Select Saved Module",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = moduleDropdownExpanded,
                        onExpandedChange = {
                            moduleDropdownExpanded =
                                !moduleDropdownExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value =
                                selectedModule?.let {
                                    "${it.code} - ${it.name}"
                                } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Saved Module")
                            },
                            placeholder = {
                                Text("Choose a module")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = uniMateFieldColors(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded =
                                moduleDropdownExpanded,
                            onDismissRequest = {
                                moduleDropdownExpanded =
                                    false
                            }
                        ) {

                            managedModules.forEach { item ->

                                DropdownMenuItem(

                                    text = {

                                        Column {

                                            Text(
                                                text =
                                                    "${item.code} - ${item.name}",
                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            Text(
                                                text =
                                                    "${item.credits} credits",
                                                fontSize = 12.sp,
                                                color =
                                                    TextSecondary
                                            )
                                        }
                                    },

                                    onClick = {

                                        selectedModule = item

                                        moduleName =
                                            item.name

                                        credits =
                                            item.credits.toString()

                                        moduleDropdownExpanded =
                                            false

                                        showValidationError =
                                            false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )
                }
            }

            // =================================================
            // MODULE NAME
            // =================================================

            item {

                OutlinedTextField(
                    value = moduleName,

                    onValueChange = {

                        moduleName = it

                        if (
                            selectedModule != null &&
                            it != selectedModule!!.name
                        ) {
                            selectedModule = null
                        }

                        showValidationError = false
                    },

                    label = {
                        Text("Module Name")
                    },

                    placeholder = {
                        Text("e.g. Data Structures")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    colors = uniMateFieldColors(),

                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =================================================
            // CREDITS
            // =================================================

            item {

                OutlinedTextField(
                    value = credits,

                    onValueChange = {

                        // Allow only numbers and decimal point
                        if (
                            it.isEmpty() ||
                            it.matches(
                                Regex(
                                    "^\\d*\\.?\\d*$"
                                )
                            )
                        ) {

                            credits = it

                            showValidationError = false
                        }
                    },

                    label = {
                        Text("Credits")
                    },

                    placeholder = {
                        Text("e.g. 3")
                    },

                    modifier = Modifier.fillMaxWidth(),

                    colors = uniMateFieldColors(),

                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =================================================
            // GRADE DROPDOWN
            // =================================================

            item {

                ExposedDropdownMenuBox(
                    expanded = gradeDropdownExpanded,
                    onExpandedChange = {

                        gradeDropdownExpanded =
                            !gradeDropdownExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = grade,

                        onValueChange = {},

                        readOnly = true,

                        label = {
                            Text("Grade")
                        },

                        placeholder = {
                            Text("Select grade")
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),

                        colors = uniMateFieldColors(),

                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded =
                            gradeDropdownExpanded,

                        onDismissRequest = {

                            gradeDropdownExpanded =
                                false
                        }
                    ) {

                        grades.forEach { item ->

                            DropdownMenuItem(

                                text = {

                                    Text(
                                        text = item,
                                        fontWeight =
                                            FontWeight.Medium
                                    )
                                },

                                onClick = {

                                    grade = item

                                    gradeDropdownExpanded =
                                        false

                                    showValidationError =
                                        false
                                }
                            )
                        }
                    }
                }

                // =================================================
                // VALIDATION
                // =================================================

                if (showValidationError) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Please enter a module name, valid credits, and select a grade.",
                        color = DangerRed,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            // =================================================
            // ADD BUTTON
            // =================================================

            item {

                Button(
                    onClick = {

                        val creditValue =
                            credits.toDoubleOrNull()

                        if (
                            moduleName.isNotBlank() &&
                            creditValue != null &&
                            creditValue > 0 &&
                            grade.isNotBlank()
                        ) {

                            onAdd(
                                Module(
                                    id =
                                        System.currentTimeMillis(),

                                    name =
                                        moduleName.trim(),

                                    credits =
                                        creditValue,

                                    grade =
                                        grade
                                )
                            )

                            // Clear form
                            moduleName = ""
                            credits = ""
                            grade = ""

                            selectedModule = null

                            showValidationError = false

                        } else {

                            showValidationError = true
                        }
                    },

                    modifier = Modifier.fillMaxWidth(),

                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor =
                                PrimaryBlue
                        ),

                    shape =
                        RoundedCornerShape(14.dp)
                ) {

                    Text(
                        text = "Add Module",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }

            // =================================================
            // MODULE LIST TITLE
            // =================================================

            item {

                Text(
                    text = "Your Modules",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =================================================
            // EMPTY STATE
            // =================================================

            if (modules.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    CardColor.copy(
                                        alpha = 0.75f
                                    )
                            ),

                        shape =
                            RoundedCornerShape(18.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),

                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text =
                                    "No modules added yet",

                                color = Color.White,

                                fontSize = 16.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Add your modules above to calculate your GPA.",

                                color =
                                    TextSecondary,

                                fontSize = 13.sp
                            )
                        }
                    }
                }

            } else {

                // =================================================
                // MODULE CARDS
                // =================================================

                items(
                    items = modules,
                    key = { module ->
                        module.id
                    }
                ) { module ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                bottom = 10.dp
                            ),

                        colors =
                            CardDefaults.cardColors(
                                containerColor =
                                    CardColor
                            ),

                        shape =
                            RoundedCornerShape(18.dp),

                        elevation =
                            CardDefaults.cardElevation(
                                defaultElevation = 4.dp
                            )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(17.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Column(
                                modifier =
                                    Modifier.weight(1f)
                            ) {

                                Text(
                                    text =
                                        module.name,

                                    color =
                                        Color.White,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize = 16.sp
                                )

                                Spacer(
                                    modifier =
                                        Modifier.height(4.dp)
                                )

                                Text(
                                    text =
                                        "${module.credits} credits",

                                    color =
                                        TextSecondary,

                                    fontSize = 13.sp
                                )
                            }

                            Card(
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            PrimaryBlue.copy(
                                                alpha = 0.25f
                                            )
                                    ),

                                shape =
                                    RoundedCornerShape(
                                        10.dp
                                    )
                            ) {

                                Text(
                                    text =
                                        module.grade,

                                    color =
                                        HighlightBlue,

                                    fontWeight =
                                        FontWeight.Bold,

                                    fontSize = 15.sp,

                                    modifier =
                                        Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 7.dp
                                        )
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.width(6.dp)
                            )

                            TextButton(
                                onClick = {

                                    onDelete(
                                        module.id
                                    )
                                }
                            ) {

                                Text(
                                    text = "Delete",

                                    color =
                                        DangerRed,

                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // =================================================
                // CLEAR ALL
                // =================================================

                item {

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    OutlinedButton(
                        onClick = onClear,

                        modifier =
                            Modifier.fillMaxWidth(),

                        shape =
                            RoundedCornerShape(14.dp)
                    ) {

                        Text(
                            text = "Clear All",
                            color = DangerRed
                        )
                    }
                }
            }

            // Bottom spacing
            item {

                Spacer(
                    modifier =
                        Modifier.height(30.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleManagerScreen(
    modules: List<ManagedModule>,
    onAdd: (ManagedModule) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    var code by remember {
        mutableStateOf("")
    }

    var name by remember {
        mutableStateOf("")
    }

    var credits by remember {
        mutableStateOf("")
    }

    var lecturer by remember {
        mutableStateOf("")
    }

    var semester by remember {
        mutableStateOf("")
    }

    var semesterExpanded by remember {
        mutableStateOf(false)
    }

    var showValidationError by remember {
        mutableStateOf(false)
    }

    val semesters = remember {
        listOf(
            "Semester 1",
            "Semester 2",
            "Semester 3",
            "Semester 4",
            "Semester 5",
            "Semester 6",
            "Semester 7",
            "Semester 8"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 20.dp
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        item {
            ScreenHeader(
                title = "Module Manager",
                onBack = onBack
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(20.dp)
            )

            Text(
                text = "Add Module",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "Save your university modules for quick access.",
                color = TextSecondary,
                fontSize = 13.sp
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        item {
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    showValidationError = false
                },
                label = {
                    Text("Module Code")
                },
                placeholder = {
                    Text("Example: COA201")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = uniMateFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    showValidationError = false
                },
                label = {
                    Text("Module Name")
                },
                placeholder = {
                    Text("Example: Computer Organization")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = uniMateFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = credits,
                onValueChange = {
                    if (
                        it.isEmpty() ||
                        it.matches(
                            Regex("^\\d*\\.?\\d*$")
                        )
                    ) {
                        credits = it
                        showValidationError = false
                    }
                },
                label = {
                    Text("Credits")
                },
                placeholder = {
                    Text("Example: 3")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = uniMateFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        item {
            OutlinedTextField(
                value = lecturer,
                onValueChange = {
                    lecturer = it
                    showValidationError = false
                },
                label = {
                    Text("Lecturer")
                },
                placeholder = {
                    Text("Optional")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = uniMateFieldColors()
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = semesterExpanded,
                onExpandedChange = {
                    semesterExpanded = !semesterExpanded
                }
            ) {

                OutlinedTextField(
                    value = semester,
                    onValueChange = {},
                    readOnly = true,
                    label = {
                        Text("Semester")
                    },
                    placeholder = {
                        Text("Select semester")
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = semesterExpanded
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = uniMateFieldColors()
                )

                ExposedDropdownMenu(
                    expanded = semesterExpanded,
                    onDismissRequest = {
                        semesterExpanded = false
                    }
                ) {

                    semesters.forEach { item ->

                        DropdownMenuItem(
                            text = {
                                Text(item)
                            },
                            onClick = {
                                semester = item
                                semesterExpanded = false
                                showValidationError = false
                            }
                        )
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }

        item {
            if (showValidationError) {

                Text(
                    text = "Please fill in the required module details.",
                    color = DangerRed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(
                        bottom = 10.dp
                    )
                )
            }

            Button(
                onClick = {

                    val creditValue =
                        credits.toDoubleOrNull()

                    if (
                        code.isBlank() ||
                        name.isBlank() ||
                        creditValue == null ||
                        creditValue <= 0 ||
                        semester.isBlank()
                    ) {
                        showValidationError = true
                        return@Button
                    }

                    onAdd(
                        ManagedModule(
                            id = System.currentTimeMillis(),
                            code = code.trim(),
                            name = name.trim(),
                            credits = creditValue,
                            lecturer = lecturer.trim(),
                            semester = semester
                        )
                    )

                    code = ""
                    name = ""
                    credits = ""
                    lecturer = ""
                    semester = ""
                    semesterExpanded = false
                    showValidationError = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Save Module"
                )
            }

            Spacer(
                modifier = Modifier.height(28.dp)
            )
        }

        item {
            Text(
                text = "Saved Modules",
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )
        }

        if (modules.isEmpty()) {

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            CardColor.copy(alpha = 0.75f)
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "No saved modules",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text =
                                "Add your university modules above.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

        } else {

            items(
                items = modules,
                key = { module ->
                    module.id
                }
            ) { module ->

                ModuleCard(
                    module = module,
                    onDelete = {
                        onDelete(module.id)
                    }
                )
            }
        }

        item {
            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}
@Composable
fun ModuleCard(
    module: ManagedModule,
    onDelete: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 10.dp
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardColor
            ),
        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = module.code,
                color = HighlightBlue,
                fontSize = 14.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            Text(
                text = module.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            Text(
                text =
                    "${module.credits} credits • ${module.semester}",
                color = TextSecondary,
                fontSize = 13.sp
            )

            if (module.lecturer.isNotBlank()) {

                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Lecturer: ${module.lecturer}",
                    color =
                        TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            TextButton(
                onClick = onDelete
            ) {

                Text(
                    text = "Delete",
                    color = DangerRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    assignments: List<Assignment>,
    managedModules: List<ManagedModule>,
    onAdd: (Assignment) -> Unit,
    onDelete: (Long) -> Unit,
    onToggle: (Long) -> Unit,
    onBack: () -> Unit
) {
    var title by remember {
        mutableStateOf("")
    }

    var selectedModule by remember {
        mutableStateOf("")
    }

    var dueDate by remember {
        mutableStateOf("")
    }

    var moduleExpanded by remember {
        mutableStateOf(false)
    }

    var showValidationError by remember {
        mutableStateOf(false)
    }

    val pendingAssignments = remember(assignments) {
        assignments.filter { !it.completed }
    }

    val completedAssignments = remember(assignments) {
        assignments.filter { it.completed }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        ScreenHeader(
            "Assignments",
            onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            )
        ) {

            // ==========================================
            // HEADER
            // ==========================================

            item {

                Text(
                    text = "Add Assignment",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Keep track of your upcoming university tasks.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )
            }

            // ==========================================
            // TITLE
            // ==========================================

            item {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showValidationError = false
                    },
                    label = {
                        Text("Assignment Title")
                    },
                    placeholder = {
                        Text("e.g. Network Design Report")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // ==========================================
            // MODULE
            // ==========================================

            item {

                if (managedModules.isNotEmpty()) {

                    ExposedDropdownMenuBox(
                        expanded = moduleExpanded,
                        onExpandedChange = {
                            moduleExpanded = !moduleExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value = selectedModule,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Module")
                            },
                            placeholder = {
                                Text("Select module")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = uniMateFieldColors(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = moduleExpanded,
                            onDismissRequest = {
                                moduleExpanded = false
                            }
                        ) {

                            managedModules.forEach { module ->

                                DropdownMenuItem(
                                    text = {

                                        Column {

                                            Text(
                                                text =
                                                    "${module.code} - ${module.name}",
                                                fontWeight =
                                                    FontWeight.Medium
                                            )

                                            Text(
                                                text =
                                                    "${module.credits} credits",
                                                color =
                                                    TextSecondary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    },
                                    onClick = {

                                        selectedModule =
                                            module.name

                                        moduleExpanded =
                                            false

                                        showValidationError =
                                            false
                                    }
                                )
                            }
                        }
                    }

                } else {

                    OutlinedTextField(
                        value = selectedModule,
                        onValueChange = {
                            selectedModule = it
                            showValidationError = false
                        },
                        label = {
                            Text("Module")
                        },
                        placeholder = {
                            Text("e.g. Data Communication")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = uniMateFieldColors(),
                        singleLine = true
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // ==========================================
            // DUE DATE
            // ==========================================

            item {

                OutlinedTextField(
                    value = dueDate,
                    onValueChange = {

                        dueDate = it
                        showValidationError = false
                    },
                    label = {
                        Text("Due Date")
                    },
                    placeholder = {
                        Text("DD/MM/YYYY")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text =
                        "Format: DD/MM/YYYY • Reminder is scheduled 1 day before at 9:00 AM",
                    color =
                        TextSecondary.copy(
                            alpha = 0.75f
                        ),
                    fontSize = 11.sp
                )
            }

            // ==========================================
            // VALIDATION
            // ==========================================

            item {

                if (showValidationError) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Please enter an assignment title and select a module.",
                        color = DangerRed,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            // ==========================================
            // ADD BUTTON
            // ==========================================

            item {

                Button(
                    onClick = {

                        if (
                            title.isNotBlank() &&
                            selectedModule.isNotBlank()
                        ) {

                            onAdd(
                                Assignment(
                                    id =
                                        System.currentTimeMillis(),

                                    title =
                                        title.trim(),

                                    module =
                                        selectedModule,

                                    dueDate =
                                        dueDate.trim(),

                                    completed =
                                        false
                                )
                            )

                            title = ""
                            selectedModule = ""
                            dueDate = ""

                            showValidationError = false

                        } else {

                            showValidationError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Text(
                        text = "Add Assignment",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }

            // ==========================================
            // ASSIGNMENT LIST TITLE
            // ==========================================

            item {

                Text(
                    text = "Your Assignments",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // ==========================================
            // EMPTY STATE
            // ==========================================

            if (assignments.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                CardColor.copy(
                                    alpha = 0.75f
                                )
                        ),
                        shape = RoundedCornerShape(18.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "No assignments yet",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Add an assignment above to start tracking your tasks.",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

            } else {

                // ==========================================
                // PENDING HEADER
                // ==========================================

                if (pendingAssignments.isNotEmpty()) {

                    item {

                        Text(
                            text = "Pending",
                            color = HighlightBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                    // ==========================================
                    // PENDING ASSIGNMENTS
                    // ==========================================

                    items(
                        items = pendingAssignments,
                        key = { assignment ->
                            assignment.id
                        }
                    ) { assignment ->

                        AssignmentCard(
                            assignment = assignment,
                            onDelete = {
                                onDelete(
                                    assignment.id
                                )
                            },
                            onToggle = {
                                onToggle(
                                    assignment.id
                                )
                            }
                        )
                    }
                }

                // ==========================================
                // COMPLETED HEADER
                // ==========================================

                if (completedAssignments.isNotEmpty()) {

                    item {

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )

                        Text(
                            text = "Completed",
                            color = SuccessGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.height(8.dp)
                        )
                    }

                    // ==========================================
                    // COMPLETED ASSIGNMENTS
                    // ==========================================

                    items(
                        items = completedAssignments,
                        key = { assignment ->
                            assignment.id
                        }
                    ) { assignment ->

                        AssignmentCard(
                            assignment = assignment,
                            onDelete = {
                                onDelete(
                                    assignment.id
                                )
                            },
                            onToggle = {
                                onToggle(
                                    assignment.id
                                )
                            }
                        )
                    }
                }
            }

            // ==========================================
            // BOTTOM SPACE
            // ==========================================

            item {

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}
@Composable
fun AssignmentCard(
    assignment: Assignment,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 10.dp
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardColor
            ),
        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Checkbox(
                    checked =
                        assignment.completed,
                    onCheckedChange = {
                        onToggle()
                    }
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            assignment.title,
                        color =
                            if (
                                assignment.completed
                            )
                                TextSecondary
                            else
                                Color.White,
                        fontSize = 16.sp,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            assignment.module,
                        color =
                            HighlightBlue,
                        fontSize = 13.sp
                    )

                    if (
                        assignment.dueDate
                            .isNotBlank()
                    ) {

                        Text(
                            text =
                                "Due: ${assignment.dueDate}",
                            color =
                                TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            TextButton(
                onClick = onDelete
            ) {

                Text(
                    text = "Delete",
                    color =
                        DangerRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    expenses: List<Expense>,
    onAdd: (Expense) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {

    var title by remember {
        mutableStateOf("")
    }

    var amount by remember {
        mutableStateOf("")
    }

    var category by remember {
        mutableStateOf("")
    }

    var date by remember {
        mutableStateOf("")
    }

    // Calculate total only when expenses change
    val total = remember(expenses) {
        expenses.sumOf {
            it.amount
        }
    }

    // Format total only when total changes
    val formattedTotal = remember(total) {
        String.format(
            "%.2f",
            total
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        ScreenHeader(
            "Finance",
            onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            )
        ) {

            // =====================================================
            // TOTAL EXPENSES
            // =====================================================

            item {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment =
                            Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "Total Expenses",
                            color = TextSecondary
                        )

                        Spacer(
                            modifier = Modifier.height(5.dp)
                        )

                        Text(
                            text = "Rs. $formattedTotal",
                            color = HighlightBlue,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }

            // =====================================================
            // EXPENSE TITLE
            // =====================================================

            item {

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Expense Title")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors()
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =====================================================
            // AMOUNT
            // =====================================================

            item {

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    label = {
                        Text("Amount")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors()
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =====================================================
            // CATEGORY
            // =====================================================

            item {

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                    },
                    label = {
                        Text("Category")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors()
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // =====================================================
            // DATE
            // =====================================================

            item {

                OutlinedTextField(
                    value = date,
                    onValueChange = {
                        date = it
                    },
                    label = {
                        Text("Date")
                    },
                    placeholder = {
                        Text("DD/MM/YYYY")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors()
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            // =====================================================
            // ADD EXPENSE
            // =====================================================

            item {

                Button(
                    onClick = {

                        val amountValue =
                            amount.toDoubleOrNull()

                        if (
                            title.isNotBlank() &&
                            amountValue != null &&
                            amountValue > 0
                        ) {

                            onAdd(
                                Expense(
                                    id =
                                        System.currentTimeMillis(),
                                    title = title,
                                    amount = amountValue,
                                    category = category,
                                    date = date
                                )
                            )

                            title = ""
                            amount = ""
                            category = ""
                            date = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    )
                ) {

                    Text(
                        "Add Expense"
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }

            // =====================================================
            // EXPENSE LIST
            // =====================================================

            items(
                items = expenses,
                key = { expense ->
                    expense.id
                }
            ) { expense ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = 10.dp
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = CardColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = expense.title,
                                color = Color.White,
                                fontWeight =
                                    FontWeight.Bold,
                                fontSize = 16.sp
                            )

                            Text(
                                text =
                                    if (expense.category.isBlank()) {
                                        "Other"
                                    } else {
                                        expense.category
                                    },
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            if (
                                expense.date.isNotBlank()
                            ) {

                                Text(
                                    text = expense.date,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {

                            Text(
                                text =
                                    "Rs. ${
                                        String.format(
                                            "%.2f",
                                            expense.amount
                                        )
                                    }",
                                color = HighlightBlue,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            TextButton(
                                onClick = {
                                    onDelete(
                                        expense.id
                                    )
                                }
                            ) {

                                Text(
                                    text = "Delete",
                                    color = DangerRed
                                )
                            }
                        }
                    }
                }
            }

            // =====================================================
            // BOTTOM SPACE
            // =====================================================

            item {

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    timetable: List<TimetableClass>,
    managedModules: List<ManagedModule>,
    onAdd: (TimetableClass) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    var day by remember {
        mutableStateOf("")
    }

    var subject by remember {
        mutableStateOf("")
    }

    var time by remember {
        mutableStateOf("")
    }

    var room by remember {
        mutableStateOf("")
    }

    var lecturer by remember {
        mutableStateOf("")
    }

    var moduleCode by remember {
        mutableStateOf("")
    }

    var moduleExpanded by remember {
        mutableStateOf(false)
    }

    var dayExpanded by remember {
        mutableStateOf(false)
    }

    var showValidationError by remember {
        mutableStateOf(false)
    }

    val days = remember {
        listOf(
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday"
        )
    }

    // Group timetable only when timetable changes
    val timetableByDay = remember(timetable) {
        timetable.groupBy { it.day }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {

        ScreenHeader(
            "Timetable",
            onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            )
        ) {

            // -----------------------------
            // ADD CLASS HEADER
            // -----------------------------

            item {

                Text(
                    text = "Add Class",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Create your weekly university timetable.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )
            }

            // -----------------------------
            // SAVED MODULE
            // -----------------------------

            if (managedModules.isNotEmpty()) {

                item {

                    ExposedDropdownMenuBox(
                        expanded = moduleExpanded,
                        onExpandedChange = {
                            moduleExpanded = !moduleExpanded
                        }
                    ) {

                        OutlinedTextField(
                            value =
                                if (moduleCode.isNotBlank()) {
                                    "$moduleCode - $subject"
                                } else {
                                    ""
                                },
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text("Saved Module")
                            },
                            placeholder = {
                                Text("Choose module")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = uniMateFieldColors(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = moduleExpanded,
                            onDismissRequest = {
                                moduleExpanded = false
                            }
                        ) {

                            managedModules.forEach { item ->

                                DropdownMenuItem(
                                    text = {

                                        Column {

                                            Text(
                                                text =
                                                    "${item.code} - ${item.name}",
                                                fontWeight =
                                                    FontWeight.Bold
                                            )

                                            if (
                                                item.lecturer.isNotBlank()
                                            ) {

                                                Text(
                                                    text = item.lecturer,
                                                    fontSize = 12.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    },
                                    onClick = {

                                        moduleCode =
                                            item.code

                                        subject =
                                            item.name

                                        lecturer =
                                            item.lecturer

                                        moduleExpanded =
                                            false

                                        showValidationError =
                                            false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }

            } else {

                // -----------------------------
                // MANUAL SUBJECT
                // -----------------------------

                item {

                    OutlinedTextField(
                        value = subject,
                        onValueChange = {
                            subject = it
                            showValidationError = false
                        },
                        label = {
                            Text("Subject")
                        },
                        placeholder = {
                            Text("e.g. Data Communication")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = uniMateFieldColors(),
                        singleLine = true
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }
            }

            // -----------------------------
            // DAY
            // -----------------------------

            item {

                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = {
                        dayExpanded = !dayExpanded
                    }
                ) {

                    OutlinedTextField(
                        value = day,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text("Day")
                        },
                        placeholder = {
                            Text("Select day")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = uniMateFieldColors(),
                        singleLine = true
                    )

                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = {
                            dayExpanded = false
                        }
                    ) {

                        days.forEach { item ->

                            DropdownMenuItem(
                                text = {
                                    Text(item)
                                },
                                onClick = {

                                    day = item

                                    dayExpanded = false

                                    showValidationError = false
                                }
                            )
                        }
                    }
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // -----------------------------
            // SUBJECT
            // -----------------------------

            item {

                OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        showValidationError = false
                    },
                    label = {
                        Text("Subject")
                    },
                    placeholder = {
                        Text("e.g. Computer Networks")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // -----------------------------
            // TIME
            // -----------------------------

            item {

                OutlinedTextField(
                    value = time,
                    onValueChange = {
                        time = it
                        showValidationError = false
                    },
                    label = {
                        Text("Time")
                    },
                    placeholder = {
                        Text("8:00 AM - 10:00 AM")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Example: 8:00 AM - 10:00 AM",
                    color = TextSecondary.copy(
                        alpha = 0.75f
                    ),
                    fontSize = 11.sp
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // -----------------------------
            // ROOM
            // -----------------------------

            item {

                OutlinedTextField(
                    value = room,
                    onValueChange = {
                        room = it
                    },
                    label = {
                        Text("Room")
                    },
                    placeholder = {
                        Text("e.g. Lab 03")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors(),
                    singleLine = true
                )

                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            // -----------------------------
            // LECTURER
            // -----------------------------

            item {

                OutlinedTextField(
                    value = lecturer,
                    onValueChange = {
                        lecturer = it
                    },
                    label = {
                        Text("Lecturer")
                    },
                    placeholder = {
                        Text("e.g. Dr. Perera")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = uniMateFieldColors(),
                    singleLine = true
                )

                // -----------------------------
                // VALIDATION
                // -----------------------------

                if (showValidationError) {

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Please select a day and enter a subject and time.",
                        color = DangerRed,
                        fontSize = 13.sp
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )
            }

            // -----------------------------
            // ADD BUTTON
            // -----------------------------

            item {

                Button(
                    onClick = {

                        if (
                            day.isNotBlank() &&
                            subject.isNotBlank() &&
                            time.isNotBlank()
                        ) {

                            onAdd(
                                TimetableClass(
                                    id =
                                        System.currentTimeMillis(),

                                    day =
                                        day,

                                    subject =
                                        subject.trim(),

                                    time =
                                        time.trim(),

                                    room =
                                        room.trim(),

                                    lecturer =
                                        lecturer.trim(),

                                    moduleCode =
                                        moduleCode.trim()
                                )
                            )

                            day = ""
                            subject = ""
                            time = ""
                            room = ""
                            lecturer = ""
                            moduleCode = ""

                            showValidationError = false

                        } else {

                            showValidationError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {

                    Text(
                        text = "Add Class",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }

            // -----------------------------
            // TIMETABLE HEADER
            // -----------------------------

            item {

                Text(
                    text = "Your Timetable",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "Your scheduled classes for the week.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )

                Spacer(
                    modifier = Modifier.height(14.dp)
                )
            }

            // -----------------------------
            // EMPTY STATE
            // -----------------------------

            if (timetable.isEmpty()) {

                item {

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                CardColor.copy(alpha = 0.75f)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                text = "No classes added yet",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Add your first class above to build your weekly timetable.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

            } else {

                // -----------------------------
                // GROUPED TIMETABLE
                // -----------------------------

                days.forEach { currentDay ->

                    val dayClasses =
                        timetableByDay[currentDay]
                            ?: emptyList()

                    if (dayClasses.isNotEmpty()) {

                        item(
                            key = "header_$currentDay"
                        ) {

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Text(
                                text = currentDay,
                                color = HighlightBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                        }

                        items(
                            items = dayClasses,
                            key = { item ->
                                item.id
                            }
                        ) { item ->

                            TimetableCard(
                                item = item,
                                onDelete = {
                                    onDelete(item.id)
                                }
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                        }
                    }
                }
            }

            item {

                Spacer(
                    modifier = Modifier.height(30.dp)
                )
            }
        }
    }
}
@Composable
fun TimetableCard(
    item: TimetableClass,
    onDelete: () -> Unit
) {

    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    bottom = 10.dp
                ),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    CardColor
            ),
        shape =
            RoundedCornerShape(
                16.dp
            )
    ) {

        Column(
            modifier =
                Modifier.padding(16.dp)
        ) {

            Text(
                text = item.day,
                color = HighlightBlue,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 14.sp
            )

            Spacer(
                modifier =
                    Modifier.height(4.dp)
            )

            if (
                item.moduleCode.isNotBlank()
            ) {

                Text(
                    text =
                        item.moduleCode,
                    color =
                        TextSecondary,
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Text(
                text =
                    item.subject,
                color =
                    Color.White,
                fontSize = 17.sp,
                fontWeight =
                    FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.height(6.dp)
            )

            Text(
                text =
                    item.time,
                color =
                    TextSecondary,
                fontSize = 13.sp
            )

            if (
                item.room.isNotBlank()
            ) {

                Text(
                    text =
                        "Room: ${item.room}",
                    color =
                        TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (
                item.lecturer.isNotBlank()
            ) {

                Text(
                    text =
                        "Lecturer: ${item.lecturer}",
                    color =
                        TextSecondary,
                    fontSize = 13.sp
                )
            }

            Spacer(
                modifier =
                    Modifier.height(5.dp)
            )

            TextButton(
                onClick = onDelete
            ) {

                Text(
                    text = "Delete",
                    color =
                        DangerRed
                )
            }
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: () -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderColor)
            .padding(
                horizontal = 8.dp,
                vertical = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = onBack
        ) {

            Text(
                text = "‹",
                color = Color.White,
                fontSize = 36.sp
            )
        }

        Spacer(
            modifier = Modifier.width(4.dp)
        )

        Text(
            text = title,
            color = Color.White,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun uniMateFieldColors(): TextFieldColors {

    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor =
            HighlightBlue,
        unfocusedBorderColor =
            BorderBlue,
        focusedLabelColor =
            HighlightBlue,
        unfocusedLabelColor =
            TextSecondary,
        cursorColor =
            HighlightBlue,
        focusedTextColor =
            Color.White,
        unfocusedTextColor =
            Color.White,
        focusedPlaceholderColor =
            TextSecondary,
        unfocusedPlaceholderColor =
            TextSecondary
    )
}
fun scheduleAssignmentReminder(
    context: Context,
    assignment: Assignment
) {

    // Completed assignments don't need reminders
    if (assignment.completed) {
        ReminderScheduler.cancelReminder(
            context = context,
            notificationId = assignment.id.hashCode()
        )
        return
    }

    // No due date = no reminder
    if (assignment.dueDate.isBlank()) {
        return
    }

    try {

        val formatter = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

        formatter.isLenient = false

        val dueDate = formatter.parse(
            assignment.dueDate
        ) ?: return

        val calendar = Calendar.getInstance()

        calendar.time = dueDate

        // Reminder one day before
        calendar.add(
            Calendar.DAY_OF_YEAR,
            -1
        )

        // Reminder time = 9:00 AM
        calendar.set(
            Calendar.HOUR_OF_DAY,
            9
        )

        calendar.set(
            Calendar.MINUTE,
            0
        )

        calendar.set(
            Calendar.SECOND,
            0
        )

        calendar.set(
            Calendar.MILLISECOND,
            0
        )

        val reminderTime =
            calendar.timeInMillis

        // Only schedule future reminders
        if (
            reminderTime >
            System.currentTimeMillis()
        ) {

            ReminderScheduler.scheduleReminder(
                context = context,
                triggerTimeMillis =
                    reminderTime,
                title =
                    "Assignment Reminder",
                message =
                    "${assignment.title} is due tomorrow.",
                notificationId =
                    assignment.id.hashCode()
            )
        }

    } catch (e: Exception) {

        e.printStackTrace()
    }
}

fun getGreeting(): String {

    val hour =
        Calendar
            .getInstance()
            .get(
                Calendar.HOUR_OF_DAY
            )

    return when {

        hour < 12 ->
            "Good Morning 👋"

        hour < 17 ->
            "Good Afternoon 👋"

        else ->
            "Good Evening 👋"
    }
}

fun getTodayName(): String {

    return when (
        Calendar
            .getInstance()
            .get(
                Calendar.DAY_OF_WEEK
            )
    ) {

        Calendar.MONDAY ->
            "Monday"

        Calendar.TUESDAY ->
            "Tuesday"

        Calendar.WEDNESDAY ->
            "Wednesday"

        Calendar.THURSDAY ->
            "Thursday"

        Calendar.FRIDAY ->
            "Friday"

        Calendar.SATURDAY ->
            "Saturday"

        else ->
            "Sunday"
    }
}

fun formatNumber(
    number: Double
): String {

    return if (
        number % 1.0 == 0.0
    ) {

        number
            .toInt()
            .toString()

    } else {

        String.format(
            "%.1f",
            number
        )
    }
}

fun gradePoint(
    grade: String
): Double {

    return when (
        grade.uppercase()
    ) {

        "A+" -> 4.0
        "A" -> 4.0
        "A-" -> 3.7
        "B+" -> 3.3
        "B" -> 3.0
        "B-" -> 2.7
        "C+" -> 2.3
        "C" -> 2.0
        "C-" -> 1.7
        "D" -> 1.0

        else -> 0.0
    }
}

fun calculateGPA(
    modules: List<Module>
): Double {

    if (modules.isEmpty()) {
        return 0.0
    }

    var totalPoints = 0.0
    var totalCredits = 0.0

    modules.forEach {
            module ->

        totalPoints +=
            gradePoint(
                module.grade
            ) * module.credits

        totalCredits +=
            module.credits
    }

    return if (
        totalCredits > 0
    ) {

        totalPoints /
                totalCredits

    } else {

        0.0
    }
}

fun saveModules(
    context: Context,
    modules: List<Module>
) {

    val array =
        JSONArray()

    modules.forEach {
            module ->

        val obj =
            JSONObject()

        obj.put(
            "id",
            module.id
        )

        obj.put(
            "name",
            module.name
        )

        obj.put(
            "credits",
            module.credits
        )

        obj.put(
            "grade",
            module.grade
        )

        array.put(obj)
    }

    context
        .getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            KEY_MODULES,
            array.toString()
        )
        .apply()
}

fun loadModules(
    context: Context
): List<Module> {

    val json =
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_MODULES,
                null
            )
            ?: return emptyList()

    val array =
        JSONArray(json)

    val list =
        mutableListOf<Module>()

    for (
    i in 0 until array.length()
    ) {

        val obj =
            array.getJSONObject(i)

        list.add(
            Module(
                id =
                    obj.getLong("id"),
                name =
                    obj.getString(
                        "name"
                    ),
                credits =
                    obj.getDouble(
                        "credits"
                    ),
                grade =
                    obj.getString(
                        "grade"
                    )
            )
        )
    }

    return list
}

fun saveManagedModules(
    context: Context,
    modules: List<ManagedModule>
) {

    val array =
        JSONArray()

    modules.forEach {
            module ->

        val obj =
            JSONObject()

        obj.put(
            "id",
            module.id
        )

        obj.put(
            "code",
            module.code
        )

        obj.put(
            "name",
            module.name
        )

        obj.put(
            "credits",
            module.credits
        )

        obj.put(
            "lecturer",
            module.lecturer
        )

        obj.put(
            "semester",
            module.semester
        )

        array.put(obj)
    }

    context
        .getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            KEY_MANAGED_MODULES,
            array.toString()
        )
        .apply()
}

fun loadManagedModules(
    context: Context
): List<ManagedModule> {

    val json =
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_MANAGED_MODULES,
                null
            )
            ?: return emptyList()

    val array =
        JSONArray(json)

    val list =
        mutableListOf<ManagedModule>()

    for (
    i in 0 until array.length()
    ) {

        val obj =
            array.getJSONObject(i)

        list.add(
            ManagedModule(
                id =
                    obj.getLong("id"),
                code =
                    obj.getString(
                        "code"
                    ),
                name =
                    obj.getString(
                        "name"
                    ),
                credits =
                    obj.getDouble(
                        "credits"
                    ),
                lecturer =
                    obj.optString(
                        "lecturer"
                    ),
                semester =
                    obj.optString(
                        "semester"
                    )
            )
        )
    }

    return list
}

fun saveAssignments(
    context: Context,
    assignments: List<Assignment>
) {

    val array =
        JSONArray()

    assignments.forEach {
            assignment ->

        val obj =
            JSONObject()

        obj.put(
            "id",
            assignment.id
        )

        obj.put(
            "title",
            assignment.title
        )

        obj.put(
            "module",
            assignment.module
        )

        obj.put(
            "dueDate",
            assignment.dueDate
        )

        obj.put(
            "completed",
            assignment.completed
        )

        array.put(obj)
    }

    context
        .getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            KEY_ASSIGNMENTS,
            array.toString()
        )
        .apply()
}

fun loadAssignments(
    context: Context
): List<Assignment> {

    val json =
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_ASSIGNMENTS,
                null
            )
            ?: return emptyList()

    val array =
        JSONArray(json)

    val list =
        mutableListOf<Assignment>()

    for (
    i in 0 until array.length()
    ) {

        val obj =
            array.getJSONObject(i)

        list.add(
            Assignment(
                id =
                    obj.getLong(
                        "id"
                    ),
                title =
                    obj.getString(
                        "title"
                    ),
                module =
                    obj.getString(
                        "module"
                    ),
                dueDate =
                    obj.optString(
                        "dueDate"
                    ),
                completed =
                    obj.optBoolean(
                        "completed",
                        false
                    )
            )
        )
    }

    return list
}

fun saveExpenses(
    context: Context,
    expenses: List<Expense>
) {

    val array =
        JSONArray()

    expenses.forEach {
            expense ->

        val obj =
            JSONObject()

        obj.put(
            "id",
            expense.id
        )

        obj.put(
            "title",
            expense.title
        )

        obj.put(
            "amount",
            expense.amount
        )

        obj.put(
            "category",
            expense.category
        )

        obj.put(
            "date",
            expense.date
        )

        array.put(obj)
    }

    context
        .getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            KEY_EXPENSES,
            array.toString()
        )
        .apply()
}

fun loadExpenses(
    context: Context
): List<Expense> {

    val json =
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_EXPENSES,
                null
            )
            ?: return emptyList()

    val array =
        JSONArray(json)

    val list =
        mutableListOf<Expense>()

    for (
    i in 0 until array.length()
    ) {

        val obj =
            array.getJSONObject(i)

        list.add(
            Expense(
                id =
                    obj.getLong(
                        "id"
                    ),
                title =
                    obj.getString(
                        "title"
                    ),
                amount =
                    obj.getDouble(
                        "amount"
                    ),
                category =
                    obj.optString(
                        "category"
                    ),
                date =
                    obj.optString(
                        "date"
                    )
            )
        )
    }

    return list
}

fun saveTimetable(
    context: Context,
    timetable: List<TimetableClass>
) {

    val array =
        JSONArray()

    timetable.forEach {
            item ->

        val obj =
            JSONObject()

        obj.put(
            "id",
            item.id
        )

        obj.put(
            "day",
            item.day
        )

        obj.put(
            "subject",
            item.subject
        )

        obj.put(
            "time",
            item.time
        )

        obj.put(
            "room",
            item.room
        )

        obj.put(
            "lecturer",
            item.lecturer
        )

        obj.put(
            "moduleCode",
            item.moduleCode
        )

        array.put(obj)
    }

    context
        .getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )
        .edit()
        .putString(
            KEY_TIMETABLE,
            array.toString()
        )
        .apply()
}

fun loadTimetable(
    context: Context
): List<TimetableClass> {

    val json =
        context
            .getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_TIMETABLE,
                null
            )
            ?: return emptyList()

    val array =
        JSONArray(json)

    val list =
        mutableListOf<TimetableClass>()

    for (
    i in 0 until array.length()
    ) {

        val obj =
            array.getJSONObject(i)

        list.add(
            TimetableClass(
                id =
                    obj.getLong(
                        "id"
                    ),
                day =
                    obj.getString(
                        "day"
                    ),
                subject =
                    obj.getString(
                        "subject"
                    ),
                time =
                    obj.getString(
                        "time"
                    ),
                room =
                    obj.optString(
                        "room"
                    ),
                lecturer =
                    obj.optString(
                        "lecturer"
                    ),
                moduleCode =
                    obj.optString(
                        "moduleCode"
                    )
            )
        )
    }

    return list
}
