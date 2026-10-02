// options_probe.mjs <url> <out.png> — the options screen of the browser build (r238), in a tab of
// W x H CSS pixels (default 1280x720) at devicePixelRatio DPR (default 1): opens the start screen,
// clicks Options on the menu and shoots the tab (MENU=menu.png shoots the menu first; RESIZE, below, resizes the tab once they are open; BACK=1 then
// goes back to the menu; MENU_ONLY=1 never opens the options, for a resize under the menu).
// Headless Chrome, muted (D6). <url> is the page without its query.
import puppeteer from 'puppeteer-core';

const [url, out] = process.argv.slice(2);
const width = Number(process.env.W || 1280), height = Number(process.env.H || 720);
const dpr = Number(process.env.DPR || 1);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
	executablePath: process.env.CHROME || '/usr/bin/google-chrome', headless: true,
	args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--mute-audio', `--window-size=${width},${height}`],
	defaultViewport: { width, height, deviceScaleFactor: dpr },
});
const page = await browser.newPage();
page.on('pageerror', (e) => console.log('pageerror: ' + (process.env.STACK ? e.stack : e.message)));
await page.goto(url + '?start=start_screen&mute', { waitUntil: 'load' });
await page.waitForFunction(() => window.onboard && window.onboard.frames() > 30, { timeout: 90000, polling: 100 });
await sleep(3000);
if (process.env.MENU) await page.screenshot({ path: process.env.MENU });
// The menu is a column at the left: Options, its second entry, sits at 0.12 x, 0.63 y of the canvas.
const box = await (await page.$('canvas')).boundingBox();
// FLOOR=1 first clicks the bare start screen, which no widget takes (r242): it must raise no pageerror.
if (process.env.FLOOR) { await page.mouse.click(box.x + box.width * 0.6, box.y + box.height * 0.8); await sleep(500); }
if (!process.env.MENU_ONLY) await page.mouse.click(box.x + box.width * 0.12, box.y + box.height * 0.63);
await sleep(2000);
// RESIZE=WxH[@dpr] resizes the tab while the options are open, as a window dragged or a page zoomed (r238).
if (process.env.RESIZE) {
	const [size, ratio] = process.env.RESIZE.split('@');
	const [w, h] = size.split('x').map(Number);
	await page.setViewport({ width: w, height: h, deviceScaleFactor: Number(ratio || dpr) });
	await sleep(1500);
}
// KEYS="ArrowDown Enter" presses those keys, in order, on the options (r243: the keyboard walks every row).
for (const key of (process.env.KEYS || '').split(/\s+/).filter(Boolean)) { await page.keyboard.press(key); await sleep(300); }
// BACK=1 presses Retour (Escape) after that, so the shot is the menu coming back.
if (process.env.BACK) { await page.keyboard.press('Escape'); await sleep(2500); }
await page.screenshot({ path: out });
console.log(`options_probe: ${width}x${height} dpr ${dpr}, game draws at ${await page.evaluate(() => window.onboard.size())} -> ${out}`);
await browser.close();
