/**
 * Pure Node.js Android APK Build Engine
 * Zero PowerShell required. Works on standard Windows CMD / Node.js runtime.
 */

const https = require('https');
const fs = require('fs');
const path = require('path');
const { spawn, execSync } = require('child_process');

const GRADLE_VERSION = '8.7';
const GRADLE_DIR = path.join(__dirname, '.gradle_bin');
const GRADLE_HOME = path.join(GRADLE_DIR, `gradle-${GRADLE_VERSION}`);
const GRADLE_BAT = path.join(GRADLE_HOME, 'bin', 'gradle.bat');
const ZIP_FILE = path.join(GRADLE_DIR, `gradle-${GRADLE_VERSION}-bin.zip`);
const DOWNLOAD_URL = `https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip`;

console.log('========================================================');
console.log('  ⚡ Building Android APK for P2P Drop (Node.js Engine)  ');
console.log('========================================================');

// Step 1: Download helper with redirects support
function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        return downloadFile(res.headers.location, dest).then(resolve).catch(reject);
      }
      if (res.statusCode !== 200) {
        return reject(new Error(`Failed to download: HTTP ${res.statusCode}`));
      }

      const totalBytes = parseInt(res.headers['content-length'] || '0', 10);
      let downloaded = 0;
      const fileStream = fs.createWriteStream(dest);

      res.on('data', (chunk) => {
        downloaded += chunk.length;
        if (totalBytes > 0) {
          const pct = Math.round((downloaded / totalBytes) * 100);
          process.stdout.write(`\r[*] Downloading Gradle ${GRADLE_VERSION}... ${pct}% (${(downloaded / (1024 * 1024)).toFixed(1)} MB / ${(totalBytes / (1024 * 1024)).toFixed(1)} MB)`);
        } else {
          process.stdout.write(`\r[*] Downloading Gradle ${GRADLE_VERSION}... ${(downloaded / (1024 * 1024)).toFixed(1)} MB`);
        }
      });

      res.pipe(fileStream);
      fileStream.on('finish', () => {
        fileStream.close();
        console.log('\n[+] Download complete!');
        resolve();
      });
    }).on('error', (err) => {
      fs.unlink(dest, () => {});
      reject(err);
    });
  });
}

// Step 2: Extract Zip using built-in Windows tar or powershell fallback if any
function extractZip(zipPath, targetDir) {
  console.log('[*] Extracting Gradle distribution...');
  try {
    // Windows 10/11 built-in tar supports zip extraction
    execSync(`tar -xf "${zipPath}" -C "${targetDir}"`, { stdio: 'inherit' });
  } catch (err) {
    // Fallback using Windows native VBScript/Explorer zip uncompress
    const scriptPath = path.join(targetDir, 'unzip.vbs');
    const vbs = `
      Set fso = CreateObject("Scripting.FileSystemObject")
      Set app = CreateObject("Shell.Application")
      Set zip = app.NameSpace("${zipPath.replace(/\\/g, '\\\\')}")
      Set out = app.NameSpace("${targetDir.replace(/\\/g, '\\\\')}")
      out.CopyHere zip.Items, 16
    `;
    fs.writeFileSync(scriptPath, vbs, 'utf8');
    execSync(`cscript //nologo "${scriptPath}"`, { stdio: 'inherit' });
    try { fs.unlinkSync(scriptPath); } catch (e) {}
  }
}

async function main() {
  if (!fs.existsSync(GRADLE_DIR)) {
    fs.mkdirSync(GRADLE_DIR, { recursive: true });
  }

  // Check if Gradle already extracted
  if (!fs.existsSync(GRADLE_BAT)) {
    if (!fs.existsSync(ZIP_FILE)) {
      console.log(`[*] Downloading Gradle from ${DOWNLOAD_URL}`);
      await downloadFile(DOWNLOAD_URL, ZIP_FILE);
    }
    extractZip(ZIP_FILE, GRADLE_DIR);
    try { fs.unlinkSync(ZIP_FILE); } catch (e) {}
  }

  if (!fs.existsSync(GRADLE_BAT)) {
    console.error(`[-] Error: Could not locate gradle.bat at ${GRADLE_BAT}`);
    process.exit(1);
  }

  console.log('[*] Executing Gradle assembleDebug to compile APK...');
  console.log('--------------------------------------------------------');

  const gradleProcess = spawn(GRADLE_BAT, ['assembleDebug', '--no-daemon'], {
    cwd: __dirname,
    stdio: 'inherit',
    shell: true
  });

  gradleProcess.on('close', (code) => {
    const apkPath = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
    console.log('--------------------------------------------------------');
    if (fs.existsSync(apkPath)) {
      console.log('\n========================================================');
      console.log('  ✅ SUCCESS! Android APK Generated Successfully!       ');
      console.log('========================================================');
      console.log(`  APK File: ${apkPath}`);
      console.log('========================================================\n');
    } else {
      console.log(`[*] Process finished with code ${code}. Check the logs above.`);
    }
  });
}

main().catch((err) => {
  console.error('[-] Build script error:', err.message);
});
