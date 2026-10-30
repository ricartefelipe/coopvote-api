param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string] $BaseUrl,
    [string] $Manager = 'gestora',
    [string] $Password = $env:SMOKE_PASSWORD
)

$ErrorActionPreference = 'Stop'
$apiUrl = $BaseUrl.TrimEnd('/')
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
if (-not $Password) { $Password = 'coopvote-local' }

# Gestora que também vota: percorre cadastro, abertura, voto e apuração com um único token.
$login = Invoke-RestMethod -Uri "$apiUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' `
    -Body (@{ associateId = $Manager; password = $Password } | ConvertTo-Json)
$authHeaders = @{ Authorization = "Bearer $($login.accessToken)" }

function Invoke-Screen {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Url,

        [hashtable] $Body
    )

    $parameters = @{
        Uri = $Url
        Method = 'Post'
        Headers = $authHeaders
    }
    if ($null -ne $Body) {
        $parameters.ContentType = 'application/json'
        $parameters.Body = $Body | ConvertTo-Json
    }
    Invoke-RestMethod @parameters
}

function Get-SelectionAction {
    param(
        [Parameter(Mandatory = $true)]
        [object] $Screen,

        [Parameter(Mandatory = $true)]
        [string] $Text
    )

    if ($Screen.tipo -ne 'SELECAO') {
        throw "Expected SELECAO, received '$($Screen.tipo)'."
    }
    $action = $Screen.itens | Where-Object texto -eq $Text | Select-Object -First 1
    if ($null -eq $action) {
        throw "Selection action '$Text' was not found."
    }
    $action
}

function Assert-CallbackUrl {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Url
    )

    $callback = [Uri] $Url
    $expected = [Uri] $apiUrl
    $usesExpectedDomain = $callback.IsAbsoluteUri `
        -and $callback.Scheme -eq $expected.Scheme `
        -and $callback.Authority -eq $expected.Authority
    if (-not $usesExpectedDomain) {
        throw "Callback '$Url' does not use the configured API domain."
    }
}

$homeScreen = Invoke-Screen -Url "$apiUrl/api/v1/mobile"
$newAgendaAction = Get-SelectionAction -Screen $homeScreen -Text 'Cadastrar nova pauta'
Assert-CallbackUrl -Url $newAgendaAction.url

$newAgendaForm = Invoke-Screen -Url $newAgendaAction.url
if ($newAgendaForm.tipo -ne 'FORMULARIO') {
    throw "Expected FORMULARIO, received '$($newAgendaForm.tipo)'."
}
Assert-CallbackUrl -Url $newAgendaForm.botaoOk.url

$agendaActions = Invoke-Screen -Url $newAgendaForm.botaoOk.url -Body @{
    title = "Mobile smoke test $runId"
    description = 'Automated verification of the published mobile protocol.'
}
$newSessionAction = Get-SelectionAction -Screen $agendaActions -Text 'Abrir sessão de votação'
Assert-CallbackUrl -Url $newSessionAction.url

$newSessionForm = Invoke-Screen -Url $newSessionAction.url
Assert-CallbackUrl -Url $newSessionForm.botaoOk.url
$openActions = Invoke-Screen -Url $newSessionForm.botaoOk.url -Body @{
    durationMinutes = 5
}

$voteAction = Get-SelectionAction -Screen $openActions -Text 'Votar'
Assert-CallbackUrl -Url $voteAction.url
$voteForm = Invoke-Screen -Url $voteAction.url
Assert-CallbackUrl -Url $voteForm.botaoOk.url

$voteBody = @{}
if ($null -ne $voteForm.botaoOk.body) {
    $voteForm.botaoOk.body.psobject.Properties | ForEach-Object {
        $voteBody[$_.Name] = $_.Value
    }
}
$afterVote = Invoke-Screen -Url $voteForm.botaoOk.url -Body $voteBody

$resultAction = Get-SelectionAction -Screen $afterVote -Text 'Consultar resultado'
Assert-CallbackUrl -Url $resultAction.url
$result = Invoke-Screen -Url $resultAction.url
$lines = @($result.itens | Where-Object tipo -eq 'TEXTO' | ForEach-Object texto)
$hasExpectedResult = $result.tipo -eq 'FORMULARIO' `
    -and 'Votos Sim: 1' -in $lines `
    -and 'Votos Não: 0' -in $lines `
    -and 'Total de votos: 1' -in $lines `
    -and 'Resultado: Em andamento' -in $lines
if (-not $hasExpectedResult) {
    throw 'The result screen does not contain the expected vote totals.'
}

[PSCustomObject]@{
    status = 'PASSED'
    homeScreen = $homeScreen.tipo
    formScreen = $newAgendaForm.tipo
    resultScreen = $result.tipo
    callbackDomain = ([Uri] $resultAction.url).Authority
    yesVotes = 1
}
