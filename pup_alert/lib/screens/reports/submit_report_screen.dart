import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:image_picker/image_picker.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:speech_to_text/speech_to_text.dart' as stt;

import '../../models/accessibility_settings.dart';
import '../../models/report_model.dart';
import '../../services/ai_service.dart';
import '../../services/auth_service.dart';
import '../../services/report_service.dart';
import 'report_detail_screen.dart';

class SubmitReportScreen extends StatefulWidget {
  final AccessibilitySettings settings;

  const SubmitReportScreen({super.key, required this.settings});

  @override
  State<SubmitReportScreen> createState() => _SubmitReportScreenState();
}

class _SubmitReportScreenState extends State<SubmitReportScreen> {
  final _reportService = ReportService();
  final _authService = AuthService();
  final _firestore = FirebaseFirestore.instance;
  final _imagePicker = ImagePicker();
  final _speech = stt.SpeechToText();

  final _titleController = TextEditingController();
  final _locationController = TextEditingController();
  final _descriptionController = TextEditingController();

  String _selectedCategory = 'Facilities';
  String _autoSuggestedCategory = '';
  Report? _duplicateReport;

  bool _isAnonymous = false;
  bool _isLoading = false;
  bool _hasDraftRestored = false;
  bool _isListening = false;
  XFile? _selectedMedia;
  String _mediaType = ''; // "image" or "video"

  String _reporterName = 'Student';
  String _reporterStudentId = '';
  String _reporterUid = '';

  final List<String> _categories = [
    'Facilities',
    'Maintenance',
    'Safety',
    'Cleanliness',
    'Equipment',
    'Other',
  ];

  @override
  void initState() {
    super.initState();
    _loadUserInfo();
    _restoreDraft();
    _initSpeech();

    _titleController.addListener(_onTitleOrDescriptionChanged);
    _descriptionController.addListener(_onTitleOrDescriptionChanged);
    _locationController.addListener(_checkDuplicateReport);
    _titleController.addListener(_checkDuplicateReport);
  }

  Future<void> _loadUserInfo() async {
    final session = await _authService.getCurrentSession();
    final prefs = await SharedPreferences.getInstance();
    final user = _authService.currentUser;
    setState(() {
      _reporterName =
          session['name'] ?? (prefs.getString('user_name') ?? 'Student');
      _reporterStudentId =
          session['studentId'] ?? (prefs.getString('student_id') ?? '');
      _reporterUid =
          user?.uid ??
          (session['uid'] ?? (prefs.getString('user_uid') ?? 'student_uid'));
    });
  }

  // Feature 3: Restore Draft
  Future<void> _restoreDraft() async {
    final prefs = await SharedPreferences.getInstance();
    final draftTitle = prefs.getString('draft_title') ?? '';
    final draftCategory = prefs.getString('draft_category') ?? '';
    final draftLocation = prefs.getString('draft_location') ?? '';
    final draftDescription = prefs.getString('draft_description') ?? '';

    if (draftTitle.isNotEmpty ||
        draftLocation.isNotEmpty ||
        draftDescription.isNotEmpty) {
      setState(() {
        if (draftTitle.isNotEmpty) _titleController.text = draftTitle;
        if (draftCategory.isNotEmpty && _categories.contains(draftCategory)) {
          _selectedCategory = draftCategory;
        }
        if (draftLocation.isNotEmpty) _locationController.text = draftLocation;
        if (draftDescription.isNotEmpty)
          _descriptionController.text = draftDescription;
        _hasDraftRestored = true;
      });
    }
  }

  Future<void> _saveDraft() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('draft_title', _titleController.text.trim());
    await prefs.setString('draft_category', _selectedCategory);
    await prefs.setString('draft_location', _locationController.text.trim());
    await prefs.setString(
      'draft_description',
      _descriptionController.text.trim(),
    );

    if (!mounted) return;
    ScaffoldMessenger.of(context)
        .showSnackBar(const SnackBar(content: Text('Draft saved locally!')));
  }

  Future<void> _clearDraft() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('draft_title');
    await prefs.remove('draft_category');
    await prefs.remove('draft_location');
    await prefs.remove('draft_description');

    setState(() {
      _titleController.clear();
      _locationController.clear();
      _descriptionController.clear();
      _selectedCategory = 'Facilities';
      _hasDraftRestored = false;
      _duplicateReport = null;
      _autoSuggestedCategory = '';
    });
  }

  // Feature 4: Smart Auto-Categorization matching Kotlin detectCategory
  void _onTitleOrDescriptionChanged() {
    final combinedText =
        '${_titleController.text} ${_descriptionController.text}';
    final detected = AIService.detectCategory(combinedText);

    if (detected.isNotEmpty) {
      if (_selectedCategory.isEmpty ||
          _selectedCategory == _autoSuggestedCategory ||
          _selectedCategory == 'Facilities') {
        setState(() {
          _selectedCategory = detected;
          _autoSuggestedCategory = detected;
        });
      }
    }
  }

  // Feature 4: Live Duplicate Incident Detection matching Kotlin LaunchedEffect
  Future<void> _checkDuplicateReport() async {
    final locQuery = _locationController.text.trim().toLowerCase();
    final titleQuery = _titleController.text.trim().toLowerCase();

    if (locQuery.length >= 3 || titleQuery.length >= 4) {
      try {
        final snapshot = await _firestore.collection('reports').get();
        Report? match;

        for (final doc in snapshot.docs) {
          final data = doc.data();
          final docStatus = data['status']?.toString() ?? 'In Review';
          if (docStatus == 'Resolved') continue;

          final docTitle = (data['title']?.toString() ?? '').toLowerCase();
          final docLoc = (data['location']?.toString() ?? '').toLowerCase();

          final locMatch = locQuery.length >= 3 && docLoc.contains(locQuery);
          final titleMatch =
              titleQuery.length >= 4 && docTitle.contains(titleQuery);

          if (locMatch || titleMatch) {
            match = Report.fromDoc(doc);
            break;
          }
        }

        if (mounted) setState(() => _duplicateReport = match);
      } catch (_) {
        // Silently ignore firestore query errors for duplicate detection
      }
    } else {
      if (_duplicateReport != null && mounted) {
        setState(() => _duplicateReport = null);
      }
    }
  }

  // Feature 5: Voice-to-Text Speech Recognition
  Future<void> _initSpeech() async {
    try {
      await _speech.initialize();
    } catch (_) {}
  }

  void _toggleSpeech() async {
    if (_isListening) {
      await _speech.stop();
      setState(() => _isListening = false);
    } else {
      final available = await _speech.initialize();
      if (available) {
        setState(() => _isListening = true);
        _speech.listen(
          onResult: (result) {
            setState(() {
              final currentText = _descriptionController.text.trim();
              _descriptionController.text = currentText.isEmpty
                  ? result.recognizedWords
                  : '$currentText ${result.recognizedWords}';
            });
          },
        );
      } else {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Speech recognition is not supported on this device.',
            ),
          ),
        );
      }
    }
  }

  Future<void> _pickMedia(ImageSource source, bool isVideo) async {
    try {
      final picked = isVideo
          ? await _imagePicker.pickVideo(source: source)
          : await _imagePicker.pickImage(
              source: source,
              maxWidth: 900,
              maxHeight: 900,
              imageQuality: 70,
            );

      if (picked != null && mounted) {
        setState(() {
          _selectedMedia = picked;
          _mediaType = isVideo ? 'video' : 'image';
        });
      }
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text('Failed to capture media: $e')));
    }
  }

  Future<void> _handleSubmit() async {
    final title = _titleController.text.trim();
    final location = _locationController.text.trim();
    final description = _descriptionController.text.trim();

    if (title.isEmpty || location.isEmpty || description.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please fill out Title, Location, and Description'),
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    try {
      await _reportService.submitReport(
        title: title,
        category: _selectedCategory,
        location: location,
        description: description,
        mediaFile: _selectedMedia,
        isAnonymous: _isAnonymous,
        reporterName: _reporterName,
        reporterUid: _reporterUid,
        reporterStudentId: _reporterStudentId,
      );

      // Clear draft on successful submission
      await _clearDraft();

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Report Submitted Successfully'),
          backgroundColor: Colors.green,
        ),
      );
      Navigator.pop(context);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Submission Error: $e'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  void dispose() {
    _titleController.dispose();
    _locationController.dispose();
    _descriptionController.dispose();
    _speech.stop();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final settings = widget.settings;
    final primaryTextColor = settings.primaryTextColor;
    final secondaryTextColor = settings.secondaryTextColor;
    final accentColor = settings.accentColor;
    final cardColor = settings.cardColor;
    final bodySize = settings.bodyFontSize;
    final titleSize = settings.titleFontSize;
    final smallSize = settings.smallFontSize;

    // AI Hazard analysis result
    final combinedDesc =
        '${_titleController.text} ${_descriptionController.text}'.trim();
    final aiAnalysis = combinedDesc.length > 5
        ? AIService.analyzeIncident(combinedDesc)
        : null;

    return Scaffold(
      backgroundColor: settings.backgroundColor,
      appBar: AppBar(
        title: Text(
          'Submit Incident Report',
          style: TextStyle(
            fontSize: titleSize,
            fontWeight: settings.titleFontWeight,
            color: Colors.white,
          ),
        ),
        backgroundColor: settings.headerColor,
        iconTheme: const IconThemeData(color: Colors.white),
      ),
      body: SingleChildScrollView(
        padding: EdgeInsets.all(settings.cardPadding),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 620),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Restored draft notification banner
                if (_hasDraftRestored)
                  Card(
                    color: settings.grayscaleMode
                        ? const Color(0xFFE9E9E9)
                        : const Color(0xFFE3F2FD),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      side: const BorderSide(color: Color(0xFF1976D2)),
                    ),
                    margin: const EdgeInsets.only(bottom: 14),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 14,
                        vertical: 8,
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            '📝 Restored saved report draft',
                            style: TextStyle(
                              fontSize: smallSize,
                              fontWeight: FontWeight.w600,
                              color: const Color(0xFF1565C0),
                            ),
                          ),
                          TextButton(
                            onPressed: _clearDraft,
                            style: TextButton.styleFrom(
                              padding: EdgeInsets.zero,
                              minimumSize: Size.zero,
                              tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                            ),
                            child: Text(
                              'Discard Draft',
                              style: TextStyle(
                                fontSize: smallSize,
                                fontWeight: FontWeight.bold,
                                color: accentColor,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),

                // Report Title *
                Text(
                  'Report Title *',
                  style: TextStyle(
                    fontSize: smallSize + 1,
                    fontWeight: settings.mediumFontWeight,
                    color: primaryTextColor,
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: _titleController,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: bodySize,
                    fontWeight: settings.bodyFontWeight,
                    color: primaryTextColor,
                  ),
                  decoration: InputDecoration(
                    hintText: 'e.g., Broken classroom door',
                    hintStyle: TextStyle(
                      fontSize: bodySize,
                      color: secondaryTextColor,
                    ),
                    filled: true,
                    fillColor: cardColor,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: settings.borderColor),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: accentColor, width: 2),
                    ),
                  ),
                ),
                const SizedBox(height: 14),

                // Category *
                Text(
                  'Category *',
                  style: TextStyle(
                    fontSize: smallSize + 1,
                    fontWeight: settings.mediumFontWeight,
                    color: primaryTextColor,
                  ),
                ),
                const SizedBox(height: 6),
                DropdownButtonFormField<String>(
                  value: _selectedCategory,
                  dropdownColor: cardColor,
                  style: TextStyle(fontSize: bodySize, color: primaryTextColor),
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: cardColor,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: settings.borderColor),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: accentColor, width: 2),
                    ),
                  ),
                  items: _categories.map((cat) {
                    return DropdownMenuItem(value: cat, child: Text(cat));
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedCategory = val);
                  },
                ),

                // Amber Auto-suggested Category feedback banner matching SubmitReportActivity.kt line 676
                if (_autoSuggestedCategory.isNotEmpty &&
                    _selectedCategory == _autoSuggestedCategory) ...[
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      const Icon(
                        Icons.lightbulb,
                        color: Color(0xFFFF8F00),
                        size: 16,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        'Auto-suggested based on title/description',
                        style: TextStyle(
                          fontSize: smallSize,
                          fontWeight: FontWeight.bold,
                          color: const Color(0xFFFF8F00),
                        ),
                      ),
                    ],
                  ),
                ],
                const SizedBox(height: 14),

                // Reporter (Student name or Anonymous)
                Text(
                  'Reporter',
                  style: TextStyle(
                    fontSize: smallSize + 1,
                    fontWeight: settings.mediumFontWeight,
                    color: primaryTextColor,
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  enabled: false,
                  controller: TextEditingController(
                    text: _isAnonymous ? 'Anonymous Student' : _reporterName,
                  ),
                  style: TextStyle(
                    fontSize: bodySize,
                    color: secondaryTextColor,
                  ),
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: cardColor,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: settings.borderColor),
                    ),
                  ),
                ),
                const SizedBox(height: 10),

                // Anonymous reporting card matching SubmitReportActivity.kt line 716
                Card(
                  color: _isAnonymous
                      ? (settings.grayscaleMode
                            ? const Color(0xFFE9E9E9)
                            : const Color(0xFFFFF5F5))
                      : cardColor,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(
                      settings.cardBorderRadius,
                    ),
                    side: BorderSide(
                      color: _isAnonymous ? accentColor : settings.borderColor,
                    ),
                  ),
                  margin: const EdgeInsets.only(bottom: 14),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 10,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Report Anonymously',
                                style: TextStyle(
                                  fontSize: bodySize,
                                  fontWeight: FontWeight.bold,
                                  color: primaryTextColor,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Hide your name and student ID from public incident feeds.',
                                style: TextStyle(
                                  fontSize: smallSize,
                                  color: secondaryTextColor,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Switch(
                          value: _isAnonymous,
                          activeThumbColor: accentColor,
                          onChanged: (val) =>
                              setState(() => _isAnonymous = val),
                        ),
                      ],
                    ),
                  ),
                ),

                // Location *
                Text(
                  'Location *',
                  style: TextStyle(
                    fontSize: smallSize + 1,
                    fontWeight: settings.mediumFontWeight,
                    color: primaryTextColor,
                  ),
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: _locationController,
                  maxLines: 1,
                  style: TextStyle(
                    fontSize: bodySize,
                    fontWeight: settings.bodyFontWeight,
                    color: primaryTextColor,
                  ),
                  decoration: InputDecoration(
                    hintText: 'e.g., Room 302, 3rd Floor East Wing',
                    hintStyle: TextStyle(
                      fontSize: bodySize,
                      color: secondaryTextColor,
                    ),
                    filled: true,
                    fillColor: cardColor,
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: settings.borderColor),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: accentColor, width: 2),
                    ),
                  ),
                ),
                const SizedBox(height: 14),

                // Amber Live Duplicate Incident Detection Alert Card matching SubmitReportActivity.kt line 821
                if (_duplicateReport != null) ...[
                  Card(
                    color: const Color(0xFFFFF8E1),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      side: const BorderSide(
                        color: Color(0xFFFFB300),
                        width: 1.2,
                      ),
                    ),
                    margin: const EdgeInsets.only(bottom: 14),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(
                                Icons.warning_amber_rounded,
                                color: Color(0xFFB78103),
                                size: 20,
                              ),
                              SizedBox(width: 6),
                              Text(
                                'Potential Duplicate Incident Detected',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFFB78103),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            "An active report '${_duplicateReport!.title}' in '${_duplicateReport!.location}' is currently ${_duplicateReport!.status}.",
                            style: TextStyle(
                              fontSize: smallSize,
                              color: const Color(0xFFB78103),
                            ),
                          ),
                          const SizedBox(height: 6),
                          Align(
                            alignment: Alignment.centerRight,
                            child: TextButton(
                              onPressed: () {
                                Navigator.push(
                                  context,
                                  MaterialPageRoute(
                                    builder: (context) => ReportDetailScreen(
                                      report: _duplicateReport!,
                                      userRole: 'student',
                                    ),
                                  ),
                                );
                              },
                              style: TextButton.styleFrom(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 8,
                                  vertical: 2,
                                ),
                              ),
                              child: const Text(
                                'View Existing Report',
                                style: TextStyle(
                                  fontWeight: FontWeight.bold,
                                  color: Color(0xFFB78103),
                                ),
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],

                // Description * (with Speech-to-Text Voice Dictation)
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Description *',
                      style: TextStyle(
                        fontSize: smallSize + 1,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    // Voice to text button matching Feature 5
                    InkWell(
                      onTap: _toggleSpeech,
                      borderRadius: BorderRadius.circular(6),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 2,
                        ),
                        child: Row(
                          children: [
                            Icon(
                              _isListening ? Icons.mic : Icons.mic_none,
                              color: _isListening ? Colors.red : accentColor,
                              size: 18,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              _isListening ? 'Listening...' : 'Voice Input',
                              style: TextStyle(
                                fontSize: smallSize,
                                fontWeight: FontWeight.bold,
                                color: _isListening ? Colors.red : accentColor,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                TextField(
                  controller: _descriptionController,
                  maxLines: 4,
                  style: TextStyle(
                    fontSize: bodySize,
                    fontWeight: settings.bodyFontWeight,
                    color: primaryTextColor,
                  ),
                  decoration: InputDecoration(
                    hintText: 'Provide detailed information about the incident or hazard...',
                    hintStyle: TextStyle(
                      fontSize: bodySize,
                      color: secondaryTextColor,
                    ),
                    filled: true,
                    fillColor: cardColor,
                    contentPadding: const EdgeInsets.all(14),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: settings.borderColor),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      borderSide: BorderSide(color: accentColor, width: 2),
                    ),
                  ),
                ),
                const SizedBox(height: 10),

                // AI Hazard Severity & Priority Banner
                if (aiAnalysis != null) ...[
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 8,
                    ),
                    decoration: BoxDecoration(
                      color:
                          aiAnalysis.severity == 'Emergency' ||
                              aiAnalysis.severity == 'High'
                          ? Colors.red.shade50
                          : Colors.amber.shade50,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(
                        color:
                            aiAnalysis.severity == 'Emergency' ||
                                aiAnalysis.severity == 'High'
                            ? Colors.red.shade200
                            : Colors.amber.shade300,
                      ),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          Icons.shield_outlined,
                          size: 18,
                          color:
                              aiAnalysis.severity == 'Emergency' ||
                                  aiAnalysis.severity == 'High'
                              ? Colors.red.shade700
                              : Colors.amber.shade800,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            'AI Hazard Assessment: ${aiAnalysis.severity} Severity (${(aiAnalysis.confidence * 100).toInt()}% confidence)',
                            style: TextStyle(
                              fontSize: smallSize,
                              fontWeight: FontWeight.bold,
                              color:
                                  aiAnalysis.severity == 'Emergency' ||
                                      aiAnalysis.severity == 'High'
                                  ? Colors.red.shade800
                                  : Colors.amber.shade900,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),
                ],

                // Media Attachment Section
                Text(
                  'Media Attachment (Photo / Video)',
                  style: TextStyle(
                    fontSize: smallSize + 1,
                    fontWeight: settings.mediumFontWeight,
                    color: primaryTextColor,
                  ),
                ),
                const SizedBox(height: 6),
                if (_selectedMedia == null) ...[
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () =>
                              _pickMedia(ImageSource.camera, false),
                          icon: const Icon(Icons.camera_alt, size: 18),
                          label: const Text('Take Photo'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: accentColor,
                            side: BorderSide(color: settings.borderColor),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(
                                settings.cardBorderRadius,
                              ),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () =>
                              _pickMedia(ImageSource.gallery, false),
                          icon: const Icon(Icons.photo_library, size: 18),
                          label: const Text('Gallery'),
                          style: OutlinedButton.styleFrom(
                            foregroundColor: accentColor,
                            side: BorderSide(color: settings.borderColor),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(
                                settings.cardBorderRadius,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ] else ...[
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: cardColor,
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                      border: Border.all(color: settings.borderColor),
                    ),
                    child: Row(
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: FutureBuilder<Uint8List>(
                            future: _selectedMedia!.readAsBytes(),
                            builder: (ctx, snap) {
                              if (snap.hasData) {
                                return Image.memory(
                                  snap.data!,
                                  width: 60,
                                  height: 60,
                                  fit: BoxFit.cover,
                                );
                              }
                              return Container(
                                width: 60,
                                height: 60,
                                color: settings.borderColor.withValues(
                                  alpha: 0.3,
                                ),
                                child: Icon(
                                  _mediaType == 'video'
                                      ? Icons.videocam
                                      : Icons.image,
                                  color: accentColor,
                                  size: 28,
                                ),
                              );
                            },
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                _selectedMedia!.name,
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: TextStyle(
                                  fontSize: smallSize,
                                  fontWeight: FontWeight.bold,
                                  color: primaryTextColor,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                _mediaType == 'video'
                                    ? 'Video attached'
                                    : 'Photo attached • Ready for upload',
                                style: TextStyle(
                                  fontSize: smallSize - 1,
                                  color: Colors.green.shade700,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: const Icon(Icons.close, color: Colors.red),
                          tooltip: 'Remove Attachment',
                          onPressed: () =>
                              setState(() => _selectedMedia = null),
                        ),
                      ],
                    ),
                  ),
                ],
                const SizedBox(height: 24),

                // Action Buttons: Save Draft & Submit Report
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton(
                        onPressed: _isLoading ? null : _saveDraft,
                        style: OutlinedButton.styleFrom(
                          minimumSize: Size(
                            double.infinity,
                            settings.buttonHeight,
                          ),
                          foregroundColor: accentColor,
                          side: BorderSide(color: accentColor),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(
                              settings.cardBorderRadius,
                            ),
                          ),
                        ),
                        child: Text(
                          'Save Draft',
                          style: TextStyle(
                            fontSize: bodySize,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: ElevatedButton(
                        onPressed: _isLoading ? null : _handleSubmit,
                        style: ElevatedButton.styleFrom(
                          minimumSize: Size(
                            double.infinity,
                            settings.buttonHeight,
                          ),
                          backgroundColor: accentColor,
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(
                              settings.cardBorderRadius,
                            ),
                          ),
                          elevation: 2,
                        ),
                        child: _isLoading
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : Text(
                                'Submit Report',
                                style: TextStyle(
                                  fontSize: bodySize,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 32),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
