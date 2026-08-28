$baseUrl = "http://localhost:8080"

# ============================================================
# FUNCOES AUXILIARES
# ============================================================

function Mostrar-Sucesso {
    param(
        [string]$Mensagem
    )

    Write-Host "[OK] $Mensagem" -ForegroundColor Green
}

function Mostrar-Erro {
    param(
        [string]$Mensagem
    )

    Write-Host "[ERRO] $Mensagem" -ForegroundColor Red
}

function Validar-ValorTotal {
    param(
        $Resposta,
        [decimal]$ValorEsperado,
        [string]$Descricao
    )

    $valorRetornado = [decimal]$Resposta.valorTotal

    if ($valorRetornado -eq $ValorEsperado) {
        Mostrar-Sucesso "$Descricao - valor total correto: R$ $valorRetornado"
    }
    else {
        Mostrar-Erro "$Descricao - valor esperado: R$ $ValorEsperado | valor retornado: R$ $valorRetornado"
    }
}

function Criar-Pedido {
    param(
        [string]$Descricao,
        [hashtable]$Pedido,
        [decimal]$ValorEsperado
    )

    Write-Host ""
    Write-Host "============================================================" -ForegroundColor DarkGray
    Write-Host $Descricao -ForegroundColor Cyan
    Write-Host "============================================================" -ForegroundColor DarkGray

    $json = $Pedido | ConvertTo-Json -Depth 5

    try {

        $resposta = Invoke-RestMethod `
            -Uri "$baseUrl/pedidos" `
            -Method Post `
            -Body $json `
            -ContentType "application/json" `
            -ErrorAction Stop

        $resposta | ConvertTo-Json -Depth 5

        Mostrar-Sucesso "Pedido criado com sucesso"
        Mostrar-Sucesso "ID do pedido: $($resposta.idPedido)"

        Validar-ValorTotal `
            -Resposta $resposta `
            -ValorEsperado $ValorEsperado `
            -Descricao $Descricao

        return $resposta
    }
    catch {

        Mostrar-Erro "Erro ao criar pedido"

        if ($_.ErrorDetails.Message) {
            Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow
        }

        return $null
    }
}


# ============================================================
# 0. VERIFICAR API
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "VERIFICACAO INICIAL DA API" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

Write-Host ""
Write-Host "Verificando se a API esta disponivel em $baseUrl..." -ForegroundColor Cyan

try {

    Invoke-RestMethod `
        -Uri "$baseUrl/produtos" `
        -Method Get `
        -ErrorAction Stop | Out-Null

    Mostrar-Sucesso "API disponivel e respondendo corretamente"
}
catch {

    Mostrar-Erro "Nao foi possivel conectar a API em $baseUrl"

    Write-Host ""
    Write-Host "Certifique-se de que os containers estao em execucao:" -ForegroundColor Yellow
    Write-Host "docker-compose up --build" -ForegroundColor Yellow

    exit 1
}


# ============================================================
# 1. LISTAR PRODUTOS
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "1. GET /produtos" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$produtos = Invoke-RestMethod `
    -Uri "$baseUrl/produtos" `
    -Method Get

$produtos | ConvertTo-Json -Depth 5


# ============================================================
# 2. PEDIDO COM UM ITEM
#
# Produto 1 = 3500
# Quantidade = 2
# Total esperado = 7000
# ============================================================

$pedido1 = @{
    idCliente = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"

    itens = @(
        @{
            idProduto = "11111111-1111-1111-1111-111111111111"
            qtdProduto = 2
        }
    )

    enderecoEntrega = @{
        logradouro = "Rua das Flores"
        numero = 123
        bairro = "Centro"
        cep = "86000-000"
        cidade = "Londrina"
        estado = "PR"
        complemento = "Apto 45"
    }
}

$resp1 = Criar-Pedido `
    -Descricao "2. POST /pedidos - um item" `
    -Pedido $pedido1 `
    -ValorEsperado 7000


# ============================================================
# 3. PEDIDO COM MULTIPLOS ITENS
#
# Produto 2 = 250 x 1 = 250
# Produto 3 = 180 x 3 = 540
# Produto 5 = 350 x 2 = 700
#
# Total esperado = 1490
# ============================================================

$pedido2 = @{
    idCliente = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"

    itens = @(
        @{
            idProduto = "22222222-2222-2222-2222-222222222222"
            qtdProduto = 1
        }

        @{
            idProduto = "33333333-3333-3333-3333-333333333333"
            qtdProduto = 3
        }

        @{
            idProduto = "55555555-5555-5555-5555-555555555555"
            qtdProduto = 2
        }
    )

    enderecoEntrega = @{
        logradouro = "Avenida Paulista"
        numero = 1000
        bairro = "Bela Vista"
        cep = "01310-100"
        cidade = "Sao Paulo"
        estado = "SP"
        complemento = ""
    }
}

$resp2 = Criar-Pedido `
    -Descricao "3. POST /pedidos - multiplos itens" `
    -Pedido $pedido2 `
    -ValorEsperado 1490


# ============================================================
# 4. PEDIDO COM PRODUTOS 1 E 4
#
# Produto 1 = 3500 x 1 = 3500
# Produto 4 = 1800 x 1 = 1800
#
# Total esperado = 5300
# ============================================================

$pedido3 = @{
    idCliente = "dddddddd-dddd-dddd-dddd-dddddddddddd"

    itens = @(
        @{
            idProduto = "11111111-1111-1111-1111-111111111111"
            qtdProduto = 1
        }

        @{
            idProduto = "44444444-4444-4444-4444-444444444444"
            qtdProduto = 1
        }
    )

    enderecoEntrega = @{
        logradouro = "Rua XV de Novembro"
        numero = 500
        bairro = "Centro"
        cep = "80020-310"
        cidade = "Curitiba"
        estado = "PR"
        complemento = "Sala 10"
    }
}

$resp3 = Criar-Pedido `
    -Descricao "4. POST /pedidos - produtos 1 e 4" `
    -Pedido $pedido3 `
    -ValorEsperado 5300


# ============================================================
# 5. PEDIDO COM PRODUTOS 2, 3 E 4
#
# Produto 2 = 250 x 4 = 1000
# Produto 3 = 180 x 2 = 360
# Produto 4 = 1800 x 1 = 1800
#
# Total esperado = 3160
# ============================================================

$pedido4 = @{
    idCliente = "eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"

    itens = @(
        @{
            idProduto = "22222222-2222-2222-2222-222222222222"
            qtdProduto = 4
        }

        @{
            idProduto = "33333333-3333-3333-3333-333333333333"
            qtdProduto = 2
        }

        @{
            idProduto = "44444444-4444-4444-4444-444444444444"
            qtdProduto = 1
        }
    )

    enderecoEntrega = @{
        logradouro = "Avenida Brasil"
        numero = 2500
        bairro = "Jardim America"
        cep = "86010-000"
        cidade = "Londrina"
        estado = "PR"
        complemento = "Casa"
    }
}

$resp4 = Criar-Pedido `
    -Descricao "5. POST /pedidos - produtos 2, 3 e 4" `
    -Pedido $pedido4 `
    -ValorEsperado 3160


# ============================================================
# 6. PEDIDO COM TODOS OS PRODUTOS
#
# Produto 1 = 3500
# Produto 2 = 250
# Produto 3 = 180
# Produto 4 = 1800
# Produto 5 = 350
#
# Total esperado = 6080
# ============================================================

$pedido5 = @{
    idCliente = "ffffffff-ffff-ffff-ffff-ffffffffffff"

    itens = @(
        @{
            idProduto = "11111111-1111-1111-1111-111111111111"
            qtdProduto = 1
        }

        @{
            idProduto = "22222222-2222-2222-2222-222222222222"
            qtdProduto = 1
        }

        @{
            idProduto = "33333333-3333-3333-3333-333333333333"
            qtdProduto = 1
        }

        @{
            idProduto = "44444444-4444-4444-4444-444444444444"
            qtdProduto = 1
        }

        @{
            idProduto = "55555555-5555-5555-5555-555555555555"
            qtdProduto = 1
        }
    )

    enderecoEntrega = @{
        logradouro = "Rua da Tecnologia"
        numero = 777
        bairro = "Centro"
        cep = "04567-000"
        cidade = "Sao Paulo"
        estado = "SP"
        complemento = "Empresa"
    }
}

$resp5 = Criar-Pedido `
    -Descricao "6. POST /pedidos - todos os produtos" `
    -Pedido $pedido5 `
    -ValorEsperado 6080


# ============================================================
# 7. PRODUTO INEXISTENTE - ESPERA 404
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "7. POST /pedidos - produto inexistente - espera 404" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$pedidoErro = @{
    idCliente = "cccccccc-cccc-cccc-cccc-cccccccccccc"

    itens = @(
        @{
            idProduto = "99999999-9999-9999-9999-999999999999"
            qtdProduto = 1
        }
    )

    enderecoEntrega = @{
        logradouro = "Rua Teste"
        numero = 1
        bairro = "Teste"
        cep = "00000-000"
        cidade = "Teste"
        estado = "TT"
        complemento = ""
    }
} | ConvertTo-Json -Depth 5

try {

    Invoke-RestMethod `
        -Uri "$baseUrl/pedidos" `
        -Method Post `
        -Body $pedidoErro `
        -ContentType "application/json" `
        -ErrorAction Stop

    Mostrar-Erro "Era esperado erro 404, mas o pedido foi criado"
}
catch {

    Mostrar-Sucesso "Erro esperado recebido"

    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow
    }
}


# ============================================================
# 8. VALIDACAO - ESPERA 400
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "8. POST /pedidos - validacao - espera 400" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$pedidoInvalido = @{
    idCliente = $null
    itens = @()
    enderecoEntrega = $null
} | ConvertTo-Json

try {

    Invoke-RestMethod `
        -Uri "$baseUrl/pedidos" `
        -Method Post `
        -Body $pedidoInvalido `
        -ContentType "application/json" `
        -ErrorAction Stop

    Mostrar-Erro "Era esperado erro 400, mas o pedido foi criado"
}
catch {

    Mostrar-Sucesso "Erro de validacao esperado recebido"

    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow
    }
}


# ============================================================
# 9. QUANTIDADE ZERO - ESPERA 400
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "9. POST /pedidos - quantidade zero - espera 400" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$pedidoQuantidadeInvalida = @{
    idCliente = "abababab-abab-abab-abab-abababababab"

    itens = @(
        @{
            idProduto = "11111111-1111-1111-1111-111111111111"
            qtdProduto = 0
        }
    )

    enderecoEntrega = @{
        logradouro = "Rua da Validacao"
        numero = 10
        bairro = "Centro"
        cep = "86000-000"
        cidade = "Londrina"
        estado = "PR"
        complemento = ""
    }
} | ConvertTo-Json -Depth 5

try {

    Invoke-RestMethod `
        -Uri "$baseUrl/pedidos" `
        -Method Post `
        -Body $pedidoQuantidadeInvalida `
        -ContentType "application/json" `
        -ErrorAction Stop

    Mostrar-Erro "Era esperado erro 400, mas o pedido foi criado"
}
catch {

    Mostrar-Sucesso "Erro de quantidade invalida recebido"

    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message -ForegroundColor Yellow
    }
}


# ============================================================
# 10. LISTAR PEDIDOS
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "10. GET /pedidos" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$pedidos = Invoke-RestMethod `
    -Uri "$baseUrl/pedidos" `
    -Method Get

$pedidos | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "Total de pedidos persistidos: $($pedidos.Count)" -ForegroundColor Cyan


# ============================================================
# 11. AGUARDAR PROCESSAMENTO DA FILA
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "PROCESSAMENTO ASSINCRONO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

Write-Host ""
Write-Host "Aguardando processamento das mensagens pelo RabbitMQ..." -ForegroundColor Yellow

Start-Sleep -Seconds 2

Mostrar-Sucesso "Tempo de processamento concluido"


# ============================================================
# 12. LISTAR ENTREGAS
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "12. GET /entregas" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$entregas = Invoke-RestMethod `
    -Uri "$baseUrl/entregas" `
    -Method Get

$entregas | ConvertTo-Json -Depth 5

Write-Host ""
Write-Host "Total de entregas criadas: $($entregas.Count)" -ForegroundColor Cyan


# ============================================================
# 13. RESUMO FINAL
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "RESUMO FINAL DA EXECUCAO" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor DarkGray

$pedidosCriados = @(
    $resp1
    $resp2
    $resp3
    $resp4
    $resp5
) | Where-Object { $_ -ne $null }

Write-Host ""
Write-Host "Pedidos criados com sucesso: $($pedidosCriados.Count)" -ForegroundColor Cyan
Write-Host "Pedidos persistidos no banco: $($pedidos.Count)" -ForegroundColor Cyan
Write-Host "Entregas criadas pelo consumidor: $($entregas.Count)" -ForegroundColor Cyan

if ($entregas.Count -ge $pedidosCriados.Count) {
    Mostrar-Sucesso "Fluxo de mensageria executado com sucesso"
}
else {
    Mostrar-Erro "Nem todos os pedidos possuem entregas processadas ainda"
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor DarkGray
Write-Host "EXECUCAO FINALIZADA" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor DarkGray
