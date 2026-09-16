class Review {
  final String id;
  final int rating;
  final String text;
  final String reviewerName;
  final String reviewerStudentId;
  final int likes;
  final int dislikes;
  final bool adminLiked;
  final String adminReply;
  final int adminRepliedAt;
  final String adminReplierName;
  final int timestamp;

  Review({
    this.id = '',
    this.rating = 5,
    this.text = '',
    this.reviewerName = '',
    this.reviewerStudentId = '',
    this.likes = 0,
    this.dislikes = 0,
    this.adminLiked = false,
    this.adminReply = '',
    this.adminRepliedAt = 0,
    this.adminReplierName = '',
    this.timestamp = 0,
  });

  factory Review.fromFirestore(Map<String, dynamic> data, String documentId) {
    return Review(
      id: documentId,
      rating: (data['rating'] as num?)?.toInt() ?? 5,
      text: data['text'] as String? ?? '',
      reviewerName: data['reviewerName'] as String? ?? '',
      reviewerStudentId: data['reviewerStudentId'] as String? ?? '',
      likes: (data['likes'] as num?)?.toInt() ?? 0,
      dislikes: (data['dislikes'] as num?)?.toInt() ?? 0,
      adminLiked: data['adminLiked'] as bool? ?? false,
      adminReply: data['adminReply'] as String? ?? '',
      adminRepliedAt: (data['adminRepliedAt'] as num?)?.toInt() ?? 0,
      adminReplierName: data['adminReplierName'] as String? ?? '',
      timestamp: (data['timestamp'] as num?)?.toInt() ?? 0,
    );
  }

  Map<String, dynamic> toFirestore() {
    return {
      'rating': rating,
      'text': text,
      'reviewerName': reviewerName,
      'reviewerStudentId': reviewerStudentId,
      'likes': likes,
      'dislikes': dislikes,
      'adminLiked': adminLiked,
      'adminReply': adminReply,
      'adminRepliedAt': adminRepliedAt,
      'adminReplierName': adminReplierName,
      'timestamp': timestamp,
    };
  }
}
