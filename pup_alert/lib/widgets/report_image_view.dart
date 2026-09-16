import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';

/// Reusable widget for displaying incident report images across Web and Mobile.
/// Supports HTTP/HTTPS network URLs, base64 Data URIs (data:image/...;base64,...),
/// and raw base64 strings with interactive tap-to-zoom support.
///
/// On Flutter Web (CanvasKit), direct requests to Firebase Storage can be blocked
/// by browser CORS policies when buckets don't have localhost in their allowed origins.
/// This widget handles that transparently by routing through high-performance CORS
/// image proxies (wsrv.nl / Cloudflare CDN) with multi-tier failover.
class ReportImageView extends StatefulWidget {
  final String imageUrl;
  final double? height;
  final double? width;
  final BoxFit fit;
  final BorderRadius? borderRadius;
  final bool enableZoom;
  final Widget? placeholder;
  final Widget? errorWidget;

  const ReportImageView({
    super.key,
    required this.imageUrl,
    this.height,
    this.width,
    this.fit = BoxFit.cover,
    this.borderRadius,
    this.enableZoom = false,
    this.placeholder,
    this.errorWidget,
  });

  @override
  State<ReportImageView> createState() => _ReportImageViewState();
}

class _ReportImageViewState extends State<ReportImageView> {
  int _candidateIndex = 0;
  List<String> _candidateUrls = [];
  bool _allFailed = false;

  @override
  void initState() {
    super.initState();
    _resolveCandidates();
  }

  @override
  void didUpdateWidget(covariant ReportImageView oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.imageUrl != widget.imageUrl) {
      _resolveCandidates();
    }
  }

  void _resolveCandidates() {
    _candidateIndex = 0;
    _allFailed = false;
    final clean = widget.imageUrl.trim();
    if (clean.isEmpty || _isBase64(clean)) {
      _candidateUrls = [];
      return;
    }

    final isStorage = _isCloudStorage(clean);
    final encoded = Uri.encodeComponent(clean);
    final wsrvUrl = 'https://wsrv.nl/?url=$encoded';
    final weservUrl = 'https://images.weserv.nl/?url=$encoded';

    if (kIsWeb) {
      // On Flutter Web, Google Cloud / Firebase Storage buckets block localhost due to CORS.
      // Therefore, routing through wsrv.nl provides instant Access-Control-Allow-Origin: * headers.
      if (isStorage) {
        _candidateUrls = [wsrvUrl, weservUrl, clean];
      } else {
        _candidateUrls = [clean, wsrvUrl, weservUrl];
      }
    } else {
      // On native mobile (Android/iOS), direct downloads have no browser CORS restrictions.
      _candidateUrls = [clean, wsrvUrl, weservUrl];
    }
  }

  bool _isBase64(String source) {
    final clean = source.trim();
    return clean.startsWith('data:image') ||
        clean.startsWith('data:application') ||
        (!clean.startsWith('http://') &&
            !clean.startsWith('https://') &&
            !clean.startsWith('blob:') &&
            clean.length > 50);
  }

  bool _isCloudStorage(String url) {
    final lower = url.toLowerCase();
    return lower.contains('firebasestorage.googleapis.com') ||
        lower.contains('.firebasestorage.app') ||
        lower.contains('storage.googleapis.com') ||
        lower.contains('googleusercontent.com');
  }

  Uint8List? _decodeBase64(String source) {
    try {
      final clean = source.trim();
      final commaIdx = clean.indexOf(',');
      final payload = commaIdx != -1 ? clean.substring(commaIdx + 1) : clean;
      final normalized = payload
          .replaceAll('\n', '')
          .replaceAll('\r', '')
          .trim();
      return base64Decode(normalized);
    } catch (_) {
      return null;
    }
  }

  void _handleImageError() {
    if (_candidateIndex + 1 < _candidateUrls.length) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() {
            _candidateIndex++;
          });
        }
      });
    } else {
      if (!_allFailed) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            setState(() {
              _allFailed = true;
            });
          }
        });
      }
    }
  }

  void _retryLoading() {
    setState(() {
      _candidateIndex = 0;
      _allFailed = false;
    });
  }

  void _openZoomDialog(BuildContext context, String currentEffectiveUrl) {
    if (currentEffectiveUrl.trim().isEmpty) return;

    showDialog(
      context: context,
      barrierColor: Colors.black87,
      builder: (ctx) {
        return Dialog(
          backgroundColor: Colors.transparent,
          insetPadding: const EdgeInsets.all(12),
          child: Stack(
            alignment: Alignment.center,
            children: [
              InteractiveViewer(
                minScale: 0.8,
                maxScale: 4.0,
                child: Center(
                  child: ReportImageView(
                    imageUrl: currentEffectiveUrl,
                    fit: BoxFit.contain,
                    enableZoom: false,
                  ),
                ),
              ),
              Positioned(
                top: 10,
                right: 10,
                child: CircleAvatar(
                  backgroundColor: Colors.black54,
                  radius: 20,
                  child: IconButton(
                    icon: const Icon(
                      Icons.close,
                      color: Colors.white,
                      size: 20,
                    ),
                    onPressed: () => Navigator.pop(ctx),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final cleanUrl = widget.imageUrl.trim();
    if (cleanUrl.isEmpty) {
      return widget.errorWidget ?? _buildFallback('No image available');
    }

    Widget content;

    if (_isBase64(cleanUrl)) {
      final bytes = _decodeBase64(cleanUrl);
      if (bytes != null && bytes.isNotEmpty) {
        content = Image.memory(
          bytes,
          height: widget.height,
          width: widget.width,
          fit: widget.fit,
          errorBuilder: (_, _, _) =>
              widget.errorWidget ?? _buildFallback('Invalid photo format'),
        );
      } else {
        content =
            widget.errorWidget ?? _buildFallback('Unable to decode image');
      }
    } else {
      if (_allFailed || _candidateUrls.isEmpty) {
        content =
            widget.errorWidget ??
            _buildFallback('Image could not be loaded', showRetry: true);
      } else {
        final currentUrl = _candidateUrls[_candidateIndex];
        content = Image.network(
          currentUrl,
          key: ValueKey(currentUrl),
          height: widget.height,
          width: widget.width,
          fit: widget.fit,
          loadingBuilder: (ctx, child, progress) {
            if (progress == null) return child;
            return widget.placeholder ??
                Container(
                  height: widget.height,
                  width: widget.width,
                  color: Colors.grey.shade100,
                  child: Center(
                    child: SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        value: progress.expectedTotalBytes != null
                            ? progress.cumulativeBytesLoaded /
                                  (progress.expectedTotalBytes ?? 1)
                            : null,
                        strokeWidth: 2,
                      ),
                    ),
                  ),
                );
          },
          errorBuilder: (ctx, err, stack) {
            _handleImageError();
            return widget.placeholder ??
                Container(
                  height: widget.height,
                  width: widget.width,
                  color: Colors.grey.shade100,
                  child: const Center(
                    child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                  ),
                );
          },
        );
      }
    }

    if (widget.borderRadius != null) {
      content = ClipRRect(borderRadius: widget.borderRadius!, child: content);
    }

    if (widget.enableZoom && !_allFailed && cleanUrl.isNotEmpty) {
      final effectiveZoomUrl = _candidateUrls.isNotEmpty
          ? _candidateUrls[_candidateIndex]
          : cleanUrl;
      return MouseRegion(
        cursor: SystemMouseCursors.click,
        child: GestureDetector(
          onTap: () => _openZoomDialog(context, effectiveZoomUrl),
          child: Tooltip(message: 'Tap to enlarge photo', child: content),
        ),
      );
    }

    return content;
  }

  Widget _buildFallback(String message, {bool showRetry = false}) {
    return Container(
      height: widget.height ?? 120,
      width: widget.width ?? double.infinity,
      decoration: BoxDecoration(
        color: Colors.grey.shade200,
        borderRadius: widget.borderRadius ?? BorderRadius.circular(8),
      ),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.broken_image_outlined,
            color: Colors.grey.shade500,
            size: 28,
          ),
          const SizedBox(height: 6),
          Text(
            message,
            style: TextStyle(fontSize: 11, color: Colors.grey.shade600),
            textAlign: TextAlign.center,
          ),
          if (showRetry) ...[
            const SizedBox(height: 6),
            InkWell(
              onTap: _retryLoading,
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.refresh, size: 14, color: Colors.blue.shade700),
                    const SizedBox(width: 4),
                    Text(
                      'Retry',
                      style: TextStyle(
                        fontSize: 11,
                        color: Colors.blue.shade700,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
