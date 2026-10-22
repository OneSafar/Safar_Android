# Library Guide

This reference is intentionally version-agnostic. Always verify current installation/API details from upstream before coding.

## Vico

Repository: https://github.com/patrykandpatrick/vico

Use as the default for polished conventional Cartesian analytics: line/trend, column/bar, combined charts, axes, markers, and interactions.

Inspection checklist:

- README / official guide linked by repository
- current stable release
- Compose/Android module coordinates
- sample module/source for the requested chart
- current model update pattern
- theming/components for axes, lines, columns, markers, legends
- LICENSE

## Drafter

Repository: https://github.com/AndroidPoet/Drafter

Use when the requested visual is specialized. The upstream project currently advertises a broad catalog including bars, lines, scatter/bubble, box/candlestick, pie/donut/funnel/treemap/sunburst, radar, Gantt, gauge, bullet, Sankey, stream graph, and contribution heatmap.

Inspection checklist:

- README
- dependency coordinates and current stable version
- renderer/data classes for exact chart type
- theme support
- animation APIs
- sample implementation
- LICENSE

## JetCo

Repository: https://github.com/developerchunk/JetCo

Use when a JetCo chart/component is a good lightweight fit or when its source is a better basis for a small app-owned implementation.

The upstream project exposes Compose UI components and chart examples such as live/multi-line, candlestick, pie, and bar variants. Verify the current library installation/documentation before deciding to depend on it.

Inspection checklist:

- README/docs
- current artifact/module usage if published
- exact component/sample source
- transitive dependencies
- API stability
- LICENSE

## Selection heuristic

```text
Standard analytics chart?       -> Vico first
Specialized visualization?      -> Drafter first
Small reusable visual/example?  -> JetCo candidate
Very bespoke/simple graphic?    -> Custom Compose Canvas
```

Use only what the screen needs. A single analytics screen should not automatically depend on all three libraries.
