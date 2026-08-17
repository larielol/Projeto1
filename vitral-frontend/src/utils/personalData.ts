export const onlyDigits = (value: string) => value.replace(/\D/g, '')

export const BRAZILIAN_UFS = new Set([
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
  'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO',
])

export function formatCpf(value: string): string {
  return onlyDigits(value).slice(0, 11).replace(/^(\d{3})(\d)/, '$1.$2').replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3').replace(/\.(\d{3})(\d)/, '.$1-$2')
}

export function formatCep(value: string): string {
  return onlyDigits(value).slice(0, 8).replace(/^(\d{5})(\d)/, '$1-$2')
}

export function isValidCpf(value: string): boolean {
  const cpf = onlyDigits(value)
  if (cpf.length !== 11 || /^(\d)\1{10}$/.test(cpf)) return false
  const digit = (length: number) => {
    const sum = cpf.slice(0, length).split('').reduce((total, number, index) => total + Number(number) * (length + 1 - index), 0)
    const result = (sum * 10) % 11
    return result === 10 ? 0 : result
  }
  return digit(9) === Number(cpf[9]) && digit(10) === Number(cpf[10])
}

export function isMaskedCpf(value: string): boolean {
  return value.includes('*') || (value.trim().length > 0 && onlyDigits(value).length !== 11)
}

export function isValidCep(value: string): boolean {
  return onlyDigits(value).length === 8
}

export function isValidUf(value: string): boolean {
  return BRAZILIAN_UFS.has(value.trim().toUpperCase())
}
