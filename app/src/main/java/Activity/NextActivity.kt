package Activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import Activity.ui.theme.SeriousModeTheme
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale

class NextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val role = intent.getStringExtra("role") ?: "Administrator"

        // Load name and student number saved from RegisterActivity
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = prefs.getString("user_name", "Student") ?: "Student"
        val studentId = prefs.getString("student_id", "") ?: ""

        setContent {
            SeriousModeTheme {
                var selectedTab by remember { mutableStateOf(0) }
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
                            userName = userName,
                            studentId = studentId,
                            onLogout = {
                                val intent = Intent(
                                    this@NextActivity,
                                    com.example.seriousmode.MainActivity::class.java
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
    val allReports = ReportRepository.reports
    val filteredReports = allReports.filter { report ->
        (selectedStatus == "All" || report.status == selectedStatus) &&
                (selectedCategory == "All Categories" || report.category == selectedCategory)
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(filteredReports) { report ->
            val statusColor = when (report.status) {
                "Resolved" -> Color(0xFF17B169)
                "In Progress" -> Color(0xFFFFC107)
                else -> Color(0xFFE1001B)
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        val intent =
                            Intent(context, ReportDetailActivity::class.java).apply {
                                putExtra("role", currentRole)
                                putExtra("title", report.title)
                                putExtra("category", report.category)
                                putExtra("location", report.location)
                                putExtra("description", report.description)
                                putExtra("imageUri", report.imageUri?.toString())
                                putExtra("status", report.status)
                                putExtra("dateSubmitted", report.dateSubmitted)
                                putExtra("reporter", report.reporter)
                            }
                        context.startActivity(intent)
                    },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.fillMaxWidth()) {
                    if (report.imageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(report.imageUri),
                            contentDescription = "Report Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = Color.LightGray
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                report.title,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                report.status,
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .background(statusColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            report.category,
                            color = Color(0xFFE1001B),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .background(Color(0xFFFDECEC), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = "Location",
                                modifier = Modifier.size(15.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                report.location,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Spacer(Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = "Date",
                                modifier = Modifier.size(15.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                report.dateSubmitted,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Reporter",
                                modifier = Modifier.size(15.dp),
                                tint = Color.Gray
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                report.reporter,
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}
