import { expect, test } from '@playwright/test'

async function login(page: import('@playwright/test').Page, email: string) {
  await page.goto('/auth')
  await page.getByLabel('Usuário ou E-mail:').fill(email)
  await page.locator('#login-password').fill('senha1234')
  await page.getByRole('button', { name: 'Entrar' }).click()
  await expect(page).toHaveURL('/')
}

test.describe.serial('frontend integrado ao backend real', () => {
  test('envia imagem e consulta arquivo enviado pela API real', async ({ request }) => {
    const loginResponse = await request.post('http://127.0.0.1:8081/api/v1/auth/login', {
      data: { email: 'sebo.e2e@vitral.test', password: 'senha1234' },
    })
    expect(loginResponse.ok()).toBeTruthy()
    const { token } = await loginResponse.json()
    const image = Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=',
      'base64',
    )

    const uploadResponse = await request.post('http://127.0.0.1:8081/api/v1/uploads/images', {
      headers: { Authorization: `Bearer ${token}` },
      multipart: {
        file: {
          name: 'pixel.png',
          mimeType: 'image/png',
          buffer: image,
        },
      },
    })

    expect(uploadResponse.ok()).toBeTruthy()
    const body = await uploadResponse.json()
    expect(body.url).toMatch(/^\/api\/v1\/uploads\/images\/.+\.png$/)

    const imageResponse = await request.get(`http://127.0.0.1:8081${body.url}`)
    expect(imageResponse.ok()).toBeTruthy()
    expect(imageResponse.headers()['content-type']).toContain('image/png')
  })

  test('consulta catalogo, categoria e detalhes sem mocks', async ({ page }) => {
    await page.goto('/')

    await expect(page.getByRole('heading', { name: 'Produtos disponíveis' })).toBeVisible()
    await expect(page.getByRole('link', { name: /Livro E2E Integrado/ }).first()).toBeVisible()

    await page.goto('/categorias')
    await page.getByRole('button', { name: /Romance E2E/ }).click()
    await expect(page.getByRole('link', { name: /Livro E2E Integrado/ })).toBeVisible()

    await page.goto('/produto/1')
    await expect(page.getByRole('heading', { name: 'Livro E2E Integrado' })).toBeVisible()
    await expect(page.getByText('Autora E2E')).toBeVisible()
    await expect(page.getByText('Sebo E2E')).toBeVisible()
  })

  test('busca produtos, aplica filtros e alterna para sebos', async ({ page }) => {
    await page.goto('/busca')
    await page.getByLabel('Pesquisar produtos ou sebos').fill('Livro E2E')
    await page.locator('.busca-search-bar').getByRole('button', { name: 'Buscar' }).click()

    await expect(page.getByText('Resultados para: "Livro E2E"')).toBeVisible()
    await expect(page.getByRole('link', { name: /Livro E2E Integrado/ })).toBeVisible()
    await expect(page.getByRole('link', { name: /Segundo Livro E2E/ })).toBeVisible()

    await page.getByLabel('Condição').selectOption('USADO')
    await page.getByLabel('Preço máx. (R$)').fill('43')
    await page.getByRole('button', { name: 'Aplicar' }).click()

    await expect(page.getByRole('tab', { name: 'Produtos (1)' })).toBeVisible()
    await expect(page.locator('.busca-grid .product-card-real', { hasText: 'Livro E2E Integrado' })).toBeVisible()
    await expect(page.locator('.busca-grid .product-card-real', { hasText: 'Segundo Livro E2E' })).toHaveCount(0)

    await page.getByRole('tab', { name: /Sebos/ }).click()
    await expect(page).toHaveURL(/tab=sebos/)
    const pageSearch = page.getByLabel('Pesquisar produtos ou sebos')
    await pageSearch.fill('Sebo E2E')
    await expect(pageSearch).toHaveValue('Sebo E2E')
    await page.locator('.busca-search-bar').getByRole('button', { name: 'Buscar' }).click()
    await expect(page).toHaveURL(/q=Sebo\+E2E|q=Sebo%20E2E/)
    await expect(page.getByRole('link', { name: /Sebo E2E/ })).toBeVisible()
    await page.getByLabel('Cidade').fill('Fortaleza')
    await page.getByLabel('Estado (UF)').fill('CE')
    await page.getByRole('button', { name: 'Aplicar' }).click()

    await expect(page.getByRole('link', { name: /Sebo E2E/ })).toBeVisible()
  })

  test('autentica e conclui favorito, cesta e pedido pela API real', async ({ page }) => {
    await login(page, 'cliente.e2e@vitral.test')

    await page.goto('/produto/1')
    await page.getByRole('button', { name: 'Favoritar' }).click()
    await expect(page.getByRole('status')).toContainText('favorit')
    await page.getByRole('button', { name: 'Adicionar à cesta' }).click()
    await expect(page.getByRole('status')).toContainText('cesta')

    await page.goto('/favoritos')
    await expect(page.getByRole('heading', { name: 'Livro E2E Integrado' })).toBeVisible()

    await page.goto('/carrinho')
    await expect(page.getByRole('heading', { name: 'Livro E2E Integrado' })).toBeVisible()
    await page.getByRole('spinbutton', { name: 'Qtd.' }).fill('2')
    await expect(page.getByText('2 itens na cesta')).toBeVisible()

    await page.goto('/confirmar-pedido')
    await page.getByRole('button', { name: 'Confirmar pedido' }).click()
    await expect(page.getByRole('heading', { name: /Pedido #\d+ enviado/ })).toBeVisible()

    await page.goto('/reservas')
    await page.getByRole('button', { name: 'Cancelar pedido' }).click()
    await expect(page.getByText('Nenhuma reserva em aberto')).toBeVisible()
  })

  test('cliente confirma pedido e sebo confirma a venda', async ({ page }) => {
    await login(page, 'cliente.e2e@vitral.test')

    await page.goto('/produto/2')
    await page.getByRole('button', { name: 'Adicionar à cesta' }).click()
    await expect(page.getByRole('status')).toContainText('cesta')

    await page.goto('/carrinho')
    await expect(page.getByRole('heading', { name: 'Segundo Livro E2E' })).toBeVisible()
    await expect(page.getByText('1 item na cesta')).toBeVisible()

    await page.goto('/confirmar-pedido')
    await page.getByRole('button', { name: 'Confirmar pedido' }).click()
    const confirmation = page.getByRole('heading', { name: /Pedido #\d+ enviado/ })
    await expect(confirmation).toBeVisible()
    const heading = await confirmation.textContent()
    const orderId = heading?.match(/#(\d+)/)?.[1]
    expect(orderId).toBeTruthy()

    await login(page, 'sebo.e2e@vitral.test')

    await page.goto('/vendas')
    const orderCard = page.locator('article.store-order-card').filter({ hasText: `Pedido #${orderId}` })
    await expect(orderCard).toContainText('Aguardando confirmação')
    await orderCard.getByRole('link', { name: 'Responder pedido' }).click()

    await expect(page.getByRole('heading', { name: `Pedido #${orderId}` })).toBeVisible()
    await page.getByRole('button', { name: 'Confirmar venda' }).click()
    await expect(page.getByRole('status')).toHaveText('Pedido confirmado com sucesso.')
    await expect(page.getByRole('link', { name: 'Ver histórico de vendas' })).toBeVisible()

    await page.goto('/vendas/historico')
    await expect(page.locator('article.store-order-card').filter({ hasText: `Pedido #${orderId}` })).toContainText('Confirmado')
  })

  test('cadastra uma conta e confirma um e-mail pela interface', async ({ page }) => {
    await page.goto('/auth')
    await page.getByLabel('Usuário:').fill('Nova Pessoa E2E')
    await page.getByLabel('Email:').fill('nova.pessoa.e2e@vitral.test')
    await page.locator('#reg-password').fill('senha1234')
    await page.getByLabel('Confirme sua senha:').fill('senha1234')
    await page.getByRole('button', { name: 'Cadastrar' }).click()
    await expect(page.getByRole('status')).toContainText('Verifique seu e-mail')
    await page.getByRole('link', { name: 'Não recebeu? Reenviar confirmação' }).click()
    await page.getByRole('button', { name: 'Reenviar link' }).click()
    await expect(page.getByRole('status')).toContainText('conta estiver pendente')

    await page.goto('/auth/confirmar?token=11111111-1111-1111-1111-111111111111')
    await expect(page.getByRole('heading', { name: 'E-mail confirmado!' })).toBeVisible()
    await page.getByRole('link', { name: 'Ir para o login agora' }).click()
    await login(page, 'confirmacao.e2e@vitral.test')
  })

  test('cliente e sebo trocam mensagens reais', async ({ page }) => {
    await login(page, 'cliente.e2e@vitral.test')
    await page.goto('/vendedor/1')
    await page.getByRole('link', { name: 'Enviar mensagem' }).click()
    await page.getByRole('textbox', { name: 'Mensagem' }).fill('Olá, esta é uma mensagem E2E.')
    await page.getByRole('button', { name: 'Enviar' }).click()
    await expect(page.getByText('Olá, esta é uma mensagem E2E.').last()).toBeVisible()

    await login(page, 'sebo.e2e@vitral.test')
    await page.goto('/mensagens')
    await page.locator('.store-message-list button').filter({ hasText: 'Olá, esta é uma mensagem E2E.' }).click()
    await expect(page.locator('.store-message-thread')).toContainText('Olá, esta é uma mensagem E2E.')
    await page.getByRole('textbox', { name: 'Mensagem' }).fill('Resposta E2E do sebo.')
    await page.getByRole('button', { name: 'Enviar' }).click()
    await expect(page.locator('.store-message-thread')).toContainText('Resposta E2E do sebo.')
  })

  test('pagina o catálogo do sebo sem esconder produtos', async ({ page, request }) => {
    const loginResponse = await request.post('http://127.0.0.1:8081/api/v1/auth/login', {
      data: { email: 'sebo.e2e@vitral.test', password: 'senha1234' },
    })
    const { token } = await loginResponse.json()
    for (let index = 1; index <= 11; index += 1) {
      const response = await request.post('http://127.0.0.1:8081/api/v1/produtos', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
          titulo: `Produto paginado ${index}`,
          preco: 10 + index,
          estoque: 1,
          condicao: 'USADO',
        },
      })
      expect(response.ok()).toBeTruthy()
    }

    await login(page, 'sebo.e2e@vitral.test')
    await page.goto('/painel/produtos')
    await expect(page.getByLabel('Páginas do catálogo')).toBeVisible()
    await page.getByRole('button', { name: 'Próxima' }).click()
    await expect(page.getByText('Página 2 de 2')).toBeVisible()
    await expect(page.locator('.produto-table tbody tr')).not.toHaveCount(0)
  })

  test('redefine a senha, invalida a sessão antiga e aceita nova recuperação', async ({ page, request }) => {
    const oldLogin = await request.post('http://127.0.0.1:8081/api/v1/auth/login', {
      data: { email: 'cliente.e2e@vitral.test', password: 'senha1234' },
    })
    const { token: oldToken } = await oldLogin.json()

    await page.goto('/auth/redefinir-senha?token=22222222-2222-2222-2222-222222222222')
    await page.getByLabel('Nova senha:', { exact: true }).fill('senhaNova123')
    await page.getByLabel('Confirme a nova senha:').fill('senhaNova123')
    await page.getByRole('button', { name: 'Redefinir senha' }).click()
    await expect(page.getByRole('status')).toContainText('Senha redefinida com sucesso')

    const expiredSession = await request.get('http://127.0.0.1:8081/api/v1/auth/me', {
      headers: { Authorization: `Bearer ${oldToken}` },
    })
    expect(expiredSession.status()).toBe(403)

    await page.goto('/auth')
    await page.getByLabel('Usuário ou E-mail:').fill('cliente.e2e@vitral.test')
    await page.locator('#login-password').fill('senhaNova123')
    await page.getByRole('button', { name: 'Entrar' }).click()
    await expect(page).toHaveURL('/')

    await page.goto('/auth/recuperar-senha')
    await page.getByLabel('E-mail:').fill('cliente.e2e@vitral.test')
    await page.getByRole('button', { name: 'Enviar link' }).click()
    await expect(page.getByRole('status')).toContainText('Se o e-mail estiver cadastrado')
  })

  test('faz CRUD do sebo e exclui a conta descartável', async ({ page }) => {
    await login(page, 'descartavel.e2e@vitral.test')
    await page.goto('/painel/sebo')
    await expect(page.getByRole('heading', { name: 'Cadastrar perfil do sebo' })).toBeVisible()
    await page.getByLabel('Descrição').fill('Perfil descartável E2E')
    await page.getByLabel('Cidade').fill('Fortaleza')
    await page.getByLabel('UF').fill('CE')
    await page.getByRole('button', { name: 'Cadastrar' }).click()
    await expect(page.getByRole('status')).toContainText('Sebo cadastrado com sucesso')

    await page.getByLabel('Descrição').fill('Perfil atualizado E2E')
    await page.getByRole('button', { name: 'Atualizar' }).click()
    await expect(page.getByRole('status')).toContainText('Sebo atualizado com sucesso')

    await page.goto('/painel/produtos')
    await page.getByLabel('Título').fill('Produto descartável E2E')
    await page.getByLabel('Preço (R$)').fill('25')
    await page.getByRole('button', { name: 'Adicionar produto' }).click()
    await expect(page.getByRole('status')).toContainText('Produto cadastrado')
    const row = page.locator('.produto-table tbody tr').filter({ hasText: 'Produto descartável E2E' })
    await row.getByRole('button', { name: 'Editar' }).click()
    await page.getByLabel('Título').fill('Produto editado E2E')
    await page.getByRole('button', { name: 'Atualizar produto' }).click()
    await expect(page.getByRole('status')).toContainText('Produto atualizado')
    page.once('dialog', (dialog) => dialog.accept())
    await page.locator('.produto-table tbody tr').filter({ hasText: 'Produto editado E2E' }).getByRole('button', { name: 'Remover' }).click()
    await expect(page.getByText('Produto editado E2E')).toHaveCount(0)

    await page.goto('/conta/excluir')
    await page.getByLabel('Confirmação').fill('EXCLUIR')
    await page.getByRole('button', { name: 'Excluir conta' }).click()
    await expect(page).toHaveURL('/auth')
  })
})
