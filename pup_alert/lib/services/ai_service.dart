class AIAnalysisResult {
  final String suggestedCategory;
  final String severity; // "Low", "Medium", "High", "Critical / Emergency"
  final double confidence;
  final String reasoning;
  final List<String> detectedKeywords;

  const AIAnalysisResult({
    required this.suggestedCategory,
    required this.severity,
    required this.confidence,
    required this.reasoning,
    required this.detectedKeywords,
  });
}

class AIService {
  // Official PUP Alert / SafeWalk incident categories matching Kotlin SubmitReportActivity.kt
  static const List<String> categories = [
    'Facilities',
    'Maintenance',
    'Safety',
    'Cleanliness',
    'Equipment',
    'Other',
  ];

  // Official keyword sets matching SubmitReportActivity.kt lines 172-185
  static const List<String> safetyKeywords = [
    'hazard',
    'fire',
    'smoke',
    'exposed',
    'slippery',
    'emergency',
    'danger',
    'alarm',
    'lock',
    'gate',
    'security',
    'injury',
    'glass',
    'sharp',
    'knife',
    'threat',
    'fight',
  ];

  static const List<String> maintenanceKeywords = [
    'leak',
    'pipe',
    'water',
    'plumbing',
    'electrical',
    'wire',
    'plug',
    'outlet',
    'light',
    'bulb',
    'fan',
    'aircon',
    'ac',
    'broken',
    'damaged',
    'repair',
    'flush',
    'toilet',
    'faucet',
    'sink',
    'drain',
  ];

  static const List<String> cleanlinessKeywords = [
    'trash',
    'garbage',
    'dirty',
    'smell',
    'odor',
    'waste',
    'spill',
    'mess',
    'clutter',
    'restroom',
    'washroom',
    'clean',
    'dust',
  ];

  static const List<String> equipmentKeywords = [
    'projector',
    'tv',
    'screen',
    'computer',
    'pc',
    'monitor',
    'speaker',
    'mic',
    'microphone',
    'lab',
    'printer',
    'appliance',
    'mouse',
    'keyboard',
  ];

  static const List<String> facilitiesKeywords = [
    'door',
    'window',
    'wall',
    'ceiling',
    'roof',
    'elevator',
    'stairs',
    'desk',
    'chair',
    'table',
    'board',
    'building',
    'hall',
    'room',
  ];

  // Feature 4: Smart Auto-Categorization matching Kotlin detectCategory function exactly
  static String detectCategory(String text) {
    final lower = text.toLowerCase();

    if (safetyKeywords.any((k) => lower.contains(k))) {
      return 'Safety';
    }
    if (maintenanceKeywords.any((k) => lower.contains(k))) {
      return 'Maintenance';
    }
    if (cleanlinessKeywords.any((k) => lower.contains(k))) {
      return 'Cleanliness';
    }
    if (equipmentKeywords.any((k) => lower.contains(k))) {
      return 'Equipment';
    }
    if (facilitiesKeywords.any((k) => lower.contains(k))) {
      return 'Facilities';
    }

    return '';
  }

  // SafeWalk Hazard Severity & Priority Engine
  static AIAnalysisResult analyzeIncident(String text) {
    if (text.trim().isEmpty) {
      return const AIAnalysisResult(
        suggestedCategory: 'Facilities',
        severity: 'Low',
        confidence: 0.0,
        reasoning: 'No description entered yet.',
        detectedKeywords: [],
      );
    }

    final lower = text.toLowerCase();
    final detectedCategory = detectCategory(text);
    final category = detectedCategory.isNotEmpty ? detectedCategory : 'Other';

    final matched = <String>[];
    for (final kw in [
      ...safetyKeywords,
      ...maintenanceKeywords,
      ...cleanlinessKeywords,
      ...equipmentKeywords,
      ...facilitiesKeywords,
    ]) {
      if (lower.contains(kw) && !matched.contains(kw)) {
        matched.add(kw);
      }
    }

    // Determine severity based on critical signals
    String severity = 'Low';
    if (lower.contains('fire') ||
        lower.contains('smoke') ||
        lower.contains('emergency') ||
        lower.contains('knife') ||
        lower.contains('threat') ||
        lower.contains('injury') ||
        lower.contains('exposed wire') ||
        lower.contains('collapse')) {
      severity = 'Emergency';
    } else if (lower.contains('hazard') ||
        lower.contains('danger') ||
        lower.contains('slippery') ||
        lower.contains('fight') ||
        lower.contains('electrical') ||
        lower.contains('spark') ||
        lower.contains('leak') ||
        lower.contains('shattered')) {
      severity = 'High';
    } else if (lower.contains('broken') ||
        lower.contains('damaged') ||
        lower.contains('clogged') ||
        lower.contains('flood') ||
        lower.contains('repair') ||
        lower.contains('stolen')) {
      severity = 'Medium';
    }

    final double confidence = matched.isNotEmpty
        ? (0.6 + (matched.length * 0.08)).clamp(0.65, 0.98)
        : 0.3;

    final reasoning = matched.isNotEmpty
        ? 'AI matched safety keywords: "${matched.take(3).join(', ')}" suggesting category "$category" with $severity priority.'
        : 'Classified based on general campus facility report patterns.';

    return AIAnalysisResult(
      suggestedCategory: category,
      severity: severity,
      confidence: confidence,
      reasoning: reasoning,
      detectedKeywords: matched,
    );
  }
}
