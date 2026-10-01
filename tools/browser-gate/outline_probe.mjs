// outline_probe.mjs <url> <outdir> [x,y ...] — shoots a carriage in headless Chrome with the mouse
// over each x,y (CSS pixels of a 1280x720 tab), to see an item's outline on WebGL (r217);
// c<x>,<y> clicks there instead and shoots the next second.
// DPR=2 makes it a Retina-like tab; BROWSER=firefox runs Firefox (under tools/offscreen.sh). Muted and headless (D6). <url> is the page with its query, e.g. .../index.html?start=game&level=1
import puppeteer from 'puppeteer-core';

const [url, out, ...points] = process.argv.slice(2);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const firefox = process.env.BROWSER === 'firefox';
const dpr = +(process.env.DPR || 1);
// Firefox has no headless WebGL here: run it inside tools/offscreen.sh (cage), as gate.sh does
const browser = await puppeteer.launch(firefox ? {
	browser: 'firefox',
	executablePath: process.env.FIREFOX || '/usr/bin/firefox',
	headless: !process.env.ATELIER_INSIDE_CAGE,
	extraPrefsFirefox: { 'media.volume_scale': '0.0', 'webgl.force-enabled': true },
	defaultViewport: { width: 1280, height: 720, deviceScaleFactor: dpr },
} : {
	executablePath: process.env.CHROME || '/usr/bin/google-chrome',
	// GPU=1, under tools/offscreen.sh: a Chrome window in cage on the machine's GPU, not SwiftShader
	headless: !process.env.GPU,
	args: [...(process.env.GPU ? ['--ozone-platform=wayland', '--ignore-gpu-blocklist'] : ['--use-angle=swiftshader', '--enable-unsafe-swiftshader']), '--autoplay-policy=no-user-gesture-required',
		'--mute-audio', '--window-size=1280,720', '--no-first-run', '--no-default-browser-check'],
	defaultViewport: { width: 1280, height: 720, deviceScaleFactor: dpr },
});
const page = await browser.newPage();
page.on('console', (m) => console.log('console: ' + m.text()));
page.on('pageerror', (e) => console.log('pageerror: ' + e.message));
await page.goto(url, { waitUntil: 'load' });
console.log('webgl: ' + await page.evaluate(() => { const g = document.createElement('canvas').getContext('webgl'); const d = g && g.getExtension('WEBGL_debug_renderer_info'); return d ? g.getParameter(d.UNMASKED_RENDERER_WEBGL) : String(g); }));
await page.waitForFunction(() => window.onboard && window.onboard.frames() > 30, { timeout: 90000, polling: 100 });
await sleep(1500);
await page.screenshot({ path: `${out}/none.png` });
for (const p of points) {
	// c<x>,<y> clicks there and shoots ten frames 100 ms apart: the key's flash after a pickup
	const click = p.startsWith('c');
	const [x, y] = p.replace(/^c/, '').split(',').map(Number);
	await page.mouse.move(x, y);
	if (click) {
		await page.mouse.down(); await page.mouse.up();
		for (let i = 0; i < 10; i++) {
			await page.screenshot({ path: `${out}/click_${x}_${y}_${i}.png` });
			await sleep(100);
		}
		console.log(`shot ${out}/click_${x}_${y}_0..9.png`);
		continue;
	}
	await sleep(800);
	await page.screenshot({ path: `${out}/at_${x}_${y}.png` });
	console.log(`shot ${out}/at_${x}_${y}.png`);
}
await browser.close();
