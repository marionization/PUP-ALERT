import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../models/accessibility_settings.dart';
import '../../models/report_model.dart';
import '../../services/auth_service.dart';
import '../../services/report_service.dart';
import '../analytics/analytics_screen.dart';
import '../polls/polls_screen.dart';
import '../reports/report_detail_screen.dart';
import '../reports/submit_report_screen.dart';
import '../settings/settings_screen.dart';
import '../../widgets/report_image_view.dart';

class PupWebPortal extends StatefulWidget {
  final String userRole;
  final String userName;
  final String firstName;
  final String studentId;
  final AccessibilitySettings settings;
  final ValueChanged<AccessibilitySettings> onSettingsChange;
  final VoidCallback onLogout;

  const PupWebPortal({
    super.key,
    required this.userRole,
    required this.userName,
    required this.firstName,
    required this.studentId,
    required this.settings,
    required this.onSettingsChange,
    required this.onLogout,
  });

  @override
  State<PupWebPortal> createState() => _PupWebPortalState();
}

enum WebMainTab { home, reports, polls, analytics, settings }

class _PupWebPortalState extends State<PupWebPortal> {
  final _reportService = ReportService();
  final _authService = AuthService();
  final _firestore = FirebaseFirestore.instance;

  WebMainTab _activeTab = WebMainTab.home;
  int _selectedSubNavIndex =
      0; // 0: Reports Inbox, 1: Action (Submit / Analytics), 2: Polls
  int _selectedStatusTab =
      0; // 0: All, 1: In Review, 2: In Progress, 3: Resolved
  String _selectedCategory = 'All Categories';

  final List<String> _statusTabs = [
    'All',
    'In Review',
    'In Progress',
    'Resolved',
  ];
  final List<String> _categories = [
    'All Categories',
    'Facilities',
    'Maintenance',
    'Safety',
    'Cleanliness',
    'Equipment',
    'Other',
  ];

  void _showAccessibilityDialog() {
    showDialog(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return Dialog(
              backgroundColor: widget.settings.cardColor,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
                side: BorderSide(color: widget.settings.borderColor),
              ),
              child: Container(
                width: 500,
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: widget.settings.cardColor,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: widget.settings.accentColor,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.accessibility_new,
                            color: Colors.white,
                            size: 24,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'PUPALERT Accessibility Tool',
                                style: TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                  color: widget.settings.primaryTextColor,
                                ),
                              ),
                              Text(
                                'Customize your visual and display comfort',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: widget.settings.secondaryTextColor,
                                ),
                              ),
                            ],
                          ),
                        ),
                        IconButton(
                          icon: Icon(
                            Icons.close,
                            color: widget.settings.primaryTextColor,
                          ),
                          onPressed: () => Navigator.pop(ctx),
                        ),
                      ],
                    ),
                    Divider(height: 24, color: widget.settings.borderColor),
                    Text(
                      'Text Size',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: widget.settings.primaryTextColor,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 8,
                      children: ['Small', 'Default', 'Large', 'Extra Large']
                          .map((s) {
                            final isSel = widget.settings.textSize == s;
                            return ChoiceChip(
                              label: Text(s),
                              selected: isSel,
                              selectedColor: widget.settings.accentColor,
                              backgroundColor:
                                  widget.settings.contrastMode ==
                                      'Dark Contrast'
                                  ? const Color(0xFF2C2C2C)
                                  : const Color(0xFFF3F4F6),
                              labelStyle: TextStyle(
                                color: isSel
                                    ? Colors.white
                                    : widget.settings.primaryTextColor,
                              ),
                              onSelected: (val) {
                                if (val) {
                                  final updated = widget.settings.copyWith(
                                    textSize: s,
                                  );
                                  widget.onSettingsChange(updated);
                                  setModalState(() {});
                                  setState(() {});
                                }
                              },
                            );
                          })
                          .toList(),
                    ),
                    const SizedBox(height: 14),
                    Text(
                      'Contrast Mode',
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: widget.settings.primaryTextColor,
                      ),
                    ),
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 8,
                      children:
                          [
                            'Default',
                            'High Contrast',
                            'Dark Contrast',
                            'Light Contrast',
                          ].map((c) {
                            final isSel = widget.settings.contrastMode == c;
                            return ChoiceChip(
                              label: Text(c),
                              selected: isSel,
                              selectedColor: widget.settings.accentColor,
                              backgroundColor:
                                  widget.settings.contrastMode ==
                                      'Dark Contrast'
                                  ? const Color(0xFF2C2C2C)
                                  : const Color(0xFFF3F4F6),
                              labelStyle: TextStyle(
                                color: isSel
                                    ? Colors.white
                                    : widget.settings.primaryTextColor,
                              ),
                              onSelected: (val) {
                                if (val) {
                                  final updated = widget.settings.copyWith(
                                    contrastMode: c,
                                  );
                                  widget.onSettingsChange(updated);
                                  setModalState(() {});
                                  setState(() {});
                                }
                              },
                            );
                          }).toList(),
                    ),
                    const SizedBox(height: 14),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text(
                        'Bold Text',
                        style: TextStyle(
                          fontSize: 14,
                          color: widget.settings.primaryTextColor,
                        ),
                      ),
                      value: widget.settings.boldText,
                      activeThumbColor: widget.settings.accentColor,
                      onChanged: (val) {
                        final updated = widget.settings.copyWith(boldText: val);
                        widget.onSettingsChange(updated);
                        setModalState(() {});
                        setState(() {});
                      },
                    ),
                    SwitchListTile(
                      contentPadding: EdgeInsets.zero,
                      title: Text(
                        'Grayscale Display',
                        style: TextStyle(
                          fontSize: 14,
                          color: widget.settings.primaryTextColor,
                        ),
                      ),
                      value: widget.settings.grayscaleMode,
                      activeThumbColor: widget.settings.accentColor,
                      onChanged: (val) {
                        final updated = widget.settings.copyWith(
                          grayscaleMode: val,
                        );
                        widget.onSettingsChange(updated);
                        setModalState(() {});
                        setState(() {});
                      },
                    ),
                    const SizedBox(height: 16),
                    SizedBox(
                      width: double.infinity,
                      child: ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: widget.settings.accentColor,
                          foregroundColor: Colors.white,
                        ),
                        onPressed: () => Navigator.pop(ctx),
                        child: const Text('Done'),
                      ),
                    ),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }

  void _openReportDetail(Report report, bool isAdmin) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) =>
            ReportDetailScreen(report: report, userRole: widget.userRole),
      ),
    );
  }

  void _openSubmitReport() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => SubmitReportScreen(settings: widget.settings),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isAdmin =
        widget.userRole.toLowerCase() == 'admin' ||
        widget.userRole.toLowerCase() == 'administrator';
    final currentUid = _authService.currentUser?.uid ?? '';

    return Scaffold(
      backgroundColor: widget.settings.backgroundColor,
      body: Stack(
        children: [
          Column(
            children: [
              // 1. Official PUPSIS Top Navigation Bar
              _buildTopNavbar(isAdmin),

              // 2. Main Scrollable Workspace
              Expanded(
                child: SingleChildScrollView(
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 1200),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 20,
                          vertical: 24,
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Section Page Title (e.g. "Home", "Reports", etc.)
                            _buildPageTitle(),
                            const SizedBox(height: 14),

                            // Main PUPSIS White Container Card
                            _buildMainPupsisContainer(isAdmin, currentUid),

                            const SizedBox(height: 24),

                            // Bottom Guidelines Banner matching PUPSIS screenshot
                            _buildBottomGuidelinesBanner(),
                            const SizedBox(height: 40),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),

          // 3. Floating Blue Accessibility Button matching PUPSIS screenshot
          Positioned(
            left: 20,
            bottom: 20,
            child: FloatingActionButton(
              heroTag: 'pupalert_accessibility_btn',
              onPressed: _showAccessibilityDialog,
              backgroundColor: const Color(0xFF0047AB),
              elevation: 4,
              shape: const CircleBorder(),
              child: const Icon(
                Icons.accessibility_new,
                color: Colors.white,
                size: 28,
              ),
            ),
          ),
        ],
      ),
    );
  }

  // --- 1. PUPSIS Top Navigation Bar ---
  Widget _buildTopNavbar(bool isAdmin) {
    return Container(
      height: 60,
      width: double.infinity,
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        border: Border(
          bottom: BorderSide(color: widget.settings.borderColor, width: 1),
        ),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Row(
        children: [
          // PUP Star & Seal Logo
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              border: Border.all(color: const Color(0xFFFDB913), width: 2),
            ),
            child: ClipOval(
              child: Image.asset(
                'assets/images/ic_school_logo.png',
                fit: BoxFit.cover,
                errorBuilder: (_, _, _) => Container(
                  color: widget.settings.headerColor,
                  child: const Icon(
                    Icons.star,
                    color: Color(0xFFFDB913),
                    size: 22,
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 10),

          // PUPALERT Brand Text
          Text(
            'PUPALERT',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w900,
              color: widget.settings.contrastMode == 'Dark Contrast'
                  ? const Color(0xFFFF6B6B)
                  : widget.settings.headerColor,
              letterSpacing: 0.8,
            ),
          ),
          const SizedBox(width: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: widget.settings.accentColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(
                color: widget.settings.accentColor.withValues(alpha: 0.3),
              ),
            ),
            child: Text(
              'Incident Monitoring',
              style: TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.bold,
                color: widget.settings.accentColor,
              ),
            ),
          ),

          const SizedBox(width: 28),

          // Horizontal Navigation Tabs matching PUPSIS
          Expanded(
            child: Row(
              children: [
                _buildNavTab('Home', WebMainTab.home),
                _buildNavTab(
                  isAdmin ? 'Campus Reports' : 'My Reports',
                  WebMainTab.reports,
                ),
                _buildNavTab('Polls', WebMainTab.polls),
                if (isAdmin) _buildNavTab('Analytics', WebMainTab.analytics),
                _buildNavTab('Settings', WebMainTab.settings),
              ],
            ),
          ),

          // Notifications Stream Icon with Unread Badge
          StreamBuilder<QuerySnapshot>(
            stream: _firestore.collection('notifications').snapshots(),
            builder: (context, snapshot) {
              int unread = 0;
              if (snapshot.hasData) {
                final currentUid = _authService.currentUser?.uid ?? '';
                for (var doc in snapshot.data!.docs) {
                  final d = doc.data() as Map<String, dynamic>;
                  final read = d['read'] ?? false;
                  final role = d['targetRole']?.toString().toLowerCase() ?? '';
                  if (!read) {
                    if (isAdmin && (role == 'admin' || role == 'administrator'))
                      unread++;
                    if (!isAdmin &&
                        (d['targetUser'] == widget.userName ||
                            (currentUid.isNotEmpty &&
                                d['targetUid'] == currentUid)))
                      unread++;
                  }
                }
              }

              return Stack(
                alignment: Alignment.topRight,
                children: [
                  IconButton(
                    icon: Icon(
                      Icons.notifications_none,
                      color: widget.settings.secondaryTextColor,
                    ),
                    tooltip: 'Notifications',
                    onPressed: () => _showWebNotificationsDialog(isAdmin),
                  ),
                  if (unread > 0)
                    Positioned(
                      right: 8,
                      top: 8,
                      child: Container(
                        padding: const EdgeInsets.all(4),
                        decoration: BoxDecoration(
                          color: widget.settings.accentColor,
                          shape: BoxShape.circle,
                        ),
                        constraints: const BoxConstraints(
                          minWidth: 16,
                          minHeight: 16,
                        ),
                        child: Text(
                          '$unread',
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                          ),
                          textAlign: TextAlign.center,
                        ),
                      ),
                    ),
                ],
              );
            },
          ),
          const SizedBox(width: 8),

          // User Profile Avatar with Dropdown Menu
          PopupMenuButton<String>(
            tooltip: 'Account Menu',
            color: widget.settings.cardColor,
            offset: const Offset(0, 45),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
              side: BorderSide(color: widget.settings.borderColor),
            ),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 16,
                  backgroundColor:
                      widget.settings.contrastMode == 'Dark Contrast'
                      ? const Color(0xFF2E2E2E)
                      : const Color(0xFFF3F4F6),
                  child: Icon(
                    Icons.person,
                    color: widget.settings.primaryTextColor,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  widget.firstName,
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: widget.settings.primaryTextColor,
                  ),
                ),
                Icon(
                  Icons.arrow_drop_down,
                  color: widget.settings.secondaryTextColor,
                  size: 20,
                ),
              ],
            ),
            onSelected: (value) {
              if (value == 'logout') {
                widget.onLogout();
              } else if (value == 'settings') {
                setState(() => _activeTab = WebMainTab.settings);
              }
            },
            itemBuilder: (context) => [
              PopupMenuItem(
                enabled: false,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      widget.userName,
                      style: TextStyle(
                        fontWeight: FontWeight.bold,
                        color: widget.settings.primaryTextColor,
                      ),
                    ),
                    Text(
                      isAdmin ? 'Administrator' : widget.studentId,
                      style: TextStyle(
                        fontSize: 12,
                        color: widget.settings.secondaryTextColor,
                      ),
                    ),
                    Divider(height: 12, color: widget.settings.borderColor),
                  ],
                ),
              ),
              PopupMenuItem(
                value: 'settings',
                child: Row(
                  children: [
                    Icon(
                      Icons.settings,
                      size: 18,
                      color: widget.settings.secondaryTextColor,
                    ),
                    const SizedBox(width: 10),
                    Text(
                      'Settings & Accessibility',
                      style: TextStyle(color: widget.settings.primaryTextColor),
                    ),
                  ],
                ),
              ),
              const PopupMenuItem(
                value: 'logout',
                child: Row(
                  children: [
                    Icon(Icons.logout, size: 18, color: Colors.red),
                    SizedBox(width: 10),
                    Text('Sign Out', style: TextStyle(color: Colors.red)),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildNavTab(String title, WebMainTab tab) {
    final isSelected = _activeTab == tab;
    return InkWell(
      onTap: () => setState(() => _activeTab = tab),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16),
        alignment: Alignment.center,
        decoration: BoxDecoration(
          border: Border(
            bottom: BorderSide(
              color: isSelected
                  ? widget.settings.accentColor
                  : Colors.transparent,
              width: 3,
            ),
          ),
        ),
        child: Text(
          title,
          style: TextStyle(
            fontSize: 14,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
            color: isSelected
                ? widget.settings.accentColor
                : widget.settings.secondaryTextColor,
          ),
        ),
      ),
    );
  }

  // --- 2. Page Title Header ---
  Widget _buildPageTitle() {
    String titleText = 'Home';
    switch (_activeTab) {
      case WebMainTab.home:
        titleText = 'Campus Safety Dashboard';
        break;
      case WebMainTab.reports:
        titleText = widget.userRole.toLowerCase() == 'admin'
            ? 'Campus Reports'
            : 'My Reports';
        break;
      case WebMainTab.polls:
        titleText = 'Campus Polls';
        break;
      case WebMainTab.analytics:
        titleText = 'Campus Safety Analytics & Reports (Admin Exclusive)';
        break;
      case WebMainTab.settings:
        titleText = 'Settings & Accessibility';
        break;
    }

    return Text(
      titleText,
      style: TextStyle(
        fontSize: 24,
        fontWeight: widget.settings.titleFontWeight,
        color: widget.settings.primaryTextColor,
      ),
    );
  }

  // --- 3. Main Central PUPSIS Container Card ---
  Widget _buildMainPupsisContainer(bool isAdmin, String currentUid) {
    // If user clicked Polls, Analytics, or Settings tab from top bar:
    if (_activeTab == WebMainTab.polls) {
      return Card(
        color: widget.settings.cardColor,
        elevation: 1,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(color: widget.settings.borderColor),
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: CampusPollsScreen(
            userRole: widget.userRole,
            isEmbedded: true,
            settings: widget.settings,
          ),
        ),
      );
    }

    if (_activeTab == WebMainTab.analytics) {
      return Card(
        color: widget.settings.cardColor,
        elevation: 1,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(color: widget.settings.borderColor),
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: AnalyticsScreen(
            isEmbedded: true,
            settings: widget.settings,
            userRole: widget.userRole,
          ),
        ),
      );
    }

    if (_activeTab == WebMainTab.settings) {
      return Card(
        color: widget.settings.cardColor,
        elevation: 1,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(8),
          side: BorderSide(color: widget.settings.borderColor),
        ),
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: SettingsScreen(
            userRole: widget.userRole,
            settings: widget.settings,
            onSettingsChange: widget.onSettingsChange,
          ),
        ),
      );
    }

    // If user clicked Home tab:
    if (_activeTab == WebMainTab.home) {
      return StreamBuilder<List<Report>>(
        stream: _reportService.getReportsStream(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(40),
                child: CircularProgressIndicator(
                  color: widget.settings.accentColor,
                ),
              ),
            );
          }
          final allReports = snapshot.data ?? [];
          return _buildHomeDashboard(isAdmin, currentUid, allReports);
        },
      );
    }

    // Default WebMainTab.reports (My Reports / Campus Reports):
    return StreamBuilder<List<Report>>(
      stream: _reportService.getReportsStream(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(40),
              child: CircularProgressIndicator(
                color: widget.settings.accentColor,
              ),
            ),
          );
        }
        final allReports = snapshot.data ?? [];
        return _buildPupsisReportsView(isAdmin, currentUid, allReports);
      },
    );
  }

  // --- Executive Campus Safety Dashboard (Home Tab) ---
  Widget _buildHomeDashboard(
    bool isAdmin,
    String currentUid,
    List<Report> allReports,
  ) {
    final totalCount = allReports.length;
    final inReviewCount = allReports
        .where((r) => r.status == 'In Review')
        .length;
    final inProgressCount = allReports
        .where((r) => r.status == 'In Progress')
        .length;
    final resolvedCount = allReports
        .where((r) => r.status == 'Resolved')
        .length;

    final userReports = !isAdmin
        ? allReports.where((r) {
            if (currentUid.isNotEmpty && r.reporterUid == currentUid) {
              return true;
            }
            if (widget.studentId.isNotEmpty &&
                r.reporterStudentId.isNotEmpty &&
                r.reporterStudentId == widget.studentId) {
              return true;
            }
            if (!r.isAnonymous &&
                (r.reporter == widget.userName ||
                    r.reporter == widget.firstName)) {
              return true;
            }
            if (r.reporter == widget.userName) return true;
            return false;
          }).toList()
        : allReports;

    final sortedRecent = List<Report>.from(allReports)
      ..sort((a, b) => b.timestamp.compareTo(a.timestamp));
    final recentReports = sortedRecent.take(4).toList();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // 1. Welcome Header Banner with Live Status
        _buildHomeWelcomeCard(isAdmin, totalCount, userReports.length),
        const SizedBox(height: 20),

        // 2. 4-KPI Metric Cards Grid
        _buildHomeKpiGrid(
          isAdmin,
          totalCount,
          inReviewCount,
          inProgressCount,
          resolvedCount,
        ),
        const SizedBox(height: 24),

        // 3. Two-Column Dashboard Grid
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Left Column (flex 3): Incident Category Meters & Recent Activity Stream
            Expanded(
              flex: 3,
              child: Column(
                children: [
                  _buildCategoryBreakdownCard(allReports, totalCount),
                  const SizedBox(height: 20),
                  _buildRecentIncidentsCard(recentReports, isAdmin),
                ],
              ),
            ),
            const SizedBox(width: 20),

            // Right Column (flex 2): Quick Actions, Emergency Hotlines, Guidelines
            Expanded(
              flex: 2,
              child: Column(
                children: [
                  _buildHomeQuickActionsCard(isAdmin, userReports.length),
                  const SizedBox(height: 20),
                  _buildEmergencyDirectoryCard(),
                  const SizedBox(height: 20),
                  _buildSafetyAdvisoryCard(),
                ],
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildHomeWelcomeCard(
    bool isAdmin,
    int totalCount,
    int myReportCount,
  ) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: widget.settings.borderColor),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.03),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              color: widget.settings.contrastMode == 'Dark Contrast'
                  ? const Color(0xFF8B0000)
                  : const Color(0xFFC62828),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Icon(
              Icons.shield_outlined,
              color: Colors.white,
              size: 28,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      'Welcome, ${widget.firstName}!',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: widget.settings.titleFontWeight,
                        color: widget.settings.primaryTextColor,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 3,
                      ),
                      decoration: BoxDecoration(
                        color: isAdmin
                            ? const Color(0xFFC62828).withValues(alpha: 0.12)
                            : widget.settings.accentColor.withValues(
                                alpha: 0.12,
                              ),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(
                          color: isAdmin
                              ? const Color(0xFFC62828).withValues(alpha: 0.3)
                              : widget.settings.accentColor.withValues(
                                  alpha: 0.3,
                                ),
                        ),
                      ),
                      child: Text(
                        isAdmin ? 'CAMPUS ADMINISTRATOR' : 'STUDENT PORTAL',
                        style: TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.bold,
                          color: isAdmin
                              ? (widget.settings.contrastMode == 'Dark Contrast'
                                    ? const Color(0xFFFF8A80)
                                    : const Color(0xFFC62828))
                              : widget.settings.accentColor,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  isAdmin
                      ? 'Live campus incident tracking, maintenance dispatch, and safety analytics hub.'
                      : 'Report campus concerns, follow live ticket progress, and stay updated on campus safety.',
                  style: TextStyle(
                    fontSize: 13,
                    color: widget.settings.secondaryTextColor,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Live status pill
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
            decoration: BoxDecoration(
              color: const Color(0xFF2E7D32).withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: const Color(0xFF2E7D32).withValues(alpha: 0.3),
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 8,
                  height: 8,
                  decoration: const BoxDecoration(
                    color: Color(0xFF2E7D32),
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 8),
                const Text(
                  'Live Monitoring Active',
                  style: TextStyle(
                    color: Color(0xFF2E7D32),
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHomeKpiGrid(
    bool isAdmin,
    int totalCount,
    int inReviewCount,
    int inProgressCount,
    int resolvedCount,
  ) {
    return Row(
      children: [
        Expanded(
          child: _buildKpiCard(
            title: 'Total Incidents',
            value: '$totalCount',
            subtitle: isAdmin ? 'Campus-wide submitted' : 'Total recorded',
            icon: Icons.assignment_outlined,
            color: widget.settings.accentColor,
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _buildKpiCard(
            title: 'Under Review',
            value: '$inReviewCount',
            subtitle: 'Pending staff assessment',
            icon: Icons.pending_actions_outlined,
            color: const Color(0xFFFF9800),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _buildKpiCard(
            title: 'In Progress',
            value: '$inProgressCount',
            subtitle: 'Active resolution / repair',
            icon: Icons.engineering_outlined,
            color: const Color(0xFF1976D2),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: _buildKpiCard(
            title: 'Resolved',
            value: '$resolvedCount',
            subtitle: 'Verified & closed tickets',
            icon: Icons.check_circle_outline,
            color: const Color(0xFF2E7D32),
          ),
        ),
      ],
    );
  }

  Widget _buildKpiCard({
    required String title,
    required String value,
    required String subtitle,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: widget.settings.secondaryTextColor,
                ),
              ),
              Container(
                padding: const EdgeInsets.all(7),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Icon(icon, color: color, size: 18),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            value,
            style: TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: widget.settings.primaryTextColor,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 11,
              color: widget.settings.secondaryTextColor,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCategoryBreakdownCard(List<Report> allReports, int totalCount) {
    final categories = [
      {
        'name': 'Facilities',
        'icon': Icons.business,
        'color': const Color(0xFF3F51B5),
      },
      {
        'name': 'Maintenance',
        'icon': Icons.build,
        'color': const Color(0xFF009688),
      },
      {
        'name': 'Safety',
        'icon': Icons.warning_amber_rounded,
        'color': const Color(0xFFE53935),
      },
      {
        'name': 'Cleanliness',
        'icon': Icons.cleaning_services,
        'color': const Color(0xFF8E24AA),
      },
      {
        'name': 'Equipment',
        'icon': Icons.devices_other,
        'color': const Color(0xFFFB8C00),
      },
      {
        'name': 'Other',
        'icon': Icons.category,
        'color': const Color(0xFF757575),
      },
    ];

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.pie_chart_outline,
                color: widget.settings.accentColor,
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                'Incident Breakdown by Category',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: widget.settings.titleFontWeight,
                  color: widget.settings.primaryTextColor,
                ),
              ),
              const Spacer(),
              Text(
                '$totalCount total records',
                style: TextStyle(
                  fontSize: 12,
                  color: widget.settings.secondaryTextColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          ...categories.map((cat) {
            final name = cat['name'] as String;
            final icon = cat['icon'] as IconData;
            final catColor = cat['color'] as Color;
            final count = allReports
                .where((r) => r.category.toLowerCase() == name.toLowerCase())
                .length;
            final pct = totalCount > 0 ? (count / totalCount) : 0.0;
            final pctStr = (pct * 100).toStringAsFixed(0);

            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Column(
                children: [
                  Row(
                    children: [
                      Icon(icon, size: 14, color: catColor),
                      const SizedBox(width: 8),
                      Text(
                        name,
                        style: TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.w500,
                          color: widget.settings.primaryTextColor,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        '$count ($pctStr%)',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: widget.settings.secondaryTextColor,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(4),
                    child: LinearProgressIndicator(
                      value: pct,
                      minHeight: 8,
                      backgroundColor: widget.settings.borderColor.withValues(
                        alpha: 0.5,
                      ),
                      valueColor: AlwaysStoppedAnimation<Color>(catColor),
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildRecentIncidentsCard(List<Report> recentReports, bool isAdmin) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  Icon(
                    Icons.history_toggle_off,
                    color: widget.settings.accentColor,
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Text(
                    'Recent Campus Incident Activity',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: widget.settings.titleFontWeight,
                      color: widget.settings.primaryTextColor,
                    ),
                  ),
                ],
              ),
              TextButton.icon(
                onPressed: () =>
                    setState(() => _activeTab = WebMainTab.reports),
                icon: const Icon(Icons.arrow_forward, size: 14),
                label: const Text('View All In Reports Tab'),
                style: TextButton.styleFrom(
                  foregroundColor: widget.settings.accentColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (recentReports.isEmpty)
            Padding(
              padding: const EdgeInsets.all(28),
              child: Center(
                child: Text(
                  'No recent incidents logged yet.',
                  style: TextStyle(
                    color: widget.settings.secondaryTextColor,
                    fontSize: 13,
                  ),
                ),
              ),
            )
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: recentReports.length,
              separatorBuilder: (_, _) => Divider(
                height: 16,
                color: widget.settings.borderColor.withValues(alpha: 0.5),
              ),
              itemBuilder: (context, i) {
                final r = recentReports[i];
                Color statusColor;
                switch (r.status) {
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

                final displayDate = r.dateSubmitted.isNotEmpty
                    ? r.dateSubmitted
                    : DateFormat('MMM d, yyyy').format(
                        DateTime.fromMillisecondsSinceEpoch(r.timestamp),
                      );

                return InkWell(
                  onTap: () => _openReportDetail(r, isAdmin),
                  borderRadius: BorderRadius.circular(8),
                  child: Padding(
                    padding: const EdgeInsets.symmetric(
                      vertical: 6,
                      horizontal: 4,
                    ),
                    child: Row(
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: r.hasImage
                              ? SizedBox(
                                  width: 42,
                                  height: 42,
                                  child: ReportImageView(
                                    imageUrl: r.displayImageUrl,
                                    width: 42,
                                    height: 42,
                                    fit: BoxFit.cover,
                                  ),
                                )
                              : Container(
                                  width: 42,
                                  height: 42,
                                  decoration: BoxDecoration(
                                    color: statusColor.withValues(alpha: 0.12),
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: Icon(
                                    r.status == 'Resolved'
                                        ? Icons.task_alt
                                        : (r.status == 'In Progress'
                                              ? Icons.engineering
                                              : Icons.hourglass_top),
                                    color: statusColor,
                                    size: 20,
                                  ),
                                ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                r.title,
                                style: TextStyle(
                                  fontSize: 14,
                                  fontWeight: FontWeight.w600,
                                  color: widget.settings.primaryTextColor,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 2),
                              Row(
                                children: [
                                  Text(
                                    r.category,
                                    style: TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.bold,
                                      color: widget.settings.accentColor,
                                    ),
                                  ),
                                  if (r.location.isNotEmpty) ...[
                                    Text(
                                      ' • ${r.location}',
                                      style: TextStyle(
                                        fontSize: 11,
                                        color:
                                            widget.settings.secondaryTextColor,
                                      ),
                                    ),
                                  ],
                                  Text(
                                    ' • $displayDate',
                                    style: TextStyle(
                                      fontSize: 11,
                                      color: widget.settings.secondaryTextColor,
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            color: statusColor.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(10),
                            border: Border.all(
                              color: statusColor.withValues(alpha: 0.3),
                            ),
                          ),
                          child: Text(
                            r.status,
                            style: TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.bold,
                              color: statusColor,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        OutlinedButton(
                          onPressed: () => _openReportDetail(r, isAdmin),
                          style: OutlinedButton.styleFrom(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 10,
                              vertical: 6,
                            ),
                            side: BorderSide(
                              color: widget.settings.borderColor,
                            ),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(6),
                            ),
                          ),
                          child: Text(
                            'Inspect',
                            style: TextStyle(
                              fontSize: 11,
                              color: widget.settings.primaryTextColor,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
        ],
      ),
    );
  }

  Widget _buildHomeQuickActionsCard(bool isAdmin, int myReportCount) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.bolt, color: widget.settings.accentColor, size: 20),
              const SizedBox(width: 8),
              Text(
                'Quick Actions',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: widget.settings.titleFontWeight,
                  color: widget.settings.primaryTextColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          if (!isAdmin) ...[
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: _openSubmitReport,
                icon: const Icon(Icons.add_circle_outline, size: 18),
                label: const Text('Report New Incident'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: widget.settings.accentColor,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () =>
                    setState(() => _activeTab = WebMainTab.reports),
                icon: const Icon(Icons.folder_shared_outlined, size: 18),
                label: Text('My Submitted Reports ($myReportCount)'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: widget.settings.primaryTextColor,
                  side: BorderSide(color: widget.settings.borderColor),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () => setState(() => _activeTab = WebMainTab.polls),
                icon: const Icon(Icons.how_to_vote_outlined, size: 18),
                label: const Text('Campus Safety Polls'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: widget.settings.primaryTextColor,
                  side: BorderSide(color: widget.settings.borderColor),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
            ),
          ] else ...[
            SizedBox(
              width: double.infinity,
              child: ElevatedButton.icon(
                onPressed: () =>
                    setState(() => _activeTab = WebMainTab.reports),
                icon: const Icon(Icons.inbox, size: 18),
                label: const Text('Manage Campus Reports Table'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: widget.settings.accentColor,
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () =>
                    setState(() => _activeTab = WebMainTab.analytics),
                icon: const Icon(Icons.analytics_outlined, size: 18),
                label: const Text('View Analytics & Survey Answers'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: widget.settings.primaryTextColor,
                  side: BorderSide(color: widget.settings.borderColor),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 10),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () => setState(() => _activeTab = WebMainTab.polls),
                icon: const Icon(Icons.poll_outlined, size: 18),
                label: const Text('Campus Polls Management'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: widget.settings.primaryTextColor,
                  side: BorderSide(color: widget.settings.borderColor),
                  padding: const EdgeInsets.symmetric(vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildEmergencyDirectoryCard() {
    final contacts = [
      {
        'title': 'PUP Main Campus Security',
        'num': '(02) 5335-1PUP loc 222',
        'icon': Icons.security,
      },
      {
        'title': 'PUP Medical & Dental Clinic',
        'num': '(02) 5335-1PUP loc 312',
        'icon': Icons.local_hospital,
      },
      {
        'title': 'National Emergency Line',
        'num': '911 / 117',
        'icon': Icons.phone_in_talk,
      },
      {
        'title': 'PNP Station 8 (Santa Mesa)',
        'num': '(02) 8713-3948',
        'icon': Icons.local_police,
      },
      {
        'title': 'Bureau of Fire Protection',
        'num': '(02) 5335-1PUP loc 210',
        'icon': Icons.fire_extinguisher,
      },
    ];

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.emergency, color: Color(0xFFC62828), size: 20),
              const SizedBox(width: 8),
              Text(
                'Campus Emergency Directory',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: widget.settings.titleFontWeight,
                  color: widget.settings.primaryTextColor,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ...contacts.map((c) {
            final title = c['title'] as String;
            final num = c['num'] as String;
            final icon = c['icon'] as IconData;

            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(6),
                    decoration: BoxDecoration(
                      color: const Color(0xFFC62828).withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: Icon(icon, size: 14, color: const Color(0xFFC62828)),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: widget.settings.primaryTextColor,
                          ),
                        ),
                        Text(
                          num,
                          style: TextStyle(
                            fontSize: 11,
                            color: widget.settings.secondaryTextColor,
                            fontFamily: 'monospace',
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  Widget _buildSafetyAdvisoryCard() {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: widget.settings.contrastMode == 'Dark Contrast'
            ? const Color(0xFF261515)
            : const Color(0xFFFFF5F5),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: widget.settings.contrastMode == 'Dark Contrast'
              ? const Color(0xFF4A2020)
              : const Color(0xFFFFCDD2),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.shield_moon_outlined,
                color: widget.settings.contrastMode == 'Dark Contrast'
                    ? const Color(0xFFFF8A80)
                    : const Color(0xFFC62828),
                size: 18,
              ),
              const SizedBox(width: 8),
              Text(
                'PUP SafeWalk Advisory',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: widget.settings.titleFontWeight,
                  color: widget.settings.contrastMode == 'Dark Contrast'
                      ? const Color(0xFFFF8A80)
                      : const Color(0xFFB71C1C),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'Keep campus pathways well-lit and unobstructed. Report malfunctioning lights, wet floors, and structural hazards promptly to maintain a secure environment for all Iskolar ng Bayan.',
            style: TextStyle(
              fontSize: 12,
              color: widget.settings.secondaryTextColor,
              height: 1.4,
            ),
          ),
        ],
      ),
    );
  }

  // --- Authentic PUPSIS Reports Management View (Campus Reports / My Reports) ---
  Widget _buildPupsisReportsView(
    bool isAdmin,
    String currentUid,
    List<Report> allReports,
  ) {
    // Student sees their own reports on My Reports, Admin sees all reports
    final visibleReports = !isAdmin
        ? allReports.where((r) {
            if (currentUid.isNotEmpty && r.reporterUid == currentUid) {
              return true;
            }
            if (widget.studentId.isNotEmpty &&
                r.reporterStudentId.isNotEmpty &&
                r.reporterStudentId == widget.studentId) {
              return true;
            }
            if (!r.isAnonymous &&
                (r.reporter == widget.userName ||
                    r.reporter == widget.firstName)) {
              return true;
            }
            if (r.reporter == widget.userName) return true;
            if (r.reporterUid.isNotEmpty &&
                (r.reporterUid == currentUid ||
                    r.reporterUid == widget.studentId)) {
              return true;
            }
            return false;
          }).toList()
        : allReports;

    final inboxCount = visibleReports.length;

    return Card(
      color: widget.settings.cardColor,
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: widget.settings.borderColor),
      ),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Student / Staff Header matching screenshot:
            // "ATOK, MARION OLIVIER COBO (2023-00220-PQ-0)"
            Text(
              isAdmin
                  ? 'ADMINISTRATOR, CAMPUS SAFETY & MONITORING (STAFF-PQ-01)'
                  : '${widget.userName.toUpperCase()} (${widget.studentId.isNotEmpty ? widget.studentId : 'STUDENT-PQ-01'})',
              style: TextStyle(
                fontSize: 16,
                fontWeight: widget.settings.titleFontWeight,
                color: widget.settings.contrastMode == 'Dark Contrast'
                    ? const Color(0xFFFF6B6B)
                    : const Color(0xFFC62828),
                letterSpacing: 0.3,
              ),
            ),
            const SizedBox(height: 18),

            // Red Advisory Memo Card matching PUPSIS screenshot
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
              decoration: BoxDecoration(
                color: widget.settings.contrastMode == 'Dark Contrast'
                    ? const Color(0xFF261515)
                    : const Color(0xFFFFF5F5),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: widget.settings.contrastMode == 'Dark Contrast'
                      ? const Color(0xFF4A2020)
                      : const Color(0xFFFFCDD2),
                ),
              ),
              child: Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: widget.settings.contrastMode == 'Dark Contrast'
                          ? const Color(0xFF8B0000)
                          : const Color(0xFFD32F2F),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Icon(
                      Icons.description,
                      color: Colors.white,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'PUP SafeWalk & Incident Monitoring System Advisory',
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: widget.settings.titleFontWeight,
                            color:
                                widget.settings.contrastMode == 'Dark Contrast'
                                ? const Color(0xFFFF8A80)
                                : const Color(0xFFB71C1C),
                          ),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          'Report campus facility hazards, maintenance requests, and safety concerns directly to university staff.',
                          style: TextStyle(
                            fontSize: 12,
                            color: widget.settings.secondaryTextColor,
                          ),
                        ),
                      ],
                    ),
                  ),
                  if (!isAdmin)
                    ElevatedButton.icon(
                      onPressed: _openSubmitReport,
                      icon: const Icon(Icons.add, size: 16),
                      label: const Text('Submit Report'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: widget.settings.accentColor,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 12,
                        ),
                      ),
                    ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Two-Column Grid: Left Sub-Menu & Right Data Table
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Left Sub-Navigation Menu Card (matching PUPSIS Inbox list)
                SizedBox(
                  width: 240,
                  child: _buildLeftSubmenuCard(isAdmin, inboxCount),
                ),
                const SizedBox(width: 20),

                // Right Main Data Table (matching PUPSIS Inbox table)
                Expanded(child: _buildRightDataTable(visibleReports, isAdmin)),
              ],
            ),
          ],
        ),
      ),
    );
  }

  // --- Left Submenu Card matching PUPSIS screenshot ---
  Widget _buildLeftSubmenuCard(bool isAdmin, int inboxCount) {
    final selectedBg = widget.settings.contrastMode == 'Dark Contrast'
        ? const Color(0xFF2E2E2E)
        : const Color(0xFFF3F4F6);

    return Container(
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        children: [
          // Item 1: Inbox (N)
          InkWell(
            onTap: () => setState(() => _selectedSubNavIndex = 0),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              color: _selectedSubNavIndex == 0
                  ? selectedBg
                  : Colors.transparent,
              child: Row(
                children: [
                  Icon(
                    Icons.inbox,
                    size: 18,
                    color: _selectedSubNavIndex == 0
                        ? widget.settings.accentColor
                        : widget.settings.secondaryTextColor,
                  ),
                  const SizedBox(width: 10),
                  Text(
                    'Inbox ($inboxCount)',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: _selectedSubNavIndex == 0
                          ? widget.settings.titleFontWeight
                          : widget.settings.bodyFontWeight,
                      color: widget.settings.primaryTextColor,
                    ),
                  ),
                ],
              ),
            ),
          ),
          Divider(height: 1, color: widget.settings.borderColor),

          // Item 2: Action item
          InkWell(
            onTap: () {
              if (isAdmin) {
                setState(() => _activeTab = WebMainTab.analytics);
              } else {
                _openSubmitReport();
              }
            },
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              child: Row(
                children: [
                  Icon(
                    isAdmin
                        ? Icons.assessment_outlined
                        : Icons.add_circle_outline,
                    size: 18,
                    color: widget.settings.secondaryTextColor,
                  ),
                  const SizedBox(width: 10),
                  Text(
                    isAdmin ? 'Campus Analytics' : 'Submit New Report',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: widget.settings.bodyFontWeight,
                      color: widget.settings.primaryTextColor,
                    ),
                  ),
                ],
              ),
            ),
          ),
          Divider(height: 1, color: widget.settings.borderColor),

          // Item 3: Campus Polls / Feedback
          InkWell(
            onTap: () => setState(() => _activeTab = WebMainTab.polls),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
              child: Row(
                children: [
                  Icon(
                    Icons.how_to_vote_outlined,
                    size: 18,
                    color: widget.settings.secondaryTextColor,
                  ),
                  const SizedBox(width: 10),
                  Text(
                    'Campus Polls & Surveys',
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: widget.settings.bodyFontWeight,
                      color: widget.settings.primaryTextColor,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  // --- Right Data Table matching PUPSIS Inbox Table ---
  Widget _buildRightDataTable(List<Report> allReports, bool isAdmin) {
    // Filter by status tab
    final currentStatus = _statusTabs[_selectedStatusTab];
    var filtered = currentStatus == 'All'
        ? allReports
        : allReports.where((r) => r.status == currentStatus).toList();

    // Filter by category
    if (_selectedCategory != 'All Categories') {
      filtered = filtered
          .where((r) => r.category == _selectedCategory)
          .toList();
    }

    return Container(
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Table Title Bar: "Inbox" with Status Filters
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: widget.settings.contrastMode == 'Dark Contrast'
                  ? const Color(0xFF252525)
                  : const Color(0xFFF9FAFB),
              borderRadius: const BorderRadius.vertical(
                top: Radius.circular(6),
              ),
              border: Border(
                bottom: BorderSide(color: widget.settings.borderColor),
              ),
            ),
            child: Row(
              children: [
                Text(
                  'Inbox',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: widget.settings.titleFontWeight,
                    color: widget.settings.primaryTextColor,
                  ),
                ),
                const SizedBox(width: 20),

                // Status Filter Chips
                ...List.generate(_statusTabs.length, (idx) {
                  final isSel = _selectedStatusTab == idx;
                  return Padding(
                    padding: const EdgeInsets.only(right: 6),
                    child: InkWell(
                      onTap: () => setState(() => _selectedStatusTab = idx),
                      child: Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 4,
                        ),
                        decoration: BoxDecoration(
                          color: isSel
                              ? widget.settings.accentColor
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(4),
                          border: Border.all(
                            color: isSel
                                ? widget.settings.accentColor
                                : widget.settings.borderColor,
                          ),
                        ),
                        child: Text(
                          _statusTabs[idx],
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: isSel
                                ? FontWeight.bold
                                : FontWeight.normal,
                            color: isSel
                                ? Colors.white
                                : widget.settings.secondaryTextColor,
                          ),
                        ),
                      ),
                    ),
                  );
                }),

                const Spacer(),

                // Category Dropdown
                DropdownButton<String>(
                  value: _selectedCategory,
                  underline: const SizedBox(),
                  dropdownColor: widget.settings.cardColor,
                  iconEnabledColor: widget.settings.secondaryTextColor,
                  style: TextStyle(
                    fontSize: 12,
                    color: widget.settings.primaryTextColor,
                    fontWeight: widget.settings.bodyFontWeight,
                  ),
                  items: _categories
                      .map((c) => DropdownMenuItem(value: c, child: Text(c)))
                      .toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedCategory = val);
                  },
                ),
              ],
            ),
          ),

          // Table Rows matching PUPSIS format (Date, Description/Title, Action Icon)
          if (filtered.isEmpty)
            Padding(
              padding: const EdgeInsets.all(40),
              child: Center(
                child: Column(
                  children: [
                    Icon(
                      Icons.inbox_outlined,
                      size: 48,
                      color: widget.settings.secondaryTextColor,
                    ),
                    const SizedBox(height: 10),
                    Text(
                      'No incident reports in this view.',
                      style: TextStyle(
                        color: widget.settings.secondaryTextColor,
                        fontSize: 14,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: filtered.length,
              separatorBuilder: (_, _) => Divider(
                height: 1,
                color: widget.settings.borderColor.withValues(alpha: 0.5),
              ),
              itemBuilder: (context, index) {
                final report = filtered[index];
                return _buildPupsisTableRow(report, isAdmin);
              },
            ),
        ],
      ),
    );
  }

  // Row matching PUPSIS screenshot with date, title/message, and red action badge icon
  Widget _buildPupsisTableRow(Report report, bool isAdmin) {
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

    final displayDate = report.dateSubmitted.isNotEmpty
        ? report.dateSubmitted
        : DateFormat('MMMM d, yyyy')
              .format(DateTime.fromMillisecondsSinceEpoch(report.timestamp));

    return InkWell(
      onTap: () => _openReportDetail(report, isAdmin),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
        child: Row(
          children: [
            // Column 1: Date matching PUPSIS (e.g. "March 28, 2026")
            SizedBox(
              width: 140,
              child: Text(
                displayDate,
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                  color: widget.settings.secondaryTextColor,
                ),
              ),
            ),

            // Photo Thumbnail if report has an attached picture
            if (report.hasImage) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(6),
                child: SizedBox(
                  width: 44,
                  height: 44,
                  child: ReportImageView(
                    imageUrl: report.displayImageUrl,
                    width: 44,
                    height: 44,
                    fit: BoxFit.cover,
                  ),
                ),
              ),
              const SizedBox(width: 12),
            ],

            // Column 2: Title & Details
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    report.title,
                    style: TextStyle(
                      fontSize: 13,
                      fontWeight: widget.settings.titleFontWeight,
                      color: widget.settings.primaryTextColor,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 1,
                        ),
                        decoration: BoxDecoration(
                          color: widget.settings.accentColor.withValues(
                            alpha: 0.12,
                          ),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          report.category,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            color: widget.settings.accentColor,
                          ),
                        ),
                      ),
                      if (report.hasImage) ...[
                        const SizedBox(width: 6),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 6,
                            vertical: 1,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.green.withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(4),
                            border: Border.all(
                              color: Colors.green.withValues(alpha: 0.3),
                            ),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.photo_camera,
                                size: 10,
                                color: Colors.green,
                              ),
                              SizedBox(width: 3),
                              Text(
                                'Photo',
                                style: TextStyle(
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold,
                                  color: Colors.green,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                      if (report.location.isNotEmpty) ...[
                        const SizedBox(width: 8),
                        Icon(
                          Icons.location_on,
                          size: 12,
                          color: widget.settings.secondaryTextColor,
                        ),
                        const SizedBox(width: 2),
                        Text(
                          report.location,
                          style: TextStyle(
                            fontSize: 11,
                            color: widget.settings.secondaryTextColor,
                          ),
                        ),
                      ],
                    ],
                  ),
                ],
              ),
            ),

            // Column 3: Status Badge
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: statusColor.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: statusColor.withValues(alpha: 0.4)),
              ),
              child: Text(
                report.status,
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  color: statusColor,
                ),
              ),
            ),
            const SizedBox(width: 14),

            // Column 4: Red View Action Icon matching PUPSIS screenshot right red icon
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: widget.settings.accentColor.withValues(alpha: 0.12),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Icon(
                Icons.assignment_outlined,
                color: widget.settings.accentColor,
                size: 16,
              ),
            ),
          ],
        ),
      ),
    );
  }

  // --- 4. Bottom Guidelines Banner matching PUPSIS screenshot ---
  Widget _buildBottomGuidelinesBanner() {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: widget.settings.cardColor,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: widget.settings.borderColor),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Red top accent stripe
          Container(
            height: 4,
            width: double.infinity,
            decoration: BoxDecoration(
              color: widget.settings.accentColor,
              borderRadius: const BorderRadius.vertical(
                top: Radius.circular(6),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'GENERAL GUIDELINES FOR INCIDENT REPORTING & EMERGENCY PROTOCOLS IN PUP',
                  style: TextStyle(
                    fontSize: 13,
                    fontWeight: widget.settings.titleFontWeight,
                    color: widget.settings.primaryTextColor,
                    letterSpacing: 0.3,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  'All reported campus safety, facilities, and cleanliness incidents are audited directly by university administrative personnel. For immediate life safety emergencies, contact the University Campus Safety Hotline: (02) 5335-1PUP or approach the nearest security guard post.',
                  style: TextStyle(
                    fontSize: 12,
                    color: widget.settings.secondaryTextColor,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  void _showWebNotificationsDialog(bool isAdmin) {
    showDialog(
      context: context,
      builder: (ctx) {
        return Dialog(
          backgroundColor: widget.settings.cardColor,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
            side: BorderSide(color: widget.settings.borderColor),
          ),
          child: Container(
            width: 500,
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: widget.settings.cardColor,
              borderRadius: BorderRadius.circular(16),
            ),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Campus Notifications',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: widget.settings.titleFontWeight,
                        color: widget.settings.primaryTextColor,
                      ),
                    ),
                    IconButton(
                      icon: Icon(
                        Icons.close,
                        color: widget.settings.primaryTextColor,
                      ),
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
                Divider(color: widget.settings.borderColor),
                ConstrainedBox(
                  constraints: const BoxConstraints(maxHeight: 360),
                  child: StreamBuilder<QuerySnapshot>(
                    stream: _firestore.collection('notifications').snapshots(),
                    builder: (context, snapshot) {
                      if (!snapshot.hasData)
                        return Center(
                          child: CircularProgressIndicator(
                            color: widget.settings.accentColor,
                          ),
                        );
                      final docs = snapshot.data!.docs;
                      if (docs.isEmpty)
                        return Center(
                          child: Text(
                            'No notifications.',
                            style: TextStyle(
                              color: widget.settings.secondaryTextColor,
                            ),
                          ),
                        );

                      return ListView.separated(
                        itemCount: docs.length,
                        separatorBuilder: (_, _) => Divider(
                          height: 1,
                          color: widget.settings.borderColor.withValues(
                            alpha: 0.5,
                          ),
                        ),
                        itemBuilder: (context, i) {
                          final d = docs[i].data() as Map<String, dynamic>;
                          return ListTile(
                            leading: Icon(
                              Icons.notifications_active,
                              color: widget.settings.accentColor,
                            ),
                            title: Text(
                              d['title'] ?? '',
                              style: TextStyle(
                                fontWeight: widget.settings.titleFontWeight,
                                color: widget.settings.primaryTextColor,
                              ),
                            ),
                            subtitle: Text(
                              d['message'] ?? '',
                              style: TextStyle(
                                color: widget.settings.secondaryTextColor,
                              ),
                            ),
                          );
                        },
                      );
                    },
                  ),
                ),
              ],
            ),
          ),
        );
      },
    );
  }
}
