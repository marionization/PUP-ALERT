import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../models/accessibility_settings.dart';
import '../../models/report_model.dart';
import '../../services/auth_service.dart';
import '../../services/report_service.dart';
import '../analytics/analytics_screen.dart';
import '../auth/login_screen.dart';
import '../polls/polls_screen.dart';
import '../profile/profile_screen.dart';
import '../reports/report_detail_screen.dart';
import '../reports/submit_report_screen.dart';
import '../settings/settings_screen.dart';
import '../web/pup_web_portal.dart';
import '../../widgets/report_image_view.dart';

enum DrawerScreen {
  profile('Profile'),
  myReports('My Reports'),
  reports('Reports'),
  dashboard('Dashboard'),
  campusPolls('Student Polls'),
  analytics('Analytics'),
  settings('Settings');

  final String title;
  const DrawerScreen(this.title);
}

class DashboardScreen extends StatefulWidget {
  final String userRole; // 'student' or 'admin'
  final String userName;
  final String firstName;

  const DashboardScreen({
    super.key,
    required this.userRole,
    required this.userName,
    this.firstName = '',
  });

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  final GlobalKey<ScaffoldState> _scaffoldKey = GlobalKey<ScaffoldState>();
  final _authService = AuthService();
  final _reportService = ReportService();
  final _firestore = FirebaseFirestore.instance;

  bool _showWelcomeScreen = true;
  DrawerScreen _selectedScreen = DrawerScreen.dashboard;
  int _reportSelectedTab =
      0; // 0: All, 1: In Review, 2: In Progress, 3: Resolved
  String _selectedCategory = 'All Categories';

  String _studentId = '';
  String _firstName = '';
  AccessibilitySettings _settings = const AccessibilitySettings();

  final List<String> _tabs = ['All', 'In Review', 'In Progress', 'Resolved'];
  final List<String> _categories = [
    'All Categories',
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
    _loadInitialData();

    // 1.8-second Welcome Loading Screen matching Kotlin NextActivity.kt line 149
    Future.delayed(const Duration(milliseconds: 1800), () {
      if (mounted) {
        setState(() => _showWelcomeScreen = false);
      }
    });
  }

  Future<void> _loadInitialData() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _studentId = prefs.getString('student_id') ?? '';
      _firstName = widget.firstName.isNotEmpty
          ? widget.firstName
          : (prefs.getString('student_first_name') ??
                widget.userName.split(' ').first);
      _settings = AccessibilitySettings(
        textSize: prefs.getString('access_text_size') ?? 'Default',
        boldText: prefs.getBool('access_bold_text') ?? false,
        contrastMode: prefs.getString('access_contrast_mode') ?? 'Default',
        reduceMotion: prefs.getBool('access_reduce_motion') ?? false,
        grayscaleMode: prefs.getBool('access_grayscale_mode') ?? false,
        largeButtons: prefs.getBool('access_large_buttons') ?? false,
        simplifiedCards: prefs.getBool('access_simplified_cards') ?? false,
        hideMediaPreview: prefs.getBool('access_hide_media_preview') ?? false,
        twoFactorEnabled: prefs.getBool('security_2fa_enabled') ?? true,
      );
    });
  }

  Future<void> _executeLogout() async {
    await _authService.logout();
    if (!mounted) return;
    Navigator.pushAndRemoveUntil(
      context,
      MaterialPageRoute(builder: (context) => const LoginScreen()),
      (route) => false,
    );
  }

  @override
  Widget build(BuildContext context) {
    final isAdmin = widget.userRole.toLowerCase() == 'admin';

    // 1. Welcome Loading Screen (1.8 seconds) matching Kotlin WelcomeLoadingScreen
    if (_showWelcomeScreen) {
      return Scaffold(
        backgroundColor: const Color(0xFFF5F8F9),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const CircularProgressIndicator(
                color: Color(0xFFE1001B),
                strokeWidth: 4,
              ),
              const SizedBox(height: 24),
              Text(
                isAdmin ? 'HELLO ADMIN!' : 'Hello, $_firstName',
                style: TextStyle(
                  color: const Color(0xFFE1001B),
                  fontSize: isAdmin ? 26 : 22,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 0.5,
                ),
              ),
            ],
          ),
        ),
      );
    }

    final drawerItems = isAdmin
        ? [
            DrawerScreen.profile,
            DrawerScreen.reports,
            DrawerScreen.dashboard,
            DrawerScreen.campusPolls,
            DrawerScreen.analytics,
            DrawerScreen.settings,
          ]
        : [
            DrawerScreen.profile,
            DrawerScreen.myReports,
            DrawerScreen.dashboard,
            DrawerScreen.campusPolls,
            DrawerScreen.settings,
          ];

    final screenTitle = _selectedScreen == DrawerScreen.myReports
        ? 'My Reports'
        : _selectedScreen == DrawerScreen.reports
        ? 'Campus Reports'
        : _selectedScreen.title;

    // Responsive Desktop / Web layout matching PUPSIS portal
    final isWideScreen = MediaQuery.of(context).size.width >= 900;
    if (isWideScreen) {
      return PupWebPortal(
        userRole: widget.userRole,
        userName: widget.userName,
        firstName: _firstName,
        studentId: _studentId,
        settings: _settings,
        onSettingsChange: (updated) {
          setState(() => _settings = updated);
        },
        onLogout: _executeLogout,
      );
    }

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: _settings.backgroundColor,
      // Floating Action Button: ONLY for Student on My Reports screen
      floatingActionButton:
          (!isAdmin && _selectedScreen == DrawerScreen.myReports)
          ? FloatingActionButton(
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) =>
                        SubmitReportScreen(settings: _settings),
                  ),
                );
              },
              backgroundColor: _settings.accentColor,
              shape: const CircleBorder(),
              child: Text(
                '+',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: _settings.largeButtons ? 32 : 28,
                  fontWeight: FontWeight.bold,
                ),
              ),
            )
          : null,
      drawer: _buildNavigationDrawer(drawerItems, isAdmin),
      body: SafeArea(
        child: Column(
          children: [
            // Top Header matching Kotlin TopHeaderWithMenu
            _buildTopHeader(screenTitle, isAdmin),

            // Main Body: in-place screen rendering
            Expanded(child: _buildCurrentScreen(isAdmin)),
          ],
        ),
      ),
    );
  }

  // --- Top Header matching Kotlin TopHeaderWithMenu ---
  Widget _buildTopHeader(String screenTitle, bool isAdmin) {
    return StreamBuilder<QuerySnapshot>(
      stream: _firestore.collection('notifications').snapshots(),
      builder: (context, snapshot) {
        int unreadCount = 0;
        if (snapshot.hasData) {
          final currentUid = _authService.currentUser?.uid ?? '';
          final docs = snapshot.data!.docs;
          unreadCount = docs.where((doc) {
            final data = doc.data() as Map<String, dynamic>;
            final read = data['read'] ?? false;
            final targetRole =
                data['targetRole']?.toString().toLowerCase() ?? '';
            final targetUser = data['targetUser']?.toString() ?? '';
            final targetUid = data['targetUid']?.toString() ?? '';

            if (read) return false;
            if (isAdmin) {
              return targetRole == 'admin' || targetRole == 'administrator';
            } else {
              return targetRole == 'student' &&
                  (targetUser == widget.userName ||
                      (currentUid.isNotEmpty && targetUid == currentUid));
            }
          }).length;
        }

        return Container(
          width: double.infinity,
          color: _settings.headerColor,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
          child: Row(
            children: [
              IconButton(
                icon: Icon(
                  Icons.menu,
                  color: Colors.white,
                  size: _settings.largeButtons ? 28 : 24,
                ),
                onPressed: () => _scaffoldKey.currentState?.openDrawer(),
              ),
              const SizedBox(width: 4),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      screenTitle,
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: _settings.titleFontSize,
                        fontWeight: _settings.titleFontWeight,
                      ),
                    ),
                    Text(
                      isAdmin ? 'Hello Admin' : 'Hello, $_firstName',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.95),
                        fontSize: _settings.bodyFontSize - 1,
                        fontWeight: _settings.bodyFontWeight,
                      ),
                    ),
                    if (!isAdmin && _studentId.isNotEmpty)
                      Text(
                        _studentId,
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.85),
                          fontSize: _settings.smallFontSize,
                        ),
                      ),
                  ],
                ),
              ),
              IconButton(
                icon: unreadCount > 0
                    ? Badge(
                        label: Text(
                          unreadCount > 9 ? '9+' : '$unreadCount',
                          style: TextStyle(
                            fontSize: 10,
                            color: _settings.headerColor,
                          ),
                        ),
                        backgroundColor: Colors.white,
                        child: Icon(
                          Icons.notifications,
                          color: Colors.white,
                          size: _settings.largeButtons ? 28 : 24,
                        ),
                      )
                    : Icon(
                        Icons.notifications,
                        color: Colors.white,
                        size: _settings.largeButtons ? 28 : 24,
                      ),
                onPressed: _showNotificationsDialogCard,
              ),
            ],
          ),
        );
      },
    );
  }

  // --- Side Navigation Drawer matching Kotlin DrawerContent ---
  Widget _buildNavigationDrawer(List<DrawerScreen> drawerItems, bool isAdmin) {
    return Drawer(
      backgroundColor: _settings.grayscaleMode
          ? const Color(0xFFF2F2F2)
          : _settings.cardColor,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'PUP Parañaque Campus',
                    style: TextStyle(
                      fontSize: _settings.titleFontSize + 2,
                      fontWeight: _settings.titleFontWeight,
                      color: _settings.accentColor,
                    ),
                  ),
                  Text(
                    'Report Monitoring System',
                    style: TextStyle(
                      fontSize: _settings.smallFontSize,
                      color: _settings.secondaryTextColor,
                    ),
                  ),
                  const SizedBox(height: 18),
                  Text(
                    isAdmin ? 'Staff Menu' : 'Student Menu',
                    style: TextStyle(
                      fontSize: _settings.bodyFontSize + 1,
                      fontWeight: _settings.mediumFontWeight,
                      color: _settings.accentColor,
                    ),
                  ),
                  const SizedBox(height: 12),

                  // User Profile Info Card
                  Container(
                    width: double.infinity,
                    padding: EdgeInsets.all(_settings.cardPadding),
                    decoration: BoxDecoration(
                      color: _settings.contrastMode == 'Dark Contrast'
                          ? const Color(0xFF1E1E1E)
                          : const Color(0xFFF8F8F8),
                      borderRadius: BorderRadius.circular(
                        _settings.cardBorderRadius,
                      ),
                      border: Border.all(color: _settings.borderColor),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          isAdmin ? 'Administrator' : widget.userName,
                          style: TextStyle(
                            fontSize: _settings.bodyFontSize,
                            fontWeight: _settings.titleFontWeight,
                            color: _settings.primaryTextColor,
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          isAdmin ? 'Administrator' : 'Student',
                          style: TextStyle(
                            fontSize: _settings.smallFontSize,
                            color: _settings.secondaryTextColor,
                          ),
                        ),
                        if (!isAdmin && _studentId.isNotEmpty) ...[
                          const SizedBox(height: 2),
                          Text(
                            _studentId,
                            style: TextStyle(
                              fontSize: _settings.smallFontSize,
                              color: _settings.secondaryTextColor,
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),

            // Navigation Items List
            Expanded(
              child: ListView(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 8,
                ),
                children: [
                  ...drawerItems.map((item) {
                    final isSelected = _selectedScreen == item;
                    return Container(
                      margin: const EdgeInsets.symmetric(vertical: 2),
                      decoration: BoxDecoration(
                        color: isSelected
                            ? (_settings.grayscaleMode
                                  ? const Color(0xFFE0E0E0)
                                  : const Color(0xFFFFEBEE))
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(
                          _settings.cardBorderRadius,
                        ),
                      ),
                      child: ListTile(
                        leading: Icon(
                          _getDrawerIcon(item),
                          color: isSelected
                              ? _settings.accentColor
                              : _settings.secondaryTextColor,
                          size: _settings.largeButtons ? 26 : 22,
                        ),
                        title: Text(
                          item.title,
                          style: TextStyle(
                            fontSize: _settings.bodyFontSize,
                            fontWeight: isSelected
                                ? FontWeight.bold
                                : _settings.bodyFontWeight,
                            color: isSelected
                                ? _settings.accentColor
                                : _settings.primaryTextColor,
                          ),
                        ),
                        dense: !_settings.largeButtons,
                        onTap: () {
                          setState(() => _selectedScreen = item);
                          Navigator.pop(context);
                        },
                      ),
                    );
                  }),
                  const Divider(height: 16),
                  ListTile(
                    leading: const Icon(Icons.logout, color: Colors.red),
                    title: Text(
                      'Sign Out',
                      style: TextStyle(
                        fontSize: _settings.bodyFontSize,
                        color: Colors.red,
                        fontWeight: _settings.mediumFontWeight,
                      ),
                    ),
                    dense: !_settings.largeButtons,
                    onTap: () {
                      Navigator.pop(context);
                      _showLogoutConfirmDialog();
                    },
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  IconData _getDrawerIcon(DrawerScreen screen) {
    switch (screen) {
      case DrawerScreen.profile:
        return Icons.person;
      case DrawerScreen.myReports:
      case DrawerScreen.reports:
        return Icons.assignment;
      case DrawerScreen.dashboard:
        return Icons.dashboard;
      case DrawerScreen.campusPolls:
        return Icons.how_to_vote;
      case DrawerScreen.analytics:
        return Icons.assessment;
      case DrawerScreen.settings:
        return Icons.settings;
    }
  }

  // --- Dynamic Screen Switcher matching Kotlin when (selectedScreen) ---
  Widget _buildCurrentScreen(bool isAdmin) {
    switch (_selectedScreen) {
      case DrawerScreen.profile:
        return ProfileScreen(
          userRole: widget.userRole,
          userName: widget.userName,
          firstName: _firstName,
          studentId: _studentId,
          isEmbedded: true,
          settings: _settings,
        );

      case DrawerScreen.myReports:
        // Student's reports only
        return _buildReportsScreen(onlyMyReports: true, isAdmin: false);

      case DrawerScreen.reports:
        // Admin views ALL reports across campus
        return _buildReportsScreen(onlyMyReports: false, isAdmin: true);

      case DrawerScreen.dashboard:
        return _buildDashboardContent(isAdmin);

      case DrawerScreen.campusPolls:
        return CampusPollsScreen(
          userRole: widget.userRole,
          isEmbedded: true,
          settings: _settings,
        );

      case DrawerScreen.analytics:
        return AnalyticsScreen(
          isEmbedded: true,
          settings: _settings,
          userRole: widget.userRole,
        );

      case DrawerScreen.settings:
        return SettingsScreen(
          userRole: widget.userRole,
          settings: _settings,
          onSettingsChange: (updated) {
            setState(() => _settings = updated);
          },
        );
    }
  }

  // --- Dashboard Summary Screen matching Kotlin DashboardScreen & SummaryCard ---
  Widget _buildDashboardContent(bool isAdmin) {
    return StreamBuilder<List<Report>>(
      stream: _reportService.getReportsStream(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(
            child: CircularProgressIndicator(color: Color(0xFFE1001B)),
          );
        }

        final currentUid = _authService.currentUser?.uid ?? '';
        final all = snapshot.data ?? [];

        // Filter list: Student sees only their own reports, Admin sees all reports
        final reports = !isAdmin
            ? all.where((r) {
                if (currentUid.isNotEmpty && r.reporterUid == currentUid)
                  return true;
                if (_studentId.isNotEmpty &&
                    r.reporterStudentId.isNotEmpty &&
                    r.reporterStudentId == _studentId)
                  return true;
                if (r.reporter.isNotEmpty &&
                    (r.reporter == widget.userName || r.reporter == _firstName))
                  return true;
                if (r.reporterUid.isNotEmpty &&
                    (r.reporterUid == currentUid ||
                        r.reporterUid == _studentId))
                  return true;
                return false;
              }).toList()
            : all;

        final totalCount = reports.length;
        final pendingCount = reports.countWhere((r) => r.status == 'In Review');
        final inProgressCount = reports.countWhere(
          (r) => r.status == 'In Progress',
        );
        final resolvedCount = reports.countWhere((r) => r.status == 'Resolved');

        double avgRating = 0.0;
        int ratedCount = 0;
        for (var r in reports) {
          if (r.ratingCount > 0) {
            avgRating += (r.averageRating * r.ratingCount);
            ratedCount += r.ratingCount;
          }
        }
        if (ratedCount > 0) avgRating /= ratedCount;

        return SingleChildScrollView(
          padding: EdgeInsets.all(_settings.cardPadding),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                !isAdmin ? 'My Report Summary' : 'All Reports Summary',
                style: TextStyle(
                  fontSize: _settings.headingFontSize,
                  fontWeight: _settings.titleFontWeight,
                  color: _settings.accentColor,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                !isAdmin
                    ? 'Tap a card to view your filtered reports'
                    : 'Tap a card to view filtered campus reports',
                style: TextStyle(
                  fontSize: _settings.bodyFontSize,
                  color: _settings.secondaryTextColor,
                  fontWeight: _settings.bodyFontWeight,
                ),
              ),
              const SizedBox(height: 18),

              // 4 Summary Metric Cards matching Kotlin SummaryCard
              Row(
                children: [
                  Expanded(
                    child: _buildSummaryCard(
                      title: 'Total Reports',
                      value: '$totalCount',
                      accentColor: _settings.grayscaleMode
                          ? Colors.black54
                          : const Color(0xFFE1001B),
                      onTap: () => _goToReportsTab(0, isAdmin),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _buildSummaryCard(
                      title: 'In Review',
                      value: '$pendingCount',
                      accentColor: _settings.grayscaleMode
                          ? Colors.grey
                          : const Color(0xFFFF9800),
                      onTap: () => _goToReportsTab(1, isAdmin),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: _buildSummaryCard(
                      title: 'In Progress',
                      value: '$inProgressCount',
                      accentColor: _settings.grayscaleMode
                          ? Colors.grey
                          : const Color(0xFF1976D2),
                      onTap: () => _goToReportsTab(2, isAdmin),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _buildSummaryCard(
                      title: 'Resolved',
                      value: '$resolvedCount',
                      accentColor: _settings.grayscaleMode
                          ? Colors.black54
                          : const Color(0xFF2E7D32),
                      onTap: () => _goToReportsTab(3, isAdmin),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),

              // Satisfaction Rating Card matching Kotlin RatingSummaryCard
              Card(
                color: _settings.cardColor,
                elevation: _settings.reduceMotion ? 0 : 2,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(
                    _settings.cardBorderRadius,
                  ),
                  side: BorderSide(color: _settings.borderColor),
                ),
                child: Padding(
                  padding: EdgeInsets.all(_settings.cardPadding),
                  child: Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: _settings.grayscaleMode
                              ? Colors.grey.shade200
                              : const Color(0xFFFFF8E1),
                        ),
                        child: const Icon(
                          Icons.star,
                          color: Colors.amber,
                          size: 32,
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Resolution Satisfaction Rating',
                              style: TextStyle(
                                fontSize: _settings.bodyFontSize,
                                fontWeight: _settings.mediumFontWeight,
                                color: _settings.primaryTextColor,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              ratedCount > 0
                                  ? '${avgRating.toStringAsFixed(1)} / 5.0 ($ratedCount feedback ratings)'
                                  : 'No ratings submitted yet',
                              style: TextStyle(
                                fontSize: _settings.smallFontSize,
                                color: _settings.secondaryTextColor,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),

              // Admin-Exclusive Analytics & Report Generation Banner
              if (isAdmin) ...[
                const SizedBox(height: 16),
                InkWell(
                  onTap: () =>
                      setState(() => _selectedScreen = DrawerScreen.analytics),
                  borderRadius: BorderRadius.circular(
                    _settings.cardBorderRadius,
                  ),
                  child: Container(
                    width: double.infinity,
                    padding: EdgeInsets.all(_settings.cardPadding),
                    decoration: BoxDecoration(
                      color: _settings.grayscaleMode
                          ? const Color(0xFFE0E0E0)
                          : _settings.accentColor.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(
                        _settings.cardBorderRadius,
                      ),
                      border: Border.all(
                        color: _settings.grayscaleMode
                            ? Colors.grey
                            : _settings.accentColor.withValues(alpha: 0.3),
                      ),
                    ),
                    child: Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: _settings.grayscaleMode
                                ? Colors.white
                                : _settings.accentColor,
                            shape: BoxShape.circle,
                          ),
                          child: Icon(
                            Icons.analytics_outlined,
                            color: _settings.grayscaleMode
                                ? Colors.black87
                                : Colors.white,
                            size: _settings.largeButtons ? 26 : 22,
                          ),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Campus Analytics & Reports (Admin Exclusive)',
                                style: TextStyle(
                                  fontSize: _settings.bodyFontSize,
                                  fontWeight: FontWeight.bold,
                                  color: _settings.accentColor,
                                ),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Generate printable PDF reports & download Excel/CSV dataset',
                                style: TextStyle(
                                  fontSize: _settings.smallFontSize,
                                  color: _settings.secondaryTextColor,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Icon(
                          Icons.arrow_forward_ios,
                          size: 14,
                          color: _settings.accentColor,
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  void _goToReportsTab(int tabIndex, bool isAdmin) {
    setState(() {
      _reportSelectedTab = tabIndex;
      _selectedScreen = isAdmin ? DrawerScreen.reports : DrawerScreen.myReports;
    });
  }

  Widget _buildSummaryCard({
    required String title,
    required String value,
    required Color accentColor,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(_settings.cardBorderRadius),
      child: Card(
        color: _settings.cardColor,
        elevation: _settings.reduceMotion ? 0 : 2,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(_settings.cardBorderRadius),
          side: BorderSide(color: _settings.borderColor),
        ),
        child: Padding(
          padding: EdgeInsets.all(_settings.cardPadding),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                title,
                style: TextStyle(
                  fontSize: _settings.smallFontSize,
                  fontWeight: _settings.mediumFontWeight,
                  color: _settings.secondaryTextColor,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                value,
                style: TextStyle(
                  fontSize: _settings.headingFontSize + 4,
                  fontWeight: FontWeight.bold,
                  color: accentColor,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  // --- Reports Screen matching Kotlin ReportsScreen, FiltersSection, StatusTabs, ReportList ---
  Widget _buildReportsScreen({
    required bool onlyMyReports,
    required bool isAdmin,
  }) {
    final currentStatus = _tabs[_reportSelectedTab];

    return Column(
      children: [
        // Category Filter Dropdown (FiltersSection)
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              Text(
                'Category:',
                style: TextStyle(
                  fontSize: _settings.smallFontSize,
                  fontWeight: _settings.mediumFontWeight,
                  color: _settings.primaryTextColor,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  decoration: BoxDecoration(
                    color: _settings.cardColor,
                    borderRadius: BorderRadius.circular(
                      _settings.cardBorderRadius,
                    ),
                    border: Border.all(color: _settings.borderColor),
                  ),
                  child: DropdownButton<String>(
                    value: _selectedCategory,
                    isExpanded: true,
                    dropdownColor: _settings.cardColor,
                    underline: const SizedBox(),
                    style: TextStyle(
                      fontSize: _settings.bodyFontSize,
                      color: _settings.primaryTextColor,
                      fontWeight: _settings.mediumFontWeight,
                    ),
                    items: _categories
                        .map((c) => DropdownMenuItem(value: c, child: Text(c)))
                        .toList(),
                    onChanged: (val) {
                      if (val != null) setState(() => _selectedCategory = val);
                    },
                  ),
                ),
              ),
            ],
          ),
        ),

        // Status Tabs matching Kotlin StatusTabs
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Row(
            children: List.generate(_tabs.length, (idx) {
              final isSelected = _reportSelectedTab == idx;
              return Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: ChoiceChip(
                  label: Text(_tabs[idx]),
                  selected: isSelected,
                  selectedColor: _settings.accentColor,
                  labelStyle: TextStyle(
                    fontSize: _settings.smallFontSize,
                    color: isSelected
                        ? Colors.white
                        : _settings.primaryTextColor,
                    fontWeight: isSelected
                        ? FontWeight.bold
                        : _settings.bodyFontWeight,
                  ),
                  backgroundColor: _settings.cardColor,
                  onSelected: (_) => setState(() => _reportSelectedTab = idx),
                ),
              );
            }),
          ),
        ),
        const SizedBox(height: 8),

        // Reports List matching Kotlin ReportList & ReportItem
        Expanded(
          child: StreamBuilder<List<Report>>(
            stream: _reportService.getReportsStream(),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(
                  child: CircularProgressIndicator(color: Color(0xFFE1001B)),
                );
              }

              if (snapshot.hasError) {
                return Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(
                          Icons.error_outline,
                          size: 54,
                          color: Colors.red,
                        ),
                        const SizedBox(height: 12),
                        Text(
                          'Error loading reports: ${snapshot.error}',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            color: _settings.secondaryTextColor,
                            fontSize: _settings.bodyFontSize,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              }

              final currentUid = _authService.currentUser?.uid ?? '';
              var list = snapshot.data ?? [];

              // Filter by student account ownership if onlyMyReports
              if (onlyMyReports) {
                list = list.where((r) {
                  if (currentUid.isNotEmpty && r.reporterUid == currentUid)
                    return true;
                  if (_studentId.isNotEmpty &&
                      r.reporterStudentId.isNotEmpty &&
                      r.reporterStudentId == _studentId)
                    return true;
                  if (!r.isAnonymous &&
                      (r.reporter == widget.userName ||
                          r.reporter == _firstName))
                    return true;
                  if (r.reporter == widget.userName) return true;
                  if (r.reporterUid.isNotEmpty &&
                      (r.reporterUid == currentUid ||
                          r.reporterUid == _studentId))
                    return true;
                  return false;
                }).toList();
              }

              // Filter by tab status
              if (currentStatus != 'All') {
                list = list.where((r) => r.status == currentStatus).toList();
              }

              // Filter by category
              if (_selectedCategory != 'All Categories') {
                list = list
                    .where((r) => r.category == _selectedCategory)
                    .toList();
              }

              if (list.isEmpty) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.assignment_outlined,
                        size: 54,
                        color: _settings.secondaryTextColor,
                      ),
                      const SizedBox(height: 10),
                      Text(
                        onlyMyReports
                            ? 'You have not submitted any reports matching this filter.'
                            : 'No campus reports found for this filter.',
                        style: TextStyle(
                          color: _settings.secondaryTextColor,
                          fontSize: _settings.bodyFontSize,
                        ),
                      ),
                    ],
                  ),
                );
              }

              return ListView.builder(
                padding: EdgeInsets.symmetric(
                  horizontal: _settings.cardPadding,
                  vertical: 8,
                ),
                itemCount: list.length,
                itemBuilder: (context, index) {
                  final report = list[index];
                  return _buildReportItem(report, isAdmin);
                },
              );
            },
          ),
        ),
      ],
    );
  }

  // --- Report Item Card matching Kotlin ReportItem ---
  Widget _buildReportItem(Report report, bool isAdmin) {
    Color statusColor;
    switch (report.status) {
      case 'In Review':
        statusColor = const Color(0xFFFF9800);
        break;
      case 'In Progress':
        statusColor = const Color(0xFF1976D2);
        break;
      case 'Resolved':
        statusColor = const Color(0xFF2E7D32);
        break;
      default:
        statusColor = Colors.grey;
    }

    return Card(
      color: _settings.cardColor,
      elevation: _settings.reduceMotion ? 0 : 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(_settings.cardBorderRadius),
        side: BorderSide(color: _settings.borderColor),
      ),
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        borderRadius: BorderRadius.circular(_settings.cardBorderRadius),
        onTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) =>
                  ReportDetailScreen(report: report, userRole: widget.userRole),
            ),
          );
        },
        child: Padding(
          padding: EdgeInsets.all(_settings.cardPadding),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Title & Status Badge
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      report.title,
                      style: TextStyle(
                        fontSize: _settings.titleFontSize,
                        fontWeight: _settings.titleFontWeight,
                        color: _settings.primaryTextColor,
                      ),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 3,
                    ),
                    decoration: BoxDecoration(
                      color: statusColor.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: statusColor),
                    ),
                    child: Text(
                      report.status,
                      style: TextStyle(
                        fontSize: _settings.smallFontSize,
                        fontWeight: FontWeight.bold,
                        color: statusColor,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),

              // Category & Location
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 6,
                      vertical: 2,
                    ),
                    decoration: BoxDecoration(
                      color: _settings.accentColor.withValues(alpha: 0.08),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Text(
                      report.category,
                      style: TextStyle(
                        fontSize: _settings.smallFontSize - 1,
                        color: _settings.accentColor,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Icon(
                    Icons.location_on,
                    size: 14,
                    color: _settings.secondaryTextColor,
                  ),
                  const SizedBox(width: 2),
                  Expanded(
                    child: Text(
                      report.location,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: _settings.smallFontSize,
                        color: _settings.secondaryTextColor,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),

              // Description snippet
              Text(
                report.description,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontSize: _settings.bodyFontSize - 1,
                  color: _settings.primaryTextColor,
                  fontWeight: _settings.bodyFontWeight,
                ),
              ),

              // Media Thumbnail (if not hidden by hideMediaPreview setting)
              if (!_settings.hideMediaPreview && report.hasImage) ...[
                const SizedBox(height: 10),
                ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: ReportImageView(
                    imageUrl: report.displayImageUrl,
                    height: 140,
                    width: double.infinity,
                    fit: BoxFit.cover,
                  ),
                ),
              ],
              const SizedBox(height: 10),

              // Footer: Reporter name / Anonymous & Rating
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Reported by: ${report.isAnonymous ? "Anonymous Student" : report.reporter}',
                    style: TextStyle(
                      fontSize: _settings.smallFontSize - 1,
                      color: _settings.secondaryTextColor,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                  if (report.ratingCount > 0)
                    Row(
                      children: [
                        const Icon(Icons.star, color: Colors.amber, size: 14),
                        const SizedBox(width: 2),
                        Text(
                          report.averageRating.toStringAsFixed(1),
                          style: TextStyle(
                            fontSize: _settings.smallFontSize,
                            fontWeight: FontWeight.bold,
                            color: _settings.primaryTextColor,
                          ),
                        ),
                      ],
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  // --- Logout Confirmation Dialog matching Kotlin LogoutConfirmDialogCard ---
  void _showLogoutConfirmDialog() {
    final isAdmin = widget.userRole.toLowerCase() == 'admin';

    showDialog(
      context: context,
      builder: (ctx) {
        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  isAdmin ? '🔒' : '👋',
                  style: const TextStyle(fontSize: 34),
                ),
                const SizedBox(height: 12),
                Text(
                  isAdmin ? 'Logout now?' : 'Are you sure?',
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  isAdmin
                      ? 'Do you want to end this admin session?'
                      : 'Are you sure you want to log out?',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Color(0xFF666666),
                  ),
                ),
                const SizedBox(height: 24),
                Row(
                  children: [
                    Expanded(
                      child: TextButton(
                        onPressed: () => Navigator.pop(ctx),
                        style: TextButton.styleFrom(
                          backgroundColor: const Color(0xFFF3F3F3),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                          padding: const EdgeInsets.symmetric(vertical: 10),
                        ),
                        child: const Text(
                          'No',
                          style: TextStyle(
                            color: Color(0xFF444444),
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextButton(
                        onPressed: () {
                          Navigator.pop(ctx);
                          _showGoodbyeDialogCard();
                        },
                        style: TextButton.styleFrom(
                          backgroundColor: const Color(0xFFE1001B),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(14),
                          ),
                          padding: const EdgeInsets.symmetric(vertical: 10),
                        ),
                        child: const Text(
                          'Yes',
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  // --- Goodbye Dialog matching Kotlin GoodbyeDialogCard ---
  void _showGoodbyeDialogCard() {
    final isAdmin = widget.userRole.toLowerCase() == 'admin';

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        Future.delayed(const Duration(milliseconds: 1200), () {
          if (ctx.mounted) {
            Navigator.pop(ctx);
            _executeLogout();
          }
        });

        return Dialog(
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(24),
          ),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 30),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  isAdmin ? '✅' : '👋',
                  style: const TextStyle(fontSize: 34),
                ),
                const SizedBox(height: 12),
                Text(
                  isAdmin ? 'See you again' : 'Goodbye, see you again',
                  style: const TextStyle(
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                    color: Color(0xFFE1001B),
                  ),
                ),
                const SizedBox(height: 8),
                const Text(
                  'Logging out...',
                  style: TextStyle(fontSize: 14, color: Color(0xFF666666)),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  // --- Notifications Dialog matching Kotlin NotificationsDialogCard ---
  void _showNotificationsDialogCard() {
    final isAdmin = widget.userRole.toLowerCase() == 'admin';
    final currentUid = _authService.currentUser?.uid ?? '';

    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          title: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.notifications_active,
                    color: _settings.accentColor,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Notifications',
                    style: TextStyle(
                      fontSize: _settings.titleFontSize,
                      fontWeight: _settings.titleFontWeight,
                      color: _settings.primaryTextColor,
                    ),
                  ),
                ],
              ),
              TextButton(
                onPressed: () async {
                  final snap = await _firestore
                      .collection('notifications')
                      .get();
                  for (var doc in snap.docs) {
                    await doc.reference.update({'read': true});
                  }
                },
                child: Text(
                  'Mark all read',
                  style: TextStyle(
                    fontSize: _settings.smallFontSize,
                    color: _settings.accentColor,
                  ),
                ),
              ),
            ],
          ),
          content: SizedBox(
            width: double.maxFinite,
            height: 380,
            child: StreamBuilder<QuerySnapshot>(
              stream: _firestore
                  .collection('notifications')
                  .orderBy('timestamp', descending: true)
                  .snapshots(),
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const Center(child: CircularProgressIndicator());
                }

                final docs = (snapshot.data?.docs ?? []).filterNotifications(
                  isAdmin: isAdmin,
                  userName: widget.userName,
                  currentUid: currentUid,
                );

                if (docs.isEmpty) {
                  return const Center(child: Text('No notifications yet.'));
                }

                return ListView.separated(
                  itemCount: docs.length,
                  separatorBuilder: (_, _) => const Divider(height: 1),
                  itemBuilder: (context, i) {
                    final data = docs[i].data() as Map<String, dynamic>;
                    final isRead = data['read'] ?? false;
                    final reportId = data['reportId']?.toString() ?? '';

                    return ListTile(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 4,
                        vertical: 2,
                      ),
                      leading: CircleAvatar(
                        radius: 16,
                        backgroundColor: isRead
                            ? Colors.grey.shade200
                            : _settings.accentColor.withValues(alpha: 0.15),
                        child: Icon(
                          Icons.notifications_outlined,
                          size: 18,
                          color: isRead ? Colors.grey : _settings.accentColor,
                        ),
                      ),
                      title: Text(
                        data['title'] ?? 'Incident Notification',
                        style: TextStyle(
                          fontSize: _settings.bodyFontSize - 1,
                          fontWeight: isRead
                              ? FontWeight.normal
                              : FontWeight.bold,
                          color: _settings.primaryTextColor,
                        ),
                      ),
                      subtitle: Text(
                        data['message'] ?? '',
                        style: TextStyle(
                          fontSize: _settings.smallFontSize - 1,
                          color: _settings.secondaryTextColor,
                        ),
                      ),
                      trailing: IconButton(
                        icon: const Icon(
                          Icons.delete_outline,
                          size: 18,
                          color: Colors.grey,
                        ),
                        onPressed: () => docs[i].reference.delete(),
                      ),
                      onTap: () async {
                        await docs[i].reference.update({'read': true});
                        if (reportId.isNotEmpty) {
                          final doc = await _firestore
                              .collection('reports')
                              .doc(reportId)
                              .get();
                          if (ctx.mounted) {
                            Navigator.pop(ctx);
                          }
                          if (!mounted) return;
                          Navigator.push(
                            this.context,
                            MaterialPageRoute(
                              builder: (context) => ReportDetailScreen(
                                report: Report.fromDoc(doc),
                                userRole: widget.userRole,
                              ),
                            ),
                          );
                        }
                      },
                    );
                  },
                );
              },
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Close'),
            ),
          ],
        );
      },
    );
  }
}

// Extension helper for notifications filtering matching Kotlin filter
extension NotificationFilter on List<QueryDocumentSnapshot> {
  List<QueryDocumentSnapshot> filterNotifications({
    required bool isAdmin,
    required String userName,
    required String currentUid,
  }) {
    return where((doc) {
      final data = doc.data() as Map<String, dynamic>;
      final targetRole = data['targetRole']?.toString().toLowerCase() ?? '';
      final targetUser = data['targetUser']?.toString() ?? '';
      final targetUid = data['targetUid']?.toString() ?? '';

      if (isAdmin) {
        return targetRole == 'admin' || targetRole == 'administrator';
      } else {
        return targetRole == 'student' &&
            (targetUser == userName ||
                (currentUid.isNotEmpty && targetUid == currentUid));
      }
    }).toList();
  }
}

// Extension helper for counting reports matching Kotlin countWhere
extension ReportCount on List<Report> {
  int countWhere(bool Function(Report) predicate) {
    int count = 0;
    for (var r in this) {
      if (predicate(r)) count++;
    }
    return count;
  }
}
