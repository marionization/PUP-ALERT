package Activity

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import Activity.ui.theme.SeriousModeTheme
import com.example.seriousmode.MainActivity
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
            SeriousModeTheme {
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
}

enum class DrawerScreen(val title: String) {
    Profile("Profile"),
    MyReports("My Reports"),
    Reports("Reports"),
    Dashboard("Dashboard"),
    Settings("Settings")
}

data class AppNotification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val reportTitle: String = "",
    val status: String = "",
    val reporter: String = "",
    val targetRole: String = "",
    val targetUser: String = "",
    val type: String = "",
    val read: Boolean = false,
    val timestamp: Long = 0L
)


@Composable
fun WelcomeLoadingScreen(role: String, firstName: String = "Student") {
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    val welcomeText = if (isAdmin) {
        "HELLO ADMIN!"
    } else {
        "Hello, $firstName"
    }

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
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    var selectedScreen by remember { mutableStateOf(DrawerScreen.Dashboard) }
    var reportSelectedTab by remember { mutableIntStateOf(0) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showGoodbyeDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var unreadNotificationCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(role, userName) {
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
                                reportTitle = doc.getString("reportTitle") ?: "",
                                status = doc.getString("status") ?: "",
                                reporter = doc.getString("reporter") ?: "",
                                targetRole = doc.getString("targetRole") ?: "",
                                targetUser = doc.getString("targetUser") ?: "",
                                type = doc.getString("type") ?: "",
                                read = doc.getBoolean("read") ?: false,
                                timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0L
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }.filter { notification ->
                        if (role.equals("Student", ignoreCase = true)) {
                            notification.targetRole.equals("Student", ignoreCase = true) &&
                                    notification.targetUser.equals(userName, ignoreCase = true)
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
            DrawerScreen.Settings
        )
    } else {
        listOf(
            DrawerScreen.Profile,
            DrawerScreen.MyReports,
            DrawerScreen.Dashboard,
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
            onNo = {
                showLogoutConfirmDialog = false
            }
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
                        Toast.makeText(
                            context,
                            "Failed to delete: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
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
                        Text("+", color = Color.White, fontSize = 28.sp)
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF5F8F9))
            ) {
                TopHeaderWithMenu(
                    role = role,
                    userName = userName,
                    greetingName = greetingName,
                    studentId = studentId,
                    selectedScreen = selectedScreen.title,
                    unreadCount = unreadNotificationCount,
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onNotificationClick = {
                        showNotificationsDialog = true
                    }
                )

                when (selectedScreen) {
                    DrawerScreen.Profile -> ProfileScreen(
                        role = role,
                        userName = userName,
                        greetingName = greetingName,
                        studentId = studentId
                    )

                    DrawerScreen.MyReports -> ReportsScreen(
                        onlyMyReports = true,
                        currentUserName = userName,
                        currentRole = role,
                        selectedTab = reportSelectedTab,
                        onTabSelected = { reportSelectedTab = it }
                    )

                    DrawerScreen.Reports -> ReportsScreen(
                        onlyMyReports = false,
                        currentUserName = userName,
                        currentRole = role,
                        selectedTab = reportSelectedTab,
                        onTabSelected = { reportSelectedTab = it }
                    )

                    DrawerScreen.Dashboard -> DashboardScreen(
                        role = role,
                        currentUserName = userName,
                        onSummaryCardClick = { tabIndex ->
                            reportSelectedTab = tabIndex
                            selectedScreen = if (role.equals("Student", ignoreCase = true)) {
                                DrawerScreen.MyReports
                            } else {
                                DrawerScreen.Reports
                            }
                        }
                    )

                    DrawerScreen.Settings -> SettingsScreen(role = role)
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
    onItemClick: (DrawerScreen) -> Unit,
    onLogoutClick: () -> Unit
) {
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerContainerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PUP Parañaque Campus",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE1001B)
            )

            Text(
                text = "Report Monitoring System",
                fontSize = 13.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isAdmin) "Staff Menu" else "Student Menu",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE1001B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF222222)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = role,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    if (studentId.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = studentId,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            drawerItems.forEach { item ->
                NavigationDrawerItem(
                    label = {
                        Text(
                            item.title,
                            fontWeight = if (selectedScreen == item) FontWeight.Bold else FontWeight.Normal
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
                                DrawerScreen.Settings -> Icons.Default.Settings
                            },
                            contentDescription = item.title
                        )
                    },
                    modifier = Modifier.padding(vertical = 4.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFFFEBEE),
                        selectedIconColor = Color(0xFFE1001B),
                        selectedTextColor = Color(0xFFE1001B)
                    )
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            HorizontalDivider(color = Color(0xFFDDDDDD))
            Spacer(modifier = Modifier.height(8.dp))

            NavigationDrawerItem(
                label = {
                    Text(
                        "Logout",
                        fontWeight = FontWeight.Bold
                    )
                },
                selected = false,
                onClick = onLogoutClick,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Logout",
                        tint = Color(0xFFE1001B)
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp),
                colors = NavigationDrawerItemDefaults.colors(
                    unselectedContainerColor = Color(0xFFFFEBEE),
                    unselectedIconColor = Color(0xFFE1001B),
                    unselectedTextColor = Color(0xFFE1001B)
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
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE1001B))
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
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = selectedScreen,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (role.equals("Student", ignoreCase = true)) {
                    Text(
                        text = "Hello, $greetingName",
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = userName,
                        color = Color.White.copy(alpha = 0.95f),
                        fontSize = 13.sp
                    )
                }

                if (studentId.isNotBlank() && role.equals("Student", ignoreCase = true)) {
                    Text(
                        text = studentId,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(onClick = onNotificationClick) {
                if (unreadCount > 0) {
                    BadgedBox(
                        badge = {
                            Badge(
                                containerColor = Color.White,
                                contentColor = Color(0xFFE1001B)
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
                            tint = Color.White
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationsDialogCard(
    notifications: List<AppNotification>,
    onDismiss: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDeleteNotification: (AppNotification) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE1001B)
                    )

                    TextButton(onClick = onMarkAllRead) {
                        Text(
                            text = "Mark all read",
                            color = Color(0xFFE1001B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (notifications.isEmpty()) {
                    Text(
                        text = "No notifications yet.",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications) { notification ->
                            val accentColor = when (notification.status) {
                                "Resolved" -> Color(0xFF2E7D32)
                                "In Progress" -> Color(0xFF1976D2)
                                else -> Color(0xFFE1001B)
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notification.read) Color(0xFFF8F8F8) else Color(0xFFFFF3F4)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = notification.title,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    color = Color(0xFF222222)
                                                )

                                                if (!notification.read) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .background(Color(0xFFE1001B), CircleShape)
                                                    )
                                                }
                                            }
                                        }

                                        IconButton(
                                            onClick = { onDeleteNotification(notification) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete notification",
                                                tint = Color(0xFFE1001B)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = notification.message,
                                        fontSize = 13.sp,
                                        color = Color(0xFF555555),
                                        lineHeight = 18.sp
                                    )

                                    if (notification.reportTitle.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Report: ${notification.reportTitle}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = notification.status,
                                        color = accentColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .border(1.dp, accentColor, RoundedCornerShape(6.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1001B))
                ) {
                    Text("Close")
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
    studentId: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Profile Information",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE1001B)
                )

                Spacer(modifier = Modifier.height(18.dp))

                ProfileField("Role", role)
                ProfileField("Full Name", userName)

                if (role.equals("Student", ignoreCase = true)) {
                    ProfileField("First Name", greetingName)
                    ProfileField("Student ID", if (studentId.isBlank()) "Not available" else studentId)
                } else {
                    ProfileField("Staff Type", "Administrator")
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF222222)
        )
    }
}

@Composable
fun DashboardScreen(
    role: String,
    currentUserName: String,
    onSummaryCardClick: (Int) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val isAdmin = role.equals("Administrator", ignoreCase = true) ||
            role.equals("Admin", ignoreCase = true)

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
                            status = doc.getString("status") ?: "Pending",
                            dateSubmitted = doc.getString("dateSubmitted") ?: "",
                            reporter = doc.getString("reporter") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0,
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                allReports = if (role.equals("Student", ignoreCase = true)) {
                    list.filter { it.reporter == currentUserName }
                } else {
                    list
                }
                isLoading = false
            }
    }

    val totalCount = allReports.size
    val pendingCount = allReports.count { it.status == "Pending" }
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
            .background(Color(0xFFF5F8F9))
            .padding(16.dp)
    ) {
        Text(
            text = if (role.equals("Student", ignoreCase = true)) "My Report Summary" else "All Reports Summary",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE1001B)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (role.equals("Student", ignoreCase = true)) {
                "Tap a card to view your filtered reports"
            } else {
                "Tap a card to view filtered campus reports"
            },
            fontSize = 14.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE1001B))
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
                        accentColor = Color(0xFFE1001B),
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(0) }
                    )
                    SummaryCard(
                        title = "Pending",
                        value = pendingCount.toString(),
                        accentColor = Color(0xFFFF9800),
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
                        accentColor = Color(0xFF1976D2),
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(2) }
                    )
                    SummaryCard(
                        title = "Resolved",
                        value = resolvedCount.toString(),
                        accentColor = Color(0xFF2E7D32),
                        modifier = Modifier.weight(1f),
                        onClick = { onSummaryCardClick(3) }
                    )
                }

                if (isAdmin) {
                    RatingSummaryCard(
                        title = "Overall Rating",
                        ratingText = if (totalRatingsCount > 0) String.format("%.1f", overallAverageRating) else "No ratings",
                        subtitle = if (totalRatingsCount > 0) "$totalRatingsCount total ratings" else "No student ratings yet",
                        accentColor = Color(0xFFFFC107)
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 42.dp, height = 6.dp)
                    .background(accentColor, RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                color = Color.Gray,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun RatingSummaryCard(
    title: String,
    ratingText: String,
    subtitle: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF444444)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (ratingText == "No ratings") ratingText else "$ratingText ★",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun SettingsScreen(role: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE1001B)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (role.equals("Student", ignoreCase = true)) {
                        "This section can be used later for profile editing, password updates, notification preferences, and account options."
                    } else {
                        "This section can be used later for staff preferences, admin tools, notification options, and account settings."
                    },
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    lineHeight = 20.sp
                )
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
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("All", "Pending", "In Progress", "Resolved")
    var selectedCategory by remember { mutableStateOf("All Categories") }
    val currentStatus = tabs[selectedTab]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F8F9))
    ) {
        FiltersSection(
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        StatusTabs(selectedTab, tabs) { onTabSelected(it) }

        ReportList(
            selectedStatus = currentStatus,
            selectedCategory = selectedCategory,
            currentRole = currentRole,
            onlyMyReports = onlyMyReports,
            currentUserName = currentUserName
        )
    }
}

@Composable
fun LogoutConfirmDialogCard(
    role: String,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Dialog(onDismissRequest = { onNo() }) {
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
                    text = if (role.equals("Student", ignoreCase = true)) "😢" else "👋",
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
                    text = if (role.equals("Student", ignoreCase = true)) "🥺" else "✅",
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
    onCategorySelected: (String) -> Unit
) {
    val categoryOptions = listOf(
        "All Categories", "Facilities", "Maintenance",
        "Safety", "Cleanliness", "Equipment", "Other"
    )
    var categoryExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.FilterList,
            contentDescription = "Filter",
            modifier = Modifier.size(24.dp),
            tint = Color(0xFFE1001B)
        )

        Spacer(Modifier.width(8.dp))

        Box {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                modifier = Modifier.widthIn(min = 160.dp, max = 200.dp),
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Category Dropdown",
                        tint = Color.Gray
                    )
                },
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Box(
                Modifier
                    .matchParentSize()
                    .background(Color.Transparent)
                    .clickable { categoryExpanded = true }
            )

            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(option)
                                if (selectedCategory == option) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFE1001B),
                                        modifier = Modifier.size(19.dp)
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
fun StatusTabs(selectedTab: Int, tabs: List<String>, onTabSelected: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        edgePadding = 0.dp,
        containerColor = Color.Transparent,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                height = 3.dp,
                color = Color(0xFFE1001B)
            )
        }
    ) {
        tabs.forEachIndexed { i, title ->
            Tab(
                selected = selectedTab == i,
                onClick = { onTabSelected(i) },
                text = {
                    Text(
                        title,
                        color = if (selectedTab == i) Color(0xFFE1001B) else Color(0xFF616161),
                        fontSize = 15.sp
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
    currentUserName: String = ""
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
                            status = doc.getString("status") ?: "Pending",
                            dateSubmitted = doc.getString("dateSubmitted") ?: "",
                            reporter = doc.getString("reporter") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0,
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            ratingCount = (doc.getLong("ratingCount") ?: 0L).toInt()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                allReports = list.sortedByDescending { it.timestamp }
                ReportRepository.reports.clear()
                ReportRepository.reports.addAll(list)
                isLoading = false
            }
    }

    val filteredReports = allReports.filter { report ->
        val statusMatch = if (selectedStatus == "All") true else report.status == selectedStatus
        val categoryMatch = if (selectedCategory == "All Categories") true else report.category == selectedCategory
        val userMatch = if (onlyMyReports) report.reporter == currentUserName else true
        statusMatch && categoryMatch && userMatch
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFE1001B))
        }
    } else if (filteredReports.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No reports found.",
                color = Color.Gray,
                fontSize = 15.sp
            )
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredReports) { report ->
                ReportItem(report) {
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
                        putExtra("id", report.id)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }
}

@Composable
fun ReportItem(report: Report, onClick: () -> Unit) {
    val statusColor = when (report.status) {
        "Resolved" -> Color(0xFF17B169)
        "In Progress" -> Color(0xFFFFC107)
        else -> Color(0xFFE1001B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                report.mediaType == "image" && report.mediaUrl.isNotBlank() -> {
                    Image(
                        painter = rememberAsyncImagePainter(report.mediaUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.Gray, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                report.mediaType == "video" && report.mediaUrl.isNotBlank() -> {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
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
                            .size(60.dp)
                            .background(Color.Gray, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )

                Text(
                    text = report.category,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = report.location,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                if (report.ratingCount > 0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", report.averageRating),
                            fontSize = 11.sp,
                            color = Color(0xFF444444),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "(${report.ratingCount})",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = report.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = report.dateSubmitted,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}