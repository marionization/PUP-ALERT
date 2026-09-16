import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/material.dart';

import '../../models/accessibility_settings.dart';
import '../../services/auth_service.dart';

class CampusPollsScreen extends StatefulWidget {
  final String userRole;
  final bool isEmbedded;
  final AccessibilitySettings? settings;

  const CampusPollsScreen({
    super.key,
    required this.userRole,
    this.isEmbedded = false,
    this.settings,
  });

  @override
  State<CampusPollsScreen> createState() => _CampusPollsScreenState();
}

class _CampusPollsScreenState extends State<CampusPollsScreen> {
  final _firestore = FirebaseFirestore.instance;
  final _authService = AuthService();

  Future<void> _vote(
    String pollId,
    int optionIndex,
    List<dynamic> options,
    List<dynamic> voters,
  ) async {
    final uid = _authService.currentUser?.uid ?? 'anon_uid';
    if (voters.contains(uid)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('You have already voted on this poll!')),
      );
      return;
    }

    final updatedOptions = List<Map<String, dynamic>>.from(options);
    final currentVotes =
        (updatedOptions[optionIndex]['votes'] as num?)?.toInt() ?? 0;
    updatedOptions[optionIndex]['votes'] = currentVotes + 1;

    await _firestore.collection('campus_polls').doc(pollId).update({
      'options': updatedOptions,
      'voters': FieldValue.arrayUnion([uid]),
    });

    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Vote recorded! Thank you.'),
        backgroundColor: Colors.green,
      ),
    );
  }

  void _showCreatePollDialog() {
    final questionCtrl = TextEditingController();
    final opt1Ctrl = TextEditingController();
    final opt2Ctrl = TextEditingController();
    final opt3Ctrl = TextEditingController();

    showDialog(
      context: context,
      builder: (context) {
        return AlertDialog(
          title: const Text('Create New Campus Poll'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: questionCtrl,
                decoration: const InputDecoration(
                  labelText: 'Poll Question',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: opt1Ctrl,
                decoration: const InputDecoration(
                  labelText: 'Option 1',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: opt2Ctrl,
                decoration: const InputDecoration(
                  labelText: 'Option 2',
                  border: OutlineInputBorder(),
                ),
              ),
              const SizedBox(height: 8),
              TextField(
                controller: opt3Ctrl,
                decoration: const InputDecoration(
                  labelText: 'Option 3 (Optional)',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () async {
                final q = questionCtrl.text.trim();
                final o1 = opt1Ctrl.text.trim();
                final o2 = opt2Ctrl.text.trim();
                final o3 = opt3Ctrl.text.trim();

                if (q.isEmpty || o1.isEmpty || o2.isEmpty) return;
                Navigator.pop(context);

                final options = [
                  {'text': o1, 'votes': 0},
                  {'text': o2, 'votes': 0},
                ];
                if (o3.isNotEmpty) {
                  options.add({'text': o3, 'votes': 0});
                }

                await _firestore.collection('campus_polls').add({
                  'question': q,
                  'options': options,
                  'voters': [],
                  'timestamp': FieldValue.serverTimestamp(),
                });
              },
              child: const Text('Create'),
            ),
          ],
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final isAdmin = widget.userRole.toLowerCase() == 'admin';
    final currentUid = _authService.currentUser?.uid ?? '';
    final settings = widget.settings ?? const AccessibilitySettings();

    final bodyContent = StreamBuilder<QuerySnapshot>(
      stream: _firestore
          .collection('campus_polls')
          .orderBy('timestamp', descending: true)
          .snapshots(),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }

        final docs = snapshot.data?.docs ?? [];
        if (docs.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(
                  Icons.how_to_vote_outlined,
                  size: 60,
                  color: Colors.grey,
                ),
                const SizedBox(height: 12),
                const Text(
                  'No active campus polls right now.',
                  style: TextStyle(color: Colors.grey),
                ),
                if (isAdmin) ...[
                  const SizedBox(height: 12),
                  ElevatedButton(
                    onPressed: _showCreatePollDialog,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: settings.accentColor,
                    ),
                    child: const Text('Create First Poll'),
                  ),
                ],
              ],
            ),
          );
        }

        return ListView.builder(
          padding: EdgeInsets.all(settings.cardPadding),
          itemCount: docs.length,
          itemBuilder: (context, index) {
            final doc = docs[index];
            final data = doc.data() as Map<String, dynamic>;
            final question = data['question'] ?? '';
            final options = (data['options'] as List<dynamic>?) ?? [];
            final voters = (data['voters'] as List<dynamic>?) ?? [];
            final hasVoted = voters.contains(currentUid);

            int totalVotes = 0;
            for (var opt in options) {
              totalVotes += (opt['votes'] as num?)?.toInt() ?? 0;
            }

            return Card(
              color: settings.cardColor,
              elevation: settings.reduceMotion ? 0 : 2,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(settings.cardBorderRadius),
                side: BorderSide(color: settings.borderColor),
              ),
              margin: const EdgeInsets.symmetric(vertical: 8),
              child: Padding(
                padding: EdgeInsets.all(settings.cardPadding),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            question,
                            style: TextStyle(
                              fontSize: settings.titleFontSize,
                              fontWeight: settings.titleFontWeight,
                              color: settings.primaryTextColor,
                            ),
                          ),
                        ),
                        if (hasVoted)
                          const Chip(
                            label: Text(
                              'VOTED',
                              style: TextStyle(
                                fontSize: 10,
                                color: Colors.green,
                              ),
                            ),
                            backgroundColor: Color(0xFFE8F5E9),
                          ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    ...options.asMap().entries.map((entry) {
                      final i = entry.key;
                      final opt = entry.value;
                      final optText = opt['text'] ?? '';
                      final votes = (opt['votes'] as num?)?.toInt() ?? 0;
                      final percent = totalVotes > 0
                          ? (votes / totalVotes)
                          : 0.0;

                      return Padding(
                        padding: const EdgeInsets.symmetric(vertical: 6),
                        child: InkWell(
                          borderRadius: BorderRadius.circular(8),
                          onTap: hasVoted
                              ? null
                              : () => _vote(doc.id, i, options, voters),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Text(
                                    optText,
                                    style: TextStyle(
                                      fontWeight: settings.mediumFontWeight,
                                      fontSize: settings.bodyFontSize,
                                      color: settings.primaryTextColor,
                                    ),
                                  ),
                                  Text(
                                    '${(percent * 100).toStringAsFixed(0)}% ($votes)',
                                    style: TextStyle(
                                      fontSize: settings.smallFontSize,
                                      color: settings.secondaryTextColor,
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              ClipRRect(
                                borderRadius: BorderRadius.circular(4),
                                child: LinearProgressIndicator(
                                  value: percent,
                                  minHeight: 8,
                                  color: settings.accentColor,
                                  backgroundColor: settings.borderColor,
                                ),
                              ),
                            ],
                          ),
                        ),
                      );
                    }),
                    const SizedBox(height: 8),
                    Text(
                      'Total votes: $totalVotes',
                      style: TextStyle(
                        fontSize: settings.smallFontSize,
                        color: settings.secondaryTextColor,
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

    if (widget.isEmbedded) {
      return Column(
        children: [
          if (isAdmin)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              child: Align(
                alignment: Alignment.centerRight,
                child: ElevatedButton.icon(
                  onPressed: _showCreatePollDialog,
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Create New Poll'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: settings.accentColor,
                  ),
                ),
              ),
            ),
          Expanded(child: bodyContent),
        ],
      );
    }

    return Scaffold(
      backgroundColor: settings.backgroundColor,
      appBar: AppBar(
        title: const Text('Student & Campus Polls'),
        backgroundColor: settings.headerColor,
        actions: [
          if (isAdmin)
            IconButton(
              icon: const Icon(Icons.add_chart),
              tooltip: 'Create Poll',
              onPressed: _showCreatePollDialog,
            ),
        ],
      ),
      body: bodyContent,
    );
  }
}
