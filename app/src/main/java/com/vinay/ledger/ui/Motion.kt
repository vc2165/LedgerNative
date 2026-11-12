package com.vinay.ledger.ui

import androidx.compose.animation.core.CubicBezierEasing

// Same curves the original Ledger used
val EaseStd = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)      // tabs, months, charts
val EaseSheet = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)    // sheets sliding up
val EaseSheetOut = CubicBezierEasing(0.4f, 0f, 1f, 1f)     // sheets sliding away
