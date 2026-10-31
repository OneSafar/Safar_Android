<!-- Hallmark · pre-emit critique: P4 H4 E4 S5 R4 V4 -->
# Mehfil design options

ImageGen concept mockups for selection; no app implementation changes.

1. Warm card feed: ivory canvas, white cards, peach announcement row, plum pill controls, centered create button. Recommended for consistency with updated profile and sidebar.
2. Plum editorial: plum masthead, underlined filters, open feed with separators, floating Post button.
3. Lavender conversation: grouped lavender feed, segmented filters, category labels above body copy, floating create button.

Reference: user-supplied Mehfil screenshot; palettes read from ProfileCompactLayout.kt, PremiumMembershipVisuals.kt and SoftSidebar.kt. Existing screenshot provides all example authors, post copy, counts and announcement count. Artwork and sample avatars are illustrative.

Preserved actions: drawer, guide, search, info, category filters, collapsible Sandesh, post overflow, likes, comments, bookmark, Connect for named author, Community/Saved/Activity/Chats navigation, create post.

These images define visual direction, not verified Compose renders. Final implementation must accommodate device width, larger fonts, long post text, localization, IME and safe insets, and retain existing visibility and access checks.
