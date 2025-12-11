package Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import Activity.ui.theme.SeriousModeTheme
import com.example.seriousmode.MainActivity


class NextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val role = intent.getStringExtra("role") ?: "Administrator"

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Student") ?: "Student"
        val studentId = prefs.getString("student_id", "") ?: ""

        setContent {
            SeriousModeTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("All", "Pending", "In Progress", "Resolved")
                var selectedCategory by remember { mutableStateOf("All Categories") }
                val currentStatus = tabs[selectedTab]

                Scaffold(
                    floatingActionButton = {
                        if (role != "Administrator") {
                            FloatingActionButton(
                                onClick = {
                                    val intent = Intent(
                                        this@NextActivity,
                                        SubmitReportActivity::class.java
                                    )
                                    startActivity(intent)
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
                        TopHeader(
                            role = role,
                            userName = if (role == "Student") userName else "",
                            studentId = if (role == "Student") studentId else "",
                            onLogout = {
                                val intent = Intent(
                                    this@NextActivity,
                                    MainActivity::class.java
                                )
                                startActivity(intent)
                                finish()
                            }
                        )
                        FiltersSection(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it }
                        )
                        StatusTabs(selectedTab, tabs) { selectedTab = it }
                        ReportList(
                            selectedStatus = currentStatus,
                            selectedCategory = selectedCategory,
                            currentRole = role
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(
    role: String = "Administrator",
    userName: String = "",
    studentId: String = "",
    onLogout: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE1001B))
            .padding(vertical = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Person, contentDescription = role, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text(role, color = Color.White, fontSize = 15.sp)
                }
                if (userName.isNotBlank()) {
                    Text(userName, color = Color.White, fontSize = 14.sp)
                }
                if (studentId.isNotBlank()) {
                    Text(
                        studentId,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onLogout() }
            ) {
                Icon(Icons.Filled.Logout, contentDescription = "Logout", tint = Color.White)
                Spacer(Modifier.width(4.dp))
                Text("Logout", color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(22.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "PUP Parañaque Campus",
                color = Color.White,
                fontSize = 17.sp,
            )
            Text(
                "Report & Monitoring System",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 13.sp,
            )
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
                modifier = Modifier
                    .widthIn(min = 160.dp, max = 200.dp),
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
    currentRole: String
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
                            status = doc.getString("status") ?: "Pending",
                            dateSubmitted = doc.getString("dateSubmitted") ?: "",
                            reporter = doc.getString("reporter") ?: "",
                            timestamp = doc.getTimestamp("timestamp")?.seconds ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                allReports = list
                ReportRepository.reports.clear()
                ReportRepository.reports.addAll(list)
                isLoading = false
            }
    }

    // Filter reports based on selected tab and category
    val filteredReports = allReports.filter { report ->
        val statusMatch = if (selectedStatus == "All") true else report.status == selectedStatus
        val categoryMatch = if (selectedCategory == "All Categories") true else report.category == selectedCategory
        statusMatch && categoryMatch
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFE1001B))
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
                        putExtra("imageUri", report.imageUrl)
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
            // Placeholder or image
            if (!report.imageUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(report.imageUrl),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.Gray)
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
