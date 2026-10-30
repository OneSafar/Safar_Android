# Exam Planner concepts — 23 October 2026

Three concept boards created with built-in image_gen. Each shows Exams, Today, Syllabus, Calendar, Progress and Create exam plan. Original prompts and targeted refinements are in prompts.json; manifest.json records the final PNGs and source hashes.

1. [Warm Clarity](01-warm-clarity.png): blush canvas, plum actions, peach tiles, mint completion and rounded cards.
2. [Study Desk](02-study-desk.png): ivory/forest, editorial headings and orderly checklist rows.
3. [Focus Grid](03-focus-grid.png): ice-blue/cobalt, compact modular summaries and a next-topic panel.

**Selected: 01 / Warm Clarity**, explicitly confirmed by the user. This is the sole visual direction for implementation across the planner, including detail sheets, revision, settings, empty/loading/error states and plan creation. Do not mix in the other two concepts.

All figures and secondary copy are illustrative sample content. Generated dates, clock times, duration estimates and study-style wording are not new product capabilities or replacement business rules. Implementation must use the existing real data, localized copy, actual style definitions and PlannerActions; never introduce hourly scheduling/estimated durations from the illustration. Preserve all supported features, confirmation and undo behavior, premium rules and topic-linked Ekagra navigation.

The boards are static visual references. Responsive behavior, dark colors, accessible semantics and motion must be implemented and checked in Compose rather than inferred from the bitmap.
