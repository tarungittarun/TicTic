// Observe the emulator action's bootstrap as well as the app script; no retry or altered exit status.
const fs = require('node:fs');
const path = require('node:path');
const directory = path.join(process.cwd(), 'evidence');
fs.mkdirSync(directory, {recursive: true});
const fd = fs.openSync(path.join(directory, 'emulator-bootstrap.log'), 'a');
let remaining = 4 * 1024 * 1024;
for (const stream of [process.stdout, process.stderr]) {
  const original = stream.write;
  stream.write = function(chunk, ...args) {
    const bytes = Buffer.isBuffer(chunk) ? chunk : Buffer.from(String(chunk));
    if (remaining > 0) {const kept = bytes.subarray(0, remaining); fs.writeSync(fd, kept); remaining -= kept.length;}
    return original.call(this, chunk, ...args);
  };
}
