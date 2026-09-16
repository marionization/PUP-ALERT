import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;

/// Default [FirebaseOptions] for use with your Flutter apps.
/// Configured for PUP ALERT (pup-alert-b9ece)
class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    if (kIsWeb) {
      return web;
    }
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      case TargetPlatform.macOS:
        return ios;
      case TargetPlatform.windows:
        return web;
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  static const FirebaseOptions web = FirebaseOptions(
    apiKey: 'AIzaSyDGv5ftNKDMc99cvI_z5s_be_A8bCnroUo',
    appId: '1:729864961364:web:58c4c3260f39a3ffcf5cc1',
    messagingSenderId: '729864961364',
    projectId: 'pup-alert-b9ece',
    authDomain: 'pup-alert-b9ece.firebaseapp.com',
    storageBucket: 'pup-alert-b9ece.firebasestorage.app',
  );

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyDGv5ftNKDMc99cvI_z5s_be_A8bCnroUo',
    appId: '1:729864961364:android:58c4c3260f39a3ffcf5cc1',
    messagingSenderId: '729864961364',
    projectId: 'pup-alert-b9ece',
    storageBucket: 'pup-alert-b9ece.firebasestorage.app',
  );

  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'AIzaSyDGv5ftNKDMc99cvI_z5s_be_A8bCnroUo',
    appId: '1:729864961364:ios:58c4c3260f39a3ffcf5cc1',
    messagingSenderId: '729864961364',
    projectId: 'pup-alert-b9ece',
    storageBucket: 'pup-alert-b9ece.firebasestorage.app',
    iosBundleId: 'com.example.seriousmode',
  );
}
