import 'package:shared_preferences/shared_preferences.dart';

class UserOnboardingProfile {
  final String churchName;
  final String name;
  final String group;
  final String gender;

  const UserOnboardingProfile({
    required this.churchName,
    required this.name,
    required this.group,
    required this.gender,
  });
}

class OnboardingStorage {
  static const _baseKey = 'onboarding_profile';
  static const _keyChurch = 'church';
  static const _keyName = 'name';
  static const _keyGroup = 'group';
  static const _keyGender = 'gender';

  String _churchKey(String userId) => '$_baseKey.$userId.$_keyChurch';
  String _nameKey(String userId) => '$_baseKey.$userId.$_keyName';
  String _groupKey(String userId) => '$_baseKey.$userId.$_keyGroup';
  String _genderKey(String userId) => '$_baseKey.$userId.$_keyGender';

  Future<UserOnboardingProfile?> getProfile(String userId) async {
    final prefs = await SharedPreferences.getInstance();
    final churchName = prefs.getString(_churchKey(userId));
    final name = prefs.getString(_nameKey(userId));
    final group = prefs.getString(_groupKey(userId));
    final gender = prefs.getString(_genderKey(userId));

    if (churchName == null ||
        churchName.trim().isEmpty ||
        name == null ||
        name.trim().isEmpty ||
        group == null ||
        group.trim().isEmpty) {
      return null;
    }

    return UserOnboardingProfile(
      churchName: churchName,
      name: name,
      group: group,
      gender: (gender == null || gender.trim().isEmpty) ? 'UNKNOWN' : gender,
    );
  }

  Future<void> saveProfile({
    required String userId,
    required String churchName,
    required String name,
    required String group,
    required String gender,
  }) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(_churchKey(userId), churchName.trim());
    await prefs.setString(_nameKey(userId), name.trim());
    await prefs.setString(_groupKey(userId), group.trim());
    await prefs.setString(_genderKey(userId), gender.trim());
  }

  Future<bool> isCompleted(String userId) async {
    final profile = await getProfile(userId);
    return profile != null;
  }

  Future<void> clearProfile(String userId) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove(_churchKey(userId));
    await prefs.remove(_nameKey(userId));
    await prefs.remove(_groupKey(userId));
    await prefs.remove(_genderKey(userId));
  }
}
