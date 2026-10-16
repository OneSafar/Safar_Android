# Titli help and replies

The global Titli menu greets the signed-in user by name (or uses a generic greeting),
then offers Ekagra, Goals, YouTube Focus, Habit Tracker, Other, and Your queries and
replies. Selecting a feature navigates there and opens its registered guide.
Ekagra and Goals retain their guided practice lessons. YouTube Focus and Habit
Tracker have short feature instructions; YouTube permission setup retains its
existing separate permission walkthrough. The final lesson action now reads OK
in English, Hindi, and Hinglish.

Other opens the Ask Titli screen. Queries must contain 3–2000 characters. A failed
send retains the draft and request ID; retrying the same draft does not create a
second record on the updated server. Changing accounts clears drafts and inbox
state. Late responses from a previous account are ignored. Late inbox reads
cannot remove a newly submitted query from the current screen.

The backend changes are in the sibling SAFAR checkout:

- POST /api/feedback stores the query under the authenticated account with
  feature=titli and a stable requestId. Ordinary feedback remains supported.
- GET /api/feedback/titli/mine returns the account’s latest 50 queries/replies.
- GET /api/feedback/titli/admin returns the latest 100 queries for admins.
- POST /api/feedback/titli/:id/reply accepts an admin reply and requestId, with
  an atomic update preventing competing or duplicate replies.
- /admin/titli is the web team inbox, protected by the existing admin guard.

A reply creates a private bell notification and uses the existing Android push
service with the title You got a reply from Titli. The safar://titli/queries deep
link opens the app inbox. Push respects existing device/user settings; replies
remain readable in the inbox when push is disabled or delivery fails. No email
forwarding or new credentials are required. Each query currently has one team reply.

Deployment: deploy the SAFAR backend and web changes before enabling live reply
reads and notifications. The Android APK is built locally; this task did not deploy
the server or submit any real user query. Phone installation/checks are manual at
the user’s request. Android instrumentation tests are supplied and compile, but
could not be run because no device was connected.
