import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:shared_preferences/shared_preferences.dart';

class AuthService {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  User? get currentUser => _auth.currentUser;
  Stream<User?> get authStateChanges => _auth.authStateChanges();

  // Check if 2FA OTP has already been verified on this device for the current user
  Future<bool> isOtpVerified(String uid) async {
    final prefs = await SharedPreferences.getInstance();
    final otpVerified = prefs.getBool('otp_verified') ?? false;
    final verifiedUid = prefs.getString('otp_verified_uid');
    return otpVerified && verifiedUid == uid;
  }

  // Save 2FA OTP verified state for this device
  Future<void> saveOtpVerifiedState(String uid) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('otp_verified', true);
    await prefs.setString('otp_verified_uid', uid);
  }

  // Clear 2FA OTP verification to force re-verification
  Future<void> clearOtpVerifiedState() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('otp_verified');
    await prefs.remove('otp_verified_uid');
  }

  Future<Map<String, dynamic>> login({
    required String email,
    required String password,
    required String selectedRole, // 'student' or 'admin'
  }) async {
    try {
      final cleanEmail = email.trim();
      final cleanPassword = password.trim();

      // Fixed Admin credentials matching MainActivity.kt
      if (selectedRole == 'admin') {
        if (cleanEmail == 'admin' &&
            (cleanPassword == 'admin01' || cleanPassword == 'admin')) {
          final prefs = await SharedPreferences.getInstance();
          await prefs.setBool('is_logged_in', true);
          await prefs.setString('role', 'admin');
          await prefs.setString('user_name', 'Administrator');
          await prefs.setString('student_first_name', 'Administrator');
          await prefs.setString('student_id', '');

          return {
            'success': true,
            'role': 'admin',
            'name': 'Administrator',
            'firstName': 'Administrator',
            'uid': 'admin_local_uid',
            'requiresOtp': false,
          };
        } else {
          return {'success': false, 'message': 'Invalid admin credentials'};
        }
      }

      // Student Login via Firebase
      String emailToUse = cleanEmail;
      if (!emailToUse.contains('@')) {
        emailToUse = '$cleanEmail@pup.edu.ph';
      }

      UserCredential credential = await _auth.signInWithEmailAndPassword(
        email: emailToUse,
        password: cleanPassword,
      );

      final user = credential.user;
      if (user != null) {
        DocumentSnapshot userDoc = await _firestore
            .collection('users')
            .doc(user.uid)
            .get();

        String fullName = 'Student';
        String firstName = 'Student';
        String studentId = '';
        String phone = '';

        if (userDoc.exists) {
          final data = userDoc.data() as Map<String, dynamic>;
          final fName = data['firstName']?.toString() ?? '';
          final lName = data['lastName']?.toString() ?? '';
          if (fName.isNotEmpty || lName.isNotEmpty) {
            fullName = '$fName $lName'.trim();
            firstName = fName.isNotEmpty ? fName : fullName.split(' ').first;
          } else {
            fullName =
                data['name']?.toString() ?? user.displayName ?? 'Student';
            firstName = fullName.split(' ').first;
          }
          studentId = data['studentId']?.toString() ?? '';
          phone =
              data['contactNumber']?.toString() ??
              data['phone']?.toString() ??
              '';
        } else {
          fullName = user.displayName ?? 'Student';
          firstName = fullName.split(' ').first;
        }

        final prefs = await SharedPreferences.getInstance();
        final is2faEnabled = prefs.getBool('security_2fa_enabled') ?? true;
        final alreadyOtpVerified = await isOtpVerified(user.uid);

        // Check if 2FA OTP is required
        final requiresOtp = is2faEnabled && !alreadyOtpVerified;

        if (!requiresOtp) {
          // Store session directly
          await prefs.setBool('is_logged_in', true);
          await prefs.setString('role', 'student');
          await prefs.setString('user_name', fullName);
          await prefs.setString('student_first_name', firstName);
          await prefs.setString('student_id', studentId);
          await prefs.setString('user_uid', user.uid);
        }

        return {
          'success': true,
          'role': 'student',
          'name': fullName,
          'firstName': firstName,
          'studentId': studentId,
          'phone': phone,
          'uid': user.uid,
          'requiresOtp': requiresOtp,
        };
      }
      return {'success': false, 'message': 'Student record not found.'};
    } on FirebaseAuthException catch (e) {
      return {
        'success': false,
        'message': e.message ?? 'Login failed. Please check credentials.',
      };
    } catch (e) {
      return {'success': false, 'message': e.toString()};
    }
  }

  Future<Map<String, dynamic>> register({
    required String name,
    required String studentId,
    required String email,
    required String password,
    required String department,
    required String contactNumber,
  }) async {
    try {
      String cleanEmail = email.trim();
      if (!cleanEmail.contains('@')) {
        cleanEmail = '$cleanEmail@iskolarngbayan.pup.edu.ph';
      }

      UserCredential credential = await _auth.createUserWithEmailAndPassword(
        email: cleanEmail,
        password: password,
      );

      final user = credential.user;
      if (user != null) {
        await user.updateDisplayName(name);

        final parts = name.trim().split(' ');
        final firstName = parts.isNotEmpty ? parts.first : name;
        final lastName = parts.length > 1 ? parts.sublist(1).join(' ') : '';

        await _firestore.collection('users').doc(user.uid).set({
          'name': name,
          'firstName': firstName,
          'lastName': lastName,
          'studentId': studentId,
          'email': cleanEmail,
          'department': department,
          'contactNumber': contactNumber,
          'phone': contactNumber,
          'role': 'student',
          'createdAt': FieldValue.serverTimestamp(),
        });

        // Set initial session
        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('is_logged_in', true);
        await prefs.setBool('otp_verified', false);
        await prefs.setString('role', 'student');
        await prefs.setString('user_name', name);
        await prefs.setString('student_first_name', firstName);
        await prefs.setString('student_id', studentId);

        return {
          'success': true,
          'uid': user.uid,
          'firstName': firstName,
          'name': name,
        };
      }
      return {'success': false, 'message': 'Failed to create student account'};
    } on FirebaseAuthException catch (e) {
      return {'success': false, 'message': e.message ?? 'Registration failed'};
    } catch (e) {
      return {'success': false, 'message': e.toString()};
    }
  }

  Future<void> logout() async {
    await _auth.signOut();
    final prefs = await SharedPreferences.getInstance();
    // Keep accessibility settings and 2FA preference, but clear login state
    final textSize = prefs.getString('access_text_size');
    final boldText = prefs.getBool('access_bold_text');
    final contrastMode = prefs.getString('access_contrast_mode');
    final reduceMotion = prefs.getBool('access_reduce_motion');
    final grayscaleMode = prefs.getBool('access_grayscale_mode');
    final largeButtons = prefs.getBool('access_large_buttons');
    final simplifiedCards = prefs.getBool('access_simplified_cards');
    final hideMediaPreview = prefs.getBool('access_hide_media_preview');
    final twoFactorEnabled = prefs.getBool('security_2fa_enabled');

    await prefs.clear();

    if (textSize != null) await prefs.setString('access_text_size', textSize);
    if (boldText != null) await prefs.setBool('access_bold_text', boldText);
    if (contrastMode != null)
      await prefs.setString('access_contrast_mode', contrastMode);
    if (reduceMotion != null)
      await prefs.setBool('access_reduce_motion', reduceMotion);
    if (grayscaleMode != null)
      await prefs.setBool('access_grayscale_mode', grayscaleMode);
    if (largeButtons != null)
      await prefs.setBool('access_large_buttons', largeButtons);
    if (simplifiedCards != null)
      await prefs.setBool('access_simplified_cards', simplifiedCards);
    if (hideMediaPreview != null)
      await prefs.setBool('access_hide_media_preview', hideMediaPreview);
    if (twoFactorEnabled != null)
      await prefs.setBool('security_2fa_enabled', twoFactorEnabled);
  }

  Future<Map<String, String>> getCurrentSession() async {
    final prefs = await SharedPreferences.getInstance();
    return {
      'role': prefs.getString('role') ?? 'student',
      'name': prefs.getString('user_name') ?? 'Student',
      'firstName': prefs.getString('student_first_name') ?? 'Student',
      'studentId': prefs.getString('student_id') ?? '',
      'uid': prefs.getString('user_uid') ?? (_auth.currentUser?.uid ?? ''),
    };
  }
}
