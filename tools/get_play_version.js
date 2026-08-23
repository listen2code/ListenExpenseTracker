const crypto = require('crypto');
const https = require('https');

const serviceAccountJson = process.env.PLAY_SERVICE_ACCOUNT_KEY;
const packageName = 'com.listen.expensetracker';
const trackName = 'internal';

if (!serviceAccountJson) {
  console.log("0");
  process.exit(0);
}

let serviceAccount;
try {
  serviceAccount = JSON.parse(serviceAccountJson);
} catch (e) {
  console.log("0");
  process.exit(0);
}

function request(options, postData) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(data ? JSON.parse(data) : {});
        } else {
          reject(new Error(`Status ${res.statusCode}: ${data}`));
        }
      });
    });
    req.on('error', reject);
    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

async function getAccessToken() {
  const header = { alg: 'RS256', typ: 'JWT' };
  const now = Math.floor(Date.now() / 1000);
  const claim = {
    iss: serviceAccount.client_email,
    scope: 'https://www.googleapis.com/auth/androidpublisher',
    aud: 'https://oauth2.googleapis.com/token',
    exp: now + 3600,
    iat: now
  };

  const base64UrlEncode = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64url');
  const jwtInput = `${base64UrlEncode(header)}.${base64UrlEncode(claim)}`;

  const sign = crypto.createSign('RSA-SHA256');
  sign.update(jwtInput);
  const signature = sign.sign(serviceAccount.private_key, 'base64url');
  const assertion = `${jwtInput}.${signature}`;

  const postData = `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${assertion}`;
  const response = await request({
    hostname: 'oauth2.googleapis.com',
    path: '/token',
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded',
      'Content-Length': Buffer.byteLength(postData)
    }
  }, postData);

  return response.access_token;
}

async function main() {
  try {
    const token = await getAccessToken();
    const headers = {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    };

    // 1. Create a new edit
    const edit = await request({
      hostname: 'androidpublisher.googleapis.com',
      path: `/androidpublisher/v3/applications/${packageName}/edits`,
      method: 'POST',
      headers: headers
    }, '{}');

    const editId = edit.id;

    // 2. Fetch the track
    let track;
    try {
      track = await request({
        hostname: 'androidpublisher.googleapis.com',
        path: `/androidpublisher/v3/applications/${packageName}/edits/${editId}/tracks/${trackName}`,
        method: 'GET',
        headers: headers
      });
    } catch (e) {
      // If track is empty/uninitialized, output 0 and discard edit
      console.log("0");
      try {
        await request({
          hostname: 'androidpublisher.googleapis.com',
          path: `/androidpublisher/v3/applications/${packageName}/edits/${editId}`,
          method: 'DELETE',
          headers: headers
        });
      } catch (_) {}
      return;
    }

    // Discard edit
    try {
      await request({
        hostname: 'androidpublisher.googleapis.com',
        path: `/androidpublisher/v3/applications/${packageName}/edits/${editId}`,
        method: 'DELETE',
        headers: headers
      });
    } catch (_) {}

    // 3. Extract highest version code from track releases
    let latestVersionCode = 0;
    if (track && track.releases) {
      for (const release of track.releases) {
        if (release.versionCodes) {
          for (const vc of release.versionCodes) {
            const num = parseInt(vc, 10);
            if (num > latestVersionCode) {
              latestVersionCode = num;
            }
          }
        }
      }
    }

    console.log(latestVersionCode);
  } catch (error) {
    // If application not created yet on play console or other error, fallback to 0
    console.log("0");
  }
}

main();
