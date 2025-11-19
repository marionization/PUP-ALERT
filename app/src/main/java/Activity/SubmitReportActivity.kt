package Activity

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import Activity.ui.theme.SeriousModeTheme

class SubmitReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeriousModeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF9F9FB)
                ) {
                    SubmitReportScreen(
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun SubmitReportScreen(
    onCancel: () -> Unit
) {
    var reportTitle by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    val categories = listOf(
        "Facilities",
        "Maintenance",
        "Safety",
        "Cleanliness",
        "Equipment",
        "Other"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            "Submit New Report",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Text("Report Title *", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = reportTitle,
            onValueChange = { reportTitle = it },
            placeholder = { Text("e.g., Broken classroom door") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        Text("Category *", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .widthIn(min = 150.dp, max = 180.dp)
                .padding(bottom = 14.dp)
        ) {
            OutlinedTextField(
                value = if (selectedCategory.isEmpty()) "" else selectedCategory,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { categoryExpanded = true },
                readOnly = true,
                enabled = false,
                placeholder = { Text("Select a category", color = Color.Gray) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Black,
                    unfocusedBorderColor = Color(0xFFDADDE2),
                    disabledBorderColor = Color(0xFFDADDE2),
                    disabledTrailingIconColor = Color(0xFF8A8A8A)
                ),
                singleLine = true
            )
            DropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                categories.forEach { option ->
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

        // ---- Upload Image Section with Undo Button ----
        Text("Upload Image", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(125.dp)
                .border(1.dp, Color(0xFFDADDE2), RoundedCornerShape(8.dp))
                .background(Color.White, RoundedCornerShape(8.dp))
                .clickable(enabled = imageUri == null) { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                Box(Modifier.fillMaxSize()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .background(Color(0xFFE1001B), RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove selected image",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFF929292))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Click to upload image",
                        fontSize = 14.sp,
                        color = Color(0xFF929292)
                    )
                }
            }
        }
        // ---- end upload image section ----

        Spacer(modifier = Modifier.height(14.dp))

        Text("Location *", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            placeholder = { Text("e.g., Building A, Room 301") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        )

        Text("Description *", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Describe the issue in detail...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .padding(bottom = 14.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp, brush = SolidColor(Color(0xFFDADDE2))
                ),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
            ) {
                Text("Cancel", color = Color.Black)
            }
            Button(
                onClick = {
                    ReportRepository.reports.add(
                        Report(
                            reportTitle,
                            selectedCategory,
                            location,
                            description,
                            imageUri
                        )
                    )
                    onCancel()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1001B))
            ) {
                Text("Submit Report", color = Color.White)
            }
        }
    }
}
