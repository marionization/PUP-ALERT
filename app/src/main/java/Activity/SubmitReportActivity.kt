package Activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import Activity.ui.theme.SeriousModeTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubmitReportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val reporterName = prefs.getString("user_name", "Student") ?: "Student"
        val reporterStudentId = prefs.getString("student_id", "") ?: ""

        val settings = AccessibilitySettings(
            textSize = prefs.getString("access_text_size", "Default") ?: "Default",
            boldText = prefs.getBoolean("access_bold_text", false),
            contrastMode = prefs.getString("access_contrast_mode", "Default") ?: "Default",
            reduceMotion = prefs.getBoolean("access_reduce_motion", false),
            grayscaleMode = prefs.getBoolean("access_grayscale_mode", false),
            largeButtons = prefs.getBoolean("access_large_buttons", false),
            simplifiedCards = prefs.getBoolean("access_simplified_cards", false),
            hideMediaPreview = prefs.getBoolean("access_hide_media_preview", false)
        )

        setContent {
            SeriousModeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = when (settings.contrastMode) {
                        "Dark Contrast" -> Color(0xFF121212)
                        "Light Contrast" -> Color.White
                        "High Contrast" -> Color(0xFFF7F7F7)
                        else -> Color(0xFFF9F9FB)
                    }
                ) {
                    SubmitReportScreen(
                        onCancel = { finish() },
                        reporterName = reporterName,
                        reporterStudentId = reporterStudentId,
                        settings = settings
                    )
                }
            }
        }
    }
}

data class AccessibilitySettings(
    val textSize: String = "Default",
    val boldText: Boolean = false,
    val contrastMode: String = "Default",
    val reduceMotion: Boolean = false,
    val grayscaleMode: Boolean = false,
    val largeButtons: Boolean = false,
    val simplifiedCards: Boolean = false,
    val hideMediaPreview: Boolean = false
)

@Composable
fun SubmitReportScreen(
    onCancel: () -> Unit,
    reporterName: String,
    reporterStudentId: String,
    settings: AccessibilitySettings
) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()

    var reportTitle by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Feature 4: Smart Auto-Categorization & Live Duplicate Detection State
    var autoSuggestedCategory by remember { mutableStateOf("") }
    var duplicateReport by remember { mutableStateOf<Report?>(null) }

    // Feature 5: Voice-to-Text State & Launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                description = if (description.isBlank()) spokenText else "$description $spokenText"
                Toast.makeText(context, "Voice input recognized!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun launchSpeechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to describe the issue...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Speech recognition is not supported on this device.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun detectCategory(text: String): String {
        val lower = text.lowercase(Locale.getDefault())

        val safetyKeywords = listOf("hazard", "fire", "smoke", "exposed", "slippery", "emergency", "danger", "alarm", "lock", "gate", "security", "injury", "glass", "sharp", "knife", "threat", "fight")
        val maintenanceKeywords = listOf("leak", "pipe", "water", "plumbing", "electrical", "wire", "plug", "outlet", "light", "bulb", "fan", "aircon", "ac", "broken", "damaged", "repair", "flush", "toilet", "faucet", "sink", "drain")
        val cleanlinessKeywords = listOf("trash", "garbage", "dirty", "smell", "odor", "waste", "spill", "mess", "clutter", "restroom", "washroom", "clean", "dust")
        val equipmentKeywords = listOf("projector", "tv", "screen", "computer", "pc", "monitor", "speaker", "mic", "microphone", "lab", "printer", "appliance", "mouse", "keyboard")
        val facilitiesKeywords = listOf("door", "window", "wall", "ceiling", "roof", "elevator", "stairs", "desk", "chair", "table", "board", "building", "hall", "room")

        return when {
            safetyKeywords.any { lower.contains(it) } -> "Safety"
            maintenanceKeywords.any { lower.contains(it) } -> "Maintenance"
            cleanlinessKeywords.any { lower.contains(it) } -> "Cleanliness"
            equipmentKeywords.any { lower.contains(it) } -> "Equipment"
            facilitiesKeywords.any { lower.contains(it) } -> "Facilities"
            else -> ""
        }
    }

    LaunchedEffect(reportTitle, description) {
        if (selectedCategory.isBlank() || selectedCategory == autoSuggestedCategory) {
            val detected = detectCategory("$reportTitle $description")
            if (detected.isNotBlank()) {
                selectedCategory = detected
                autoSuggestedCategory = detected
            }
        }
    }

    LaunchedEffect(location, reportTitle) {
        val locQuery = location.trim().lowercase(Locale.getDefault())
        val titleQuery = reportTitle.trim().lowercase(Locale.getDefault())

        if (locQuery.length >= 3 || titleQuery.length >= 4) {
            db.collection("reports")
                .get()
                .addOnSuccessListener { snapshot ->
                    val match = snapshot.documents.mapNotNull { doc ->
                        try {
                            val docStatus = doc.getString("status") ?: "In Review"
                            if (docStatus == "Resolved") return@mapNotNull null

                            val docTitle = doc.getString("title") ?: ""
                            val docLoc = doc.getString("location") ?: ""

                            val locMatch = locQuery.length >= 3 && docLoc.lowercase(Locale.getDefault()).contains(locQuery)
                            val titleMatch = titleQuery.length >= 4 && docTitle.lowercase(Locale.getDefault()).contains(titleQuery)

                            if (locMatch || titleMatch) {
                                Report(
                                    id = doc.id,
                                    title = docTitle,
                                    category = doc.getString("category") ?: "",
                                    location = docLoc,
                                    description = doc.getString("description") ?: "",
                                    imageUrl = doc.getString("imageUrl"),
                                    mediaUrl = doc.getString("mediaUrl") ?: "",
                                    mediaType = doc.getString("mediaType") ?: "",
                                    status = docStatus,
                                    dateSubmitted = doc.getString("dateSubmitted") ?: "",
                                    reporter = doc.getString("reporter") ?: ""
                                )
                            } else null
                        } catch (_: Exception) {
                            null
                        }
                    }.firstOrNull()

                    duplicateReport = match
                }
        } else {
            duplicateReport = null
        }
    }

    // Anonymous reporting state
    var isAnonymous by remember { mutableStateOf(false) }

    // Feature 3: Drafts & Offline Local Auto-Save State
    val draftPrefs = remember { context.getSharedPreferences("report_drafts", Context.MODE_PRIVATE) }
    var hasDraftRestored by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val savedTitle = draftPrefs.getString("draft_title", "") ?: ""
        val savedCategory = draftPrefs.getString("draft_category", "") ?: ""
        val savedLocation = draftPrefs.getString("draft_location", "") ?: ""
        val savedDescription = draftPrefs.getString("draft_description", "") ?: ""

        if (savedTitle.isNotBlank() || savedLocation.isNotBlank() || savedDescription.isNotBlank()) {
            reportTitle = savedTitle
            selectedCategory = savedCategory
            location = savedLocation
            description = savedDescription
            hasDraftRestored = true
        }
    }

    fun saveDraft() {
        draftPrefs.edit()
            .putString("draft_title", reportTitle)
            .putString("draft_category", selectedCategory)
            .putString("draft_location", location)
            .putString("draft_description", description)
            .apply()
        Toast.makeText(context, "Draft saved locally!", Toast.LENGTH_SHORT).show()
    }

    fun clearDraft() {
        draftPrefs.edit().clear().apply()
        reportTitle = ""
        selectedCategory = ""
        location = ""
        description = ""
        hasDraftRestored = false
    }

    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaType by remember { mutableStateOf("") }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var pendingAction by remember { mutableStateOf("") }

    var showMediaError by remember { mutableStateOf(false) }
    var showCaptureOptions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val backgroundColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF121212)
        "Light Contrast" -> Color.White
        "High Contrast" -> Color(0xFFF7F7F7)
        else -> Color(0xFFF9F9FB)
    }

    val cardColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF1E1E1E)
        else -> Color.White
    }

    val anonymousCardColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFF2A2A2A)
        else -> Color(0xFFFFF5F5)
    }

    val textColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color.White
        else -> Color(0xFF222222)
    }

    val subTextColor = when (settings.contrastMode) {
        "Dark Contrast" -> Color(0xFFD0D0D0)
        else -> Color.Gray
    }

    val fieldBorderColor =
        if (settings.contrastMode == "Dark Contrast") {
            Color(0xFF666666)
        } else {
            Color(0xFFDADDE2)
        }

    val accentColor =
        if (settings.grayscaleMode) Color(0xFF444444) else Color(0xFFE1001B)

    val titleSize = when (settings.textSize) {
        "Small" -> 16.sp
        "Large" -> 20.sp
        "Extra Large" -> 22.sp
        else -> 18.sp
    }

    val labelSize = when (settings.textSize) {
        "Small" -> 12.sp
        "Large" -> 16.sp
        "Extra Large" -> 18.sp
        else -> 14.sp
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

    val cardShape = RoundedCornerShape(if (settings.simplifiedCards) 8.dp else 12.dp)
    val inputShape = RoundedCornerShape(if (settings.simplifiedCards) 8.dp else 10.dp)
    val contentPadding = if (settings.largeButtons) 28.dp else 24.dp
    val buttonHeight = if (settings.largeButtons) 54.dp else 48.dp
    val previewHeight = if (settings.largeButtons) 190.dp else 160.dp

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = subTextColor,
        focusedBorderColor = accentColor,
        unfocusedBorderColor = fieldBorderColor,
        disabledBorderColor = fieldBorderColor,
        focusedContainerColor = cardColor,
        unfocusedContainerColor = cardColor,
        disabledContainerColor = cardColor,
        cursorColor = accentColor,
        focusedPlaceholderColor = subTextColor,
        unfocusedPlaceholderColor = subTextColor,
        disabledPlaceholderColor = subTextColor
    )

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCaptureUri != null) {
            selectedMediaUri = pendingCaptureUri
            selectedMediaType = "image"
            showMediaError = false
        } else {
            pendingCaptureUri = null
            Toast.makeText(context, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val captureVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        if (success && pendingCaptureUri != null) {
            selectedMediaUri = pendingCaptureUri
            selectedMediaType = "video"
            showMediaError = false
        } else {
            pendingCaptureUri = null
            Toast.makeText(context, "Video capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        val uri = createTempMediaUri(
            context = context,
            isVideo = pendingAction == "video"
        )

        if (uri == null) {
            Toast.makeText(
                context,
                if (pendingAction == "video") {
                    "Failed to create video file"
                } else {
                    "Failed to create photo file"
                },
                Toast.LENGTH_SHORT
            ).show()
            return@rememberLauncherForActivityResult
        }

        pendingCaptureUri = uri

        if (pendingAction == "photo") {
            takePictureLauncher.launch(uri)
        } else {
            captureVideoLauncher.launch(uri)
        }
    }

    fun launchCameraCapture(type: String) {
        pendingAction = type

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val uri = createTempMediaUri(
                context = context,
                isVideo = type == "video"
            )

            if (uri == null) {
                Toast.makeText(
                    context,
                    "Failed to prepare media file",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            pendingCaptureUri = uri

            if (type == "photo") {
                takePictureLauncher.launch(uri)
            } else {
                captureVideoLauncher.launch(uri)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scrollState = rememberScrollState()

    val isFormValid =
        reportTitle.isNotBlank() &&
                selectedCategory.isNotBlank() &&
                location.isNotBlank() &&
                description.isNotBlank() &&
                selectedMediaUri != null &&
                selectedMediaType.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (settings.grayscaleMode) {
                    Color(0xFFF2F2F2)
                } else {
                    backgroundColor
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
        ) {
            Text(
                text = "Submit New Report",
                fontSize = titleSize,
                fontWeight = if (settings.boldText) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            if (hasDraftRestored) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = inputShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.grayscaleMode) Color(0xFFE9E9E9) else Color(0xFFE3F2FD)
                    ),
                    border = BorderStroke(1.dp, Color(0xFF1976D2))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝 Restored saved report draft",
                            fontSize = smallSize,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { clearDraft() },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = "Discard Draft",
                                color = accentColor,
                                fontSize = smallSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Text(
                text = "Report Title *",
                fontSize = labelSize,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = reportTitle,
                onValueChange = { reportTitle = it },
                placeholder = {
                    Text(
                        "e.g., Broken classroom door",
                        fontSize = bodySize
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = inputShape,
                colors = textFieldColors,
                textStyle = TextStyle(
                    fontSize = bodySize,
                    fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal
                )
            )

            Text(
                text = "Category *",
                fontSize = labelSize,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .widthIn(min = 150.dp, max = 220.dp)
                    .padding(bottom = 14.dp)
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "Select a category",
                            color = subTextColor,
                            fontSize = bodySize
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Category Dropdown",
                            tint = subTextColor
                        )
                    },
                    singleLine = true,
                    shape = inputShape,
                    colors = textFieldColors,
                    textStyle = TextStyle(
                        fontSize = bodySize,
                        fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal
                    )
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Transparent)
                        .clickable(enabled = !isLoading) {
                            categoryExpanded = true
                        }
                )

                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier.background(cardColor)
                ) {
                    listOf(
                        "Facilities",
                        "Maintenance",
                        "Safety",
                        "Cleanliness",
                        "Equipment",
                        "Other"
                    ).forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = option,
                                        fontSize = bodySize,
                                        color = textColor
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
                                selectedCategory = option
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            if (autoSuggestedCategory.isNotBlank() && selectedCategory == autoSuggestedCategory) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = "Auto-suggested Category",
                        tint = Color(0xFFFF8F00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Auto-suggested based on title/description",
                        fontSize = smallSize,
                        color = Color(0xFFFF8F00)
                    )
                }
            }

            Text(
                text = "Reporter",
                fontSize = labelSize,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = if (isAnonymous) "Anonymous Student" else reporterName,
                onValueChange = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                shape = inputShape,
                colors = textFieldColors,
                textStyle = TextStyle(fontSize = bodySize)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = inputShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.grayscaleMode) {
                        Color(0xFFE9E9E9)
                    } else {
                        anonymousCardColor
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Submit anonymously",
                            fontSize = bodySize,
                            fontWeight = if (settings.boldText) {
                                FontWeight.Bold
                            } else {
                                FontWeight.Medium
                            },
                            color = textColor
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Your name and student ID will be hidden from administrators.",
                            fontSize = smallSize,
                            color = subTextColor
                        )
                    }

                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        enabled = !isLoading,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = fieldBorderColor
                        )
                    )
                }
            }

            if (!isAnonymous && reporterStudentId.isNotBlank()) {
                Text(
                    text = "Student ID",
                    fontSize = labelSize,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                    color = textColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = reporterStudentId,
                    onValueChange = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = inputShape,
                    colors = textFieldColors,
                    textStyle = TextStyle(fontSize = bodySize)
                )
            }

            Text(
                text = "Location *",
                fontSize = labelSize,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                placeholder = {
                    Text(
                        "e.g., Building A, Room 301",
                        fontSize = bodySize
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = inputShape,
                colors = textFieldColors,
                textStyle = TextStyle(
                    fontSize = bodySize,
                    fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal
                )
            )

            if (duplicateReport != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = inputShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (settings.grayscaleMode) Color(0xFFE9E9E9) else Color(0xFFFFF8E1)
                    ),
                    border = BorderStroke(1.dp, Color(0xFFFFB300))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Potential Duplicate",
                                tint = Color(0xFFB78103),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Potential Duplicate Report Found",
                                fontSize = bodySize,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB78103)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "An active report '${duplicateReport?.title}' in '${duplicateReport?.location}' is currently ${duplicateReport?.status}.",
                            fontSize = smallSize,
                            color = Color(0xFFB78103)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(context, ReportDetailActivity::class.java).apply {
                                    putExtra("id", duplicateReport?.id)
                                    putExtra("title", duplicateReport?.title)
                                    putExtra("category", duplicateReport?.category)
                                    putExtra("location", duplicateReport?.location)
                                    putExtra("description", duplicateReport?.description)
                                    putExtra("status", duplicateReport?.status)
                                    putExtra("reporter", duplicateReport?.reporter)
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "View Active Report",
                                color = accentColor,
                                fontSize = smallSize,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description *",
                    fontSize = labelSize,
                    fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )

                IconButton(
                    onClick = { launchSpeechToText() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Voice Input Speech-to-Text",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = {
                    Text(
                        "Describe the issue in detail...",
                        fontSize = bodySize
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (settings.largeButtons) 110.dp else 90.dp)
                    .padding(bottom = 14.dp),
                maxLines = 4,
                shape = inputShape,
                colors = textFieldColors,
                textStyle = TextStyle(
                    fontSize = bodySize,
                    fontWeight = if (settings.boldText) FontWeight.SemiBold else FontWeight.Normal
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Capture Photo or Video *",
                fontSize = labelSize,
                fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(previewHeight)
                    .border(
                        1.dp,
                        if (showMediaError) accentColor else fieldBorderColor,
                        cardShape
                    )
                    .background(cardColor, cardShape)
                    .clickable(enabled = selectedMediaUri == null && !isLoading) {
                        showCaptureOptions = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedMediaUri != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (selectedMediaType == "image" && !settings.hideMediaPreview) {
                            Image(
                                painter = rememberAsyncImagePainter(model = selectedMediaUri),
                                contentDescription = "Captured Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardColor, cardShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(cardColor),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedMediaType == "video") {
                                        Icons.Filled.Videocam
                                    } else {
                                        Icons.Filled.CameraAlt
                                    },
                                    contentDescription = "Captured Media",
                                    tint = accentColor,
                                    modifier = Modifier.size(
                                        if (settings.largeButtons) 50.dp else 42.dp
                                    )
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = if (selectedMediaType == "video") {
                                        "Video captured"
                                    } else {
                                        "Photo captured"
                                    },
                                    fontSize = bodySize,
                                    fontWeight = if (settings.boldText) {
                                        FontWeight.Medium
                                    } else {
                                        FontWeight.Normal
                                    },
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Ready to upload with your report",
                                    fontSize = smallSize,
                                    color = subTextColor
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedMediaUri = null
                                selectedMediaType = ""
                                pendingCaptureUri = null
                                pendingAction = ""
                                showMediaError = false
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                                .background(accentColor, CircleShape)
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
                            tint = if (showMediaError) accentColor else Color(0xFF929292)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tap to open camera",
                            fontSize = bodySize,
                            color = if (showMediaError) accentColor else Color(0xFF929292)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Take a photo or record a video",
                            fontSize = smallSize,
                            color = subTextColor
                        )
                    }
                }
            }

            if (showMediaError) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Photo or video evidence is required.",
                    color = accentColor,
                    fontSize = smallSize
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { saveDraft() },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = SolidColor(fieldBorderColor)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = cardColor
                    ),
                    enabled = !isLoading && (reportTitle.isNotBlank() || location.isNotBlank() || description.isNotBlank())
                ) {
                    Text(
                        text = "Save Draft",
                        color = textColor,
                        fontSize = (bodySize.value - 2).sp,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal
                    )
                }

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp,
                        brush = SolidColor(fieldBorderColor)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = cardColor
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Cancel",
                        color = textColor,
                        fontSize = (bodySize.value - 2).sp,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Button(
                    onClick = {
                        if (
                            reportTitle.isBlank() ||
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
                                "Please capture a photo or video as evidence",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        val currentUser = FirebaseAuth.getInstance().currentUser

                        if (currentUser == null) {
                            Toast.makeText(
                                context,
                                "Your session has expired. Please log in again.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }

                        isLoading = true

                        val currentDate = SimpleDateFormat(
                            "MMMM d, yyyy",
                            Locale.getDefault()
                        ).format(Date())

                        fun saveReportToFirestore(mediaUrl: String, mediaType: String) {
                            val reportRef = db.collection("reports").document()

                            val visibleReporter = if (isAnonymous) {
                                "Anonymous Student"
                            } else {
                                reporterName
                            }

                            val visibleStudentId = if (isAnonymous) {
                                ""
                            } else {
                                reporterStudentId
                            }

                            val reportData = hashMapOf(
                                "reportId" to reportRef.id,
                                "title" to reportTitle.trim(),
                                "category" to selectedCategory,
                                "location" to location.trim(),
                                "description" to description.trim(),
                                "status" to "In Review",
                                "dateSubmitted" to currentDate,

                                // Anonymous-report settings
                                "isAnonymous" to isAnonymous,
                                "reporter" to visibleReporter,
                                "reporterStudentId" to visibleStudentId,

                                // Private report ownership field
                                "reporterUid" to currentUser.uid,

                                "mediaUrl" to mediaUrl,
                                "mediaType" to mediaType,
                                "imageUrl" to if (mediaType == "image") mediaUrl else "",
                                "timestamp" to Timestamp.now()
                            )

                            reportRef.set(reportData)
                                .addOnSuccessListener {
                                    val adminNotification = hashMapOf(
                                        "reportId" to reportRef.id,
                                        "title" to "New Report Submitted",
                                        "message" to if (isAnonymous) {
                                            "A new report \"${reportTitle.trim()}\" was submitted by Anonymous Student."
                                        } else {
                                            "A new report \"${reportTitle.trim()}\" was submitted by $reporterName."
                                        },
                                        "reportTitle" to reportTitle.trim(),
                                        "status" to "In Review",
                                        "reporter" to visibleReporter,
                                        "isAnonymous" to isAnonymous,
                                        "targetRole" to "Admin",
                                        "targetUser" to "",
                                        "type" to "new_report",
                                        "read" to false,
                                        "timestamp" to Timestamp.now()
                                    )
                                    db.collection("notifications").add(adminNotification)

                                    NotificationHelper.createNotificationChannel(context)
                                    NotificationHelper.showNotification(
                                        context = context,
                                        title = "Report Submitted",
                                        message = if (isAnonymous) {
                                            "Your report \"${reportTitle.trim()}\" was submitted anonymously."
                                        } else {
                                            "Your report \"${reportTitle.trim()}\" was submitted successfully."
                                        }
                                    )

                                    draftPrefs.edit().clear().apply()

                                    isLoading = false

                                    Toast.makeText(
                                        context,
                                        if (isAnonymous) {
                                            "Anonymous report submitted successfully"
                                        } else {
                                            "Report submitted successfully"
                                        },
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

                        val fileName =
                            "reports/${currentUser.uid}/${System.currentTimeMillis()}_${
                                if (selectedMediaType == "video") {
                                    "video.mp4"
                                } else {
                                    "image.jpg"
                                }
                            }"

                        val storageRef = storage.reference.child(fileName)

                        storageRef.putFile(uri)
                            .addOnSuccessListener {
                                storageRef.downloadUrl
                                    .addOnSuccessListener { downloadUri ->
                                        saveReportToFirestore(
                                            downloadUri.toString(),
                                            selectedMediaType
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
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(buttonHeight),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor
                    ),
                    enabled = !isLoading && isFormValid
                ) {
                    Text(
                        text = "Submit Report",
                        color = Color.White,
                        fontSize = bodySize,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        if (showCaptureOptions) {
            AlertDialog(
                onDismissRequest = { showCaptureOptions = false },
                containerColor = cardColor,
                title = {
                    Text(
                        text = "Capture Evidence",
                        color = textColor,
                        fontSize = bodySize,
                        fontWeight = if (settings.boldText) FontWeight.Bold else FontWeight.Medium
                    )
                },
                text = {
                    Text(
                        text = "Choose how you want to submit evidence.",
                        color = subTextColor,
                        fontSize = bodySize
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCaptureOptions = false
                            launchCameraCapture("photo")
                        }
                    ) {
                        Text(
                            text = "Take Photo",
                            color = accentColor
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showCaptureOptions = false
                            launchCameraCapture("video")
                        }
                    ) {
                        Text(
                            text = "Record Video",
                            color = accentColor
                        )
                    }
                }
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(
                            alpha = if (settings.reduceMotion) 0.2f else 0.3f
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        }
    }
}

private fun createTempMediaUri(
    context: Context,
    isVideo: Boolean
): Uri? {
    return try {
        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val file = File.createTempFile(
            if (isVideo) {
                "REPORT_VIDEO_$timeStamp"
            } else {
                "REPORT_IMAGE_$timeStamp"
            },
            if (isVideo) ".mp4" else ".jpg",
            context.cacheDir
        )

        FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    } catch (e: Exception) {
        null
    }
}