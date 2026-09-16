import 'package:flutter/material.dart';

class AccessibilitySettings {
  final String textSize; // "Small", "Default", "Large", "Extra Large"
  final bool boldText;
  final String
  contrastMode; // "Default", "High Contrast", "Dark Contrast", "Light Contrast"
  final bool reduceMotion;
  final bool grayscaleMode;
  final bool largeButtons;
  final bool simplifiedCards;
  final bool hideMediaPreview;
  final bool twoFactorEnabled;

  const AccessibilitySettings({
    this.textSize = 'Default',
    this.boldText = false,
    this.contrastMode = 'Default',
    this.reduceMotion = false,
    this.grayscaleMode = false,
    this.largeButtons = false,
    this.simplifiedCards = false,
    this.hideMediaPreview = false,
    this.twoFactorEnabled = true,
  });

  AccessibilitySettings copyWith({
    String? textSize,
    bool? boldText,
    String? contrastMode,
    bool? reduceMotion,
    bool? grayscaleMode,
    bool? largeButtons,
    bool? simplifiedCards,
    bool? hideMediaPreview,
    bool? twoFactorEnabled,
  }) {
    return AccessibilitySettings(
      textSize: textSize ?? this.textSize,
      boldText: boldText ?? this.boldText,
      contrastMode: contrastMode ?? this.contrastMode,
      reduceMotion: reduceMotion ?? this.reduceMotion,
      grayscaleMode: grayscaleMode ?? this.grayscaleMode,
      largeButtons: largeButtons ?? this.largeButtons,
      simplifiedCards: simplifiedCards ?? this.simplifiedCards,
      hideMediaPreview: hideMediaPreview ?? this.hideMediaPreview,
      twoFactorEnabled: twoFactorEnabled ?? this.twoFactorEnabled,
    );
  }

  // --- Dynamic Color System (matching Kotlin NextActivity.kt) ---
  Color get backgroundColor {
    switch (contrastMode) {
      case 'Dark Contrast':
        return const Color(0xFF121212);
      case 'Light Contrast':
        return Colors.white;
      case 'High Contrast':
        return const Color(0xFFF7F7F7);
      default:
        return const Color(0xFFF5F8F9);
    }
  }

  Color get cardColor {
    if (contrastMode == 'Dark Contrast') {
      return const Color(0xFF1E1E1E);
    }
    return Colors.white;
  }

  Color get primaryTextColor {
    switch (contrastMode) {
      case 'Dark Contrast':
        return Colors.white;
      case 'High Contrast':
        return Colors.black;
      default:
        return const Color(0xFF222222);
    }
  }

  Color get secondaryTextColor {
    switch (contrastMode) {
      case 'Dark Contrast':
        return const Color(0xFFD0D0D0);
      case 'High Contrast':
        return const Color(0xFF111111);
      default:
        return const Color(0xFF666666);
    }
  }

  Color get accentColor {
    if (grayscaleMode) return const Color(0xFF444444);
    if (contrastMode == 'Dark Contrast') return const Color(0xFFFF6B6B);
    return const Color(0xFFE1001B);
  }

  Color get headerColor {
    if (grayscaleMode) return const Color(0xFF444444);
    if (contrastMode == 'Dark Contrast') return const Color(0xFF8B0000);
    return const Color(0xFFE1001B);
  }

  Color get borderColor {
    if (contrastMode == 'Dark Contrast') return const Color(0xFF444444);
    return const Color(0xFFE0E0E0);
  }

  // --- Dynamic Typography System ---
  double get headingFontSize {
    switch (textSize) {
      case 'Small':
        return 18.0;
      case 'Large':
        return 24.0;
      case 'Extra Large':
        return 26.0;
      default:
        return 21.0;
    }
  }

  double get sectionTitleFontSize {
    switch (textSize) {
      case 'Small':
        return 15.0;
      case 'Large':
        return 19.0;
      case 'Extra Large':
        return 21.0;
      default:
        return 17.0;
    }
  }

  double get titleFontSize {
    switch (textSize) {
      case 'Small':
        return 15.0;
      case 'Large':
        return 19.0;
      case 'Extra Large':
        return 21.0;
      default:
        return 17.0;
    }
  }

  double get bodyFontSize {
    switch (textSize) {
      case 'Small':
        return 12.0;
      case 'Large':
        return 15.0;
      case 'Extra Large':
        return 17.0;
      default:
        return 14.0;
    }
  }

  double get smallFontSize {
    switch (textSize) {
      case 'Small':
        return 10.0;
      case 'Large':
        return 13.0;
      case 'Extra Large':
        return 15.0;
      default:
        return 12.0;
    }
  }

  FontWeight get titleFontWeight =>
      boldText ? FontWeight.w900 : FontWeight.bold;
  FontWeight get bodyFontWeight =>
      boldText ? FontWeight.bold : FontWeight.normal;
  FontWeight get mediumFontWeight =>
      boldText ? FontWeight.w800 : FontWeight.w600;

  // --- Dynamic Dimensions ---
  double get cardBorderRadius => simplifiedCards ? 8.0 : 16.0;
  double get buttonHeight => largeButtons ? 54.0 : 48.0;
  double get cardPadding => largeButtons ? 20.0 : 16.0;
}
