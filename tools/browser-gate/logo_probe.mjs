// logo_probe.mjs <url> <outdir> — what logo the browser build shows before the game draws (r165).
//
// Two places carry one: the tab's icon (the page's <link rel="icon">, or /favicon.ico when it sets
// none — and jwebserver, which serves the page, answers that with Java's Duke), and the preloader
// GWT draws while the 33 MB of assets load. Headless Chrome has no tab strip, so the probe fetches
// the icon the page resolves to and draws it into a mock tab (tab.png); the preloader is shot with
// the network throttled so it stays up long enough (preloader.png, and preloader_phone.png at 390
// wide). Headless and muted, like gate.mjs (D6).
//
//   node tools/browser-gate/logo_probe.mjs http://127.0.0.1:<port>/index.html <outdir>
import puppeteer from 'puppeteer-core';
import { writeFileSync } from 'node:fs';

const [url, out] = process.argv.slice(2);
const browser = await puppeteer.launch({
	executablePath: process.env.CHROME || '/usr/bin/google-chrome',
	headless: true,
	args: ['--use-angle=swiftshader', '--enable-unsafe-swiftshader', '--mute-audio', '--no-first-run'],
});
const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function preloader(name, viewport) {
	const page = await browser.newPage();
	await page.setViewport(viewport);
	const cdp = await page.createCDPSession();
	// ~2 MB/s: the preloader stays up for several seconds instead of a blink
	await cdp.send('Network.emulateNetworkConditions', { offline: false, latency: 20, downloadThroughput: 2e6, uploadThroughput: 1e6 });
	await page.goto(url + '?mute', { waitUntil: 'domcontentloaded' });
	await page.waitForSelector('.gdx-preloader img', { timeout: 30000 });
	await page.waitForFunction(() => { const i = document.querySelector('.gdx-preloader img'); return i.complete && i.naturalWidth > 0; }, { timeout: 30000 });
	await sleep(1500);
	const logo = await page.evaluate(() => { const i = document.querySelector('.gdx-preloader img'); return { src: i.src, natural: [i.naturalWidth, i.naturalHeight], shown: [i.width, i.height] }; });
	await page.screenshot({ path: `${out}/${name}.png` });
	await page.close();
	return logo;
}

const report = { url };
report.preloader = await preloader('preloader', { width: 1280, height: 720 });
report.preloader_phone = await preloader('preloader_phone', { width: 390, height: 844, isMobile: true, hasTouch: true, deviceScaleFactor: 2 });

// The icon the tab would wear: the page's own link, else the origin's /favicon.ico
const page = await browser.newPage();
await page.goto(url + '?mute', { waitUntil: 'domcontentloaded' });
report.icon = await page.evaluate(async () => {
	const link = document.querySelector('link[rel~="icon"]');
	const href = link ? link.href : new URL('/favicon.ico', location.href).href;
	const res = await fetch(href);
	const blob = await res.blob();
	const data = await new Promise((r) => { const f = new FileReader(); f.onload = () => r(f.result); f.readAsDataURL(blob); });
	return { href, fromLink: !!link, status: res.status, type: res.headers.get('content-type'), bytes: blob.size, data, title: document.title };
});
await page.setViewport({ width: 360, height: 60, deviceScaleFactor: 2 });
await page.setContent(`<body style="margin:0;background:#dee1e6;font:13px sans-serif">
	<div style="margin:8px 0 0 8px;width:240px;height:36px;background:#fff;border-radius:8px 8px 0 0;display:flex;align-items:center;gap:8px;padding:0 12px">
	<img src="${report.icon.data}" style="width:16px;height:16px"><span>${report.icon.title}</span></div>
	<img src="${report.icon.data}" style="position:absolute;right:8px;top:6px;width:48px;height:48px"></body>`);
await sleep(200);
await page.screenshot({ path: `${out}/tab.png` });
delete report.icon.data;

writeFileSync(`${out}/logo_probe.json`, JSON.stringify(report, null, 2));
console.log(JSON.stringify(report, null, 2));
await browser.close();
