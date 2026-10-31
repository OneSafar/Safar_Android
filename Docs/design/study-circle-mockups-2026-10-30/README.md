<!-- Hallmark · pre-emit critique: P4 H4 E4 S5 R4 V4 -->
# Study Circle mockups and component audit

Latest requested variant: 04-plum-peach-workspace.png keeps direction C structure and changes lavender to ivory, plum and peach. Prior three directions preserved for reference. ImageGen concepts, not rendered Compose screens. App unchanged.

## Audit result

StudyCircleScreen.kt currently imports Material3 wildcard controls plus SafarMaterialAlertDialog, SafarModalBottomSheet, SafarDropdownMenu, and generic SafarDialog. These are not Composables UI implementations. The design boards do not perform migration. Existing app already depends on com.composables:ui:0.2.0 and uses SafarComposablesTheme elsewhere.

Current documentation checked through Composables MCP: buttons, tabs, text-fields, dropdown-menu, alert-dialog, bottom-sheet.

## Complete surface mapping

| Surface / states | Planned Composables UI implementation | Existing behavior to preserve |
|---|---|---|
| Hub actions, refresh, notification bell | Button, IconButton, Icon, Text | busy/disabled state, five-circle limit and server creationEligibility |
| All/Yours and public search | Tabs or selectable Buttons, state-based TextField | counts, search filtering, pagination, joined status |
| Official, public and private list rows | Compose layout, library Text/Icon/Button | admin-pinned official status, owner badge, member/capacity/focusing counts, join versus open |
| Create dialog | SafarComposablesAlertDialog, TextField, Button selection | 40-character name cap, private/public visibility, required streak eligibility, busy and errors |
| Join-code dialog | SafarComposablesAlertDialog, TextField, Button | code validation, already-member result, busy, server errors |
| Detail public preview and member view | Compose card layouts, library Text/Icon/Button | preview join banner, creator, role, counts, ranked self row, focusing and durations |
| Connect actions | compact Button, Icon, Text | access gate and DM callback, no Connect on self, loading/busy |
| Self menu | SafarComposablesDropdownMenu / DropdownMenuPanel / DropdownMenuItem | Leave action only; not an invented View profile action |
| Owner/admin menu | same scoped dropdown API, Destructive item style | pin/unpin only for admins, rename permissions, delete and leave rules |
| Private circle code | Text, IconButton | copy existing backend joinCode only when allowed; no generated secrets |
| Visibility toggle | Button choices | owner only and not pinned; preserve backend confirmation and busy behavior |
| Rename dialog | SafarComposablesAlertDialog, TextField, Button | 40 characters, undo, save request, busy, snackbar/retry |
| Leave/delete/remove confirmations | SafarComposablesAlertDialog, destructive Button | member leave, owner transfer, last-member archive, delete warning, remove target |
| Connection requests | SafarComposablesBottomSheet, library Text/Icon/Button | pending list, accept/decline, accepting spinner, empty state, Open chats route |
| Loading/error/empty states | library progress/buttons/text with Compose layouts | retry callbacks, no fake data or fabricated counts |

Use SafarComposablesTheme with local plum accent above all sibling dialogs and sheets; use the existing popup density/bounds wrappers for actual library windows. Use library Text and Icon inside panels and button slots. No dependency addition needed.

## Data and permission constraints

DTOs checked: StudyCircleSummaryDto, PublicStudyCircleDto, StudyCircleDetailDto, StudyCircleMemberDto, StudyCircleLeaderboardEntryDto, creationEligibility and request/response DTOs. Backing repository/viewmodel and routes remain authoritative. Do not change requests or relax limit, streak, premium, owner/admin, visibility or destructive-action checks for a design.

Boards combine alternative states for design review, not simultaneous runtime UI. Student name in request row is explicitly sample placeholder. Counts/avatars are illustrative snapshots from supplied screenshots. Text in generated images is illustrative: official status must use isPinned rather than visual name matching; owner visibility control hides for pinned circles; join/create buttons must actually disable at limit even where the mockup looks prominent. Private-code availability follows backend and existing permissions; board Owner only annotation must not override code.

Dates, time and live status strings must use correct localization/plurals and backend values; no concatenated 0m and weekly time. Error insets are alternate states. Top Connect pill must retain usable touch area, avoid crowding duration, and expose premium access semantically without oversized labels.

Implementation verification after design choice: compile proper product flavor, preserve dialogs and input state, IME/safe insets, screen width and large text, reduced-motion policy, dark equivalents. User requested no emulator inspection in this conversation.
