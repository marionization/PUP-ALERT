import 'dart:async';

import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../services/auth_service.dart';
import '../dashboard/dashboard_screen.dart';

class OtpVerificationScreen extends StatefulWidget {
  final String targetDestination; // Phone or Email
  final String userRole;
  final String userName;
  final String firstName;
  final String studentId;
  final String uid;

  const OtpVerificationScreen({
    super.key,
    required this.targetDestination,
    required this.userRole,
    required this.userName,
    required this.firstName,
    required this.studentId,
    required this.uid,
  });

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  final _otpController = TextEditingController();
  final _authService = AuthService();

  int _countdownSeconds = 60;
  Timer? _timer;
  bool _isVerifying = false;
  final String _generatedDemoCode = '123456';

  @override
  void initState() {
    super.initState();
    _startCountdownTimer();
  }

  void _startCountdownTimer() {
    _timer?.cancel();
    setState(() => _countdownSeconds = 60);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_countdownSeconds > 0) {
        setState(() => _countdownSeconds--);
      } else {
        timer.cancel();
      }
    });
  }

  @override
  void dispose() {
    _timer?.cancel();
    _otpController.dispose();
    super.dispose();
  }

  Future<void> _handleVerify() async {
    final code = _otpController.text.trim();

    if (code.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please enter a valid 6-digit OTP'),
          backgroundColor: Color(0xFFD32F2F),
        ),
      );
      return;
    }

    setState(() => _isVerifying = true);

    // Verify OTP code (accepts generated code or standard test PIN)
    await Future.delayed(const Duration(milliseconds: 600));

    if (code == _generatedDemoCode || code == '123456' || code == '654321') {
      // Save OTP verified state matching OtpVerificationActivity.kt saveOtpVerifiedState
      if (widget.uid.isNotEmpty) {
        await _authService.saveOtpVerifiedState(widget.uid);
      }

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('is_logged_in', true);
      await prefs.setString('role', widget.userRole);
      await prefs.setString('user_name', widget.userName);
      await prefs.setString('student_first_name', widget.firstName);
      await prefs.setString('student_id', widget.studentId);
      await prefs.setString('user_uid', widget.uid);

      setState(() => _isVerifying = false);

      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Login Successful'),
          backgroundColor: Colors.green,
        ),
      );

      // Open NextActivityContent (DashboardScreen)
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
          builder: (context) => DashboardScreen(
            userRole: widget.userRole,
            userName: widget.userName,
            firstName: widget.firstName,
          ),
        ),
        (route) => false,
      );
    } else {
      setState(() => _isVerifying = false);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Invalid OTP. Please check your verification code.'),
          backgroundColor: Color(0xFFD32F2F),
        ),
      );
    }
  }

  void _handleResend() {
    if (_countdownSeconds > 0) return;
    _startCountdownTimer();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('New code sent! (Use code 123456 for testing)'),
        backgroundColor: Color(0xFFD32F2F),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F9),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Card(
              elevation: 4,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 24,
                  vertical: 32,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.security,
                      size: 56,
                      color: Color(0xFFD32F2F),
                    ),
                    const SizedBox(height: 12),

                    // Title matching activity_otp_verification.xml
                    const Text(
                      'Enter Verification Code',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFFD32F2F),
                      ),
                    ),
                    const SizedBox(height: 8),

                    // Subtitle message
                    Text(
                      widget.targetDestination.isNotEmpty
                          ? 'A 6-digit code was sent to ${widget.targetDestination}.'
                          : 'A 6-digit code was sent to your phone number.',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFF616161),
                      ),
                    ),
                    const SizedBox(height: 24),

                    // OTP Input Field
                    TextField(
                      controller: _otpController,
                      keyboardType: TextInputType.number,
                      textAlign: TextAlign.center,
                      maxLength: 6,
                      style: const TextStyle(
                        fontSize: 24,
                        letterSpacing: 8,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF212121),
                      ),
                      decoration: InputDecoration(
                        hintText: 'Enter OTP',
                        hintStyle: const TextStyle(
                          fontSize: 16,
                          letterSpacing: 1,
                          fontWeight: FontWeight.normal,
                          color: Colors.grey,
                        ),
                        counterText: '',
                        filled: true,
                        fillColor: const Color(0xFFF9F9F9),
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 16,
                          vertical: 14,
                        ),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: Color(0xFFDADDE2),
                          ),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: Color(0xFFD32F2F),
                            width: 2,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 20),

                    // Verify Button matching activity_otp_verification.xml
                    SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: ElevatedButton(
                        onPressed: _isVerifying ? null : _handleVerify,
                        style: ElevatedButton.styleFrom(
                          backgroundColor: const Color(0xFFD32F2F),
                          foregroundColor: Colors.white,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          elevation: 2,
                        ),
                        child: _isVerifying
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Text(
                                'Verify',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Resend Code with 60s countdown timer
                    InkWell(
                      onTap: _countdownSeconds == 0 ? _handleResend : null,
                      child: Padding(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        child: Text(
                          _countdownSeconds > 0
                              ? 'Resend code in ${_countdownSeconds}s'
                              : 'Resend Code',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                            color: _countdownSeconds > 0
                                ? Colors.grey
                                : const Color(0xFFD32F2F),
                          ),
                        ),
                      ),
                    ),

                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 10,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.amber.shade50,
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: Colors.amber.shade200),
                      ),
                      child: const Text(
                        'Demo/Testing code: 123456',
                        style: TextStyle(fontSize: 11, color: Colors.amber),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}
