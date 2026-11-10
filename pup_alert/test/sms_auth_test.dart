import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pup_alert/services/auth_service.dart';

void main() {
  group('SMS Authentication & Phone Formatting Tests', () {
    test('Check FirebaseAuth Phone Auth APIs exist', () {
      expect(PhoneAuthProvider, isNotNull);
      expect(ConfirmationResult, isNotNull);
      expect(MultiFactorResolver, isNotNull);
      expect(PhoneMultiFactorGenerator, isNotNull);
    });

    test('formatToE164 normalizes Philippine phone numbers properly', () {
      // 11-digit standard starting with 09
      expect(AuthService.formatToE164('09171234567'), '+639171234567');
      expect(AuthService.formatToE164('0918-987-6543'), '+639189876543');
      expect(AuthService.formatToE164('(0919) 111 2222'), '+639191112222');

      // 10-digit without leading 0
      expect(AuthService.formatToE164('9171234567'), '+639171234567');

      // Already formatted with +63
      expect(AuthService.formatToE164('+639171234567'), '+639171234567');
      expect(AuthService.formatToE164('+63 917 123 4567'), '+639171234567');

      // Other international numbers
      expect(AuthService.formatToE164('+14155552671'), '+14155552671');
    });

    test('maskPhoneNumber masks numbers for user privacy and security', () {
      expect(AuthService.maskPhoneNumber('09171234567'), '+63 9•• ••• •567');
      expect(AuthService.maskPhoneNumber('+639189876543'), '+63 9•• ••• •543');
      expect(AuthService.maskPhoneNumber(''), 'your registered phone number');
    });
  });
}
