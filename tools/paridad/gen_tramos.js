// Genera src/test/resources/paridad/tramos.txt: lo que genChunk del prototipo pone en cada tramo (rocas, peligros,
// franjas, peces y tesoros) para varios biomas, climas y semillas. TramosParidadTest comprueba que core/juego/Partida
// genera lo mismo. Uso: node tools/paridad/gen_tramos.js
const fs = require('fs'), path = require('path');
const {P, set} = require('./harness');
const BIOMAS = [['oceano', 0], ['calido', 0], ['helado', 0], ['rio', 0], ['pantano', 0], ['lava', 'deltas'], ['lava', 'almas'],
  ['lava', 'carmesi'], ['lava', 'distorsionado'], ['lava', 'yermos'], ['vacio', 0]];
const AGUA = ['oceano', 'calido', 'helado', 'rio', 'pantano'];
const f = (x) => +(+x).toFixed(5);
const HAZ = {
  bub: (h) => [h.x, h.y, h.r, h.v, h.met ? 1 : 0, 0], drift: (h) => [h.x0, h.y, h.amp, h.ph, h.sp, h.r], net: (h) => [h.x0, h.y, h.ph, 0, 0, 0],
  seek: (h) => [h.x, h.y, h.r, h.bul ? 1 : 0, 0, 0], cross: (h) => [h.x, h.y, h.dir, h.v, h.ghast ? 1 : 0, 0], gey: (h) => [h.x, h.y, h.dx, h.dy, h.range, h.tm],
  col: (h) => [h.x, h.y, h.hh, h.tm, h.chim ? 1 : 0, 0], fall: (h) => [h.x, h.y, 0, 0, 0, 0], lion: (h) => [h.x, h.y, h.tm, 0, 0, 0],
  lunge: (h) => [h.wx, h.y, h.dir, h.range, h.tm, h.stay ? 1 : 0],
};
const out = [];
let n = 0;
for (const [biome, sub] of BIOMAS) for (const weather of AGUA.includes(biome) ? ['despejado', 'lluvia', 'tormenta'] : ['despejado']) {
  for (const v of [{seed: '12345', luck: 0, water: 120}, {seed: '4000000000', luck: 2, water: 75}]) {
    set({biome, sub: sub || 'yermos', weather, water: v.water, luck: v.luck, lure: 0, base: 0.8, vh: 80, k: 0.04, seed: v.seed});
    P.startCast();
    const s = P.s, maxD = s.run.maxD, last = Math.floor((Number.isFinite(maxD) ? maxD : 700) * P.PPB / P.CH);
    out.push(['CASE', biome, sub || 'yermos', weather, s.seed | 0, maxD, v.luck].join(' '));
    for (let i = 0; i <= last; i++) {
      const c = P.genChunk(i);
      out.push('T ' + i);
      for (const o of c.rocks) out.push(['R', o.kind, f(o.x), f(o.y), f(o.r), o.s].join(' '));
      for (const h of c.haz) out.push(['Z', h.k, ...HAZ[h.k](h).map(f)].join(' '));
      for (const o of c.cur) out.push(['F', o.fx, f(o.y), f(o.h), o.dir, f(o.f), o.m].join(' '));
      for (const it of c.items) out.push(['I', it.id, f(it.x), f(it.y), f(it.vx), f(it.ph), f(it.wait), it.hid ? 1 : 0].join(' '));
      n += c.rocks.length + c.haz.length + c.cur.length + c.items.length;
    }
  }
}
const file = path.join(__dirname, '..', '..', 'src', 'test', 'resources', 'paridad', 'tramos.txt');
fs.writeFileSync(file, out.join('\n') + '\n');
console.log(out.filter((l) => l.startsWith('CASE')).length + ' casos, ' + n + ' elementos, ' + out.length + ' líneas');
