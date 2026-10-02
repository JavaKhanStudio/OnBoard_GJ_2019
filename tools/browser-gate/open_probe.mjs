// open_probe.mjs <url> <out.png> — open a page once, the way a person would, and say whether the game
// started (r222). Headless Chrome, muted (D6). Prints every console error and failed request, then
// whether window.onboard ran 30 frames within 40 s, and shoots what the tab shows.
//   node tools/browser-gate/open_probe.mjs file:///path/to/index.html out.png   # double-clicked
//   node tools/browser-gate/open_probe.mjs http://127.0.0.1:8795/ out.png       # served
import puppeteer from 'puppeteer-core';

const [url, out] = process.argv.slice(2);
const browser = await puppeteer.launch({
	executablePath: process.env.CHROME || '/usr/bin/google-chrome', headless: true,
	args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--mute-audio', '--window-size=1280,720'],
	defaultViewport: { width: 1280, height: 720 },
});
const page = await browser.newPage();
page.on('console', (m) => { if (m.type() === 'error') console.log('console: ' + m.text()); });
page.on('pageerror', (e) => console.log('pageerror: ' + e.message));
page.on('requestfailed', (r) => console.log('requestfailed: ' + r.url() + ' ' + (r.failure() || {}).errorText));
await page.goto(url, { waitUntil: 'load' });
let started = true;
try {
	await page.waitForFunction(() => window.onboard && window.onboard.frames() > 30, { timeout: 40000, polling: 200 });
} catch { started = false; }
await page.screenshot({ path: out });
console.log('open_probe: ' + (started ? 'STARTED' : 'DID NOT START') + ' — ' + url + ' -> ' + out);
await browser.close();
process.exitCode = started ? 0 : 1;
