// gate.mjs <url> <outdir> — the browser gate's driver, run by gate.sh (r137).
//
// Plays the page the way a player would, in HEADLESS Chrome with its audio muted (never a window
// or a sound on anyone's machine, D6): the logos must hand over to the intro by themselves, real
// mouse clicks must turn the intro's pages to the start screen, and a carriage must open. It drives
// the system Chrome through puppeteer-core, NOT `chrome --screenshot`, which pumps about five
// animation frames and then shoots a game that looks frozen (La chasse-galerie's
// docs/browser-target.md section 10). Screenshots and gate.json land in <outdir>.
//
// It fails when: the game does not run, the logos do not reach the intro, clicks do not reach the
// start screen, no music plays, a French letter has no glyph, the config is not kept in
// localStorage or not read back from it, the carriage does not open, or the loop runs under 20 fps.
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
	headless: !process.env.ONBOARD_INSIDE_CAGE,
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

async function fps(page) {
	const f0 = await probe(page, () => window.onboard.frames());
	await sleep(3000);
	return Math.round(((await probe(page, () => window.onboard.frames())) - f0) / 3);
}

try {
	const page = await browser.newPage();
	page.on('console', (m) => report.console.push(m.type() + ': ' + m.text()));
	page.on('pageerror', (e) => report.console.push('pageerror: ' + e.message));

	// 1. The logos, by themselves
	report.firstFramesMs = await open(page, '');
	say(`running after ${report.firstFramesMs} ms, on ${await vue(page)}`);
	await sleep(1500);
	await page.screenshot({ path: `${out}/1_logo.png` });
	report.logoMusic = await probe(page, () => window.onboard.music());
	try {
		await waitVue(page, 'Vue_Scenematic_Intro', 60000);
	} catch {
		fail(`the logos did not hand over to the intro in 60 s (still on ${await vue(page)})`);
		throw new Error('stopped');
	}
	await sleep(2500);
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

	// 6. A carriage: its backdrop is read from the .plaxpj, its music is an .ogg
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
