<!-- Hallmark · pre-emit critique: P4 H4 E4 S5 R4 V4 -->
# Safar first-launch design boards

Three ImageGen concept boards, each showing the same eight flow states: welcome, sign in, create account, reset password, check email, new password, goals questionnaire, protection preferences questionnaire. User explicitly clarified first-launch onboarding, not KAVACH permission setup.

1. Soft garden: open ivory canvas, plum pills, tutorial-inspired static leaves and geometry. Recommended.
2. Plum editorial: consistent plum header band, peach field-icon tiles, quiet corner decorations.
3. Peach paper: white form cards, peach arch and lilac accents, low coral wave.

Reference code: SplashScreen.kt, AuthScreen.kt, LaunchUsageQuestionnaireScreen.kt; SafarOnboardingScreen.kt is currently a standalone preview component, not a separate active route. No invented onboarding feature slides were added. Existing tutorial artwork references: TutorialLessonMenu.kt and TutorialMenuArtwork.kt (shared palette).

Concepts only. No authentication, questionnaire, onboarding navigation or backend behavior changed. Brand symbol in generated concepts is illustrative: retain the actual Safar brand asset during implementation. Preserve validation, password reset states, Google login, conditional custom exam field and all existing form options. Google button branding and localized larger-text sizing need verification on implementation.

Separate authorized implementation: KAVACH's inactive main screen now draws static tutorial geometry below the measured controls. It uses the existing tutorial art palette and no animation, bitmap, touch handling, or reserved layout area. It is hidden when protection controls expand or less than 140dp of blank space remains.
