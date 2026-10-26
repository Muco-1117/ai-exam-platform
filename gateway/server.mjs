import http from 'node:http'
import net from 'node:net'
import path from 'node:path'
import fs from 'node:fs'
import { fileURLToPath } from 'node:url'
import { randomUUID } from 'node:crypto'

const port = Number(process.env.PORT || 8080)
const upstream = new URL(process.env.EXAM_URL || 'http://127.0.0.1:8081')
const root = path.resolve(process.env.FRONTEND_DIST || path.join(path.dirname(fileURLToPath(import.meta.url)), '../frontend/dist'))
let requests = 0
const mime = { '.html': 'text/html; charset=utf-8', '.js': 'application/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8', '.svg': 'image/svg+xml', '.png': 'image/png', '.ico': 'image/x-icon', '.woff2': 'font/woff2' }
const server = http.createServer((req, res) => {
  const requestId = randomUUID()
  res.setHeader('X-Request-Id', requestId)
  res.setHeader('X-Content-Type-Options', 'nosniff')
  if (req.url === '/health') {
    res.setHeader('Content-Type', 'application/json; charset=utf-8')
    return res.end(JSON.stringify({ service: 'api-gateway', status: 'UP', version: '2.0.0', requests, upstream: upstream.origin }))
  }
  if (req.url?.startsWith('/api/')) {
    requests++
    const proxy = http.request({ hostname: upstream.hostname, port: upstream.port, path: req.url, method: req.method, headers: { ...req.headers, host: upstream.host, 'x-request-id': requestId, 'x-forwarded-for': req.socket.remoteAddress } }, (response) => {
      res.writeHead(response.statusCode || 502, { ...response.headers, 'X-Request-Id': requestId })
      response.pipe(res)
    })
    proxy.setTimeout(65000, () => proxy.destroy(new Error('upstream timeout')))
    proxy.on('error', () => {
      if (!res.headersSent) res.writeHead(502, { 'Content-Type': 'application/json; charset=utf-8' })
      res.end(JSON.stringify({ code: 502, message: '考试业务服务暂不可用，请检查服务状态', requestId }))
    })
    req.on('aborted', () => proxy.destroy())
    return req.pipe(proxy)
  }
  if (!['GET', 'HEAD'].includes(req.method || '')) { res.writeHead(405); return res.end() }
  let requested
  try { requested = decodeURIComponent(new URL(req.url, 'http://localhost').pathname) } catch { res.writeHead(400); return res.end() }
  let filename = path.resolve(root, '.' + requested)
  if (filename !== root && !filename.startsWith(root + path.sep)) { res.writeHead(403); return res.end() }
  if (!fs.existsSync(filename) || fs.statSync(filename).isDirectory()) {
    if (path.extname(requested) && !requested.endsWith('.html')) { res.writeHead(404); return res.end() }
    filename = path.join(root, 'index.html')
  }
  if (!fs.existsSync(filename)) { res.writeHead(503); return res.end('请先执行 frontend/npm run build') }
  res.setHeader('Content-Type', mime[path.extname(filename)] || 'application/octet-stream')
  res.setHeader('Cache-Control', path.basename(filename) === 'index.html' ? 'no-cache' : 'public, max-age=3600')
  if (req.method === 'HEAD') return res.end()
  fs.createReadStream(filename).on('error', () => res.destroy()).pipe(res)
})
server.on('upgrade', (req, socket, head) => {
  if (!req.url?.startsWith('/ws/')) return socket.destroy()
  const target = net.connect(Number(upstream.port || 80), upstream.hostname, () => {
    target.write(`${req.method} ${req.url} HTTP/${req.httpVersion}\r\n`)
    for (const [name, value] of Object.entries(req.headers)) {
      if (Array.isArray(value)) for (const part of value) target.write(`${name}: ${part}\r\n`)
      else if (value !== undefined) target.write(`${name}: ${value}\r\n`)
    }
    target.write('\r\n')
    if (head.length) target.write(head)
    socket.pipe(target).pipe(socket)
  })
  target.on('error', () => socket.destroy())
  socket.on('error', () => target.destroy())
  socket.on('close', () => target.destroy())
})
server.listen(port, '0.0.0.0', () => console.log(JSON.stringify({ service: 'api-gateway', port, upstream: upstream.origin, frontend: root })))
process.on('SIGTERM', () => server.close())
