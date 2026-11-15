// Genera src/test/resources/paridad/terreno.txt: el terreno del prototipo para varias semillas y biomas.
// La prueba TerrenoParidadTest comprueba que core/juego/Terreno da lo mismo. Uso: node tools/paridad/gen_terreno.js
const fs = require('fs'), path = require('path');
const {P, set} = require('./harness');
const CASES = [];
const BIOMAS = [['oceano', 'oceano', 0], ['calido', 'calido', 0], ['helado', 'helado', 0], ['rio', 'rio', 0], ['pantano', 'pantano', 0],
  ['lava', 'deltas', 'deltas'], ['lava', 'almas', 'almas'], ['lava', 'carmesi', 'carmesi'], ['lava', 'distorsionado', 'distorsionado'],
  ['lava', 'yermos', 'yermos'], ['vacio', 'vacio', 0]];
const VARIANTES = [{seed: '12345', water: 120, base: 0.8, vh: 80}, {seed: '987654321', water: 120, base: 1.04, vh: 116},
  {seed: '4000000000', water: 70, base: 0.56, vh: 76}, {seed: '77', water: 34, base: 0.8, vh: 80}];
const f = (x) => (Number.isFinite(x) ? +x.toFixed(6) : x);
const out = [];
for (const [biome, perfil, sub] of BIOMAS) for (const v of VARIANTES) {
  set({biome, sub: sub || 'yermos', water: v.water, base: v.base, vh: v.vh, k: 0.04, seed: v.seed});
  P.startCast();
  const s = P.s, maxD = s.run.maxD, magma = ['oceano', 'calido', 'rio', 'pantano'].includes(biome);
  out.push(['CASE', perfil, magma ? 1 : 0, s.seed | 0, maxD, v.base, v.vh].join(' '));
  for (const sec of s.run.sections) {
    out.push(['SEC', f(sec.y0), f(sec.y1), f(sec.cw), f(sec.ex), f(sec.xx), f(sec.fi), f(sec.fo), sec.paths.length, sec.pil.length].join(' '));
  }
  const end = (Number.isFinite(maxD) ? maxD : 140) * P.PPB;
  for (let y = -40; y <= end; y += 13) {
    const iv = P.spansAt(y);
    out.push(['Y', y, iv.length, ...iv.flatMap((q) => [f(q[0]), f(q[1])])].join(' '));
  }
}
const file = path.join(__dirname, '..', '..', 'src', 'test', 'resources', 'paridad', 'terreno.txt');
fs.mkdirSync(path.dirname(file), {recursive: true});
fs.writeFileSync(file, out.join('\n') + '\n');
console.log(out.filter((l) => l.startsWith('CASE')).length + ' casos, ' + out.filter((l) => l.startsWith('SEC')).length + ' cuevas, ' + out.length + ' líneas');
