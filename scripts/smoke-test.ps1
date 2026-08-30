param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string] $BaseUrl
)

$ErrorActionPreference = 'Stop'
$apiUrl = $BaseUrl.TrimEnd('/')
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$associateId = "smoke-associate-$runId"

$health = Invoke-RestMethod -Uri "$apiUrl/actuator/health"
if ($health.status -ne 'UP') {
    throw "Health check returned '$($health.status)'."
}

$openApi = Invoke-RestMethod -Uri "$apiUrl/api-docs"
if (-not $openApi.openapi.StartsWith('3.')) {
    throw "OpenAPI document is not available."
}

$agendaBody = @{
    title = "Deployment smoke test $runId"
    description = 'Automated verification of the published API.'
} | ConvertTo-Json

$agenda = Invoke-RestMethod `
    -Uri "$apiUrl/api/v1/agendas" `
    -Method Post `
    -ContentType 'application/json' `
    -Body $agendaBody

$session = Invoke-RestMethod `
    -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/voting-sessions" `
    -Method Post `
    -ContentType 'application/json' `
    -Body '{"durationMinutes":1}'

$voteBody = @{
    associateId = $associateId
    vote = 'Sim'
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/votes" `
    -Method Post `
    -ContentType 'application/json' `
    -Body $voteBody | Out-Null

$result = Invoke-RestMethod -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/result"
if ($result.yesVotes -ne 1 -or $result.noVotes -ne 0 -or $result.totalVotes -ne 1) {
    throw 'Vote totals do not match the smoke-test scenario.'
}

function Invoke-JsonHttp {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Method,
        [Parameter(Mandatory = $true)]
        [string] $Uri,
        [string] $Body
    )

    $outFile = Join-Path ([System.IO.Path]::GetTempPath()) ("coopvote-smoke-" + [guid]::NewGuid().ToString() + ".json")
    $curlArgs = @(
        '-sS',
        '-w', '%{http_code}',
        '-o', $outFile,
        '-X', $Method,
        $Uri
    )
    if ($PSBoundParameters.ContainsKey('Body')) {
        $curlArgs += @('-H', 'Content-Type: application/json', '--data-binary', $Body)
    }

    $statusCode = & curl.exe @curlArgs
    $payload = $null
    if ((Test-Path $outFile) -and (Get-Item $outFile).Length -gt 0) {
        $payload = Get-Content $outFile -Raw -Encoding UTF8 | ConvertFrom-Json
    }
    Remove-Item $outFile -ErrorAction SilentlyContinue

    [PSCustomObject]@{
        StatusCode = [int] $statusCode
        Payload = $payload
    }
}

$duplicate = Invoke-JsonHttp `
    -Method POST `
    -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/votes" `
    -Body $voteBody
if ($duplicate.StatusCode -ne 409 -or $duplicate.Payload.code -ne 'DUPLICATE_VOTE') {
    throw "Duplicate-vote protection returned HTTP $($duplicate.StatusCode) '$($duplicate.Payload.code)'."
}

$unsupportedMethod = Invoke-JsonHttp -Method DELETE -Uri "$apiUrl/api/v1/agendas"
if ($unsupportedMethod.StatusCode -ne 405 -or $unsupportedMethod.Payload.code -ne 'METHOD_NOT_ALLOWED') {
    throw "Unsupported method returned HTTP $($unsupportedMethod.StatusCode) '$($unsupportedMethod.Payload.code)'."
}

$missingResource = Invoke-JsonHttp -Method GET -Uri "$apiUrl/api/v1/does-not-exist"
if ($missingResource.StatusCode -ne 404 -or $missingResource.Payload.code -ne 'RESOURCE_NOT_FOUND') {
    throw "Unknown route returned HTTP $($missingResource.StatusCode) '$($missingResource.Payload.code)'."
}

[PSCustomObject]@{
    status = 'PASSED'
    health = $health.status
    openApi = $openApi.openapi
    agendaId = $agenda.id
    sessionId = $session.id
    yesVotes = $result.yesVotes
    duplicateVote = $duplicate.Payload.code
    unsupportedMethod = $unsupportedMethod.Payload.code
    missingResource = $missingResource.Payload.code
}
