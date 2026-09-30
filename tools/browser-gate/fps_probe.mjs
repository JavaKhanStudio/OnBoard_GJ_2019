// fps_probe.mjs <url> <width>x<height>[@dpr]... — carriage 1's frame rate in headless Chrome (SwiftShader,
// muted, D6) for each tab size given, r184. It tells the fill cost from the devicePixelRatio: a 1280x720@2
// tab and a 2560x1440 one draw the same 2560x1440 canvas. Serve html/build/war first (gate.sh does).
import puppeteer from 'puppeteer-core';

const [url, ...tabs] = process.argv.slice(2);
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));
const browser = await puppeteer.launch({
	executablePath: process.env.CHROME || '/usr/bin/google-chrome',
	headless: true,
	args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--autoplay-policy=no-user-gesture-required',
		'--mute-audio', '--no-first-run', '--no-default-browser-check'],
});
try {
	const page = await browser.newPage();
	for (const tab of tabs) {
		const [, width, height, dpr] = tab.match(/^(\d+)x(\d+)(?:@([\d.]+))?$/);
		await page.setViewport({ width: +width, height: +height, deviceScaleFactor: +(dpr || 1) });
		await page.goto(url + '?mute&start=game&level=1', { waitUntil: 'load' });
		await page.waitForFunction(() => window.onboard && window.onboard.vue() === 'Vue_Game' && window.onboard.frames() > 30,
			{ timeout: 90000, polling: 100 });
		await sleep(2000);
		const f0 = await page.evaluate(() => window.onboard.frames());
		await sleep(5000);
		const fps = Math.round(((await page.evaluate(() => window.onboard.frames())) - f0) / 5);
		console.log(`${tab}: game ${await page.evaluate(() => window.onboard.size())}, ${fps} fps`);
	}
} finally {
	await browser.close();
}
