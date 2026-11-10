import 'dart:math';

import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../services/auth_service.dart';
import '../dashboard/dashboard_screen.dart';
import 'otp_verification_screen.dart';
import 'register_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  static const List<String> _backgroundImages = [
    'assets/images/login_bg_1.jpg',
    'assets/images/login_bg_2.jpg',
  ];

  final _authService = AuthService();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();

  String _selectedRole = 'Student'; // "Student" or "Administrator"
  bool _isLoading = false;
  bool _obscurePassword = true;
  late String _selectedBgImage;

  @override
  void initState() {
    super.initState();
    _selectedBgImage =
        _backgroundImages[Random().nextInt(_backgroundImages.length)];
    _shuffleBackgroundOnLoad();
  }

  Future<void> _shuffleBackgroundOnLoad() async {
    final prefs = await SharedPreferences.getInstance();
    final lastIndex = prefs.getInt('last_login_bg_index');
    int nextIndex;
    if (lastIndex != null && _backgroundImages.length > 1) {
      // Pick a different image from the previous login/refresh
      nextIndex = (lastIndex + 1 + Random().nextInt(_backgroundImages.length - 1)) %
          _backgroundImages.length;
    } else {
      nextIndex = Random().nextInt(_backgroundImages.length);
    }
    await prefs.setInt('last_login_bg_index', nextIndex);
    if (mounted) {
      setState(() {
        _selectedBgImage = _backgroundImages[nextIndex];
      });
    }
  }

  Future<void> _handleLogin() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text.trim();

    if (email.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            _selectedRole == 'Student'
                ? 'Email is required'
                : 'Admin username is required',
          ),
          backgroundColor: Colors.red.shade700,
        ),
      );
      return;
    }

    if (password.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text('Password is required'),
          backgroundColor: Colors.red.shade700,
        ),
      );
      return;
    }

    setState(() => _isLoading = true);

    final result = await _authService.login(
      email: email,
      password: password,
      selectedRole: _selectedRole == 'Student' ? 'student' : 'admin',
    );

    setState(() => _isLoading = false);

    if (result['success'] == true) {
      if (!mounted) return;

      final role =
          result['role'] ?? (_selectedRole == 'Student' ? 'student' : 'admin');
      final userName =
          result['name'] ??
          (_selectedRole == 'Student' ? 'Student' : 'Administrator');
      final firstName =
          result['firstName'] ??
          (_selectedRole == 'Student' ? 'Student' : 'Administrator');
      final studentId = result['studentId'] ?? '';
      final uid = result['uid'] ?? '';
      final phone = result['phone'] ?? '';

      // Check if 2FA OTP is required for Student
      if (result['requiresOtp'] == true) {
        final resolver = result['resolver'] as MultiFactorResolver?;
        final phoneHint = result['phoneHint'] as PhoneMultiFactorInfo?;

        Navigator.pushReplacement(
          context,
          MaterialPageRoute(
            builder: (context) => OtpVerificationScreen(
              targetDestination: phone.isNotEmpty ? phone : email,
              userRole: role,
              userName: userName,
              firstName: firstName,
              studentId: studentId,
              uid: uid,
              phoneNumber: phone,
              mfaResolver: resolver,
              mfaPhoneInfo: phoneHint,
            ),
          ),
        );
        return;
      }

      // Proceed directly to Dashboard / NextActivityContent
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (context) => DashboardScreen(
            userRole: role,
            userName: userName,
            firstName: firstName,
            studentId: studentId,
          ),
        ),
      );
    } else {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(result['message'] ?? 'Login failed, please try again.'),
          backgroundColor: Colors.red.shade700,
        ),
      );
    }
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final isStudent = _selectedRole == 'Student';

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F7),
      body: Stack(
        fit: StackFit.expand,
        children: [
          // Shuffled PUP Campus Background Image
          Image.asset(
            _selectedBgImage,
            fit: BoxFit.cover,
            alignment: Alignment.center,
            errorBuilder: (context, error, stackTrace) =>
                Container(color: const Color(0xFFF5F5F7)),
          ),
          // Dark tint overlay for contrast and readability
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.black.withValues(alpha: 0.45),
                  Colors.black.withValues(alpha: 0.60),
                ],
              ),
            ),
          ),
          Center(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 460),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    // School Logo
                    Image.asset(
                      'assets/images/ic_school_logo.png',
                      height: 88,
                      width: 88,
                      errorBuilder: (context, error, stackTrace) => Container(
                        width: 88,
                        height: 88,
                        decoration: const BoxDecoration(
                          shape: BoxShape.circle,
                          color: Color(0xFFC62828),
                        ),
                        child: const Icon(
                          Icons.school,
                          size: 52,
                          color: Colors.white,
                        ),
                      ),
                    ),
                    const SizedBox(height: 8),

                    // Campus Header
                    const Text(
                      'PUP Parañaque Campus',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                        color: Colors.white,
                        shadows: [
                          Shadow(
                            color: Colors.black54,
                            blurRadius: 8,
                            offset: Offset(0, 2),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 2),
                    const Text(
                      'Report and Monitoring System',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.white,
                        fontWeight: FontWeight.w500,
                        shadows: [
                          Shadow(
                            color: Colors.black54,
                            blurRadius: 6,
                            offset: Offset(0, 1),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Login Card
                    Card(
                      elevation: 10,
                      color: Colors.white.withValues(alpha: 0.96),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(20),
                      ),
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Welcome Back',
                          style: TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF212121),
                          ),
                        ),
                        const SizedBox(height: 4),
                        const Text(
                          'Sign in to submit and monitor campus reports',
                          style: TextStyle(
                            fontSize: 13,
                            color: Color(0xFF757575),
                          ),
                        ),
                        const SizedBox(height: 18),

                        // Role Radio Selector matching activity_main.xml
                        Row(
                          children: [
                            Expanded(
                              child: InkWell(
                                onTap: () =>
                                    setState(() => _selectedRole = 'Student'),
                                borderRadius: BorderRadius.circular(8),
                                child: Row(
                                  children: [
                                    Radio<String>(
                                      value: 'Student',
                                      groupValue: _selectedRole,
                                      activeColor: const Color(0xFFC62828),
                                      onChanged: (val) {
                                        if (val != null)
                                          setState(() => _selectedRole = val);
                                      },
                                    ),
                                    const Text(
                                      'Student',
                                      style: TextStyle(
                                        fontSize: 15,
                                        fontWeight: FontWeight.w600,
                                        color: Color(0xFF212121),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                            Expanded(
                              child: InkWell(
                                onTap: () => setState(
                                  () => _selectedRole = 'Administrator',
                                ),
                                borderRadius: BorderRadius.circular(8),
                                child: Row(
                                  children: [
                                    Radio<String>(
                                      value: 'Administrator',
                                      groupValue: _selectedRole,
                                      activeColor: const Color(0xFFC62828),
                                      onChanged: (val) {
                                        if (val != null)
                                          setState(() => _selectedRole = val);
                                      },
                                    ),
                                    const Text(
                                      'Admin',
                                      style: TextStyle(
                                        fontSize: 15,
                                        fontWeight: FontWeight.w600,
                                        color: Color(0xFF212121),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Email / Username Label
                        Text(
                          isStudent ? 'Email Address' : 'Admin Username',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF616161),
                          ),
                        ),
                        const SizedBox(height: 6),
                        TextField(
                          controller: _emailController,
                          keyboardType: isStudent
                              ? TextInputType.emailAddress
                              : TextInputType.text,
                          decoration: InputDecoration(
                            hintText: isStudent
                                ? 'Enter your email'
                                : 'Enter admin username (admin)',
                            helperText: isStudent
                                ? 'Use your PUP email address'
                                : null,
                            filled: true,
                            fillColor: const Color(0xFFFAFAFA),
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
                                color: Color(0xFFC62828),
                                width: 2,
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(height: 14),

                        // Password Label
                        const Text(
                          'Password',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                            color: Color(0xFF616161),
                          ),
                        ),
                        const SizedBox(height: 6),
                        TextField(
                          controller: _passwordController,
                          obscureText: _obscurePassword,
                          decoration: InputDecoration(
                            hintText: isStudent
                                ? 'Enter your password'
                                : 'Enter password (admin01)',
                            filled: true,
                            fillColor: const Color(0xFFFAFAFA),
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
                                color: Color(0xFFC62828),
                                width: 2,
                              ),
                            ),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscurePassword
                                    ? Icons.visibility_off_outlined
                                    : Icons.visibility_outlined,
                                color: Colors.grey,
                              ),
                              onPressed: () {
                                setState(
                                  () => _obscurePassword = !_obscurePassword,
                                );
                              },
                            ),
                          ),
                        ),
                        const SizedBox(height: 20),

                        // Sign In Button
                        SizedBox(
                          width: double.infinity,
                          height: 48,
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _handleLogin,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: const Color(0xFFC62828),
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              elevation: 2,
                            ),
                            child: _isLoading
                                ? const SizedBox(
                                    height: 20,
                                    width: 20,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Colors.white,
                                    ),
                                  )
                                : Text(
                                    isStudent
                                        ? 'Sign In as Student'
                                        : 'Sign In as Admin',
                                    style: const TextStyle(
                                      fontSize: 15,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                          ),
                        ),

                        // Register Link (for student)
                        if (isStudent) ...[
                          const SizedBox(height: 18),
                          Center(
                            child: Wrap(
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: [
                                const Text(
                                  "Don't have an account? ",
                                  style: TextStyle(
                                    fontSize: 13,
                                    color: Colors.grey,
                                  ),
                                ),
                                GestureDetector(
                                  onTap: () {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (context) =>
                                            const RegisterScreen(),
                                      ),
                                    );
                                  },
                                  child: const Text(
                                    'Register here',
                                    style: TextStyle(
                                      fontSize: 13,
                                      color: Color(0xFFC62828),
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
