// Bulletproof Lightweight HTTP Server for BOSS Web Launcher & APK Distribution
const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;

// Protect process from uncaught stream errors (e.g. client disconnect / EPIPE)
process.on('uncaughtException', (err) => {
    console.error('[BOSS Server] Caught exception:', err.message);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('[BOSS Server] Unhandled rejection:', reason);
});

// Prevent process from exiting if stdin closes
setInterval(() => {}, 60000);

const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.apk': 'application/vnd.android.package-archive',
    '.pdf': 'application/pdf',
    '.txt': 'text/plain; charset=utf-8'
};

function getApkFilePath(preferRelease = false) {
    const releaseCandidates = [
        '/app/applet/BOSS-app-release.apk',
        '/BOSS-app-release.apk',
        '/app/applet/website/BOSS-app-release.apk',
        '/app/applet/docs/BOSS-app-release.apk',
        '/app/applet/app/build/outputs/apk/release/app-release.apk',
        path.join(__dirname, 'BOSS-app-release.apk'),
        path.join(process.cwd(), 'BOSS-app-release.apk'),
        path.join(process.cwd(), 'website', 'BOSS-app-release.apk')
    ];
    if (preferRelease) {
        for (const p of releaseCandidates) {
            try {
                if (fs.existsSync(p) && fs.statSync(p).size > 1000000) {
                    return { path: p, filename: 'BOSS-app-release.apk' };
                }
            } catch (_) {}
        }
    }

    const debugCandidates = [
        '/app/applet/BOSS-app-debug.apk',
        '/BOSS-app-debug.apk',
        '/app/applet/website/BOSS-app-debug.apk',
        '/app/applet/docs/BOSS-app-debug.apk',
        '/app/applet/.build-outputs/app-debug.apk',
        path.join(__dirname, 'BOSS-app-debug.apk'),
        path.join(process.cwd(), 'BOSS-app-debug.apk'),
        path.join(process.cwd(), 'website', 'BOSS-app-debug.apk')
    ];
    for (const p of debugCandidates) {
        try {
            if (fs.existsSync(p) && fs.statSync(p).size > 1000000) {
                return { path: p, filename: 'BOSS-app-debug.apk' };
            }
        } catch (_) {}
    }

    // Fallback to release if debug candidates not found
    for (const p of releaseCandidates) {
        try {
            if (fs.existsSync(p) && fs.statSync(p).size > 1000000) {
                return { path: p, filename: 'BOSS-app-release.apk' };
            }
        } catch (_) {}
    }
    return null;
}

function getWebsiteDir() {
    const candidates = [
        '/app/applet/website',
        path.join(__dirname, 'website'),
        path.join(process.cwd(), 'website'),
        '/app/applet/docs',
        '/app/applet'
    ];
    for (const p of candidates) {
        try {
            if (fs.existsSync(p) && fs.statSync(p).isDirectory()) {
                return p;
            }
        } catch (_) {}
    }
    return '/app/applet/website';
}

const server = http.createServer((req, res) => {
    // Add CORS headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, HEAD, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', '*');

    if (req.method === 'OPTIONS') {
        res.writeHead(204);
        res.end();
        return;
    }

    const parsedUrl = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
    let pathname = decodeURIComponent(parsedUrl.pathname);

    // Health check
    if (pathname === '/health' || pathname === '/healthz') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ status: 'healthy', app: 'BOSS', port: PORT }));
        return;
    }

    // Direct APK Download routes
    if (
        pathname === '/BOSS-app-release.apk' ||
        pathname === '/website/BOSS-app-release.apk' ||
        pathname === '/docs/BOSS-app-release.apk' ||
        pathname === '/BOSS-app-debug.apk' ||
        pathname === '/download' ||
        pathname === '/api/download' ||
        pathname === '/website/BOSS-app-debug.apk' ||
        pathname === '/docs/BOSS-app-debug.apk' ||
        pathname.endsWith('.apk')
    ) {
        const isExplicitRelease = pathname.includes('release') || pathname === '/download' || pathname === '/api/download';
        const apkInfo = getApkFilePath(isExplicitRelease);
        if (apkInfo) {
            const stat = fs.statSync(apkInfo.path);
            res.writeHead(200, {
                'Content-Type': 'application/vnd.android.package-archive',
                'Content-Length': stat.size,
                'Content-Disposition': `attachment; filename="${apkInfo.filename}"`,
                'Cache-Control': 'public, max-age=3600',
                'Accept-Ranges': 'bytes'
            });

            const stream = fs.createReadStream(apkInfo.path);
            stream.on('error', (err) => {
                console.warn('[BOSS Server] APK stream error:', err.message);
                if (!res.headersSent) {
                    res.writeHead(500, { 'Content-Type': 'text/plain' });
                }
                res.end();
            });

            res.on('close', () => {
                stream.destroy();
            });

            res.on('error', (err) => {
                console.warn('[BOSS Server] Response error:', err.message);
                stream.destroy();
            });

            stream.pipe(res);
            return;
        } else {
            res.writeHead(404, { 'Content-Type': 'text/plain' });
            res.end('APK file is compiling or not found. Please check back in a few seconds.');
            return;
        }
    }

    // Normalize path for static files from website directory
    const websiteDir = getWebsiteDir();
    let filePath;
    if (pathname === '/' || pathname === '/index.html') {
        filePath = path.join(websiteDir, 'index.html');
    } else if (pathname === '/login' || pathname === '/login.html') {
        const primaryLogin = path.join(websiteDir, 'login.html');
        const fallbackLogin = path.join('/app/applet/docs', 'login.html');
        const cwdLogin = path.join(process.cwd(), 'docs', 'login.html');
        if (fs.existsSync(primaryLogin)) {
            filePath = primaryLogin;
        } else if (fs.existsSync(fallbackLogin)) {
            filePath = fallbackLogin;
        } else if (fs.existsSync(cwdLogin)) {
            filePath = cwdLogin;
        } else {
            filePath = primaryLogin;
        }
    } else {
        const candidate1 = path.join(websiteDir, pathname);
        const candidate2 = path.join('/app/applet', pathname);
        const candidateDocs = path.join('/app/applet/docs', pathname);

        if (fs.existsSync(candidate1) && fs.statSync(candidate1).isFile()) {
            filePath = candidate1;
        } else if (fs.existsSync(candidate2) && fs.statSync(candidate2).isFile()) {
            filePath = candidate2;
        } else if (fs.existsSync(candidateDocs) && fs.statSync(candidateDocs).isFile()) {
            filePath = candidateDocs;
        } else {
            filePath = path.join(websiteDir, 'index.html');
        }
    }

    if (!fs.existsSync(filePath)) {
        res.writeHead(404, { 'Content-Type': 'text/plain' });
        res.end('404 Not Found');
        return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';
    const stat = fs.statSync(filePath);

    res.writeHead(200, {
        'Content-Type': contentType,
        'Content-Length': stat.size,
        'Cache-Control': ext === '.html' ? 'no-cache' : 'public, max-age=86400'
    });

    const fileStream = fs.createReadStream(filePath);
    fileStream.on('error', (err) => {
        console.warn('[BOSS Server] File stream error:', err.message);
        if (!res.headersSent) {
            res.writeHead(500, { 'Content-Type': 'text/plain' });
        }
        res.end();
    });

    res.on('close', () => {
        fileStream.destroy();
    });

    res.on('error', (err) => {
        console.warn('[BOSS Server] Response error:', err.message);
        fileStream.destroy();
    });

    fileStream.pipe(res);
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.log(`[BOSS Web Server] Port ${PORT} is already in use by an active instance. Exiting redundant process cleanly.`);
        process.exit(0);
    } else {
        console.error('[BOSS Web Server] Server error:', err.message);
    }
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`[BOSS Web Server] Running on http://0.0.0.0:${PORT}`);
});
