// Run with: node --test scripts/tests/inline-player-viewport.test.mjs
// Exercises the actual embedded-page script without downloading a YouTube video.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';

const source = readFileSync(new URL('../../app/src/main/java/com/safarparmar/app/feature/live/presentation/YouTubePlayerWebView.kt', import.meta.url), 'utf8');
const script = source.match(/<script>\s*([\s\S]*?)<\/script>/)[1];

function playerPage(width = 335, height = 200) {
  const frame = { style: {} };
  const events = {};
  const pendingFrames = [];
  const document = {
    documentElement: { style: {} }, body: { style: {} },
    fullscreenElement: null, webkitFullscreenElement: null,
    getElementById: () => frame,
    addEventListener: (name, callback) => { events[name] = callback; },
  };
  const window = {
    innerWidth: width, innerHeight: height,
    requestAnimationFrame: callback => pendingFrames.push(callback),
    addEventListener: (name, callback) => { events[name] = callback; },
  };
  vm.runInNewContext(script, { window, document, console });
  return { frame, window, document, events,
    flush: () => { while (pendingFrames.length) pendingFrames.shift()(); } };
}

function assertFits(page, width, height) {
  const scale = Number(page.frame.style.transform.match(/scale\((.*)\)/)[1]);
  assert.ok(Math.abs(parseFloat(page.frame.style.width) * scale - width) < 0.001);
  assert.ok(Math.abs(parseFloat(page.frame.style.height) * scale - height) < 0.001);
  assert.ok(scale > 0 && scale <= 1, 'Never enlarge the controls above their logical size');
  assert.ok(parseFloat(page.frame.style.width) >= 480, 'Keep space for standard YouTube controls');
}

test('first paint fits narrow and wide cards without cropping', () => {
  for (const width of [240, 335, 412, 480, 800]) {
    const height = width * 9 / 16;
    assertFits(playerPage(width, height), width, height);
  }
});

test('native layout recovers from loading before measurement', () => {
  const page = playerPage(0, 0);
  page.window.safarResizePlayer(335, 200);
  assertFits(page, 335, 200);
});

test('native final bounds take precedence over a stale browser viewport', () => {
  const page = playerPage(800, 450);
  page.window.safarResizePlayer(335, 200);
  page.events.resize();
  page.flush();
  assertFits(page, 335, 200);
});

test('fullscreen ignores underlying inline layout changes', () => {
  const page = playerPage();
  const portrait = { ...page.frame.style };
  page.window.safarSetPlayerFullscreen(true);
  page.window.safarResizePlayer(800, 450);
  page.events.resize();
  page.flush();
  assert.deepEqual(page.frame.style, portrait);
});

test('repeated fullscreen round trips restore exactly the same portrait control scale', () => {
  const page = playerPage();
  page.window.safarResizePlayer(335, 200);
  const portrait = { ...page.frame.style };
  for (let cycle = 0; cycle < 10; cycle++) {
    page.window.safarSetPlayerFullscreen(true);
    page.window.safarResizePlayer(800, 450);
    page.window.safarSetPlayerFullscreen(false);
    page.flush();
    page.window.safarResizePlayer(335, 200);
    page.events.fullscreenchange();
    page.flush();
    assert.deepEqual(page.frame.style, portrait);
    assertFits(page, 335, 200);
  }
});

test('delayed browser fullscreen exit is resized after its completion event', () => {
  for (const fullscreenProperty of ['fullscreenElement', 'webkitFullscreenElement']) {
    const page = playerPage();
    const portrait = { ...page.frame.style };
    page.document[fullscreenProperty] = page.frame;
    page.window.safarResizePlayer(600, 340);
    assert.deepEqual(page.frame.style, portrait);
    page.document[fullscreenProperty] = null;
    page.events.fullscreenchange();
    page.flush();
    assertFits(page, 600, 340);
  }
});
