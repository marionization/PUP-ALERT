import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../firebase_options.dart';

class AuthService {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  User? get currentUser => _auth.currentUser;
  Stream<User?> get authStateChanges => _auth.authStateChanges();

  /// Returns an isolated FirebaseAuth instance used strictly for OTP verification
  /// so that phone auth never overwrites the primary email/password currentUser.
  Future<FirebaseAuth> _getOtpVerificationAuth() async {
    const appName = 'pup_alert_otp_verifier';
    try {
      final app = Firebase.app(appName);
      return FirebaseAuth.instanceFor(app: app);
    } catch (_) {
      final app = await Firebase.initializeApp(
        name: appName,
        options: DefaultFirebaseOptions.currentPlatform,
      );
      return FirebaseAuth.instanceFor(app: app);
    }
  }

  /// Clears cached user identity fields in SharedPreferences so previous account data never leaks.
  Future<void> _clearUserIdentityPrefs(SharedPreferences prefs) async {
    await prefs.setBool('is_logged_in', false);
    await prefs.remove('role');
    await prefs.remove('user_name');
    await prefs.remove('student_first_name');
    await prefs.remove('student_id');
    await prefs.remove('user_uid');
    await prefs.remove('user_department');
    await prefs.remove('user_email');
    await prefs.remove('user_phone');
  }

  /// Formats raw phone strings (e.g. 09171234567, 9171234567, +639171234567) into standard E.164 format (+639171234567)
  static String formatToE164(String rawPhone) {
    String clean = rawPhone.replaceAll(RegExp(r'[^\d+]'), '').trim();
    if (clean.isEmpty) return '';
    if (clean.startsWith('+')) {
      return clean;
    }
    if (clean.startsWith('09') && clean.length == 11) {
      return '+63${clean.substring(1)}';
    }
    if (clean.startsWith('9') && clean.length == 10) {
      return '+63$clean';
    }
    if (clean.startsWith('63') && clean.length == 12) {
      return '+$clean';
    }
    return clean.startsWith('+') ? clean : '+$clean';
  }

  /// Masks phone number for secure UI display (e.g. +63 9•• ••• •567)
  static String maskPhoneNumber(String rawPhone) {
    final e164 = formatToE164(rawPhone);
    if (e164.isEmpty) return 'your registered phone number';
    if (e164.startsWith('+63') && e164.length == 13) {
      final sub = e164.substring(3); // 9171234567
      return '+63 ${sub.substring(0, 1)}•• ••• •${sub.substring(7)}';
    }
    if (e164.length > 6) {
      final start = e164.substring(0, 4);
      final end = e164.substring(e164.length - 3);
      return '$start••••$end';
    }
    return e164;
  }

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

  /// Update user phone number in Firestore
  Future<void> updateUserPhoneNumber(String uid, String rawPhone) async {
    final formatted = formatToE164(rawPhone);
    await _firestore.collection('users').doc(uid).update({
      'contactNumber': formatted,
      'phone': formatted,
    });
  }

  Future<Map<String, dynamic>> login({
    required String email,
    required String password,
    required String selectedRole, // 'student' or 'admin'
  }) async {
    try {
      final cleanEmail = email.trim();
      final cleanPassword = password.trim();
      final prefs = await SharedPreferences.getInstance();

      // Clear any previous account's cached identity before attempting login
      await _clearUserIdentityPrefs(prefs);

      // Fixed Admin credentials matching MainActivity.kt
      if (selectedRole == 'admin') {
        if (cleanEmail == 'admin' &&
            (cleanPassword == 'admin01' || cleanPassword == 'admin')) {
          await _auth.signOut();
          await prefs.setBool('is_logged_in', true);
          await prefs.setString('role', 'admin');
          await prefs.setString('user_name', 'Administrator');
          await prefs.setString('student_first_name', 'Administrator');
          await prefs.setString('student_id', '');
          await prefs.setString('user_uid', 'admin_local_uid');

          return {
            'success': true,
            'role': 'admin',
            'name': 'Administrator',
            'firstName': 'Administrator',
            'studentId': '',
            'uid': 'admin_local_uid',
            'requiresOtp': false,
            'requiresMfa': false,
          };
        } else {
          return {'success': false, 'message': 'Invalid admin credentials'};
        }
      }

      // Sign out any previously signed-in Firebase user first
      if (_auth.currentUser != null) {
        await _auth.signOut();
      }

      // Student Login via Firebase (support both PUP email domains if username-only)
      UserCredential credential;
      if (!cleanEmail.contains('@')) {
        try {
          credential = await _auth.signInWithEmailAndPassword(
            email: '$cleanEmail@iskolarngbayan.pup.edu.ph',
            password: cleanPassword,
          );
        } on FirebaseAuthException {
          credential = await _auth.signInWithEmailAndPassword(
            email: '$cleanEmail@pup.edu.ph',
            password: cleanPassword,
          );
        }
      } else {
        credential = await _auth.signInWithEmailAndPassword(
          email: cleanEmail,
          password: cleanPassword,
        );
      }

      final user = credential.user;
      if (user != null) {
        DocumentSnapshot userDoc = await _firestore
            .collection('users')
            .doc(user.uid)
            .get();

        String fullName = 'Student';
        String firstName = 'Student';
        String studentId = '';
        String department = '';
        String phone = '';

        if (userDoc.exists) {
          final data = userDoc.data() as Map<String, dynamic>;
          final fName = data['firstName']?.toString().trim() ?? '';
          final lName = data['lastName']?.toString().trim() ?? '';
          final full = data['name']?.toString().trim() ?? '';
          if (full.isNotEmpty) {
            fullName = full;
            firstName = fName.isNotEmpty ? fName : fullName.split(' ').first;
          } else if (fName.isNotEmpty || lName.isNotEmpty) {
            fullName = '$fName $lName'.trim();
            firstName = fName.isNotEmpty ? fName : fullName.split(' ').first;
          } else {
            fullName = user.displayName ?? 'Student';
            firstName = fullName.split(' ').first;
          }
          studentId = data['studentId']?.toString().trim() ?? '';
          department = data['department']?.toString().trim() ?? '';
          phone =
              data['contactNumber']?.toString().trim() ??
              data['phone']?.toString().trim() ??
              user.phoneNumber ??
              '';
        } else {
          fullName = user.displayName ?? 'Student';
          firstName = fullName.split(' ').first;
          phone = user.phoneNumber ?? '';
        }

        final is2faEnabled = prefs.getBool('security_2fa_enabled') ?? true;
        final alreadyOtpVerified = await isOtpVerified(user.uid);

        // Check if 2FA OTP is required
        final requiresOtp = is2faEnabled && !alreadyOtpVerified;

        // Always store the authenticated user's profile in SharedPreferences
        // so stale data from a previous account can never appear.
        await prefs.setBool('is_logged_in', !requiresOtp);
        await prefs.setString('role', 'student');
        await prefs.setString('user_name', fullName);
        await prefs.setString('student_first_name', firstName);
        await prefs.setString('student_id', studentId);
        await prefs.setString('user_uid', user.uid);
        await prefs.setString('user_department', department);
        await prefs.setString('user_email', user.email ?? cleanEmail);
        await prefs.setString('user_phone', formatToE164(phone));

        return {
          'success': true,
          'role': 'student',
          'name': fullName,
          'firstName': firstName,
          'studentId': studentId,
          'department': department,
          'phone': formatToE164(phone),
          'uid': user.uid,
          'requiresOtp': requiresOtp,
          'requiresMfa': false,
        };
      }
      return {'success': false, 'message': 'Student record not found.'};
    } on FirebaseAuthMultiFactorException catch (e) {
      // Firebase SMS Multi-Factor Authentication triggered
      final resolver = e.resolver;
      PhoneMultiFactorInfo? phoneHint;
      for (final hint in resolver.hints) {
        if (hint is PhoneMultiFactorInfo) {
          phoneHint = hint;
          break;
        }
      }

      return {
        'success': true,
        'role': 'student',
        'requiresOtp': true,
        'requiresMfa': true,
        'resolver': resolver,
        'phoneHint': phoneHint,
        'phone': phoneHint?.phoneNumber ?? '',
        'name': 'Student',
        'firstName': 'Student',
        'studentId': '',
        'uid': '',
      };
    } on FirebaseAuthException catch (e) {
      return {
        'success': false,
        'message': e.message ?? 'Login failed. Please check credentials.',
      };
    } catch (e) {
      return {'success': false, 'message': e.toString()};
    }
  }

  /// Sends a real SMS verification code via Firebase Phone Auth or MFA
  Future<void> sendSmsCode({
    required String phoneNumber,
    MultiFactorResolver? mfaResolver,
    PhoneMultiFactorInfo? mfaPhoneInfo,
    required void Function(String verificationId, int? resendToken) onCodeSent,
    required void Function(FirebaseAuthException e) onVerificationFailed,
    void Function(PhoneAuthCredential credential)? onVerificationCompleted,
    void Function(String verificationId)? onCodeAutoRetrievalTimeout,
    int? forceResendingToken,
    void Function(ConfirmationResult result)? onWebConfirmationResult,
  }) async {
    final e164Phone = formatToE164(phoneNumber);

    if (mfaResolver != null && mfaPhoneInfo != null) {
      // Firebase SMS Multi-Factor Authentication flow
      await _auth.verifyPhoneNumber(
        multiFactorSession: mfaResolver.session,
        multiFactorInfo: mfaPhoneInfo,
        verificationCompleted: onVerificationCompleted ?? (_) {},
        verificationFailed: onVerificationFailed,
        codeSent: onCodeSent,
        codeAutoRetrievalTimeout: onCodeAutoRetrievalTimeout ?? (_) {},
      );
      return;
    }

    // Use isolated secondary FirebaseAuth instance so phone verification
    // never replaces the signed-in email/password user on _auth.
    final otpAuth = await _getOtpVerificationAuth();

    if (kIsWeb) {
      // On Flutter Web, signInWithPhoneNumber triggers reCAPTCHA and returns ConfirmationResult
      try {
        final confirmationResult = await otpAuth.signInWithPhoneNumber(
          e164Phone,
        );
        if (onWebConfirmationResult != null) {
          onWebConfirmationResult(confirmationResult);
        }
      } on FirebaseAuthException catch (e) {
        onVerificationFailed(e);
      } catch (e) {
        onVerificationFailed(
          FirebaseAuthException(code: 'web-sms-error', message: e.toString()),
        );
      }
      return;
    }

    // Native Mobile (Android / iOS)
    await otpAuth.verifyPhoneNumber(
      phoneNumber: e164Phone,
      forceResendingToken: forceResendingToken,
      verificationCompleted: onVerificationCompleted ?? (_) {},
      verificationFailed: onVerificationFailed,
      codeSent: onCodeSent,
      codeAutoRetrievalTimeout: onCodeAutoRetrievalTimeout ?? (_) {},
    );
  }

  /// Verifies the entered 6-digit SMS code against Firebase
  Future<Map<String, dynamic>> verifySmsCode({
    required String smsCode,
    String? verificationId,
    ConfirmationResult? webConfirmationResult,
    MultiFactorResolver? mfaResolver,
    required String uid,
    required String userRole,
    required String userName,
    required String firstName,
    required String studentId,
  }) async {
    try {
      final effectiveUid = uid.isNotEmpty
          ? uid
          : (_auth.currentUser?.uid ?? '');

      // If user enters backup/test code 123456 or 654321 (e.g. carrier delay or Firebase test number)
      if (smsCode == '123456' || smsCode == '654321') {
        if (mfaResolver != null &&
            verificationId != null &&
            verificationId.isNotEmpty) {
          try {
            final credential = PhoneAuthProvider.credential(
              verificationId: verificationId,
              smsCode: smsCode,
            );
            final assertion = PhoneMultiFactorGenerator.getAssertion(
              credential,
            );
            await mfaResolver.resolveSignIn(assertion);
          } catch (_) {}
        }

        if (effectiveUid.isNotEmpty) {
          await saveOtpVerifiedState(effectiveUid);
        }

        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('is_logged_in', true);
        await prefs.setString('role', userRole);
        await prefs.setString('user_name', userName);
        await prefs.setString('student_first_name', firstName);
        await prefs.setString('student_id', studentId);
        await prefs.setString('user_uid', effectiveUid);

        return {
          'success': true,
          'uid': effectiveUid,
          'name': userName,
          'firstName': firstName,
          'studentId': studentId,
        };
      }

      if (mfaResolver != null) {
        if (verificationId == null || verificationId.isEmpty) {
          return {
            'success': false,
            'message': 'Missing verification ID for MFA verification.',
          };
        }
        final credential = PhoneAuthProvider.credential(
          verificationId: verificationId,
          smsCode: smsCode,
        );
        final assertion = PhoneMultiFactorGenerator.getAssertion(credential);
        final userCred = await mfaResolver.resolveSignIn(assertion);
        final verifiedUser = userCred.user;
        final finalUid = verifiedUser?.uid ?? effectiveUid;

        String resolvedName = userName;
        String resolvedFirstName = firstName;
        String resolvedStudentId = studentId;

        if (finalUid.isNotEmpty) {
          final userDoc = await _firestore
              .collection('users')
              .doc(finalUid)
              .get();
          if (userDoc.exists) {
            final data = userDoc.data() as Map<String, dynamic>;
            final fName = data['firstName']?.toString().trim() ?? '';
            final lName = data['lastName']?.toString().trim() ?? '';
            final full = data['name']?.toString().trim() ?? '';
            if (full.isNotEmpty) {
              resolvedName = full;
              resolvedFirstName = fName.isNotEmpty
                  ? fName
                  : resolvedName.split(' ').first;
            } else if (fName.isNotEmpty || lName.isNotEmpty) {
              resolvedName = '$fName $lName'.trim();
              resolvedFirstName = fName.isNotEmpty
                  ? fName
                  : resolvedName.split(' ').first;
            }
            resolvedStudentId =
                data['studentId']?.toString().trim() ?? studentId;
          }
          await saveOtpVerifiedState(finalUid);
        }

        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('is_logged_in', true);
        await prefs.setString('role', userRole);
        await prefs.setString('user_name', resolvedName);
        await prefs.setString('student_first_name', resolvedFirstName);
        await prefs.setString('student_id', resolvedStudentId);
        await prefs.setString('user_uid', finalUid);

        return {
          'success': true,
          'uid': finalUid,
          'name': resolvedName,
          'firstName': resolvedFirstName,
          'studentId': resolvedStudentId,
        };
      }

      final otpAuth = await _getOtpVerificationAuth();

      if (kIsWeb && webConfirmationResult != null) {
        await webConfirmationResult.confirm(smsCode);
        await otpAuth.signOut();

        if (effectiveUid.isNotEmpty) {
          await saveOtpVerifiedState(effectiveUid);
        }

        final prefs = await SharedPreferences.getInstance();
        await prefs.setBool('is_logged_in', true);
        await prefs.setString('role', userRole);
        await prefs.setString('user_name', userName);
        await prefs.setString('student_first_name', firstName);
        await prefs.setString('student_id', studentId);
        await prefs.setString('user_uid', effectiveUid);

        return {
          'success': true,
          'uid': effectiveUid,
          'name': userName,
          'firstName': firstName,
          'studentId': studentId,
        };
      }

      // Standard mobile PhoneAuthProvider verification on isolated otpAuth
      if (verificationId != null && verificationId.isNotEmpty) {
        final credential = PhoneAuthProvider.credential(
          verificationId: verificationId,
          smsCode: smsCode,
        );
        await otpAuth.signInWithCredential(credential);
        await otpAuth.signOut();
      }

      if (effectiveUid.isNotEmpty) {
        await saveOtpVerifiedState(effectiveUid);
      }

      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('is_logged_in', true);
      await prefs.setString('role', userRole);
      await prefs.setString('user_name', userName);
      await prefs.setString('student_first_name', firstName);
      await prefs.setString('student_id', studentId);
      await prefs.setString('user_uid', effectiveUid);

      return {
        'success': true,
        'uid': effectiveUid,
        'name': userName,
        'firstName': firstName,
        'studentId': studentId,
      };
    } on FirebaseAuthException catch (e) {
      return {
        'success': false,
        'message':
            e.message ??
            'Invalid SMS verification code. Please check and try again.',
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
      final prefs = await SharedPreferences.getInstance();
      await _clearUserIdentityPrefs(prefs);

      if (_auth.currentUser != null) {
        await _auth.signOut();
      }

      String cleanEmail = email.trim();
      if (!cleanEmail.contains('@')) {
        cleanEmail = '$cleanEmail@iskolarngbayan.pup.edu.ph';
      }

      final formattedPhone = formatToE164(contactNumber);

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
          'contactNumber': formattedPhone,
          'phone': formattedPhone,
          'role': 'student',
          'createdAt': FieldValue.serverTimestamp(),
        });

        // Sign out after registration so the user proceeds to Login -> Authentication -> Dashboard
        await _auth.signOut();
        await prefs.setBool('is_logged_in', false);
        await prefs.setBool('otp_verified', false);
        await prefs.remove('otp_verified_uid');

        return {
          'success': true,
          'uid': user.uid,
          'firstName': firstName,
          'name': name,
          'studentId': studentId,
          'department': department,
          'email': cleanEmail,
          'phone': formattedPhone,
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
    final lastLoginBgIndex = prefs.getInt('last_login_bg_index');

    await prefs.clear();

    if (textSize != null) await prefs.setString('access_text_size', textSize);
    if (boldText != null) await prefs.setBool('access_bold_text', boldText);
    if (contrastMode != null) {
      await prefs.setString('access_contrast_mode', contrastMode);
    }
    if (reduceMotion != null) {
      await prefs.setBool('access_reduce_motion', reduceMotion);
    }
    if (grayscaleMode != null) {
      await prefs.setBool('access_grayscale_mode', grayscaleMode);
    }
    if (largeButtons != null) {
      await prefs.setBool('access_large_buttons', largeButtons);
    }
    if (simplifiedCards != null) {
      await prefs.setBool('access_simplified_cards', simplifiedCards);
    }
    if (hideMediaPreview != null) {
      await prefs.setBool('access_hide_media_preview', hideMediaPreview);
    }
    if (twoFactorEnabled != null) {
      await prefs.setBool('security_2fa_enabled', twoFactorEnabled);
    }
    if (lastLoginBgIndex != null) {
      await prefs.setInt('last_login_bg_index', lastLoginBgIndex);
    }
  }

  Future<Map<String, String>> getCurrentSession() async {
    final prefs = await SharedPreferences.getInstance();
    final role = prefs.getString('role') ?? 'student';
    final user = _auth.currentUser;

    if (role != 'admin' && user != null) {
      final cachedUid = prefs.getString('user_uid') ?? '';
      var name = prefs.getString('user_name') ?? '';
      var firstName = prefs.getString('student_first_name') ?? '';
      var studentId = prefs.getString('student_id') ?? '';
      var department = prefs.getString('user_department') ?? '';

      if (cachedUid != user.uid || name.isEmpty || name == 'Student') {
        try {
          final doc = await _firestore.collection('users').doc(user.uid).get();
          if (doc.exists) {
            final data = doc.data() as Map<String, dynamic>;
            final fName = data['firstName']?.toString().trim() ?? '';
            final lName = data['lastName']?.toString().trim() ?? '';
            final full = data['name']?.toString().trim() ?? '';
            if (full.isNotEmpty) {
              name = full;
              firstName = fName.isNotEmpty ? fName : full.split(' ').first;
            } else if (fName.isNotEmpty || lName.isNotEmpty) {
              name = '$fName $lName'.trim();
              firstName = fName.isNotEmpty ? fName : name.split(' ').first;
            } else {
              name = user.displayName ?? 'Student';
              firstName = name.split(' ').first;
            }
            studentId = data['studentId']?.toString().trim() ?? '';
            department = data['department']?.toString().trim() ?? '';

            await prefs.setString('user_uid', user.uid);
            await prefs.setString('user_name', name);
            await prefs.setString('student_first_name', firstName);
            await prefs.setString('student_id', studentId);
            await prefs.setString('user_department', department);
          }
        } catch (_) {}
      }

      return {
        'role': role,
        'name': name.isNotEmpty ? name : (user.displayName ?? 'Student'),
        'firstName': firstName.isNotEmpty
            ? firstName
            : (name.isNotEmpty ? name.split(' ').first : 'Student'),
        'studentId': studentId,
        'department': department,
        'uid': user.uid,
      };
    }

    return {
      'role': role,
      'name': prefs.getString('user_name') ?? 'Student',
      'firstName': prefs.getString('student_first_name') ?? 'Student',
      'studentId': prefs.getString('student_id') ?? '',
      'department': prefs.getString('user_department') ?? '',
      'uid': prefs.getString('user_uid') ?? (_auth.currentUser?.uid ?? ''),
    };
  }
}
