import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../models/accessibility_settings.dart';
import '../../services/auth_service.dart';

class SettingsScreen extends StatefulWidget {
  final String userRole;
  final AccessibilitySettings settings;
  final Function(AccessibilitySettings) onSettingsChange;

  const SettingsScreen({
    super.key,
    required this.userRole,
    required this.settings,
    required this.onSettingsChange,
  });

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late AccessibilitySettings _settings;
  final _authService = AuthService();

  @override
  void initState() {
    super.initState();
    _settings = widget.settings;
  }

  @override
  void didUpdateWidget(covariant SettingsScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.settings != widget.settings) {
      _settings = widget.settings;
    }
  }

  Future<void> _update(AccessibilitySettings updated) async {
    setState(() => _settings = updated);
    widget.onSettingsChange(updated);

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('access_text_size', updated.textSize);
    await prefs.setBool('access_bold_text', updated.boldText);
    await prefs.setString('access_contrast_mode', updated.contrastMode);
    await prefs.setBool('access_reduce_motion', updated.reduceMotion);
    await prefs.setBool('access_grayscale_mode', updated.grayscaleMode);
    await prefs.setBool('access_large_buttons', updated.largeButtons);
    await prefs.setBool('access_simplified_cards', updated.simplifiedCards);
    await prefs.setBool('access_hide_media_preview', updated.hideMediaPreview);
    await prefs.setBool('security_2fa_enabled', updated.twoFactorEnabled);
  }

  Future<void> _reset2fa() async {
    await _authService.clearOtpVerifiedState();
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text(
          '2FA verification reset for this device. Code required on next login.',
        ),
        backgroundColor: Colors.green,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final settings = _settings;
    final primaryTextColor = settings.primaryTextColor;
    final secondaryTextColor = settings.secondaryTextColor;
    final titleColor = settings.accentColor;
    final cardColor = settings.cardColor;
    final bodySize = settings.bodyFontSize;
    final titleSize = settings.titleFontSize;
    final sectionTitleSize = settings.sectionTitleFontSize;
    final headingSize = settings.headingFontSize;
    final smallSize = settings.smallFontSize;

    final isStudent = widget.userRole.toLowerCase() == 'student';

    return SingleChildScrollView(
      padding: EdgeInsets.all(settings.cardPadding),
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 750),
          child: Card(
            color: cardColor,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(settings.cardBorderRadius),
              side: BorderSide(color: settings.borderColor),
            ),
            elevation: settings.reduceMotion ? 0 : 3,
            child: Padding(
              padding: EdgeInsets.all(settings.cardPadding),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Accessibility Settings',
                    style: TextStyle(
                      fontSize: headingSize,
                      fontWeight: settings.titleFontWeight,
                      color: titleColor,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    isStudent
                        ? 'Customize readability, visibility, and comfort for using PUP Alert.'
                        : 'Adjust accessibility, readability, and viewing preferences for admin use.',
                    style: TextStyle(
                      fontSize: bodySize,
                      color: secondaryTextColor,
                      fontWeight: settings.bodyFontWeight,
                    ),
                  ),
                  const SizedBox(height: 22),

                  // Section 1: Text Display matching Kotlin NextActivity.kt line 1913
                  _buildSectionTitle(
                    'Text Display',
                    titleColor,
                    sectionTitleSize,
                    settings.boldText,
                  ),
                  const SizedBox(height: 8),

                  // Text Size dropdown
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(
                      'Text Size',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Adjust reading font size across the entire application',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    trailing: DropdownButton<String>(
                      value: _settings.textSize,
                      dropdownColor: cardColor,
                      style: TextStyle(
                        fontSize: bodySize,
                        color: primaryTextColor,
                        fontWeight: settings.mediumFontWeight,
                      ),
                      underline: const SizedBox(),
                      items: ['Small', 'Default', 'Large', 'Extra Large'].map((
                        s,
                      ) {
                        return DropdownMenuItem(value: s, child: Text(s));
                      }).toList(),
                      onChanged: (val) {
                        if (val != null)
                          _update(_settings.copyWith(textSize: val));
                      },
                    ),
                  ),

                  // Bold Text switch
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Bold Text',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Make labels and content easier to read.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.boldText,
                    onChanged: (val) =>
                        _update(_settings.copyWith(boldText: val)),
                  ),

                  // Contrast Mode dropdown
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(
                      'Contrast Mode',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Adjust theme contrast and readability',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    trailing: DropdownButton<String>(
                      value: _settings.contrastMode,
                      dropdownColor: cardColor,
                      style: TextStyle(
                        fontSize: bodySize,
                        color: primaryTextColor,
                        fontWeight: settings.mediumFontWeight,
                      ),
                      underline: const SizedBox(),
                      items:
                          [
                            'Default',
                            'High Contrast',
                            'Dark Contrast',
                            'Light Contrast',
                          ].map((c) {
                            return DropdownMenuItem(value: c, child: Text(c));
                          }).toList(),
                      onChanged: (val) {
                        if (val != null)
                          _update(_settings.copyWith(contrastMode: val));
                      },
                    ),
                  ),

                  const Divider(height: 36),

                  // Section 2: Comfort Options matching Kotlin NextActivity.kt line 1967
                  _buildSectionTitle(
                    'Comfort Options',
                    titleColor,
                    sectionTitleSize,
                    settings.boldText,
                  ),
                  const SizedBox(height: 8),

                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Reduce Motion',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Minimize motion effects and visual transitions.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.reduceMotion,
                    onChanged: (val) =>
                        _update(_settings.copyWith(reduceMotion: val)),
                  ),

                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Grayscale Mode',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Reduce strong colors for a calmer display.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.grayscaleMode,
                    onChanged: (val) =>
                        _update(_settings.copyWith(grayscaleMode: val)),
                  ),

                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Large Buttons',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Increase touch comfort for controls and actions.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.largeButtons,
                    onChanged: (val) =>
                        _update(_settings.copyWith(largeButtons: val)),
                  ),

                  const Divider(height: 36),

                  // Section 3: PUP Alert View Options matching Kotlin line 2023
                  _buildSectionTitle(
                    'PUP Alert View Options',
                    titleColor,
                    sectionTitleSize,
                    settings.boldText,
                  ),
                  const SizedBox(height: 8),

                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Simplified Cards',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Use a cleaner card style for reports and dashboard items.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.simplifiedCards,
                    onChanged: (val) =>
                        _update(_settings.copyWith(simplifiedCards: val)),
                  ),

                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Hide Media Preview',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Hide images and media previews while browsing reports.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.hideMediaPreview,
                    onChanged: (val) =>
                        _update(_settings.copyWith(hideMediaPreview: val)),
                  ),

                  const Divider(height: 36),

                  // Section 4: Security & Two-Factor Authentication (2FA)
                  _buildSectionTitle(
                    'Security & Authentication (2FA)',
                    titleColor,
                    sectionTitleSize,
                    settings.boldText,
                  ),
                  const SizedBox(height: 8),

                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    activeThumbColor: titleColor,
                    title: Text(
                      'Two-Factor Authentication (2FA)',
                      style: TextStyle(
                        fontSize: titleSize,
                        fontWeight: settings.mediumFontWeight,
                        color: primaryTextColor,
                      ),
                    ),
                    subtitle: Text(
                      'Require a 6-digit OTP verification code when signing in as student.',
                      style: TextStyle(
                        fontSize: bodySize - 1,
                        color: secondaryTextColor,
                      ),
                    ),
                    value: _settings.twoFactorEnabled,
                    onChanged: (val) =>
                        _update(_settings.copyWith(twoFactorEnabled: val)),
                  ),
                  const SizedBox(height: 10),

                  OutlinedButton.icon(
                    onPressed: _reset2fa,
                    icon: const Icon(Icons.refresh, size: 18),
                    label: const Text('Reset 2FA Device Verification'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: titleColor,
                      side: BorderSide(color: titleColor),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(
                          settings.cardBorderRadius,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Clears verified device cache so that OTP verification will be prompted on next student sign-in.',
                    style: TextStyle(
                      fontSize: smallSize,
                      color: secondaryTextColor,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildSectionTitle(
    String title,
    Color color,
    double fontSize,
    bool isBold,
  ) {
    return Text(
      title,
      style: TextStyle(
        fontSize: fontSize,
        fontWeight: isBold ? FontWeight.w900 : FontWeight.bold,
        color: color,
      ),
    );
  }
}
