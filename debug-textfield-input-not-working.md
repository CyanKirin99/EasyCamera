# Debug Session: TextField Input Not Working

**Session ID**: `textfield-input-not-working`
**Created**: 2026-06-03
**Status**: OPEN

## Bug Description
BBCH and PlantHeight OutlinedTextField cannot receive user input (tap does not trigger keyboard / text does not change).

## Hypotheses

### H1: Touch event consumed by parent modifier
Parent layout modifiers (weight, scroll, etc.) may consume or intercept touch events before they reach OutlinedTextField, preventing it from gaining focus.

**Evidence needed**: Show that `onValueChange` is NOT called when user taps and types.

### H2: OutlinedTextField too small - touch target clipped
`Modifier.height(40.dp)` may clip the minimum touch target size (48dp) required by Material3, causing touch events to be rejected.

**Evidence needed**: Show touch event coordinates and whether they hit the TextField's touch region.

### H3: Keyboard (IME) not appearing due to missing imePadding / window insets
The Compose view may not properly handle IME (soft keyboard) insets, so the keyboard opens but the TextField remains behind it, or the keyboard never opens because the system doesn't think it's needed.

**Evidence needed**: Show focus state changes and IME visibility events.

### H4: StateFlow update not triggering recomposition
`viewModel.updateBbch(it)` calls `_sessionConfig.update { it.copy(bbch = bbch) }`, but the UI may not be recomposing because the sessionConfig state is not being observed correctly in the scope.

**Evidence needed**: Show that `onValueChange` IS called and the StateFlow IS updated, but the UI text doesn't change.

### H5: AnimatedContent or parent composable interfering with focus
The entire UI might be wrapped in something (like AnimatedContent with a shared element transition) that intercepts or resets focus.

**Evidence needed**: Show the composable parent hierarchy and focus restoration events.

## Plan
1. Start Debug Server
2. Add instrumentation logs to verify/falsify each hypothesis
3. Analyze logs to identify root cause
4. Implement minimal fix
5. Verify with post-fix logs
6. Cleanup after user confirmation