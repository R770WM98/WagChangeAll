# Approved Rules to Reminders Integration

## Summary

The predictive rules system is now fully integrated with the reminders system. When users approve AI-suggested maintenance rules, they can save them as actual reminders in the app.

## How It Works

### 1. UI Flow
- User navigates to "Predictive Rules" screen
- AI generates predictive maintenance suggestions (component, suggested interval, rationale, risk, confidence)
- User reviews suggestions and clicks "Approve" to add them to the "Approved Rules" section
- When ready, user clicks "Save Approved Rules" button
- The approved rules are converted to service reminders

### 2. Implementation Details

#### Interval Parsing
The system automatically parses AI-generated interval strings like:
- "6000 km or 6 months"
- "5000-6000 km"
- "12 months"
- "3000 km"

And extracts:
- **km value**: Used as the repeating interval for km-based triggers
- **days value**: Used as the date-based trigger (months converted to ~30 days per month)

#### Reminder Creation
Each approved rule creates a reminder with:
- **Title**: "AI Suggested: {component_name}" (e.g., "AI Suggested: Chain Lubrication")
- **Trigger Km**: Current odometer + suggested km interval
- **Trigger Date**: Today + calculated days from the interval
- **Repeating Interval**: The km interval (if present), so reminders repeat at that interval
- **Status**: Marked as active (not archived)

### 3. Files Modified

#### 1. `feature/predictive/PredictiveRulesViewModel.kt`
**Changes:**
- Added `ReminderRepository` dependency injection
- Added `isSaving` and `saveMessage` state fields to track save progress
- Implemented `saveApprovedRules()` method that:
  - Parses interval strings
  - Converts approved suggestions to reminder parameters
  - Calls `reminderRepository.addReminder()` for each approved rule
  - Displays success message with count of saved reminders
  - Clears the approved suggestions list after saving
- Added `parseInterval()` helper method to extract km and days from strings using regex patterns

#### 2. `feature/predictive/PredictiveRulesScreen.kt`
**Changes:**
- Added success message card (similar to error card) to show when rules are saved
- Updated "Save Approved Rules" button to:
  - Show loading indicator while saving
  - Disable button during save operation
  - Display user feedback

#### 3. `core/navigation/AppNavGraph.kt`
**Changes:**
- Added import for `WagChangeReminderRepository`
- Instantiate `WagChangeReminderRepository` when creating `PredictiveRulesViewModel`
- Pass repository to viewmodel constructor for dependency injection

## Testing the Feature

### Manual Testing Steps:
1. Launch the app with Gemini API key enabled
2. Navigate to "Predictive Rules" screen
3. Wait for AI to generate suggestions
4. Click "Approve" on 1-2 suggestions
5. Click "Save Approved Rules" button
6. Verify:
   - Success message appears: "Successfully saved X reminder(s)"
   - Approved list clears
   - Navigate to "Reminders" screen
   - The new reminders appear with names like "AI Suggested: Chain Lubrication"

### Debugging:
- Check Logcat for tag `PredictiveRulesVM` if save fails
- Error messages are displayed in red error card on screen
- Each failed reminder logs to Logcat but continues processing others

## Interval Parsing Examples

| Input String | Parsed Km | Parsed Days |
|---|---|---|
| "6000 km or 6 months" | 6000 | 180 |
| "5000 km" | 5000 | 0 |
| "12 months" | 0 | 360 |
| "3000-4000 km" | 3000 | 0 |
| "2 months or 1000 km" | 1000 | 60 |

## Error Handling

- **Parsing errors**: Non-standard interval formats gracefully default to 0 for km/days
- **Reminder creation errors**: Individual reminder failures don't prevent others from being created; errors are logged
- **Network/database errors**: Caught and displayed to user in error card
- **Empty approved list**: User is shown message "No approved rules to save"

## Future Enhancements

Possible future improvements:
1. Allow users to edit/adjust intervals before saving
2. Add bulk import of multiple rules at once
3. Create a "pending rules" feature to stage rules before final approval
4. Add rule templates for common motorcycle maintenance items
5. Show estimated next due date/km when saving rules
6. Add confirmation dialog before saving rules

## Related Classes

- `ServiceReminder`: Domain model for reminders
- `ReminderRepository`: Interface for reminder CRUD operations
- `WagChangeReminderRepository`: Implementation using WagChangeRepository
- `WagChangeRepository`: Main data repository with reminder storage
- `PredictiveRulesSuggestion`: AI-generated suggestion data class


