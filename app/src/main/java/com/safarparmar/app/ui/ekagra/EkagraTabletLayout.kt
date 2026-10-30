package com.safarparmar.app.ui.ekagra

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Shared outer edges for the summary cards and navigation dock. */
internal fun ekagraTabletChromeWidth(availableWidth: Dp): Dp =
    (availableWidth * 0.68f).coerceAtMost(680.dp)
