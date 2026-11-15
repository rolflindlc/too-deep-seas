// Vuelca los 100 peces del prototipo (ITEMS) al datapack: src/main/resources/data/toodeepseas/fish/<id>.json
// Uso: node tools/paridad/gen_peces.js
const fs = require('fs'), path = require('path');
const {P} = require('./harness');
const CEBOS = {'carne podrida aderezada': 'carne_podrida_aderezada', 'cobre cargado': 'cebo_cobre_cargado', 'tinta brillante': 'cebo_tinta_brillante',
  'gamba de arrecife': 'gamba_arrecife', 'helado': 'cebo_helado', 'baya luminosa': 'baya_luminosa', 'crema de magma cebada': 'crema_magma_cebada',
  'hongo carmesí cebado': 'hongo_carmesi_cebado', 'hongo distorsionado cebado': 'hongo_distorsionado_cebado', 'verruga cebada': 'verruga_cebada'};
const slug = (n) => n.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().replace(/[^a-z0-9]+/g, '_').replace(/^_|_$/g, '');
function hsl(c) {
  const m = /hsl\((\d+),(\d+)%,(\d+)%\)/.exec(c); if (!m) throw new Error(c);
  const h = +m[1] / 360, s = +m[2] / 100, l = +m[3] / 100, q = l < 0.5 ? l * (1 + s) : l + s - l * s, p = 2 * l - q;
  const k = (t) => { t = (t + 1) % 1; return t < 1 / 6 ? p + (q - p) * 6 * t : t < 0.5 ? q : t < 2 / 3 ? p + (q - p) * (2 / 3 - t) * 6 : p; };
  return '#' + [k(h + 1 / 3), k(h), k(h - 1 / 3)].map((v) => Math.round(v * 255).toString(16).padStart(2, '0')).join('');
}
const dir = path.join(__dirname, '..', '..', 'src', 'main', 'resources', 'data', 'toodeepseas', 'fish');
fs.rmSync(dir, {recursive: true, force: true}); fs.mkdirSync(dir, {recursive: true});
const seen = new Set(), count = {};
P.ITEMS.forEach((it, i) => {
  if (it.t !== 'pez' || it.hz) return;
  const id = slug(it.n); if (seen.has(id)) throw new Error('repetido: ' + id); seen.add(id);
  const o = {name: it.n, order: i, rarity: it.r, min_depth: it.d, biomes: it.b, movement: it.m, family: it.fam, color: it.col.startsWith('#') ? it.col : hsl(it.col)};
  if (it.w) o.weather = it.w;
  if (it.sub) o.sub_biome = it.sub;
  if (it.bait) { if (!CEBOS[it.bait]) throw new Error('cebo: ' + it.bait); o.bait = CEBOS[it.bait]; }
  if (it.sh) o.shape = it.sh;
  if (it.ghost) o.ghost = true;
  count[it.m] = (count[it.m] || 0) + 1;
  fs.writeFileSync(path.join(dir, id + '.json'), JSON.stringify(o, null, 2) + '\n');
});
console.log(seen.size + ' peces', count);
