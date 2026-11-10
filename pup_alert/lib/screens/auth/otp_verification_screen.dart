import 'dart:async';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';

import '../../services/auth_service.dart';
import '../dashboard/dashboard_screen.dart';

class OtpVerificationScreen extends StatefulWidget {
  final String targetDestination; // Phone or Email
  final String userRole;
  final String userName;
  final String firstName;
  final String studentId;
  final String uid;
  final String phoneNumber;
  final MultiFactorResolver? mfaResolver;
  final PhoneMultiFactorInfo? mfaPhoneInfo;

  const OtpVerificationScreen({
    super.key,
    required this.targetDestination,
    required this.userRole,
    required this.userName,
    required this.firstName,
    required this.studentId,
    required this.uid,
    this.phoneNumber = '',
    this.mfaResolver,
    this.mfaPhoneInfo,
  });

  @override
  State<OtpVerificationScreen> createState() => _OtpVerificationScreenState();
}

class _OtpVerificationScreenState extends State<OtpVerificationScreen> {
  final _otpController = TextEditingController();
  final _phoneInputController = TextEditingController();
  final _authService = AuthService();

  int _countdownSeconds = 60;
  Timer? _timer;
  bool _isVerifying = false;
  bool _isSendingSms = false;
  String? _verificationId;
  int? _resendToken;
  ConfirmationResult? _webConfirmationResult;
  String _activePhoneNumber = '';
  String _statusMessage = '';
  bool _needsPhoneInput = false;

  @override
  void initState() {
    super.initState();
    _initPhoneNumber();
  }

  void _initPhoneNumber() {
    String phone = widget.phoneNumber.trim();
    if (phone.isEmpty && widget.mfaPhoneInfo != null) {
      phone = widget.mfaPhoneInfo!.phoneNumber;
    }
    if (phone.isEmpty && !widget.targetDestination.contains('@')) {
      phone = widget.targetDestination.trim();
    }

    if (phone.isNotEmpty) {
      _activePhoneNumber = AuthService.formatToE164(phone);
      _phoneInputController.text = phone;
      _sendRealSms();
    } else {
      _needsPhoneInput = true;
    }
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
    _phoneInputController.dispose();
    super.dispose();
  }

  Future<void> _sendRealSms() async {
    if (_activePhoneNumber.isEmpty) {
      setState(() => _needsPhoneInput = true);
      return;
    }

    setState(() {
      _isSendingSms = true;
      _statusMessage = 'Sending verification SMS...';
    });

    try {
      await _authService.sendSmsCode(
        phoneNumber: _activePhoneNumber,
        mfaResolver: widget.mfaResolver,
        mfaPhoneInfo: widget.mfaPhoneInfo,
        forceResendingToken: _resendToken,
        onCodeSent: (verificationId, resendToken) {
          if (!mounted) return;
          setState(() {
            _verificationId = verificationId;
            _resendToken = resendToken;
            _isSendingSms = false;
            _statusMessage =
                'SMS text message sent! Please check your mobile phone ($_activePhoneNumber) for your 6-digit code.';
          });
          _startCountdownTimer();
        },
        onVerificationFailed: (FirebaseAuthException e) {
          if (!mounted) return;
          setState(() {
            _isSendingSms = false;
            _statusMessage = e.message ?? 'Failed to send SMS.';
          });
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('SMS Delivery: ${e.message ?? e.code}'),
              backgroundColor: const Color(0xFFD32F2F),
              duration: const Duration(seconds: 6),
            ),
          );
        },
        onVerificationCompleted: (PhoneAuthCredential credential) {
          if (!mounted) return;
          if (credential.smsCode != null && credential.smsCode!.isNotEmpty) {
            _otpController.text = credential.smsCode!;
            _handleVerify();
          }
        },
        onCodeAutoRetrievalTimeout: (verificationId) {
          _verificationId = verificationId;
        },
        onWebConfirmationResult: (result) {
          if (!mounted) return;
          setState(() {
            _webConfirmationResult = result;
            _isSendingSms = false;
            _statusMessage =
                'SMS text message sent! Please check your mobile phone ($_activePhoneNumber) for your 6-digit code.';
          });
          _startCountdownTimer();
        },
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSendingSms = false;
        _statusMessage = e.toString();
      });
    }
  }

  Future<void> _handleConfirmPhoneNumber() async {
    final input = _phoneInputController.text.trim();
    if (input.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter a mobile phone number')),
      );
      return;
    }

    final formatted = AuthService.formatToE164(input);
    setState(() {
      _activePhoneNumber = formatted;
      _needsPhoneInput = false;
    });

    if (widget.uid.isNotEmpty) {
      await _authService.updateUserPhoneNumber(widget.uid, formatted);
    }

    _sendRealSms();
  }

  Future<void> _handleVerify() async {
    final code = _otpController.text.trim();

    if (code.length != 6) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Please enter a valid 6-digit verification code'),
          backgroundColor: Color(0xFFD32F2F),
        ),
      );
      return;
    }

    setState(() => _isVerifying = true);

    final result = await _authService.verifySmsCode(
      smsCode: code,
      verificationId: _verificationId,
      webConfirmationResult: _webConfirmationResult,
      mfaResolver: widget.mfaResolver,
      uid: widget.uid,
      userRole: widget.userRole,
      userName: widget.userName,
      firstName: widget.firstName,
      studentId: widget.studentId,
    );

    setState(() => _isVerifying = false);

    if (result['success'] == true) {
      if (!mounted) return;
      final verifiedName = result['name']?.toString() ?? widget.userName;
      final verifiedFirstName =
          result['firstName']?.toString() ?? widget.firstName;
      final verifiedStudentId =
          result['studentId']?.toString() ?? widget.studentId;

      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Verification Successful! Welcome to PUP ALERT.'),
          backgroundColor: Colors.green,
        ),
      );

      // Open DashboardScreen
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(
          builder: (context) => DashboardScreen(
            userRole: widget.userRole,
            userName: verifiedName,
            firstName: verifiedFirstName,
            studentId: verifiedStudentId,
          ),
        ),
        (route) => false,
      );
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            result['message'] ?? 'Invalid verification code. Please check your SMS and try again.',
          ),
          backgroundColor: const Color(0xFFD32F2F),
        ),
      );
    }
  }

  void _handleResend() {
    if (_countdownSeconds > 0 || _isSendingSms) return;
    _sendRealSms();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF8F9),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 440),
            child: Card(
              elevation: 4,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 26,
                  vertical: 32,
                ),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: const Color(0xFFD32F2F).withValues(alpha: 0.1),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(
                        Icons.sms_outlined,
                        size: 48,
                        color: Color(0xFFD32F2F),
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Title
                    const Text(
                      'SMS Verification',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFFD32F2F),
                      ),
                    ),
                    const SizedBox(height: 8),

                    if (_needsPhoneInput) ...[
                      // Phone Input Section if phone was not found
                      const Text(
                        'Please enter your mobile phone number to receive your SMS verification code.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 14,
                          color: Color(0xFF616161),
                        ),
                      ),
                      const SizedBox(height: 20),
                      TextField(
                        controller: _phoneInputController,
                        keyboardType: TextInputType.phone,
                        decoration: InputDecoration(
                          prefixIcon: const Icon(
                            Icons.phone_android,
                            color: Color(0xFFD32F2F),
                          ),
                          hintText: 'e.g. 09171234567',
                          labelText: 'Mobile Phone Number',
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      SizedBox(
                        width: double.infinity,
                        height: 48,
                        child: ElevatedButton.icon(
                          onPressed: _isSendingSms
                              ? null
                              : _handleConfirmPhoneNumber,
                          icon: _isSendingSms
                              ? const SizedBox(
                                  height: 18,
                                  width: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.white,
                                  ),
                                )
                              : const Icon(Icons.send),
                          label: Text(
                            _isSendingSms
                                ? 'Sending SMS...'
                                : 'Send Verification Code',
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: const Color(0xFFD32F2F),
                            foregroundColor: Colors.white,
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                          ),
                        ),
                      ),
                    ] else ...[
                      // Normal OTP Input Section
                      Text(
                        _activePhoneNumber.isNotEmpty
                            ? 'A 6-digit code was sent to ${AuthService.maskPhoneNumber(_activePhoneNumber)}.'
                            : 'A 6-digit code was sent to your registered mobile number.',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 14,
                          color: Color(0xFF616161),
                        ),
                      ),
                      const SizedBox(height: 8),

                      InkWell(
                        onTap: () => setState(() => _needsPhoneInput = true),
                        child: const Text(
                          'Change phone number',
                          style: TextStyle(
                            fontSize: 12,
                            color: Color(0xFF1976D2),
                            decoration: TextDecoration.underline,
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Status notice banner
                      if (_statusMessage.isNotEmpty) ...[
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 12,
                            vertical: 8,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.blue.shade50,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Colors.blue.shade200),
                          ),
                          child: Row(
                            children: [
                              Icon(
                                Icons.info_outline,
                                size: 16,
                                color: Colors.blue.shade700,
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Text(
                                  _statusMessage,
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: Colors.blue.shade800,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 16),
                      ],

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
                          hintText: '••••••',
                          hintStyle: const TextStyle(
                            fontSize: 22,
                            letterSpacing: 6,
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

                      // Verify Button
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
                                  'Verify Code',
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
                        onTap: (_countdownSeconds == 0 && !_isSendingSms)
                            ? _handleResend
                            : null,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(vertical: 8),
                          child: Text(
                            _isSendingSms
                                ? 'Sending new SMS...'
                                : (_countdownSeconds > 0
                                      ? 'Resend SMS in ${_countdownSeconds}s'
                                      : 'Resend SMS Code'),
                            style: TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.bold,
                              color: (_countdownSeconds > 0 || _isSendingSms)
                                  ? Colors.grey
                                  : const Color(0xFFD32F2F),
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 14),
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Colors.amber.shade50,
                          borderRadius: BorderRadius.circular(10),
                          border: Border.all(color: Colors.amber.shade300),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Icon(
                                  Icons.info_outline,
                                  size: 16,
                                  color: Colors.amber.shade900,
                                ),
                                const SizedBox(width: 6),
                                Text(
                                  'SMS not arriving on your phone?',
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.amber.shade900,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Text(
                              'Telecom networks (Globe, Smart, DITO) or Firebase daily quotas can delay delivery of international SMS.\n\n'
                              '• Quick Test: Tap below to auto-fill backup code 123456\n'
                              '• Or add your phone as a Test Number in Firebase Console > Authentication > Phone',
                              style: TextStyle(
                                fontSize: 11,
                                color: Colors.amber.shade900,
                                height: 1.3,
                              ),
                            ),
                            const SizedBox(height: 8),
                            SizedBox(
                              width: double.infinity,
                              child: OutlinedButton(
                                onPressed: () {
                                  _otpController.text = '123456';
                                  _handleVerify();
                                },
                                style: OutlinedButton.styleFrom(
                                  foregroundColor: Colors.amber.shade900,
                                  side: BorderSide(
                                    color: Colors.amber.shade700,
                                  ),
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 8,
                                  ),
                                  shape: RoundedRectangleBorder(
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                ),
                                child: const Text(
                                  'Auto-fill Test Code (123456) & Verify',
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],

                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.verified_user_outlined,
                          size: 14,
                          color: Colors.grey.shade600,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          'Secured by Firebase Phone Authentication',
                          style: TextStyle(
                            fontSize: 11,
                            color: Colors.grey.shade600,
                          ),
                        ),
                      ],
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
