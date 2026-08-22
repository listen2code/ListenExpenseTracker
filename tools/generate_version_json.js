const fs = require('fs');
const path = require('path');

function main() {
  const gradleFilePath = path.join(__dirname, '..', 'app', 'build.gradle.kts');
  if (!fs.existsSync(gradleFilePath)) {
    console.error('Error: app/build.gradle.kts not found.');
    process.exit(1);
  }

  const content = fs.readFileSync(gradleFilePath, 'utf8');
  const vNameMatch = content.match(/val\s+vName\s*=\s*["']([^"']+)["']/);

  if (!vNameMatch) {
    console.error('Error: Could not parse vName from app/build.gradle.kts');
    process.exit(1);
  }

  const version = vNameMatch[1];
  const parts = version.split('.');
  const major = parseInt(parts[0] || '0', 10);
  const minor = parseInt(parts[1] || '0', 10);
  const patch = parseInt((parts[2] || '0').replace(/\D/g, ''), 10);
  const buildNumber = major * 10000 + minor * 100 + patch;

  const pagesDir = path.join(__dirname, '..', 'pages');
  if (!fs.existsSync(pagesDir)) {
    fs.mkdirSync(pagesDir, { recursive: true });
  }

  const pagesVersionJsonPath = path.join(pagesDir, 'version.json');
  let data = {
    version: version,
    buildNumber: buildNumber,
    url: 'https://play.google.com/store/apps/details?id=com.listen.expensetracker',
    changelog: {
      zh: `版本 ${version} 发布更新`,
      en: `Update to version ${version}`,
      ja: `バージョン ${version} へのアップデート`
    }
  };

  if (fs.existsSync(pagesVersionJsonPath)) {
    try {
      const existing = JSON.parse(fs.readFileSync(pagesVersionJsonPath, 'utf8'));
      data.url = existing.url || data.url;
      if (existing.changelog) {
        data.changelog = { ...data.changelog, ...existing.changelog };
      }
    } catch (_) {}
  }

  fs.writeFileSync(pagesVersionJsonPath, JSON.stringify(data, null, 2) + '\n', 'utf8');
  console.log(`Successfully generated version.json at pages/ with version: ${version} (${buildNumber})`);
}

main();
