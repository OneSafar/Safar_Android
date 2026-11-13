# Calm Centre motion refinement

- Blossom previews: 20-second cycle (half their previous speed). Guided breathing remains
  synchronized to the existing four-second inhale and six-second exhale.
- Blossom pigments: richer apricot, terracotta and golden peach, with unchanged text colors.
- Kindness: 24-second projected orbit; companions exchange sides every 12 seconds, with
  depth-dependent scale, slight perspective compression, Z tilt, overlapping draw order,
  following shadows and a softly floating paired petal.
- Wind-down: moon heights follow the four existing prompts, descending, rising, then settling.
  Each transition takes up to 4.5 seconds with zero velocity and acceleration at the endpoints.
  Halos follow the moon and clouds drift behind its face. Gallery artwork uses a closed float
  loop instead of resetting the session choreography.
- All practice motion uses the existing elapsed-time source; pause and reduced motion still apply.

Verification: eight unit tests passed, including orbit closure and step-boundary continuity
at one-, two- and five-minute durations. The focused instrumented motion test passed on the
USB-connected moto g45, checking all four wind-down prompts and paused session behavior.
Screenshots show the kindness orbit at three positions and each wind-down step.
