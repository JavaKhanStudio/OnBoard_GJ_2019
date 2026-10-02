// click_walk_probe.mjs <url> <outdir> — click-to-walk in the browser (r229): a fresh tab, so the
// config is the default one (FORCE_ON=1 ticks the option in it first), opens carriage 1, clicks the floor at x,y (CSS px of a 1280x720 tab;
// CLICKS="x,y x,y", default "900,520") and shoots right after each click and 4 s later.
// Headless Chrome, muted (D6). <url> is the page without its query.
import puppeteer from 'puppeteer-core';

const [url, out] = process.argv.slice(2);
const clicks = (process.env.CLICKS || '900,520').split(/\s+/).map((p) => p.split(',').map(Number));
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
	executablePath: process.env.CHROME || '/usr/bin/google-chrome',
	headless: true,
	args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--mute-audio', '--window-size=1280,720'],
	defaultViewport: { width: 1280, height: 720 },
});
const page = await browser.newPage();
page.on('pageerror', (e) => console.log('pageerror: ' + e.message));
const ready = () => page.waitForFunction(() => window.onboard && window.onboard.frames() > 30, { timeout: 90000, polling: 100 });

// First start writes the config; FORCE_ON=1 ticks clickToWalk in it, as the options box would.
await page.goto(url + '?start=start_screen&mute', { waitUntil: 'load' });
await ready();
const key = await page.evaluate(() => Object.keys(localStorage).find((k) => k.startsWith('onboard') && k.includes('config')));
const stored = await page.evaluate((k) => JSON.parse(localStorage.getItem(k)), key);
console.log(`config ${key}: clickToWalk ${stored.clickToWalk}`);
if (process.env.FORCE_ON) await page.evaluate((k) => {
	const c = JSON.parse(localStorage.getItem(k));
	c.clickToWalk = true;
	localStorage.setItem(k, JSON.stringify(c));
}, key);

await page.goto(url + '?start=game&level=1&mute', { waitUntil: 'load' });
await ready();
await sleep(2500);
await page.screenshot({ path: `${out}/0_start.png` });
let i = 1;
for (const [x, y] of clicks) {
	await page.mouse.move(x, y);
	await page.mouse.down(); await page.mouse.up();
	await sleep(150);
	await page.screenshot({ path: `${out}/${i}_click_${x}_${y}.png` });
	await sleep(4000);
	await page.screenshot({ path: `${out}/${i}_after_${x}_${y}.png` });
	console.log(`shot ${out}/${i}_click_${x}_${y}.png and ${i}_after`);
	i++;
}
await browser.close();
