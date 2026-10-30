# Premium header text visibility fix

The peach/coral/leaf illustration previously used an absolute TopEnd placement with a fixed vertical offset. Its layout did not reserve height before the full-width membership details, so descriptions could overlap pale artwork in both themes.

The headline and illustration now share a measured Row. The illustration has its own clipped bounds and the membership details start below the entire Row, including any wrapped or font-scaled heading. Dark and light themes share this layout correction. The approved palette, original Titli component, content, and payment behavior remain unchanged.

Verification: production-debug assembly only; no tests or device runs. See assemble.log for the completed result.
