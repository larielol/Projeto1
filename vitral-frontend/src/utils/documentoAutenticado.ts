import { TOKEN_KEY } from '../services/api'

/**
 * Documentos de verificacao exigem autenticacao (SEBO dono ou ADMIN). Uma navegacao
 * comum via <a href target="_blank"> nao envia o Bearer token, entao o backend
 * responde 403. Por isso abrimos a aba primeiro (preserva o gesto do usuario contra
 * bloqueadores de pop-up) e so depois buscamos o arquivo autenticado via fetch,
 * substituindo o conteudo da aba pelo blob recebido.
 *
 * Importante: a flag "noopener" faz window.open() retornar null (sem referencia
 * utilizavel), entao nao pode ser usada aqui. Em vez dela, zeramos manualmente
 * novaAba.opener logo apos abrir, o que isola a aba da mesma forma sem perder a
 * referencia necessaria para navega-la depois.
 */
export async function abrirDocumentoAutenticado(url: string): Promise<void> {
  const novaAba = window.open('', '_blank')
  if (novaAba) {
    try {
      novaAba.opener = null
    } catch {
      // Alguns navegadores/ambientes de teste nao permitem sobrescrever opener; inofensivo.
    }
  }
  try {
    const token = localStorage.getItem(TOKEN_KEY)
    const response = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
    if (!response.ok) {
      throw new Error('Não foi possível abrir o documento.')
    }
    const blob = await response.blob()
    const objectUrl = URL.createObjectURL(blob)
    if (novaAba) {
      novaAba.location.href = objectUrl
    } else {
      window.open(objectUrl, '_blank', 'noopener,noreferrer')
    }
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60_000)
  } catch (error) {
    novaAba?.close()
    throw error
  }
}
