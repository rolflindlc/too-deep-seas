// Carga prototipo/pesca.html en Node con un DOM simulado y devuelve window.__pesca.
// Uso: const {P, set} = require('./harness'); set({biome:'rio', water:80, seed:'123'}); P.startCast();
const fs = require('fs'), path = require('path'), vm = require('vm');
const html = fs.readFileSync(path.join(__dirname, '..', '..', 'prototipo', 'pesca.html'), 'utf8');
let src = html.slice(html.indexOf('<script>') + 8, html.lastIndexOf('</script>'));
// El estado es interno: se añade una función para volver al reposo y poder lanzar varias veces
const fin = src.lastIndexOf('})();');
src = src.slice(0, fin) + "window.__reset=()=>{state='idle'};\n" + src.slice(fin);
const stub = new Proxy(function () {}, {
  get: (t, k) => (k === Symbol.toPrimitive ? () => 0 : k === 'length' ? 0 : stub),
  apply: () => stub, construct: () => stub, set: () => true,
});
const VALUES = {biome: 'oceano', weather: 'despejado', water: 60, luck: 0, lure: 0, base: 0.8, k: 0.04, vh: 80, sub: 'yermos', seed: '1'};
const CHECKED = {hit: true, bait: true};
const els = {};
function el(id) {
  if (!els[id]) els[id] = new Proxy({}, {
    get: (t, k) => (k === 'value' ? (id in VALUES ? String(VALUES[id]) : '') : k === 'checked' ? !!CHECKED[id] : k in t ? t[k] : stub),
    set: (t, k, v) => { if (k === 'value') VALUES[id] = v; else if (k === 'checked') CHECKED[id] = v; else t[k] = v; return true; },
  });
  return els[id];
}
const win = {addEventListener() {}, matchMedia: () => ({matches: false, addEventListener() {}})};
const ctx = vm.createContext({
  window: win, document: {getElementById: el, querySelector: () => stub, querySelectorAll: () => [], addEventListener() {}, createElement: () => stub, body: stub},
  performance: {now: () => 0}, requestAnimationFrame() {}, console, Math, navigator: {}, localStorage: {getItem: () => null, setItem() {}},
  setTimeout() {}, clearTimeout() {}, addEventListener() {},
});
vm.runInContext(src, ctx);
const cast0 = win.__pesca.startCast;
win.__pesca.startCast = () => { win.__reset(); cast0(); };
module.exports = {P: win.__pesca, set: (o) => { for (const k in o) (k in CHECKED ? CHECKED : VALUES)[k] = o[k]; }};
