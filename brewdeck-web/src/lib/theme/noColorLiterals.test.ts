import { readdirSync, readFileSync } from 'node:fs';
import { join, relative } from 'node:path';
import { describe, expect, it } from 'vitest';

// Components take colors from the theme palette, never raw values
// (docs/architecture/design-foundation-tdd.md, VR-003).
const SRC = join(__dirname, '..', '..');
const SCANNED_DIRS = ['components', 'app'];
const COLOR_LITERAL = /#[0-9a-f]{3,8}\b|rgba?\(|hsla?\(/i;

function sourceFiles(dir: string): string[] {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return sourceFiles(path);
    return /\.tsx?$/.test(entry.name) && !/\.test\.tsx?$/.test(entry.name) ? [path] : [];
  });
}

describe('color literals', () => {
  it('appear nowhere in components or app routes', () => {
    const offenders = SCANNED_DIRS.flatMap((dir) => sourceFiles(join(SRC, dir))).flatMap((file) =>
      readFileSync(file, 'utf8')
        .split('\n')
        .flatMap((line, index) =>
          COLOR_LITERAL.test(line) ? [`${relative(SRC, file)}:${index + 1}: ${line.trim()}`] : [],
        ),
    );

    expect(offenders).toEqual([]);
  });
});
