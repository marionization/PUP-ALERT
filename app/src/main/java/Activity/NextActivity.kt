package Activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.LinearProgressIndicator
import Activity.AnalyticsPrintHelper
import Activity.AnalyticsExcelHelper
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.example.seriousmode.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val role = intent.getStringExtra("role") ?: "Administrator"

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val fullUserName = prefs.getString("user_name", "Student") ?: "Student"
        val firstName = prefs.getString("student_first_name", "Student") ?: "Student"
        val studentId = prefs.getString("student_id", "") ?: ""

        setContent {
            var showWelcomeScreen by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1800)
                showWelcomeScreen = false
            }

            if (showWelcomeScreen) {
                WelcomeLoadingScreen(
                    role = role,
                    firstName = firstName
                )
            } else {
                NextActivityContent(
                    role = role,
                    userName = fullUserName,
                    greetingName = firstName,
                    studentId = studentId,
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        prefs.edit().clear().apply()
                        val intent = Intent(this@NextActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}

enum class DrawerScreen(val title: String) {
    Profile("Profile"),
    MyReports("My Reports"),
    Reports("Reports"),
    Dashboard("Dashboard"),
    CampusPolls("Student Polls"),
    Analytics("Analytics"),
    Settings("Settings")
}

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val reportId: String = "",
    val reportTitle: String = "",
    val status: String = "",
    val reporter: String = "",
    val targetRole: String = "",
    val targetUser: String = "",
    val targetUid: String = "",
    val type: String = "",
    val isAnonymous: Boolean = false,
    val read: Boolean = false,
    val timestamp: Long = 0L
)


@Composable
fun WelcomeLoadingScreen(role: String, firstName: String = "Student") {
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    val welcomeText = if (isAdmin) "HELLO ADMIN!" else "Hello, $firstName"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F9)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFFE1001B),
                strokeWidth = 4.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = welcomeText,
                    color = Color(0xFFE1001B),
                    fontSize = if (isAdmin) 26.sp else 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )
            }
        }
    }
}

@Composable
fun NextActivityContent(
    role: String,
    userName: String,
    greetingName: String,
    studentId: String,
    onLogout: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    var selectedScreen by remember { mutableStateOf(DrawerScreen.Dashboard) }
    var reportSelectedTab by remember { mutableIntStateOf(0) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showGoodbyeDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }

    var appSettings by remember {
        mutableStateOf(
            AccessibilitySettings(
                textSize = prefs.getString("access_text_size", "Default") ?: "Default",
                boldText = prefs.getBoolean("access_bold_text", false),
                contrastMode = prefs.getString("access_contrast_mode", "Default") ?: "Default",
                reduceMotion = prefs.getBoolean("access_reduce_motion", false),
                grayscaleMode = prefs.getBoolean("access_grayscale_mode", false),
                largeButtons = prefs.getBoolean("access_large_buttons", false),
                simplifiedCards = prefs.getBoolean("access_simplified_cards", false),
                hideMediaPreview = prefs.getBoolean("access_hide_media_preview", false)
            )
        )
    }

    LaunchedEffect(role, userName) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        FirebaseFirestore.getInstance()
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val loadedNotifications = snapshot.documents.mapNotNull { doc ->
                        try {
                            AppNotification(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                message = doc.getString("message") ?: "",
                                reportId = doc.getString("reportId") ?: "",
                                reportTitle = doc.getString("reportTitle") ?: "",
                                status = doc.getString("status") ?: "",
                                reporter = doc.getString("reporter") ?: "",
                                targetRole = doc.getString("targetRole") ?: "",
                                targetUser = doc.getString("targetUser") ?: "",
                                targetUid = doc.getString("targetUid") ?: "",
                                type = doc.getString("type") ?: "",
                                isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                                read = doc.getBoolean("read") ?: false,
                                timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }.filter { notification ->
                        if (role.equals("Student", ignoreCase = true)) {
                            notification.targetRole.equals("Student", ignoreCase = true) &&
                                    (notification.targetUser.equals(userName, ignoreCase = true) ||
                                            (currentUid.isNotBlank() && notification.targetUid == currentUid))
                        } else {
                            notification.targetRole.equals("Admin", ignoreCase = true) ||
                                    notification.targetRole.equals("Administrator", ignoreCase = true)
                        }
                    }

                    notifications = loadedNotifications
                    unreadNotificationCount = loadedNotifications.count { !it.read }
                }
            }
    }

    val drawerItems = if (isAdmin) {
        listOf(
            DrawerScreen.Profile,
            DrawerScreen.Reports,
            DrawerScreen.Dashboard,
            DrawerScreen.CampusPolls,
            DrawerScreen.Analytics,
            DrawerScreen.Settings
        )
    } else {
        listOf(
            DrawerScreen.Profile,
            DrawerScreen.MyReports,
            DrawerScreen.Dashboard,
            DrawerScreen.CampusPolls,
            DrawerScreen.Settings
        )
    }

    if (showLogoutConfirmDialog) {
        LogoutConfirmDialogCard(
            role = role,
            onYes = {
                showLogoutConfirmDialog = false
                showGoodbyeDialog = true
            },
            onNo = { showLogoutConfirmDialog = false }
        )
    }

    if (showGoodbyeDialog) {
        LaunchedEffect(Unit) {
            delay(1200)
            showGoodbyeDialog = false
            onLogout()
        }
        GoodbyeDialogCard(role = role)
    }

    if (showNotificationsDialog) {
        NotificationsDialogCard(
            notifications = notifications,
            settings = appSettings,
            onDismiss = { showNotificationsDialog = false },
            onMarkAllRead = {
                val db = FirebaseFirestore.getInstance()
                notifications.forEach { notification ->
                    if (!notification.read) {
                        db.collection("notifications")
                            .document(notification.id)
                            .update("read", true)
                    }
                }
            },
            onDeleteNotification = { notification ->
                FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .document(notification.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Notification deleted", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to delete: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            },
            onNotificationClick = { notification ->
                val db = FirebaseFirestore.getInstance()

                db.collection("notifications")
                    .document(notification.id)
                    .update("read", true)
                    .addOnSuccessListener {
                        if (notification.reportId.isBlank()) {
                            Toast.makeText(context, "No linked ticket found.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        db.collection("reports")
                            .document(notification.reportId)
                            .get()
                            .addOnSuccessListener { doc ->
                                if (!doc.exists()) {
                                    Toast.makeText(context, "Ticket not found.", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                val intent = Intent(context, ReportDetailActivity::class.java).apply {
                                    putExtra("role", role)
                                    putExtra("title", doc.getString("title") ?: "")
                                    putExtra("category", doc.getString("category") ?: "")
                                    putExtra("location", doc.getString("location") ?: "")
                                    putExtra("description", doc.getString("description") ?: "")
                                    putExtra("imageUri", doc.getString("imageUrl") ?: "")
                                    putExtra("mediaUrl", doc.getString("mediaUrl") ?: "")
                                    putExtra("mediaType", doc.getString("mediaType") ?: "")
                                    putExtra("status", doc.getString("status") ?: "In Review")
                                    putExtra("dateSubmitted", doc.getString("dateSubmitted") ?: "")
                                    putExtra("reporter", doc.getString("reporter") ?: "")
                                    putExtra("id", doc.id)
                                }
                                showNotificationsDialog = false
                                context.startActivity(intent)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Failed to open ticket: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to mark as read: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                role = role,
                userName = userName,
                studentId = studentId,
                selectedScreen = selectedScreen,
                drawerItems = drawerItems,
                settings = appSettings,
                onItemClick = { screen ->
                    selectedScreen = screen
                    scope.launch { drawerState.close() }
                },
                onLogoutClick = {
                    scope.launch { drawerState.close() }
                    showLogoutConfirmDialog = true
                }
            )
        }
    ) {
        Scaffold(
            floatingActionButton = {
                if (role.equals("Student", ignoreCase = true) &&
                    selectedScreen == DrawerScreen.MyReports
                ) {
                    FloatingActionButton(
                        onClick = {
                            val intent = Intent(context, SubmitReportActivity::class.java).apply {
                                putExtra("reporterName", userName)
                            }
                            context.startActivity(intent)
                        },
                        containerColor = Color(0xFFE1001B),
                        shape = CircleShape
                    ) {
                        Text(
                            "+",
                            color = Color.White,
                            fontSize = if (appSettings.largeButtons) 32.sp else 28.sp
                        )
                    }
                }
            }
        ) { innerPadding ->
            val screenBackground = when (appSettings.contrastMode) {
                "Dark Contrast" -> Color(0xFF121212)
                "Light Contrast" -> Color.White
                "High Contrast" -> Color(0xFFF7F7F7)
                else -> Color(0xFFF5F8F9)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(screenBackground)
            ) {
                TopHeaderWithMenu(
                    role = role,
                    userName = userName,
                    greetingName = greetingName,
                    studentId = studentId,
                    selectedScreen = selectedScreen.title,
                    unreadCount = unreadNotificationCount,
                    settings = appSettings,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNotificationClick = { showNotificationsDialog = true }
                )

                when (selectedScreen) {
                    DrawerScreen.Profile -> ProfileScreen(
                        role = role,
                        userName = userName,
                        greetingName = greetingName,
                        studentId = studentId,
                        settings = appSettings
                    )

                    DrawerScreen.MyReports -> ReportsScreen(
                        onlyMyReports = true,
                        currentUserName = userName,
                        currentRole = role,
                        selectedTab = reportSelectedTab,
                        onTabSelected = { reportSelectedTab = it },
                        settings = appSettings
                    )

                    DrawerScreen.Reports -> ReportsScreen(
                        onlyMyReports = false,
                        currentUserName = userName,
                        currentRole = role,
                        selectedTab = reportSelectedTab,
                        onTabSelected = { reportSelectedTab = it },
                        settings = appSettings
                    )

                    DrawerScreen.Dashboard -> DashboardScreen(
                        role = role,
                        currentUserName = userName,
                        settings = appSettings,
                        onSummaryCardClick = { tabIndex ->
                            reportSelectedTab = tabIndex
                            selectedScreen = if (role.equals("Student", ignoreCase = true)) {
                                DrawerScreen.MyReports
                            } else {
                                DrawerScreen.Reports
                            }
                        }
                    )

                    DrawerScreen.CampusPolls -> CampusPollsScreen(
                        role = role,
                        currentUserName = userName,
                        settings = appSettings
                    )

                    DrawerScreen.Settings -> SettingsScreen(
                        role = role,
                        settings = appSettings,
                        onSettingsChange = { updated -> appSettings = updated }
                    )

                    DrawerScreen.Analytics -> AnalyticsScreen(
                        settings = appSettings
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    role: String,
    userName: String,
    studentId: String,
    selectedScreen: DrawerScreen,
    drawerItems: List<DrawerScreen>,
    settings: AccessibilitySettings,
    onItemClick: (DrawerScreen) -> Unit,
    onLogoutClick: () -> Unit
) {
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    val drawerBg = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color.White
    }

    val profileCardColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF1E1E1E)
        else -> Color(0xFFF8F8F8)
    }

    val textColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        "High Contrast" -> Color.Black
        else -> Color(0xFF222222)
    }

    val subTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        "High Contrast" -> Color(0xFF222222)
        else -> Color.Gray
    }

    val accentColor = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFE1001B)
    val selectedItemColor = if (settings.grayscaleMode) {
        Color(0xFFE0E0E0)
    } else {
        Color(0xFFFFEBEE)
    }

    val headerSize = when (settings.textSize) {
        "Small" -> 18.sp
        "Large" -> 22.sp
        "Extra Large" -> 24.sp
        else -> 20.sp
    }

    val sectionSize = when (settings.textSize) {
        "Small" -> 14.sp
        "Large" -> 18.sp
        "Extra Large" -> 20.sp
        else -> 16.sp
    }

    val itemTextSize = when (settings.textSize) {
        "Small" -> 13.sp
        "Large" -> 17.sp
        "Extra Large" -> 19.sp
        else -> 15.sp
    }

    val smallTextSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 14.sp
        "Extra Large" -> 16.sp
        else -> 12.sp
    }

    val cardShape = RoundedCornerShape(if (settings.simplifiedCards) 10.dp else 16.dp)
    val navShape = RoundedCornerShape(if (settings.simplifiedCards) 10.dp else 20.dp)
    val cardPadding = if (settings.largeButtons) 20.dp else 16.dp
    val itemPadding = if (settings.largeButtons) 10.dp else 4.dp
    val iconSize = if (settings.largeButtons) 26.dp else 22.dp

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = if (settings.grayscaleMode) Color(0xFFF2F2F2) else drawerBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PUP Parañaque Campus",
                fontSize = headerSize,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = "Report Monitoring System",
                fontSize = smallTextSize,
                color = subTextColor
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isAdmin) "Staff Menu" else "Student Menu",
                fontSize = sectionSize,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = cardShape,
                colors = CardDefaults.cardColors(containerColor = profileCardColor),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (settings.reduceMotion) 0.dp else 2.dp
                )
            ) {
                Column(modifier = Modifier.padding(cardPadding)) {
                    Text(
                        text = userName,
                        fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                        fontSize = itemTextSize,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = role,
                        fontSize = smallTextSize,
                        color = subTextColor
                    )

                    if (studentId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = studentId,
                            fontSize = smallTextSize,
                            color = subTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            drawerItems.forEach { item ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            text = item.title,
                            fontSize = itemTextSize,
                            fontWeight = if (selectedScreen == item || settings.boldText) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    selected = selectedScreen == item,
                    onClick = { onItemClick(item) },
                    icon = {
                        Icon(
                            imageVector = when (item) {
                                DrawerScreen.Profile -> Icons.Default.Person
                                DrawerScreen.MyReports -> Icons.Default.Assignment
                                DrawerScreen.Reports -> Icons.AutoMirrored.Filled.MenuBook
                                DrawerScreen.Dashboard -> Icons.Default.Dashboard
                                DrawerScreen.CampusPolls -> Icons.Default.HowToVote
                                DrawerScreen.Analytics -> Icons.Default.Assessment
                                DrawerScreen.Settings -> Icons.Default.Settings
                            },
                            contentDescription = item.title,
                            modifier = Modifier.size(iconSize)
                        )
                    },
                    modifier = Modifier.padding(vertical = itemPadding),
                    shape = navShape,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = selectedItemColor,
                        selectedIconColor = accentColor,
                        selectedTextColor = accentColor,
                        unselectedContainerColor = Color.Transparent,
                        unselectedIconColor = subTextColor,
                        unselectedTextColor = textColor
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                color = if (settings.contrastMode == "Dark Contrast") {
                    Color(0xFF444444)
                } else {
                    Color(0xFFDDDDDD)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                label = {
                    Text(
                        text = "Logout",
                        fontSize = itemTextSize,
                        fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold
                    )
                },
                selected = false,
                onClick = onLogoutClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        modifier = Modifier.size(iconSize)
                    )
                },
                modifier = Modifier.padding(vertical = itemPadding),
                shape = navShape,
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = selectedItemColor,
                    selectedIconColor = accentColor,
                    selectedTextColor = accentColor,
                    unselectedContainerColor = selectedItemColor,
                    unselectedIconColor = accentColor,
                    unselectedTextColor = accentColor
                )
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun TopHeaderWithMenu(
    role: String,
    userName: String,
    greetingName: String,
    studentId: String,
    selectedScreen: String,
    unreadCount: Int,
    settings: AccessibilitySettings,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    val headerColor = when {
        settings.grayscaleMode -> Color(0xFF444444)
        settings.contrastMode == "Dark Contrast" -> Color(0xFF8B0000)
        else -> Color(0xFFE1001B)
    }

    val titleSize = when (settings.textSize) {
        "Small" -> 16.sp
        "Large" -> 20.sp
        "Extra Large" -> 22.sp
        else -> 18.sp
    }

    val subtitleSize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 15.sp
        "Extra Large" -> 16.sp
        else -> 13.sp
    }

    val smallTextSize = when (settings.textSize) {
        "Small" -> 10.sp
        "Large" -> 13.sp
        "Extra Large" -> 14.sp
        else -> 11.sp
    }

    val iconSize = if (settings.largeButtons) 28.dp else 24.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(top = 18.dp, bottom = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = selectedScreen,
                    color = Color.White,
                    fontSize = titleSize,
                    fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold
                )

                if (role.equals("Student", ignoreCase = true)) {
                    Text(
                        text = "Hello, $greetingName",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = subtitleSize
                    )
                } else {
                    Text(
                        text = userName,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = subtitleSize
                    )
                }

                if (studentId.isNotBlank() && role.equals("Student", ignoreCase = true)) {
                    Text(
                        text = studentId,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = smallTextSize
                    )
                }
            }

            IconButton(onClick = onNotificationClick) {
                if (unreadCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color.White,
                                contentColor = headerColor
                            ) {
                                Text(
                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(iconSize)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsDialogCard(
    notifications: List<AppNotification>,
    settings: AccessibilitySettings,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (AppNotification) -> Unit,
    onNotificationClick: (AppNotification) -> Unit
) {
    val dialogBg = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF1E1E1E)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color.White
    }

    val itemReadColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF2A2A2A)
        else -> Color(0xFFF8F8F8)
    }

    val itemUnreadColor = when {
        settings.grayscaleMode -> Color(0xFFEAEAEA)
        settings.contrastMode == "Dark Contrast" -> Color(0xFF33292B)
        else -> Color(0xFFFFF3F4)
    }

    val textColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        else -> Color(0xFF222222)
    }

    val subTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        else -> Color(0xFF555555)
    }

    val faintTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFB0B0B0)
        else -> Color.Gray
    }

    val accentColor = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFE1001B)

    val titleSize = when (settings.textSize) {
        "Small" -> 17.sp
        "Large" -> 22.sp
        "Extra Large" -> 24.sp
        else -> 20.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 15.sp
        "Extra Large" -> 17.sp
        else -> 14.sp
    }

    val smallSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 13.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    val cardShape = RoundedCornerShape(if (settings.simplifiedCards) 12.dp else 20.dp)
    val itemShape = RoundedCornerShape(if (settings.simplifiedCards) 10.dp else 14.dp)
    val contentPadding = if (settings.largeButtons) 22.dp else 18.dp
    val buttonHeight = if (settings.largeButtons) 52.dp else 44.dp
    val iconSize = if (settings.largeButtons) 24.dp else 20.dp

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (settings.reduceMotion) 2.dp else 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = titleSize,
                        fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                        color = accentColor
                    )

                    TextButton(
                        onClick = onMarkAllRead,
                        modifier = Modifier.height(buttonHeight)
                    ) {
                        Text(
                            text = "Mark all read",
                            color = accentColor,
                            fontSize = smallSize,
                            fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (notifications.isEmpty()) {
                    Text(
                        text = "No notifications yet.",
                        color = faintTextColor,
                        fontSize = bodySize,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications) { notification ->
                            val statusColor = when {
                                settings.grayscaleMode -> Color.DarkGray
                                notification.status == "Resolved" -> Color(0xFF2E7D32)
                                notification.status == "In Progress" -> Color(0xFF1976D2)
                                else -> accentColor
                            }

                            Card(
                                shape = itemShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notification.read) itemReadColor else itemUnreadColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNotificationClick(notification) },
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (settings.reduceMotion) 0.dp else 1.dp
                                )
                            ) {
                                Column(modifier = Modifier.padding(if (settings.largeButtons) 16.dp else 14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = notification.title,
                                                    fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                                                    fontSize = bodySize,
                                                    color = textColor
                                                )

                                                if (!notification.read) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(if (settings.largeButtons) 10.dp else 8.dp)
                                                            .background(accentColor, CircleShape)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteNotification(notification) },
                                            modifier = Modifier.size(if (settings.largeButtons) 42.dp else 36.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete notification",
                                                tint = accentColor,
                                                modifier = Modifier.size(iconSize)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = notification.message,
                                        fontSize = bodySize,
                                        color = subTextColor,
                                        lineHeight = (bodySize.value + 5).sp
                                    )

                                    if (notification.reportTitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Report: ${notification.reportTitle}",
                                            fontSize = smallSize,
                                            color = faintTextColor
                                        )
                                    }

                                    if (notification.reporter.isNotBlank()) {
                                        val displayReporter = if (notification.isAnonymous || notification.reporter == "Anonymous Student") {
                                            "Anonymous Student"
                                        } else {
                                            notification.reporter
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Reporter: $displayReporter",
                                                fontSize = smallSize,
                                                color = faintTextColor
                                            )
                                            if (notification.isAnonymous || notification.reporter == "Anonymous Student") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (settings.contrastMode == "Dark Contrast") Color(0xFF2A2A2A) else Color(0xFFEEEEEE),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "Anonymous",
                                                        fontSize = (smallSize.value - 2).sp,
                                                        color = if (settings.contrastMode == "Dark Contrast") Color.LightGray else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = notification.status,
                                        color = statusColor,
                                        fontSize = smallSize,
                                        fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, statusColor, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text(
                        text = "Close",
                        color = Color.White,
                        fontSize = bodySize,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    role: String,
    userName: String,
    greetingName: String,
    studentId: String,
    settings: AccessibilitySettings
) {
    val bgColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val cardColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF1E1E1E)
        else -> Color.White
    }

    val titleColor = when {
        settings.grayscaleMode -> Color(0xFF333333)
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }

    val primaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        "High Contrast" -> Color.Black
        else -> Color(0xFF222222)
    }

    val secondaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        "High Contrast" -> Color(0xFF111111)
        else -> Color.Gray
    }

    val titleSize = when (settings.textSize) {
        "Small" -> 18.sp
        "Large" -> 24.sp
        "Extra Large" -> 26.sp
        else -> 20.sp
    }

    val labelSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 14.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    val valueSize = when (settings.textSize) {
        "Small" -> 14.sp
        "Large" -> 18.sp
        "Extra Large" -> 20.sp
        else -> 16.sp
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(if (settings.simplifiedCards) 12.dp else 18.dp),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (settings.reduceMotion) 0.dp else 4.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(if (settings.largeButtons) 24.dp else 20.dp)
            ) {
                Text(
                    text = "Profile Information",
                    fontSize = titleSize,
                    fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                ProfileField(
                    label = "Role",
                    value = role,
                    labelColor = secondaryTextColor,
                    valueColor = primaryTextColor,
                    labelSize = labelSize,
                    valueSize = valueSize,
                    boldText = settings.boldText
                )

                ProfileField(
                    label = "Full Name",
                    value = userName,
                    labelColor = secondaryTextColor,
                    valueColor = primaryTextColor,
                    labelSize = labelSize,
                    valueSize = valueSize,
                    boldText = settings.boldText
                )

                if (role.equals("Student", ignoreCase = true)) {
                    ProfileField(
                        label = "First Name",
                        value = greetingName,
                        labelColor = secondaryTextColor,
                        valueColor = primaryTextColor,
                        labelSize = labelSize,
                        valueSize = valueSize,
                        boldText = settings.boldText
                    )

                    ProfileField(
                        label = "Student ID",
                        value = if (studentId.isBlank()) "Not available" else studentId,
                        labelColor = secondaryTextColor,
                        valueColor = primaryTextColor,
                        labelSize = labelSize,
                        valueSize = valueSize,
                        boldText = settings.boldText
                    )
                } else {
                    ProfileField(
                        label = "Staff Type",
                        value = "Administrator",
                        labelColor = secondaryTextColor,
                        valueColor = primaryTextColor,
                        labelSize = labelSize,
                        valueSize = valueSize,
                        boldText = settings.boldText
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    labelSize: TextUnit,
    valueSize: TextUnit,
    boldText: Boolean
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = labelSize,
            color = labelColor
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontSize = valueSize,
            fontWeight = if (boldText) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun DashboardScreen(
    role: String,
    currentUserName: String,
    settings: AccessibilitySettings,
    onSummaryCardClick: (Int) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    var allReports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Report(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            category = doc.getString("category") ?: "",
                            location = doc.getString("location") ?: "",
                            description = doc.getString("description") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            mediaUrl = doc.getString("mediaUrl") ?: "",
                            mediaType = doc.getString("mediaType") ?: "",
                            status = doc.getString("status") ?: "In Review",
                            dateSubmitted = doc.getString("dateSubmitted") ?: "",
                            reporter = doc.getString("reporter") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0,
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt(),
                            isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                            reporterUid = doc.getString("reporterUid") ?: "",
                            reporterStudentId = doc.getString("reporterStudentId") ?: ""
                        )
                    } catch (_: Exception) {
                        null
                    }
                }

                allReports = if (role.equals("Student", ignoreCase = true)) {
                    list.filter {
                        it.reporter == currentUserName ||
                                (currentUid.isNotBlank() && it.reporterUid == currentUid)
                    }
                } else {
                    list
                }
                isLoading = false
            }
    }

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val titleColor = when {
        settings.grayscaleMode -> Color(0xFF333333)
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }

    val primaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        "High Contrast" -> Color.Black
        else -> Color(0xFF222222)
    }

    val secondaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        "High Contrast" -> Color(0xFF111111)
        else -> Color.Gray
    }

    val headingSize = when (settings.textSize) {
        "Small" -> 18.sp
        "Large" -> 24.sp
        "Extra Large" -> 26.sp
        else -> 22.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 16.sp
        "Extra Large" -> 18.sp
        else -> 14.sp
    }

    val totalCount = allReports.size
    val pendingCount = allReports.count { it.status == "In Review" }
    val inProgressCount = allReports.count { it.status == "In Progress" }
    val resolvedCount = allReports.count { it.status == "Resolved" }

    val totalRatingsSum = allReports.sumOf { it.averageRating * it.ratingCount }
    val totalRatingsCount = allReports.sumOf { it.ratingCount }
    val overallAverageRating = if (totalRatingsCount > 0) {
        totalRatingsSum / totalRatingsCount
    } else {
        0.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = if (role.equals("Student", ignoreCase = true)) {
                "My Report Summary"
            } else {
                "All Reports Summary"
            },
            fontSize = headingSize,
            fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
            color = titleColor
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (role.equals("Student", ignoreCase = true)) {
                "Tap a card to view your filtered reports"
            } else {
                "Tap a card to view filtered campus reports"
            },
            fontSize = bodySize,
            color = secondaryTextColor
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = titleColor)
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "Total Reports",
                        value = totalCount.toString(),
                        accentColor = if (settings.grayscaleMode) Color.DarkGray else Color(0xFFE1001B),
                        settings = settings,
                        textColor = primaryTextColor,
                        subTextColor = secondaryTextColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(0) }
                    )

                    SummaryCard(
                        title = "In Review",
                        value = pendingCount.toString(),
                        accentColor = if (settings.grayscaleMode) Color.Gray else Color(0xFFFF9800),
                        settings = settings,
                        textColor = primaryTextColor,
                        subTextColor = secondaryTextColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(1) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SummaryCard(
                        title = "In Progress",
                        value = inProgressCount.toString(),
                        accentColor = if (settings.grayscaleMode) Color.Gray else Color(0xFF1976D2),
                        settings = settings,
                        textColor = primaryTextColor,
                        subTextColor = secondaryTextColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(2) }
                    )

                    SummaryCard(
                        title = "Resolved",
                        value = resolvedCount.toString(),
                        accentColor = if (settings.grayscaleMode) Color.DarkGray else Color(0xFF2E7D32),
                        settings = settings,
                        textColor = primaryTextColor,
                        subTextColor = secondaryTextColor,
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(3) }
                    )
                }

                if (isAdmin) {
                    RatingSummaryCard(
                        title = "Overall Rating",
                        ratingText = if (totalRatingsCount > 0) {
                            String.format("%.1f", overallAverageRating)
                        } else {
                            "No ratings"
                        },
                        subtitle = if (totalRatingsCount > 0) {
                            "$totalRatingsCount total ratings"
                        } else {
                            "No student ratings yet"
                        },
                        accentColor = if (settings.grayscaleMode) Color.Gray else Color(0xFFFFC107),
                        settings = settings,
                        textColor = primaryTextColor,
                        subTextColor = secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    accentColor: Color,
    settings: AccessibilitySettings,
    textColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White

    val valueSize = when (settings.textSize) {
        "Small" -> 22.sp
        "Large" -> 32.sp
        "Extra Large" -> 34.sp
        else -> 28.sp
    }

    val titleSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 15.sp
        "Extra Large" -> 16.sp
        else -> 13.sp
    }

    Card(
        modifier = modifier
            .height(if (settings.largeButtons) 156.dp else 140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(if (settings.simplifiedCards) 10.dp else 18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (settings.reduceMotion) 0.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (settings.largeButtons) 18.dp else 16.dp,
                    vertical = if (settings.largeButtons) 22.dp else 20.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(
                        width = if (settings.largeButtons) 48.dp else 42.dp,
                        height = 6.dp
                    )
                    .background(accentColor, RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = value,
                fontSize = valueSize,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = titleSize,
                color = subTextColor,
                lineHeight = (titleSize.value + 3).sp
            )
        }
    }
}

@Composable
fun RatingSummaryCard(
    title: String,
    ratingText: String,
    subtitle: String,
    accentColor: Color,
    settings: AccessibilitySettings,
    textColor: Color,
    subTextColor: Color
) {
    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White

    val titleSize = when (settings.textSize) {
        "Small" -> 13.sp
        "Large" -> 17.sp
        "Extra Large" -> 18.sp
        else -> 15.sp
    }

    val ratingSize = when (settings.textSize) {
        "Small" -> 22.sp
        "Large" -> 32.sp
        "Extra Large" -> 34.sp
        else -> 28.sp
    }

    val subtitleSize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 15.sp
        "Extra Large" -> 16.sp
        else -> 13.sp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (settings.largeButtons) 156.dp else 145.dp),
        shape = RoundedCornerShape(if (settings.simplifiedCards) 10.dp else 18.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (settings.reduceMotion) 0.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (settings.largeButtons) 18.dp else 16.dp,
                    vertical = if (settings.largeButtons) 22.dp else 20.dp
                ),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = "Rating",
                    tint = accentColor
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = title,
                    fontSize = titleSize,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = ratingText,
                fontSize = ratingSize,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = subtitleSize,
                color = subTextColor
            )
        }
    }
}

@Composable
fun SettingsScreen(
    role: String,
    settings: AccessibilitySettings,
    onSettingsChange: (AccessibilitySettings) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }

    fun saveSettings(updated: AccessibilitySettings) {
        prefs.edit()
            .putString("access_text_size", updated.textSize)
            .putBoolean("access_bold_text", updated.boldText)
            .putString("access_contrast_mode", updated.contrastMode)
            .putBoolean("access_reduce_motion", updated.reduceMotion)
            .putBoolean("access_grayscale_mode", updated.grayscaleMode)
            .putBoolean("access_large_buttons", updated.largeButtons)
            .putBoolean("access_simplified_cards", updated.simplifiedCards)
            .putBoolean("access_hide_media_preview", updated.hideMediaPreview)
            .apply()

        onSettingsChange(updated)
    }

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White
    val titleColor = when {
        settings.grayscaleMode -> Color(0xFF333333)
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }
    val primaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        "High Contrast" -> Color.Black
        else -> Color(0xFF222222)
    }
    val secondaryTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        "High Contrast" -> Color(0xFF111111)
        else -> Color(0xFF555555)
    }

    val selectedTextSp = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 18.sp
        "Extra Large" -> 20.sp
        else -> 14.sp
    }

    val headingSp = when (settings.textSize) {
        "Small" -> 18.sp
        "Large" -> 24.sp
        "Extra Large" -> 26.sp
        else -> 20.sp
    }

    val sectionTitleSp = when (settings.textSize) {
        "Small" -> 16.sp
        "Large" -> 20.sp
        "Extra Large" -> 22.sp
        else -> 18.sp
    }

    val cardPadding = if (settings.largeButtons) 24.dp else 20.dp
    val itemVerticalPadding = if (settings.largeButtons) 18.dp else 12.dp
    val switchScale = if (settings.largeButtons) 1.15f else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (settings.simplifiedCards) 12.dp else 18.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (settings.reduceMotion) 1.dp else 4.dp
                    )
                ) {
                    Column(modifier = Modifier.padding(cardPadding)) {
                        Text(
                            text = "Accessibility Settings",
                            fontSize = headingSp,
                            fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                            color = titleColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = if (role.equals("Student", ignoreCase = true)) {
                                "Customize readability, visibility, and comfort for using PUP Alert."
                            } else {
                                "Adjust accessibility, readability, and viewing preferences for admin use."
                            },
                            fontSize = selectedTextSp,
                            lineHeight = (selectedTextSp.value + 6).sp,
                            fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal,
                            color = secondaryTextColor
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        SettingsSectionTitle(
                            title = "Text Display",
                            color = titleColor,
                            fontSize = sectionTitleSp,
                            boldText = settings.boldText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsDropdownRow(
                            title = "Text Size",
                            value = settings.textSize,
                            options = listOf("Small", "Default", "Large", "Extra Large"),
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldText = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            onValueSelected = {
                                saveSettings(settings.copy(textSize = it))
                            }
                        )

                        SettingsSwitchRow(
                            title = "Bold Text",
                            subtitle = "Make labels and content easier to read.",
                            checked = settings.boldText,
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldWeight = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            switchScale = switchScale,
                            onCheckedChange = {
                                saveSettings(settings.copy(boldText = it))
                            }
                        )

                        SettingsDropdownRow(
                            title = "Contrast Mode",
                            value = settings.contrastMode,
                            options = listOf("Default", "High Contrast", "Dark Contrast", "Light Contrast"),
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldText = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            onValueSelected = {
                                saveSettings(settings.copy(contrastMode = it))
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SettingsSectionTitle(
                            title = "Comfort Options",
                            color = titleColor,
                            fontSize = sectionTitleSp,
                            boldText = settings.boldText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsSwitchRow(
                            title = "Reduce Motion",
                            subtitle = "Minimize motion effects and visual transitions.",
                            checked = settings.reduceMotion,
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldWeight = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            switchScale = switchScale,
                            onCheckedChange = {
                                saveSettings(settings.copy(reduceMotion = it))
                            }
                        )

                        SettingsSwitchRow(
                            title = "Grayscale Mode",
                            subtitle = "Reduce strong colors for a calmer display.",
                            checked = settings.grayscaleMode,
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldWeight = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            switchScale = switchScale,
                            onCheckedChange = {
                                saveSettings(settings.copy(grayscaleMode = it))
                            }
                        )

                        SettingsSwitchRow(
                            title = "Large Buttons",
                            subtitle = "Increase touch comfort for controls and actions.",
                            checked = settings.largeButtons,
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldWeight = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            switchScale = switchScale,
                            onCheckedChange = {
                                saveSettings(settings.copy(largeButtons = it))
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SettingsSectionTitle(
                            title = "PUP Alert View Options",
                            color = titleColor,
                            fontSize = sectionTitleSp,
                            boldText = settings.boldText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingsSwitchRow(
                            title = "Simplified Cards",
                            subtitle = "Use a cleaner card style for reports and dashboard items.",
                            checked = settings.simplifiedCards,
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldWeight = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            switchScale = switchScale,
                            onCheckedChange = {
                                saveSettings(settings.copy(simplifiedCards = it))
                            }
                        )

                        SettingsSwitchRow(
                            title = "Hide Media Preview",
                            subtitle = "Hide images and media previews while browsing reports.",
                            checked = settings.hideMediaPreview,
                            textColor = primaryTextColor,
                            subTextColor = secondaryTextColor,
                            titleFontSize = selectedTextSp,
                            boldWeight = settings.boldText,
                            itemPadding = itemVerticalPadding,
                            switchScale = switchScale,
                            onCheckedChange = {
                                saveSettings(settings.copy(hideMediaPreview = it))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionTitle(
    title: String,
    color: Color,
    fontSize: TextUnit,
    boldText: Boolean
) {
    Text(
        text = title,
        fontSize = fontSize,
        fontWeight = if (boldText) FontWeight.ExtraBold else FontWeight.Bold,
        color = color
    )
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    textColor: Color,
    subTextColor: Color,
    titleFontSize: TextUnit,
    boldWeight: Boolean,
    itemPadding: Dp,
    switchScale: Float,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = itemPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = titleFontSize,
                fontWeight = if (boldWeight) FontWeight.Bold else FontWeight.Medium,
                color = textColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = (titleFontSize.value - 1).sp,
                lineHeight = (titleFontSize.value + 4).sp,
                color = subTextColor
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.scale(switchScale)
        )
    }
}

@Composable
fun SettingsDropdownRow(
    title: String,
    value: String,
    options: List<String>,
    textColor: Color,
    subTextColor: Color,
    titleFontSize: TextUnit,
    boldText: Boolean,
    itemPadding: Dp,
    onValueSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = itemPadding)
    ) {
        Text(
            text = title,
            fontSize = titleFontSize,
            fontWeight = if (boldText) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = titleFontSize
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Open options",
                        tint = subTextColor
                    )
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable { expanded = true }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = textColor
                            )
                        },
                        onClick = {
                            onValueSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(
    onlyMyReports: Boolean,
    currentUserName: String,
    currentRole: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    settings: AccessibilitySettings
) {
    val tabs = listOf("All", "In Review", "In Progress", "Resolved")
    var selectedCategory by remember { mutableStateOf("All Categories") }
    val currentStatus = tabs[selectedTab]

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        FiltersSection(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it },
            settings = settings
        )

        StatusTabs(
            selectedTab = selectedTab,
            tabs = tabs,
            onTabSelected = onTabSelected,
            settings = settings
        )

        ReportList(
            selectedStatus = currentStatus,
            selectedCategory = selectedCategory,
            currentRole = currentRole,
            onlyMyReports = onlyMyReports,
            currentUserName = currentUserName,
            settings = settings
        )
    }
}

@Composable
fun LogoutConfirmDialogCard(
    role: String,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Dialog(onDismissRequest = onNo) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (role.equals("Student", ignoreCase = true)) "👋" else "🔒",
                    fontSize = 34.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (role.equals("Student", ignoreCase = true)) "Are you sure?" else "Logout now?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (role.equals("Student", ignoreCase = true)) {
                        "Are you sure you want to log out?"
                    } else {
                        "Do you want to end this admin session?"
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onNo,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = Color(0xFFF3F3F3),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "No",
                            color = Color(0xFF444444),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = onYes,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = Color(0xFFE1001B),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = "Yes",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GoodbyeDialogCard(role: String) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (role.equals("Student", ignoreCase = true)) "👋" else "✅",
                    fontSize = 34.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (role.equals("Student", ignoreCase = true)) {
                        "Goodbye, see you again"
                    } else {
                        "See you again"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE1001B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Logging out...",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun FiltersSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    settings: AccessibilitySettings
) {
    val categoryOptions = listOf(
        "All Categories",
        "Facilities",
        "Maintenance",
        "Safety",
        "Cleanliness",
        "Equipment",
        "Other"
    )

    var categoryExpanded by remember { mutableStateOf(false) }

    val bgColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val cardColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF1E1E1E)
        else -> Color.White
    }

    val textColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        "High Contrast" -> Color.Black
        else -> Color(0xFF222222)
    }

    val accentColor = when {
        settings.grayscaleMode -> Color.DarkGray
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }

    val textSize = when (settings.textSize) {
        "Small" -> 13.sp
        "Large" -> 17.sp
        "Extra Large" -> 18.sp
        else -> 14.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Filter by category",
            fontSize = textSize,
            fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium,
            color = accentColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = textSize
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Category Dropdown",
                        tint = accentColor
                    )
                },
                shape = RoundedCornerShape(if (settings.simplifiedCards) 10.dp else 12.dp),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable { categoryExpanded = true }
            )

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                modifier = Modifier.background(cardColor)
            ) {
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    color = textColor,
                                    fontSize = textSize
                                )
                                if (selectedCategory == option) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onCategorySelected(option)
                            categoryExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StatusTabs(
    selectedTab: Int,
    tabs: List<String>,
    onTabSelected: (Int) -> Unit,
    settings: AccessibilitySettings
) {
    val bgColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val selectedColor = when {
        settings.grayscaleMode -> Color.DarkGray
        settings.contrastMode == "Dark Contrast" -> Color(0xFFFF6B6B)
        else -> Color(0xFFE1001B)
    }

    val unselectedColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        "High Contrast" -> Color.Black
        else -> Color(0xFF616161)
    }

    val textSize = when (settings.textSize) {
        "Small" -> 13.sp
        "Large" -> 17.sp
        "Extra Large" -> 18.sp
        else -> 14.sp
    }

    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 16.dp,
        containerColor = bgColor,
        contentColor = selectedColor,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = if (settings.largeButtons) 4.dp else 3.dp,
                color = selectedColor
            )
        }
    ) {
        tabs.forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = textSize,
                        fontWeight = if (selectedTab == index) {
                            if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold
                        } else {
                            FontWeight.Normal
                        },
                        color = if (selectedTab == index) selectedColor else unselectedColor
                    )
                }
            )
        }
    }
}

@Composable
fun ReportList(
    selectedStatus: String,
    selectedCategory: String,
    currentRole: String,
    onlyMyReports: Boolean = false,
    currentUserName: String,
    settings: AccessibilitySettings
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var allReports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Report(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            category = doc.getString("category") ?: "",
                            location = doc.getString("location") ?: "",
                            description = doc.getString("description") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            mediaUrl = doc.getString("mediaUrl") ?: "",
                            mediaType = doc.getString("mediaType") ?: "",
                            status = doc.getString("status") ?: "In Review",
                            dateSubmitted = doc.getString("dateSubmitted") ?: "",
                            reporter = doc.getString("reporter") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0,
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt(),
                            isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                            reporterUid = doc.getString("reporterUid") ?: "",
                            reporterStudentId = doc.getString("reporterStudentId") ?: ""
                        )
                    } catch (_: Exception) {
                        null
                    }
                }

                allReports = list.sortedByDescending { it.timestamp }
                ReportRepository.reports.clear()
                ReportRepository.reports.addAll(list)
                isLoading = false
            }
    }

    val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val filteredReports = allReports.filter { report ->
        val statusMatch = if (selectedStatus == "All") true else report.status == selectedStatus
        val categoryMatch = if (selectedCategory == "All Categories") true else report.category == selectedCategory
        val userMatch = if (onlyMyReports) {
            report.reporter == currentUserName ||
                    (currentUid.isNotBlank() && report.reporterUid == currentUid)
        } else true
        statusMatch && categoryMatch && userMatch
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE1001B))
            }
        }

        filteredReports.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reports found.",
                    color = Color.Gray,
                    fontSize = when (settings.textSize) {
                        "Small" -> 13.sp
                        "Large" -> 17.sp
                        "Extra Large" -> 19.sp
                        else -> 15.sp
                    }
                )
            }
        }

        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReports) { report ->
                    ReportItem(
                        report = report,
                        settings = settings
                    ) {
                        val intent = Intent(context, ReportDetailActivity::class.java).apply {
                            putExtra("role", currentRole)
                            putExtra("title", report.title)
                            putExtra("category", report.category)
                            putExtra("location", report.location)
                            putExtra("description", report.description)
                            putExtra("imageUri", report.imageUrl ?: "")
                            putExtra("mediaUrl", report.mediaUrl)
                            putExtra("mediaType", report.mediaType)
                            putExtra("status", report.status)
                            putExtra("dateSubmitted", report.dateSubmitted)
                            putExtra("reporter", report.reporter)
                            putExtra("isAnonymous", report.isAnonymous)
                            putExtra("reporterUid", report.reporterUid)
                            putExtra("id", report.id)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItem(
    report: Report,
    settings: AccessibilitySettings,
    onClick: () -> Unit
) {
    val statusColor = when (report.status) {
        "Resolved" -> Color(0xFF17B169)
        "In Progress" -> Color(0xFFFFC107)
        else -> Color(0xFFE1001B)
    }

    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White
    val textColor = if (settings.contrastMode == "Dark Contrast") Color.White else Color.Black
    val subTextColor = if (settings.contrastMode == "Dark Contrast") Color(0xFFD0D0D0) else Color.Gray

    val titleSize = when (settings.textSize) {
        "Small" -> 13.sp
        "Large" -> 17.sp
        "Extra Large" -> 19.sp
        else -> 15.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 11.sp
        "Large" -> 14.sp
        "Extra Large" -> 16.sp
        else -> 12.sp
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(if (settings.simplifiedCards) 8.dp else 12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (settings.reduceMotion) 0.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(if (settings.largeButtons) 16.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!settings.hideMediaPreview) {
                when {
                    report.mediaType == "image" && report.mediaUrl.isNotBlank() -> {
                        Image(
                            painter = rememberAsyncImagePainter(report.mediaUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(if (settings.largeButtons) 72.dp else 60.dp)
                                .background(Color.Gray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    report.mediaType == "video" && report.mediaUrl.isNotBlank() -> {
                        Box(
                            modifier = Modifier
                                .size(if (settings.largeButtons) 72.dp else 60.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Video",
                                tint = Color(0xFFE1001B)
                            )
                        }
                    }

                    !report.imageUrl.isNullOrEmpty() -> {
                        Image(
                            painter = rememberAsyncImagePainter(report.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(if (settings.largeButtons) 72.dp else 60.dp)
                                .background(Color.Gray, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .size(if (settings.largeButtons) 72.dp else 60.dp)
                                .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                    fontSize = titleSize,
                    color = textColor,
                    maxLines = 1
                )

                Text(
                    text = report.category,
                    fontSize = bodySize,
                    color = subTextColor
                )

                if (report.isAnonymous || report.reporter == "Anonymous Student") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (settings.contrastMode == "Dark Contrast") Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Anonymous",
                            fontSize = (bodySize.value - 3).sp,
                            color = if (settings.contrastMode == "Dark Contrast") Color(0xFFD0D0D0) else Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = subTextColor
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = report.location,
                        fontSize = bodySize,
                        color = subTextColor,
                        maxLines = 1
                    )
                }

                if (report.ratingCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", report.averageRating),
                            fontSize = bodySize,
                            color = textColor,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${report.ratingCount})",
                            fontSize = (bodySize.value - 1).sp,
                            color = subTextColor
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = report.status,
                    color = statusColor,
                    fontSize = bodySize,
                    fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = report.dateSubmitted,
                    fontSize = (bodySize.value - 1).sp,
                    color = subTextColor
                )
            }
        }
    }
}

data class SurveyData(
    val id: String = "",
    val reportId: String = "",
    val studentName: String = "",
    val rating: Int = 5,
    val resolutionSpeed: String = "",
    val qualityOfWork: String = "",
    val communicationClarity: String = "",
    val suggestions: String = ""
)

@Composable
fun AnalyticsScreen(
    settings: AccessibilitySettings
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()

    var allReports by remember { mutableStateOf<List<Report>>(emptyList()) }
    var surveysList by remember { mutableStateOf<List<SurveyData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collectionGroup("surveys")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            SurveyData(
                                id = doc.id,
                                reportId = doc.getString("reportId") ?: "",
                                studentName = doc.getString("studentName") ?: "Student",
                                rating = (doc.getLong("rating") ?: 5L).toInt(),
                                resolutionSpeed = doc.getString("resolutionSpeed") ?: "",
                                qualityOfWork = doc.getString("qualityOfWork") ?: "",
                                communicationClarity = doc.getString("communicationClarity") ?: "",
                                suggestions = doc.getString("suggestions") ?: ""
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    surveysList = list
                }
            }

        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    isLoading = false
                    return@addSnapshotListener
                }

                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Report(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            category = doc.getString("category") ?: "",
                            location = doc.getString("location") ?: "",
                            description = doc.getString("description") ?: "",
                            imageUrl = doc.getString("imageUrl"),
                            mediaUrl = doc.getString("mediaUrl") ?: "",
                            mediaType = doc.getString("mediaType") ?: "",
                            status = doc.getString("status") ?: "In Review",
                            dateSubmitted = doc.getString("dateSubmitted") ?: "",
                            reporter = doc.getString("reporter") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0,
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt(),
                            isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                            reporterUid = doc.getString("reporterUid") ?: "",
                            reporterStudentId = doc.getString("reporterStudentId") ?: ""
                        )
                    } catch (_: Exception) {
                        null
                    }
                }

                allReports = list
                isLoading = false
            }
    }

    val total = allReports.size
    val inReview = allReports.count { it.status == "In Review" }
    val inProgress = allReports.count { it.status == "In Progress" }
    val resolved = allReports.count { it.status == "Resolved" }
    val resolutionRate = if (total > 0) (resolved.toDouble() / total * 100) else 0.0

    val totalSurveys = surveysList.size
    val completelySatisfiedCount = surveysList.count {
        it.qualityOfWork.contains("completely", ignoreCase = true) || it.qualityOfWork.contains("yes", ignoreCase = true)
    }
    val partiallySatisfiedCount = surveysList.count {
        it.qualityOfWork.contains("partially", ignoreCase = true)
    }
    val dissatisfiedCount = surveysList.count {
        it.qualityOfWork.contains("no", ignoreCase = true) || it.qualityOfWork.contains("needs work", ignoreCase = true)
    }

    val completelyPct = if (totalSurveys > 0) (completelySatisfiedCount.toFloat() / totalSurveys) else 0f
    val partiallyPct = if (totalSurveys > 0) (partiallySatisfiedCount.toFloat() / totalSurveys) else 0f
    val dissatisfiedPct = if (totalSurveys > 0) (dissatisfiedCount.toFloat() / totalSurveys) else 0f

    val categoryCounts = allReports.groupBy { it.category }.mapValues { it.value.size }
    val locationRiskList = allReports.filter { it.status != "Resolved" }
        .groupBy { it.location }
        .mapValues { it.value.size }
        .entries
        .sortedByDescending { it.value }

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White
    val textColor = if (settings.contrastMode == "Dark Contrast") Color.White else Color(0xFF222222)
    val subTextColor = if (settings.contrastMode == "Dark Contrast") Color(0xFFD0D0D0) else Color.Gray
    val accentColor = if (settings.grayscaleMode) Color(0xFF444444) else Color(0xFFE1001B)

    val titleSize = when (settings.textSize) {
        "Small" -> 16.sp
        "Large" -> 20.sp
        "Extra Large" -> 22.sp
        else -> 18.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 15.sp
        "Extra Large" -> 17.sp
        else -> 14.sp
    }

    val smallSize = when (settings.textSize) {
        "Small" -> 10.sp
        "Large" -> 13.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Campus Analytics & Intelligence",
                            fontSize = titleSize,
                            fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Automated executive summary & exporting",
                            fontSize = smallSize,
                            color = subTextColor
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            AnalyticsPrintHelper.downloadPdfReport(context, allReports, surveysList, settings)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print / Download PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download PDF", fontSize = smallSize, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            AnalyticsExcelHelper.downloadExcel(context, allReports, surveysList)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Download Excel",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Excel", fontSize = smallSize, fontWeight = FontWeight.Bold)
                    }
                }

                // Executive Summary Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "Executive Summary",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "📊 Executive Summary",
                                fontSize = bodySize,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val topCat = categoryCounts.maxByOrNull { it.value }?.key ?: "General"
                        val topLoc = locationRiskList.firstOrNull()?.key ?: "Campus-wide"

                        val summaryText = if (total == 0) {
                            "No report records currently available for analysis."
                        } else {
                            "Out of $total total filed reports, $resolved issues have been resolved (${String.format(Locale.getDefault(), "%.1f", resolutionRate)}% resolution rate). Category '$topCat' represents the highest incident volume (${categoryCounts.getOrDefault(topCat, 0)} reports). Priority location requiring maintenance dispatch: '$topLoc'."
                        }

                        Text(
                            text = summaryText,
                            fontSize = bodySize,
                            color = textColor,
                            lineHeight = (bodySize.value + 5).sp
                        )
                    }
                }

                // Student Survey Satisfaction Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "📋 Student Resolution Survey Feedback",
                                fontSize = bodySize,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1976D2), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$totalSurveys Answered",
                                    fontSize = smallSize,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (totalSurveys == 0) {
                            Text(
                                text = "No survey responses recorded yet. Surveys automatically prompt students once their report is marked as Resolved.",
                                fontSize = smallSize,
                                color = subTextColor
                            )
                        } else {
                            // Completely Satisfied
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🟢 Completely Satisfied", fontSize = smallSize, color = textColor, fontWeight = FontWeight.Medium)
                                    Text("$completelySatisfiedCount (${String.format(Locale.getDefault(), "%.0f", completelyPct * 100)}%)", fontSize = smallSize, color = subTextColor)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { completelyPct },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFF2E7D32),
                                    trackColor = Color(0xFFEEEEEE)
                                )
                            }

                            // Partially Satisfied
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🟡 Partially Satisfied", fontSize = smallSize, color = textColor, fontWeight = FontWeight.Medium)
                                    Text("$partiallySatisfiedCount (${String.format(Locale.getDefault(), "%.0f", partiallyPct * 100)}%)", fontSize = smallSize, color = subTextColor)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { partiallyPct },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFFF57C00),
                                    trackColor = Color(0xFFEEEEEE)
                                )
                            }

                            // Dissatisfied / Needs Work
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("🔴 Not Satisfied / Needs Work", fontSize = smallSize, color = textColor, fontWeight = FontWeight.Medium)
                                    Text("$dissatisfiedCount (${String.format(Locale.getDefault(), "%.0f", dissatisfiedPct * 100)}%)", fontSize = smallSize, color = subTextColor)
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { dissatisfiedPct },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFFD32F2F),
                                    trackColor = Color(0xFFEEEEEE)
                                )
                            }

                            // Student Suggestions
                            val suggestionsList = surveysList.mapNotNull { if (it.suggestions.isNotBlank()) it.suggestions else null }
                            if (suggestionsList.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "💡 Recent Student Suggestions:",
                                    fontSize = smallSize,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                suggestionsList.take(3).forEach { suggestion ->
                                    Text(
                                        text = "• \"$suggestion\"",
                                        fontSize = smallSize,
                                        color = subTextColor,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Key Performance Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$total", fontSize = titleSize, fontWeight = FontWeight.Bold, color = accentColor)
                            Text("Total Filed", fontSize = smallSize, color = subTextColor)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("${String.format(Locale.getDefault(), "%.0f", resolutionRate)}%", fontSize = titleSize, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Text("Resolution Rate", fontSize = smallSize, color = subTextColor)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("$inReview", fontSize = titleSize, fontWeight = FontWeight.Bold, color = Color(0xFFE1001B))
                            Text("Unaddressed", fontSize = smallSize, color = subTextColor)
                        }
                    }
                }

                // Category Distribution Breakdown
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Category Distribution",
                            fontSize = bodySize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (categoryCounts.isEmpty()) {
                            Text("No category data available.", fontSize = smallSize, color = subTextColor)
                        } else {
                            categoryCounts.forEach { (cat, count) ->
                                val pct = if (total > 0) count.toFloat() / total else 0f
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(cat, fontSize = smallSize, color = textColor, fontWeight = FontWeight.Medium)
                                        Text("$count (${String.format(Locale.getDefault(), "%.0f", pct * 100)}%)", fontSize = smallSize, color = subTextColor)
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp),
                                        color = accentColor,
                                        trackColor = Color(0xFFEEEEEE)
                                    )
                                }
                            }
                        }
                    }
                }

                // Location Hotspot Risk Scores
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Campus Hotspots & Risk Score",
                            fontSize = bodySize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (locationRiskList.isEmpty()) {
                            Text("No unresolved location risks found.", fontSize = smallSize, color = Color(0xFF2E7D32))
                        } else {
                            locationRiskList.take(5).forEach { (loc, activeCount) ->
                                val riskLevel = when {
                                    activeCount >= 5 -> "CRITICAL"
                                    activeCount >= 3 -> "HIGH"
                                    else -> "MODERATE"
                                }
                                val badgeColor = when (riskLevel) {
                                    "CRITICAL" -> Color(0xFFD32F2F)
                                    "HIGH" -> Color(0xFFF57C00)
                                    else -> Color(0xFF1976D2)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(loc, fontSize = bodySize, fontWeight = FontWeight.SemiBold, color = textColor)
                                        Text("$activeCount active unresolved issues", fontSize = smallSize, color = subTextColor)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(badgeColor, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(riskLevel, fontSize = (smallSize.value - 2).sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

data class ChatMessage(
    val id: String = "",
    val channelId: String = "",
    val senderName: String = "",
    val senderUid: String = "",
    val senderRole: String = "",
    val text: String = "",
    val isAnonymous: Boolean = false,
    val timestamp: Long = 0L,
    val formattedTime: String = ""
)

@Composable
fun CommunityChatScreen(
    role: String,
    currentUserName: String,
    settings: AccessibilitySettings
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    val channels = listOf(
        "lost_found" to "🔍 Lost & Found",
        "general" to "📢 General Talk",
        "facilities" to "🏫 Facilities",
        "academics" to "📚 Study Groups",
        "welfare" to "💡 Student Welfare"
    )

    var selectedChannel by remember { mutableStateOf("lost_found") }
    var messages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var isAnonymousPost by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedChannel) {
        isLoading = true
        db.collection("community_messages")
            .whereEqualTo("channelId", selectedChannel)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            val timeSec = doc.getTimestamp("timestamp")?.seconds ?: 0L
                            val dateStr = if (timeSec > 0) {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timeSec * 1000))
                            } else ""

                            ChatMessage(
                                id = doc.id,
                                channelId = doc.getString("channelId") ?: "",
                                senderName = doc.getString("senderName") ?: "Student",
                                senderUid = doc.getString("senderUid") ?: "",
                                senderRole = doc.getString("senderRole") ?: "Student",
                                text = doc.getString("text") ?: "",
                                isAnonymous = doc.getBoolean("isAnonymous") ?: false,
                                timestamp = timeSec,
                                formattedTime = dateStr
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    messages = list
                }
                isLoading = false
            }
    }

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White
    val textColor = if (settings.contrastMode == "Dark Contrast") Color.White else Color(0xFF222222)
    val subTextColor = if (settings.contrastMode == "Dark Contrast") Color(0xFFD0D0D0) else Color.Gray
    val accentColor = if (settings.grayscaleMode) Color(0xFF444444) else Color(0xFFE1001B)

    val bodySize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 15.sp
        "Extra Large" -> 17.sp
        else -> 14.sp
    }

    val smallSize = when (settings.textSize) {
        "Small" -> 10.sp
        "Large" -> 13.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(12.dp)
    ) {
        // Channel Selector Horizontal Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            items(channels) { (channelKey, channelLabel) ->
                val isSelected = selectedChannel == channelKey
                Button(
                    onClick = { selectedChannel = channelKey },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) accentColor else cardColor
                    ),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = channelLabel,
                        color = if (isSelected) Color.White else textColor,
                        fontSize = smallSize,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Chat Messages Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accentColor)
                }
            } else if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages yet in this community channel.\nBe the first to post!",
                        color = subTextColor,
                        fontSize = bodySize,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages) { msg ->
                        val isMyMessage = currentUser != null && msg.senderUid == currentUser.uid
                        val displaySender = if (msg.isAnonymous) "Anonymous Student" else msg.senderName

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isMyMessage) {
                                        if (settings.grayscaleMode) Color.DarkGray else Color(0xFFE1001B)
                                    } else {
                                        cardColor
                                    }
                                ),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = displaySender,
                                            fontSize = smallSize,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMyMessage) Color.White else accentColor
                                        )

                                        if (msg.senderRole.equals("Admin", ignoreCase = true) || msg.senderRole.equals("Administrator", ignoreCase = true)) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("ADMIN", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = msg.text,
                                        fontSize = bodySize,
                                        color = if (isMyMessage) Color.White else textColor
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = msg.formattedTime,
                                            fontSize = (smallSize.value - 2).sp,
                                            color = if (isMyMessage) Color.White.copy(alpha = 0.8f) else subTextColor
                                        )

                                        if (role.equals("Administrator", ignoreCase = true) || role.equals("Admin", ignoreCase = true) || isMyMessage) {
                                            IconButton(
                                                onClick = {
                                                    db.collection("community_messages").document(msg.id).delete()
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Message",
                                                    tint = if (isMyMessage) Color.White else Color.Gray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Anonymous Toggle Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Post anonymously in this channel",
                fontSize = smallSize,
                color = subTextColor
            )
            Switch(
                checked = isAnonymousPost,
                onCheckedChange = { isAnonymousPost = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = accentColor
                )
            )
        }

        // Bottom Message Input Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Type a community message...", fontSize = bodySize) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFFDADDE2),
                    focusedContainerColor = cardColor,
                    unfocusedContainerColor = cardColor,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                textStyle = TextStyle(fontSize = bodySize)
            )

            IconButton(
                onClick = {
                    if (messageText.isNotBlank() && currentUser != null) {
                        val textToSend = messageText.trim()
                        messageText = ""

                        val msgData = hashMapOf(
                            "channelId" to selectedChannel,
                            "senderName" to if (isAnonymousPost) "Anonymous Student" else currentUserName,
                            "senderUid" to currentUser.uid,
                            "senderRole" to role,
                            "text" to textToSend,
                            "isAnonymous" to isAnonymousPost,
                            "timestamp" to Timestamp.now()
                        )

                        db.collection("community_messages").add(msgData)
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(accentColor, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Message",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun CampusPollsScreen(
    role: String,
    currentUserName: String,
    settings: AccessibilitySettings
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    var polls by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showCreatePollDialog by remember { mutableStateOf(false) }

    val isAdmin = role.equals("Administrator", ignoreCase = true) || role.equals("Admin", ignoreCase = true)

    LaunchedEffect(Unit) {
        db.collection("campus_polls")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        data
                    }
                    polls = list
                }
                isLoading = false
            }
    }

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF5F8F9)
    }

    val cardColor = if (settings.contrastMode == "Dark Contrast") Color(0xFF1E1E1E) else Color.White
    val textColor = if (settings.contrastMode == "Dark Contrast") Color.White else Color(0xFF222222)
    val subTextColor = if (settings.contrastMode == "Dark Contrast") Color(0xFFD0D0D0) else Color.Gray
    val accentColor = if (settings.grayscaleMode) Color(0xFF444444) else Color(0xFFE1001B)

    val titleSize = when (settings.textSize) {
        "Small" -> 16.sp
        "Large" -> 20.sp
        "Extra Large" -> 22.sp
        else -> 18.sp
    }

    val bodySize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 15.sp
        "Extra Large" -> 17.sp
        else -> 14.sp
    }

    val smallSize = when (settings.textSize) {
        "Small" -> 10.sp
        "Large" -> 13.sp
        "Extra Large" -> 15.sp
        else -> 12.sp
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accentColor)
            }
        } else {
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Student Pulse Polls",
                            fontSize = titleSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Vote on campus topics & welfare initiatives",
                            fontSize = smallSize,
                            color = subTextColor
                        )
                    }

                    if (isAdmin) {
                        Button(
                            onClick = { showCreatePollDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                        ) {
                            Text("+ Poll", fontSize = smallSize, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (polls.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No active student polls available.",
                            color = subTextColor,
                            fontSize = bodySize
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(polls) { pollData ->
                            val pollId = pollData["id"] as? String ?: ""
                            val question = pollData["question"] as? String ?: ""
                            val rawOptions = pollData["options"] as? List<Map<String, Any>> ?: emptyList()
                            val votedUids = pollData["votedUids"] as? List<String> ?: emptyList()

                            val myUid = currentUser?.uid ?: ""
                            val hasVoted = votedUids.contains(myUid)

                            val totalVotes = rawOptions.sumOf { (it["votes"] as? Long ?: 0L).toInt() }

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = cardColor),
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "📊 $question",
                                        fontSize = bodySize,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    rawOptions.forEachIndexed { optIndex, optionMap ->
                                        val optText = optionMap["text"] as? String ?: ""
                                        val optVotes = (optionMap["votes"] as? Long ?: 0L).toInt()
                                        val pct = if (totalVotes > 0) optVotes.toFloat() / totalVotes else 0f

                                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                            OutlinedButton(
                                                onClick = {
                                                    if (!hasVoted && myUid.isNotBlank()) {
                                                        db.runTransaction { transaction ->
                                                            val pollRef = db.collection("campus_polls").document(pollId)
                                                            val snapshot = transaction.get(pollRef)

                                                            val currentVoted = snapshot.get("votedUids") as? List<String> ?: emptyList()
                                                            if (!currentVoted.contains(myUid)) {
                                                                val currentOpts = snapshot.get("options") as? List<Map<String, Any>> ?: emptyList()
                                                                val updatedOpts = currentOpts.mapIndexed { idx, opt ->
                                                                    val mutableOpt = opt.toMutableMap()
                                                                    if (idx == optIndex) {
                                                                        val currentVal = (mutableOpt["votes"] as? Long ?: 0L).toInt()
                                                                        mutableOpt["votes"] = currentVal + 1
                                                                    }
                                                                    mutableOpt
                                                                }

                                                                val updatedVoted = currentVoted + myUid
                                                                transaction.update(pollRef, "options", updatedOpts)
                                                                transaction.update(pollRef, "votedUids", updatedVoted)
                                                            }
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    containerColor = if (hasVoted) Color(0xFFF0F0F0) else Color.Transparent
                                                ),
                                                border = BorderStroke(1.dp, if (hasVoted) Color(0xFFDDDDDD) else accentColor)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(optText, fontSize = bodySize, color = textColor)
                                                    if (hasVoted) {
                                                        Text("$optVotes votes (${String.format(Locale.getDefault(), "%.0f", pct * 100)}%)", fontSize = smallSize, color = subTextColor)
                                                    }
                                                }
                                            }

                                            if (hasVoted) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                LinearProgressIndicator(
                                                    progress = { pct },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(4.dp),
                                                    color = accentColor,
                                                    trackColor = Color(0xFFEEEEEE)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "$totalVotes total votes",
                                            fontSize = smallSize,
                                            color = subTextColor
                                        )

                                        if (hasVoted) {
                                            Text(
                                                text = "✓ Voted",
                                                fontSize = smallSize,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreatePollDialog) {
            var newQuestion by remember { mutableStateOf("") }
            var option1 by remember { mutableStateOf("") }
            var option2 by remember { mutableStateOf("") }
            var option3 by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreatePollDialog = false },
                containerColor = cardColor,
                title = { Text("Create Campus Poll", color = textColor, fontSize = bodySize, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newQuestion,
                            onValueChange = { newQuestion = it },
                            label = { Text("Poll Question") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = option1,
                            onValueChange = { option1 = it },
                            label = { Text("Option 1") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = option2,
                            onValueChange = { option2 = it },
                            label = { Text("Option 2") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = option3,
                            onValueChange = { option3 = it },
                            label = { Text("Option 3 (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newQuestion.isNotBlank() && option1.isNotBlank() && option2.isNotBlank()) {
                                showCreatePollDialog = false

                                val opts = mutableListOf(
                                    mapOf("text" to option1.trim(), "votes" to 0),
                                    mapOf("text" to option2.trim(), "votes" to 0)
                                )
                                if (option3.isNotBlank()) {
                                    opts.add(mapOf("text" to option3.trim(), "votes" to 0))
                                }

                                val pollData = hashMapOf(
                                    "question" to newQuestion.trim(),
                                    "options" to opts,
                                    "votedUids" to emptyList<String>(),
                                    "createdBy" to currentUserName,
                                    "timestamp" to Timestamp.now()
                                )

                                db.collection("campus_polls").add(pollData)
                            }
                        }
                    ) {
                        Text("Create", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePollDialog = false }) {
                        Text("Cancel", color = subTextColor)
                    }
                }
            )
        }
    }
}