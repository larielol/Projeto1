# Vitral Frontend

Frontend React + TypeScript do Vitral, integrado ao backend Spring em `/api/v1`.

## Requisitos

- Node.js compatível com Vite 8
- npm
- Backend Vitral rodando localmente, por padrão em `http://localhost:8080`

## Configuração

Crie um arquivo `.env` a partir do exemplo:

```bash
cp .env.example .env
```

Variáveis disponíveis:

```bash
VITE_API_URL=http://localhost:8080
VITE_API_BASE_URL=/api/v1
```

- `VITE_API_URL`: destino do proxy do Vite para chamadas iniciadas com `/api`.
- `VITE_API_BASE_URL`: base usada pelo Axios. Em desenvolvimento, mantenha `/api/v1` para usar o proxy e evitar CORS.

## Rodando localmente

```bash
npm install
npm run dev
```

O app abre em `http://localhost:5173` e encaminha `/api` para o backend configurado em `VITE_API_URL`.

## Scripts

```bash
npm run dev
npm run lint
npm run build
npm run preview
```

## Integrações atuais

Já usam endpoints reais do backend:

- autenticação, cadastro, confirmação de e-mail e logout;
- perfil do usuário autenticado;
- cadastro/edição do perfil do sebo;
- cadastro, edição, listagem e remoção de produtos do sebo;
- busca de produtos e sebos;
- detalhe real do produto por ID;
- perfil público real do sebo por ID;
- categorias e produtos por categoria;
- favoritos do usuário;
- cesta/carrinho persistido;
- quantidade de itens na cesta;
- confirmação de pedido a partir da cesta, com pagamento simulado (mock) e escolha da forma de pagamento;
- cancelamento de pedido pelo usuário enquanto aguarda confirmação;
- histórico de pedidos do usuário;
- compras recebidas pelo sebo;
- confirmação ou recusa de vendas pelo sebo;
- histórico de vendas do sebo;
- ofertas ativas (vitrine pública);
- gestão de ofertas pelo sebo (criar, editar, desativar e listar as próprias ofertas);
- lançamentos;
- mensagens entre contas;
- estoque e categoria no cadastro de produtos;
- exclusão de conta de usuário comum e de sebo;
- proteção de rotas para usuário autenticado e contas `SEBO`.

## Validação

Antes de abrir PR ou fazer push, rode:

```bash
npm run lint
npm run build
```
