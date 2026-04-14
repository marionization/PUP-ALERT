package Activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storageMetadata
import Activity.ui.theme.SeriousModeTheme
import java.text.SimpleDateFormat
import java.util.*

class SubmitReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val reporterName = intent.getStringExtra("reporterName") ?: "Student"

        setContent {
            SeriousModeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFF9F9FB)
                ) {
                    SubmitReportScreen(
                        onCancel = { finish() },
                        reporterName = reporterName
                    )
                }
            }
        }
    }
}

@Composable
fun SubmitReportScreen(
    onCancel: () -> Unit,
    reporterName: String
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var reportTitle by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("") }
    var showMediaError by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedMediaUri = uri
            val mimeType = context.contentResolver.getType(uri) ?: ""

            selectedMediaType = when {
                mimeType.startsWith("video/") -> "video"
                mimeType.startsWith("image/") -> "image"
                else -> ""
            }

            if (selectedMediaType.isEmpty()) {
                Toast.makeText(context, "Unsupported media type", Toast.LENGTH_SHORT).show()
                selectedMediaUri = null
            } else {
                showMediaError = false
            }
        }
    }

    val scrollState = rememberScrollState()

    val categories = listOf(
        "Facilities",
        "Maintenance",
        "Safety",
        "Cleanliness",
        "Equipment",
        "Other"
    )

    val isFormValid =
        reportTitle.isNotBlank() &&
                selectedCategory.isNotBlank() &&
                location.isNotBlank() &&
                description.isNotBlank() &&
                selectedMediaUri != null &&
                selectedMediaType.isNotBlank()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                    modifier = Modifier.fillMaxWidth(),
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

            Text("Reporter", fontSize = 14.sp, modifier = Modifier.padding(bottom = 4.dp))
            OutlinedTextField(
                value = reporterName,
                onValueChange = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

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

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "Upload Photo or Video *",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(
                        1.dp,
                        if (showMediaError) Color(0xFFE1001B) else Color(0xFFDADDE2),
                        RoundedCornerShape(8.dp)
                    )
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .clickable(enabled = selectedMediaUri == null && !isLoading) {
                        mediaPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedMediaUri != null) {
                    Box(Modifier.fillMaxSize()) {
                        if (selectedMediaType == "image") {
                            Image(
                                painter = rememberAsyncImagePainter(model = selectedMediaUri),
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White, RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFF7F7F7)),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = "Selected Video",
                                    tint = Color(0xFFE1001B),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Video selected",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF333333)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Ready to upload with your report",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedMediaUri = null
                                selectedMediaType = ""
                                showMediaError = false
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(Color(0xFFE1001B), RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove selected media",
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = null,
                            tint = if (showMediaError) Color(0xFFE1001B) else Color(0xFF929292)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Click to upload photo or video",
                            fontSize = 14.sp,
                            color = if (showMediaError) Color(0xFFE1001B) else Color(0xFF929292)
                        )
                    }
                }
            }

            if (showMediaError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Photo or video evidence is required.",
                    color = Color(0xFFE1001B),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = SolidColor(Color(0xFFDADDE2))
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    enabled = !isLoading
                ) {
                    Text("Cancel", color = Color.Black)
                }

                Button(
                    onClick = {
                        if (reportTitle.isBlank() ||
                            selectedCategory.isBlank() ||
                            location.isBlank() ||
                            description.isBlank()
                        ) {
                            Toast.makeText(
                                context,
                                "Please fill in all required fields",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        if (selectedMediaUri == null || selectedMediaType.isBlank()) {
                            showMediaError = true
                            Toast.makeText(
                                context,
                                "Please upload a photo or video as evidence",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true
                        val currentDate = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                            .format(Date())

                        fun saveReportToFirestore(mediaUrl: String, mediaType: String) {
                            val reportData = hashMapOf(
                                "title" to reportTitle,
                                "category" to selectedCategory,
                                "location" to location,
                                "description" to description,
                                "status" to "Pending",
                                "dateSubmitted" to currentDate,
                                "reporter" to reporterName,
                                "mediaUrl" to mediaUrl,
                                "mediaType" to mediaType,
                                "imageUrl" to if (mediaType == "image") mediaUrl else "",
                                "timestamp" to Timestamp.now()
                            )

                            db.collection("reports")
                                .add(reportData)
                                .addOnSuccessListener {
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Report Submitted Successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onCancel()
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Error: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                        val uri = selectedMediaUri!!
                        val mimeType = context.contentResolver.getType(uri) ?: ""
                        val extension = when {
                            mimeType.startsWith("video/") -> ".mp4"
                            mimeType.startsWith("image/") -> ".jpg"
                            else -> if (selectedMediaType == "video") ".mp4" else ".jpg"
                        }

                        val fileName = "reports/${System.currentTimeMillis()}$extension"
                        val storageRef = storage.reference.child(fileName)

                        try {
                            val inputStream = context.contentResolver.openInputStream(uri)

                            if (inputStream == null) {
                                isLoading = false
                                Toast.makeText(
                                    context,
                                    "Cannot read selected file. Please pick it again.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val bytes = inputStream.readBytes()
                            inputStream.close()

                            val metadata = storageMetadata {
                                contentType = mimeType.ifBlank {
                                    if (selectedMediaType == "video") "video/mp4" else "image/jpeg"
                                }
                            }

                            storageRef.putBytes(bytes, metadata)
                                .addOnSuccessListener {
                                    storageRef.downloadUrl
                                        .addOnSuccessListener { downloadUri ->
                                            saveReportToFirestore(
                                                mediaUrl = downloadUri.toString(),
                                                mediaType = selectedMediaType
                                            )
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            Toast.makeText(
                                                context,
                                                "Failed to get file URL: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    isLoading = false
                                    Toast.makeText(
                                        context,
                                        "Upload failed: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Failed to read selected media: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1001B)),
                    enabled = !isLoading && isFormValid
                ) {
                    Text("Submit Report", color = Color.White)
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFE1001B))
            }
        }
    }
}