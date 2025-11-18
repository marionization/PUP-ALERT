package Activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import Activity.ui.theme.SeriousModeTheme
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset

class NextActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val role = intent.getStringExtra("role") ?: "Administrator"
        setContent {
            SeriousModeTheme {
                Scaffold(
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                val intent = Intent(this@NextActivity, SubmitReportActivity::class.java)
                                startActivity(intent)
                            },
                            containerColor = Color(0xFFE1001B),
                            shape = CircleShape
                        ) {
                            Text("+", color = Color.White, fontSize = 28.sp)
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
                            onLogout = {
                                val intent = Intent(this@NextActivity, com.example.seriousmode.MainActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                        )
                        FiltersSection()
                        StatusTabs()
                        // Add the rest of your content here
                    }
                }
            }
        }
    }
}

@Composable
fun TopHeader(role: String = "Administrator", onLogout: () -> Unit = {}) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Person, contentDescription = role, tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(role, color = Color.White, fontSize = 15.sp)
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
fun FiltersSection() {
    val categoryOptions = listOf("All Categories", "Facilities", "Maintenance", "Safety", "Cleanliness", "Equipment", "Other")
    var selectedCategory by remember { mutableStateOf(categoryOptions.first()) }
    var categoryExpanded by remember { mutableStateOf(false) }

    val statusOptions = listOf("All Status", "Pending", "In Progress", "Resolved")
    var selectedStatus by remember { mutableStateOf(statusOptions.first()) }
    var statusExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category Dropdown (compact)
        Box {
            OutlinedTextField(
                value = selectedCategory,
                onValueChange = {},
                modifier = Modifier
                    .widthIn(min = 150.dp, max = 180.dp)
                    .clickable { categoryExpanded = true },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Category Dropdown",
                        tint = Color.Gray
                    )
                },
                singleLine = true
            )
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                categoryOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedCategory = option
                            categoryExpanded = false
                        }
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { categoryExpanded = true }
                    .background(Color.Transparent)
            )
        }
        // Status Dropdown (compact)
        Box {
            OutlinedTextField(
                value = selectedStatus,
                onValueChange = {},
                modifier = Modifier
                    .widthIn(min = 120.dp, max = 150.dp)
                    .clickable { statusExpanded = true },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Status Dropdown",
                        tint = Color.Gray
                    )
                },
                singleLine = true
            )
            DropdownMenu(
                expanded = statusExpanded,
                onDismissRequest = { statusExpanded = false }
            ) {
                statusOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedStatus = option
                            statusExpanded = false
                        }
                    )
                }
            }
            Spacer(
                modifier = Modifier
                    .matchParentSize()
                    .clickable { statusExpanded = true }
                    .background(Color.Transparent)
            )
        }
    }
}

@Composable
fun StatusTabs() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Pending", "In Progress", "Resolved")
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
                onClick = { selectedTab = i },
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
