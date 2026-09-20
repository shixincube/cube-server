/**
 * MD5 实现（RFC 1321）。
 *
 * 控制台的登录口令与管理口令均以 MD5 十六进制摘要提交，后端
 * `UserManager#signIn` 与 `User#validatePassword` 按摘要比对，
 * 因此该实现必须与 `md5.js` 的输出保持一致。
 */

/** 每轮的左移位数 */
const SHIFT = [
  7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9, 14,
  20, 5, 9, 14, 20, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 6, 10, 15, 21, 6,
  10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21
]

/** 正弦常量表：K[i] = floor(abs(sin(i + 1)) * 2^32) */
const SINE_TABLE = (() => {
  const table = new Uint32Array(64)
  for (let i = 0; i < 64; i++) {
    table[i] = Math.floor(Math.abs(Math.sin(i + 1)) * 4294967296)
  }
  return table
})()

const encoder = new TextEncoder()

/**
 * 计算字符串（UTF-8 编码）的 MD5 摘要。
 *
 * @param input 原始文本
 * @returns 32 位小写十六进制摘要
 */
export function md5(input: string): string {
  const bytes = encoder.encode(input)
  const bitLength = bytes.length * 8

  // 填充：附加 0x80，补零至 (len + 9) % 64 == 0，末尾 8 字节写入比特长度（小端）
  const paddedLength = Math.ceil((bytes.length + 9) / 64) * 64
  const buffer = new Uint8Array(paddedLength)
  buffer.set(bytes)
  buffer[bytes.length] = 0x80

  const view = new DataView(buffer.buffer)
  view.setUint32(paddedLength - 8, bitLength >>> 0, true)
  view.setUint32(paddedLength - 4, Math.floor(bitLength / 4294967296), true)

  let h0 = 0x67452301
  let h1 = 0xefcdab89
  let h2 = 0x98badcfe
  let h3 = 0x10325476

  const words = new Uint32Array(16)

  for (let offset = 0; offset < paddedLength; offset += 64) {
    for (let i = 0; i < 16; i++) {
      words[i] = view.getUint32(offset + i * 4, true)
    }

    let a = h0
    let b = h1
    let c = h2
    let d = h3

    for (let i = 0; i < 64; i++) {
      let f: number
      let g: number

      if (i < 16) {
        f = (b & c) | (~b & d)
        g = i
      } else if (i < 32) {
        f = (d & b) | (~d & c)
        g = (5 * i + 1) % 16
      } else if (i < 48) {
        f = b ^ c ^ d
        g = (3 * i + 5) % 16
      } else {
        f = c ^ (b | ~d)
        g = (7 * i) % 16
      }

      const sum = (f + a + SINE_TABLE[i] + words[g]) >>> 0
      a = d
      d = c
      c = b
      b = (b + ((sum << SHIFT[i]) | (sum >>> (32 - SHIFT[i])))) >>> 0
    }

    h0 = (h0 + a) >>> 0
    h1 = (h1 + b) >>> 0
    h2 = (h2 + c) >>> 0
    h3 = (h3 + d) >>> 0
  }

  const digest = new Uint8Array(16)
  const digestView = new DataView(digest.buffer)
  digestView.setUint32(0, h0, true)
  digestView.setUint32(4, h1, true)
  digestView.setUint32(8, h2, true)
  digestView.setUint32(12, h3, true)

  let hex = ''
  for (let i = 0; i < 16; i++) {
    hex += digest[i].toString(16).padStart(2, '0')
  }
  return hex
}
