# serve.ps1 — the drive's browser version on Windows (atelier r222). Serves the folder it sits in on
# http://127.0.0.1:47219/ and opens it in the browser. A browser refuses to run the game from a disk
# (file://), so double-clicking index.html can never work; a web server, even this one, can.
# Windows has no Python or Perl but always has PowerShell 5.1: this is written for 5.1 (no ?? or ?:)
# and is tried with pwsh 7 on Linux. A raw TcpListener, not HttpListener: nothing to register, no admin.
# One thread, polled: a browser opens connections it never asks anything on, and a loop that
# waited on one would hang the page. serve.pl is the same thing for Mac and Linux.
# The port is FIXED: the game keeps its options in localStorage, which belongs to the port.
$ErrorActionPreference = 'Stop'
$root = [System.IO.Path]::GetFullPath($PSScriptRoot)
$types = @{ html = 'text/html; charset=utf-8'; js = 'text/javascript'; css = 'text/css'
	png = 'image/png'; jpg = 'image/jpeg'; gif = 'image/gif'; ico = 'image/x-icon'
	mp3 = 'audio/mpeg'; ttf = 'font/ttf'; json = 'application/json'; txt = 'text/plain; charset=utf-8' }

$listener = $null
foreach ($p in 47219..47228) {
	try {
		$l = New-Object System.Net.Sockets.TcpListener ([System.Net.IPAddress]::Loopback), $p
		$l.Start(); $listener = $l; $port = $p; break
	} catch { }
}
if (-not $listener) { Write-Host 'On Board: no free port from 47219 to 47228'; exit 1 }

$url = "http://127.0.0.1:$port/"
Write-Host "On Board : $url"
Write-Host ''
Write-Host 'Laissez cette fenêtre ouverte pendant que vous jouez. Fermez-la pour arrêter.'
Write-Host 'Leave this window open while you play. Close it to stop.'
if (-not $env:ONBOARD_NO_BROWSER) { Start-Process $url }

function Send($stream, $code, $type, [byte[]]$body, $head) {
	$reason = 'No'; if ($code -eq 200) { $reason = 'OK' }
	$h = "HTTP/1.1 $code $reason`r`nContent-Type: $type`r`nContent-Length: $($body.Length)`r`nCache-Control: no-cache`r`nConnection: close`r`n`r`n"
	$hb = [System.Text.Encoding]::ASCII.GetBytes($h)
	$stream.Write($hb, 0, $hb.Length)
	if (-not $head -and $body.Length) { $stream.Write($body, 0, $body.Length) }
}

function Answer($client, [string]$request) {
	$stream = $client.GetStream()
	$none = [byte[]]@()
	try {
		$first = ($request -split "`r?`n")[0]
		if ($first -notmatch '^(GET|HEAD) (\S+)') { Send $stream 405 'text/plain' $none $false; return }
		$head = $Matches[1] -eq 'HEAD'
		$path = [System.Uri]::UnescapeDataString(($Matches[2] -split '[?#]')[0])
		if ($path.EndsWith('/')) { $path += 'index.html' }
		$file = [System.IO.Path]::GetFullPath((Join-Path $root $path.TrimStart('/')))
		if (-not $file.StartsWith($root + [System.IO.Path]::DirectorySeparatorChar) -or -not [System.IO.File]::Exists($file)) {
			Send $stream 404 'text/plain' $none $head; return
		}
		$ext = [System.IO.Path]::GetExtension($file).TrimStart('.').ToLower()
		$type = $types[$ext]; if (-not $type) { $type = 'application/octet-stream' }
		Send $stream 200 $type ([System.IO.File]::ReadAllBytes($file)) $head
	} catch { } finally { $client.Close() }
}

# Each waiting connection: its client, what it has sent so far, and when it opened.
$waiting = New-Object System.Collections.ArrayList
$buffer = New-Object byte[] 65536
while ($true) {
	while ($listener.Pending()) {
		[void]$waiting.Add(@{ client = $listener.AcceptTcpClient(); text = ''; since = [DateTime]::Now })
	}
	foreach ($w in @($waiting)) {
		$c = $w.client
		try {
			if ($c.Available -gt 0) {
				$n = $c.GetStream().Read($buffer, 0, $buffer.Length)
				$w.text += [System.Text.Encoding]::ASCII.GetString($buffer, 0, $n)
			}
			if ($w.text.Contains("`r`n`r`n")) { $waiting.Remove($w); Answer $c $w.text }
			elseif (([DateTime]::Now - $w.since).TotalSeconds -gt 30) { $waiting.Remove($w); $c.Close() }
		} catch { $waiting.Remove($w); $c.Close() }
	}
	if ($waiting.Count -eq 0 -and -not $listener.Pending()) { Start-Sleep -Milliseconds 10 }
}
