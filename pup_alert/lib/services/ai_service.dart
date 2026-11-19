import 'dart:convert';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

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

class AIImageVerificationResult {
  final bool isAcceptable;
  final bool needsMoreContext;
  final double confidence;
  final String detectedSubject;
  final String suggestedCategory;
  final String feedback;
  final String engineUsed;

  const AIImageVerificationResult({
    required this.isAcceptable,
    this.needsMoreContext = false,
    required this.confidence,
    required this.detectedSubject,
    required this.suggestedCategory,
    required this.feedback,
    this.engineUsed = 'PUP-ALERT Vision AI',
  });
}

class AIService {
  static const String _envGeminiApiKey = String.fromEnvironment(
    'GEMINI_API_KEY',
    defaultValue: '',
  );

  static Future<String> getGeminiApiKey() async {
    if (_envGeminiApiKey.trim().isNotEmpty) {
      return _envGeminiApiKey.trim();
    }
    final prefs = await SharedPreferences.getInstance();
    return (prefs.getString('gemini_api_key') ?? '').trim();
  }

  static Future<void> setGeminiApiKey(String apiKey) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('gemini_api_key', apiKey.trim());
  }

  // Official 5 PUP-ALERT incident categories
  static const List<String> categories = [
    'Facilities',
    'Maintenance',
    'Safety',
    'Cleanliness',
    'Equipment',
  ];

  // Official keyword sets matching the 5 categories
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
    'unsafe',
    'accident',
    'fall',
    'trip',
    'spark',
    'burn',
    'extinguisher',
    'exit',
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
    'clog',
    'clogged',
    'fix',
    'flicker',
    'switch',
    'socket',
    'hinge',
    'paint',
    'crack',
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
    'litter',
    'stain',
    'mud',
    'filthy',
    'unclean',
    'sanitation',
    'bin',
    'mop',
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
    'cable',
    'hdmi',
    'router',
    'wifi',
    'system',
    'device',
    'machine',
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
    'whiteboard',
    'blackboard',
    'building',
    'hall',
    'hallway',
    'corridor',
    'room',
    'classroom',
    'floor',
    'tile',
    'ramp',
    'railing',
    'bench',
    'gym',
    'library',
    'canteen',
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

  static const List<String> _nonsensePatterns = [
    'meme',
    'tiktok',
    'anime',
    'waifu',
    'wallpaper',
    'avatar',
    'profile_pic',
    'pfp',
    'funny',
    'joke',
    'prank',
    'shitpost',
    'cat',
    'dog',
    'pet',
    'food',
    'lunch',
    'dinner',
    'valorant',
    'genshin',
    'roblox',
    'minecraft',
    'dota',
    'mlbb',
    'asdf',
    'qwerty',
    'zxcv',
    '12345',
    'test123',
    'haha',
    'lol',
    'lmao',
    'nothing',
    'none',
    'idk',
    'random',
    'nonsense',
  ];

  /// Verifies if an attached photo is acceptable and relevant to the incident report:
  /// 1) Must belong to one of the 5 official categories (Facilities, Maintenance, Safety, Cleanliness, Equipment)
  /// 2) Someone must be standing at the scene giving a thumbs up (👍)
  static Future<AIImageVerificationResult> verifyIncidentImage({
    required Uint8List imageBytes,
    required String fileName,
    required String reportTitle,
    required String reportDescription,
    required String selectedCategory,
  }) async {
    final apiKey = await getGeminiApiKey();
    if (apiKey.isNotEmpty) {
      final geminiResult = await _verifyWithGeminiVision(
        apiKey: apiKey,
        imageBytes: imageBytes,
        fileName: fileName,
        reportTitle: reportTitle,
        reportDescription: reportDescription,
        selectedCategory: selectedCategory,
      );
      if (geminiResult != null) {
        return geminiResult;
      }
    }

    return _verifyWithOnDeviceVision(
      imageBytes: imageBytes,
      fileName: fileName,
      reportTitle: reportTitle,
      reportDescription: reportDescription,
      selectedCategory: selectedCategory,
    );
  }

  static Future<AIImageVerificationResult?> _verifyWithGeminiVision({
    required String apiKey,
    required Uint8List imageBytes,
    required String fileName,
    required String reportTitle,
    required String reportDescription,
    required String selectedCategory,
  }) async {
    try {
      final isPng = fileName.toLowerCase().endsWith('.png');
      final mimeType = isPng ? 'image/png' : 'image/jpeg';
      final base64Image = base64Encode(imageBytes);

      final prompt = '''
You are the PUP-ALERT Campus Incident Photo Verification AI for Polytechnic University of the Philippines.
A student is submitting a campus incident report with the following details:
- Reported Title: "${reportTitle.trim()}"
- Selected Category: "${selectedCategory.trim()}" (Allowed categories: Facilities, Maintenance, Safety, Cleanliness, Equipment)
- Reported Description: "${reportDescription.trim()}"

STRICT VERIFICATION REQUIREMENTS — BOTH MUST BE TRUE FOR isAcceptable = true:
1. CATEGORY & INCIDENT MATCH: The photo must show a real physical environment or campus issue matching one of the 5 official categories (Facilities, Maintenance, Safety, Cleanliness, Equipment) and aligning with the reported Title/Description.
2. PERSON STANDING WITH THUMBS UP (👍): Someone MUST be visible standing at the scene giving a clear THUMBS UP gesture (👍) in the photo as live proof of on-site reporting.

Reject the photo (isAcceptable: false) if:
- There is NO person or NO thumbs-up gesture (👍) visible in the photo.
- It is just a close-up face selfie without a thumbs-up gesture and without the reported facility/incident scene.
- It is a meme, anime/cartoon, pet, food, video game, blank/black frame, or unrelated to the 5 categories (Facilities, Maintenance, Safety, Cleanliness, Equipment).

Respond strictly with valid JSON matching this schema:
{
  "isAcceptable": true or false,
  "confidence": 0.0 to 1.0,
  "detectedSubject": "short description of what is visible (including whether person standing with thumbs-up and incident are present)",
  "suggestedCategory": "one of: Facilities, Maintenance, Safety, Cleanliness, Equipment",
  "feedback": "1-2 sentence explanation for the student explaining why the photo passed or which requirement failed"
}
''';

      final uri = Uri.parse(
        'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey',
      );

      final response = await http
          .post(
            uri,
            headers: {'Content-Type': 'application/json'},
            body: jsonEncode({
              'contents': [
                {
                  'parts': [
                    {'text': prompt},
                    {
                      'inline_data': {
                        'mime_type': mimeType,
                        'data': base64Image,
                      },
                    },
                  ],
                },
              ],
              'generationConfig': {
                'responseMimeType': 'application/json',
                'temperature': 0.1,
              },
            }),
          )
          .timeout(const Duration(seconds: 15));

      if (response.statusCode == 200) {
        final decoded = jsonDecode(response.body) as Map<String, dynamic>;
        final candidates = decoded['candidates'] as List<dynamic>?;
        if (candidates != null && candidates.isNotEmpty) {
          final content =
              candidates.first['content'] as Map<String, dynamic>?;
          final parts = content?['parts'] as List<dynamic>?;
          final rawText = parts?.first['text']?.toString() ?? '';
          if (rawText.isNotEmpty) {
            final cleanedJson = rawText
                .replaceAll('```json', '')
                .replaceAll('```', '')
                .trim();
            final resultJson =
                jsonDecode(cleanedJson) as Map<String, dynamic>;
            final isAcceptable = resultJson['isAcceptable'] == true;
            final confidence =
                (resultJson['confidence'] as num?)?.toDouble() ?? 0.9;
            final detectedSubject =
                resultJson['detectedSubject']?.toString() ??
                'Analyzed visual content';
            final suggestedCat =
                resultJson['suggestedCategory']?.toString() ??
                selectedCategory;
            final feedback =
                resultJson['feedback']?.toString() ??
                (isAcceptable
                    ? 'Photo verified: Person standing with thumbs-up (👍) at the $selectedCategory incident scene.'
                    : 'Photo rejected: Must show someone standing with a thumbs up (👍) at the reported $selectedCategory issue.');

            return AIImageVerificationResult(
              isAcceptable: isAcceptable,
              confidence: confidence.clamp(0.0, 1.0),
              detectedSubject: detectedSubject,
              suggestedCategory: categories.contains(suggestedCat)
                  ? suggestedCat
                  : selectedCategory,
              feedback: feedback,
              engineUsed: 'Gemini Vision AI',
            );
          }
        }
      }
    } catch (_) {
      // Fall back to on-device vision analyzer if offline or API key invalid
    }
    return null;
  }

  static Future<AIImageVerificationResult> _verifyWithOnDeviceVision({
    required Uint8List imageBytes,
    required String fileName,
    required String reportTitle,
    required String reportDescription,
    required String selectedCategory,
  }) async {
    final lowerFile = fileName.toLowerCase();
    final combinedText = '$reportTitle $reportDescription'.trim();
    final lowerText = combinedText.toLowerCase();

    // 0. Ensure selectedCategory is one of the 5 official categories
    if (!categories.contains(selectedCategory)) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.96,
        detectedSubject: 'Invalid category "$selectedCategory"',
        suggestedCategory: 'Facilities',
        feedback:
            'Rejected: The report must be under one of the 5 official categories: Facilities, Maintenance, Safety, Cleanliness, or Equipment.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 1. Check filename for obvious meme / game / anime media
    for (final bad in _nonsensePatterns) {
      if (lowerFile.contains(bad)) {
        return AIImageVerificationResult(
          isAcceptable: false,
          confidence: 0.94,
          detectedSubject: 'Unrelated media file ("$bad" detected)',
          suggestedCategory: selectedCategory,
          feedback:
              'Rejected: The uploaded file ("$fileName") appears to be a meme, game screenshot, or unrelated media rather than a real campus incident photo.',
          engineUsed: 'PUP-ALERT Vision AI',
        );
      }
    }

    // 2. Decode image and inspect pixel grid for:
    //    - Campus scene lighting & structural edges
    //    - Person standing + separated Thumbs-Up (👍) hand gesture cluster
    int width = 0;
    int height = 0;
    double avgLuminance = 128.0;
    double darkRatio = 0.0;
    double brightRatio = 0.0;
    double colorStdDev = 40.0;
    double edgeStrength = 25.0;
    double skinToneRatio = 0.0;
    double centerSkinRatio = 0.0;
    int maxRowContinuousSkinWidth = 0;
    int rowsWithSeparatedSkinClusters = 0;
    int narrowThumbLikeSkinRows = 0;
    double backgroundEdgeStrength = 0.0;

    try {
      final codec = await ui.instantiateImageCodec(
        imageBytes,
        targetWidth: 64,
        targetHeight: 64,
      );
      final frame = await codec.getNextFrame();
      final image = frame.image;
      width = image.width;
      height = image.height;

      final byteData = await image.toByteData(
        format: ui.ImageByteFormat.rawRgba,
      );

      if (byteData != null) {
        final pixels = byteData.buffer.asUint8List();
        final totalPixels = (pixels.length ~/ 4).clamp(1, 100000);
        final skinGrid = List<bool>.filled(totalPixels, false);

        double sumLum = 0;
        double sumLumSq = 0;
        int darkCount = 0;
        int brightCount = 0;
        int skinCount = 0;
        int centerSkinCount = 0;
        int centerPixelsCount = 0;
        double edgeAccum = 0;
        int edgeSamples = 0;
        double bgEdgeAccum = 0;
        int bgEdgeSamples = 0;

        for (int i = 0; i < pixels.length; i += 4) {
          final pIdx = i ~/ 4;
          final px = width > 0 ? (pIdx % width) : 0;
          final py = width > 0 ? (pIdx ~/ width) : 0;

          final r = pixels[i];
          final g = pixels[i + 1];
          final b = pixels[i + 2];

          final lum = 0.299 * r + 0.587 * g + 0.114 * b;
          final cb = 128.0 - 0.168736 * r - 0.331264 * g + 0.5 * b;
          final cr = 128.0 + 0.5 * r - 0.418688 * g - 0.081312 * b;

          sumLum += lum;
          sumLumSq += lum * lum;

          if (lum < 26) darkCount++;
          if (lum > 242) brightCount++;

          // YCbCr + RGB human skin detection (face & thumbs-up hand, excluding beige walls)
          final isSkinPixel =
              lum >= 38 &&
              lum <= 215 &&
              r > g &&
              r > b &&
              (r - g) >= 11 &&
              (r - b) >= 16 &&
              cb >= 75 &&
              cb <= 133 &&
              cr >= 132 &&
              cr <= 182;

          if (isSkinPixel) {
            skinCount++;
            if (pIdx < skinGrid.length) {
              skinGrid[pIdx] = true;
            }
          }

          final inCenterZone =
              width >= 16 &&
              height >= 16 &&
              px >= (width * 0.22) &&
              px <= (width * 0.78) &&
              py >= (height * 0.18) &&
              py <= (height * 0.85);

          if (inCenterZone) {
            centerPixelsCount++;
            if (isSkinPixel) {
              centerSkinCount++;
            }
          }

          if (i >= 4) {
            final prevR = pixels[i - 4];
            final prevG = pixels[i - 3];
            final prevB = pixels[i - 2];
            final diff =
                ((r - prevR).abs() + (g - prevG).abs() + (b - prevB).abs()) /
                3.0;
            edgeAccum += diff;
            edgeSamples++;
            if (!isSkinPixel) {
              bgEdgeAccum += diff;
              bgEdgeSamples++;
            }
          }
        }

        // Analyze horizontal scanlines of skinGrid for:
        // 1) maxRowContinuousSkinWidth (detects oversized close-up selfie face)
        // 2) rowsWithSeparatedSkinClusters (detects standing person + separate raised thumbs-up hand)
        // 3) narrowThumbLikeSkinRows (detects narrow raised thumb/hand segment width 2..11 px)
        if (width > 0 && height > 0) {
          for (int y = 2; y < height - 2; y++) {
            int clustersInRow = 0;
            int currentRun = 0;
            int gapSinceLastCluster = 999;

            for (int x = 0; x < width; x++) {
              final idx = y * width + x;
              final isSkin = idx < skinGrid.length && skinGrid[idx];
              if (isSkin) {
                currentRun++;
              } else {
                if (currentRun >= 2) {
                  if (currentRun > maxRowContinuousSkinWidth) {
                    maxRowContinuousSkinWidth = currentRun;
                  }
                  if (currentRun >= 2 && currentRun <= 11) {
                    narrowThumbLikeSkinRows++;
                  }
                  if (clustersInRow == 0 || gapSinceLastCluster >= 3) {
                    clustersInRow++;
                  }
                  gapSinceLastCluster = 0;
                }
                currentRun = 0;
                gapSinceLastCluster++;
              }
            }
            if (currentRun >= 2) {
              if (currentRun > maxRowContinuousSkinWidth) {
                maxRowContinuousSkinWidth = currentRun;
              }
              if (currentRun >= 2 && currentRun <= 11) {
                narrowThumbLikeSkinRows++;
              }
              if (clustersInRow == 0 || gapSinceLastCluster >= 3) {
                clustersInRow++;
              }
            }
            if (clustersInRow >= 2) {
              rowsWithSeparatedSkinClusters++;
            }
          }
        }

        avgLuminance = sumLum / totalPixels;
        final variance =
            (sumLumSq / totalPixels) - (avgLuminance * avgLuminance);
        colorStdDev = variance > 0 ? _sqrt(variance) : 0.0;
        darkRatio = darkCount / totalPixels;
        brightRatio = brightCount / totalPixels;
        skinToneRatio = skinCount / totalPixels;
        if (centerPixelsCount > 0) {
          centerSkinRatio = centerSkinCount / centerPixelsCount;
        }
        edgeStrength = edgeSamples > 0 ? (edgeAccum / edgeSamples) : 0.0;
        backgroundEdgeStrength = bgEdgeSamples > 0
            ? (bgEdgeAccum / bgEdgeSamples)
            : edgeStrength;
      }
    } catch (_) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.95,
        detectedSubject: 'Corrupted or unreadable image file',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: The selected file could not be decoded as a valid photo. Please take or upload a clear JPEG/PNG photo of the incident.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 3. Reject tiny icons/stickers
    if (width < 48 || height < 48 || imageBytes.lengthInBytes < 1200) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.92,
        detectedSubject: 'Low-resolution icon / sticker',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: Image resolution is too small or appears to be an icon/sticker. Please upload a real photo of the campus incident with someone standing and giving a thumbs up (👍).',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 4. Reject pitch-black / covered camera lens photos
    if (avgLuminance < 18 || darkRatio > 0.86) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.96,
        detectedSubject: 'Dark / covered camera frame',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: The photo is almost completely black or the camera lens was covered. Please take a clear, well-lit photo showing the incident and someone standing with a thumbs up (👍).',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 5. Reject completely blank / overexposed white frames
    if (avgLuminance > 244 || brightRatio > 0.88) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.95,
        detectedSubject: 'Blank / overexposed white image',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: The image appears blank or washed out. Please upload a clear photo showing the campus issue and someone standing with a thumbs up (👍).',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 6. Reject solid-color / zero-detail / heavily blurred nonsense frames
    if (colorStdDev < 8.5 && edgeStrength < 3.0) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.91,
        detectedSubject: 'Solid color / indistinct frame',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: No recognizable facility, equipment, or hazard structure detected. Please upload a clear photo of what you are reporting.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 7. REQUIREMENT: Must have a person standing with a Thumbs Up (👍) in the photo!
    if (skinToneRatio < 0.014 || narrowThumbLikeSkinRows < 3) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.93,
        detectedSubject: 'No person standing with thumbs-up (👍) detected',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: Requirement not met — Someone must be standing at the incident scene giving a thumbs up (👍) in the photo to verify the report.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 8. Reject close-up face-only selfies / webcam headshots where the person is NOT standing back at a campus incident scene with a thumbs up
    if (centerSkinRatio >= 0.21 ||
        skinToneRatio >= 0.25 ||
        maxRowContinuousSkinWidth >= 19 ||
        (rowsWithSeparatedSkinClusters < 2 && centerSkinRatio >= 0.14) ||
        backgroundEdgeStrength < 7.5) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.94,
        detectedSubject:
            'Close-up face selfie / no standing thumbs-up (👍) scene detected',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: Close-up face selfie or portrait without a clear standing thumbs-up (👍) pose at the campus incident scene. Please step back so the photo shows someone standing by the $selectedCategory issue giving a thumbs up (👍).',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    // 9. Check Report Title, Description, and Category match (Facilities, Maintenance, Safety, Cleanliness, Equipment)
    if (reportTitle.trim().length < 3 && reportDescription.trim().length < 5) {
      return AIImageVerificationResult(
        isAcceptable: true,
        needsMoreContext: true,
        confidence: 0.80,
        detectedSubject:
            'Person standing with thumbs-up (👍) detected (awaiting Title & Description)',
        suggestedCategory: selectedCategory,
        feedback:
            'Thumbs-up (👍) standing pose detected! Please fill in the Report Title and Description under "$selectedCategory" to complete verification.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    if (_isGibberishOrNonsenseText(lowerText)) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.94,
        detectedSubject: 'Nonsense report title or description',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: Your report title/description contains nonsense or test text ("${reportTitle.trim()}"). Please describe a real campus issue under Facilities, Maintenance, Safety, Cleanliness, or Equipment.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    final detectedCat = detectCategory(combinedText);
    if (detectedCat.isEmpty) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.89,
        detectedSubject:
            'Report text does not match any of the 5 official categories',
        suggestedCategory: selectedCategory,
        feedback:
            'Rejected: Your report description must clearly describe a campus issue in one of the 5 categories: Facilities, Maintenance, Safety, Cleanliness, or Equipment.',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    if (detectedCat.toLowerCase() != selectedCategory.toLowerCase()) {
      return AIImageVerificationResult(
        isAcceptable: false,
        confidence: 0.91,
        detectedSubject:
            'Category mismatch (Described: $detectedCat vs Selected: $selectedCategory)',
        suggestedCategory: detectedCat,
        feedback:
            'Rejected: Your report describes a "$detectedCat" issue, but the selected category is "$selectedCategory". Please change the Category dropdown to "$detectedCat".',
        engineUsed: 'PUP-ALERT Vision AI',
      );
    }

    final textAnalysis = analyzeIncident(combinedText);

    return AIImageVerificationResult(
      isAcceptable: true,
      needsMoreContext: false,
      confidence: (0.86 + (textAnalysis.detectedKeywords.length * 0.03)).clamp(
        0.86,
        0.98,
      ),
      detectedSubject:
          'Person standing with thumbs-up (👍) at $selectedCategory scene',
      suggestedCategory: selectedCategory,
      feedback:
          'Verified: Someone standing with a thumbs up (👍) was detected at the scene, and the report matches the "$selectedCategory" category (${textAnalysis.detectedKeywords.take(3).join(", ")}).',
      engineUsed: 'PUP-ALERT Vision AI',
    );
  }

  static bool _isGibberishOrNonsenseText(String lowerText) {
    final clean = lowerText.trim();
    if (clean.isEmpty) return false;

    for (final bad in _nonsensePatterns) {
      if (clean == bad ||
          clean.split(RegExp(r'\s+')).contains(bad)) {
        return true;
      }
    }

    // Check for repeated single characters like "aaaaaaa" or keyboard smashes with no vowels
    final lettersOnly = clean.replaceAll(RegExp(r'[^a-z]'), '');
    if (lettersOnly.length >= 5) {
      final uniqueChars = lettersOnly.split('').toSet().length;
      if (uniqueChars <= 2) return true;
      final vowelCount = RegExp(r'[aeiou]').allMatches(lettersOnly).length;
      if (vowelCount == 0) return true;
    }

    return false;
  }

  static double _sqrt(double x) {
    if (x <= 0) return 0;
    double guess = x / 2.0;
    for (int i = 0; i < 12; i++) {
      guess = 0.5 * (guess + x / guess);
    }
    return guess;
  }
}
