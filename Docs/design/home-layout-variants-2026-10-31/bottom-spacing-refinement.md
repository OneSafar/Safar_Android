# Lower Home spacing refinement

Tool labels increased from 11sp/14sp to 12sp/15sp with equal two-line label slots. Artwork aligns to the bottom of its image slot, leaving a consistent 4dp between image and label rather than centering it above surplus whitespace. Horizontal grid gutters increased from 8dp to 12dp, card side padding from 12dp to 16dp, and vertical grid spacing from 6dp to 8dp. Existing image assets, routes and press feedback are preserved.

Dashboard button height increased from 48dp to 56dp and horizontal inset from 16dp to 24dp. Footer now reserves 12dp above and 24dp below the button, plus system navigation insets. This changes the previous 64dp footer budget to 92dp. Flexible main sections absorb the difference; the phone layout remains non-scrolling. Larger labels have reserved two-line slots so the rows stay aligned.

Source and whitespace checks only. No compilation, build, installation or runtime visual verification performed.
