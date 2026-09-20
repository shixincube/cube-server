/** 表单校验工具，规则与旧版 `assets/js/util.js` 一致。 */

const IPV4_PATTERN =
  /^(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])\.(\d{1,2}|1\d\d|2[0-4]\d|25[0-5])$/

const UNSIGNED_PATTERN = /^[1-9]\d*$/

/** 是否为合法 IPv4 */
export function isIPv4(value: string): boolean {
  return IPV4_PATTERN.test(value)
}

/** 是否为合法 IPv6 */
export function isIPv6(value: string): boolean {
  const colonCount = (value.match(/:/g) || []).length
  if (!value.includes(':') || colonCount >= 8) {
    return false
  }
  if (value.includes('::')) {
    return (
      (value.match(/::/g) || []).length === 1 &&
      /^::$|^(::)?([\da-f]{1,4}(:|::))*[\da-f]{1,4}(:|::)?$/i.test(value)
    )
  }
  return /^([\da-f]{1,4}:){7}[\da-f]{1,4}$/i.test(value)
}

/** 主机地址（IPv4 或 IPv6）是否合法 */
export function isHostAddress(value: string): boolean {
  return isIPv4(value) || isIPv6(value)
}

/** 是否为正整数 */
export function isUnsigned(value: string): boolean {
  return UNSIGNED_PATTERN.test(value)
}

/** 是否为合法端口 */
export function isPort(value: string): boolean {
  return isUnsigned(value) && Number(value) <= 65535
}
