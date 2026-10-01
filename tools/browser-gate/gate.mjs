// gate.mjs <url> <outdir> — the browser gate's driver, run by gate.sh (r137).
//
// Plays the page the way a player would, in HEADLESS Chrome with its audio muted (never a window
// or a sound on anyone's machine, D6): the game must open on the intro, its first page
// carrying the team's logo (r158: the logos ride on the intro's pages, no screen of their own), real
// mouse clicks must turn the intro's pages to the start screen, and a carriage must open. It drives
// the system Chrome through puppeteer-core, NOT `chrome --screenshot`, which pumps about five
// animation frames and then shoots a game that looks frozen (La chasse-galerie's
// docs/browser-target.md section 10). Screenshots and gate.json land in <outdir>.
//
// It fails when: the game does not run, it does not open on the intro, clicks do not reach the
// start screen, no music plays, a French letter has no glyph, the config is not kept in
// localStorage or not read back from it, the carriage does not open, or the loop runs under 20 fps.
// And when the tab is resized, the game must fill it at 16:9, centred, drawn at the canvas's own
// size and not scaled by CSS (r179, D3): 6_fit_<w>x<h>.png, landscape and portrait. On a devicePixelRatio 2
// tab the canvas must hold twice the CSS pixels (r184) and run within 80% of a ratio-1 tab of the same
// pixels (r206), and at 10+ fps: 7_dpr1/2.png.
import puppeteer from 'puppeteer-core';
import { writeFileSync } from 'node:fs';

const [url, out] = process.argv.slice(2);
// BROWSER=firefox drives the system Firefox instead (WebDriver BiDi), for GWT's other permutation
const firefox = process.env.BROWSER === 'firefox';
const chrome = process.env.CHROME || '/usr/bin/google-chrome';
const say = (line) => console.log('gate: ' + line);
const failures = [];
const fail = (why) => { failures.push(why); console.error('gate: FAILED — ' + why); process.exitCode = 1; };
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// Every accented letter and French mark the text table uses, and the capitals a title could take
const FRENCH = 'éèêëàâäçîïôöûùüÿœæ’‘“”…«»–—ÉÈÊÀÂÇÎÔÛŒ';

const browser = await puppeteer.launch(firefox ? {
	browser: 'firefox',
	executablePath: process.env.FIREFOX || '/usr/bin/firefox',
	// Headless Firefox has no WebGL on this machine, so gate.sh runs it in a window inside cage (offscreen)
	headless: !process.env.ATELIER_INSIDE_CAGE,
	// Silent (volume scale 0, the page's audio still plays) and allowed to start music unprompted
	extraPrefsFirefox: { 'media.volume_scale': '0.0', 'media.autoplay.default': 0, 'webgl.force-enabled': true },
	defaultViewport: { width: 1280, height: 720 },
} : {
	executablePath: chrome,
	headless: true,
	// SwiftShader is software WebGL: no GPU assumed. --mute-audio keeps the speakers silent while the
	// page's audio still plays, so the game's own "is the music playing" stays honest.
	args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--autoplay-policy=no-user-gesture-required',
		'--mute-audio', '--window-size=1280,720', '--no-first-run', '--no-default-browser-check'],
	defaultViewport: { width: 1280, height: 720 },
});
const report = { url, browser: firefox ? 'firefox' : 'chrome', war: { files: +process.env.WAR_FILES || null, bytes: +process.env.WAR_BYTES || null }, console: [] };
const probe = (page, js, ...args) => page.evaluate(js, ...args);
const vue = (page) => probe(page, () => window.onboard.vue());
const waitVue = (page, name, timeout) =>
	page.waitForFunction((n) => window.onboard && window.onboard.vue() === n, { timeout, polling: 100 }, name);

async function open(page, query) {
	const t0 = Date.now();
	await page.goto(url + query, { waitUntil: 'load' });
	// The preloader pulls 33 MB of assets before the first frame
	await page.waitForFunction(() => window.onboard && window.onboard.frames() > 30, { timeout: 90000, polling: 100 });
	return Date.now() - t0;
}

// The canvas in a tab of this size: the largest 16:9 box it holds, centred, holding the screen's own pixels
// (dpr of them to a CSS pixel, r184) and shown at the box's CSS size, and the game drawing at the canvas's size.
// Shot as 6_fit_<name>.png.
async function fit(page, width, height, name, dpr = 1) {
	await page.setViewport({ width, height, deviceScaleFactor: dpr });
	await sleep(1500);
	const got = await probe(page, () => {
		const c = document.querySelector('canvas'), r = c.getBoundingClientRect();
		return { w: c.width, h: c.height, cssW: r.width, cssH: r.height, left: r.left, top: r.top, game: window.onboard.size(), dpr: devicePixelRatio };
	});
	const box = Math.min(width, Math.floor(height * 16 / 9));
	const w = Math.floor(box * dpr), h = Math.floor(w * 9 / 16);
	const tab = `${width}x${height}${dpr === 1 ? '' : '@' + dpr}`;
	report.fit[tab] = got;
	await page.screenshot({ path: `${out}/6_fit_${name}.png` });
	say(`tab ${tab}: canvas ${got.w}x${got.h} shown ${got.cssW}x${got.cssH} at ${Math.round(got.left)},${Math.round(got.top)}, game ${got.game}`);
	if (got.w !== w || got.h !== h) fail(`tab ${tab}: canvas ${got.w}x${got.h}, not the ${w}x${h} that fills it at 16:9`);
	if (Math.abs(got.cssW * dpr - got.w) > 1 || Math.abs(got.cssH * dpr - got.h) > 1) fail(`tab ${tab}: CSS shows the ${got.w}x${got.h} canvas at ${got.cssW}x${got.cssH}, not ${1 / dpr} of it`);
	if (Math.abs(got.left - (width - got.cssW) / 2) > 1 || Math.abs(got.top - (height - got.cssH) / 2) > 1) fail(`tab ${tab}: canvas not centred (${got.left},${got.top})`);
	if (got.game !== `${w}x${h}`) fail(`tab ${tab}: the game draws at ${got.game}, not ${w}x${h}`);
}

async function fps(page, seconds = 3) {
	const f0 = await probe(page, () => window.onboard.frames());
	await sleep(seconds * 1000);
	return Math.round(((await probe(page, () => window.onboard.frames())) - f0) / seconds);
}

try {
	const page = await browser.newPage();
	page.on('console', (m) => report.console.push(m.type() + ': ' + m.text()));
	page.on('pageerror', (e) => report.console.push('pageerror: ' + e.message));

	// 1. The intro, its first page and the team's logo
	report.firstFramesMs = await open(page, '');
	say(`running after ${report.firstFramesMs} ms, on ${await vue(page)}`);
	try {
		await waitVue(page, 'Vue_Scenematic_Intro', 60000);
	} catch {
		fail(`the game did not open on the intro in 60 s (still on ${await vue(page)})`);
		throw new Error('stopped');
	}
	// The page takes 2 s to come up and its logo 1.5 s more
	await sleep(4000);
	await page.screenshot({ path: `${out}/2_intro.png` });
	report.introMusic = await probe(page, () => window.onboard.music());
	say(`intro reached; music ${report.introMusic || 'NONE'}`);
	if (!report.introMusic) fail('no music is playing in the intro (musics/intro.mp3)');

	// 2. Real clicks turn the pages. Stop the moment the start screen is up: a click more could press a button
	report.introClicks = 0;
	while ((await vue(page)) !== 'Vue_StartScreen' && report.introClicks < 12) {
		await page.mouse.click(640, 360);
		report.introClicks++;
		await waitVue(page, 'Vue_StartScreen', 3500).catch(() => {});
	}
	if ((await vue(page)) !== 'Vue_StartScreen') {
		fail(`${report.introClicks} clicks did not reach the start screen (still on ${await vue(page)})`);
		throw new Error('stopped');
	}
	await sleep(3000);
	await page.screenshot({ path: `${out}/3_start.png` });
	say(`start screen after ${report.introClicks} clicks`);
	// Options, for the render: in a tab its Graphics board has only the mipmaps row (Platform.choosesWindowSize).
	// "Options" is the second word of the menu at 1280x720 (3_start.png)
	await page.mouse.click(155, 453);
	await sleep(2000);
	await page.screenshot({ path: `${out}/3_options.png` });

	// 3. FreeType in the browser: every French letter has a glyph in both faces the game writes French in
	report.missingGlyphs = await probe(page, (t) => window.onboard.missingGlyphs(t), FRENCH);
	if (report.missingGlyphs) fail('letters without a glyph: ' + report.missingGlyphs.replace(/\n/g, '; '));

	// 4. The config: written to localStorage on the first start, and read back on the next
	const stored = await probe(page, () => Object.keys(localStorage).filter((k) => k.startsWith('onboard')));
	report.configKeys = stored;
	const key = stored.find((k) => k.includes('config'));
	if (!key) {
		fail('the config was not written to localStorage (keys: ' + JSON.stringify(stored) + ')');
	} else {
		await probe(page, (k) => localStorage.setItem(k, localStorage.getItem(k).replace(/"volume"\s*:\s*[0-9.]+/, '"volume": 0.37')), key);
		await open(page, '?start=start_screen');
		report.volumeAfterReload = await probe(page, () => window.onboard.volume());
		if (Math.abs(report.volumeAfterReload - 0.37) > 1e-4) fail(`the config was not read back: volume ${report.volumeAfterReload}, not 0.37`);
		else say('config kept in localStorage and read back');
	}

	// 5. The credits: the screen with the most French on it
	await open(page, '?start=credits');
	await sleep(2500);
	await page.screenshot({ path: `${out}/4_credits.png` });

	// 6. A carriage: its backdrop is read from the .plaxpj, its music is an .ogg made .mp3 (r152)
	report.carriageMs = await open(page, '?start=game&level=1');
	try {
		await waitVue(page, 'Vue_Game', 20000);
	} catch {
		fail(`the carriage did not open (on ${await vue(page)})`);
	}
	await sleep(2000);
	report.fps = await fps(page);
	await page.screenshot({ path: `${out}/5_carriage.png` });
	report.carriageMusic = await probe(page, () => window.onboard.music());
	say(`carriage 1 open; music ${report.carriageMusic || 'NONE'}; ${report.fps} fps`);
	if (!report.carriageMusic) fail('no music is playing in carriage 1');
	// A page stuck on a handful of frames is what --screenshot shows; a running game is far above this
	if (report.fps < 20) fail(`${report.fps} fps: the game loop is not running`);

	// 7. The tab resized under a running carriage (the resize handler), then a portrait tab from the start (the config)
	report.fit = {};
	await fit(page, 1600, 900, 'wide');
	await fit(page, 1000, 800, 'tall');
	await fit(page, 1700, 700, 'short');
	await fit(page, 420, 900, 'portrait');
	await open(page, '?start=start_screen');
	await sleep(2500);
	await fit(page, 420, 900, 'portrait_start');

	// 8. A high-DPI screen (r184, D3): a carriage opened on a devicePixelRatio 2 tab has twice the canvas
	// pixels, so the art is not upscaled by the browser, and the ratio costs nothing beyond those pixels.
	// 7_dpr1.png and 7_dpr2.png are the same carriage at ratio 1 and 2, for a crop compared side by side.
	await page.setViewport({ width: 1280, height: 720, deviceScaleFactor: 1 });
	await open(page, '?start=game&level=1');
	await waitVue(page, 'Vue_Game', 20000).catch(() => {});
	await sleep(2000);
	await page.screenshot({ path: `${out}/7_dpr1.png` });
	// Its frame rate is held against a ratio-1 tab of the same 2560x1440 pixels, not a fixed 20 fps (r206):
	// SwiftShader fills in software, so both run at 17-22 fps with the machine's load, while a 640x360@2
	// tab runs as fast as a 1280x720 one (tools/browser-gate/fps_probe.mjs). A fixed floor measured the
	// renderer and the load; this measures what the ratio adds. Taken dpr2, 1x, 1x, dpr2 so a load that
	// rises or falls during the step weighs on both alike.
	const carriage = async (dpr) => {
		await page.setViewport({ width: 2560 / dpr, height: 1440 / dpr, deviceScaleFactor: dpr });
		await open(page, '?start=game&level=1');
		await waitVue(page, 'Vue_Game', 20000).catch(() => {});
		await sleep(2000);
		return fps(page, 4);
	};
	const dpr2 = [await carriage(2)], same = [await carriage(1), await carriage(1)];
	dpr2.push(await carriage(2));
	report.fpsDpr2 = Math.round((dpr2[0] + dpr2[1]) / 2);
	report.fps2560x1440 = Math.round((same[0] + same[1]) / 2);
	await page.screenshot({ path: `${out}/7_dpr2.png` });
	await fit(page, 1280, 720, 'dpr2', 2);
	say(`carriage 1 at devicePixelRatio 2: ${report.fpsDpr2} fps (${dpr2.join(', ')}); a 2560x1440 tab at 1: ${report.fps2560x1440} fps (${same.join(', ')})`);
	if (report.fpsDpr2 < 0.8 * report.fps2560x1440) fail(`${report.fpsDpr2} fps at devicePixelRatio 2, against ${report.fps2560x1440} for the same pixels at 1`);
	if (report.fpsDpr2 < 10) fail(`${report.fpsDpr2} fps at devicePixelRatio 2: under 10, the game is not drawing, whatever the load`);
	// Resized under it, the canvas keeps the ratio
	await fit(page, 1000, 800, 'dpr2_tall', 2);
	await page.setViewport({ width: 1280, height: 720, deviceScaleFactor: 1 });

	const errors = report.console.filter((l) => l.startsWith('pageerror') || l.startsWith('error'));
	if (errors.length) say(`console errors (not fatal):\n  ${errors.join('\n  ')}`);
} catch (e) {
	if (e.message !== 'stopped') fail(e.message);
} finally {
	report.failures = failures;
	writeFileSync(`${out}/gate.json`, JSON.stringify(report, null, 2));
	await browser.close();
	if (!failures.length) say('PASSED');
}
