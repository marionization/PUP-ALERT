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
                            onClick = { /* TODO: Add action */ },
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
                        // Pass role to the header!
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Search reports...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            singleLine = true
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip("All Categories")
            FilterChip("All Status")
        }
    }
}

@Composable
fun FilterChip(text: String) {
    Surface(
        shape = CircleShape,
        color = Color(0xFFF3F3F3)
    ) {
        Text(
            text = text,
            color = Color.Black,
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 8.dp),
            fontSize = 14.sp
        )
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
