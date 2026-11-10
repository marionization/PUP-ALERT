import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../models/accessibility_settings.dart';
import '../../services/auth_service.dart';
import '../../theme/app_theme.dart';

class ProfileScreen extends StatefulWidget {
  final String userRole;
  final String userName;
  final String studentId;
  final String firstName;
  final bool isEmbedded;
  final AccessibilitySettings? settings;

  const ProfileScreen({
    super.key,
    required this.userRole,
    required this.userName,
    required this.studentId,
    this.firstName = '',
    this.isEmbedded = false,
    this.settings,
  });

  @override
  State<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends State<ProfileScreen> {
  final _authService = AuthService();
  final _firestore = FirebaseFirestore.instance;
  late TextEditingController _nameController;
  late TextEditingController _studentIdController;
  late TextEditingController _departmentController;
  late TextEditingController _emailController;

  bool _isEditing = false;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _nameController = TextEditingController(text: widget.userName);
    _studentIdController = TextEditingController(text: widget.studentId);
    _departmentController = TextEditingController(text: '');
    _emailController = TextEditingController(
      text:
          _authService.currentUser?.email ??
          (widget.userRole.toLowerCase() == 'admin'
              ? 'admin@pup.edu.ph'
              : 'student@pup.edu.ph'),
    );
    _loadProfileData();
  }

  Future<void> _loadProfileData() async {
    final prefs = await SharedPreferences.getInstance();
    final user = _authService.currentUser;
    String name = widget.userName;
    String studentId = widget.studentId.isNotEmpty
        ? widget.studentId
        : (prefs.getString('student_id') ?? '');
    String department = prefs.getString('user_department') ?? '';
    String email =
        user?.email ??
        (prefs.getString('user_email') ?? _emailController.text);

    if (widget.userRole.toLowerCase() != 'admin' && user != null) {
      try {
        final doc = await _firestore.collection('users').doc(user.uid).get();
        if (doc.exists) {
          final data = doc.data() as Map<String, dynamic>;
          final fName = data['firstName']?.toString().trim() ?? '';
          final lName = data['lastName']?.toString().trim() ?? '';
          final full = data['name']?.toString().trim() ?? '';
          if (full.isNotEmpty) {
            name = full;
          } else if (fName.isNotEmpty || lName.isNotEmpty) {
            name = '$fName $lName'.trim();
          }
          studentId = data['studentId']?.toString().trim() ?? studentId;
          department = data['department']?.toString().trim() ?? department;
          email = data['email']?.toString().trim() ?? email;
        }
      } catch (_) {}
    }

    if (!mounted) return;
    setState(() {
      _nameController.text = name;
      _studentIdController.text = studentId;
      _departmentController.text = department;
      _emailController.text = email;
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _studentIdController.dispose();
    _departmentController.dispose();
    _emailController.dispose();
    super.dispose();
  }

  Future<void> _handleSave() async {
    setState(() => _isLoading = true);
    final newName = _nameController.text.trim();
    final newStudentId = _studentIdController.text.trim();
    final newDepartment = _departmentController.text.trim();
    final parts = newName.split(' ');
    final newFirstName = parts.isNotEmpty ? parts.first : newName;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('user_name', newName);
    await prefs.setString('student_first_name', newFirstName);
    await prefs.setString('student_id', newStudentId);
    await prefs.setString('user_department', newDepartment);

    final user = _authService.currentUser;
    if (widget.userRole.toLowerCase() != 'admin' && user != null) {
      try {
        await user.updateDisplayName(newName);
        await _firestore.collection('users').doc(user.uid).set({
          'name': newName,
          'firstName': newFirstName,
          'lastName': parts.length > 1 ? parts.sublist(1).join(' ') : '',
          'studentId': newStudentId,
          'department': newDepartment,
        }, SetOptions(merge: true));
      } catch (_) {}
    }

    setState(() {
      _isLoading = false;
      _isEditing = false;
    });

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Profile updated successfully!'),
        backgroundColor: Colors.green,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final settings = widget.settings ?? const AccessibilitySettings();
    final primaryTextColor = settings.primaryTextColor;
    final secondaryTextColor = settings.secondaryTextColor;
    final accentColor = settings.accentColor;
    final cardColor = settings.cardColor;

    final content = SingleChildScrollView(
      padding: EdgeInsets.all(settings.cardPadding),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 600),
          child: Column(
            children: [
              // Header Avatar
              CircleAvatar(
                radius: 46,
                backgroundColor: accentColor,
                child: Text(
                  widget.userName.isNotEmpty
                      ? widget.userName[0].toUpperCase()
                      : 'P',
                  style: const TextStyle(
                    fontSize: 36,
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(height: 12),
              Text(
                widget.userName,
                style: TextStyle(
                  fontSize: settings.headingFontSize,
                  fontWeight: settings.titleFontWeight,
                  color: primaryTextColor,
                ),
              ),
              Container(
                margin: const EdgeInsets.only(top: 6),
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  color: AppTheme.pupLightGold,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  widget.userRole.toUpperCase(),
                  style: const TextStyle(
                    color: AppTheme.pupMaroon,
                    fontWeight: FontWeight.bold,
                    fontSize: 12,
                  ),
                ),
              ),
              const SizedBox(height: 24),

              // Fields Card
              Card(
                color: cardColor,
                elevation: settings.reduceMotion ? 0 : 2,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(
                    settings.cardBorderRadius,
                  ),
                  side: BorderSide(color: settings.borderColor),
                ),
                child: Padding(
                  padding: EdgeInsets.all(settings.cardPadding),
                  child: Column(
                    children: [
                      _buildField(
                        'Full Name',
                        _nameController,
                        Icons.person_outline,
                        primaryTextColor,
                        secondaryTextColor,
                        settings,
                      ),
                      Divider(color: settings.borderColor),
                      if (widget.userRole.toLowerCase() == 'student') ...[
                        _buildField(
                          'Student ID',
                          _studentIdController,
                          Icons.badge_outlined,
                          primaryTextColor,
                          secondaryTextColor,
                          settings,
                        ),
                        Divider(color: settings.borderColor),
                      ] else ...[
                        _buildField(
                          'Staff Type',
                          TextEditingController(text: 'Administrator'),
                          Icons.admin_panel_settings_outlined,
                          primaryTextColor,
                          secondaryTextColor,
                          settings,
                          editable: false,
                        ),
                        Divider(color: settings.borderColor),
                      ],
                      _buildField(
                        'Email / Webmail',
                        _emailController,
                        Icons.email_outlined,
                        primaryTextColor,
                        secondaryTextColor,
                        settings,
                        editable: false,
                      ),
                      Divider(color: settings.borderColor),
                      _buildField(
                        'Department',
                        _departmentController,
                        Icons.account_balance_outlined,
                        primaryTextColor,
                        secondaryTextColor,
                        settings,
                      ),
                    ],
                  ),
                ),
              ),

              const SizedBox(height: 20),
              if (_isEditing)
                ElevatedButton.icon(
                  onPressed: _isLoading ? null : _handleSave,
                  icon: const Icon(Icons.check),
                  label: const Text('Save Changes'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: accentColor,
                    foregroundColor: Colors.white,
                    minimumSize: Size(double.infinity, settings.buttonHeight),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                    ),
                  ),
                )
              else
                OutlinedButton.icon(
                  onPressed: () => setState(() => _isEditing = true),
                  icon: const Icon(Icons.edit_outlined),
                  label: const Text('Edit Profile Information'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: accentColor,
                    side: BorderSide(color: accentColor),
                    minimumSize: Size(double.infinity, settings.buttonHeight),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(
                        settings.cardBorderRadius,
                      ),
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );

    if (widget.isEmbedded) {
      return content;
    }

    return Scaffold(
      backgroundColor: settings.backgroundColor,
      appBar: AppBar(
        title: const Text('My Profile'),
        backgroundColor: settings.headerColor,
      ),
      body: content,
    );
  }

  Widget _buildField(
    String label,
    TextEditingController controller,
    IconData icon,
    Color primaryColor,
    Color secondaryColor,
    AccessibilitySettings settings, {
    bool editable = true,
  }) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon, color: settings.accentColor, size: 22),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: TextStyle(
                    fontSize: settings.smallFontSize,
                    color: secondaryColor,
                    fontWeight: settings.mediumFontWeight,
                  ),
                ),
                const SizedBox(height: 2),
                if (_isEditing && editable)
                  TextField(
                    controller: controller,
                    style: TextStyle(
                      fontSize: settings.bodyFontSize,
                      fontWeight: settings.bodyFontWeight,
                      color: primaryColor,
                    ),
                    decoration: InputDecoration(
                      isDense: true,
                      contentPadding: const EdgeInsets.symmetric(vertical: 4),
                      border: const UnderlineInputBorder(),
                      focusedBorder: UnderlineInputBorder(
                        borderSide: BorderSide(color: settings.accentColor),
                      ),
                    ),
                  )
                else
                  Text(
                    controller.text.isEmpty ? 'Not set' : controller.text,
                    style: TextStyle(
                      fontSize: settings.bodyFontSize,
                      fontWeight: settings.bodyFontWeight,
                      color: primaryColor,
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
